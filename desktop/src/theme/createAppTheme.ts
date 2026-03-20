import { createTheme, type Theme } from "@mui/material/styles";
import type { DesktopTheme } from "../features/settings/themeStorage";
import { customShadows } from "./custom-shadows";
import { getThemeComponentOverrides } from "./overrides";
import { resolveThemePreset } from "./presets";
import { shadows } from "./shadows";
import { typography } from "./typography";

export function createAppTheme(mode: DesktopTheme): Theme {
  const preset = resolveThemePreset(mode);
  const themeCore = createTheme({
    ...preset.themeOptions,
    shape: { borderRadius: 18 },
    typography,
    shadows: shadows(preset.themeOptions.palette?.mode === "dark" ? "dark" : "light")
  });

  const themeWithCustomShadows = createTheme(themeCore, {
    customShadows: customShadows(themeCore)
  });

  return createTheme(themeWithCustomShadows, {
    components: getThemeComponentOverrides(themeWithCustomShadows, preset)
  });
}
