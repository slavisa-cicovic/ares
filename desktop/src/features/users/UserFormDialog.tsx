import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Stack } from "@mui/material";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import type { CreateUserPayload, CreatedUserResponse } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { CustomDialog, FormProvider, RHFTextField } from "../../components";
import { userSchema, type UserFormValues } from "./user.schema";

type UserFormDialogProps = {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateUserPayload) => Promise<CreatedUserResponse>;
  onCreated: (user: CreatedUserResponse) => void;
};

const defaultValues: UserFormValues = {
  email: "",
  firstName: "",
  lastName: ""
};

export function UserFormDialog({ open, onClose, onSubmit, onCreated }: UserFormDialogProps) {
  const methods = useForm<UserFormValues>({
    defaultValues,
    resolver: zodResolver(userSchema)
  });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      methods.reset(defaultValues);
      setError(null);
    }
  }, [methods, open]);

  const isSubmitting = methods.formState.isSubmitting;

  async function handleSave(values: UserFormValues) {
    setError(null);

    try {
      const createdUser = await onSubmit({
        email: values.email.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        status: "ACTIVE",
        roles: ["user"]
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
      description="Create a company user and reveal a temporary password once."
      onClose={onClose}
      open={open}
      title="New User"
    >
      <FormProvider methods={methods} onSubmit={(event) => void submit(event)}>
        <Stack spacing={2}>
          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <RHFTextField disabled={isSubmitting} label="First Name" maxLength={100} name="firstName" size="small" />
            <RHFTextField disabled={isSubmitting} label="Last Name" maxLength={100} name="lastName" size="small" />
          </Stack>

          <RHFTextField disabled={isSubmitting} label="Email" name="email" size="small" />

          {error ? <Alert severity="error">{error}</Alert> : null}
        </Stack>
      </FormProvider>
    </CustomDialog>
  );
}
