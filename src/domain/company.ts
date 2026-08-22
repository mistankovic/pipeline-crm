import { Codes, fail } from "./errors";

export type CompanySnapshot = {
  id: string;
  name: string;
  domain: string | null;
  notes: string | null;
};

export class Company {
  private constructor(
    readonly id: string,
    readonly name: string,
    readonly domain: string | null,
    readonly notes: string | null,
  ) {}

  static create(input: {
    id: string;
    name: string;
    domain?: string | null;
    notes?: string | null;
  }): Company {
    return new Company(
      input.id,
      requireName(input.name),
      emptyToNull(input.domain),
      emptyToNull(input.notes),
    );
  }

  static rehydrate(snapshot: CompanySnapshot): Company {
    return Company.create(snapshot);
  }

  rename(name: string): Company {
    return new Company(this.id, requireName(name), this.domain, this.notes);
  }

  update(input: { name?: string; domain?: string | null; notes?: string | null }): Company {
    return new Company(
      this.id,
      input.name !== undefined ? requireName(input.name) : this.name,
      input.domain !== undefined ? emptyToNull(input.domain) : this.domain,
      input.notes !== undefined ? emptyToNull(input.notes) : this.notes,
    );
  }

  toSnapshot(): CompanySnapshot {
    return {
      id: this.id,
      name: this.name,
      domain: this.domain,
      notes: this.notes,
    };
  }
}

function requireName(name: string): string {
  const trimmed = name.trim();
  if (trimmed.length < 1) fail(Codes.INVALID_NAME, "Company name is required");
  return trimmed;
}

function emptyToNull(value: string | null | undefined): string | null {
  if (value == null) return null;
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
