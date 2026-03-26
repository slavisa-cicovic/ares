import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, MenuItem, Stack } from "@mui/material";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import type { CompanySummary, CreatePlatformUserPayload, CreatedPlatformUserResponse } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { CustomDialog, FormProvider, RHFSelect, RHFTextField } from "../../components";
import { superAdminUserSchema, type SuperAdminUserFormValues } from "./super-admin-user.schema";

type SuperAdminUserFormDialogProps = {
  open: boolean;
  companies: CompanySummary[];
  onClose: () => void;
  onSubmit: (payload: CreatePlatformUserPayload) => Promise<CreatedPlatformUserResponse>;
  onCreated: (user: CreatedPlatformUserResponse) => void;
};

const defaultValues: SuperAdminUserFormValues = {
  companyId: "",
  email: "",
  firstName: "",
  lastName: "",
  role: "company-admin"
};

export function SuperAdminUserFormDialog({
  open,
  companies,
  onClose,
  onSubmit,
  onCreated
}: SuperAdminUserFormDialogProps) {
  const methods = useForm<SuperAdminUserFormValues>({
    defaultValues,
    resolver: zodResolver(superAdminUserSchema)
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      methods.reset(defaultValues);
      setError(null);
      return;
    }

    methods.reset({
      ...defaultValues,
      companyId: companies[0]?.id ?? ""
    });
  }, [companies, methods, open]);

  const isSubmitting = methods.formState.isSubmitting;

  async function handleSave(values: SuperAdminUserFormValues) {
    setError(null);

    try {
      const createdUser = await onSubmit({
        companyId: values.companyId,
        email: values.email.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        status: "ACTIVE",
        roles: [values.role]
      });
      onCreated(createdUser);
      methods.reset(defaultValues);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "Unable to create user right now."
      );
    }
  }

  const submit = methods.handleSubmit(handleSave);

  return (
    <CustomDialog
      actions={
        <>
          <Button disabled={isSubmitting} onClick={onClose} variant="text">
            Cancel
          </Button>
          <Button disabled={isSubmitting} onClick={() => void submit()} variant="contained">
            {isSubmitting ? "Creating..." : "Create User"}
          </Button>
        </>
      }
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      description="Create the first company admin or a regular company user with a temporary password."
      onClose={onClose}
      open={open}
      title="New User"
    >
      <FormProvider methods={methods} onSubmit={(event) => void submit(event)}>
        <Stack spacing={2}>
          <RHFSelect disabled={isSubmitting} label="Company" name="companyId">
            {companies.map((company) => (
              <MenuItem key={company.id} value={company.id}>
                {company.name}
              </MenuItem>
            ))}
          </RHFSelect>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <RHFTextField disabled={isSubmitting} label="First Name" maxLength={100} name="firstName" size="small" />
            <RHFTextField disabled={isSubmitting} label="Last Name" maxLength={100} name="lastName" size="small" />
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <RHFTextField disabled={isSubmitting} label="Email" name="email" size="small" />
            <RHFSelect disabled={isSubmitting} label="Role" name="role">
              <MenuItem value="company-admin">Company Admin</MenuItem>
              <MenuItem value="user">User</MenuItem>
            </RHFSelect>
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}
