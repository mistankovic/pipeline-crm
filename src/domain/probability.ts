import { Codes, fail } from "./errors";

export class Probability {
  private constructor(readonly percent: number) {}

  static of(percent: number): Probability {
    if (!Number.isInteger(percent) || percent < 0 || percent > 100) {
      fail(Codes.INVALID_PROBABILITY, "Probability must be an integer 0–100");
    }
    return new Probability(percent);
  }

  static certain(): Probability {
    return new Probability(100);
  }

  static none(): Probability {
    return new Probability(0);
  }

  asFraction(): number {
    return this.percent / 100;
  }

  equals(other: Probability): boolean {
    return this.percent === other.percent;
  }
}
