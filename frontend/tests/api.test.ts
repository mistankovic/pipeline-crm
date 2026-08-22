import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from '../src/lib/api';

type Spy = ReturnType<typeof vi.fn>;

function respondWith(status: number, body: unknown): Spy {
  return vi.fn(async () =>
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' }
    })
  );
}

/** The one call the spy recorded. Fails loudly rather than reading undefined. */
function onlyCall(fetcher: Spy): { url: string; headers: Record<string, string> } {
  expect(fetcher.mock.calls).toHaveLength(1);
  const [url, init] = fetcher.mock.calls[0] as [string, RequestInit];
  return { url, headers: (init.headers ?? {}) as Record<string, string> };
}

describe('the api client', () => {
  it('sends no authorization header before sign-in', async () => {
    const fetcher = respondWith(200, { token: 't' });
    await new Api(null, fetcher as unknown as typeof fetch).signIn('sam@example.com', 'pw');

    expect(onlyCall(fetcher).headers.Authorization).toBeUndefined();
  });

  it('sends the bearer token once there is one', async () => {
    const fetcher = respondWith(200, []);
    await new Api('abc', fetcher as unknown as typeof fetch).deals();

    expect(onlyCall(fetcher).headers.Authorization).toBe('Bearer abc');
  });

  it('narrows the board to one owner when asked', async () => {
    const fetcher = respondWith(200, []);
    await new Api('abc', fetcher as unknown as typeof fetch).deals('owner-1');

    expect(onlyCall(fetcher).url).toBe('/api/deals?ownerId=owner-1');
  });

  it('turns a refusal into an ApiError carrying the servers own words', async () => {
    const fetcher = respondWith(409, {
      error: 'WinRequiresValueAndEngagement',
      message: 'no call or meeting has been logged against it'
    });

    await expect(
      new Api('abc', fetcher as unknown as typeof fetch).moveDeal('d', 'CLOSED_WON')
    ).rejects.toSatisfy(
      (failure: ApiError) =>
        failure.status === 409 &&
        failure.code === 'WinRequiresValueAndEngagement' &&
        failure.message === 'no call or meeting has been logged against it'
    );
  });

  it('still reports a refusal whose body is not json', async () => {
    const fetcher = vi.fn(async () => new Response('<html>gateway</html>', { status: 502 }));

    await expect(
      new Api('abc', fetcher as unknown as typeof fetch).deals()
    ).rejects.toThrow('the server answered 502');
  });

  it('does not invent a body for a 204', async () => {
    const fetcher = vi.fn(async () => new Response(null, { status: 204 }));

    await expect(
      new Api('abc', fetcher as unknown as typeof fetch).deals()
    ).resolves.toBeUndefined();
  });
});
