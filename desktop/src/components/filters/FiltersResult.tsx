import { Box, Button, Chip, Stack, Typography } from "@mui/material";

export type FilterItem = {
  label: string;
  value: string;
  onRemove?: () => void;
};

type FiltersResultProps = {
  total: number;
  filters: FilterItem[];
  onReset: () => void;
};

export function FiltersResult({ total, filters, onReset }: FiltersResultProps) {
  if (filters.length === 0) {
    return null;
  }

  return (
    <Box sx={{ px: 0.5, pb: 0.5 }}>
      <Stack alignItems={{ sm: "center" }} direction={{ xs: "column", sm: "row" }} spacing={1.5}>
        <Typography color="text.secondary" variant="body2">
          Results: {total}
        </Typography>
        <Stack direction="row" flexWrap="wrap" spacing={1} useFlexGap>
          {filters.map((filter) => (
            <Chip
              key={`${filter.label}-${filter.value}`}
              label={`${filter.label}: ${filter.value}`}
              onDelete={filter.onRemove}
              size="small"
            />
          ))}
        </Stack>
        <Box sx={{ flexGrow: 1 }} />
        <Button color="inherit" onClick={onReset} size="small">
          Reset
        </Button>
      </Stack>
    </Box>
  );
}
