import { Actor } from "../../domain/actor";
import { EmailAddress } from "../../domain/email-address";
import { Role } from "../../domain/role";
import { CrmUser } from "../../domain/user";
import { Codes, fail } from "../../domain/errors";
import type { Ports } from "../ports";
import { requireUser } from "../require";

export async function ensureProfile(
  ports: Ports,
  input: { id: string; email: string; name?: string | null },
): Promise<CrmUser> {
  const existing = await ports.users.findById(input.id);
  if (existing) return existing;
  const email = EmailAddress.parse(input.email);
  const name = (input.name ?? "").trim() || email.localPart();
  const count = await ports.users.count();
  const role = count === 0 ? Role.manager() : Role.sales();
  const user = CrmUser.create({ id: input.id, email: email.value, name, role });
  await ports.users.save(user);
  return user;
}

export async function assignRole(
  ports: Ports,
  input: { actorId: string; userId: string; role: string },
): Promise<CrmUser> {
  const actorUser = await requireUser(ports.users, input.actorId);
  if (!actorUser.role.isManager()) {
    fail(Codes.NOT_AUTHORIZED, "Only a manager may assign roles");
  }
  const target = await requireUser(ports.users, input.userId);
  const updated = target.withRole(Role.parse(input.role));
  await ports.users.save(updated);
  return updated;
}

export async function listUsers(ports: Ports): Promise<CrmUser[]> {
  return ports.users.list();
}

export async function actorFor(ports: Ports, userId: string): Promise<Actor> {
  const user = await requireUser(ports.users, userId);
  return new Actor(user.id, user.role);
}
