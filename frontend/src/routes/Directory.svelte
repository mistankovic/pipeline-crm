<script lang="ts">
  import ActivityTimeline from '../components/ActivityTimeline.svelte';
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import { Failures } from '../lib/failures.svelte';
  import { session } from '../lib/session.svelte';
  import type { ActivityView, CompanyView, ContactView } from '../lib/types';

  let companies = $state<CompanyView[]>([]);
  let contacts = $state<ContactView[]>([]);
  let filter = $state('');
  const failures = new Failures();

  let companyName = $state('');
  let contactName = $state('');
  let contactEmail = $state('');
  let contactCompany = $state('');

  let openContact = $state<string | null>(null);
  let timeline = $state<ActivityView[]>([]);
  let note = $state('');

  async function reload() {
    companies = await session.api.companies();
    contacts = await session.api.contacts(filter || undefined);
    contactCompany = contactCompany || (companies[0]?.id ?? '');
  }

  const attempt = (action: () => Promise<unknown>) => failures.attempt(action, reload);

  const addCompany = (event: Event) => {
    event.preventDefault();
    return attempt(async () => {
      await session.api.createCompany(companyName);
      companyName = '';
    });
  };

  const addContact = (event: Event) => {
    event.preventDefault();
    return attempt(async () => {
      await session.api.createContact(contactCompany, contactName, contactEmail);
      contactName = '';
      contactEmail = '';
    });
  };

  async function show(contactId: string) {
    openContact = contactId;
    await failures.attempt(async () => {
      timeline = await session.api.contactTimeline(contactId);
    });
  }

  const addNote = (event: Event) => {
    event.preventDefault();
    const contactId = openContact;
    return attempt(async () => {
      await session.api.logActivity({ contactId: contactId!, type: 'NOTE', summary: note });
      note = '';
      await show(contactId!);
    });
  };

  $effect(() => {
    void filter;
    void failures.attempt(reload);
  });
</script>

<section>
  <h1>Companies and contacts</h1>
  <ErrorBanner failure={failures.failure} />

  <div class="panels">
    <div class="card">
      <h2>Companies</h2>
      <form onsubmit={addCompany} class="inline">
        <input bind:value={companyName} placeholder="New company" required data-testid="company-name" />
        <button class="primary" type="submit" data-testid="add-company">Add</button>
      </form>
      <ul data-testid="company-list">
        {#each companies as company (company.id)}
          <li>{company.name}</li>
        {/each}
      </ul>
    </div>

    <div class="card">
      <h2>Contacts</h2>
      <form onsubmit={addContact} class="stack">
        <select bind:value={contactCompany} required data-testid="contact-company">
          {#each companies as company (company.id)}
            <option value={company.id}>{company.name}</option>
          {/each}
        </select>
        <input bind:value={contactName} placeholder="Name" required data-testid="contact-name" />
        <input bind:value={contactEmail} placeholder="Email" required data-testid="contact-email" />
        <button class="primary" type="submit" data-testid="add-contact">Add contact</button>
      </form>

      <label class="spaced">
        <span>Filter by company</span>
        <select bind:value={filter} data-testid="contact-filter">
          <option value="">All companies</option>
          {#each companies as company (company.id)}
            <option value={company.id}>{company.name}</option>
          {/each}
        </select>
      </label>

      <ul data-testid="contact-list">
        {#each contacts as contact (contact.id)}
          <li>
            <button class="link" onclick={() => show(contact.id)} data-testid="open-contact">
              {contact.name}
            </button>
            <span class="muted">{contact.email}</span>
          </li>
        {/each}
      </ul>
    </div>

    {#if openContact}
      <div class="card">
        <h2>Contact timeline</h2>
        <form onsubmit={addNote} class="inline">
          <input bind:value={note} placeholder="Note" required data-testid="contact-note" />
          <button class="primary" type="submit" data-testid="add-note">Log note</button>
        </form>
        <ActivityTimeline entries={timeline} />
      </div>
    {/if}
  </div>
</section>

<style>
  .panels { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 0.75rem; align-items: start; }
  h2 { font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; color: var(--muted); }
  ul { list-style: none; padding: 0; margin: 0.5rem 0 0; }
  li { padding: 0.3rem 0; border-top: 1px solid var(--line); display: flex; justify-content: space-between; gap: 0.5rem; }
  .inline { display: flex; gap: 0.4rem; }
  .stack { display: grid; gap: 0.4rem; }
  .spaced { margin-top: 0.75rem; }
  .link { border: none; background: none; padding: 0; color: var(--accent); cursor: pointer; text-align: left; }
</style>
