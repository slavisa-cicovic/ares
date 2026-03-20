import { alpha, type Components, type Theme } from "@mui/material/styles";

export function getPaperOverrides(theme: Theme): Components<Theme> {
  return {
    MuiPaper: {
      defaultProps: {
        elevation: 0
      },
      styleOverrides: {
        root: {
          backgroundImage: "none"
        },
        rounded: {
          borderRadius: 20
        },
        outlined: {
          borderColor: theme.palette.divider,
          backgroundColor:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.background.paper, 0.92)
              : alpha(theme.palette.background.paper, 0.86)
        }
      }
    }
  };
}
