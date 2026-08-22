/**
 * Arranging deals into columns.
 *
 * The column order is a presentation choice — which order the columns appear on screen — and
 * lives here. Which moves are *legal* is not a presentation choice and does not: that comes
 * from each deal's `allowedTransitions`, computed by the server's domain.
 */

import type { DealView } from './types';

export const COLUMNS = [
  'LEAD',
  'QUALIFIED',
  'PROPOSAL',
  'NEGOTIATION',
  'CLOSED_WON',
  'CLOSED_LOST'
] as const;

export type Column = { stage: string; deals: DealView[] };

export function columnsOf(deals: DealView[]): Column[] {
  return COLUMNS.map((stage) => ({ stage, deals: deals.filter((deal) => deal.stage === stage) }));
}

/**
 * Whether a deal may be dropped on a column.
 *
 * This asks the deal what the server said. It does not know the pipeline, and it must not:
 * the browser greys out an illegal target as a courtesy, and the server refuses it as the rule.
 */
export function mayDropOn(deal: DealView | null, stage: string): boolean {
  return deal !== null && deal.allowedTransitions.includes(stage);
}

export function totalOf(deals: DealView[]): number {
  return deals.reduce((sum, deal) => sum + deal.weightedValue.amount, 0);
}
