import { clearAuthSession, getAccessToken } from './auth';

const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_BASE_URL?.trim() ?? '';

export const BACKEND_BASE_URL = configuredBackendBaseUrl.replace(/\/+$/, '');

type RequestOptions = RequestInit & {
  skipAuth?: boolean;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (!options.skipAuth) {
    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  const response = await fetch(`${BACKEND_BASE_URL}${path}`, {
    ...options,
    headers
  });

  const contentType = response.headers.get('content-type') || '';
  let payload: unknown;
  if (contentType.includes('application/json')) {
    payload = await response.json();
  } else {
    const text = await response.text();
    payload = { status: 'error', message: text || `请求失败 (HTTP ${response.status})` };
  }

  if (!response.ok || (typeof payload === 'object' && payload !== null && 'status' in payload && payload.status !== 'ok')) {
    if (response.status === 401) {
      clearAuthSession();
    }
    throw new Error(
      typeof payload === 'object' && payload !== null && 'message' in payload && typeof payload.message === 'string'
        ? payload.message
        : '请求失败'
    );
  }
  return (payload as { data: T }).data;
}
