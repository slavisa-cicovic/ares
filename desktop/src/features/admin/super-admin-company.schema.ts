import { z } from "zod";

export const superAdminCompanySchema = z.object({
  name: z.string().trim().min(1, "Company name is required.").max(255, "Company name must be 255 characters or fewer."),
  pib: z.string().trim().min(1, "PIB is required.").max(64, "PIB must be 64 characters or fewer."),
  mb: z.string().trim().min(1, "MB is required.").max(64, "MB must be 64 characters or fewer.")
});

export type SuperAdminCompanyFormValues = z.output<typeof superAdminCompanySchema>;
