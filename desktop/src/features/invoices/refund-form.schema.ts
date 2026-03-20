import { z } from "zod";

export const refundFormSchema = z.object({
  refundDate: z.string().min(1, "Refund date is required."),
  amount: z
    .string()
    .min(1, "Amount is required.")
    .refine((value) => Number(value.replace(",", ".")) > 0, "Amount must be greater than zero."),
  note: z.string().max(1000, "Note can contain up to 1000 characters.")
});

export type RefundFormValues = z.infer<typeof refundFormSchema>;
