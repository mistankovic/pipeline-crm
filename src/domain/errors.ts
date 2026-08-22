/** Domain failures are named codes, never HTTP statuses. */
export class DomainError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = "DomainError";
    this.code = code;
  }
}

export const Codes = {
  INVALID_EMAIL: "INVALID_EMAIL",
  INVALID_MONEY: "INVALID_MONEY",
  CURRENCY_MISMATCH: "CURRENCY_MISMATCH",
  INVALID_PROBABILITY: "INVALID_PROBABILITY",
  INVALID_NAME: "INVALID_NAME",
  INVALID_TITLE: "INVALID_TITLE",
  INVALID_ROLE: "INVALID_ROLE",
  INVALID_ACTIVITY_TYPE: "INVALID_ACTIVITY_TYPE",
  ACTIVITY_TARGET_REQUIRED: "ACTIVITY_TARGET_REQUIRED",
  ACTIVITY_BODY_REQUIRED: "ACTIVITY_BODY_REQUIRED",
  ILLEGAL_STAGE_TRANSITION: "ILLEGAL_STAGE_TRANSITION",
  TERMINAL_DEAL: "TERMINAL_DEAL",
  NOT_AUTHORIZED: "NOT_AUTHORIZED",
  CLOSE_WON_VALUE: "CLOSE_WON_VALUE",
  CLOSE_WON_ACTIVITY: "CLOSE_WON_ACTIVITY",
  DEAL_COMPANY_REQUIRED: "DEAL_COMPANY_REQUIRED",
  CONTACT_COMPANY_REQUIRED: "CONTACT_COMPANY_REQUIRED",
} as const;

export function fail(code: string, message: string): never {
  throw new DomainError(code, message);
}
