<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '../lib/api';
  import { STAGES, type Deal, type User, type Company } from '../lib/types';

  let deals = $state<Deal[]>([]);
  let users = $state<User[]>([]);
  let companies = $state<Company[]>([]);
  let error = $state('');
  let ownerFilter = $state('');
  let title = $state('');
  let companyId = $state('');
  let ownerId = $state('');
  let amount = $state('1000.00');
  let probability = $state(25);

  const byId = (id: string) => users.find((u) => u.id === id)?.name ?? id.slice(0, 8);
  const companyName = (id: string) => companies.find((c) => c.id === id)?.name ?? id.slice(0, 8);

  async function load() {
    error = '';
    try {
      [deals, users, companies] = await Promise.all([
        client.deals(undefined, ownerFilter || undefined),
        client.users(),
        client.companies()
      ]);
      const me = localStorage.getItem('userId');
      if (!ownerId) ownerId = me && users.some((u) => u.id === me) ? me : users[0]?.id ?? '';
      if (!companyId && companies[0]) companyId = companies[0].id;
    } catch (err) {
      error = err instanceof Error ? err.message : 'Failed to load board';
    }
  }

  async function createDeal(event: Event) {
    event.preventDefault();
    error = '';
    try {
      await client.createDeal({
        companyId,
        ownerId,
        title,
        amount,
        currency: 'USD',
        probability
      });
      title = '';
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Create failed';
    }
  }

  function onDragStart(event: DragEvent, deal: Deal) {
    event.dataTransfer?.setData('text/plain', deal.id);
  }

  async function onDrop(event: DragEvent, stage: string) {
    event.preventDefault();
    const id = event.dataTransfer?.getData('text/plain');
    if (!id) return;
    error = '';
    try {
      await client.changeStage(id, stage);
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Stage change rejected by server';
    }
  }

  onMount(load);
</script>

<h2>Pipeline</h2>
{#if error}<p class="error">{error}</p>{/if}

<div class="row">
  <label>Owner filter
    <select bind:value={ownerFilter} onchange={load}>
      <option value="">All</option>
      {#each users as user}<option value={user.id}>{user.name}</option>{/each}
    </select>
  </label>
  <form class="row" onsubmit={createDeal}>
    <input placeholder="New deal title" bind:value={title} required />
    <select bind:value={companyId}>
      {#each companies as company}<option value={company.id}>{company.name}</option>{/each}
    </select>
    <select bind:value={ownerId}>
      {#each users as user}<option value={user.id}>{user.name}</option>{/each}
    </select>
    <input bind:value={amount} />
    <input type="number" min="0" max="100" bind:value={probability} />
    <button type="submit">Add deal</button>
  </form>
</div>

<div class="board">
  {#each STAGES as stage}
    <section
      class="column"
      role="group"
      aria-label={stage}
      ondragover={(e) => e.preventDefault()}
      ondrop={(e) => onDrop(e, stage)}
    >
      <h2>{stage.replaceAll('_', ' ')}</h2>
      {#each deals.filter((d) => d.stage === stage) as deal}
        <article
          class={`deal-card ${deal.stage}`}
          draggable="true"
          ondragstart={(e) => onDragStart(e, deal)}
        >
          <a href={`#/deals/${deal.id}`}><strong>{deal.title}</strong></a>
          <div class="muted">{deal.amount} {deal.currency} · {deal.probability}%</div>
          <div class="muted">{companyName(deal.companyId)} · {byId(deal.ownerId)}</div>
        </article>
      {/each}
    </section>
  {/each}
</div>
