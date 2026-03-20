import { TextField } from "@mui/material";
import { Controller, useFormContext } from "react-hook-form";

type RHFDatePickerProps = {
  name: string;
  label: string;
  disabled?: boolean;
  helperText?: string;
  min?: string;
  max?: string;
  className?: string;
};

export default function RHFDatePicker({
  name,
  label,
  disabled = false,
  helperText,
  min,
  max,
  className
}: RHFDatePickerProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");

        return (
          <TextField
            className={className}
            disabled={disabled}
            error={Boolean(error)}
            fullWidth
            helperText={resolvedHelperText}
            inputRef={field.ref}
            label={label}
            onBlur={field.onBlur}
            onChange={(event) => field.onChange(event.target.value)}
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { min, max }
            }}
            type="date"
            value={(field.value as string) ?? ""}
          />
        );
      }}
    />
  );
}
