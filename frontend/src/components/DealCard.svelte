<script lang="ts">
  import { money, percentage } from '../lib/format';
  import type { DealView } from '../lib/types';

  let {
    deal,
    ondragstart,
    onopen
  }: {
    deal: DealView;
    ondragstart: (deal: DealView) => void;
    onopen: (deal: DealView) => void;
  } = $props();
</script>

<div
  class="card deal"
  class:immovable={!deal.youMayChangeThis}
  title={deal.youMayChangeThis ? '' : `Owned by ${deal.owner.name}. Only they or a manager may move it.`}
  draggable={deal.youMayChangeThis}
  data-testid="deal-card"
  data-deal-id={deal.id}
  data-stage={deal.stage}
  ondragstart={() => ondragstart(deal)}
  role="button"
  tabindex="0"
  onclick={() => onopen(deal)}
  onkeydown={(event) => event.key === 'Enter' && onopen(deal)}
>
  <h3>{deal.title}</h3>
  <p class="muted">{deal.company.name} · {deal.owner.name}</p>
  <p class="figures">
    <strong>{money(deal.value)}</strong>
    <span class="muted">{percentage(deal.probability)} → {money(deal.weightedValue)}</span>
  </p>
</div>

<style>
  .deal { margin-bottom: 0.5rem; cursor: grab; }
  /* The server said this caller may not change it. The card still opens; it just does not drag. */
  .deal.immovable { cursor: pointer; border-style: dashed; opacity: 0.75; }
  .deal h3 { font-size: 14px; }
  .deal p { margin: 0.15rem 0; font-size: 12px; }
  .figures { display: flex; justify-content: space-between; gap: 0.5rem; }
</style>
