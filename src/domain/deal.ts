import { Activity } from "./activity";
import { Actor } from "./actor";
import { DealStage } from "./deal-stage";
import { Codes, fail } from "./errors";
import { type Currency, Money } from "./money";
import { Probability } from "./probability";

export type DealSnapshot = {
  id: string;
  title: string;
  companyId: string;
  ownerId: string;
  valueMinor: number;
  currency: Currency;
  probability: number;
  stage: string;
};

export class Deal {
  private constructor(
    readonly id: string,
    readonly title: string,
    readonly companyId: string,
    readonly ownerId: string,
    readonly value: Money,
    readonly probability: Probability,
    readonly stage: DealStage,
  ) {}

  static open(input: {
    id: string;
    title: string;
    companyId: string;
    ownerId: string;
    value: Money;
    probability: Probability;
    stage?: DealStage;
  }): Deal {
    const stage = input.stage ?? DealStage.lead();
    if (stage.isTerminal()) {
      fail(Codes.ILLEGAL_STAGE_TRANSITION, "New deals must start in an open stage");
    }
    return new Deal(
      input.id,
      requireTitle(input.title),
      requireCompany(input.companyId),
      input.ownerId,
      input.value,
      input.probability,
      stage,
    );
  }

  static rehydrate(snapshot: DealSnapshot): Deal {
    return new Deal(
      snapshot.id,
      requireTitle(snapshot.title),
      requireCompany(snapshot.companyId),
      snapshot.ownerId,
      Money.fromMinor(snapshot.valueMinor, snapshot.currency),
      Probability.of(snapshot.probability),
      DealStage.parse(snapshot.stage),
    );
  }

  changeStage(actor: Actor, target: DealStage, activities: readonly Activity[]): Deal {
    this.ensureActorMayMutate(actor);
    const next = this.stage.mustAllowTransitionTo(target);
    if (next.isWon()) return this.closeWon(activities);
    if (next.isLost()) return this.closeLost();
    return this.copy({ stage: next });
  }

  updateDetails(
    actor: Actor,
    patch: {
      title?: string;
      value?: Money;
      probability?: Probability;
      companyId?: string;
    },
  ): Deal {
    this.ensureActorMayMutate(actor);
    this.ensureOpen();
    return this.copy({
      title: patch.title !== undefined ? requireTitle(patch.title) : this.title,
      value: patch.value ?? this.value,
      probability: patch.probability ?? this.probability,
      companyId:
        patch.companyId !== undefined ? requireCompany(patch.companyId) : this.companyId,
    });
  }

  reassignOwner(actor: Actor, newOwnerId: string): Deal {
    if (!actor.isManager()) {
      fail(Codes.NOT_AUTHORIZED, "Only a manager may reassign a deal");
    }
    this.ensureOpen();
    return this.copy({ ownerId: newOwnerId });
  }

  isOpen(): boolean {
    return this.stage.isOpen();
  }

  weightedValue(): Money {
    if (!this.isOpen()) return Money.zero(this.value.currency);
    return this.value.weightedByPercent(this.probability.percent);
  }

  toSnapshot(): DealSnapshot {
    return {
      id: this.id,
      title: this.title,
      companyId: this.companyId,
      ownerId: this.ownerId,
      valueMinor: this.value.minorUnits,
      currency: this.value.currency,
      probability: this.probability.percent,
      stage: this.stage.name,
    };
  }

  private closeWon(activities: readonly Activity[]): Deal {
    if (!this.value.isPositive()) {
      fail(
        Codes.CLOSE_WON_VALUE,
        "A deal needs a positive value before it can be marked won",
      );
    }
    const hasConversation = activities.some((activity) =>
      activity.countsTowardCloseWon(this.id),
    );
    if (!hasConversation) {
      fail(
        Codes.CLOSE_WON_ACTIVITY,
        "A won deal needs at least one Call or Meeting on the deal",
      );
    }
    return this.copy({
      stage: DealStage.parse("CLOSED_WON"),
      probability: Probability.certain(),
    });
  }

  private closeLost(): Deal {
    return this.copy({
      stage: DealStage.parse("CLOSED_LOST"),
      probability: Probability.none(),
    });
  }

  private ensureActorMayMutate(actor: Actor): void {
    if (!actor.canChangeStageOf(this.ownerId)) {
      fail(Codes.NOT_AUTHORIZED, "Only the owner or a manager may change this deal");
    }
  }

  private ensureOpen(): void {
    if (this.stage.isTerminal()) {
      fail(Codes.TERMINAL_DEAL, "A closed deal cannot be modified");
    }
  }

  private copy(patch: Partial<{
    title: string;
    companyId: string;
    ownerId: string;
    value: Money;
    probability: Probability;
    stage: DealStage;
  }>): Deal {
    return new Deal(
      this.id,
      patch.title ?? this.title,
      patch.companyId ?? this.companyId,
      patch.ownerId ?? this.ownerId,
      patch.value ?? this.value,
      patch.probability ?? this.probability,
      patch.stage ?? this.stage,
    );
  }
}

function requireTitle(title: string): string {
  const trimmed = title.trim();
  if (trimmed.length < 1) fail(Codes.INVALID_TITLE, "Deal title is required");
  return trimmed;
}

function requireCompany(companyId: string): string {
  if (!companyId.trim()) {
    fail(Codes.DEAL_COMPANY_REQUIRED, "Deal must belong to a company");
  }
  return companyId;
}
