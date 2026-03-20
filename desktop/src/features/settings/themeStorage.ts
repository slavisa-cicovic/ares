export type DesktopTheme = "amber" | "ocean" | "graphite";

const storageKey = "ares.desktop.theme";
const defaultTheme: DesktopTheme = "amber";

const allowedThemes = new Set<DesktopTheme>(["amber", "ocean", "graphite"]);

export function loadThemePreference(): DesktopTheme {
  const raw = window.localStorage.getItem(storageKey);

  if (raw && allowedThemes.has(raw as DesktopTheme)) {
    return raw as DesktopTheme;
  }

  return defaultTheme;
}

export function saveThemePreference(theme: DesktopTheme): void {
  window.localStorage.setItem(storageKey, theme);
}
