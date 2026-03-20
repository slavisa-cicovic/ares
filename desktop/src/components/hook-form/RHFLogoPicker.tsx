import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Alert, Box, Button, Paper, Stack, Typography } from "@mui/material";
import { useFormContext } from "react-hook-form";

type RHFLogoPickerProps = {
  name: string;
  title?: string;
  description?: string;
  disabled?: boolean;
  className?: string;
};

function toPreviewSource(value: unknown): string | null {
  if (typeof value === "string" && value.trim()) {
    return value;
  }

  if (value instanceof File) {
    return URL.createObjectURL(value);
  }

  return null;
}

export default function RHFLogoPicker({
  name,
  title,
  description,
  disabled = false,
  className
}: RHFLogoPickerProps) {
  const { watch, setValue } = useFormContext();
  const logoValue = watch(name) as string | File | null | undefined;
  const [logoError, setLogoError] = useState<string | null>(null);
  const previewSource = useMemo(() => toPreviewSource(logoValue), [logoValue]);

  useEffect(() => {
    return () => {
      if (previewSource?.startsWith("blob:")) {
        URL.revokeObjectURL(previewSource);
      }
    };
  }, [previewSource]);

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setLogoError(null);

    const file = event.target.files?.[0] ?? null;

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      setLogoError("Selected file must be an image.");
      return;
    }

    setValue(name, file, {
      shouldDirty: true,
      shouldTouch: true,
      shouldValidate: true
    });
  }

  function handleRemoveLogo() {
    setLogoError(null);
    setValue(name, null, {
      shouldDirty: true,
      shouldTouch: true,
      shouldValidate: true
    });
  }

  return (
    <Stack className={className} spacing={2}>
      {logoError ? <Alert severity="error">{logoError}</Alert> : null}

      <Paper sx={{ borderRadius: 3, p: 2.5 }} variant="outlined">
        <Stack spacing={2}>
          <Box>
            <Typography variant="subtitle1">{title ?? "Company Logo"}</Typography>
            <Typography color="text.secondary" variant="body2">
              {description ??
                "Select an image file to use later in branding, PDF templates, and exports."}
            </Typography>
          </Box>

          <div className="rhf-logo-picker-layout">
            <div className="rhf-logo-preview">
              {previewSource ? (
                <img alt="Selected logo preview" src={previewSource} />
              ) : (
                <span>No logo selected.</span>
              )}
            </div>

            <div className="rhf-logo-actions">
              <Typography color="text.secondary" variant="body2">
                {logoValue instanceof File
                  ? logoValue.name
                  : typeof logoValue === "string" && logoValue
                    ? logoValue
                    : "Choose a PNG, JPG, WEBP, or SVG image."}
              </Typography>

              <Stack direction="row" spacing={2}>
                <Button component="label" disabled={disabled} variant="outlined">
                  Select
                  <input
                    accept="image/*"
                    hidden
                    onChange={handleFileChange}
                    type="file"
                  />
                </Button>

                <Button
                  disabled={disabled || !logoValue}
                  onClick={handleRemoveLogo}
                  variant="text"
                >
                  Remove
                </Button>
              </Stack>
            </div>
          </div>
        </Stack>
      </Paper>
    </Stack>
  );
}
