import { useEffect } from "react";
import { Alert, Button } from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { CustomDialog, FormProvider, RHFSwitch, RHFTextField } from "../../components";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import type { ClientSummary, UpsertClientPayload } from "../../lib/api/types";
import { useAuth } from "../auth/AuthProvider";
import { clientFormSchema, type ClientFormValues } from "./client-form.schema";

type ClientFormDialogProps = {
  open: boolean;
  client: ClientSummary | null;
  onClose: () => void;
  onSaved: () => void;
};

const emptyValues: ClientFormValues = {
  name: "",
  pib: "",
  mb: "",
  email: "",
  phone: "",
  address: "",
  city: "",
  postalCode: "",
  country: "",
  active: true
};

function toFormValues(client: ClientSummary | null): ClientFormValues {
  if (!client) {
    return emptyValues;
  }

  return {
    name: client.name,
    pib: client.pib ?? "",
    mb: client.mb ?? "",
    email: client.email ?? "",
    phone: client.phone ?? "",
    address: client.address ?? "",
    city: client.city ?? "",
    postalCode: client.postalCode ?? "",
    country: client.country ?? "",
    active: client.status === "ACTIVE"
  };
}

function toPayload(values: ClientFormValues): UpsertClientPayload {
  function normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  return {
    name: values.name.trim(),
    pib: normalize(values.pib),
    mb: normalize(values.mb),
    email: normalize(values.email),
    phone: normalize(values.phone),
    address: normalize(values.address),
    city: normalize(values.city),
    postalCode: normalize(values.postalCode),
    country: normalize(values.country),
    status: values.active ? "ACTIVE" : "INACTIVE"
  };
}

export function ClientFormDialog({ open, client, onClose, onSaved }: ClientFormDialogProps) {
  const { createClient, updateClient } = useAuth();
  const methods = useForm<ClientFormValues>({
    defaultValues: toFormValues(client),
    resolver: zodResolver(clientFormSchema)
  });

  const {
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting }
  } = methods;

  useEffect(() => {
    if (open) {
      reset(toFormValues(client));
    }
  }, [client, open, reset]);

  async function handleFormSubmit(values: ClientFormValues) {
    const payload = toPayload(values);

    try {
      if (client) {
        await updateClient(client.id, payload);
      } else {
        await createClient(payload);
      }

      onSaved();
      onClose();
    } catch (error) {
      const message =
        error instanceof ApiClientError ? error.message : "Unable to save client right now.";
      setError("root", { message });
    }
  }

  const submitForm = handleSubmit(handleFormSubmit);

  return (
    <CustomDialog
      actions={
        <>
          <Button onClick={onClose} variant="text">
            Cancel
          </Button>
          <Button
            disabled={isSubmitting}
            onClick={() => void submitForm()}
            variant="contained"
          >
            {isSubmitting ? "Saving..." : client ? "Save" : "Create"}
          </Button>
        </>
      }
      onClose={onClose}
      open={open}
      size="lg"
      title={client ? "Edit Client" : "New Client"}
    >
      <FormProvider
        className="client-form"
        methods={methods}
        onSubmit={(event) => void submitForm(event)}
      >
        <div className="client-form-grid">
          <RHFTextField helperText="Legal or display name." label="Name" name="name" />
          <RHFSwitch
            className="client-form-switch"
            label="Client is active"
            name="active"
          />
          <RHFTextField label="PIB" maxLength={32} name="pib" />
          <RHFTextField label="MB" maxLength={32} name="mb" />
          <RHFTextField label="Email" name="email" type="email" />
          <RHFTextField label="Phone" name="phone" />
          <RHFTextField className="field-span-2" label="Address" name="address" />
          <RHFTextField label="City" name="city" />
          <RHFTextField label="Postal Code" name="postalCode" />
          <RHFTextField className="field-span-2" label="Country" name="country" />
        </div>

        {errors.root?.message ? (
          <Alert severity="error">{errors.root.message}</Alert>
        ) : null}
      </FormProvider>
    </CustomDialog>
  );
}
