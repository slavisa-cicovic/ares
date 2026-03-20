import { useState } from "react";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "./AuthProvider";

export function LoginScreen() {
  const { login, state } = useAuth();
  const [email, setEmail] = useState("admin.one@local.ares");
  const [password, setPassword] = useState("ares-dev");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await login({ email, password });
    } catch (submissionError) {
      setError(
        submissionError instanceof ApiClientError
          ? submissionError.message
          : "Unable to sign in right now."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-layout">
      <section className="auth-panel auth-panel-accent">
        <span className="eyebrow">Ares Desktop</span>
        <h1>Backend-connected accounting workspace.</h1>
        <p>
          Sign in against the Spring Boot backend and load the first dashboard
          summary from the real tenant data model.
        </p>
      </section>

      <section className="auth-panel auth-panel-form">
        <form className="auth-form" onSubmit={handleSubmit}>
          <div>
            <span className="eyebrow">Login</span>
            <h2>Sign in</h2>
          </div>

          <label className="field">
            <span>Email</span>
            <input
              autoComplete="username"
              onChange={(event) => setEmail(event.target.value)}
              type="email"
              value={email}
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              value={password}
            />
          </label>

          {(error || state.status === "unauthenticated" && state.error) && (
            <div className="error-banner">{error ?? state.error}</div>
          )}

          <button className="primary-button" disabled={submitting} type="submit">
            {submitting ? "Signing In..." : "Sign In"}
          </button>
        </form>
      </section>
    </div>
  );
}
