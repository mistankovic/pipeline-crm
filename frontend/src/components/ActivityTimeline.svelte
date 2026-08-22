<script lang="ts">
  import { moment } from '../lib/format';
  import type { ActivityView } from '../lib/types';

  let { entries }: { entries: ActivityView[] } = $props();
</script>

<ol class="timeline" data-testid="timeline">
  {#each entries as entry (entry.id)}
    <li class="card">
      <p class="head">
        <strong data-testid="activity-type">{entry.type}</strong>
        <span class="muted">{entry.author.name} · {moment(entry.occurredAt)}</span>
      </p>
      <p>{entry.summary}</p>
    </li>
  {/each}
  {#if entries.length === 0}
    <li class="muted">Nothing has been recorded yet.</li>
  {/if}
</ol>

<style>
  .timeline { list-style: none; padding: 0; margin: 0; display: grid; gap: 0.5rem; }
  .head { display: flex; justify-content: space-between; gap: 1rem; font-size: 12px; margin: 0 0 0.25rem; }
  .timeline p:last-child { margin: 0; }
</style>
