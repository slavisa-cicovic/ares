import { z } from "zod";

const SIMPLE_EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function optionalText(maxLength: number, label: string) {
  return z
    .string()
    .trim()
    .max(maxLength, `${label} must be at most ${maxLength} characters.`);
}

function requiredText(maxLength: number, label: string) {
  return z
    .string()
    .trim()
    .min(1, `${label} is required.`)
    .max(maxLength, `${label} must be at most ${maxLength} characters.`);
}

export const companyCoreSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Company name is required.")
    .max(255, "Company name must be at most 255 characters."),
  pib: requiredText(64, "PIB"),
  mb: requiredText(64, "MB"),
  bankAccount: optionalText(128, "Bank account"),
  email: optionalText(320, "Email").refine(
    (value) => !value || SIMPLE_EMAIL_PATTERN.test(value),
    "Email must be a valid email address."
  ),
  phone: optionalText(64, "Phone"),
  address: optionalText(255, "Address"),
  postalCode: optionalText(32, "Postal code"),
  city: optionalText(128, "City"),
  country: optionalText(128, "Country")
});

export type CompanyCoreFormValues = z.infer<typeof companyCoreSchema>;
