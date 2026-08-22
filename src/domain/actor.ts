import { Role } from "./role";

/** The authenticated person attempting a command. */
export class Actor {
  constructor(
    readonly userId: string,
    readonly role: Role,
  ) {}

  canChangeStageOf(ownerId: string): boolean {
    return this.userId === ownerId || this.role.canManageOthersDeals();
  }

  isManager(): boolean {
    return this.role.isManager();
  }
}
