/**
 * The shapes the API returns.
 *
 * These mirror the server's view records. They are deliberately dumb: no methods, no derived
 * values, no rules. Anything this file knew about how a deal behaves would be a copy of
 * something the server already decides.
 */

export type UserView = {
  id: string;
  email: string;
  name: string;
  role: 'SALES' | 'MANAGER';
};

export type CompanyView = { id: string; name: string };

export type ContactView = {
  id: string;
  companyId: string;
  name: string;
  email: string;
};

export type MoneyView = { amount: number; currency: string };

export type DealView = {
  id: string;
  title: string;
  company: CompanyView;
  owner: UserView;
  value: MoneyView;
  probability: number;
  stage: string;
  weightedValue: MoneyView;
  /**
   * Where this deal may legally move right now, decided by the server's domain.
   *
   * The browser never works this out for itself. Duplicating the stage machine here would
   * put a business rule in the frontend, and the two copies would disagree the first time
   * one changed. See docs/domain-decisions.md, decision D-11.
   */
  allowedTransitions: string[];
};

export type ActivityView = {
  id: string;
  type: 'NOTE' | 'CALL' | 'MEETING';
  summary: string;
  dealId: string | null;
  contactId: string | null;
  author: UserView;
  occurredAt: string;
};

export type DealDetail = { deal: DealView; timeline: ActivityView[] };

export type ForecastLineView = { group: string; label: string; weightedValue: MoneyView };

export type ForecastView = { dimension: string; lines: ForecastLineView[] };

export type AuthenticatedUser = { user: UserView; token: string; expiresAt: string };

/** What the API says when it refuses. */
export type ApiErrorBody = { error: string; message: string };
