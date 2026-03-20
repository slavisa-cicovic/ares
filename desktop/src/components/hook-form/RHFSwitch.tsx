import type { ReactNode } from "react";
import { FormControl, FormControlLabel, FormHelperText, Switch } from "@mui/material";
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
          <FormControl className={className} error={Boolean(error)} fullWidth>
            <FormControlLabel
              control={
                <Switch
                  checked={Boolean(field.value)}
                  disabled={disabled}
                  onBlur={field.onBlur}
                  onChange={(event) => field.onChange(event.target.checked)}
                  slotProps={{
                    input: {
                      ref: field.ref
                    }
                  }}
                />
              }
              label={`${label}: ${Boolean(field.value) ? "Active" : "Inactive"}`}
              sx={{
                minHeight: 56,
                m: 0
              }}
            />
            <FormHelperText>{resolvedHelperText}</FormHelperText>
          </FormControl>
        );
      }}
    />
  );
}
