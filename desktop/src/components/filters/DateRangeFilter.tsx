import { Stack, TextField } from "@mui/material";

type DateRangeFilterProps = {
  from: string;
  to: string;
  onFromChange: (value: string) => void;
  onToChange: (value: string) => void;
};

export function DateRangeFilter({ from, to, onFromChange, onToChange }: DateRangeFilterProps) {
  return (
    <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
      <TextField
        InputLabelProps={{ shrink: true }}
        label="From"
        onChange={(event) => onFromChange(event.target.value)}
        type="date"
        value={from}
      />

      <TextField
        InputLabelProps={{ shrink: true }}
        label="To"
        onChange={(event) => onToChange(event.target.value)}
        type="date"
        value={to}
      />
    </Stack>
  );
}
