import type { ReactNode } from "react";
import {
  FormControl,
  FormHelperText,
  InputLabel,
  Select
} from "@mui/material";
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
      render={({ field, fieldState: { error } }) => {
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");
        const labelId = `${name}-label`;

        return (
          <FormControl className={className} error={Boolean(error)} fullWidth required={required}>
            {label ? <InputLabel id={labelId}>{label}</InputLabel> : null}
            <Select
              disabled={disabled}
              label={label}
              labelId={label ? labelId : undefined}
              onBlur={field.onBlur}
              onChange={(event) => field.onChange(event.target.value)}
              value={field.value ?? ""}
            >
              {children}
            </Select>
            <FormHelperText>{resolvedHelperText}</FormHelperText>
          </FormControl>
        );
      }}
    />
  );
}
