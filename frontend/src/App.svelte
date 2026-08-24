<script lang="ts">
  import Login from './pages/Login.svelte';
  import Board from './pages/Board.svelte';
  import DealDetail from './pages/DealDetail.svelte';
  import Companies from './pages/Companies.svelte';
  import Contacts from './pages/Contacts.svelte';
  import Forecast from './pages/Forecast.svelte';

  let hash = $state(location.hash || '#/login');

  function onHash() {
    hash = location.hash || '#/login';
    if (!localStorage.getItem('token') && !hash.startsWith('#/login')) {
      location.hash = '#/login';
    }
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    location.hash = '#/login';
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('hashchange', onHash);
    onHash();
  }

  let route = $derived(hash.replace(/^#/, '') || '/login');
  let dealId = $derived(route.startsWith('/deals/') ? route.slice('/deals/'.length) : '');
</script>

{#if route === '/login'}
  <Login />
{:else}
  <div class="shell">
    <nav>
      <h1>PipelineCRM</h1>
      <a class:active={route === '/board'} href="#/board">Pipeline</a>
      <a class:active={route === '/companies'} href="#/companies">Companies</a>
      <a class:active={route === '/contacts'} href="#/contacts">Contacts</a>
      <a class:active={route === '/forecast'} href="#/forecast">Forecast</a>
      <p></p>
      <button class="ghost" type="button" onclick={logout}>Log out</button>
    </nav>
    <main>
      {#if route === '/board'}
        <Board />
      {:else if dealId}
        <DealDetail {dealId} />
      {:else if route === '/companies'}
        <Companies />
      {:else if route === '/contacts'}
        <Contacts />
      {:else if route === '/forecast'}
        <Forecast />
      {:else}
        <Board />
      {/if}
    </main>
  </div>
{/if}
