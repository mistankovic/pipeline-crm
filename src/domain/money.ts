import { Codes, fail } from "./errors";

export const Currencies = ["USD", "EUR", "GBP"] as const;
export type Currency = (typeof Currencies)[number];

export function isCurrency(raw: string): raw is Currency {
  return (Currencies as readonly string[]).includes(raw);
}

export class Money {
  private constructor(
    readonly minorUnits: number,
    readonly currency: Currency,
  ) {}

  static of(amount: number, currency: Currency): Money {
    if (!Number.isFinite(amount)) {
      fail(Codes.INVALID_MONEY, "Money amount must be finite");
    }
    if (amount < 0) {
      fail(Codes.INVALID_MONEY, "Money amount cannot be negative");
    }
    return new Money(Math.round(amount * 100), currency);
  }

  static fromMinor(minorUnits: number, currency: Currency): Money {
    if (!Number.isInteger(minorUnits) || minorUnits < 0) {
      fail(Codes.INVALID_MONEY, "Minor units must be a non-negative integer");
    }
    return new Money(minorUnits, currency);
  }

  static zero(currency: Currency): Money {
    return new Money(0, currency);
  }

  get amount(): number {
    return this.minorUnits / 100;
  }

  isPositive(): boolean {
    return this.minorUnits > 0;
  }

  isZero(): boolean {
    return this.minorUnits === 0;
  }

  add(other: Money): Money {
    this.assertSameCurrency(other);
    return new Money(this.minorUnits + other.minorUnits, this.currency);
  }

  weightedByPercent(percent: number): Money {
    return new Money(Math.round((this.minorUnits * percent) / 100), this.currency);
  }

  format(): string {
    return `${this.currency} ${this.amount.toLocaleString("en-US", {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    })}`;
  }

  private assertSameCurrency(other: Money): void {
    if (this.currency !== other.currency) {
      fail(
        Codes.CURRENCY_MISMATCH,
        `Cannot combine ${this.currency} with ${other.currency}`,
      );
    }
  }
}
