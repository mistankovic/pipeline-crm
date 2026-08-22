import { Codes, fail } from "./errors";

export const RoleName = {
  SALES: "SALES",
  MANAGER: "MANAGER",
} as const;

export type RoleName = (typeof RoleName)[keyof typeof RoleName];

export class Role {
  private constructor(readonly name: RoleName) {}

  static sales(): Role {
    return new Role(RoleName.SALES);
  }

  static manager(): Role {
    return new Role(RoleName.MANAGER);
  }

  static parse(raw: string): Role {
    if (raw === RoleName.MANAGER) return Role.manager();
    if (raw === RoleName.SALES) return Role.sales();
    fail(Codes.INVALID_ROLE, `Unknown role: ${raw}`);
  }

  isManager(): boolean {
    return this.name === RoleName.MANAGER;
  }

  canManageOthersDeals(): boolean {
    return this.isManager();
  }

  equals(other: Role): boolean {
    return this.name === other.name;
  }
}
