import { Api } from './api';
import type { UserView } from './types';

const STORED_SESSION = 'pipelinecrm.session';

type StoredSession = { token: string; user: UserView; expiresAt: string };

/**
 * Who is signed in, and the API client that speaks for them.
 *
 * The token is kept in `localStorage` so a reload does not sign the user out. That is a demo
 * decision with a real cost — a script on the page could read it — and it is recorded as such
 * in CONSTITUTION.md section 6 rather than presented as production security.
 */
class Session {
  private state = $state<StoredSession | null>(restore());

  get user(): UserView | null {
    return this.state?.user ?? null;
  }

  get signedIn(): boolean {
    return this.state !== null;
  }

  get api(): Api {
    return new Api(this.state?.token ?? null);
  }

  async signIn(email: string, password: string): Promise<void> {
    const authenticated = await new Api().signIn(email, password);
    this.state = {
      token: authenticated.token,
      user: authenticated.user,
      expiresAt: authenticated.expiresAt
    };
    remember(this.state);
  }

  signOut(): void {
    this.state = null;
    forget();
  }
}

function restore(): StoredSession | null {
  try {
    const stored = localStorage.getItem(STORED_SESSION);
    return stored ? (JSON.parse(stored) as StoredSession) : null;
  } catch {
    return null;
  }
}

function remember(session: StoredSession): void {
  try {
    localStorage.setItem(STORED_SESSION, JSON.stringify(session));
  } catch {
    // A browser that refuses storage still works; the user signs in again after a reload.
  }
}

function forget(): void {
  try {
    localStorage.removeItem(STORED_SESSION);
  } catch {
    // Nothing to do: the in-memory session is already cleared.
  }
}

export const session = new Session();
