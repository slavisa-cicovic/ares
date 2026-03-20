import type { Components, Theme } from "@mui/material/styles";
import { getAppBarOverrides } from "./app-bar";
import { getButtonOverrides } from "./button";
import { getCardOverrides } from "./card";
import { getDialogOverrides } from "./dialog";
import { getPaperOverrides } from "./paper";
import { getTableOverrides } from "./table";
import { getTextFieldOverrides } from "./text-field";
import type { ThemePreset } from "../presets";

export function getThemeComponentOverrides(
  theme: Theme,
  preset: ThemePreset
): Components<Theme> {
  return {
    ...getAppBarOverrides(theme, preset),
    ...getButtonOverrides(theme),
    ...getPaperOverrides(theme),
    ...getCardOverrides(theme),
    ...getDialogOverrides(theme),
    ...getTextFieldOverrides(theme),
    ...getTableOverrides(theme)
  };
}
