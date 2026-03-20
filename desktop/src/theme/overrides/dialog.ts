import { alpha, type Components, type Theme } from "@mui/material/styles";

export function getDialogOverrides(theme: Theme): Components<Theme> {
  return {
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 24,
          border: `1px solid ${theme.palette.divider}`,
          backgroundImage: "none",
          backgroundColor:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.background.paper, 0.96)
              : alpha(theme.palette.background.paper, 0.96),
          boxShadow: theme.customShadows.dialog
        }
      }
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: {
          padding: theme.spacing(3, 3, 1.5)
        }
      }
    },
    MuiDialogContent: {
      styleOverrides: {
        dividers: {
          borderColor: theme.palette.divider
        },
        root: {
          padding: theme.spacing(2, 3, 3)
        }
      }
    },
    MuiDialogActions: {
      defaultProps: {
        disableSpacing: true
      },
      styleOverrides: {
        root: {
          padding: theme.spacing(0, 3, 3),
          gap: theme.spacing(1.5)
        }
      }
    }
  };
}
