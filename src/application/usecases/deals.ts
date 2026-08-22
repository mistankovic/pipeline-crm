import { Deal } from "../../domain/deal";
import { DealStage } from "../../domain/deal-stage";
import { isCurrency, Money } from "../../domain/money";
import { Probability } from "../../domain/probability";
import { Codes, fail } from "../../domain/errors";
import type { Ports } from "../ports";
import { requireCompany, requireDeal, requireUser } from "../require";
import { actorFor } from "./profiles";

export async function createDeal(
  ports: Ports,
  input: {
    actorId: string;
    title: string;
    companyId: string;
    ownerId?: string;
    amount: number;
    currency: string;
    probability: number;
    stage?: string;
  },
): Promise<Deal> {
  const actor = await actorFor(ports, input.actorId);
  await requireCompany(ports.companies, input.companyId);
  const ownerId = input.ownerId ?? input.actorId;
  if (ownerId !== input.actorId && !actor.isManager()) {
    fail(Codes.NOT_AUTHORIZED, "Only a manager may create a deal for someone else");
  }
  await requireUser(ports.users, ownerId);
  if (!isCurrency(input.currency)) {
    fail(Codes.INVALID_MONEY, `Unknown currency: ${input.currency}`);
  }
  const deal = Deal.open({
    id: ports.ids.next(),
    title: input.title,
    companyId: input.companyId,
    ownerId,
    value: Money.of(input.amount, input.currency),
    probability: Probability.of(input.probability),
    stage: input.stage ? DealStage.parse(input.stage) : DealStage.lead(),
  });
  await ports.deals.save(deal);
  return deal;
}

export async function updateDeal(
  ports: Ports,
  input: {
    actorId: string;
    dealId: string;
    title?: string;
    companyId?: string;
    amount?: number;
    currency?: string;
    probability?: number;
  },
): Promise<Deal> {
  const actor = await actorFor(ports, input.actorId);
  const current = await requireDeal(ports.deals, input.dealId);
  if (input.companyId) await requireCompany(ports.companies, input.companyId);
  const currency = input.currency ?? current.value.currency;
  if (!isCurrency(currency)) {
    fail(Codes.INVALID_MONEY, `Unknown currency: ${currency}`);
  }
  const value =
    input.amount !== undefined ? Money.of(input.amount, currency) : current.value;
  const updated = current.updateDetails(actor, {
    title: input.title,
    companyId: input.companyId,
    value,
    probability:
      input.probability !== undefined
        ? Probability.of(input.probability)
        : undefined,
  });
  await ports.deals.save(updated);
  return updated;
}

export async function changeDealStage(
  ports: Ports,
  input: { actorId: string; dealId: string; targetStage: string },
): Promise<Deal> {
  const actor = await actorFor(ports, input.actorId);
  const current = await requireDeal(ports.deals, input.dealId);
  const activities = await ports.activities.listForDeal(current.id);
  const updated = current.changeStage(
    actor,
    DealStage.parse(input.targetStage),
    activities,
  );
  await ports.deals.save(updated);
  return updated;
}

export async function reassignDeal(
  ports: Ports,
  input: { actorId: string; dealId: string; newOwnerId: string },
): Promise<Deal> {
  const actor = await actorFor(ports, input.actorId);
  const current = await requireDeal(ports.deals, input.dealId);
  await requireUser(ports.users, input.newOwnerId);
  const updated = current.reassignOwner(actor, input.newOwnerId);
  await ports.deals.save(updated);
  return updated;
}

export async function getDeal(ports: Ports, id: string): Promise<Deal> {
  return requireDeal(ports.deals, id);
}

export async function listDeals(ports: Ports): Promise<Deal[]> {
  return ports.deals.list();
}
