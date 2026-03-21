import type { ChangeEvent } from "react";
import { Box, FormControl, FormHelperText, Stack, TextField, Typography } from "@mui/material";
import { Controller, useFormContext } from "react-hook-form";

type RHFColorPickerProps = {
  name: string;
  label?: string;
  helperText?: string;
  disabled?: boolean;
  required?: boolean;
  className?: string;
};

function normalizeColorValue(value: unknown): string {
  return typeof value === "string" && value.trim() ? value.trim().toUpperCase() : "#000000";
}

export default function RHFColorPicker({
  name,
  label,
  helperText,
  disabled,
  required,
  className
}: RHFColorPickerProps) {
  const { control, setValue } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");
        const normalizedValue = normalizeColorValue(field.value);

        return (
          <FormControl className={className} error={Boolean(error)} fullWidth required={required}>
            {label ? (
              <Typography sx={{ mb: 1 }} variant="body2">
                {label}
              </Typography>
            ) : null}

            <Stack alignItems="center" direction="row" spacing={2}>
              <Box
                sx={{
                  border: "1px solid",
                  borderColor: error ? "error.main" : "divider",
                  borderRadius: 2,
                  height: 56,
                  overflow: "hidden",
                  width: 72
                }}
              >
                <Box
                  component="input"
                  disabled={disabled}
                  onBlur={field.onBlur}
                  onChange={(event: ChangeEvent<HTMLInputElement>) => {
                    const nextValue = event.target.value.toUpperCase();
                    setValue(name, nextValue, {
                      shouldDirty: true,
                      shouldTouch: true,
                      shouldValidate: true
                    });
                  }}
                  sx={{
                    appearance: "none",
                    background: "none",
                    border: 0,
                    cursor: disabled ? "default" : "pointer",
                    display: "block",
                    height: "100%",
                    p: 0,
                    width: "100%"
                  }}
                  type="color"
                  value={normalizedValue}
                />
              </Box>

              <TextField
                disabled={disabled}
                fullWidth
                label="Hex Color"
                onBlur={field.onBlur}
                onChange={(event) => field.onChange(event.target.value.toUpperCase())}
                placeholder="#B7844F"
                value={typeof field.value === "string" ? field.value : ""}
              />
            </Stack>

            <FormHelperText>{resolvedHelperText}</FormHelperText>
          </FormControl>
        );
      }}
    />
  );
}
