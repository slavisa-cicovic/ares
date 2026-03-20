import type { PropsWithChildren } from "react";

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
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="brand-block">
          <span className="brand-kicker">Ares</span>
          <h1>Ares Desktop</h1>
        </div>

        <nav className="app-nav">
          {navigationItems.map((item) => (
            <button
              key={item.id}
              className={`nav-item${item.id === activeView ? " active" : ""}`}
              onClick={() => onNavigate(item.id)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="app-main">
        <header className="app-header">
          <div className="header-copy">
            <span className="eyebrow">{activeLabel}</span>
            <h2>{companyName}</h2>
            {headerDescription ? <p>{headerDescription}</p> : null}
          </div>

          <div className="account-panel">
            <div className="account-copy">
              <span className="eyebrow">Signed In</span>
              <strong>{userName}</strong>
              <small>{userEmail}</small>
            </div>

            <button
              className="secondary-button"
              onClick={() => void onLogout()}
              type="button"
            >
              Logout
            </button>
          </div>
        </header>

        <section className="app-content">{children}</section>
      </main>
    </div>
  );
}
