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
      render={({ field, fieldState: { error } }) => (
        <label className={`field rhf-field${className ? ` ${className}` : ""}`}>
          <span>{label}</span>

          <input
            disabled={disabled}
            max={max}
            min={min}
            onBlur={field.onBlur}
            onChange={(event) => field.onChange(event.target.value)}
            ref={field.ref}
            type="date"
            value={(field.value as string) ?? ""}
          />

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
