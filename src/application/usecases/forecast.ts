import {
  forecastOpenDeals,
  type Forecast,
  type ForecastGroupBy,
} from "../../domain/forecast";
import type { Ports } from "../ports";

export async function getForecast(
  ports: Ports,
  groupBy: ForecastGroupBy,
): Promise<Forecast> {
  const [deals, users] = await Promise.all([
    ports.deals.list(),
    ports.users.list(),
  ]);
  const names = new Map(users.map((user) => [user.id, user.name]));
  return forecastOpenDeals(deals, groupBy, (ownerId) => names.get(ownerId) ?? ownerId);
}
