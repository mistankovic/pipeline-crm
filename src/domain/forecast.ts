import { Deal } from "./deal";
import { type Currency, Money } from "./money";

export type ForecastGroupBy = "owner" | "stage";

export type CurrencyTotal = {
  currency: Currency;
  weightedMinor: number;
  weightedAmount: number;
};

export type ForecastBucket = {
  key: string;
  totals: CurrencyTotal[];
};

export type Forecast = {
  groupBy: ForecastGroupBy;
  buckets: ForecastBucket[];
  grand: CurrencyTotal[];
};

export function forecastOpenDeals(
  deals: readonly Deal[],
  groupBy: ForecastGroupBy,
  ownerLabel: (ownerId: string) => string = (id) => id,
): Forecast {
  const open = deals.filter((deal) => deal.isOpen());
  const grouped = new Map<string, Deal[]>();
  for (const deal of open) {
    const key = groupBy === "owner" ? ownerLabel(deal.ownerId) : deal.stage.name;
    const list = grouped.get(key) ?? [];
    list.push(deal);
    grouped.set(key, list);
  }
  const buckets: ForecastBucket[] = [];
  for (const [key, bucketDeals] of grouped) {
    buckets.push({ key, totals: sumWeighted(bucketDeals) });
  }
  buckets.sort((a, b) => a.key.localeCompare(b.key));
  return { groupBy, buckets, grand: sumWeighted(open) };
}

function sumWeighted(deals: readonly Deal[]): CurrencyTotal[] {
  const byCurrency = new Map<Currency, Money>();
  for (const deal of deals) {
    const weighted = deal.weightedValue();
    const current = byCurrency.get(weighted.currency) ?? Money.zero(weighted.currency);
    byCurrency.set(weighted.currency, current.add(weighted));
  }
  return [...byCurrency.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([currency, money]) => ({
      currency,
      weightedMinor: money.minorUnits,
      weightedAmount: money.amount,
    }));
}
