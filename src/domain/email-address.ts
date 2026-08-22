import { Codes, fail } from "./errors";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export class EmailAddress {
  private constructor(readonly value: string) {}

  static parse(raw: string): EmailAddress {
    const trimmed = raw.trim().toLowerCase();
    if (!EMAIL_PATTERN.test(trimmed)) {
      fail(Codes.INVALID_EMAIL, `Invalid email: ${raw}`);
    }
    return new EmailAddress(trimmed);
  }

  localPart(): string {
    return this.value.split("@")[0] ?? this.value;
  }

  equals(other: EmailAddress): boolean {
    return this.value === other.value;
  }
}
