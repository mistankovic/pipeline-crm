import type { Company, Contact, Deal, ForecastRow, LoginResult, User } from './types';

function token(): string | null {
  return localStorage.getItem('token');
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json');
  }
  const bearer = token();
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`);
  }
  const response = await fetch(path, { ...init, headers });
  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    if (!path.endsWith('/api/auth/login')) {
      location.hash = '#/login';
    }
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const payload = response.headers.get('content-type')?.includes('json')
    ? await response.json()
    : await response.text();
  if (!response.ok) {
    const message =
      typeof payload === 'object' && payload && 'message' in payload
        ? String(payload.message)
        : response.statusText;
    throw new Error(message);
  }
  return payload as T;
}

export const client = {
  login: (email: string, password: string) =>
    api<LoginResult>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  users: () => api<User[]>('/api/users'),
  companies: () => api<Company[]>('/api/companies'),
  createCompany: (name: string) =>
    api<{ id: string }>('/api/companies', { method: 'POST', body: JSON.stringify({ name }) }),
  updateCompany: (id: string, name: string) =>
    api<void>(`/api/companies/${id}`, { method: 'PUT', body: JSON.stringify({ name }) }),
  contacts: () => api<Contact[]>('/api/contacts'),
  createContact: (companyId: string, name: string, email: string | null) =>
    api<{ id: string }>('/api/contacts', {
      method: 'POST',
      body: JSON.stringify({ companyId, name, email })
    }),
  deals: (stage?: string, ownerId?: string) => {
    const query = new URLSearchParams();
    if (stage) query.set('stage', stage);
    if (ownerId) query.set('ownerId', ownerId);
    const suffix = query.toString() ? `?${query}` : '';
    return api<Deal[]>(`/api/deals${suffix}`);
  },
  deal: (id: string) => api<Deal>(`/api/deals/${id}`),
  createDeal: (body: {
    companyId: string;
    ownerId: string;
    title: string;
    amount: string;
    currency: string;
    probability: number;
  }) => api<{ id: string }>('/api/deals', { method: 'POST', body: JSON.stringify(body) }),
  updateDeal: (
    id: string,
    body: { title: string; amount: string; currency: string; probability: number }
  ) => api<void>(`/api/deals/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  changeStage: (id: string, stage: string) =>
    api<void>(`/api/deals/${id}/stage`, { method: 'POST', body: JSON.stringify({ stage }) }),
  recordActivity: (body: { type: string; body: string; dealId?: string; contactId?: string }) =>
    api<{ id: string }>('/api/activities', { method: 'POST', body: JSON.stringify(body) }),
  forecast: (groupBy: 'owner' | 'stage', currency = 'USD') =>
    api<ForecastRow[]>(`/api/forecast?groupBy=${groupBy}&currency=${currency}`)
};
