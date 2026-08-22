<script lang="ts">
  import Board from './routes/Board.svelte';
  import DealDetail from './routes/DealDetail.svelte';
  import Directory from './routes/Directory.svelte';
  import Forecast from './routes/Forecast.svelte';
  import Login from './routes/Login.svelte';
  import { session } from './lib/session.svelte';
  import type { DealView } from './lib/types';

  type Screen = { name: 'board' } | { name: 'deal'; id: string } | { name: 'directory' } | { name: 'forecast' };

  let screen = $state<Screen>({ name: 'board' });

  const open = (deal: DealView) => (screen = { name: 'deal', id: deal.id });
  const board = () => (screen = { name: 'board' });
</script>

{#if !session.signedIn}
  <Login />
{:else}
  <nav>
    <strong>PipelineCRM</strong>
    <button class:active={screen.name === 'board' || screen.name === 'deal'} onclick={board} data-testid="nav-board">
      Board
    </button>
    <button
      class:active={screen.name === 'directory'}
      onclick={() => (screen = { name: 'directory' })}
      data-testid="nav-directory"
    >
      Companies
    </button>
    <button
      class:active={screen.name === 'forecast'}
      onclick={() => (screen = { name: 'forecast' })}
      data-testid="nav-forecast"
    >
      Forecast
    </button>
    <span class="spacer"></span>
    <span class="muted" data-testid="signed-in-as">{session.user?.name} ({session.user?.role})</span>
    <button onclick={() => session.signOut()} data-testid="sign-out">Sign out</button>
  </nav>

  <main>
    {#if screen.name === 'board'}
      <Board onopen={open} />
    {:else if screen.name === 'deal'}
      <DealDetail dealId={screen.id} onback={board} />
    {:else if screen.name === 'directory'}
      <Directory />
    {:else}
      <Forecast />
    {/if}
  </main>
{/if}

<style>
  nav {
    display: flex; align-items: center; gap: 0.5rem;
    padding: 0.6rem 1rem; background: var(--paper); border-bottom: 1px solid var(--line);
  }
  nav .spacer { flex: 1; }
  nav button.active { background: var(--accent-soft); border-color: var(--accent); color: var(--accent); }
  main { padding: 1rem; max-width: 1400px; margin: 0 auto; }
</style>
