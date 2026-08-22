import { Activity } from "../../domain/activity";
import { ActivityType } from "../../domain/activity-type";
import type { Ports } from "../ports";
import { requireContact, requireDeal, requireUser } from "../require";

export async function createActivity(
  ports: Ports,
  input: {
    actorId: string;
    type: string;
    body: string;
    dealId?: string | null;
    contactId?: string | null;
  },
): Promise<Activity> {
  await requireUser(ports.users, input.actorId);
  if (input.dealId) await requireDeal(ports.deals, input.dealId);
  if (input.contactId) await requireContact(ports.contacts, input.contactId);
  const activity = Activity.create({
    id: ports.ids.next(),
    type: ActivityType.parse(input.type),
    body: input.body,
    createdByUserId: input.actorId,
    dealId: input.dealId,
    contactId: input.contactId,
    occurredAt: ports.clock.now(),
  });
  await ports.activities.save(activity);
  return activity;
}

export async function listDealActivities(ports: Ports, dealId: string) {
  await requireDeal(ports.deals, dealId);
  return ports.activities.listForDeal(dealId);
}

export async function listContactActivities(ports: Ports, contactId: string) {
  await requireContact(ports.contacts, contactId);
  return ports.activities.listForContact(contactId);
}
