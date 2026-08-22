import { createServerFn } from "@tanstack/react-start";
import { authMiddleware } from "@/lib/auth/middleware";
import { getSql } from "@/lib/db";
import { createSqlPorts } from "@/adapters/sql-ports";
import { DomainError } from "@/domain/errors";
import { NotFoundError } from "@/application/errors";
import { ensureProfile } from "@/application/usecases/profiles";
import { seedIfEmpty } from "@/application/usecases/bootstrap";
import { loadWorkspace } from "@/application/usecases/workspace";
import { createCompany, updateCompany } from "@/application/usecases/companies";
import { createContact, updateContact } from "@/application/usecases/contacts";
import {
  changeDealStage,
  createDeal,
  reassignDeal,
  updateDeal,
} from "@/application/usecases/deals";
import { createActivity } from "@/application/usecases/activities";
import type { CrmUser } from "@/domain/user";
import type { Company } from "@/domain/company";
import type { Contact } from "@/domain/contact";
import type { Deal } from "@/domain/deal";
import type { Activity } from "@/domain/activity";
import type { Forecast } from "@/domain/forecast";

export type UserDto = { id: string; email: string; name: string; role: string };
export type CompanyDto = {
  id: string;
  name: string;
  domain: string | null;
  notes: string | null;
};
export type ContactDto = {
  id: string;
  companyId: string;
  companyName: string;
  name: string;
  email: string | null;
  title: string | null;
};
export type DealDto = {
  id: string;
  title: string;
  companyId: string;
  companyName: string;
  ownerId: string;
  ownerName: string;
  amount: number;
  currency: string;
  probability: number;
  stage: string;
};
export type ActivityDto = {
  id: string;
  type: string;
  body: string;
  createdByUserId: string;
  createdByName: string;
  dealId: string | null;
  contactId: string | null;
  occurredAt: string;
};
export type ForecastDto = Forecast;
export type WorkspaceDto = {
  me: UserDto;
  users: UserDto[];
  companies: CompanyDto[];
  contacts: ContactDto[];
  deals: DealDto[];
  activities: ActivityDto[];
  forecastByOwner: ForecastDto;
  forecastByStage: ForecastDto;
};

type Result<T> = { ok: true; data: T } | { ok: false; code: string; message: string };

async function run<T>(fn: () => Promise<T>): Promise<Result<T>> {
  try {
    return { ok: true, data: await fn() };
  } catch (error) {
    if (error instanceof DomainError || error instanceof NotFoundError) {
      return { ok: false, code: error.code, message: error.message };
    }
    throw error;
  }
}

async function identity(userId: string): Promise<{ email: string; name: string }> {
  const sql = await getSql();
  const rows = await sql<{ email: string | null; name: string | null }>`
    select "email" as email, "name" as name from "user" where "id" = ${userId}
  `;
  const row = rows[0];
  const email = row?.email?.trim() || `${userId}@pipeline.local`;
  const name = row?.name?.trim() || email.split("@")[0] || "Operator";
  return { email, name };
}

function userDto(user: CrmUser): UserDto {
  return user.toSnapshot();
}

function companyDto(company: Company): CompanyDto {
  return company.toSnapshot();
}

function contactDto(contact: Contact, companies: Map<string, Company>): ContactDto {
  const snap = contact.toSnapshot();
  return {
    ...snap,
    companyName: companies.get(contact.companyId)?.name ?? "Unknown",
  };
}

function dealDto(
  deal: Deal,
  companies: Map<string, Company>,
  users: Map<string, CrmUser>,
): DealDto {
  const snap = deal.toSnapshot();
  return {
    id: snap.id,
    title: snap.title,
    companyId: snap.companyId,
    companyName: companies.get(deal.companyId)?.name ?? "Unknown",
    ownerId: snap.ownerId,
    ownerName: users.get(deal.ownerId)?.name ?? "Unknown",
    amount: deal.value.amount,
    currency: snap.currency,
    probability: snap.probability,
    stage: snap.stage,
  };
}

function activityDto(activity: Activity, users: Map<string, CrmUser>): ActivityDto {
  const snap = activity.toSnapshot();
  return {
    ...snap,
    createdByName: users.get(activity.createdByUserId)?.name ?? "Unknown",
  };
}

function mapWorkspace(input: {
  me: CrmUser;
  users: CrmUser[];
  companies: Company[];
  contacts: Contact[];
  deals: Deal[];
  activities: Activity[];
  forecastByOwner: Forecast;
  forecastByStage: Forecast;
}): WorkspaceDto {
  const companies = new Map(input.companies.map((item) => [item.id, item]));
  const users = new Map(input.users.map((item) => [item.id, item]));
  return {
    me: userDto(input.me),
    users: input.users.map(userDto),
    companies: input.companies.map(companyDto),
    contacts: input.contacts.map((item) => contactDto(item, companies)),
    deals: input.deals.map((item) => dealDto(item, companies, users)),
    activities: input.activities.map((item) => activityDto(item, users)),
    forecastByOwner: input.forecastByOwner,
    forecastByStage: input.forecastByStage,
  };
}

export const bootstrapCrm = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .handler(async ({ context }): Promise<Result<WorkspaceDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const who = await identity(context.userId);
      const me = await ensureProfile(ports, {
        id: context.userId,
        email: who.email,
        name: who.name,
      });
      await seedIfEmpty(ports, me.id);
      return mapWorkspace(await loadWorkspace(ports, me));
    });
  });

export const loadCrm = createServerFn({ method: "GET" })
  .middleware([authMiddleware])
  .handler(async ({ context }): Promise<Result<WorkspaceDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const who = await identity(context.userId);
      const me = await ensureProfile(ports, {
        id: context.userId,
        email: who.email,
        name: who.name,
      });
      return mapWorkspace(await loadWorkspace(ports, me));
    });
  });

export const saveCompany = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    (data: {
      id?: string;
      name: string;
      domain?: string | null;
      notes?: string | null;
    }) => data,
  )
  .handler(async ({ data }): Promise<Result<CompanyDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const company = data.id
        ? await updateCompany(ports, { ...data, id: data.id })
        : await createCompany(ports, data);
      return companyDto(company);
    });
  });

export const saveContact = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    (data: {
      id?: string;
      companyId: string;
      name: string;
      email?: string | null;
      title?: string | null;
    }) => data,
  )
  .handler(async ({ data }): Promise<Result<ContactDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const contact = data.id
        ? await updateContact(ports, { ...data, id: data.id })
        : await createContact(ports, data);
      const companies = new Map(
        (await ports.companies.list()).map((item) => [item.id, item]),
      );
      return contactDto(contact, companies);
    });
  });

export const saveDeal = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    (data: {
      id?: string;
      title: string;
      companyId: string;
      ownerId?: string;
      amount: number;
      currency: string;
      probability: number;
      stage?: string;
    }) => data,
  )
  .handler(async ({ context, data }): Promise<Result<DealDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const deal = data.id
        ? await updateDeal(ports, {
            actorId: context.userId,
            dealId: data.id,
            title: data.title,
            companyId: data.companyId,
            amount: data.amount,
            currency: data.currency,
            probability: data.probability,
          })
        : await createDeal(ports, { actorId: context.userId, ...data });
      const companies = new Map(
        (await ports.companies.list()).map((item) => [item.id, item]),
      );
      const users = new Map((await ports.users.list()).map((item) => [item.id, item]));
      return dealDto(deal, companies, users);
    });
  });

export const moveDeal = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator((data: { dealId: string; targetStage: string }) => data)
  .handler(async ({ context, data }): Promise<Result<DealDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const deal = await changeDealStage(ports, {
        actorId: context.userId,
        dealId: data.dealId,
        targetStage: data.targetStage,
      });
      const companies = new Map(
        (await ports.companies.list()).map((item) => [item.id, item]),
      );
      const users = new Map((await ports.users.list()).map((item) => [item.id, item]));
      return dealDto(deal, companies, users);
    });
  });

export const transferDeal = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator((data: { dealId: string; newOwnerId: string }) => data)
  .handler(async ({ context, data }): Promise<Result<DealDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const deal = await reassignDeal(ports, {
        actorId: context.userId,
        dealId: data.dealId,
        newOwnerId: data.newOwnerId,
      });
      const companies = new Map(
        (await ports.companies.list()).map((item) => [item.id, item]),
      );
      const users = new Map((await ports.users.list()).map((item) => [item.id, item]));
      return dealDto(deal, companies, users);
    });
  });

export const saveActivity = createServerFn({ method: "POST" })
  .middleware([authMiddleware])
  .validator(
    (data: {
      type: string;
      body: string;
      dealId?: string | null;
      contactId?: string | null;
    }) => data,
  )
  .handler(async ({ context, data }): Promise<Result<ActivityDto>> => {
    return run(async () => {
      const ports = createSqlPorts();
      const activity = await createActivity(ports, {
        actorId: context.userId,
        ...data,
      });
      const users = new Map((await ports.users.list()).map((item) => [item.id, item]));
      return activityDto(activity, users);
    });
  });
