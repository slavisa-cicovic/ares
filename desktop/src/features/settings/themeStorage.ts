export type DesktopTheme = "amber" | "ocean" | "graphite";

const allowedThemes = new Set<DesktopTheme>(["amber", "ocean", "graphite"]);

export function normalizeDesktopTheme(value: string | null | undefined): DesktopTheme {
  if (value && allowedThemes.has(value as DesktopTheme)) {
    return value as DesktopTheme;
  }

  return "amber";
}
