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
      render={({ field, fieldState: { error } }) => {
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");

        return (
          <label className={`field rhf-field rhf-switch${className ? ` ${className}` : ""}`}>
            <span>{label}</span>

            <div className="rhf-switch-control">
              <input
                checked={Boolean(field.value)}
                disabled={disabled}
                onBlur={field.onBlur}
                onChange={(event) => field.onChange(event.target.checked)}
                ref={field.ref}
                type="checkbox"
              />
              <span>{Boolean(field.value) ? "Active" : "Inactive"}</span>
            </div>

            <small className={`rhf-helper-text${error ? " error" : ""}`}>
              {resolvedHelperText}
            </small>
          </label>
        );
      }}
    />
  );
}
