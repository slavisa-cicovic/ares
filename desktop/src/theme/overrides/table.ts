import { alpha, type Components, type Theme } from "@mui/material/styles";

export function getTableOverrides(theme: Theme): Components<Theme> {
  return {
    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 18
        }
      }
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor:
            theme.palette.mode === "dark"
              ? alpha(theme.palette.common.white, 0.02)
              : alpha(theme.palette.primary.main, 0.04)
        }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: `1px dashed ${theme.palette.divider}`
        },
        head: {
          color: theme.palette.text.secondary,
          fontSize: theme.typography.caption.fontSize,
          fontWeight: 700,
          letterSpacing: "0.08em",
          textTransform: "uppercase"
        },
        body: {
          fontSize: theme.typography.body2.fontSize
        }
      }
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          "&:last-of-type .MuiTableCell-root": {
            borderBottom: 0
          }
        }
      }
    },
    MuiTablePagination: {
      defaultProps: {
        backIconButtonProps: {
          size: "small"
        },
        nextIconButtonProps: {
          size: "small"
        }
      },
      styleOverrides: {
        toolbar: {
          paddingInline: theme.spacing(2)
        },
        selectLabel: {
          ...theme.typography.body2
        },
        displayedRows: {
          ...theme.typography.body2
        }
      }
    }
  };
}
