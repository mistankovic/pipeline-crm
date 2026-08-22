import { Codes, fail } from "./errors";

export const ActivityKind = {
  NOTE: "NOTE",
  CALL: "CALL",
  MEETING: "MEETING",
} as const;

export type ActivityKind = (typeof ActivityKind)[keyof typeof ActivityKind];

export class ActivityType {
  private constructor(readonly kind: ActivityKind) {}

  static note(): ActivityType {
    return new ActivityType(ActivityKind.NOTE);
  }

  static call(): ActivityType {
    return new ActivityType(ActivityKind.CALL);
  }

  static meeting(): ActivityType {
    return new ActivityType(ActivityKind.MEETING);
  }

  static parse(raw: string): ActivityType {
    if (raw === ActivityKind.NOTE) return ActivityType.note();
    if (raw === ActivityKind.CALL) return ActivityType.call();
    if (raw === ActivityKind.MEETING) return ActivityType.meeting();
    fail(Codes.INVALID_ACTIVITY_TYPE, `Unknown activity type: ${raw}`);
  }

  /** Call and Meeting count toward the close-won conversation guard. */
  isConversation(): boolean {
    return this.kind === ActivityKind.CALL || this.kind === ActivityKind.MEETING;
  }

  equals(other: ActivityType): boolean {
    return this.kind === other.kind;
  }
}
