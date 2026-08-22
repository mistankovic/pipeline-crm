import { EmailAddress } from "./email-address";
import { Codes, fail } from "./errors";

export type ContactSnapshot = {
  id: string;
  companyId: string;
  name: string;
  email: string | null;
  title: string | null;
};

export class Contact {
  private constructor(
    readonly id: string,
    readonly companyId: string,
    readonly name: string,
    readonly email: EmailAddress | null,
    readonly title: string | null,
  ) {}

  static create(input: {
    id: string;
    companyId: string;
    name: string;
    email?: string | null;
    title?: string | null;
  }): Contact {
    if (!input.companyId.trim()) {
      fail(Codes.CONTACT_COMPANY_REQUIRED, "Contact must belong to a company");
    }
    return new Contact(
      input.id,
      input.companyId,
      requireName(input.name),
      parseOptionalEmail(input.email),
      emptyToNull(input.title),
    );
  }

  static rehydrate(snapshot: ContactSnapshot): Contact {
    return Contact.create(snapshot);
  }

  update(input: {
    name?: string;
    email?: string | null;
    title?: string | null;
    companyId?: string;
  }): Contact {
    const companyId = input.companyId ?? this.companyId;
    if (!companyId.trim()) {
      fail(Codes.CONTACT_COMPANY_REQUIRED, "Contact must belong to a company");
    }
    return new Contact(
      this.id,
      companyId,
      input.name !== undefined ? requireName(input.name) : this.name,
      input.email !== undefined ? parseOptionalEmail(input.email) : this.email,
      input.title !== undefined ? emptyToNull(input.title) : this.title,
    );
  }

  toSnapshot(): ContactSnapshot {
    return {
      id: this.id,
      companyId: this.companyId,
      name: this.name,
      email: this.email?.value ?? null,
      title: this.title,
    };
  }
}

function requireName(name: string): string {
  const trimmed = name.trim();
  if (trimmed.length < 1) fail(Codes.INVALID_NAME, "Contact name is required");
  return trimmed;
}

function emptyToNull(value: string | null | undefined): string | null {
  if (value == null) return null;
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function parseOptionalEmail(raw: string | null | undefined): EmailAddress | null {
  if (raw == null || raw.trim().length === 0) return null;
  return EmailAddress.parse(raw);
}
