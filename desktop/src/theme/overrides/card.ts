import { alpha, type Components, type Theme } from "@mui/material/styles";

export function getCardOverrides(theme: Theme): Components<Theme> {
  return {
    MuiCard: {
      defaultProps: {
        elevation: 0
      },
      styleOverrides: {
        root: {
          borderRadius: 22,
          border: `1px solid ${theme.palette.divider}`,
          backgroundColor:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.background.paper, 0.92)
              : alpha(theme.palette.background.paper, 0.86),
          boxShadow: theme.customShadows.card
        }
      }
    }
  };
}
