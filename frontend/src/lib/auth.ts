export type CurrentUser = {
  id: number;
  userCode: string;
  username: string;
  realName: string;
  status: string;
  roleCodes: string[];
};

export type AuthSession = {
  accessToken: string;
  expiresAt: string;
  user: CurrentUser;
};

const STORAGE_KEY = 'space-auth-session';

function getSessionStorage() {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.sessionStorage;
}

function clearLegacyLocalStorage() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(STORAGE_KEY);
}

export function readAuthSession(): AuthSession | null {
  const storage = getSessionStorage();
  if (!storage) {
    return null;
  }
  // Clear old persistent tokens so users must re-authenticate after this fix.
  clearLegacyLocalStorage();
  const raw = storage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const session = JSON.parse(raw) as AuthSession;
    if (!session.accessToken || !session.expiresAt || !session.user) {
      clearAuthSession();
      return null;
    }
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      clearAuthSession();
      return null;
    }
    return session;
  } catch {
    clearAuthSession();
    return null;
  }
}

export function writeAuthSession(session: AuthSession) {
  const storage = getSessionStorage();
  if (!storage) {
    return;
  }
  clearLegacyLocalStorage();
  storage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearAuthSession() {
  const storage = getSessionStorage();
  if (!storage) {
    return;
  }
  storage.removeItem(STORAGE_KEY);
  clearLegacyLocalStorage();
}

export function getAccessToken(): string | null {
  return readAuthSession()?.accessToken ?? null;
}
