import type { SessionData, LoginPayload, AuthResponse, AuthenticatedUser } from "../../features/auth/types";
import type {
  ApiErrorPayload,
  ChangePasswordPayload,
  CreatePaymentPayload,
  ClientSummary,
  CompanyResponse,
  InvoiceDetail,
  InvoiceSummary,
  ItemSummary,
  ListClientsParams,
  ListInvoicesParams,
  ListItemsParams,
  PaymentRecord,
  PagedResponse,
  ReportsSummary,
  UpsertClientPayload,
  UpsertInvoicePayload,
  UpsertItemPayload,
  UpdateCompanyPayload
} from "./types";

type SessionGetter = () => SessionData | null;
type SessionSetter = (session: SessionData | null) => void;

export class ApiClientError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export class DesktopApiClient {
  private refreshPromise: Promise<SessionData | null> | null = null;

  constructor(
    private readonly baseUrl: string,
    private readonly getSession: SessionGetter,
    private readonly setSession: SessionSetter
  ) {}

  async login(payload: LoginPayload): Promise<SessionData> {
    const response = await this.rawRequest<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    const session = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user
    };

    this.setSession(session);

    return session;
  }

  async restoreSession(): Promise<SessionData | null> {
    const session = this.getSession();

    if (!session) {
      return null;
    }

    try {
      return await this.refreshSession();
    } catch {
      this.setSession(null);
      return null;
    }
  }

  async logout(): Promise<void> {
    const session = this.getSession();

    if (session) {
      try {
        await this.rawRequest<void>("/auth/logout", {
          method: "POST",
          body: JSON.stringify({ refreshToken: session.refreshToken }),
          headers: {
            Authorization: `Bearer ${session.accessToken}`
          }
        });
      } catch {
        // Best-effort logout.
      }
    }

    this.setSession(null);
  }

  async getCurrentUser(): Promise<AuthenticatedUser> {
    return this.request<AuthenticatedUser>("/auth/me");
  }

  async getReportsSummary(): Promise<ReportsSummary> {
    return this.request<ReportsSummary>("/reports/summary");
  }

  async getCurrentCompany(): Promise<CompanyResponse> {
    return this.request<CompanyResponse>("/company");
  }

  async updateCurrentCompany(payload: UpdateCompanyPayload): Promise<CompanyResponse> {
    return this.request<CompanyResponse>("/company", {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  }

  async changeOwnPassword(payload: ChangePasswordPayload): Promise<void> {
    await this.request<void>("/users/me/change-password", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  async listClients(params: ListClientsParams): Promise<PagedResponse<ClientSummary>> {
    const query = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
      sort: params.sort
    });

    if (params.status) {
      query.set("status", params.status);
    }

    if (params.search && params.search.trim()) {
      query.set("search", params.search.trim());
    }

    return this.request<PagedResponse<ClientSummary>>(`/clients?${query.toString()}`);
  }

  async createClient(payload: UpsertClientPayload): Promise<ClientSummary> {
    return this.request<ClientSummary>("/clients", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  async updateClient(clientId: string, payload: UpsertClientPayload): Promise<ClientSummary> {
    return this.request<ClientSummary>(`/clients/${clientId}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  }

  async listItems(params: ListItemsParams): Promise<PagedResponse<ItemSummary>> {
    const query = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
      sort: params.sort
    });

    if (params.status) {
      query.set("status", params.status);
    }

    if (params.search && params.search.trim()) {
      query.set("search", params.search.trim());
    }

    return this.request<PagedResponse<ItemSummary>>(`/items?${query.toString()}`);
  }

  async createItem(payload: UpsertItemPayload): Promise<ItemSummary> {
    return this.request<ItemSummary>("/items", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  async updateItem(itemId: string, payload: UpsertItemPayload): Promise<ItemSummary> {
    return this.request<ItemSummary>(`/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  }

  async deleteItem(itemId: string): Promise<void> {
    await this.request<void>(`/items/${itemId}`, {
      method: "DELETE"
    });
  }

  async listInvoices(params: ListInvoicesParams): Promise<PagedResponse<InvoiceSummary>> {
    const query = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
      sort: params.sort
    });

    if (params.status) {
      query.set("status", params.status);
    }

    if (params.paymentStatus) {
      query.set("paymentStatus", params.paymentStatus);
    }

    if (params.search && params.search.trim()) {
      query.set("search", params.search.trim());
    }

    return this.request<PagedResponse<InvoiceSummary>>(`/invoices?${query.toString()}`);
  }

  async getInvoice(invoiceId: string): Promise<InvoiceDetail> {
    return this.request<InvoiceDetail>(`/invoices/${invoiceId}`);
  }

  async createInvoice(payload: UpsertInvoicePayload): Promise<InvoiceDetail> {
    return this.request<InvoiceDetail>("/invoices", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  async updateInvoice(invoiceId: string, payload: UpsertInvoicePayload): Promise<InvoiceDetail> {
    return this.request<InvoiceDetail>(`/invoices/${invoiceId}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  }

  async issueInvoice(invoiceId: string): Promise<InvoiceDetail> {
    return this.request<InvoiceDetail>(`/invoices/${invoiceId}/issue`, {
      method: "POST"
    });
  }

  async createInvoiceCreditNote(invoiceId: string): Promise<InvoiceDetail> {
    return this.request<InvoiceDetail>(`/invoices/${invoiceId}/credit-note`, {
      method: "POST"
    });
  }

  async listInvoicePayments(invoiceId: string): Promise<PaymentRecord[]> {
    return this.request<PaymentRecord[]>(`/invoices/${invoiceId}/payments`);
  }

  async createInvoicePayment(invoiceId: string, payload: CreatePaymentPayload): Promise<PaymentRecord> {
    return this.request<PaymentRecord>(`/invoices/${invoiceId}/payments`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  private async request<T>(path: string, init?: RequestInit, retry = true): Promise<T> {
    const session = this.getSession();

    if (!session) {
      throw new ApiClientError(401, "Session is missing.");
    }

    const response = await fetch(this.url(path), {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {}),
        Authorization: `Bearer ${session.accessToken}`
      }
    });

    if (response.status === 401 && retry) {
      const nextSession = await this.refreshSession();

      if (!nextSession) {
        throw new ApiClientError(401, "Session expired.");
      }

      return this.request<T>(path, init, false);
    }

    return this.parseResponse<T>(response);
  }

  private async refreshSession(): Promise<SessionData | null> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    const session = this.getSession();

    if (!session) {
      return null;
    }

    this.refreshPromise = this.rawRequest<AuthResponse>("/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken: session.refreshToken })
    })
      .then((response) => {
        const nextSession = {
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          user: response.user
        };

        this.setSession(nextSession);
        return nextSession;
      })
      .catch(() => {
        this.setSession(null);
        return null;
      })
      .finally(() => {
        this.refreshPromise = null;
      });

    return this.refreshPromise;
  }

  private async rawRequest<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(this.url(path), {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {})
      }
    });

    return this.parseResponse<T>(response);
  }

  private async parseResponse<T>(response: Response): Promise<T> {
    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    const data = text ? (JSON.parse(text) as T | ApiErrorPayload) : ({} as T);

    if (!response.ok) {
      const message =
        typeof data === "object" && data && "message" in data && typeof data.message === "string"
          ? data.message
          : "Request failed.";
      throw new ApiClientError(response.status, message);
    }

    return data as T;
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }
}
