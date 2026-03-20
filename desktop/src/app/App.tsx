import { AuthProvider, useAuth } from "../features/auth/AuthProvider";
import { DashboardScreen } from "../features/dashboard/DashboardScreen";
import { LoginScreen } from "../features/auth/LoginScreen";
import { Shell } from "./Shell";

function AppContent() {
  const { state, logout } = useAuth();

  if (state.status === "bootstrapping") {
    return <div className="screen-state">Restoring session...</div>;
  }

  if (state.status === "unauthenticated") {
    return <LoginScreen />;
  }

  return (
    <Shell
      companyName={state.user.companyName}
      userEmail={state.user.email}
      onLogout={logout}
    >
      <DashboardScreen />
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
