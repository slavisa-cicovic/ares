import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Stack } from "@mui/material";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import type { CompanySummary, CreatePlatformCompanyPayload } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { FormProvider, RHFTextField, CustomDialog } from "../../components";
import { superAdminCompanySchema, type SuperAdminCompanyFormValues } from "./super-admin-company.schema";

type SuperAdminCompanyFormDialogProps = {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreatePlatformCompanyPayload) => Promise<CompanySummary>;
  onCreated: (company: CompanySummary) => void;
};

const defaultValues: SuperAdminCompanyFormValues = {
  name: "",
  pib: "",
  mb: ""
};

export function SuperAdminCompanyFormDialog({
  open,
  onClose,
  onSubmit,
  onCreated
}: SuperAdminCompanyFormDialogProps) {
  const methods = useForm<SuperAdminCompanyFormValues>({
    defaultValues,
    resolver: zodResolver(superAdminCompanySchema)
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      methods.reset(defaultValues);
      setError(null);
    }
  }, [methods, open]);

  const isSubmitting = methods.formState.isSubmitting;

  async function handleSave(values: SuperAdminCompanyFormValues) {
    setError(null);

    try {
      const createdCompany = await onSubmit({
        name: values.name.trim(),
        pib: values.pib.trim(),
        mb: values.mb.trim()
      });
      onCreated(createdCompany);
      methods.reset(defaultValues);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "Unable to create company right now."
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
            {isSubmitting ? "Creating..." : "Create Company"}
          </Button>
        </>
      }
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      description="Create the tenant record with the minimal business identity required for onboarding."
      onClose={onClose}
      open={open}
      title="New Company"
    >
      <FormProvider methods={methods} onSubmit={(event) => void submit(event)}>
        <Stack spacing={2}>
          <RHFTextField disabled={isSubmitting} label="Name" name="name" size="small" />
          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <RHFTextField disabled={isSubmitting} label="PIB" maxLength={64} name="pib" size="small" />
            <RHFTextField disabled={isSubmitting} label="MB" maxLength={64} name="mb" size="small" />
          </Stack>

          {error ? <Alert severity="error">{error}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}
