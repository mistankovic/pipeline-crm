<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '../lib/api';
  import type { ForecastRow, User } from '../lib/types';

  let byOwner = $state<ForecastRow[]>([]);
  let byStage = $state<ForecastRow[]>([]);
  let users = $state<User[]>([]);
  let error = $state('');

  const nameOf = (id: string) => users.find((u) => u.id === id)?.name ?? id.slice(0, 8);

  onMount(async () => {
    try {
      [byOwner, byStage, users] = await Promise.all([
        client.forecast('owner'),
        client.forecast('stage'),
        client.users()
      ]);
    } catch (err) {
      error = err instanceof Error ? err.message : 'Forecast failed';
    }
  });
</script>

<h2>Forecast</h2>
<p class="muted">Weighted open pipeline (value × probability / 100). Computed on the server.</p>
{#if error}<p class="error">{error}</p>{/if}

<div class="row">
  <section class="card" style="flex:1">
    <h3>By owner</h3>
    <table>
      <thead><tr><th>Owner</th><th>Weighted</th></tr></thead>
      <tbody>
        {#each byOwner as row}
          <tr><td>{nameOf(row.key)}</td><td>{row.amount} {row.currency}</td></tr>
        {/each}
      </tbody>
    </table>
  </section>
  <section class="card" style="flex:1">
    <h3>By stage</h3>
    <table>
      <thead><tr><th>Stage</th><th>Weighted</th></tr></thead>
      <tbody>
        {#each byStage as row}
          <tr><td>{row.key}</td><td>{row.amount} {row.currency}</td></tr>
        {/each}
      </tbody>
    </table>
  </section>
</div>
