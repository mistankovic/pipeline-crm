import { createMemoryPorts } from "../support/memory-ports";
import type { Ports } from "../../src/application/ports";
import { Actor } from "../../src/domain/actor";
import { DomainError } from "../../src/domain/errors";
import { DealStage } from "../../src/domain/deal-stage";
import { Money, type Currency } from "../../src/domain/money";
import { Probability } from "../../src/domain/probability";
import { Role } from "../../src/domain/role";
import { createActivity } from "../../src/application/usecases/activities";
import { createCompany, listCompanies } from "../../src/application/usecases/companies";
import { createContact, listContacts } from "../../src/application/usecases/contacts";
import {
  changeDealStage,
  createDeal,
  getDeal,
  listDeals,
  reassignDeal,
} from "../../src/application/usecases/deals";
import { getForecast } from "../../src/application/usecases/forecast";
import { ensureProfile, listUsers } from "../../src/application/usecases/profiles";
import type { Forecast } from "../../src/domain/forecast";
import type { Deal } from "../../src/domain/deal";

export type World = {
  ports: Ports;
  lastError: DomainError | null;
  lastForecast: Forecast | null;
  focusDealTitle: string | null;
};

export function newWorld(): World {
  return {
    ports: createMemoryPorts(),
    lastError: null,
    lastForecast: null,
    focusDealTitle: null,
  };
}

function setupActor(ownerId: string): Actor {
  return new Actor(ownerId, Role.manager());
}

async function companyId(world: World, name: string): Promise<string> {
  const companies = await listCompanies(world.ports);
  const found = companies.find((company) => company.name === name);
  if (!found) throw new Error(`Unknown company ${name}`);
  return found.id;
}

async function dealByTitle(world: World, title: string): Promise<Deal> {
  const deals = await listDeals(world.ports);
  const found = deals.find((deal) => deal.title === title);
  if (!found) throw new Error(`Unknown deal ${title}`);
  return found;
}

async function focusedDeal(world: World): Promise<Deal> {
  if (!world.focusDealTitle) throw new Error("No focused deal");
  return dealByTitle(world, world.focusDealTitle);
}

async function capture(world: World, run: () => Promise<unknown>): Promise<void> {
  world.lastError = null;
  try {
    await run();
  } catch (error) {
    if (error instanceof DomainError) {
      world.lastError = error;
      return;
    }
    throw error;
  }
}

async function patchValue(
  world: World,
  deal: Deal,
  amount: number,
  currency: Currency,
  probability: number,
): Promise<void> {
  const patched = deal.updateDetails(setupActor(deal.ownerId), {
    value: Money.of(amount, currency),
    probability: Probability.of(probability),
  });
  await world.ports.deals.save(patched);
}

async function forceStage(world: World, deal: Deal, targetName: string): Promise<void> {
  const target = DealStage.parse(targetName);
  if (deal.stage.equals(target)) return;
  if (target.isWon()) {
    await createActivity(world.ports, {
      actorId: deal.ownerId,
      type: "MEETING",
      body: "Setup for given",
      dealId: deal.id,
    });
  }
  const activities = await world.ports.activities.listForDeal(deal.id);
  const patched = deal.changeStage(setupActor(deal.ownerId), target, activities);
  await world.ports.deals.save(patched);
}

type StepDef = {
  pattern: RegExp;
  run: (world: World, match: RegExpMatchArray) => Promise<void>;
};

export const steps: StepDef[] = [
  {
    pattern: /^a sales user "([^"]+)" exists$/,
    run: async (world, match) => {
      const handle = match[1] ?? "";
      const user = await ensureProfile(world.ports, {
        id: handle,
        email: `${handle}@pipeline.test`,
        name: handle,
      });
      await world.ports.users.save(user.withRole(Role.sales()));
    },
  },
  {
    pattern: /^a manager user "([^"]+)" exists$/,
    run: async (world, match) => {
      const handle = match[1] ?? "";
      const user = await ensureProfile(world.ports, {
        id: handle,
        email: `${handle}@pipeline.test`,
        name: handle,
      });
      await world.ports.users.save(user.withRole(Role.manager()));
    },
  },
  {
    pattern: /^a company "([^"]+)" exists$/,
    run: async (world, match) => {
      await createCompany(world.ports, { name: match[1] ?? "" });
    },
  },
  {
    pattern: /^a contact "([^"]+)" at company "([^"]+)"$/,
    run: async (world, match) => {
      await createContact(world.ports, {
        name: match[1] ?? "",
        companyId: await companyId(world, match[2] ?? ""),
      });
    },
  },
  {
    pattern: /^"([^"]+)" owns a deal "([^"]+)" at company "([^"]+)"$/,
    run: async (world, match) => {
      const owner = match[1] ?? "";
      const title = match[2] ?? "";
      await createDeal(world.ports, {
        actorId: owner,
        ownerId: owner,
        title,
        companyId: await companyId(world, match[3] ?? ""),
        amount: 1,
        currency: "USD",
        probability: 10,
      });
      world.focusDealTitle = title;
    },
  },
  {
    pattern: /^another deal "([^"]+)" owned by "([^"]+)" at company "([^"]+)"$/,
    run: async (world, match) => {
      await createDeal(world.ports, {
        actorId: match[2] ?? "",
        ownerId: match[2],
        title: match[1] ?? "",
        companyId: await companyId(world, match[3] ?? ""),
        amount: 1,
        currency: "USD",
        probability: 10,
      });
    },
  },
  {
    pattern: /^the deal is worth (\d+) (USD|EUR|GBP) at (\d+)% probability$/,
    run: async (world, match) => {
      await patchValue(
        world,
        await focusedDeal(world),
        Number(match[1]),
        match[2] as Currency,
        Number(match[3]),
      );
    },
  },
  {
    pattern: /^deal "([^"]+)" is worth (\d+) (USD|EUR|GBP) at (\d+)% probability$/,
    run: async (world, match) => {
      await patchValue(
        world,
        await dealByTitle(world, match[1] ?? ""),
        Number(match[2]),
        match[3] as Currency,
        Number(match[4]),
      );
    },
  },
  {
    pattern: /^the deal is in stage ([A-Z_]+)$/,
    run: async (world, match) => {
      await forceStage(world, await focusedDeal(world), match[1] ?? "");
    },
  },
  {
    pattern: /^deal "([^"]+)" is in stage ([A-Z_]+)$/,
    run: async (world, match) => {
      world.focusDealTitle = match[1] ?? "";
      await forceStage(world, await focusedDeal(world), match[2] ?? "");
    },
  },
  {
    pattern: /^the deal has a (meeting|call|note) activity "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await focusedDeal(world);
      await createActivity(world.ports, {
        actorId: deal.ownerId,
        type: (match[1] ?? "note").toUpperCase(),
        body: match[2] ?? "",
        dealId: deal.id,
      });
    },
  },
  {
    pattern: /^deal "([^"]+)" has a (meeting|call|note) activity "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[1] ?? "");
      await createActivity(world.ports, {
        actorId: deal.ownerId,
        type: (match[2] ?? "note").toUpperCase(),
        body: match[3] ?? "",
        dealId: deal.id,
      });
    },
  },
  {
    pattern: /^"([^"]+)" moves the deal to ([A-Z_]+)$/,
    run: async (world, match) => {
      const deal = await focusedDeal(world);
      await capture(world, () =>
        changeDealStage(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          targetStage: match[2] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" moves deal "([^"]+)" to ([A-Z_]+)$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[2] ?? "");
      await capture(world, () =>
        changeDealStage(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          targetStage: match[3] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" attempts to move the deal to ([A-Z_]+)$/,
    run: async (world, match) => {
      const deal = await focusedDeal(world);
      await capture(world, () =>
        changeDealStage(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          targetStage: match[2] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" attempts to move the deal "([^"]+)" to ([A-Z_]+)$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[2] ?? "");
      await capture(world, () =>
        changeDealStage(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          targetStage: match[3] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" reassigns the deal to "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await focusedDeal(world);
      await capture(world, () =>
        reassignDeal(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          newOwnerId: match[2] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" attempts to reassign the deal to "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await focusedDeal(world);
      await capture(world, () =>
        reassignDeal(world.ports, {
          actorId: match[1] ?? "",
          dealId: deal.id,
          newOwnerId: match[2] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" logs a (meeting|call|note) "([^"]*)" on deal "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[4] ?? "");
      await capture(world, () =>
        createActivity(world.ports, {
          actorId: match[1] ?? "",
          type: (match[2] ?? "").toUpperCase(),
          body: match[3] ?? "",
          dealId: deal.id,
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" logs a (meeting|call|note) "([^"]*)" on contact "([^"]+)"$/,
    run: async (world, match) => {
      const contacts = await listContacts(world.ports);
      const contact = contacts.find((item) => item.name === match[4]);
      if (!contact) throw new Error("missing contact");
      await capture(world, () =>
        createActivity(world.ports, {
          actorId: match[1] ?? "",
          type: (match[2] ?? "").toUpperCase(),
          body: match[3] ?? "",
          contactId: contact.id,
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" attempts to log a (meeting|call|note) "([^"]*)" with no target$/,
    run: async (world, match) => {
      await capture(world, () =>
        createActivity(world.ports, {
          actorId: match[1] ?? "",
          type: (match[2] ?? "").toUpperCase(),
          body: match[3] ?? "",
        }),
      );
    },
  },
  {
    pattern: /^"([^"]+)" attempts to log a note "([^"]*)" on deal "([^"]+)"$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[3] ?? "");
      await capture(world, () =>
        createActivity(world.ports, {
          actorId: match[1] ?? "",
          type: "NOTE",
          body: match[2] ?? "",
          dealId: deal.id,
        }),
      );
    },
  },
  {
    pattern: /^the forecast is grouped by (owner|stage)$/,
    run: async (world, match) => {
      world.lastForecast = await getForecast(
        world.ports,
        (match[1] ?? "owner") as "owner" | "stage",
      );
    },
  },
  {
    pattern: /^the deal stage is ([A-Z_]+)$/,
    run: async (world, match) => {
      const fresh = await getDeal(world.ports, (await focusedDeal(world)).id);
      if (fresh.stage.name !== match[1]) {
        throw new Error(`Expected stage ${match[1]} but was ${fresh.stage.name}`);
      }
    },
  },
  {
    pattern: /^the deal probability is (\d+)%$/,
    run: async (world, match) => {
      const fresh = await getDeal(world.ports, (await focusedDeal(world)).id);
      if (fresh.probability.percent !== Number(match[1])) {
        throw new Error(`Expected probability ${match[1]} but was ${fresh.probability.percent}`);
      }
    },
  },
  {
    pattern: /^the change is rejected with code "([^"]+)"$/,
    run: async (world, match) => {
      if (!world.lastError) throw new Error("Expected a domain error");
      if (world.lastError.code !== match[1]) {
        throw new Error(`Expected code ${match[1]} but was ${world.lastError.code}`);
      }
    },
  },
  {
    pattern: /^the deal owner is "([^"]+)"$/,
    run: async (world, match) => {
      const fresh = await getDeal(world.ports, (await focusedDeal(world)).id);
      if (fresh.ownerId !== match[1]) {
        throw new Error(`Expected owner ${match[1]} but was ${fresh.ownerId}`);
      }
    },
  },
  {
    pattern: /^owner "([^"]+)" has weighted (\d+) (USD|EUR|GBP)$/,
    run: async (world, match) => {
      assertBucket(world, match[1] ?? "", Number(match[2]), match[3] ?? "");
    },
  },
  {
    pattern: /^stage "([^"]+)" has weighted (\d+) (USD|EUR|GBP)$/,
    run: async (world, match) => {
      assertBucket(world, match[1] ?? "", Number(match[2]), match[3] ?? "");
    },
  },
  {
    pattern: /^owner "([^"]+)" does not have a (USD|EUR|GBP) total$/,
    run: async (world, match) => {
      const bucket = world.lastForecast?.buckets.find((item) => item.key === match[1]);
      const hit = bucket?.totals.find((total) => total.currency === match[2]);
      if (hit) throw new Error(`Did not expect ${match[2]} for ${match[1]}`);
    },
  },
  {
    pattern: /^the grand total includes (\d+) (USD|EUR|GBP)$/,
    run: async (world, match) => {
      const hit = world.lastForecast?.grand.find((total) => total.currency === match[2]);
      if (hit?.weightedAmount !== Number(match[1])) {
        throw new Error(`Grand ${match[2]} was ${hit?.weightedAmount}`);
      }
    },
  },
  {
    pattern: /^deal "([^"]+)" has (\d+) conversation activity$/,
    run: async (world, match) => {
      const deal = await dealByTitle(world, match[1] ?? "");
      const activities = await world.ports.activities.listForDeal(deal.id);
      const count = activities.filter((item) => item.countsTowardCloseWon(deal.id)).length;
      if (count !== Number(match[2])) throw new Error(`conversation count ${count}`);
    },
  },
  {
    pattern: /^contact "([^"]+)" has (\d+) activity$/,
    run: async (world, match) => {
      const contacts = await listContacts(world.ports);
      const contact = contacts.find((item) => item.name === match[1]);
      if (!contact) throw new Error("missing contact");
      const activities = await world.ports.activities.listForContact(contact.id);
      if (activities.length !== Number(match[2])) {
        throw new Error(`activity count ${activities.length}`);
      }
    },
  },
];

function assertBucket(world: World, key: string, amount: number, currency: string) {
  const bucket = world.lastForecast?.buckets.find((item) => item.key === key);
  if (!bucket) throw new Error(`Missing bucket ${key}`);
  const hit = bucket.totals.find((total) => total.currency === currency);
  if (hit?.weightedAmount !== amount) {
    throw new Error(`${key} ${currency} was ${hit?.weightedAmount}`);
  }
}

export async function runStep(world: World, text: string): Promise<void> {
  for (const step of steps) {
    const match = text.match(step.pattern);
    if (match) {
      await step.run(world, match);
      return;
    }
  }
  throw new Error(`No step definition for: ${text}`);
}

void listUsers;
