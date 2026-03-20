import type { SessionData, LoginPayload, AuthResponse, AuthenticatedUser } from "../../features/auth/types";
import type {
  ApiErrorPayload,
  CompanyResponse,
  ReportsSummary,
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
