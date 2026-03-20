import { useState } from "react";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { AuthProvider, useAuth } from "../features/auth/AuthProvider";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { LoginScreen } from "../features/auth/LoginScreen";
import { SettingsScreen } from "../features/settings/SettingsScreen";
import { ClientsScreen } from "../features/clients/ClientsScreen";
import { ItemsScreen } from "../features/items/ItemsScreen";
import { InvoicesScreen } from "../features/invoices/InvoicesScreen";
import { PaymentsScreen } from "../features/payments/PaymentsScreen";
import { PlaceholderScreen } from "../features/navigation/PlaceholderScreen";
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
        {activeView === "items" ? <ItemsScreen user={state.user} /> : null}
        {activeView === "invoices" ? <InvoicesScreen /> : null}
        {activeView === "payments" ? <PaymentsScreen /> : null}
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
