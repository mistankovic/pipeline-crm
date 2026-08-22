<script lang="ts">
  import ErrorBanner from '../components/ErrorBanner.svelte';
  import { session } from '../lib/session.svelte';

  /**
   * The one screen that keeps its own error state, on purpose.
   *
   * `Failures` exists because every other screen reloads after an action and used to wipe the
   * refusal doing it. This screen has nothing to reload. And a 401 here means "wrong password",
   * not "your session ended", so `Failures`' sign-out-on-401 would be answering a different
   * question. Two reasons, both about meaning rather than convenience.
   */
  let email = $state('sam@pipelinecrm.demo');
  let password = $state('');
  let failure = $state<unknown>(null);
  let busy = $state(false);

  async function submit(event: Event) {
    event.preventDefault();
    failure = null;
    busy = true;
    try {
      await session.signIn(email, password);
    } catch (refused) {
      failure = refused;
    } finally {
      busy = false;
    }
  }
</script>

<main class="signin">
  <form class="card" onsubmit={submit}>
    <h1>PipelineCRM</h1>
    <p class="muted">Sign in to see the pipeline.</p>

    <ErrorBanner {failure} />

    <label>
      <span>Email</span>
      <input name="email" type="email" bind:value={email} required data-testid="email" />
    </label>
    <label>
      <span>Password</span>
      <input name="password" type="password" bind:value={password} required data-testid="password" />
    </label>

    <button class="primary" type="submit" disabled={busy} data-testid="sign-in">
      {busy ? 'Signing in…' : 'Sign in'}
    </button>
  </form>
</main>

<style>
  .signin {
    display: grid;
    place-items: center;
    min-height: 100vh;
    padding: 1rem;
  }
  form {
    width: min(360px, 100%);
  }
</style>
