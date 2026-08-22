<script lang="ts">
  import ErrorBanner from './ErrorBanner.svelte';
  import { session } from '../lib/session.svelte';
  import type { CompanyView, UserView } from '../lib/types';

  let { users, oncreated }: { users: UserView[]; oncreated: () => void } = $props();

  let companies = $state<CompanyView[]>([]);
  let title = $state('');
  let companyId = $state('');
  let ownerId = $state('');
  let value = $state(10000);
  let currency = $state('EUR');
  let probability = $state(50);
  let failure = $state<unknown>(null);

  $effect(() => {
    session.api
      .companies()
      .then((found) => {
        companies = found;
        companyId = companyId || (found[0]?.id ?? '');
      })
      .catch((refused) => (failure = refused));
  });

  async function submit(event: Event) {
    event.preventDefault();
    failure = null;
    try {
      await session.api.createDeal({
        title,
        companyId,
        ownerId: ownerId || undefined,
        value,
        currency,
        probability
      });
      title = '';
      oncreated();
    } catch (refused) {
      failure = refused;
    }
  }
</script>

<form class="card new-deal" onsubmit={submit} data-testid="new-deal-form">
  <ErrorBanner {failure} />
  <div class="fields">
    <label>
      <span>Title</span>
      <input bind:value={title} required data-testid="deal-title" />
    </label>
    <label>
      <span>Company</span>
      <select bind:value={companyId} required data-testid="deal-company">
        {#each companies as company (company.id)}
          <option value={company.id}>{company.name}</option>
        {/each}
      </select>
    </label>
    <label>
      <span>Owner</span>
      <select bind:value={ownerId} data-testid="deal-owner">
        <option value="">Me</option>
        {#each users as user (user.id)}
          <option value={user.id}>{user.name}</option>
        {/each}
      </select>
    </label>
    <label>
      <span>Value</span>
      <input type="number" min="0" step="1" bind:value data-testid="deal-value" />
    </label>
    <label>
      <span>Currency</span>
      <input bind:value={currency} maxlength="3" data-testid="deal-currency" />
    </label>
    <label>
      <span>Probability %</span>
      <input type="number" min="0" max="100" bind:value={probability} data-testid="deal-probability" />
    </label>
  </div>
  <button class="primary" type="submit" data-testid="create-deal">Create deal</button>
</form>

<style>
  .new-deal { margin-bottom: 0.75rem; }
  .fields { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 0 0.75rem; }
</style>
