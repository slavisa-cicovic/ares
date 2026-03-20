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
import type {
  ChangePasswordPayload,
  ClientSummary,
  CompanyResponse,
  InvoiceDetail,
  InvoiceSummary,
  ItemSummary,
  ListClientsParams,
  ListInvoicesParams,
  ListItemsParams,
  PagedResponse,
  ReportsSummary,
  UpsertClientPayload,
  UpsertInvoicePayload,
  UpsertItemPayload,
  UpdateCompanyPayload
} from "../../lib/api/types";

type AuthState =
  | { status: "bootstrapping" }
  | { status: "unauthenticated"; error?: string }
  | { status: "authenticated"; user: SessionData["user"] };

type AuthContextValue = {
  state: AuthState;
  login: (payload: LoginPayload) => Promise<void>;
  logout: () => Promise<void>;
  getReportsSummary: () => Promise<ReportsSummary>;
  getCompany: () => Promise<CompanyResponse>;
  updateCompany: (payload: UpdateCompanyPayload) => Promise<CompanyResponse>;
  changePassword: (payload: ChangePasswordPayload) => Promise<void>;
  getClients: (params: ListClientsParams) => Promise<PagedResponse<ClientSummary>>;
  createClient: (payload: UpsertClientPayload) => Promise<ClientSummary>;
  updateClient: (clientId: string, payload: UpsertClientPayload) => Promise<ClientSummary>;
  getItems: (params: ListItemsParams) => Promise<PagedResponse<ItemSummary>>;
  createItem: (payload: UpsertItemPayload) => Promise<ItemSummary>;
  updateItem: (itemId: string, payload: UpsertItemPayload) => Promise<ItemSummary>;
  deleteItem: (itemId: string) => Promise<void>;
  getInvoices: (params: ListInvoicesParams) => Promise<PagedResponse<InvoiceSummary>>;
  getInvoice: (invoiceId: string) => Promise<InvoiceDetail>;
  createInvoice: (payload: UpsertInvoicePayload) => Promise<InvoiceDetail>;
  updateInvoice: (invoiceId: string, payload: UpsertInvoicePayload) => Promise<InvoiceDetail>;
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<SessionData | null>(() => loadSession());
  const [state, setState] = useState<AuthState>({ status: "bootstrapping" });

  function setStoredSession(nextSession: SessionData | null) {
    if (nextSession) {
      saveSession(nextSession);
    } else {
      clearSession();
    }

    setSession(nextSession);
  }

  const client = useMemo(
    () =>
      new DesktopApiClient(
        apiBaseUrl,
        () => loadSession(),
        setStoredSession
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
    },
    getCompany: async () => {
      return client.getCurrentCompany();
    },
    updateCompany: async (payload) => {
      const updatedCompany = await client.updateCurrentCompany(payload);
      const currentSession = loadSession();

      if (currentSession) {
        const nextSession = {
          ...currentSession,
          user: {
            ...currentSession.user,
            companyName: updatedCompany.name,
            companyBrandColor: updatedCompany.brandColor,
            companyLogoUrl: updatedCompany.logoUrl,
            companyThemePreset: updatedCompany.themePreset
          }
        };

        setStoredSession(nextSession);
        setState({ status: "authenticated", user: nextSession.user });
      }

      return updatedCompany;
    },
    changePassword: async (payload) => {
      await client.changeOwnPassword(payload);
    },
    getClients: async (params) => {
      return client.listClients(params);
    },
    createClient: async (payload) => {
      return client.createClient(payload);
    },
    updateClient: async (clientId, payload) => {
      return client.updateClient(clientId, payload);
    },
    getItems: async (params) => {
      return client.listItems(params);
    },
    createItem: async (payload) => {
      return client.createItem(payload);
    },
    updateItem: async (itemId, payload) => {
      return client.updateItem(itemId, payload);
    },
    deleteItem: async (itemId) => {
      await client.deleteItem(itemId);
    },
    getInvoices: async (params) => {
      return client.listInvoices(params);
    },
    getInvoice: async (invoiceId) => {
      return client.getInvoice(invoiceId);
    },
    createInvoice: async (payload) => {
      return client.createInvoice(payload);
    },
    updateInvoice: async (invoiceId, payload) => {
      return client.updateInvoice(invoiceId, payload);
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
