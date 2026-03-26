import { useEffect, useState } from "react";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { AuthProvider, useAuth } from "../features/auth/AuthProvider";
import { LoginScreen } from "../features/auth/LoginScreen";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { SettingsScreen } from "../features/settings/SettingsScreen";
import { ClientsScreen } from "../features/clients/ClientsScreen";
import { ItemsScreen } from "../features/items/ItemsScreen";
import { InvoicesScreen } from "../features/invoices/InvoicesScreen";
import { PaymentsScreen } from "../features/payments/PaymentsScreen";
import { ReportsScreen } from "../features/reports/ReportsScreen";
import { SuperAdminCompaniesScreen } from "../features/admin/SuperAdminCompaniesScreen";
import { SuperAdminUsersScreen } from "../features/admin/SuperAdminUsersScreen";
import { normalizeDesktopTheme } from "../features/settings/themeStorage";
import { Shell, type AppView } from "./Shell";
import { createAppTheme } from "../theme/createAppTheme";

type BrandingPreviewState = {
  themePreset: string;
  brandColor: string | null;
  logoUrl: string | null;
};

function AppContent() {
  const { state, logout } = useAuth();
  const [activeView, setActiveView] = useState<AppView>("dashboard");
  const [brandingPreview, setBrandingPreview] = useState<BrandingPreviewState | null>(null);
  const isAuthenticated = state.status === "authenticated";
  const isPlatformAdmin = isAuthenticated ? state.user.platformAdmin : false;

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    if (isPlatformAdmin && activeView !== "companies" && activeView !== "users" && activeView !== "settings") {
      setActiveView("companies");
    }

    if (!isPlatformAdmin && (activeView === "companies" || activeView === "users")) {
      setActiveView("dashboard");
    }
  }, [activeView, isAuthenticated, isPlatformAdmin]);

  if (state.status === "bootstrapping") {
    return <div className="screen-state">Restoring session...</div>;
  }

  if (state.status === "unauthenticated") {
    return <LoginScreen />;
  }

  const themePreset = normalizeDesktopTheme(brandingPreview?.themePreset ?? state.user.companyThemePreset);
  const muiTheme = createAppTheme(themePreset, brandingPreview?.brandColor ?? state.user.companyBrandColor);

  return (
    <ThemeProvider theme={muiTheme}>
      <CssBaseline />
      <Shell
        activeView={activeView}
        companyLogoUrl={brandingPreview?.logoUrl ?? state.user.companyLogoUrl}
        companyName={state.user.companyName}
        onNavigate={setActiveView}
        platformAdmin={state.user.platformAdmin}
        userEmail={state.user.email}
        userLogoUrl={state.user.userLogoUrl}
        userName={`${state.user.firstName} ${state.user.lastName}`}
        onLogout={logout}
      >
        {!isPlatformAdmin && activeView === "dashboard" ? <DashboardScreen /> : null}
        {isPlatformAdmin && activeView === "companies" ? <SuperAdminCompaniesScreen /> : null}
        {isPlatformAdmin && activeView === "users" ? <SuperAdminUsersScreen /> : null}
        {activeView === "settings" ? (
          <SettingsScreen
            onBrandingPreviewChange={setBrandingPreview}
            user={state.user}
          />
        ) : null}
        {!isPlatformAdmin && activeView === "clients" ? <ClientsScreen user={state.user} /> : null}
        {!isPlatformAdmin && activeView === "items" ? <ItemsScreen user={state.user} /> : null}
        {!isPlatformAdmin && activeView === "invoices" ? <InvoicesScreen /> : null}
        {!isPlatformAdmin && activeView === "payments" ? <PaymentsScreen /> : null}
        {!isPlatformAdmin && activeView === "reports" ? <ReportsScreen /> : null}
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
