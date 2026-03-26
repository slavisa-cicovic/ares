import { z } from "zod";

export const userSchema = z.object({
  email: z.email("Enter a valid email address."),
  firstName: z.string().trim().min(1, "First name is required.").max(100, "First name is too long."),
  lastName: z.string().trim().min(1, "Last name is required.").max(100, "Last name is too long.")
});

export type UserFormValues = z.infer<typeof userSchema>;
