<script lang="ts">
  import DealCard from './DealCard.svelte';
  import { mayDropOn, totalOf } from '../lib/board';
  import { stageLabel } from '../lib/format';
  import type { DealView } from '../lib/types';

  let {
    stage,
    deals,
    dragged,
    ondragstart,
    ondrop,
    onopen
  }: {
    stage: string;
    deals: DealView[];
    dragged: DealView | null;
    ondragstart: (deal: DealView) => void;
    ondrop: (stage: string) => void;
    onopen: (deal: DealView) => void;
  } = $props();

  /**
   * Whether the card currently being dragged may land here.
   *
   * Read from the deal's own `allowedTransitions`, which the server computed. Greying out an
   * impossible column is a courtesy to the user; the server still refuses the move if a client
   * sends it anyway.
   */
  const welcoming = $derived(dragged !== null && mayDropOn(dragged, stage));
  const total = $derived(totalOf(deals));
</script>

<section
  class="column"
  class:welcoming
  class:unwelcoming={dragged !== null && !welcoming}
  data-testid="stage-column"
  data-stage={stage}
  ondragover={(event) => welcoming && event.preventDefault()}
  ondrop={() => welcoming && ondrop(stage)}
  aria-label={stageLabel(stage)}
>
  <header>
    <h2>{stageLabel(stage)}</h2>
    <span class="muted" data-testid="column-count">{deals.length}</span>
  </header>
  <p class="muted weighted">weighted {total.toLocaleString('en-GB')}</p>

  {#each deals as deal (deal.id)}
    <DealCard {deal} {ondragstart} {onopen} />
  {/each}

  {#if deals.length === 0}
    <p class="muted empty">nothing here</p>
  {/if}
</section>

<style>
  .column {
    background: #eaeef3;
    border-radius: 8px;
    padding: 0.6rem;
    min-width: 210px;
    flex: 1;
    border: 2px solid transparent;
  }
  .column.welcoming { border-color: var(--accent); background: var(--accent-soft); }
  .column.unwelcoming { opacity: 0.55; }
  header { display: flex; justify-content: space-between; align-items: baseline; }
  header h2 { font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }
  .weighted { font-size: 11px; margin: 0 0 0.5rem; }
  .empty { font-size: 12px; font-style: italic; }
</style>
