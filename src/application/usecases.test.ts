import { describe, expect, it } from "vitest";
import { createMemoryPorts } from "../../tests/support/memory-ports";
import { Codes, DomainError } from "../domain/errors";
import { Role } from "../domain/role";
import { NotFoundError } from "./errors";
import { createActivity } from "./usecases/activities";
import { createCompany } from "./usecases/companies";
import { createContact } from "./usecases/contacts";
import {
  changeDealStage,
  createDeal,
  reassignDeal,
  updateDeal,
} from "./usecases/deals";
import { getForecast } from "./usecases/forecast";
import { assignRole, ensureProfile } from "./usecases/profiles";

async function world() {
  const ports = createMemoryPorts();
  const alex = await ensureProfile(ports, {
    id: "alex",
    email: "alex@harbor.co",
    name: "Alex",
  });
  const blair = await ensureProfile(ports, {
    id: "blair",
    email: "blair@harbor.co",
    name: "Blair",
  });
  const company = await createCompany(ports, { name: "Harbor & Co" });
  return { ports, alex, blair, company };
}

describe("ensureProfile", () => {
  it("makes the first user a manager and later users sales", async () => {
    const { alex, blair } = await world();
    expect(alex.role.isManager()).toBe(true);
    expect(blair.role.isManager()).toBe(false);
  });

  it("is idempotent for the same auth id", async () => {
    const { ports } = await world();
    const again = await ensureProfile(ports, {
      id: "alex",
      email: "other@harbor.co",
      name: "Nope",
    });
    expect(again.email.value).toBe("alex@harbor.co");
  });
});

describe("use cases", () => {
  it("creates, updates, and lists a company and contact", async () => {
    const { ports, company } = await world();
    const renamed = await (await import("./usecases/companies")).updateCompany(ports, {
      id: company.id,
      name: "Harbor",
      domain: "harbor.co",
    });
    expect(renamed.domain).toBe("harbor.co");
    const contact = await createContact(ports, {
      companyId: company.id,
      name: "Mina Cole",
      email: "mina@harbor.co",
    });
    expect(contact.email?.value).toBe("mina@harbor.co");
  });

  it("rejects a deal for an unknown company", async () => {
    const { ports, alex } = await world();
    await expect(
      createDeal(ports, {
        actorId: alex.id,
        title: "Ghost",
        companyId: "missing",
        amount: 10,
        currency: "USD",
        probability: 10,
      }),
    ).rejects.toBeInstanceOf(NotFoundError);
  });

  it("stops a sales user creating a deal for someone else", async () => {
    const { ports, blair, company } = await world();
    await expect(
      createDeal(ports, {
        actorId: blair.id,
        ownerId: "alex",
        title: "Stolen",
        companyId: company.id,
        amount: 10,
        currency: "USD",
        probability: 10,
      }),
    ).rejects.toMatchObject({ code: Codes.NOT_AUTHORIZED });
  });

  it("changes stage through the use case and forecasts the result", async () => {
    const { ports, alex, company } = await world();
    const deal = await createDeal(ports, {
      actorId: alex.id,
      title: "Fleet",
      companyId: company.id,
      amount: 100000,
      currency: "USD",
      probability: 50,
    });
    await createActivity(ports, {
      actorId: alex.id,
      type: "MEETING",
      body: "Kickoff",
      dealId: deal.id,
    });
    const won = await changeDealStage(ports, {
      actorId: alex.id,
      dealId: deal.id,
      targetStage: "CLOSED_WON",
    });
    expect(won.probability.percent).toBe(100);
    const forecast = await getForecast(ports, "owner");
    expect(forecast.grand).toEqual([]);
  });

  it("lets a manager assign roles and reassign deals", async () => {
    const { ports, alex, blair, company } = await world();
    const promoted = await assignRole(ports, {
      actorId: alex.id,
      userId: blair.id,
      role: Role.manager().name,
    });
    expect(promoted.role.isManager()).toBe(true);
    const deal = await createDeal(ports, {
      actorId: alex.id,
      title: "Yard",
      companyId: company.id,
      amount: 1,
      currency: "USD",
      probability: 10,
    });
    const moved = await reassignDeal(ports, {
      actorId: alex.id,
      dealId: deal.id,
      newOwnerId: blair.id,
    });
    expect(moved.ownerId).toBe(blair.id);
  });

  it("updates deal details while open", async () => {
    const { ports, alex, company } = await world();
    const deal = await createDeal(ports, {
      actorId: alex.id,
      title: "Yard",
      companyId: company.id,
      amount: 10,
      currency: "USD",
      probability: 10,
    });
    const updated = await updateDeal(ports, {
      actorId: alex.id,
      dealId: deal.id,
      title: "Yard scanners",
      amount: 80,
      probability: 30,
    });
    expect(updated.title).toBe("Yard scanners");
    expect(updated.value.amount).toBe(80);
  });

  it("surfaces domain errors from the activity use case", async () => {
    const { ports, alex } = await world();
    await expect(
      createActivity(ports, {
        actorId: alex.id,
        type: "CALL",
        body: "Orphan",
      }),
    ).rejects.toBeInstanceOf(DomainError);
  });
});
