import { z } from "zod";

export const invoiceLineSchema = z.object({
  itemId: z.string().nullable(),
  itemName: z
    .string()
    .trim()
    .min(1, "Item name is required.")
    .max(255, "Item name must be at most 255 characters."),
  itemCode: z
    .string()
    .trim()
    .max(64, "Item code must be at most 64 characters."),
  unit: z
    .string()
    .trim()
    .min(1, "Unit is required.")
    .max(50, "Unit must be at most 50 characters."),
  quantity: z
    .string()
    .trim()
    .min(1, "Quantity is required.")
    .refine((value) => Number(value.replace(",", ".")) > 0, "Quantity must be greater than zero."),
  unitPrice: z
    .string()
    .trim()
    .min(1, "Unit price is required.")
    .refine((value) => Number(value.replace(",", ".")) >= 0, "Unit price must not be negative."),
  taxRate: z
    .string()
    .trim()
    .min(1, "Tax rate is required.")
    .refine((value) => Number(value.replace(",", ".")) >= 0, "Tax rate must not be negative.")
});

export const invoiceDraftSchema = z.object({
  clientId: z.string().trim().min(1, "Client is required."),
  issueDate: z.string().trim().min(1, "Issue date is required."),
  dueDate: z.string().trim().min(1, "Due date is required."),
  currency: z
    .string()
    .trim()
    .length(3, "Currency must be exactly 3 characters.")
    .transform((value) => value.toUpperCase()),
  note: z.string().trim().max(1000, "Note must be at most 1000 characters."),
  lines: z.array(invoiceLineSchema).min(1, "At least one invoice line is required.")
}).superRefine((value, ctx) => {
  if (value.issueDate && value.dueDate && value.dueDate < value.issueDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: "Due date must be on or after issue date.",
      path: ["dueDate"]
    });
  }
});

export type InvoiceDraftFormValues = z.infer<typeof invoiceDraftSchema>;
export type InvoiceLineFormValues = z.infer<typeof invoiceLineSchema>;
