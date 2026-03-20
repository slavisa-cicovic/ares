import type { SessionData } from "./types";

const storageKey = "ares.desktop.session";

export function loadSession(): SessionData | null {
  const raw = window.localStorage.getItem(storageKey);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionData;
  } catch {
    window.localStorage.removeItem(storageKey);
    return null;
  }
}

export function saveSession(session: SessionData): void {
  window.localStorage.setItem(storageKey, JSON.stringify(session));
}

export function clearSession(): void {
  window.localStorage.removeItem(storageKey);
}
