import { Autocomplete, TextField } from "@mui/material";
import { Controller, useFormContext } from "react-hook-form";

type RHFAutocompleteProps<TOption, TValue> = {
  name: string;
  label?: string;
  placeholder?: string;
  helperText?: string;
  options: TOption[];
  disabled?: boolean;
  className?: string;
  getOptionLabel: (option: TOption) => string;
  getOptionValue: (option: TOption) => TValue;
};

export default function RHFAutocomplete<TOption, TValue extends string | number>({
  name,
  label,
  placeholder,
  helperText,
  options,
  disabled,
  className,
  getOptionLabel,
  getOptionValue
}: RHFAutocompleteProps<TOption, TValue>) {
  const { control, setValue } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const selectedOption = options.find((option) => getOptionValue(option) === field.value);
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");

        return (
          <Autocomplete
            className={className}
            disabled={disabled}
            getOptionLabel={getOptionLabel}
            isOptionEqualToValue={(option, value) =>
              getOptionValue(option) === getOptionValue(value)
            }
            onChange={(_event, nextOption) => {
              setValue(name, nextOption ? getOptionValue(nextOption) : null, {
                shouldValidate: true,
                shouldDirty: true
              });
            }}
            options={options}
            renderInput={(params) => (
              <TextField
                {...params}
                error={Boolean(error)}
                helperText={resolvedHelperText}
                label={label}
                placeholder={placeholder}
              />
            )}
            value={selectedOption ?? null}
          />
        );
      }}
    />
  );
}
