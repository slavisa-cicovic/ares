import { z } from "zod";

function isNonNegativeDecimal(value: string): boolean {
  if (!/^\d+(\.\d{1,2})?$/.test(value)) {
    return false;
  }

  return Number.parseFloat(value) >= 0;
}

function isPositiveQuantity(value: string): boolean {
  if (!/^\d+(\.\d{1,3})?$/.test(value)) {
    return false;
  }

  return Number.parseFloat(value) >= 0.001;
}

export const itemFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required.")
    .max(255, "Name must be at most 255 characters."),
  code: z
    .string()
    .trim()
    .max(64, "Code must be at most 64 characters."),
  description: z
    .string()
    .trim()
    .max(1000, "Description must be at most 1000 characters."),
  unit: z
    .string()
    .trim()
    .min(1, "Unit is required.")
    .max(50, "Unit must be at most 50 characters."),
  price: z
    .string()
    .trim()
    .min(1, "Price is required.")
    .refine(isNonNegativeDecimal, "Price must be a valid non-negative amount."),
  taxRate: z
    .string()
    .trim()
    .min(1, "Tax rate is required.")
    .refine(isNonNegativeDecimal, "Tax rate must be a valid non-negative amount.")
    .refine((value) => Number.parseFloat(value) <= 100, "Tax rate must be at most 100."),
  defaultValue: z
    .string()
    .trim()
    .min(1, "Default value is required.")
    .refine(isPositiveQuantity, "Default value must be at least 0.001."),
  active: z.boolean()
});

export type ItemFormValues = z.infer<typeof itemFormSchema>;
