import { describe, expect, it } from "vitest";
import { Activity } from "./activity";
import { ActivityType } from "./activity-type";
import { Company } from "./company";
import { Contact } from "./contact";
import { Codes, DomainError } from "./errors";
import { EmailAddress } from "./email-address";
import { DealStage } from "./deal-stage";
import { Money } from "./money";
import { Probability } from "./probability";
import { Role } from "./role";
import { CrmUser } from "./user";

function expectCode(run: () => unknown, code: string) {
  try {
    run();
    throw new Error("expected domain error");
  } catch (error) {
    expect(error).toBeInstanceOf(DomainError);
    expect((error as DomainError).code).toBe(code);
  }
}

describe("Money", () => {
  it("stores minor units and rejects negatives", () => {
    expect(Money.of(10.5, "USD").minorUnits).toBe(1050);
    expectCode(() => Money.of(-1, "USD"), Codes.INVALID_MONEY);
    expectCode(() => Money.of(Number.NaN, "USD"), Codes.INVALID_MONEY);
  });

  it("refuses to add mixed currencies", () => {
    expectCode(() => Money.of(1, "USD").add(Money.of(1, "EUR")), Codes.CURRENCY_MISMATCH);
  });

  it("weights by percent using integer rounding", () => {
    expect(Money.of(100, "USD").weightedByPercent(33).minorUnits).toBe(3300);
  });
});

describe("Probability", () => {
  it("accepts only integers 0–100", () => {
    expect(Probability.of(0).asFraction()).toBe(0);
    expect(Probability.certain().percent).toBe(100);
    expectCode(() => Probability.of(101), Codes.INVALID_PROBABILITY);
    expectCode(() => Probability.of(10.5), Codes.INVALID_PROBABILITY);
  });
});

describe("Email, role, stage, activity type", () => {
  it("normalizes email and rejects junk", () => {
    expect(EmailAddress.parse("  Alex@Harbor.co ").value).toBe("alex@harbor.co");
    expectCode(() => EmailAddress.parse("nope"), Codes.INVALID_EMAIL);
  });

  it("parses known roles and stages only", () => {
    expect(Role.parse("MANAGER").isManager()).toBe(true);
    expectCode(() => Role.parse("ADMIN"), Codes.INVALID_ROLE);
    expect(DealStage.parse("LEAD").isOpen()).toBe(true);
    expect(DealStage.parse("CLOSED_WON").isTerminal()).toBe(true);
    expectCode(() => DealStage.parse("DREAMING"), Codes.ILLEGAL_STAGE_TRANSITION);
  });

  it("treats call and meeting as conversations", () => {
    expect(ActivityType.call().isConversation()).toBe(true);
    expect(ActivityType.meeting().isConversation()).toBe(true);
    expect(ActivityType.note().isConversation()).toBe(false);
    expectCode(() => ActivityType.parse("SMS"), Codes.INVALID_ACTIVITY_TYPE);
  });
});

describe("Company, contact, user, activity invariants", () => {
  it("requires names", () => {
    expectCode(() => Company.create({ id: "1", name: " " }), Codes.INVALID_NAME);
    expectCode(
      () => Contact.create({ id: "1", companyId: "c", name: "" }),
      Codes.INVALID_NAME,
    );
    expectCode(
      () =>
        CrmUser.create({
          id: "u",
          email: "a@b.co",
          name: " ",
          role: Role.sales(),
        }),
      Codes.INVALID_NAME,
    );
  });

  it("requires a company on a contact", () => {
    expectCode(
      () => Contact.create({ id: "1", companyId: "", name: "Mina" }),
      Codes.CONTACT_COMPANY_REQUIRED,
    );
  });

  it("requires a body and a target on an activity", () => {
    expectCode(
      () =>
        Activity.create({
          id: "1",
          type: ActivityType.note(),
          body: " ",
          createdByUserId: "alex",
          dealId: "d",
          occurredAt: new Date(),
        }),
      Codes.ACTIVITY_BODY_REQUIRED,
    );
    expectCode(
      () =>
        Activity.create({
          id: "1",
          type: ActivityType.call(),
          body: "Hello",
          createdByUserId: "alex",
          occurredAt: new Date(),
        }),
      Codes.ACTIVITY_TARGET_REQUIRED,
    );
  });

  it("round-trips snapshots", () => {
    const company = Company.create({ id: "c", name: "Harbor", domain: "harbor.co" });
    expect(Company.rehydrate(company.toSnapshot()).name).toBe("Harbor");
    const user = CrmUser.create({
      id: "u",
      email: "a@b.co",
      name: "Alex",
      role: Role.manager(),
    });
    expect(CrmUser.rehydrate(user.toSnapshot()).role.isManager()).toBe(true);
  });
});
