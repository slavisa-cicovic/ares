import { z } from "zod";

const HEX_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;
const themePresetSchema = z.enum(["amber", "ocean", "graphite"]);

export const companyBrandingSchema = z.object({
  logo: z.custom<string | File | null>(
    (value) => value === null || typeof value === "string" || value instanceof File,
    "Logo must be an image file."
  ),
  themePreset: themePresetSchema,
  brandColor: z
    .string()
    .trim()
    .refine((value) => !value || HEX_COLOR_PATTERN.test(value), "Brand color must be a valid hex color.")
});

export type CompanyBrandingFormValues = z.infer<typeof companyBrandingSchema>;
