import { useEffect, useState } from "react";
import type { AuthenticatedUser } from "../auth/types";
import type { CompanyResponse } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "../auth/AuthProvider";
import type { DesktopTheme } from "./themeStorage";

type SettingsSection = "company" | "profile" | "appearance";

type SettingsScreenProps = {
  user: AuthenticatedUser;
  theme: DesktopTheme;
  onThemeChange: (theme: DesktopTheme) => void;
};

const settingsSections: Array<{ id: SettingsSection; label: string; description: string }> = [
  { id: "company", label: "Company", description: "Tenant identity, legal info, and branding." },
  { id: "profile", label: "Profile", description: "Your personal account and security settings." },
  { id: "appearance", label: "Appearance", description: "Choose how the desktop workspace looks." }
];

const themeOptions: Array<{ id: DesktopTheme; label: string; description: string }> = [
  { id: "amber", label: "Amber Ledger", description: "Warm paper tones close to the current workspace." },
  { id: "ocean", label: "Ocean Steel", description: "Cool blue surfaces with stronger contrast." },
  { id: "graphite", label: "Graphite", description: "Muted neutral palette for denser daily work." }
];

function buildInitialCompanyName(user: AuthenticatedUser): string {
  return user.companyName;
}

export function SettingsScreen({ user, theme, onThemeChange }: SettingsScreenProps) {
  const { getCompany, updateCompany } = useAuth();
  const [activeSection, setActiveSection] = useState<SettingsSection>("company");
  const [company, setCompany] = useState<CompanyResponse | null>(null);
  const [companyName, setCompanyName] = useState(() => buildInitialCompanyName(user));
  const [companyLoading, setCompanyLoading] = useState(true);
  const [companySaving, setCompanySaving] = useState(false);
  const [companyError, setCompanyError] = useState<string | null>(null);
  const [companyNotice, setCompanyNotice] = useState<string | null>(null);

  const isCompanyAdmin = user.roles.includes("company-admin");

  useEffect(() => {
    let active = true;

    void getCompany()
      .then((response) => {
        if (!active) {
          return;
        }

        setCompany(response);
        setCompanyName(response.name);
        setCompanyLoading(false);
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }

        setCompanyError(
          requestError instanceof ApiClientError
            ? requestError.message
            : "Unable to load company settings."
        );
        setCompanyLoading(false);
      });

    return () => {
      active = false;
    };
  }, [getCompany]);

  async function handleCompanySave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCompanySaving(true);
    setCompanyError(null);
    setCompanyNotice(null);

    try {
      const updatedCompany = await updateCompany({ name: companyName });
      setCompany(updatedCompany);
      setCompanyName(updatedCompany.name);
      setCompanyNotice("Company name updated.");
    } catch (requestError) {
      setCompanyError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "Unable to save company settings."
      );
    } finally {
      setCompanySaving(false);
    }
  }

  return (
    <div className="settings-layout">
      <aside className="settings-nav">
        {settingsSections.map((section) => (
          <button
            key={section.id}
            className={`settings-nav-item${activeSection === section.id ? " active" : ""}`}
            onClick={() => setActiveSection(section.id)}
            type="button"
          >
            <strong>{section.label}</strong>
            <span>{section.description}</span>
          </button>
        ))}
      </aside>

      <section className="settings-panel">
        {activeSection === "company" ? (
          <div className="settings-section-stack">
            <div className="screen-intro">
              <span className="eyebrow">Settings</span>
              <h3>Company</h3>
              <p>Company administrators can update the tenant profile and later configure branding.</p>
            </div>

            <form className="settings-card" onSubmit={(event) => void handleCompanySave(event)}>
              <div className="settings-card-header">
                <div>
                  <span className="eyebrow">Company Profile</span>
                  <h4>Core identity</h4>
                </div>

                {company?.status ? <span className="status-pill">{company.status}</span> : null}
              </div>

              <div className="settings-grid">
                <label className="field">
                  <span>Company Name</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) => setCompanyName(event.target.value)}
                    value={companyName}
                  />
                </label>

                <label className="field field-disabled">
                  <span>PIB</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <label className="field field-disabled">
                  <span>MB</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <label className="field field-disabled">
                  <span>Email</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <label className="field field-disabled">
                  <span>Phone</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <label className="field field-disabled field-span-2">
                  <span>Address</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <label className="field field-disabled">
                  <span>Bank Account</span>
                  <input disabled placeholder="Backend field not available yet" value="" />
                </label>

                <div className="settings-upload-placeholder">
                  <span className="eyebrow">Branding</span>
                  <strong>Company Logo</strong>
                  <p>Logo upload will be enabled when asset storage and backend support are ready.</p>
                </div>
              </div>

              {companyError ? <div className="error-banner inline-banner">{companyError}</div> : null}
              {companyNotice ? <div className="success-banner inline-banner">{companyNotice}</div> : null}

              <div className="settings-actions">
                <button
                  className="primary-button"
                  disabled={!isCompanyAdmin || companyLoading || companySaving}
                  type="submit"
                >
                  {companySaving ? "Saving..." : "Save Company"}
                </button>

                {!isCompanyAdmin ? (
                  <span className="muted-note">Only company admins can edit company settings.</span>
                ) : null}
              </div>
            </form>
          </div>
        ) : null}

        {activeSection === "profile" ? (
          <div className="settings-section-stack">
            <div className="screen-intro">
              <span className="eyebrow">Settings</span>
              <h3>Profile</h3>
              <p>Personal account settings stay separate from tenant-wide company settings.</p>
            </div>

            <div className="settings-card">
              <div className="settings-card-header">
                <div>
                  <span className="eyebrow">Account</span>
                  <h4>{user.firstName} {user.lastName}</h4>
                </div>
              </div>

              <div className="profile-summary-grid">
                <div className="profile-avatar-placeholder">
                  <span>{user.firstName.charAt(0)}{user.lastName.charAt(0)}</span>
                </div>

                <div className="profile-summary-copy">
                  <strong>{user.email}</strong>
                  <p>User avatar and password change will live here once backend support is added.</p>
                </div>
              </div>

              <div className="profile-actions-grid">
                <div className="feature-placeholder-card">
                  <span className="eyebrow">Future Action</span>
                  <strong>Change Password</strong>
                  <p>Requires dedicated backend endpoint and validation flow.</p>
                </div>

                <div className="feature-placeholder-card">
                  <span className="eyebrow">Future Action</span>
                  <strong>Avatar</strong>
                  <p>Requires asset upload strategy and profile image storage.</p>
                </div>
              </div>
            </div>
          </div>
        ) : null}

        {activeSection === "appearance" ? (
          <div className="settings-section-stack">
            <div className="screen-intro">
              <span className="eyebrow">Settings</span>
              <h3>Appearance</h3>
              <p>Desktop theme selection starts locally and can later become a backend-backed preference.</p>
            </div>

            <div className="settings-card">
              <div className="settings-card-header">
                <div>
                  <span className="eyebrow">Theme</span>
                  <h4>Workspace look and feel</h4>
                </div>
              </div>

              <div className="theme-grid">
                {themeOptions.map((option) => (
                  <button
                    key={option.id}
                    className={`theme-card${theme === option.id ? " active" : ""}`}
                    onClick={() => onThemeChange(option.id)}
                    type="button"
                  >
                    <span className="eyebrow">{option.label}</span>
                    <strong>{option.description}</strong>
                  </button>
                ))}
              </div>

              <div className="feature-placeholder-card compact">
                <span className="eyebrow">Future</span>
                <strong>Invoice Templates</strong>
                <p>PDF template selection and customization will later live under settings.</p>
              </div>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  );
}
