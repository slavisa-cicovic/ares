import type { ReactNode } from "react";
import { Controller, useFormContext } from "react-hook-form";

type RHFSelectProps = {
  name: string;
  label?: string;
  helperText?: ReactNode;
  disabled?: boolean;
  required?: boolean;
  className?: string;
  children: ReactNode;
};

export default function RHFSelect({
  name,
  label,
  helperText,
  disabled,
  required,
  className,
  children
}: RHFSelectProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => (
        <label className={`field rhf-field${className ? ` ${className}` : ""}`}>
          {label ? <span>{label}</span> : null}

          <select
            disabled={disabled}
            onBlur={field.onBlur}
            onChange={(event) => field.onChange(event.target.value)}
            ref={field.ref}
            required={required}
            value={field.value ?? ""}
          >
            {children}
          </select>

          {error?.message || helperText ? (
            <small className={`rhf-helper-text${error ? " error" : ""}`}>
              {error ? error.message : helperText}
            </small>
          ) : null}
        </label>
      )}
    />
  );
}
