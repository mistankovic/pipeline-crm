import type { Activity } from "../../src/domain/activity";
import type { Company } from "../../src/domain/company";
import type { Contact } from "../../src/domain/contact";
import type { Deal } from "../../src/domain/deal";
import type { CrmUser } from "../../src/domain/user";
import type { Ports } from "../../src/application/ports";

function mapStore<T extends { id: string }>() {
  const data = new Map<string, T>();
  return {
    data,
    async findById(id: string): Promise<T | null> {
      return data.get(id) ?? null;
    },
    async save(entity: T): Promise<void> {
      data.set(entity.id, entity);
    },
    async list(): Promise<T[]> {
      return [...data.values()];
    },
    async count(): Promise<number> {
      return data.size;
    },
  };
}

export function createMemoryPorts(options?: {
  now?: Date;
  ids?: string[];
}): Ports {
  const users = mapStore<CrmUser>();
  const companies = mapStore<Company>();
  const contacts = mapStore<Contact>();
  const deals = mapStore<Deal>();
  const activities = mapStore<Activity>();
  const presetIds = options?.ids ? [...options.ids] : [];
  let seq = 0;
  return {
    users: {
      findById: users.findById,
      save: users.save,
      list: users.list,
      count: users.count,
    },
    companies: {
      findById: companies.findById,
      save: companies.save,
      list: companies.list,
    },
    contacts: {
      findById: contacts.findById,
      save: contacts.save,
      list: contacts.list,
    },
    deals: {
      findById: deals.findById,
      save: deals.save,
      list: deals.list,
    },
    activities: {
      findById: activities.findById,
      save: activities.save,
      list: activities.list,
      async listForDeal(dealId: string) {
        return (await activities.list()).filter((item) => item.dealId === dealId);
      },
      async listForContact(contactId: string) {
        return (await activities.list()).filter((item) => item.contactId === contactId);
      },
    },
    clock: {
      now: () => options?.now ?? new Date("2026-04-01T12:00:00.000Z"),
    },
    ids: {
      next: () => presetIds.shift() ?? `id-${++seq}`,
    },
  };
}
