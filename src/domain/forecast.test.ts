import { describe, expect, it } from "vitest";
import { Deal } from "./deal";
import { DealStage } from "./deal-stage";
import { forecastOpenDeals } from "./forecast";
import { Money } from "./money";
import { Probability } from "./probability";
import { Activity } from "./activity";
import { ActivityType } from "./activity-type";
import { Actor } from "./actor";
import { Role } from "./role";

function deal(input: {
  id: string;
  ownerId: string;
  amount: number;
  currency: "USD" | "EUR" | "GBP";
  probability: number;
  stage?: string;
}) {
  return Deal.open({
    id: input.id,
    title: input.id,
    companyId: "co",
    ownerId: input.ownerId,
    value: Money.of(input.amount, input.currency),
    probability: Probability.of(input.probability),
    stage: input.stage ? DealStage.parse(input.stage) : undefined,
  });
}

describe("forecastOpenDeals", () => {
  const alex = deal({
    id: "a",
    ownerId: "alex",
    amount: 100000,
    currency: "USD",
    probability: 50,
    stage: "PROPOSAL",
  });
  const blair = deal({
    id: "b",
    ownerId: "blair",
    amount: 40000,
    currency: "USD",
    probability: 25,
    stage: "LEAD",
  });
  const eu = deal({
    id: "c",
    ownerId: "alex",
    amount: 20000,
    currency: "EUR",
    probability: 10,
    stage: "QUALIFIED",
  });

  it("groups by owner without mixing currencies", () => {
    const forecast = forecastOpenDeals([alex, blair, eu], "owner", (id) => id);
    const alexBucket = forecast.buckets.find((bucket) => bucket.key === "alex");
    expect(alexBucket?.totals).toEqual([
      { currency: "EUR", weightedMinor: 200000, weightedAmount: 2000 },
      { currency: "USD", weightedMinor: 5000000, weightedAmount: 50000 },
    ]);
    const blairBucket = forecast.buckets.find((bucket) => bucket.key === "blair");
    expect(blairBucket?.totals[0]?.weightedAmount).toBe(10000);
  });

  it("groups by stage", () => {
    const forecast = forecastOpenDeals([alex, blair, eu], "stage");
    expect(forecast.buckets.map((bucket) => bucket.key)).toEqual([
      "LEAD",
      "PROPOSAL",
      "QUALIFIED",
    ]);
  });

  it("excludes closed deals from the grand total", () => {
    const meeting = Activity.create({
      id: "m",
      type: ActivityType.meeting(),
      body: "Factory tour",
      createdByUserId: "alex",
      dealId: "a",
      occurredAt: new Date(),
    });
    const won = alex.changeStage(
      new Actor("alex", Role.sales()),
      DealStage.parse("CLOSED_WON"),
      [meeting],
    );
    const forecast = forecastOpenDeals([won, blair, eu], "owner");
    expect(forecast.grand).toEqual([
      { currency: "EUR", weightedMinor: 200000, weightedAmount: 2000 },
      { currency: "USD", weightedMinor: 1000000, weightedAmount: 10000 },
    ]);
  });

  it("treats a 0% deal as a zero contribution", () => {
    const zero = deal({
      id: "z",
      ownerId: "blair",
      amount: 40000,
      currency: "USD",
      probability: 0,
    });
    const forecast = forecastOpenDeals([zero], "owner");
    expect(forecast.grand[0]?.weightedAmount).toBe(0);
  });
});
