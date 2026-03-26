import type { PropsWithChildren } from "react";
import {
  AppBar,
  Avatar,
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
  | "companies"
  | "users"
  | "clients"
  | "items"
  | "invoices"
  | "payments"
  | "reports"
  | "settings";

type ShellProps = PropsWithChildren<{
  companyName: string;
  companyLogoUrl: string | null;
  userName: string;
  userEmail: string;
  userLogoUrl: string | null;
  platformAdmin: boolean;
  companyAdmin: boolean;
  activeView: AppView;
  onNavigate: (view: AppView) => void;
  onLogout: () => Promise<void>;
}>;

const tenantNavigationItems: Array<{ id: AppView; label: string }> = [
  { id: "dashboard", label: "Dashboard" },
  { id: "clients", label: "Clients" },
  { id: "items", label: "Items" },
  { id: "invoices", label: "Invoices" },
  { id: "payments", label: "Payments" },
  { id: "reports", label: "Reports" },
  { id: "settings", label: "Settings" }
];

const platformNavigationItems: Array<{ id: AppView; label: string }> = [
  { id: "companies", label: "Companies" },
  { id: "users", label: "Users" },
  { id: "settings", label: "Settings" }
];

const drawerWidth = 280;

export function Shell({
  companyName,
  companyLogoUrl,
  userName,
  userEmail,
  userLogoUrl,
  platformAdmin,
  companyAdmin,
  activeView,
  onNavigate,
  onLogout,
  children
}: ShellProps) {
  const navigationItems = platformAdmin
    ? platformNavigationItems
    : companyAdmin
      ? [...tenantNavigationItems.slice(0, 1), { id: "users" as const, label: "Users" }, ...tenantNavigationItems.slice(1)]
      : tenantNavigationItems;

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
              px: 2.5,
              py: 3
            }
          }
        }}
        sx={{ width: drawerWidth, flexShrink: 0 }}
        variant="permanent"
      >
        <Stack spacing={3}>
          <Box>
            <Typography color="primary.light" variant="overline">
              Ares
            </Typography>
            <Typography sx={{ color: "common.white" }} variant="h4">
              Ares Desktop
            </Typography>
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
        <AppBar position="static" sx={{ borderBottom: "1px solid", borderColor: "divider" }}>
          <Toolbar sx={{ alignItems: "center", gap: 3, justifyContent: "space-between", py: 2 }}>
            <Stack alignItems="center" direction="row" spacing={2} alignSelf="center">
              <Avatar
                src={!platformAdmin ? (companyLogoUrl ?? undefined) : undefined}
                sx={{
                  width: 52,
                  height: 52,
                  borderRadius: 2.5,
                  bgcolor: "primary.main",
                  color: "primary.contrastText",
                  fontWeight: 700,
                  fontSize: 18
                }}
              >
                {(platformAdmin ? "Ares" : companyName).charAt(0).toUpperCase()}
              </Avatar>

              <Stack spacing={0.25}>
                <Typography color="primary.main" variant="overline">
                  {platformAdmin ? "Platform" : "Active Company"}
                </Typography>
                <Typography variant="h5">{platformAdmin ? "Super Admin Console" : companyName}</Typography>
              </Stack>
            </Stack>

            <Paper
              sx={{ alignSelf: "center", minWidth: 300, p: 2.25 }}
              variant="outlined"
            >
              <Stack alignItems="flex-start" direction="row" justifyContent="space-between" spacing={2}>
                <Stack alignItems="center" direction="row" spacing={1.5}>
                  <Avatar src={userLogoUrl ?? undefined} sx={{ width: 44, height: 44 }}>
                    {userName.charAt(0).toUpperCase()}
                  </Avatar>

                  <Box>
                    <Typography color="primary.main" variant="overline">
                      Signed In
                    </Typography>
                    <Typography variant="subtitle1">{userName}</Typography>
                    <Typography color="text.secondary" variant="body2">
                      {userEmail}
                    </Typography>
                  </Box>
                </Stack>

                <Button onClick={() => void onLogout()} variant="outlined">
                  Logout
                </Button>
              </Stack>
            </Paper>
          </Toolbar>
        </AppBar>

        <Box component="main" sx={{ flexGrow: 1, p: 3.5 }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
}
