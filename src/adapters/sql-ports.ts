import { getSql } from "@/lib/db";
import type { Ports } from "../application/ports";
import { Activity } from "../domain/activity";
import { Company } from "../domain/company";
import { Contact } from "../domain/contact";
import { Deal } from "../domain/deal";
import { CrmUser } from "../domain/user";
import { systemClock, uuidIds } from "./system";

type Row = Record<string, unknown>;

function text(row: Row, key: string): string {
  return String(row[key] ?? "");
}

function optional(row: Row, key: string): string | null {
  const value = row[key];
  if (value == null) return null;
  const asText = String(value);
  return asText.length === 0 ? null : asText;
}

function int(row: Row, key: string): number {
  return Number(row[key]);
}

function occurredAt(row: Row): string {
  const value = row.occurred_at;
  if (value instanceof Date) return value.toISOString();
  return String(value);
}

export function createSqlPorts(): Ports {
  return {
    users: {
      async findById(id) {
        const sql = await getSql();
        const rows = await sql<Row>`select * from crm_users where id = ${id}`;
        const row = rows[0];
        return row
          ? CrmUser.rehydrate({
              id: text(row, "id"),
              email: text(row, "email"),
              name: text(row, "name"),
              role: text(row, "role"),
            })
          : null;
      },
      async save(user) {
        const sql = await getSql();
        const snap = user.toSnapshot();
        await sql`
          insert into crm_users (id, email, name, role)
          values (${snap.id}, ${snap.email}, ${snap.name}, ${snap.role})
          on conflict (id) do update set
            email = excluded.email,
            name = excluded.name,
            role = excluded.role
        `;
      },
      async list() {
        const sql = await getSql();
        const rows = await sql<Row>`select * from crm_users order by name`;
        return rows.map((row) =>
          CrmUser.rehydrate({
            id: text(row, "id"),
            email: text(row, "email"),
            name: text(row, "name"),
            role: text(row, "role"),
          }),
        );
      },
      async count() {
        const sql = await getSql();
        const rows = await sql<{ n: number }>`select count(*)::int as n from crm_users`;
        return Number(rows[0]?.n ?? 0);
      },
    },
    companies: {
      async findById(id) {
        const sql = await getSql();
        const rows = await sql<Row>`select * from companies where id = ${id}`;
        const row = rows[0];
        return row ? mapCompany(row) : null;
      },
      async save(company) {
        const sql = await getSql();
        const snap = company.toSnapshot();
        await sql`
          insert into companies (id, name, domain, notes)
          values (${snap.id}, ${snap.name}, ${snap.domain}, ${snap.notes})
          on conflict (id) do update set
            name = excluded.name,
            domain = excluded.domain,
            notes = excluded.notes
        `;
      },
      async list() {
        const sql = await getSql();
        const rows = await sql<Row>`select * from companies order by name`;
        return rows.map(mapCompany);
      },
    },
    contacts: {
      async findById(id) {
        const sql = await getSql();
        const rows = await sql<Row>`select * from contacts where id = ${id}`;
        const row = rows[0];
        return row ? mapContact(row) : null;
      },
      async save(contact) {
        const sql = await getSql();
        const snap = contact.toSnapshot();
        await sql`
          insert into contacts (id, company_id, name, email, title)
          values (${snap.id}, ${snap.companyId}, ${snap.name}, ${snap.email}, ${snap.title})
          on conflict (id) do update set
            company_id = excluded.company_id,
            name = excluded.name,
            email = excluded.email,
            title = excluded.title
        `;
      },
      async list() {
        const sql = await getSql();
        const rows = await sql<Row>`select * from contacts order by name`;
        return rows.map(mapContact);
      },
    },
    deals: {
      async findById(id) {
        const sql = await getSql();
        const rows = await sql<Row>`select * from deals where id = ${id}`;
        const row = rows[0];
        return row ? mapDeal(row) : null;
      },
      async save(deal) {
        const sql = await getSql();
        const snap = deal.toSnapshot();
        await sql`
          insert into deals (id, title, company_id, owner_id, value_minor, currency, probability, stage)
          values (
            ${snap.id}, ${snap.title}, ${snap.companyId}, ${snap.ownerId},
            ${snap.valueMinor}, ${snap.currency}, ${snap.probability}, ${snap.stage}
          )
          on conflict (id) do update set
            title = excluded.title,
            company_id = excluded.company_id,
            owner_id = excluded.owner_id,
            value_minor = excluded.value_minor,
            currency = excluded.currency,
            probability = excluded.probability,
            stage = excluded.stage
        `;
      },
      async list() {
        const sql = await getSql();
        const rows = await sql<Row>`select * from deals order by title`;
        return rows.map(mapDeal);
      },
    },
    activities: {
      async findById(id) {
        const sql = await getSql();
        const rows = await sql<Row>`select * from activities where id = ${id}`;
        const row = rows[0];
        return row ? mapActivity(row) : null;
      },
      async save(activity) {
        const sql = await getSql();
        const snap = activity.toSnapshot();
        await sql`
          insert into activities (id, type, body, created_by_user_id, deal_id, contact_id, occurred_at)
          values (
            ${snap.id}, ${snap.type}, ${snap.body}, ${snap.createdByUserId},
            ${snap.dealId}, ${snap.contactId}, ${snap.occurredAt}
          )
          on conflict (id) do update set
            type = excluded.type,
            body = excluded.body,
            created_by_user_id = excluded.created_by_user_id,
            deal_id = excluded.deal_id,
            contact_id = excluded.contact_id,
            occurred_at = excluded.occurred_at
        `;
      },
      async list() {
        const sql = await getSql();
        const rows = await sql<Row>`select * from activities order by occurred_at desc`;
        return rows.map(mapActivity);
      },
      async listForDeal(dealId) {
        const sql = await getSql();
        const rows = await sql<Row>`
          select * from activities where deal_id = ${dealId} order by occurred_at desc
        `;
        return rows.map(mapActivity);
      },
      async listForContact(contactId) {
        const sql = await getSql();
        const rows = await sql<Row>`
          select * from activities where contact_id = ${contactId} order by occurred_at desc
        `;
        return rows.map(mapActivity);
      },
    },
    clock: systemClock,
    ids: uuidIds,
  };
}

function mapCompany(row: Row): Company {
  return Company.rehydrate({
    id: text(row, "id"),
    name: text(row, "name"),
    domain: optional(row, "domain"),
    notes: optional(row, "notes"),
  });
}

function mapContact(row: Row): Contact {
  return Contact.rehydrate({
    id: text(row, "id"),
    companyId: text(row, "company_id"),
    name: text(row, "name"),
    email: optional(row, "email"),
    title: optional(row, "title"),
  });
}

function mapDeal(row: Row): Deal {
  return Deal.rehydrate({
    id: text(row, "id"),
    title: text(row, "title"),
    companyId: text(row, "company_id"),
    ownerId: text(row, "owner_id"),
    valueMinor: int(row, "value_minor"),
    currency: text(row, "currency") as Deal["value"]["currency"],
    probability: int(row, "probability"),
    stage: text(row, "stage"),
  });
}

function mapActivity(row: Row): Activity {
  return Activity.rehydrate({
    id: text(row, "id"),
    type: text(row, "type"),
    body: text(row, "body"),
    createdByUserId: text(row, "created_by_user_id"),
    dealId: optional(row, "deal_id"),
    contactId: optional(row, "contact_id"),
    occurredAt: occurredAt(row),
  });
}
