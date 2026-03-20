import { useEffect, useState } from "react";
import { AuthProvider, useAuth } from "../features/auth/AuthProvider";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { LoginScreen } from "../features/auth/LoginScreen";
import { PlaceholderScreen } from "../features/navigation/PlaceholderScreen";
import { SettingsScreen } from "../features/settings/SettingsScreen";
import { ClientsScreen } from "../features/clients/ClientsScreen";
import { loadThemePreference, saveThemePreference, type DesktopTheme } from "../features/settings/themeStorage";
import { Shell, type AppView } from "./Shell";

function AppContent() {
  const { state, logout } = useAuth();
  const [activeView, setActiveView] = useState<AppView>("dashboard");
  const [theme, setTheme] = useState<DesktopTheme>(() => loadThemePreference());

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  function handleThemeChange(nextTheme: DesktopTheme) {
    saveThemePreference(nextTheme);
    setTheme(nextTheme);
  }

  if (state.status === "bootstrapping") {
    return <div className="screen-state">Restoring session...</div>;
  }

  if (state.status === "unauthenticated") {
    return <LoginScreen />;
  }

  return (
    <Shell
      activeView={activeView}
      companyName={state.user.companyName}
      onNavigate={setActiveView}
      userEmail={state.user.email}
      userName={`${state.user.firstName} ${state.user.lastName}`}
      onLogout={logout}
    >
      {activeView === "dashboard" ? <DashboardScreen /> : null}
      {activeView === "settings" ? (
        <SettingsScreen onThemeChange={handleThemeChange} theme={theme} user={state.user} />
      ) : null}
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
  );
}

export function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
