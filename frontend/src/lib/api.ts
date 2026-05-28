import { clearAuthSession, getAccessToken } from './auth';

export const BACKEND_BASE_URL = process.env.NEXT_PUBLIC_BACKEND_BASE_URL ?? 'http://localhost:8080';

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

  const payload = await response.json();
  if (!response.ok || payload.status !== 'ok') {
    if (response.status === 401) {
      clearAuthSession();
    }
    throw new Error(payload.message ?? '请求失败');
  }
  return payload.data as T;
}
