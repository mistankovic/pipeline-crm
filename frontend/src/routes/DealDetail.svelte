<script lang="ts">
  import ActivityTimeline from '../components/ActivityTimeline.svelte';
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import { money, percentage, stageLabel } from '../lib/format';
  import { session } from '../lib/session.svelte';
  import type { DealDetail } from '../lib/types';

  let { dealId, onback }: { dealId: string; onback: () => void } = $props();

  let detail = $state<DealDetail | null>(null);
  let failure = $state<unknown>(null);

  let summary = $state('');
  let type = $state('CALL');
  let newValue = $state(0);
  let newProbability = $state(0);

  async function load() {
    failure = null;
    try {
      detail = await session.api.deal(dealId);
      newValue = detail.deal.value.amount;
      newProbability = detail.deal.probability;
    } catch (refused) {
      failure = refused;
    }
  }

  async function attempt(action: () => Promise<unknown>) {
    failure = null;
    try {
      await action();
    } catch (refused) {
      failure = refused;
    }
    await load();
  }

  const move = (stage: string) => attempt(() => session.api.moveDeal(dealId, stage));

  const log = (event: Event) => {
    event.preventDefault();
    return attempt(async () => {
      await session.api.logActivity({ dealId, type, summary });
      summary = '';
    });
  };

  const reprice = () =>
    attempt(() => session.api.repriceDeal(dealId, newValue, detail!.deal.value.currency));

  const reweight = () => attempt(() => session.api.reweightDeal(dealId, newProbability));

  $effect(() => {
    void dealId;
    void load();
  });
</script>

<section>
  <button onclick={onback} data-testid="back">← Back to the board</button>
  <ErrorBanner {failure} />

  {#if detail}
    <header class="card head">
      <div>
        <h1 data-testid="deal-title">{detail.deal.title}</h1>
        <p class="muted">{detail.deal.company.name} · owned by {detail.deal.owner.name}</p>
      </div>
      <div class="figures">
        <p><strong data-testid="deal-value">{money(detail.deal.value)}</strong></p>
        <p class="muted">
          <span data-testid="deal-probability">{percentage(detail.deal.probability)}</span>
          → {money(detail.deal.weightedValue)}
        </p>
        <p><span class="stage" data-testid="deal-stage">{stageLabel(detail.deal.stage)}</span></p>
      </div>
    </header>

    <div class="panels">
      <div class="card">
        <h2>Move</h2>
        {#if detail.deal.allowedTransitions.length === 0}
          <p class="muted">This deal is closed. Nothing moves it now.</p>
        {:else}
          <div class="moves">
            {#each detail.deal.allowedTransitions as stage (stage)}
              <button onclick={() => move(stage)} data-testid={`move-to-${stage}`}>
                {stageLabel(stage)}
              </button>
            {/each}
          </div>
        {/if}

        <h2>Revise</h2>
        <label>
          <span>Value ({detail.deal.value.currency})</span>
          <input type="number" min="0" bind:value={newValue} data-testid="revise-value" />
        </label>
        <button onclick={reprice} data-testid="save-value">Save value</button>

        <label class="spaced">
          <span>Probability %</span>
          <input type="number" min="0" max="100" bind:value={newProbability} data-testid="revise-probability" />
        </label>
        <button onclick={reweight} data-testid="save-probability">Save probability</button>
      </div>

      <div class="card">
        <h2>Timeline</h2>
        <form onsubmit={log} class="log">
          <select bind:value={type} data-testid="activity-type-input">
            <option value="NOTE">Note</option>
            <option value="CALL">Call</option>
            <option value="MEETING">Meeting</option>
          </select>
          <input bind:value={summary} placeholder="What happened?" required data-testid="activity-summary" />
          <button class="primary" type="submit" data-testid="log-activity">Log</button>
        </form>
        <ActivityTimeline entries={detail.timeline} />
      </div>
    </div>
  {/if}
</section>

<style>
  .head { display: flex; justify-content: space-between; gap: 1rem; margin: 0.5rem 0; }
  .figures { text-align: right; }
  .figures p { margin: 0; }
  .stage {
    display: inline-block; margin-top: 0.25rem; padding: 0.1rem 0.5rem;
    background: var(--accent-soft); color: var(--accent); border-radius: 999px; font-size: 12px;
  }
  .panels { display: grid; grid-template-columns: minmax(240px, 1fr) 2fr; gap: 0.75rem; align-items: start; }
  .moves { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
  .log { display: flex; gap: 0.4rem; margin-bottom: 0.75rem; }
  .log select { width: 110px; }
  .spaced { margin-top: 1rem; }
  h2 { font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; color: var(--muted); }
</style>
