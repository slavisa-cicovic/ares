import { z } from "zod";

export const paymentFormSchema = z.object({
  paymentDate: z.string().trim().min(1, "Payment date is required."),
  amount: z
    .string()
    .trim()
    .min(1, "Amount is required.")
    .refine((value) => /^\d+(\.\d{1,2})?$/.test(value), "Amount must be a valid amount.")
    .refine((value) => Number.parseFloat(value) > 0, "Amount must be greater than zero."),
  note: z.string().trim().max(1000, "Note must be at most 1000 characters.")
});

export type PaymentFormValues = z.infer<typeof paymentFormSchema>;
