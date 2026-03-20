import type { PropsWithChildren } from "react";
import {
  AppBar,
  Box,
  Button,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Paper,
  Stack,
  Toolbar,
  Typography
} from "@mui/material";

export type AppView =
  | "dashboard"
  | "clients"
  | "items"
  | "invoices"
  | "payments"
  | "reports"
  | "settings";

type ShellProps = PropsWithChildren<{
  companyName: string;
  userName: string;
  userEmail: string;
  activeView: AppView;
  onNavigate: (view: AppView) => void;
  onLogout: () => Promise<void>;
}>;

const navigationItems: Array<{ id: AppView; label: string }> = [
  { id: "dashboard", label: "Dashboard" },
  { id: "clients", label: "Clients" },
  { id: "items", label: "Items" },
  { id: "invoices", label: "Invoices" },
  { id: "payments", label: "Payments" },
  { id: "reports", label: "Reports" },
  { id: "settings", label: "Settings" }
];

const drawerWidth = 280;

export function Shell({
  companyName,
  userName,
  userEmail,
  activeView,
  onNavigate,
  onLogout,
  children
}: ShellProps) {
  const activeLabel = navigationItems.find((item) => item.id === activeView)?.label ?? "Ares";
  const headerDescription =
    activeView === "settings" ? "Company, profile, and appearance controls." : null;

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <Drawer
        slotProps={{
          paper: {
            sx: {
              width: drawerWidth,
              boxSizing: "border-box",
              borderRight: "1px solid",
              borderColor: "divider",
              px: 2,
              py: 3
            }
          }
        }}
        sx={{ width: drawerWidth, flexShrink: 0 }}
        variant="permanent"
      >
        <Stack spacing={3}>
          <Box>
            <Typography color="primary.main" sx={{ letterSpacing: "0.14em", textTransform: "uppercase" }} variant="overline">
              Ares
            </Typography>
            <Typography variant="h4">Ares Desktop</Typography>
          </Box>

          <List disablePadding sx={{ display: "grid", gap: 1 }}>
            {navigationItems.map((item) => (
              <ListItemButton
                key={item.id}
                onClick={() => onNavigate(item.id)}
                selected={item.id === activeView}
                sx={{ borderRadius: 2 }}
              >
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
        </Stack>
      </Drawer>

      <Box sx={{ display: "flex", flexDirection: "column", flexGrow: 1, minWidth: 0 }}>
        <AppBar color="transparent" elevation={0} position="static" sx={{ borderBottom: "1px solid", borderColor: "divider", backdropFilter: "blur(8px)" }}>
          <Toolbar sx={{ alignItems: "flex-start", gap: 3, justifyContent: "space-between", py: 2 }}>
            <Box>
              <Typography color="primary.main" sx={{ letterSpacing: "0.14em", textTransform: "uppercase" }} variant="overline">
                {activeLabel}
              </Typography>
              <Typography variant="h4">{companyName}</Typography>
              {headerDescription ? (
                <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
                  {headerDescription}
                </Typography>
              ) : null}
            </Box>

            <Paper elevation={0} sx={{ p: 2, minWidth: 280 }}>
              <Stack alignItems="flex-start" direction="row" justifyContent="space-between" spacing={2}>
                <Box>
                  <Typography color="primary.main" sx={{ letterSpacing: "0.14em", textTransform: "uppercase" }} variant="overline">
                    Signed In
                  </Typography>
                  <Typography variant="subtitle1">{userName}</Typography>
                  <Typography color="text.secondary" variant="body2">
                    {userEmail}
                  </Typography>
                </Box>

                <Button onClick={() => void onLogout()} variant="outlined">
                  Logout
                </Button>
              </Stack>
            </Paper>
          </Toolbar>
        </AppBar>

        <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
}
