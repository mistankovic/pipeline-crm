import type { Ports } from "../ports";
import { getForecast } from "./forecast";
import { listCompanies } from "./companies";
import { listContacts } from "./contacts";
import { listDeals } from "./deals";
import { listUsers } from "./profiles";
import type { CrmUser } from "../../domain/user";

export async function loadWorkspace(ports: Ports, actor: CrmUser) {
  const [users, companies, contacts, deals, activities, forecastByOwner, forecastByStage] =
    await Promise.all([
      listUsers(ports),
      listCompanies(ports),
      listContacts(ports),
      listDeals(ports),
      ports.activities.list(),
      getForecast(ports, "owner"),
      getForecast(ports, "stage"),
    ]);
  return {
    me: actor,
    users,
    companies,
    contacts,
    deals,
    activities,
    forecastByOwner,
    forecastByStage,
  };
}
