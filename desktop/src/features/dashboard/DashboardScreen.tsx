import { useEffect, useState } from "react";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "../auth/AuthProvider";
import type { ReportsSummary } from "../../lib/api/types";

const summaryCards: Array<{
  key: keyof ReportsSummary;
  label: string;
  currency?: boolean;
}> = [
  { key: "totalInvoicedAmount", label: "Invoiced", currency: true },
  { key: "totalPaidAmount", label: "Paid", currency: true },
  { key: "totalOutstandingAmount", label: "Outstanding", currency: true },
  { key: "invoiceCount", label: "Invoices" },
  { key: "unpaidInvoiceCount", label: "Unpaid" },
  { key: "partiallyPaidInvoiceCount", label: "Partially Paid" },
  { key: "paidInvoiceCount", label: "Paid In Full" }
];

function formatMetric(value: number, currency?: boolean): string {
  if (!currency) {
    return new Intl.NumberFormat("en-US").format(value);
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "EUR",
    maximumFractionDigits: 2
  }).format(value);
}

export function DashboardScreen() {
  const { getReportsSummary } = useAuth();
  const [summary, setSummary] = useState<ReportsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    void getReportsSummary()
      .then((data) => {
        if (!active) {
          return;
        }

        setSummary(data);
        setLoading(false);
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }

        setError(
          requestError instanceof ApiClientError
            ? requestError.message
            : "Unable to load dashboard summary."
        );
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [getReportsSummary]);

  if (loading) {
    return <div className="screen-state">Loading dashboard summary...</div>;
  }

  if (!summary || error) {
    return <div className="screen-state error-banner">{error ?? "Summary is unavailable."}</div>;
  }

  return (
    <div className="dashboard-grid">
      {summaryCards.map((card) => (
        <article className="summary-card" key={card.key}>
          <span className="eyebrow">{card.label}</span>
          <strong>{formatMetric(summary[card.key] as number, card.currency)}</strong>
        </article>
      ))}
    </div>
  );
}
