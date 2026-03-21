import { FormControl, InputLabel, MenuItem, Select, Stack, TextField, type SelectChangeEvent } from "@mui/material";

export type DateRangePreset =
  | "previous_year"
  | "previous_quarter"
  | "previous_month"
  | "previous_week"
  | "yesterday"
  | "current_year"
  | "current_quarter"
  | "current_month"
  | "current_week"
  | "today"
  | "custom";

type DateRangeFilterProps = {
  preset: DateRangePreset | null;
  from: string;
  to: string;
  onPresetChange: (value: DateRangePreset | null) => void;
  onFromChange: (value: string) => void;
  onToChange: (value: string) => void;
};

const PRESET_OPTIONS: Array<{ value: DateRangePreset | null; label: string }> = [
  { value: null, label: "All time" },
  { value: "previous_year", label: "Previous year" },
  { value: "previous_quarter", label: "Previous quarter" },
  { value: "previous_month", label: "Previous month" },
  { value: "previous_week", label: "Previous week" },
  { value: "yesterday", label: "Yesterday" },
  { value: "current_year", label: "Current year" },
  { value: "current_quarter", label: "Current quarter" },
  { value: "current_month", label: "Current month" },
  { value: "current_week", label: "Current week" },
  { value: "today", label: "Today" },
  { value: "custom", label: "Custom" }
];

export function formatDateRangePresetLabel(preset: DateRangePreset | null): string {
  return PRESET_OPTIONS.find((option) => option.value === preset)?.label ?? "Custom";
}

export function resolveDateRangePreset(preset: Exclude<DateRangePreset, "custom">, now = new Date()): { from: string; to: string } {
  const today = atStartOfDay(now);

  switch (preset) {
    case "today":
      return singleDay(today);
    case "yesterday":
      return singleDay(addDays(today, -1));
    case "current_week": {
      const start = startOfWeek(today);
      return range(start, today);
    }
    case "previous_week": {
      const end = addDays(startOfWeek(today), -1);
      return range(startOfWeek(end), end);
    }
    case "current_month": {
      const start = new Date(today.getFullYear(), today.getMonth(), 1);
      return range(start, today);
    }
    case "previous_month": {
      const start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      const end = new Date(today.getFullYear(), today.getMonth(), 0);
      return range(start, end);
    }
    case "current_quarter": {
      const start = startOfQuarter(today);
      return range(start, today);
    }
    case "previous_quarter": {
      const currentQuarterStart = startOfQuarter(today);
      const previousQuarterEnd = addDays(currentQuarterStart, -1);
      return range(startOfQuarter(previousQuarterEnd), previousQuarterEnd);
    }
    case "current_year": {
      const start = new Date(today.getFullYear(), 0, 1);
      return range(start, today);
    }
    case "previous_year": {
      const start = new Date(today.getFullYear() - 1, 0, 1);
      const end = new Date(today.getFullYear() - 1, 11, 31);
      return range(start, end);
    }
  }
}

export function DateRangeFilter({
  preset,
  from,
  to,
  onPresetChange,
  onFromChange,
  onToChange
}: DateRangeFilterProps) {
  function handlePresetChange(event: SelectChangeEvent<string>) {
    const value = event.target.value === "" ? null : (event.target.value as DateRangePreset);
    onPresetChange(value);

    if (value === null) {
      onFromChange("");
      onToChange("");
      return;
    }

    if (value === "custom") {
      return;
    }

    const nextRange = resolveDateRangePreset(value);
    onFromChange(nextRange.from);
    onToChange(nextRange.to);
  }

  return (
    <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
      <FormControl sx={{ minWidth: 220 }}>
        <InputLabel id="date-range-preset-label">Date range</InputLabel>
        <Select
          label="Date range"
          labelId="date-range-preset-label"
          onChange={handlePresetChange}
          value={preset ?? ""}
        >
          {PRESET_OPTIONS.map((option) => (
            <MenuItem key={option.value ?? "all_time"} value={option.value ?? ""}>
              {option.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {preset === "custom" ? (
        <>
          <TextField
            label="From"
            onChange={(event) => onFromChange(event.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            type="date"
            value={from}
          />

          <TextField
            label="To"
            onChange={(event) => onToChange(event.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            type="date"
            value={to}
          />
        </>
      ) : null}
    </Stack>
  );
}

function range(from: Date, to: Date): { from: string; to: string } {
  return {
    from: formatDate(from),
    to: formatDate(to)
  };
}

function singleDay(day: Date): { from: string; to: string } {
  const value = formatDate(day);
  return { from: value, to: value };
}

function startOfWeek(date: Date): Date {
  const day = date.getDay();
  const distanceFromMonday = day === 0 ? 6 : day - 1;
  return addDays(date, -distanceFromMonday);
}

function startOfQuarter(date: Date): Date {
  const quarterMonth = Math.floor(date.getMonth() / 3) * 3;
  return new Date(date.getFullYear(), quarterMonth, 1);
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return atStartOfDay(next);
}

function atStartOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}
