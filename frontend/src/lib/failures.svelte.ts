import { ApiError } from './api';
import { session } from './session.svelte';

/**
 * What went wrong, and keeping it on screen.
 *
 * Every screen used to do this by hand, and every screen got it wrong the same way: the
 * refusal was caught, assigned, and then wiped by the reload on the next line, because the
 * reload began by clearing the error. The user dragged a card they were not allowed to move,
 * it snapped back, and nothing was said. See docs/reviews/stage-6-review.md, finding F-6.1.
 *
 * `attempt` runs an action and a reload as one unit: the reload cannot clear a refusal the
 * action just recorded, because only `attempt` may clear it, and only before the action runs.
 */
export class Failures {
  private current = $state<unknown>(null);

  get failure(): unknown {
    return this.current;
  }

  /** Runs an action, then a reload, keeping whatever either of them refused. */
  async attempt(action: () => Promise<unknown>, reload?: () => Promise<unknown>): Promise<void> {
    this.current = null;
    try {
      await action();
    } catch (refused) {
      this.record(refused);
    }
    if (reload) {
      try {
        await reload();
      } catch (refused) {
        this.record(refused);
      }
    }
  }

  clear(): void {
    this.current = null;
  }

  /**
   * A 401 means the token is gone or expired, whatever the call was. Leaving the user
   * apparently signed in with a dead token gives them nothing to do but wonder. F-6.3.
   */
  private record(refused: unknown): void {
    this.current = refused;
    if (refused instanceof ApiError && refused.status === 401) {
      session.signOut();
    }
  }
}
