<script lang="ts">
  import { client } from '../lib/api';

  let email = $state('sales@pipelinecrm.demo');
  let password = $state('password');
  let error = $state('');

  async function submit(event: Event) {
    event.preventDefault();
    error = '';
    try {
      const result = await client.login(email, password);
      localStorage.setItem('token', result.token);
      localStorage.setItem('userId', result.userId);
      location.hash = '#/board';
    } catch (err) {
      error = err instanceof Error ? err.message : 'Login failed';
    }
  }
</script>

<section class="login card">
  <h1>PipelineCRM</h1>
  <p class="muted">Demo JWT login. Try sales@pipelinecrm.demo / password</p>
  <form onsubmit={submit}>
    <label>Email<br /><input type="email" bind:value={email} required /></label>
    <p></p>
    <label>Password<br /><input type="password" bind:value={password} required /></label>
    <p></p>
    <button type="submit">Sign in</button>
  </form>
  {#if error}<p class="error">{error}</p>{/if}
</section>
