import { alpha, type ThemeOptions } from "@mui/material/styles";
import type { DesktopTheme } from "../features/settings/themeStorage";

export type ThemePreset = {
  id: DesktopTheme;
  gradient: string;
  shellBackground: string;
  shellMuted: string;
  shellActiveText: string;
  shellSelectedGradient: string;
  themeOptions: ThemeOptions;
};

function applyBrandColor(preset: ThemePreset, brandColor?: string | null): ThemePreset {
  if (!brandColor || !/^#[0-9A-Fa-f]{6}$/.test(brandColor)) {
    return preset;
  }

  return {
    ...preset,
    shellSelectedGradient: `linear-gradient(135deg, ${brandColor} 0%, ${brandColor}CC 100%)`,
    themeOptions: {
      ...preset.themeOptions,
      palette: {
        ...preset.themeOptions.palette,
        primary: {
          ...preset.themeOptions.palette?.primary,
          main: brandColor
        }
      }
    }
  };
}

function amberPreset(): ThemePreset {
  return {
    id: "amber",
    gradient:
      "radial-gradient(circle at top left, rgba(183, 132, 79, 0.18), transparent 26%), linear-gradient(160deg, #f4efe5 0%, #f8f3eb 52%, #ede2d2 100%)",
    shellBackground: "#20252b",
    shellMuted: "rgba(248, 241, 233, 0.68)",
    shellActiveText: "#1d2430",
    shellSelectedGradient: "linear-gradient(135deg, #c89259 0%, #e0b988 100%)",
    themeOptions: {
      palette: {
        mode: "light",
        primary: { main: "#b7844f", contrastText: "#1d2430" },
        secondary: { main: "#d4ab77" },
        background: {
          default: "#f4efe5",
          paper: "#fffdf9"
        },
        text: {
          primary: "#1f2430",
          secondary: "#6c6258"
        },
        divider: alpha("#6c6258", 0.14)
      }
    }
  };
}

function oceanPreset(): ThemePreset {
  return {
    id: "ocean",
    gradient:
      "radial-gradient(circle at top left, rgba(75, 138, 180, 0.18), transparent 26%), linear-gradient(160deg, #edf6fa 0%, #e7f0f5 52%, #d8e7ef 100%)",
    shellBackground: "#163040",
    shellMuted: "rgba(237, 246, 250, 0.7)",
    shellActiveText: "#112733",
    shellSelectedGradient: "linear-gradient(135deg, #4b8ab4 0%, #78accd 100%)",
    themeOptions: {
      palette: {
        mode: "light",
        primary: { main: "#4b8ab4", contrastText: "#f5fbff" },
        secondary: { main: "#75a9ca" },
        background: {
          default: "#edf6fa",
          paper: "#ffffff"
        },
        text: {
          primary: "#163040",
          secondary: "#4e6978"
        },
        divider: alpha("#4e6978", 0.15)
      }
    }
  };
}

function graphitePreset(): ThemePreset {
  return {
    id: "graphite",
    gradient:
      "radial-gradient(circle at top left, rgba(130, 143, 151, 0.14), transparent 24%), linear-gradient(160deg, #20262b 0%, #252d34 48%, #2b333a 100%)",
    shellBackground: "#14181c",
    shellMuted: "rgba(232, 236, 239, 0.62)",
    shellActiveText: "#101417",
    shellSelectedGradient: "linear-gradient(135deg, #b6c6d0 0%, #d4dee5 100%)",
    themeOptions: {
      palette: {
        mode: "dark",
        primary: { main: "#c2d0d9", contrastText: "#161d22" },
        secondary: { main: "#9eb0bc" },
        background: {
          default: "#20262b",
          paper: "#242b31"
        },
        text: {
          primary: "#e8ecef",
          secondary: "#b3bec7"
        },
        divider: alpha("#e8ecef", 0.08)
      }
    }
  };
}

export function resolveThemePreset(
  theme: DesktopTheme,
  brandColor?: string | null
): ThemePreset {
  let preset: ThemePreset;

  if (theme === "ocean") {
    preset = oceanPreset();
  } else if (theme === "graphite") {
    preset = graphitePreset();
  } else {
    preset = amberPreset();
  }

  return applyBrandColor(preset, brandColor);
}
