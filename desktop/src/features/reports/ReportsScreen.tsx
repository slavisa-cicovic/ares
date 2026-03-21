import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Card,
  CardContent,
  Paper,
  Stack,
  Typography
} from "@mui/material";
import {
  DateRangeFilter,
  FiltersResult,
  formatDateRangePresetLabel,
  type DateRangePreset,
  type FilterItem
} from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { ReportsSummary } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";

type SummaryCardConfig = {
  key: keyof ReportsSummary;
  label: string;
  currency?: boolean;
};

const primaryCards: SummaryCardConfig[] = [
  { key: "invoiceCount", label: "Invoices" },
  { key: "totalInvoicedAmount", label: "Total Invoiced", currency: true },
  { key: "totalPaidAmount", label: "Total Paid", currency: true },
  { key: "totalRefundedAmount", label: "Total Refunded", currency: true },
  { key: "totalOutstandingAmount", label: "Outstanding", currency: true },
  { key: "unsettledRefundAmount", label: "Unsettled Refund", currency: true }
];

const breakdownCards: SummaryCardConfig[] = [
  { key: "unpaidInvoiceCount", label: "Unpaid" },
  { key: "partiallyPaidInvoiceCount", label: "Partially Paid" },
  { key: "paidInvoiceCount", label: "Paid" }
];

function formatMoney(value: number): string {
  return new Intl.NumberFormat("sr-RS", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

function formatMetric(value: number, currency?: boolean): string {
  if (currency) {
    return `${formatMoney(value)} RSD`;
  }

  return new Intl.NumberFormat("sr-RS").format(value);
}

export function ReportsScreen() {
  const { getReportsSummary } = useAuth();
  const [dateRangePreset, setDateRangePreset] = useState<DateRangePreset | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [summary, setSummary] = useState<ReportsSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const hasInvalidDateRange = Boolean(from && to && from > to);
  const filters: FilterItem[] = [
    ...(dateRangePreset !== null && dateRangePreset !== "custom" && from && to
      ? [{
          label: "Range",
          value: formatDateRangePresetLabel(dateRangePreset),
          onRemove: () => {
            setDateRangePreset(null);
            setFrom("");
            setTo("");
          }
        }]
      : []),
    ...(dateRangePreset === "custom" && from
      ? [{
          label: "From",
          value: formatDateLabel(from),
          onRemove: () => {
            setFrom("");
          }
        }]
      : []),
    ...(dateRangePreset === "custom" && to
      ? [{
          label: "To",
          value: formatDateLabel(to),
          onRemove: () => {
            setTo("");
          }
        }]
      : [])
  ];

  useEffect(() => {
    let active = true;

    setIsLoading(true);
    setError(null);

    if (hasInvalidDateRange) {
      setError("From date must be earlier than or equal to To date.");
      setSummary(null);
      setIsLoading(false);
      return () => {
        active = false;
      };
    }

    void getReportsSummary({
      from: from || undefined,
      to: to || undefined
    })
      .then((response) => {
        if (active) {
          setSummary(response);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError instanceof ApiClientError
              ? requestError.message
              : "Unable to load reports summary right now."
          );
          setSummary(null);
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [from, getReportsSummary, hasInvalidDateRange, to]);

  return (
    <Stack spacing={3}>
      <Box sx={{ display: "grid", gap: 0.75 }}>
        <Typography color="primary.main" variant="overline">
          Reports
        </Typography>
        <Typography variant="h3">Financial summary</Typography>
        <Typography color="text.secondary" variant="body1">
          Track invoiced, paid, refunded, and outstanding amounts across the current company.
        </Typography>
      </Box>

      <Paper sx={{ p: 3 }} variant="outlined">
        <Stack spacing={3}>
          <DateRangeFilter
            from={from}
            onFromChange={setFrom}
            onPresetChange={setDateRangePreset}
            onToChange={setTo}
            preset={dateRangePreset}
            to={to}
          />

          <FiltersResult
            filters={filters}
            onReset={() => {
              setDateRangePreset(null);
              setFrom("");
              setTo("");
            }}
            total={summary?.invoiceCount ?? 0}
          />

          {error ? <Alert severity="error">{error}</Alert> : null}

          {isLoading ? (
            <Typography color="text.secondary" variant="body2">
              Loading reports summary...
            </Typography>
          ) : null}

          {!isLoading && summary ? (
            <>
              <Box
                sx={{
                  display: "grid",
                  gap: 2,
                  gridTemplateColumns: {
                    xs: "1fr",
                    sm: "repeat(2, minmax(0, 1fr))",
                    xl: "repeat(3, minmax(0, 1fr))"
                  }
                }}
              >
                {primaryCards.map((card) => (
                  <SummaryCard
                    key={card.key}
                    label={card.label}
                    value={formatMetric(summary[card.key] as number, card.currency)}
                  />
                ))}
              </Box>

              <Box
                sx={{
                  display: "grid",
                  gap: 2,
                  gridTemplateColumns: {
                    xs: "1fr",
                    md: "repeat(3, minmax(0, 1fr))"
                  }
                }}
              >
                {breakdownCards.map((card) => (
                  <SummaryCard
                    key={card.key}
                    label={card.label}
                    value={formatMetric(summary[card.key] as number)}
                  />
                ))}
              </Box>
            </>
          ) : null}
        </Stack>
      </Paper>
    </Stack>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={1}>
          <Typography color="text.secondary" variant="body2">
            {label}
          </Typography>
          <Typography variant="h4">{value}</Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function formatDateLabel(value: string): string {
  return new Intl.DateTimeFormat("sr-RS").format(new Date(`${value}T00:00:00`));
}
