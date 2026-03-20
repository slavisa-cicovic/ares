import type { Components, Theme } from "@mui/material/styles";

export function getButtonOverrides(theme: Theme): Components<Theme> {
  return {
    MuiButton: {
      defaultProps: {
        disableElevation: true
      },
      styleOverrides: {
        root: {
          borderRadius: 14,
          paddingInline: theme.spacing(2),
          minHeight: 42
        },
        contained: {
          boxShadow: theme.customShadows.selected
        },
        outlined: {
          borderColor: theme.palette.divider
        }
      }
    }
  };
}
