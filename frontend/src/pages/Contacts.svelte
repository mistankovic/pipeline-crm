<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '../lib/api';
  import type { Company, Contact } from '../lib/types';

  let contacts = $state<Contact[]>([]);
  let companies = $state<Company[]>([]);
  let name = $state('');
  let email = $state('');
  let companyId = $state('');
  let error = $state('');

  async function load() {
    [contacts, companies] = await Promise.all([client.contacts(), client.companies()]);
    if (!companyId && companies[0]) companyId = companies[0].id;
  }

  async function create(event: Event) {
    event.preventDefault();
    error = '';
    try {
      await client.createContact(companyId, name, email || null);
      name = '';
      email = '';
      await load();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Create failed';
    }
  }

  onMount(load);
</script>

<h2>Contacts</h2>
{#if error}<p class="error">{error}</p>{/if}
<form class="row" onsubmit={create}>
  <select bind:value={companyId}>
    {#each companies as company}<option value={company.id}>{company.name}</option>{/each}
  </select>
  <input bind:value={name} placeholder="Name" required />
  <input bind:value={email} placeholder="Email (optional)" />
  <button type="submit">Create</button>
</form>
<table>
  <thead><tr><th>Name</th><th>Email</th></tr></thead>
  <tbody>
    {#each contacts as contact}
      <tr>
        <td>{contact.name}</td>
        <td>{contact.email ?? ''}</td>
      </tr>
    {/each}
  </tbody>
</table>
