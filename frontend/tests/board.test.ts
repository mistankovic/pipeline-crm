import { describe, expect, it } from 'vitest';
import { COLUMNS, columnsOf, mayDropOn, totalOf } from '../src/lib/board';
import type { DealView } from '../src/lib/types';

function deal(overrides: Partial<DealView> = {}): DealView {
  return {
    id: crypto.randomUUID(),
    title: 'Acme renewal',
    company: { id: 'c', name: 'Acme' },
    owner: { id: 'u', email: 'sam@example.com', name: 'Sam', role: 'SALES' },
    value: { amount: 1000, currency: 'EUR' },
    probability: 50,
    stage: 'LEAD',
    weightedValue: { amount: 500, currency: 'EUR' },
    allowedTransitions: ['QUALIFIED', 'CLOSED_LOST'],
    ...overrides
  };
}

describe('the board', () => {
  it('shows a column for every stage, even the empty ones', () => {
    expect(columnsOf([]).map((column) => column.stage)).toEqual([...COLUMNS]);
  });

  it('puts each deal in the column for its stage', () => {
    const columns = columnsOf([deal({ stage: 'LEAD' }), deal({ stage: 'NEGOTIATION' })]);

    expect(columns.find((c) => c.stage === 'LEAD')!.deals).toHaveLength(1);
    expect(columns.find((c) => c.stage === 'NEGOTIATION')!.deals).toHaveLength(1);
    expect(columns.find((c) => c.stage === 'PROPOSAL')!.deals).toHaveLength(0);
  });

  it('adds up the weighted value of a column', () => {
    const total = totalOf([
      deal({ weightedValue: { amount: 500, currency: 'EUR' } }),
      deal({ weightedValue: { amount: 250, currency: 'EUR' } })
    ]);

    expect(total).toBe(750);
  });
});

describe('deciding where a card may be dropped', () => {
  it('asks the deal what the server said', () => {
    expect(mayDropOn(deal({ allowedTransitions: ['QUALIFIED'] }), 'QUALIFIED')).toBe(true);
  });

  it('refuses a column the server did not list', () => {
    expect(mayDropOn(deal({ allowedTransitions: ['QUALIFIED'] }), 'CLOSED_WON')).toBe(false);
  });

  it('refuses everything for a closed deal, because the server lists nothing', () => {
    const closed = deal({ stage: 'CLOSED_WON', allowedTransitions: [] });

    expect(COLUMNS.every((stage) => !mayDropOn(closed, stage))).toBe(true);
  });

  it('refuses when nothing is being dragged', () => {
    expect(mayDropOn(null, 'QUALIFIED')).toBe(false);
  });

  it('never consults the stage machine itself', () => {
    // A deal in LEAD whose server says it may be won. Nonsense in the real domain -- and
    // exactly the point: the browser has no opinion, so it obeys. If this test ever fails,
    // somebody has taught the frontend the pipeline, and there are now two copies of a rule.
    const strange = deal({ stage: 'LEAD', allowedTransitions: ['CLOSED_WON'] });

    expect(mayDropOn(strange, 'CLOSED_WON')).toBe(true);
  });
});
