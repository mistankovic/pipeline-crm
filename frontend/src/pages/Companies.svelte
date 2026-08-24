<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '../lib/api';
  import type { Company } from '../lib/types';

  let companies = $state<Company[]>([]);
  let name = $state('');
  let error = $state('');

  async function load() {
    companies = await client.companies();
  }

  async function create(event: Event) {
    event.preventDefault();
    error = '';
    try {
      await client.createCompany(name);
      name = '';
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Create failed';
    }
  }

  async function rename(company: Company) {
    const next = prompt('New name', company.name);
    if (!next) return;
    await client.updateCompany(company.id, next);
    await load();
  }

  onMount(load);
</script>

<h2>Companies</h2>
{#if error}<p class="error">{error}</p>{/if}
<form class="row" onsubmit={create}>
  <input bind:value={name} placeholder="Company name" required />
  <button type="submit">Create</button>
</form>
<table>
  <thead><tr><th>Name</th><th></th></tr></thead>
  <tbody>
    {#each companies as company}
      <tr>
        <td>{company.name}</td>
        <td><button class="ghost" type="button" onclick={() => rename(company)}>Rename</button></td>
      </tr>
    {/each}
  </tbody>
</table>
