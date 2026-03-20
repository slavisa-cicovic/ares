import type { HTMLInputTypeAttribute } from "react";
import { TextField } from "@mui/material";
import { Controller, useFormContext } from "react-hook-form";

type RHFTextFieldProps = {
  name: string;
  label?: string;
  helperText?: string;
  placeholder?: string;
  type?: HTMLInputTypeAttribute;
  disabled?: boolean;
  required?: boolean;
  maxLength?: number;
  multiline?: boolean;
  rows?: number;
  className?: string;
};

export default function RHFTextField({
  name,
  label,
  helperText,
  placeholder,
  type = "text",
  disabled,
  required,
  maxLength,
  multiline = false,
  rows = 3,
  className
}: RHFTextFieldProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const normalizedValue = field.value == null ? "" : String(field.value);
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
            multiline={multiline}
            onBlur={field.onBlur}
            onChange={(event) => field.onChange(event.target.value)}
            placeholder={placeholder}
            required={required}
            rows={multiline ? rows : undefined}
            slotProps={{ htmlInput: { maxLength } }}
            type={type}
            value={normalizedValue}
            variant="outlined"
          />
        );
      }}
    />
  );
}
