import { Codes, fail } from "./errors";

export const StageName = {
  LEAD: "LEAD",
  QUALIFIED: "QUALIFIED",
  PROPOSAL: "PROPOSAL",
  NEGOTIATION: "NEGOTIATION",
  CLOSED_WON: "CLOSED_WON",
  CLOSED_LOST: "CLOSED_LOST",
} as const;

export type StageName = (typeof StageName)[keyof typeof StageName];

const OPEN_STAGES: readonly StageName[] = [
  StageName.LEAD,
  StageName.QUALIFIED,
  StageName.PROPOSAL,
  StageName.NEGOTIATION,
];

const ALL_STAGES: readonly StageName[] = [
  ...OPEN_STAGES,
  StageName.CLOSED_WON,
  StageName.CLOSED_LOST,
];

export class DealStage {
  private constructor(readonly name: StageName) {}

  static lead(): DealStage {
    return new DealStage(StageName.LEAD);
  }

  static parse(raw: string): DealStage {
    if (!isStageName(raw)) {
      fail(Codes.ILLEGAL_STAGE_TRANSITION, `Unknown stage: ${raw}`);
    }
    return new DealStage(raw);
  }

  static openPipeline(): readonly DealStage[] {
    return OPEN_STAGES.map((name) => new DealStage(name));
  }

  static all(): readonly DealStage[] {
    return ALL_STAGES.map((name) => new DealStage(name));
  }

  isOpen(): boolean {
    return (OPEN_STAGES as readonly string[]).includes(this.name);
  }

  isTerminal(): boolean {
    return !this.isOpen();
  }

  isWon(): boolean {
    return this.name === StageName.CLOSED_WON;
  }

  isLost(): boolean {
    return this.name === StageName.CLOSED_LOST;
  }

  equals(other: DealStage): boolean {
    return this.name === other.name;
  }

  /**
   * Open deals may move to any other stage. Terminal deals never leave.
   * Same-stage "moves" are rejected so callers notice no-ops.
   */
  mustAllowTransitionTo(target: DealStage): DealStage {
    if (this.isTerminal()) {
      fail(
        Codes.ILLEGAL_STAGE_TRANSITION,
        `Cannot leave terminal stage ${this.name}`,
      );
    }
    if (this.equals(target)) {
      fail(
        Codes.ILLEGAL_STAGE_TRANSITION,
        `Deal is already in ${this.name}`,
      );
    }
    return target;
  }
}

export function isStageName(raw: string): raw is StageName {
  return (ALL_STAGES as readonly string[]).includes(raw);
}
