import { z } from "zod";

export const clientFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required.")
    .max(255, "Name must be at most 255 characters."),
  pib: z
    .string()
    .trim()
    .min(1, "PIB is required.")
    .max(32, "PIB must be at most 32 characters."),
  mb: z
    .string()
    .trim()
    .min(1, "MB is required.")
    .max(32, "MB must be at most 32 characters."),
  email: z
    .string()
    .trim()
    .max(320, "Email must be at most 320 characters.")
    .refine((value) => !value || z.email().safeParse(value).success, {
      message: "Email is not valid."
    }),
  phone: z.string().trim().max(50, "Phone must be at most 50 characters."),
  address: z.string().trim().max(255, "Address must be at most 255 characters."),
  city: z.string().trim().max(120, "City must be at most 120 characters."),
  postalCode: z.string().trim().max(20, "Postal code must be at most 20 characters."),
  country: z.string().trim().max(120, "Country must be at most 120 characters."),
  active: z.boolean()
});

export type ClientFormValues = z.infer<typeof clientFormSchema>;
