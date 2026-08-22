import { ActivityType } from "./activity-type";
import { Codes, fail } from "./errors";

export type ActivitySnapshot = {
  id: string;
  type: string;
  body: string;
  createdByUserId: string;
  dealId: string | null;
  contactId: string | null;
  occurredAt: string;
};

export class Activity {
  private constructor(
    readonly id: string,
    readonly type: ActivityType,
    readonly body: string,
    readonly createdByUserId: string,
    readonly dealId: string | null,
    readonly contactId: string | null,
    readonly occurredAt: Date,
  ) {}

  static create(input: {
    id: string;
    type: ActivityType;
    body: string;
    createdByUserId: string;
    dealId?: string | null;
    contactId?: string | null;
    occurredAt: Date;
  }): Activity {
    const body = input.body.trim();
    if (body.length < 1) {
      fail(Codes.ACTIVITY_BODY_REQUIRED, "Activity body is required");
    }
    const dealId = emptyToNull(input.dealId);
    const contactId = emptyToNull(input.contactId);
    if (!dealId && !contactId) {
      fail(
        Codes.ACTIVITY_TARGET_REQUIRED,
        "Activity must be linked to a deal or a contact",
      );
    }
    return new Activity(
      input.id,
      input.type,
      body,
      input.createdByUserId,
      dealId,
      contactId,
      input.occurredAt,
    );
  }

  static rehydrate(snapshot: ActivitySnapshot): Activity {
    return Activity.create({
      id: snapshot.id,
      type: ActivityType.parse(snapshot.type),
      body: snapshot.body,
      createdByUserId: snapshot.createdByUserId,
      dealId: snapshot.dealId,
      contactId: snapshot.contactId,
      occurredAt: new Date(snapshot.occurredAt),
    });
  }

  isOnDeal(dealId: string): boolean {
    return this.dealId === dealId;
  }

  countsTowardCloseWon(dealId: string): boolean {
    return this.isOnDeal(dealId) && this.type.isConversation();
  }

  toSnapshot(): ActivitySnapshot {
    return {
      id: this.id,
      type: this.type.kind,
      body: this.body,
      createdByUserId: this.createdByUserId,
      dealId: this.dealId,
      contactId: this.contactId,
      occurredAt: this.occurredAt.toISOString(),
    };
  }
}

function emptyToNull(value: string | null | undefined): string | null {
  if (value == null) return null;
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
