<script lang="ts">
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import { money } from '../lib/format';
  import { Failures } from '../lib/failures.svelte';
  import { session } from '../lib/session.svelte';
  import type { ForecastView } from '../lib/types';

  let dimension = $state<'OWNER' | 'STAGE'>('OWNER');
  let forecast = $state<ForecastView | null>(null);
  const failures = new Failures();

  $effect(() => {
    const by = dimension;
    void failures.attempt(async () => {
      forecast = await session.api.forecast(by);
    });
  });
</script>

<section>
  <header class="bar">
    <h1>Forecast</h1>
    <label class="picker">
      <span>Grouped by</span>
      <select bind:value={dimension} data-testid="forecast-dimension">
        <option value="OWNER">Owner</option>
        <option value="STAGE">Stage</option>
      </select>
    </label>
  </header>

  <p class="muted">
    Weighted value of open deals only. Won and lost deals are history and never appear here, and
    currencies are never added together — two currencies mean two lines.
  </p>

  <ErrorBanner failure={failures.failure} />

  {#if forecast}
    <table class="card" data-testid="forecast-table">
      <thead>
        <!--
          The heading comes from the response, not from the dropdown, so it changes only when
          the new grouping has actually arrived. A QA script that waits for it cannot read the
          previous grouping's rows by mistake.
        -->
        <tr>
          <th data-testid="forecast-grouping">{forecast.dimension === 'OWNER' ? 'Owner' : 'Stage'}</th>
          <th>Weighted</th>
        </tr>
      </thead>
      <tbody>
        {#each forecast.lines as line (line.group + line.weightedValue.currency)}
          <tr data-testid="forecast-line">
            <td>{line.label}</td>
            <td class="figure">{money(line.weightedValue)}</td>
          </tr>
        {/each}
        {#if forecast.lines.length === 0}
          <tr><td colspan="2" class="muted">No open deals.</td></tr>
        {/if}
      </tbody>
    </table>
  {/if}
</section>

<style>
  .bar { display: flex; align-items: end; gap: 1rem; }
  .bar h1 { flex: 1; }
  .picker { margin: 0; width: 160px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: left; padding: 0.4rem 0.6rem; border-bottom: 1px solid var(--line); }
  th { font-size: 12px; text-transform: uppercase; color: var(--muted); }
  .figure { text-align: right; font-variant-numeric: tabular-nums; }
</style>
