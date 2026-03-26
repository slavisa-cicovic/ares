import { z } from "zod";

export const superAdminUserSchema = z.object({
  companyId: z.string().trim().min(1, "Company is required."),
  email: z.string().trim().min(1, "Email is required.").email("Email must be a valid email address."),
  firstName: z.string().trim().min(1, "First name is required.").max(100, "First name must be 100 characters or fewer."),
  lastName: z.string().trim().min(1, "Last name is required.").max(100, "Last name must be 100 characters or fewer."),
  role: z.enum(["company-admin", "user"], {
    error: "Role is required."
  })
});

export type SuperAdminUserFormValues = z.output<typeof superAdminUserSchema>;
