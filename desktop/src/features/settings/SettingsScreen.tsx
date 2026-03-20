import { useEffect, useState } from "react";
import type { AuthenticatedUser } from "../auth/types";
import type { ChangePasswordPayload, CompanyResponse, UpdateCompanyPayload } from "../../lib/api/types";
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

function toFormValue(value: string | null | undefined): string {
  return value ?? "";
}

export function SettingsScreen({ user, theme, onThemeChange }: SettingsScreenProps) {
  const { getCompany, updateCompany, changePassword } = useAuth();
  const [activeSection, setActiveSection] = useState<SettingsSection>("company");
  const [company, setCompany] = useState<CompanyResponse | null>(null);
  const [companyForm, setCompanyForm] = useState<UpdateCompanyPayload>({
    name: buildInitialCompanyName(user),
    pib: null,
    mb: null,
    address: null,
    email: null,
    phone: null,
    bankAccount: null
  });
  const [companyLoading, setCompanyLoading] = useState(true);
  const [companySaving, setCompanySaving] = useState(false);
  const [companyError, setCompanyError] = useState<string | null>(null);
  const [companyNotice, setCompanyNotice] = useState<string | null>(null);
  const [passwordForm, setPasswordForm] = useState<ChangePasswordPayload>({
    currentPassword: "",
    newPassword: "",
    confirmNewPassword: ""
  });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);

  const isCompanyAdmin = user.roles.includes("company-admin");

  useEffect(() => {
    let active = true;

    void getCompany()
      .then((response) => {
        if (!active) {
          return;
        }

        setCompany(response);
        setCompanyForm({
          name: response.name,
          pib: response.pib,
          mb: response.mb,
          address: response.address,
          email: response.email,
          phone: response.phone,
          bankAccount: response.bankAccount
        });
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
      const updatedCompany = await updateCompany(companyForm);
      setCompany(updatedCompany);
      setCompanyForm({
        name: updatedCompany.name,
        pib: updatedCompany.pib,
        mb: updatedCompany.mb,
        address: updatedCompany.address,
        email: updatedCompany.email,
        phone: updatedCompany.phone,
        bankAccount: updatedCompany.bankAccount
      });
      setCompanyNotice("Company profile updated.");
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

  function handleCompanyCancel() {
    if (company) {
      setCompanyForm({
        name: company.name,
        pib: company.pib,
        mb: company.mb,
        address: company.address,
        email: company.email,
        phone: company.phone,
        bankAccount: company.bankAccount
      });
    }

    setCompanyError(null);
    setCompanyNotice(null);
  }

  async function handlePasswordSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordSaving(true);
    setPasswordError(null);
    setPasswordNotice(null);

    try {
      await changePassword(passwordForm);
      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmNewPassword: ""
      });
      setPasswordNotice("Password updated.");
    } catch (requestError) {
      setPasswordError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "Unable to change password right now."
      );
    } finally {
      setPasswordSaving(false);
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
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, name: event.target.value }))
                    }
                    value={companyForm.name}
                  />
                </label>

                <label className="field">
                  <span>PIB</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, pib: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.pib)}
                  />
                </label>

                <label className="field">
                  <span>MB</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, mb: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.mb)}
                  />
                </label>

                <label className="field">
                  <span>Email</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, email: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.email)}
                  />
                </label>

                <label className="field">
                  <span>Phone</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, phone: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.phone)}
                  />
                </label>

                <label className="field field-span-2">
                  <span>Address</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, address: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.address)}
                  />
                </label>

                <label className="field">
                  <span>Bank Account</span>
                  <input
                    disabled={!isCompanyAdmin || companyLoading || companySaving}
                    onChange={(event) =>
                      setCompanyForm((current) => ({ ...current, bankAccount: event.target.value || null }))
                    }
                    value={toFormValue(companyForm.bankAccount)}
                  />
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

                <button
                  className="secondary-button"
                  disabled={!isCompanyAdmin || companyLoading || companySaving}
                  onClick={handleCompanyCancel}
                  type="button"
                >
                  Cancel
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

            <form className="settings-card" onSubmit={(event) => void handlePasswordSave(event)}>
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
                  <p>Password change is available now. Avatar support stays deferred until upload storage is ready.</p>
                </div>
              </div>

              <div className="profile-actions-grid">
                <div className="feature-placeholder-card password-form-card">
                  <span className="eyebrow">Security</span>
                  <strong>Change Password</strong>
                  <div className="password-form-grid">
                    <label className="field">
                      <span>Current Password</span>
                      <input
                        disabled={passwordSaving}
                        onChange={(event) =>
                          setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))
                        }
                        type="password"
                        value={passwordForm.currentPassword}
                      />
                    </label>

                    <label className="field">
                      <span>New Password</span>
                      <input
                        disabled={passwordSaving}
                        onChange={(event) =>
                          setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))
                        }
                        type="password"
                        value={passwordForm.newPassword}
                      />
                    </label>

                    <label className="field">
                      <span>Confirm New Password</span>
                      <input
                        disabled={passwordSaving}
                        onChange={(event) =>
                          setPasswordForm((current) => ({ ...current, confirmNewPassword: event.target.value }))
                        }
                        type="password"
                        value={passwordForm.confirmNewPassword}
                      />
                    </label>
                  </div>

                  {passwordError ? <div className="error-banner inline-banner">{passwordError}</div> : null}
                  {passwordNotice ? <div className="success-banner inline-banner">{passwordNotice}</div> : null}

                  <div className="settings-actions">
                    <button className="primary-button" disabled={passwordSaving} type="submit">
                      {passwordSaving ? "Saving..." : "Change Password"}
                    </button>
                  </div>
                </div>

                <div className="feature-placeholder-card">
                  <span className="eyebrow">Future Action</span>
                  <strong>Avatar</strong>
                  <p>Requires asset upload strategy and profile image storage.</p>
                </div>
              </div>
            </form>
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
