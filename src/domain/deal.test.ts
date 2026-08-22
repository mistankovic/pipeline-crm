import { describe, expect, it } from "vitest";
import { Activity } from "./activity";
import { ActivityType } from "./activity-type";
import { Actor } from "./actor";
import { Codes, DomainError } from "./errors";
import { Deal } from "./deal";
import { DealStage } from "./deal-stage";
import { Money } from "./money";
import { Probability } from "./probability";
import { Role } from "./role";

const owner = new Actor("alex", Role.sales());
const otherSales = new Actor("blair", Role.sales());
const manager = new Actor("casey", Role.manager());

function openDeal(overrides: Partial<{
  id: string;
  value: number;
  probability: number;
  stage: DealStage;
}> = {}) {
  return Deal.open({
    id: overrides.id ?? "deal-1",
    title: "Clinic rollout",
    companyId: "co-1",
    ownerId: "alex",
    value: Money.of(overrides.value ?? 120000, "EUR"),
    probability: Probability.of(overrides.probability ?? 40),
    stage: overrides.stage,
  });
}

function meeting(dealId = "deal-1") {
  return Activity.create({
    id: "act-1",
    type: ActivityType.meeting(),
    body: "Site visit",
    createdByUserId: "alex",
    dealId,
    occurredAt: new Date("2026-04-01T12:00:00Z"),
  });
}

function expectCode(run: () => unknown, code: string) {
  try {
    run();
    throw new Error("expected domain error");
  } catch (error) {
    expect(error).toBeInstanceOf(DomainError);
    expect((error as DomainError).code).toBe(code);
  }
}

describe("Deal stage changes", () => {
  it("lets the owner move between open stages without touching probability", () => {
    const next = openDeal().changeStage(owner, DealStage.parse("QUALIFIED"), []);
    expect(next.stage.name).toBe("QUALIFIED");
    expect(next.probability.percent).toBe(40);
  });

  it("rejects a no-op stage change", () => {
    expectCode(
      () => openDeal().changeStage(owner, DealStage.parse("LEAD"), []),
      Codes.ILLEGAL_STAGE_TRANSITION,
    );
  });

  it("wins a deal and forces probability to 100", () => {
    const next = openDeal().changeStage(owner, DealStage.parse("CLOSED_WON"), [
      meeting(),
    ]);
    expect(next.stage.name).toBe("CLOSED_WON");
    expect(next.probability.percent).toBe(100);
  });

  it("accepts a call as the conversation for a win", () => {
    const call = Activity.create({
      id: "act-2",
      type: ActivityType.call(),
      body: "Budget confirmation",
      createdByUserId: "alex",
      dealId: "deal-1",
      occurredAt: new Date("2026-04-01T12:00:00Z"),
    });
    const next = openDeal().changeStage(owner, DealStage.parse("CLOSED_WON"), [call]);
    expect(next.stage.isWon()).toBe(true);
  });

  it("rejects a win when only a note exists", () => {
    const note = Activity.create({
      id: "act-3",
      type: ActivityType.note(),
      body: "Internal recap",
      createdByUserId: "alex",
      dealId: "deal-1",
      occurredAt: new Date("2026-04-01T12:00:00Z"),
    });
    expectCode(
      () => openDeal().changeStage(owner, DealStage.parse("CLOSED_WON"), [note]),
      Codes.CLOSE_WON_ACTIVITY,
    );
  });

  it("rejects a win when the meeting belongs to another deal", () => {
    expectCode(
      () => openDeal().changeStage(owner, DealStage.parse("CLOSED_WON"), [
        meeting("other"),
      ]),
      Codes.CLOSE_WON_ACTIVITY,
    );
  });

  it("treats one cent as a positive value that can be won", () => {
    const deal = Deal.open({
      id: "deal-1",
      title: "Penny",
      companyId: "co-1",
      ownerId: "alex",
      value: Money.fromMinor(1, "USD"),
      probability: Probability.of(10),
    });
    const next = deal.changeStage(owner, DealStage.parse("CLOSED_WON"), [meeting()]);
    expect(next.stage.isWon()).toBe(true);
  });

  it("rejects a win when the value is not positive", () => {
    expectCode(
      () =>
        openDeal({ value: 0 }).changeStage(owner, DealStage.parse("CLOSED_WON"), [
          meeting(),
        ]),
      Codes.CLOSE_WON_VALUE,
    );
  });

  it("marks a deal lost and forces probability to 0", () => {
    const next = openDeal({ probability: 15 }).changeStage(
      owner,
      DealStage.parse("CLOSED_LOST"),
      [],
    );
    expect(next.stage.name).toBe("CLOSED_LOST");
    expect(next.probability.percent).toBe(0);
  });

  it("does not reopen a terminal deal", () => {
    const lost = openDeal().changeStage(owner, DealStage.parse("CLOSED_LOST"), []);
    expectCode(
      () => lost.changeStage(owner, DealStage.parse("LEAD"), []),
      Codes.ILLEGAL_STAGE_TRANSITION,
    );
  });
});

describe("Deal authorization", () => {
  it("rejects a stage change from a different sales user", () => {
    expectCode(
      () => openDeal().changeStage(otherSales, DealStage.parse("PROPOSAL"), []),
      Codes.NOT_AUTHORIZED,
    );
  });

  it("lets a manager change someone else's deal", () => {
    const next = openDeal().changeStage(manager, DealStage.parse("PROPOSAL"), []);
    expect(next.stage.name).toBe("PROPOSAL");
  });

  it("lets a manager win someone else's deal", () => {
    const next = openDeal().changeStage(manager, DealStage.parse("CLOSED_WON"), [
      meeting(),
    ]);
    expect(next.stage.isWon()).toBe(true);
  });

  it("lets only a manager reassign ownership", () => {
    expectCode(() => openDeal().reassignOwner(owner, "blair"), Codes.NOT_AUTHORIZED);
    const moved = openDeal().reassignOwner(manager, "blair");
    expect(moved.ownerId).toBe("blair");
  });

  it("refuses reassignment of a closed deal", () => {
    const lost = openDeal().changeStage(owner, DealStage.parse("CLOSED_LOST"), []);
    expectCode(() => lost.reassignOwner(manager, "blair"), Codes.TERMINAL_DEAL);
  });
});

describe("Deal construction", () => {
  it("rejects a blank title and missing company", () => {
    expectCode(
      () =>
        Deal.open({
          id: "x",
          title: "  ",
          companyId: "co",
          ownerId: "alex",
          value: Money.of(1, "USD"),
          probability: Probability.of(10),
        }),
      Codes.INVALID_TITLE,
    );
    expectCode(
      () =>
        Deal.open({
          id: "x",
          title: "Ok",
          companyId: " ",
          ownerId: "alex",
          value: Money.of(1, "USD"),
          probability: Probability.of(10),
        }),
      Codes.DEAL_COMPANY_REQUIRED,
    );
  });

  it("rejects opening a deal already closed", () => {
    expectCode(
      () => openDeal({ stage: DealStage.parse("CLOSED_WON") }),
      Codes.ILLEGAL_STAGE_TRANSITION,
    );
  });

  it("refuses edits on a closed deal", () => {
    const lost = openDeal().changeStage(owner, DealStage.parse("CLOSED_LOST"), []);
    expectCode(
      () => lost.updateDetails(owner, { title: "Nope" }),
      Codes.TERMINAL_DEAL,
    );
  });

  it("rehydrates from a snapshot", () => {
    const deal = openDeal();
    const copy = Deal.rehydrate(deal.toSnapshot());
    expect(copy.title).toBe(deal.title);
    expect(copy.value.amount).toBe(120000);
    expect(copy.stage.name).toBe("LEAD");
  });
});
