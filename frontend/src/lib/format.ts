/** Turning server values into words on a screen. Pure, so it is tested directly. */

import type { MoneyView } from './types';

export function money(value: MoneyView): string {
  return new Intl.NumberFormat('en-GB', {
    style: 'currency',
    currency: value.currency,
    maximumFractionDigits: 0
  }).format(value.amount);
}

export function percentage(value: number): string {
  return `${value}%`;
}

export function stageLabel(stage: string): string {
  return stage
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

export function moment(isoTimestamp: string): string {
  return new Date(isoTimestamp).toLocaleString('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
}
