import { alpha, type Components, type Theme } from "@mui/material/styles";
import type { ThemePreset } from "../presets";

export function getAppBarOverrides(
  theme: Theme,
  preset: ThemePreset
): Components<Theme> {
  return {
    MuiCssBaseline: {
      styleOverrides: {
        ":root": {
          colorScheme: theme.palette.mode
        },
        body: {
          background: preset.gradient,
          backgroundAttachment: "fixed"
        },
        "#root": {
          minHeight: "100vh"
        }
      }
    },
    MuiStack: {
      defaultProps: {
        useFlexGap: true
      }
    },
    MuiAppBar: {
      defaultProps: {
        color: "transparent",
        elevation: 0
      },
      styleOverrides: {
        root: {
          backdropFilter: "blur(16px)",
          backgroundColor:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.background.paper, 0.66)
              : alpha(theme.palette.background.paper, 0.72)
        }
      }
    },
    MuiToolbar: {
      styleOverrides: {
        root: {
          minHeight: "88px"
        }
      }
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          color: theme.palette.common.white,
          backgroundColor: preset.shellBackground,
          backgroundImage: "none"
        }
      }
    },
    MuiListItemText: {
      styleOverrides: {
        primary: {
          fontWeight: 600
        }
      }
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          minHeight: 46,
          borderRadius: 14,
          color: preset.shellMuted,
          transition: theme.transitions.create(
            ["transform", "background-color", "color", "box-shadow"],
            { duration: theme.transitions.duration.shorter }
          ),
          "&:hover": {
            transform: "translateX(2px)",
            backgroundColor: alpha(theme.palette.common.white, 0.06)
          },
          "&.Mui-selected": {
            color: preset.shellActiveText,
            backgroundImage: preset.shellSelectedGradient,
            boxShadow: theme.customShadows.selected
          },
          "&.Mui-selected:hover": {
            backgroundImage: preset.shellSelectedGradient
          }
        }
      }
    }
  };
}
