import { alpha, type Components, type Theme } from "@mui/material/styles";

export function getTextFieldOverrides(theme: Theme): Components<Theme> {
  const lightSurface = alpha(theme.palette.background.paper, theme.palette.mode === "dark" ? 0.78 : 0.84);

  return {
    MuiFormHelperText: {
      defaultProps: {
        component: "div"
      },
      styleOverrides: {
        root: {
          marginLeft: 0,
          marginRight: 0,
          minHeight: 20
        }
      }
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          fontWeight: 600
        }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          backgroundColor: lightSurface,
          transition: theme.transitions.create(["border-color", "box-shadow", "background-color"], {
            duration: theme.transitions.duration.shorter
          }),
          "& .MuiOutlinedInput-notchedOutline": {
            borderColor: theme.palette.divider
          },
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: alpha(theme.palette.text.primary, 0.28)
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderColor: theme.palette.primary.main,
            borderWidth: 1
          },
          "&.Mui-focused": {
            boxShadow: `0 0 0 4px ${alpha(theme.palette.primary.main, 0.14)}`
          }
        },
        input: {
          paddingTop: 15,
          paddingBottom: 15
        }
      }
    }
  };
}
