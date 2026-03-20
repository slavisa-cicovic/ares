import { alpha, type Theme } from "@mui/material/styles";

export type CustomShadows = {
  card: string;
  dialog: string;
  dropdown: string;
  selected: string;
};

declare module "@mui/material/styles" {
  interface Theme {
    customShadows: CustomShadows;
  }

  interface ThemeOptions {
    customShadows?: Partial<CustomShadows>;
  }
}

export function customShadows(theme: Theme): CustomShadows {
  const base = theme.palette.mode === "dark" ? "#010409" : theme.palette.primary.main;

  return {
    card: `0 18px 38px ${alpha(base, theme.palette.mode === "dark" ? 0.24 : 0.1)}`,
    dialog: `0 28px 72px ${alpha(base, theme.palette.mode === "dark" ? 0.3 : 0.16)}`,
    dropdown: `0 18px 48px ${alpha(base, theme.palette.mode === "dark" ? 0.26 : 0.14)}`,
    selected: `0 0 0 1px ${alpha(theme.palette.primary.main, 0.28)}, 0 12px 30px ${alpha(
      theme.palette.primary.main,
      theme.palette.mode === "dark" ? 0.22 : 0.14
    )}`
  };
}
