import { useState } from "react";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { AuthProvider, useAuth } from "../features/auth/AuthProvider";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { LoginScreen } from "../features/auth/LoginScreen";
import { PlaceholderScreen } from "../features/navigation/PlaceholderScreen";
import { SettingsScreen } from "../features/settings/SettingsScreen";
import { ClientsScreen } from "../features/clients/ClientsScreen";
import { normalizeDesktopTheme } from "../features/settings/themeStorage";
import { Shell, type AppView } from "./Shell";
import { createAppTheme } from "../theme/createAppTheme";

function AppContent() {
  const { state, logout } = useAuth();
  const [activeView, setActiveView] = useState<AppView>("dashboard");

  if (state.status === "bootstrapping") {
    return <div className="screen-state">Restoring session...</div>;
  }

  if (state.status === "unauthenticated") {
    return <LoginScreen />;
  }

  const themePreset = normalizeDesktopTheme(state.user.companyThemePreset);
  const muiTheme = createAppTheme(themePreset, state.user.companyBrandColor);

  return (
    <ThemeProvider theme={muiTheme}>
      <CssBaseline />
      <Shell
        activeView={activeView}
        companyLogoUrl={state.user.companyLogoUrl}
        companyName={state.user.companyName}
        onNavigate={setActiveView}
        userEmail={state.user.email}
        userName={`${state.user.firstName} ${state.user.lastName}`}
        onLogout={logout}
      >
        {activeView === "dashboard" ? <DashboardScreen /> : null}
        {activeView === "settings" ? <SettingsScreen user={state.user} /> : null}
        {activeView === "clients" ? <ClientsScreen user={state.user} /> : null}
        {activeView === "items" ? (
          <PlaceholderScreen
            description="Item catalog screens will connect to the existing backend item module."
            title="Items"
          />
        ) : null}
        {activeView === "invoices" ? (
          <PlaceholderScreen
            description="Invoice drafting, issuing, and detail workflows will live here."
            title="Invoices"
          />
        ) : null}
        {activeView === "payments" ? (
          <PlaceholderScreen
            description="Payment registration and invoice collection views will live here."
            title="Payments"
          />
        ) : null}
        {activeView === "reports" ? (
          <PlaceholderScreen
            description="Reporting screens beyond the dashboard summary will live here."
            title="Reports"
          />
        ) : null}
      </Shell>
    </ThemeProvider>
  );
}

export function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
