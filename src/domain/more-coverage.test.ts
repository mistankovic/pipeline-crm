import { describe, expect, it } from "vitest";
import { Activity } from "./activity";
import { ActivityType } from "./activity-type";
import { Company } from "./company";
import { Contact } from "./contact";
import { Deal } from "./deal";
import { DealStage } from "./deal-stage";
import { EmailAddress } from "./email-address";
import { Money } from "./money";
import { Probability } from "./probability";
import { Role } from "./role";
import { CrmUser } from "./user";
import { Actor } from "./actor";
import { Codes, DomainError } from "./errors";

describe("remaining domain branches", () => {
  it("covers money helpers", () => {
    const money = Money.fromMinor(199, "GBP");
    expect(money.amount).toBe(1.99);
    expect(money.isZero()).toBe(false);
    expect(Money.zero("GBP").isZero()).toBe(true);
    expect(money.format()).toContain("GBP");
    expect(() => Money.fromMinor(-1, "GBP")).toThrow(DomainError);
  });

  it("covers email, role, probability, and stage catalogs", () => {
    const email = EmailAddress.parse("Mina@Harbor.co");
    expect(email.localPart()).toBe("mina");
    expect(email.equals(EmailAddress.parse("mina@harbor.co"))).toBe(true);
    expect(Role.sales().equals(Role.sales())).toBe(true);
    expect(Role.sales().canManageOthersDeals()).toBe(false);
    expect(Probability.of(10).equals(Probability.of(10))).toBe(true);
    expect(DealStage.openPipeline().map((s) => s.name)).toContain("LEAD");
    expect(DealStage.all().map((s) => s.name)).toContain("CLOSED_WON");
    expect(ActivityType.note().equals(ActivityType.note())).toBe(true);
  });

  it("covers company rename, user rename, contact update, activity snapshot", () => {
    const company = Company.create({ id: "c", name: "Harbor" }).rename("Harbor & Co");
    expect(company.name).toBe("Harbor & Co");
    const user = CrmUser.create({
      id: "u",
      email: "a@b.co",
      name: "Alex",
      role: Role.sales(),
    }).withName("Alexandra");
    expect(user.name).toBe("Alexandra");
    const contact = Contact.create({
      id: "k",
      companyId: "c",
      name: "Mina",
    }).update({
      name: "Mina Cole",
      email: "mina@harbor.co",
      title: "Head of Fleet",
      companyId: "c2",
    });
    expect(contact.toSnapshot()).toMatchObject({
      name: "Mina Cole",
      email: "mina@harbor.co",
      companyId: "c2",
    });
    const activity = Activity.create({
      id: "a",
      type: ActivityType.call(),
      body: "Hello",
      createdByUserId: "u",
      contactId: "k",
      occurredAt: new Date("2026-01-01T00:00:00Z"),
    });
    const copy = Activity.rehydrate(activity.toSnapshot());
    expect(copy.body).toBe("Hello");
    expect(copy.countsTowardCloseWon("nope")).toBe(false);
  });

  it("weights open deals and updates details", () => {
    const owner = new Actor("alex", Role.sales());
    const deal = Deal.open({
      id: "d",
      title: "X",
      companyId: "c",
      ownerId: "alex",
      value: Money.of(100, "USD"),
      probability: Probability.of(50),
    });
    expect(deal.weightedValue().amount).toBe(50);
    const updated = deal.updateDetails(owner, {
      title: "Y",
      companyId: "c2",
      value: Money.of(200, "USD"),
      probability: Probability.of(10),
    });
    expect(updated.title).toBe("Y");
    const lost = deal.changeStage(owner, DealStage.parse("CLOSED_LOST"), []);
    expect(lost.weightedValue().isZero()).toBe(true);
  });

  it("rejects a contact update that drops the company", () => {
    const contact = Contact.create({ id: "k", companyId: "c", name: "Mina" });
    try {
      contact.update({ companyId: " " });
      throw new Error("expected failure");
    } catch (error) {
      expect((error as DomainError).code).toBe(Codes.CONTACT_COMPANY_REQUIRED);
    }
  });
});
