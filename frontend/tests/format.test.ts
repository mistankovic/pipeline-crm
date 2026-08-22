import { describe, expect, it } from 'vitest';
import { money, percentage, stageLabel } from '../src/lib/format';

describe('formatting', () => {
  it('renders money with its own currency', () => {
    expect(money({ amount: 1234, currency: 'EUR' })).toContain('1,234');
    expect(money({ amount: 1234, currency: 'EUR' })).toContain('€');
  });

  it('renders a different currency differently', () => {
    expect(money({ amount: 10, currency: 'USD' })).toContain('$');
  });

  it('renders a probability', () => {
    expect(percentage(45)).toBe('45%');
  });

  it('turns a stage constant into words', () => {
    expect(stageLabel('CLOSED_WON')).toBe('Closed Won');
    expect(stageLabel('LEAD')).toBe('Lead');
  });
});
