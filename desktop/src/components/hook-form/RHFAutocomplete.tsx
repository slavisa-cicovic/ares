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
  const listId = `${name}-options`;

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const selectedOption = options.find((option) => getOptionValue(option) === field.value);
        const inputValue = selectedOption ? getOptionLabel(selectedOption) : "";
        const resolvedHelperText = error ? error.message : (helperText ?? "\u00A0");

        return (
          <label className={`field rhf-field${className ? ` ${className}` : ""}`}>
            {label ? <span>{label}</span> : null}

            <input
              defaultValue={inputValue}
              disabled={disabled}
              list={listId}
              onBlur={field.onBlur}
              onChange={(event) => {
                const nextOption = options.find(
                  (option) => getOptionLabel(option).toLowerCase() === event.target.value.toLowerCase()
                );

                setValue(name, nextOption ? getOptionValue(nextOption) : null, {
                  shouldValidate: true,
                  shouldDirty: true
                });
              }}
              placeholder={placeholder}
              ref={field.ref}
            />

            <datalist id={listId}>
              {options.map((option) => (
                <option key={String(getOptionValue(option))} value={getOptionLabel(option)} />
              ))}
            </datalist>

            <small className={`rhf-helper-text${error ? " error" : ""}`}>
              {resolvedHelperText}
            </small>
          </label>
        );
      }}
    />
  );
}
