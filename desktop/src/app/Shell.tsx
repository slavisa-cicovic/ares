import type { PropsWithChildren } from "react";

type ShellProps = PropsWithChildren<{
  companyName: string;
  userEmail: string;
  onLogout: () => Promise<void>;
}>;

const navigationItems = [
  "Dashboard",
  "Clients",
  "Items",
  "Invoices",
  "Payments",
  "Reports"
];

export function Shell({ companyName, userEmail, onLogout, children }: ShellProps) {
  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="brand-block">
          <span className="brand-kicker">Ares</span>
          <h1>Ares Desktop</h1>
          <p>Backend-connected business workspace</p>
        </div>

        <nav className="app-nav">
          {navigationItems.map((item) => (
            <button
              key={item}
              className={`nav-item${item === "Dashboard" ? " active" : ""}`}
              type="button"
            >
              {item}
            </button>
          ))}
        </nav>
      </aside>

      <main className="app-main">
        <header className="app-header">
          <div>
            <span className="eyebrow">Company</span>
            <h2>{companyName}</h2>
          </div>

          <div className="session-card">
            <div>
              <span className="eyebrow">Signed In</span>
              <strong>{userEmail}</strong>
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
