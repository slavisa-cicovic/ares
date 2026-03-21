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
import { ReportsScreen } from "../features/reports/ReportsScreen";
import { PlaceholderScreen } from "../features/navigation/PlaceholderScreen";
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
        userEmail={state.user.email}
        userLogoUrl={state.user.userLogoUrl}
        userName={`${state.user.firstName} ${state.user.lastName}`}
        onLogout={logout}
      >
        {activeView === "dashboard" ? <DashboardScreen /> : null}
        {activeView === "settings" ? (
          <SettingsScreen
            onBrandingPreviewChange={setBrandingPreview}
            user={state.user}
          />
        ) : null}
        {activeView === "clients" ? <ClientsScreen user={state.user} /> : null}
        {activeView === "items" ? <ItemsScreen user={state.user} /> : null}
        {activeView === "invoices" ? <InvoicesScreen /> : null}
        {activeView === "payments" ? <PaymentsScreen /> : null}
        {activeView === "reports" ? <ReportsScreen /> : null}
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
