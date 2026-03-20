import { createTheme, type Theme } from "@mui/material/styles";
import type { DesktopTheme } from "../features/settings/themeStorage";

export function createAppTheme(mode: DesktopTheme): Theme {
  if (mode === "ocean") {
    return createTheme({
      palette: {
        mode: "light",
        primary: { main: "#4b8ab4" },
        secondary: { main: "#75a9ca" },
        background: {
          default: "#edf6fa",
          paper: "#ffffff"
        },
        text: {
          primary: "#163040",
          secondary: "#4e6978"
        }
      },
      shape: { borderRadius: 16 }
    });
  }

  if (mode === "graphite") {
    return createTheme({
      palette: {
        mode: "dark",
        primary: { main: "#c2d0d9" },
        secondary: { main: "#9eb0bc" },
        background: {
          default: "#20262b",
          paper: "#242b31"
        }
      },
      shape: { borderRadius: 16 }
    });
  }

  return createTheme({
    palette: {
      mode: "light",
      primary: { main: "#b7844f" },
      secondary: { main: "#d4ab77" },
      background: {
        default: "#f4efe5",
        paper: "#fffdf9"
      },
      text: {
        primary: "#1f2430",
        secondary: "#6c6258"
      }
    },
    shape: { borderRadius: 16 }
  });
}
