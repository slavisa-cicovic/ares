import { InputAdornment, TextField } from "@mui/material";
import { Controller, useFormContext } from "react-hook-form";

type RHFNumberFieldProps = {
  name: string;
  label?: string;
  helperText?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  decimals?: number;
  suffix?: string;
  className?: string;
};

function toDigitString(value: unknown): string {
  if (typeof value !== "string") {
    return "";
  }

  return value.replace(/\D/g, "");
}

function formatDigits(digits: string, decimals: number): string {
  if (!digits) {
    return "";
  }

  if (decimals <= 0) {
    return String(Number.parseInt(digits, 10));
  }

  const paddedDigits = digits.padStart(decimals + 1, "0");
  const integerDigits = paddedDigits.slice(0, -decimals).replace(/^0+(?=\d)/, "") || "0";
  const fractionDigits = paddedDigits.slice(-decimals);

  return `${integerDigits}.${fractionDigits}`;
}

export default function RHFNumberField({
  name,
  label,
  helperText,
  placeholder,
  disabled,
  required,
  decimals = 0,
  suffix,
  className
}: RHFNumberFieldProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const normalizedValue = formatDigits(toDigitString(field.value), decimals);
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
            onChange={(event) => {
              const nextDigits = event.target.value.replace(/\D/g, "");
              field.onChange(formatDigits(nextDigits, decimals));
            }}
            placeholder={placeholder}
            required={required}
            slotProps={{
              htmlInput: { inputMode: "numeric" },
              input: {
                endAdornment: suffix ? <InputAdornment position="end">{suffix}</InputAdornment> : undefined
              }
            }}
            sx={{ "& input": { textAlign: "right" } }}
            value={normalizedValue}
          />
        );
      }}
    />
  );
}
