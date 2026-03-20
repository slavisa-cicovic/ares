import type { HTMLInputTypeAttribute } from "react";
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

        return (
          <label className={`field rhf-field${className ? ` ${className}` : ""}`}>
            {label ? <span>{label}</span> : null}

            {multiline ? (
              <textarea
                disabled={disabled}
                maxLength={maxLength}
                onBlur={field.onBlur}
                onChange={(event) => field.onChange(event.target.value)}
                placeholder={placeholder}
                ref={field.ref}
                required={required}
                rows={rows}
                value={normalizedValue}
              />
            ) : (
              <input
                disabled={disabled}
                maxLength={maxLength}
                onBlur={field.onBlur}
                onChange={(event) => field.onChange(event.target.value)}
                placeholder={placeholder}
                ref={field.ref}
                required={required}
                type={type}
                value={normalizedValue}
              />
            )}

            {error?.message || helperText ? (
              <small className={`rhf-helper-text${error ? " error" : ""}`}>
                {error ? error.message : helperText}
              </small>
            ) : null}
          </label>
        );
      }}
    />
  );
}
