<script lang="ts">
  import { ApiError } from '../lib/api';

  let { failure }: { failure: unknown } = $props();

  /**
   * What the user is told. For a refusal, the server's own words: it is the only thing that
   * knows why. For anything else, a generic line, because we do not know either.
   */
  const text = $derived(
    failure instanceof ApiError
      ? failure.message
      : failure
        ? 'Something went wrong talking to the server.'
        : ''
  );
</script>

{#if text}
  <p class="error" role="alert" data-testid="error">{text}</p>
{/if}
