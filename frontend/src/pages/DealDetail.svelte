<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '../lib/api';
  import { STAGES, type Deal } from '../lib/types';

  let { dealId }: { dealId: string } = $props();
  let deal = $state<Deal | null>(null);
  let error = $state('');
  let title = $state('');
  let amount = $state('');
  let probability = $state(0);
  let activityType = $state('NOTE');
  let activityBody = $state('');

  async function load() {
    error = '';
    try {
      deal = await client.deal(dealId);
      title = deal.title;
      amount = deal.amount;
      probability = deal.probability;
    } catch (err) {
      error = err instanceof Error ? err.message : 'Failed to load deal';
    }
  }

  async function save(event: Event) {
    event.preventDefault();
    if (!deal) return;
    try {
      await client.updateDeal(deal.id, {
        title,
        amount,
        currency: deal.currency,
        probability
      });
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Update rejected';
    }
  }

  async function addActivity(event: Event) {
    event.preventDefault();
    try {
      await client.recordActivity({ type: activityType, body: activityBody, dealId });
      activityBody = '';
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Activity failed';
    }
  }

  async function move(stage: string) {
    try {
      await client.changeStage(dealId, stage);
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Stage change rejected by server';
    }
  }

  onMount(load);
</script>

<a href="#/board">← Pipeline</a>
{#if error}<p class="error">{error}</p>{/if}
{#if deal}
  <h2>{deal.title}</h2>
  <p class="muted">{deal.stage} · {deal.amount} {deal.currency} · {deal.probability}%</p>

  <form class="row card" onsubmit={save}>
    <input bind:value={title} />
    <input bind:value={amount} />
    <input type="number" min="0" max="100" bind:value={probability} />
    <button type="submit">Save deal</button>
  </form>

  <div class="row">
    {#each STAGES as stage}
      <button class="ghost" type="button" onclick={() => move(stage)}>{stage}</button>
    {/each}
  </div>

  <h3>Activity</h3>
  <form class="row card" onsubmit={addActivity}>
    <select bind:value={activityType}>
      <option>NOTE</option>
      <option>CALL</option>
      <option>MEETING</option>
    </select>
    <input bind:value={activityBody} placeholder="What happened?" required />
    <button type="submit">Record</button>
  </form>
  {#each deal.activities as activity}
    <article class="card">
      <strong>{activity.type}</strong>
      <span class="muted"> {activity.createdAt}</span>
      <p>{activity.body}</p>
    </article>
  {/each}
{/if}
