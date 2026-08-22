import { EmailAddress } from "./email-address";
import { Codes, fail } from "./errors";
import { Role } from "./role";

export type UserSnapshot = {
  id: string;
  email: string;
  name: string;
  role: string;
};

export class CrmUser {
  private constructor(
    readonly id: string,
    readonly email: EmailAddress,
    readonly name: string,
    readonly role: Role,
  ) {}

  static create(input: {
    id: string;
    email: string;
    name: string;
    role: Role;
  }): CrmUser {
    const name = requireName(input.name);
    return new CrmUser(input.id, EmailAddress.parse(input.email), name, input.role);
  }

  static rehydrate(snapshot: UserSnapshot): CrmUser {
    return CrmUser.create({
      id: snapshot.id,
      email: snapshot.email,
      name: snapshot.name,
      role: Role.parse(snapshot.role),
    });
  }

  withRole(role: Role): CrmUser {
    return new CrmUser(this.id, this.email, this.name, role);
  }

  withName(name: string): CrmUser {
    return new CrmUser(this.id, this.email, requireName(name), this.role);
  }

  toSnapshot(): UserSnapshot {
    return {
      id: this.id,
      email: this.email.value,
      name: this.name,
      role: this.role.name,
    };
  }
}

function requireName(name: string): string {
  const trimmed = name.trim();
  if (trimmed.length < 1) fail(Codes.INVALID_NAME, "Name is required");
  return trimmed;
}
