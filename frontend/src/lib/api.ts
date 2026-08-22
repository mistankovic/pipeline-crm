import type {
  ActivityView,
  ApiErrorBody,
  AuthenticatedUser,
  CompanyView,
  ContactView,
  DealDetail,
  DealView,
  ForecastView,
  UserView
} from './types';

/**
 * A refusal from the server, carrying the server's own vocabulary.
 *
 * The UI shows `message` and may branch on `error` — but it never decides *why* something was
 * refused. That decision arrived in the response.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function refusalFrom(response: Response): Promise<ApiError> {
  const fallback = `the server answered ${response.status}`;
  try {
    const body = (await response.json()) as ApiErrorBody;
    return new ApiError(response.status, body.error ?? 'Unknown', body.message ?? fallback);
  } catch {
    return new ApiError(response.status, 'Unknown', fallback);
  }
}

/**
 * Everything the browser can ask the server.
 *
 * One class, one place that knows about URLs, headers and status codes. A component that
 * wanted to call `fetch` itself would be a component that knows about HTTP.
 */
export class Api {
  /**
   * The fetcher is wrapped rather than stored as a bare reference to `fetch`.
   *
   * A browser's `fetch` must be called with `window` as its receiver; held in a field and
   * invoked as `this.fetcher(...)` it throws "Illegal invocation", and every request fails
   * with a network error rather than a refusal. Unit tests pass their own function and never
   * noticed. Running the real UI in a real browser did.
   */
  constructor(
    private readonly token: string | null = null,
    private readonly fetcher: typeof fetch = (input, init) => fetch(input, init)
  ) {}

  withToken(token: string): Api {
    return new Api(token, this.fetcher);
  }

  signIn(email: string, password: string): Promise<AuthenticatedUser> {
    return this.send('POST', '/api/sessions', { email, password });
  }

  users(): Promise<UserView[]> {
    return this.send('GET', '/api/users');
  }

  companies(): Promise<CompanyView[]> {
    return this.send('GET', '/api/companies');
  }

  createCompany(name: string): Promise<CompanyView> {
    return this.send('POST', '/api/companies', { name });
  }

  renameCompany(id: string, name: string): Promise<CompanyView> {
    return this.send('PATCH', `/api/companies/${id}`, { name });
  }

  contacts(companyId?: string): Promise<ContactView[]> {
    return this.send('GET', companyId ? `/api/contacts?companyId=${companyId}` : '/api/contacts');
  }

  createContact(companyId: string, name: string, email: string): Promise<ContactView> {
    return this.send('POST', '/api/contacts', { companyId, name, email });
  }

  correctContact(id: string, name: string, email: string): Promise<ContactView> {
    return this.send('PATCH', `/api/contacts/${id}`, { name, email });
  }

  contactTimeline(contactId: string): Promise<ActivityView[]> {
    return this.send('GET', `/api/contacts/${contactId}/activities`);
  }

  deals(ownerId?: string): Promise<DealView[]> {
    return this.send('GET', ownerId ? `/api/deals?ownerId=${ownerId}` : '/api/deals');
  }

  deal(id: string): Promise<DealDetail> {
    return this.send('GET', `/api/deals/${id}`);
  }

  createDeal(deal: NewDeal): Promise<DealView> {
    return this.send('POST', '/api/deals', deal);
  }

  moveDeal(id: string, stage: string): Promise<DealView> {
    return this.send('PATCH', `/api/deals/${id}/stage`, { stage });
  }

  repriceDeal(id: string, amount: number, currency: string): Promise<DealView> {
    return this.send('PATCH', `/api/deals/${id}/value`, { amount, currency });
  }

  reweightDeal(id: string, probability: number): Promise<DealView> {
    return this.send('PATCH', `/api/deals/${id}/probability`, { probability });
  }

  logActivity(activity: NewActivity): Promise<ActivityView> {
    return this.send('POST', '/api/activities', activity);
  }

  forecast(by: 'OWNER' | 'STAGE'): Promise<ForecastView> {
    return this.send('GET', `/api/forecast?by=${by}`);
  }

  private async send<T>(method: string, path: string, body?: unknown): Promise<T> {
    const response = await this.fetcher(path, {
      method,
      headers: this.headers(),
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) {
      throw await refusalFrom(response);
    }
    return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
  }

  private headers(): Record<string, string> {
    return this.token ? { ...JSON_HEADERS, Authorization: `Bearer ${this.token}` } : JSON_HEADERS;
  }
}

export type NewDeal = {
  title: string;
  companyId: string;
  ownerId?: string;
  value: number;
  currency: string;
  probability: number;
};

export type NewActivity = {
  dealId?: string;
  contactId?: string;
  type: string;
  summary: string;
};
