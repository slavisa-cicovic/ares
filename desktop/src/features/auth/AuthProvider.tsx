import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
import { DesktopApiClient, ApiClientError } from "../../lib/api/DesktopApiClient";
import { clearSession, loadSession, saveSession } from "./sessionStorage";
import type { LoginPayload, SessionData } from "./types";
import type { ReportsSummary } from "../../lib/api/types";

type AuthState =
  | { status: "bootstrapping" }
  | { status: "unauthenticated"; error?: string }
  | { status: "authenticated"; user: SessionData["user"] };

type AuthContextValue = {
  state: AuthState;
  login: (payload: LoginPayload) => Promise<void>;
  logout: () => Promise<void>;
  getReportsSummary: () => Promise<ReportsSummary>;
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<SessionData | null>(() => loadSession());
  const [state, setState] = useState<AuthState>({ status: "bootstrapping" });

  const client = useMemo(
    () =>
      new DesktopApiClient(
        apiBaseUrl,
        () => loadSession(),
        (nextSession) => {
          if (nextSession) {
            saveSession(nextSession);
          } else {
            clearSession();
          }

          setSession(nextSession);
        }
      ),
    []
  );

  useEffect(() => {
    let active = true;

    void client.restoreSession().then((nextSession) => {
      if (!active) {
        return;
      }

      if (nextSession) {
        setState({ status: "authenticated", user: nextSession.user });
        return;
      }

      setState({ status: "unauthenticated" });
    });

    return () => {
      active = false;
    };
  }, [client]);

  useEffect(() => {
    if (!session && state.status === "authenticated") {
      setState({ status: "unauthenticated" });
    }
  }, [session, state.status]);

  const value: AuthContextValue = {
    state,
    login: async (payload) => {
      try {
        const nextSession = await client.login(payload);
        setState({ status: "authenticated", user: nextSession.user });
      } catch (error) {
        const message =
          error instanceof ApiClientError ? error.message : "Unable to sign in right now.";
        setState({ status: "unauthenticated", error: message });
        throw error;
      }
    },
    logout: async () => {
      await client.logout();
      setState({ status: "unauthenticated" });
    },
    getReportsSummary: async () => {
      return client.getReportsSummary();
    }
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }

  return context;
}
