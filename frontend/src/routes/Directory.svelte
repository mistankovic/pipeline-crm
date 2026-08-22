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

  // Which row is being edited, and the values being typed into it. One row at a time: an
  // edit form per row that is all live at once is a lot of state for very little.
  let editingCompany = $state<string | null>(null);
  let editedCompanyName = $state('');
  let editingContact = $state<string | null>(null);
  let editedContactName = $state('');
  let editedContactEmail = $state('');

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

  function startRenamingCompany(company: CompanyView) {
    editingCompany = company.id;
    editedCompanyName = company.name;
  }

  const saveCompanyName = (event: Event) => {
    event.preventDefault();
    const id = editingCompany!;
    return attempt(async () => {
      await session.api.renameCompany(id, editedCompanyName);
      editingCompany = null;
    });
  };

  function startCorrectingContact(contact: ContactView) {
    editingContact = contact.id;
    editedContactName = contact.name;
    editedContactEmail = contact.email;
  }

  const saveContact = (event: Event) => {
    event.preventDefault();
    const id = editingContact!;
    return attempt(async () => {
      await session.api.correctContact(id, editedContactName, editedContactEmail);
      editingContact = null;
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
          <li>
            {#if editingCompany === company.id}
              <form onsubmit={saveCompanyName} class="inline grow">
                <input bind:value={editedCompanyName} required data-testid="company-new-name" />
                <button class="primary" type="submit" data-testid="save-company">Save</button>
                <button type="button" onclick={() => (editingCompany = null)}>Cancel</button>
              </form>
            {:else}
              <span data-testid="company-row-name">{company.name}</span>
              <button class="link" onclick={() => startRenamingCompany(company)}
                      data-testid="rename-company">Rename</button>
            {/if}
          </li>
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
            {#if editingContact === contact.id}
              <form onsubmit={saveContact} class="stack grow">
                <input bind:value={editedContactName} required data-testid="contact-new-name" />
                <input bind:value={editedContactEmail} required data-testid="contact-new-email" />
                <div class="inline">
                  <button class="primary" type="submit" data-testid="save-contact">Save</button>
                  <button type="button" onclick={() => (editingContact = null)}>Cancel</button>
                </div>
              </form>
            {:else}
              <button class="link" onclick={() => show(contact.id)} data-testid="open-contact">
                {contact.name}
              </button>
              <span class="muted">{contact.email}</span>
              <button class="link" onclick={() => startCorrectingContact(contact)}
                      data-testid="edit-contact">Edit</button>
            {/if}
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
  .grow { flex: 1; }
  .link { border: none; background: none; padding: 0; color: var(--accent); cursor: pointer; text-align: left; }
</style>
