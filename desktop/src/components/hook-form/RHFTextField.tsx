import { useState, type HTMLInputTypeAttribute } from "react";
import VisibilityOffRoundedIcon from "@mui/icons-material/VisibilityOffRounded";
import VisibilityRoundedIcon from "@mui/icons-material/VisibilityRounded";
import { IconButton, InputAdornment, TextField, type TextFieldProps } from "@mui/material";
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
  size?: TextFieldProps["size"];
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
  size,
  className
}: RHFTextFieldProps) {
  const { control } = useFormContext();
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const isPasswordField = type === "password";

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => {
        const normalizedValue = field.value == null ? "" : String(field.value);
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
            multiline={multiline}
            onBlur={field.onBlur}
            onChange={(event) => field.onChange(event.target.value)}
            placeholder={placeholder}
            required={required}
            rows={multiline ? rows : undefined}
            size={size}
            slotProps={{
              htmlInput: { maxLength },
              input: isPasswordField
                ? {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={isPasswordVisible ? "Hide password" : "Show password"}
                          edge="end"
                          onClick={() => setIsPasswordVisible((current) => !current)}
                          onMouseDown={(event) => event.preventDefault()}
                          size="small"
                          tabIndex={-1}
                        >
                          {isPasswordVisible ? <VisibilityOffRoundedIcon fontSize="small" /> : <VisibilityRoundedIcon fontSize="small" />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }
                : undefined
            }}
            type={isPasswordField && isPasswordVisible ? "text" : type}
            value={normalizedValue}
            variant="outlined"
          />
        );
      }}
    />
  );
}
