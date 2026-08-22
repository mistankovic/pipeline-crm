<script lang="ts">
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import StageColumn from '../components/StageColumn.svelte';
  import NewDealForm from '../components/NewDealForm.svelte';
  import { columnsOf } from '../lib/board';
  import { Failures } from '../lib/failures.svelte';
  import { session } from '../lib/session.svelte';
  import type { DealView, UserView } from '../lib/types';

  let { onopen }: { onopen: (deal: DealView) => void } = $props();

  const failures = new Failures();

  let deals = $state<DealView[]>([]);
  let users = $state<UserView[]>([]);
  let ownerFilter = $state('');
  let dragged = $state<DealView | null>(null);
  let creating = $state(false);
  let loaded = $state(false);

  const columns = $derived(columnsOf(deals));

  async function reload() {
    deals = await session.api.deals(ownerFilter || undefined);
    loaded = true;
  }

  async function drop(stage: string) {
    const moving = dragged;
    dragged = null;
    if (!moving) {
      return;
    }
    await failures.attempt(() => session.api.moveDeal(moving.id, stage), reload);
  }

  $effect(() => {
    void ownerFilter;
    void failures.attempt(reload);
  });

  $effect(() => {
    void failures.attempt(async () => {
      users = await session.api.users();
    });
  });
</script>

<section>
  <header class="bar">
    <h1>Pipeline</h1>
    <label class="filter">
      <span>Owner</span>
      <select bind:value={ownerFilter} data-testid="owner-filter">
        <option value="">Everyone</option>
        {#each users as user (user.id)}
          <option value={user.id}>{user.name}</option>
        {/each}
      </select>
    </label>
    <button class="primary" onclick={() => (creating = !creating)} data-testid="new-deal">
      {creating ? 'Cancel' : 'New deal'}
    </button>
  </header>

  <ErrorBanner failure={failures.failure} />

  {#if creating}
    <NewDealForm
      {users}
      oncreated={async () => {
        creating = false;
        await failures.attempt(reload);
      }}
    />
  {/if}

  {#if !loaded}
    <p class="muted" data-testid="loading">Loading the pipeline…</p>
  {:else}
    <div class="board" ondragend={() => (dragged = null)} role="presentation">
      {#each columns as column (column.stage)}
        <StageColumn
          stage={column.stage}
          deals={column.deals}
          {dragged}
          ondragstart={(deal) => (dragged = deal)}
          ondrop={drop}
          {onopen}
        />
      {/each}
    </div>
  {/if}
</section>

<style>
  .bar { display: flex; align-items: end; gap: 1rem; margin-bottom: 0.5rem; }
  .bar h1 { flex: 1; }
  .filter { margin: 0; width: 180px; }
  .board { display: flex; gap: 0.6rem; overflow-x: auto; padding-bottom: 0.5rem; }
</style>
