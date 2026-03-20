import { useEffect, useMemo, useState, type ChangeEvent } from "react";
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
    <div className={`rhf-logo-picker${className ? ` ${className}` : ""}`}>
      {logoError ? <div className="error-banner inline-banner">{logoError}</div> : null}

      <div className="settings-upload-placeholder">
        <div>
          <strong>{title ?? "Company Logo"}</strong>
          <p>
            {description ??
              "Select an image file to use later in branding, PDF templates, and exports."}
          </p>
        </div>

        <div className="rhf-logo-picker-layout">
          <div className="rhf-logo-preview">
            {previewSource ? (
              <img alt="Selected logo preview" src={previewSource} />
            ) : (
              <span>No logo selected.</span>
            )}
          </div>

          <div className="rhf-logo-actions">
            <span className="muted-note">
              {logoValue instanceof File
                ? logoValue.name
                : typeof logoValue === "string" && logoValue
                  ? logoValue
                  : "Choose a PNG, JPG, WEBP, or SVG image."}
            </span>

            <div className="settings-actions">
              <label className="secondary-button rhf-file-button">
                <input
                  accept="image/*"
                  disabled={disabled}
                  onChange={handleFileChange}
                  type="file"
                />
                Select
              </label>

              <button
                className="secondary-button"
                disabled={disabled || !logoValue}
                onClick={handleRemoveLogo}
                type="button"
              >
                Remove
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
