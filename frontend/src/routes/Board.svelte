<script lang="ts">
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import StageColumn from '../components/StageColumn.svelte';
  import NewDealForm from '../components/NewDealForm.svelte';
  import { columnsOf } from '../lib/board';
  import { session } from '../lib/session.svelte';
  import type { DealView, UserView } from '../lib/types';

  let { onopen }: { onopen: (deal: DealView) => void } = $props();

  let deals = $state<DealView[]>([]);
  let users = $state<UserView[]>([]);
  let ownerFilter = $state('');
  let dragged = $state<DealView | null>(null);
  let failure = $state<unknown>(null);
  let creating = $state(false);

  const columns = $derived(columnsOf(deals));

  async function load() {
    failure = null;
    try {
      deals = await session.api.deals(ownerFilter || undefined);
    } catch (refused) {
      failure = refused;
    }
  }

  async function loadUsers() {
    try {
      users = await session.api.users();
    } catch (refused) {
      failure = refused;
    }
  }

  async function drop(stage: string) {
    const moving = dragged;
    dragged = null;
    if (!moving) {
      return;
    }
    failure = null;
    try {
      await session.api.moveDeal(moving.id, stage);
    } catch (refused) {
      failure = refused;
    }
    await load();
  }

  $effect(() => {
    void ownerFilter;
    void load();
  });

  $effect(() => {
    void loadUsers();
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

  <ErrorBanner {failure} />

  {#if creating}
    <NewDealForm
      {users}
      oncreated={async () => {
        creating = false;
        await load();
      }}
    />
  {/if}

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
</section>

<style>
  .bar { display: flex; align-items: end; gap: 1rem; margin-bottom: 0.5rem; }
  .bar h1 { flex: 1; }
  .filter { margin: 0; width: 180px; }
  .board { display: flex; gap: 0.6rem; overflow-x: auto; padding-bottom: 0.5rem; }
</style>
