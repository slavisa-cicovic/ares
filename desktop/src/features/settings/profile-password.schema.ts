import { z } from "zod";

export const profilePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Current password is required."),
    newPassword: z
      .string()
      .min(8, "New password must be at least 8 characters.")
      .max(255, "New password must be at most 255 characters."),
    confirmNewPassword: z.string().min(1, "Confirm password is required.")
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: "Passwords do not match.",
    path: ["confirmNewPassword"]
  });

export type ProfilePasswordFormValues = z.infer<typeof profilePasswordSchema>;
