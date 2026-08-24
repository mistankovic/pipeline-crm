export const STAGES = [
  'LEAD',
  'QUALIFIED',
  'PROPOSAL',
  'NEGOTIATION',
  'CLOSED_WON',
  'CLOSED_LOST'
] as const;

export type Stage = (typeof STAGES)[number];

export interface Company {
  id: string;
  name: string;
}

export interface Contact {
  id: string;
  companyId: string;
  name: string;
  email: string | null;
}

export interface Activity {
  id: string;
  type: string;
  body: string;
  dealId: string | null;
  contactId: string | null;
  createdBy: string;
  createdAt: string;
}

export interface Deal {
  id: string;
  companyId: string;
  ownerId: string;
  title: string;
  amount: string;
  currency: string;
  probability: number;
  stage: Stage;
  activities: Activity[];
}

export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface ForecastRow {
  groupBy: string;
  key: string;
  amount: string;
  currency: string;
}

export interface LoginResult {
  token: string;
  userId: string;
}
