import { describe, expect, it } from "vitest";
import { createMemoryPorts } from "../../tests/support/memory-ports";
import { Codes } from "../domain/errors";
import { NotFoundError } from "./errors";
import { requireActivity } from "./require";
import {
  createActivity,
  listContactActivities,
  listDealActivities,
} from "./usecases/activities";
import { seedIfEmpty } from "./usecases/bootstrap";
import {
  createCompany,
  getCompany,
  listCompanies,
  updateCompany,
} from "./usecases/companies";
import {
  createContact,
  getContact,
  listContacts,
  updateContact,
} from "./usecases/contacts";
import { createDeal, updateDeal } from "./usecases/deals";
import { assignRole, ensureProfile, listUsers } from "./usecases/profiles";
import { loadWorkspace } from "./usecases/workspace";

describe("application edges", () => {
  it("covers not-found, role assignment, and list helpers", async () => {
    const ports = createMemoryPorts();
    const alex = await ensureProfile(ports, { id: "alex", email: "alex@a.co", name: "Alex" });
    const blair = await ensureProfile(ports, { id: "blair", email: "blair@a.co", name: "Blair" });
    await expect(requireActivity(ports.activities, "missing")).rejects.toBeInstanceOf(
      NotFoundError,
    );
    await expect(
      assignRole(ports, { actorId: blair.id, userId: alex.id, role: "MANAGER" }),
    ).rejects.toMatchObject({ code: Codes.NOT_AUTHORIZED });
    const users = await listUsers(ports);
    expect(users).toHaveLength(2);
    const company = await createCompany(ports, { name: "Acme" });
    expect((await getCompany(ports, company.id)).name).toBe("Acme");
    expect((await listCompanies(ports))[0]?.name).toBe("Acme");
    const contact = await createContact(ports, { companyId: company.id, name: "Pat" });
    const renamed = await updateContact(ports, {
      id: contact.id,
      name: "Pat Lee",
      title: "Buyer",
      email: "pat@acme.co",
    });
    expect(renamed.title).toBe("Buyer");
    expect((await getContact(ports, contact.id)).name).toBe("Pat Lee");
    expect(await listContacts(ports)).toHaveLength(1);
    const deal = await createDeal(ports, {
      actorId: alex.id,
      title: "Widget",
      companyId: company.id,
      amount: 10,
      currency: "USD",
      probability: 10,
    });
    await expect(
      createDeal(ports, {
        actorId: alex.id,
        title: "Bad fx",
        companyId: company.id,
        amount: 1,
        currency: "JPY",
        probability: 1,
      }),
    ).rejects.toMatchObject({ code: Codes.INVALID_MONEY });
    const updated = await updateDeal(ports, {
      actorId: alex.id,
      dealId: deal.id,
      currency: "EUR",
      amount: 20,
    });
    expect(updated.value.currency).toBe("EUR");
    await expect(
      updateDeal(ports, { actorId: alex.id, dealId: deal.id, currency: "JPY" }),
    ).rejects.toMatchObject({ code: Codes.INVALID_MONEY });
    await createActivity(ports, {
      actorId: alex.id,
      type: "NOTE",
      body: "Hello",
      dealId: deal.id,
      contactId: contact.id,
    });
    expect(await listDealActivities(ports, deal.id)).toHaveLength(1);
    expect(await listContactActivities(ports, contact.id)).toHaveLength(1);
  });

  it("seeds an empty workspace once and loads it", async () => {
    const ports = createMemoryPorts();
    const alex = await ensureProfile(ports, { id: "alex", email: "alex@a.co", name: "Alex" });
    await seedIfEmpty(ports, alex.id);
    await seedIfEmpty(ports, alex.id);
    const workspace = await loadWorkspace(ports, alex);
    expect(workspace.companies.length).toBeGreaterThan(3);
    expect(workspace.deals.some((deal) => deal.stage.isWon())).toBe(true);
    expect(workspace.activities.length).toBeGreaterThan(3);
  });

  it("uses email local-part when no name is given", async () => {
    const ports = createMemoryPorts();
    const user = await ensureProfile(ports, { id: "x", email: "taylor@harbor.co" });
    expect(user.name).toBe("taylor");
  });
});
