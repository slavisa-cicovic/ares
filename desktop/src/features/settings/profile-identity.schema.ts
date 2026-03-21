import { z } from "zod";

export const profileIdentitySchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Email is required.")
    .email("Email must be a valid email address."),
  firstName: z
    .string()
    .trim()
    .min(1, "First name is required.")
    .max(100, "First name must be 100 characters or fewer."),
  lastName: z
    .string()
    .trim()
    .min(1, "Last name is required.")
    .max(100, "Last name must be 100 characters or fewer."),
  phone: z
    .preprocess(
      (value) => (typeof value === "string" ? value : ""),
      z.string().trim().max(64, "Phone must be 64 characters or fewer.")
    ),
  profileLogo: z.any().nullable().optional()
});

export type ProfileIdentityFormInput = z.input<typeof profileIdentitySchema>;
export type ProfileIdentityFormValues = z.output<typeof profileIdentitySchema>;
