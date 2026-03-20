import type { ReactNode } from "react";
import { Controller, useFormContext } from "react-hook-form";

type RHFSwitchProps = {
  name: string;
  label: string;
  helperText?: ReactNode;
  disabled?: boolean;
  className?: string;
};

export default function RHFSwitch({
  name,
  label,
  helperText,
  disabled,
  className
}: RHFSwitchProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => (
        <div className={`rhf-switch${className ? ` ${className}` : ""}`}>
          <label className="rhf-switch-control">
            <input
              checked={Boolean(field.value)}
              disabled={disabled}
              onBlur={field.onBlur}
              onChange={(event) => field.onChange(event.target.checked)}
              ref={field.ref}
              type="checkbox"
            />
            <span>{label}</span>
          </label>

          {error?.message || helperText ? (
            <small className={`rhf-helper-text${error ? " error" : ""}`}>
              {error ? error.message : helperText}
            </small>
          ) : null}
        </div>
      )}
    />
  );
}
