import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../src/lib/api';
import { Failures } from '../src/lib/failures.svelte';
import { session } from '../src/lib/session.svelte';

/**
 * The defect this file exists for: every screen caught a refusal, assigned it, and then wiped
 * it with the reload on the next line. The user saw nothing at all. No unit test covered a
 * failing call, and eighteen green tests said the frontend was fine.
 * See docs/reviews/stage-6-review.md, finding F-6.1.
 */
describe('keeping a refusal on screen', () => {
  const refusal = (status: number) => new ApiError(status, 'StageChangeForbidden', 'not your deal');

  it('has nothing to report before anything is attempted', () => {
    expect(new Failures().failure).toBeNull();
  });

  it('reports a refusal from the action', async () => {
    const failures = new Failures();

    await failures.attempt(() => Promise.reject(refusal(403)));

    expect(failures.failure).toBeInstanceOf(ApiError);
  });

  it('KEEPS the refusal through the reload that follows it', async () => {
    const failures = new Failures();
    const reload = vi.fn(async () => undefined);

    await failures.attempt(() => Promise.reject(refusal(403)), reload);

    expect(reload).toHaveBeenCalled();
    expect(failures.failure, 'the reload must not wipe what the action refused').not.toBeNull();
  });

  it('reports a refusal from the reload itself', async () => {
    const failures = new Failures();

    await failures.attempt(
      () => Promise.resolve(),
      () => Promise.reject(refusal(500))
    );

    expect(failures.failure).toBeInstanceOf(ApiError);
  });

  it('clears the previous refusal when a new attempt starts', async () => {
    const failures = new Failures();
    await failures.attempt(() => Promise.reject(refusal(403)));

    await failures.attempt(() => Promise.resolve());

    expect(failures.failure).toBeNull();
  });

  it('reports something that is not an ApiError at all', async () => {
    const failures = new Failures();

    await failures.attempt(() => Promise.reject(new TypeError('Failed to fetch')));

    expect(failures.failure).toBeInstanceOf(TypeError);
  });
});

describe('a dead token', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('ends the session on any 401, so the user is not stuck signed in', async () => {
    const failures = new Failures();
    const signOut = vi.spyOn(session, 'signOut');

    await failures.attempt(() => Promise.reject(new ApiError(401, 'AuthenticationFailed', 'expired')));

    expect(signOut).toHaveBeenCalled();
    signOut.mockRestore();
  });

  it('leaves the session alone for any other refusal', async () => {
    const failures = new Failures();
    const signOut = vi.spyOn(session, 'signOut');

    await failures.attempt(() => Promise.reject(new ApiError(403, 'StageChangeForbidden', 'no')));

    expect(signOut).not.toHaveBeenCalled();
    signOut.mockRestore();
  });
});
