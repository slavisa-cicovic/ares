import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  CircularProgress,
  Divider,
  MenuItem,
  Stack
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import type { AuthenticatedUser } from "../auth/types";
import type { CompanyResponse, UpdateCompanyPayload } from "../../lib/api/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "../auth/AuthProvider";
import {
  FormProvider,
  RHFColorPicker,
  RHFLogoPicker,
  RHFSelect,
  RHFTextField
} from "../../components";
import { companyBrandingSchema, type CompanyBrandingFormValues } from "./company-branding.schema";
import { companyCoreSchema, type CompanyCoreFormValues } from "./company-core.schema";
import { normalizeDesktopTheme, type DesktopTheme } from "./themeStorage";

type CompanySettingsProps = {
  user: AuthenticatedUser;
  onBrandingPreviewChange?: (preview: {
    themePreset: string;
    brandColor: string | null;
    logoUrl: string | null;
  } | null) => void;
};

const themeOptions: Array<{ id: DesktopTheme; label: string; description: string }> = [
  { id: "amber", label: "Amber Ledger", description: "Warm paper tones close to the current workspace." },
  { id: "ocean", label: "Ocean Steel", description: "Cool blue surfaces with stronger contrast." },
  { id: "graphite", label: "Graphite", description: "Muted neutral palette for denser daily work." }
];

const emptyCompanyCoreValues: CompanyCoreFormValues = {
  name: "",
  pib: "",
  mb: "",
  bankAccount: "",
  email: "",
  phone: "",
  address: "",
  postalCode: "",
  city: "",
  country: "Serbia"
};

function toOptional(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function toCompanyCoreValues(company: CompanyResponse | null, user: AuthenticatedUser): CompanyCoreFormValues {
  if (!company) {
    return {
      ...emptyCompanyCoreValues,
      name: user.companyName
    };
  }

  return {
    name: company.name,
    pib: company.pib ?? "",
    mb: company.mb ?? "",
    bankAccount: company.bankAccount ?? "",
    email: company.email ?? "",
    phone: company.phone ?? "",
    address: company.address ?? "",
    postalCode: company.postalCode ?? "",
    city: company.city ?? "",
    country: company.country ?? "Serbia"
  };
}

function toBrandingValues(company: CompanyResponse | null, user: AuthenticatedUser): CompanyBrandingFormValues {
  return {
    logo: company?.logoUrl ?? user.companyLogoUrl ?? null,
    themePreset: normalizeDesktopTheme(company?.themePreset ?? user.companyThemePreset),
    brandColor: company?.brandColor ?? user.companyBrandColor ?? ""
  };
}

function buildPayload(company: CompanyResponse, overrides: Partial<UpdateCompanyPayload>): UpdateCompanyPayload {
  return {
    name: company.name,
    pib: company.pib,
    mb: company.mb,
    address: company.address,
    postalCode: company.postalCode,
    city: company.city,
    country: company.country,
    email: company.email,
    phone: company.phone,
    bankAccount: company.bankAccount,
    brandColor: company.brandColor,
    logoUrl: company.logoUrl,
    themePreset: company.themePreset,
    ...overrides
  };
}

function buildCorePayload(company: CompanyResponse, values: CompanyCoreFormValues): UpdateCompanyPayload {
  return buildPayload(company, {
    name: values.name.trim(),
    pib: toOptional(values.pib),
    mb: toOptional(values.mb),
    address: toOptional(values.address),
    postalCode: toOptional(values.postalCode),
    city: toOptional(values.city),
    country: toOptional(values.country),
    email: toOptional(values.email),
    phone: toOptional(values.phone),
    bankAccount: toOptional(values.bankAccount)
  });
}

function getFieldGridSx(columns: number) {
  return {
    columnGap: 2,
    display: "grid",
    gridTemplateColumns: {
      xs: "1fr",
      md: `repeat(${columns}, minmax(0, 1fr))`
    },
    rowGap: 2
  } as const;
}

export function CompanySettings({ user, onBrandingPreviewChange }: CompanySettingsProps) {
  const { getCompany, updateCompany, uploadCompanyLogo } = useAuth();
  const [company, setCompany] = useState<CompanyResponse | null>(null);
  const [companyLoading, setCompanyLoading] = useState(true);
  const [companyLoadError, setCompanyLoadError] = useState<string | null>(null);
  const [coreNotice, setCoreNotice] = useState<string | null>(null);
  const [coreError, setCoreError] = useState<string | null>(null);
  const [brandingNotice, setBrandingNotice] = useState<string | null>(null);
  const [brandingError, setBrandingError] = useState<string | null>(null);

  const isCompanyAdmin = user.roles.includes("company-admin");

  const coreMethods = useForm<CompanyCoreFormValues>({
    defaultValues: toCompanyCoreValues(null, user),
    resolver: zodResolver(companyCoreSchema)
  });
  const brandingMethods = useForm<CompanyBrandingFormValues>({
    defaultValues: toBrandingValues(null, user),
    resolver: zodResolver(companyBrandingSchema)
  });

  useEffect(() => {
    if (!isCompanyAdmin) {
      setCompanyLoading(false);
      setCompanyLoadError(null);
      onBrandingPreviewChange?.(null);
      return;
    }

    let active = true;

    void getCompany()
      .then((response) => {
        if (!active) {
          return;
        }

        setCompany(response);
        setCompanyLoadError(null);
        coreMethods.reset(toCompanyCoreValues(response, user));
        brandingMethods.reset(toBrandingValues(response, user));
      })
      .catch((error) => {
        if (!active) {
          return;
        }

        setCompanyLoadError(
          error instanceof ApiClientError ? error.message : "Unable to load company settings."
        );
      })
      .finally(() => {
        if (active) {
          setCompanyLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [brandingMethods, coreMethods, getCompany, isCompanyAdmin, onBrandingPreviewChange, user]);

  useEffect(() => {
    if (!onBrandingPreviewChange) {
      return;
    }

    const subscription = brandingMethods.watch((values) => {
      const themePreset =
        typeof values.themePreset === "string"
          ? normalizeDesktopTheme(values.themePreset)
          : normalizeDesktopTheme(user.companyThemePreset);
      const brandColor =
        typeof values.brandColor === "string" && values.brandColor.trim()
          ? values.brandColor.trim().toUpperCase()
          : null;
      const logoUrl =
        typeof values.logo === "string" && values.logo.trim()
          ? values.logo.trim()
          : null;

      onBrandingPreviewChange({
        themePreset,
        brandColor,
        logoUrl
      });
    });

    return () => {
      subscription.unsubscribe();
      onBrandingPreviewChange(null);
    };
  }, [brandingMethods, onBrandingPreviewChange, user.companyThemePreset]);

  function syncCompanyState(updatedCompany: CompanyResponse) {
    setCompany(updatedCompany);
    coreMethods.reset(toCompanyCoreValues(updatedCompany, user));
    brandingMethods.reset(toBrandingValues(updatedCompany, user));
    onBrandingPreviewChange?.(null);
  }

  async function handleCoreSave(values: CompanyCoreFormValues) {
    if (!company) {
      return;
    }

    setCoreNotice(null);
    setCoreError(null);

    try {
      const updatedCompany = await updateCompany(buildCorePayload(company, values));
      syncCompanyState(updatedCompany);
      setCoreNotice("Core company data updated.");
    } catch (error) {
      setCoreError(
        error instanceof ApiClientError ? error.message : "Unable to save core company data."
      );
    }
  }

  async function handleBrandingSave(values: CompanyBrandingFormValues) {
    if (!company) {
      return;
    }

    setBrandingNotice(null);
    setBrandingError(null);

    try {
      let nextLogoUrl: string | null =
        typeof values.logo === "string" ? (values.logo.trim() || null) : null;

      if (values.logo instanceof File) {
        nextLogoUrl = await uploadCompanyLogo(values.logo);
      }

      const updatedCompany = await updateCompany(
        buildPayload(company, {
          logoUrl: nextLogoUrl,
          themePreset: values.themePreset,
          brandColor: toOptional(values.brandColor)
        })
      );
      syncCompanyState(updatedCompany);
      setBrandingNotice("Company branding updated.");
    } catch (error) {
      setBrandingError(
        error instanceof ApiClientError ? error.message : "Unable to save company branding."
      );
    }
  }

  const coreSubmit = coreMethods.handleSubmit(handleCoreSave);
  const brandingSubmit = brandingMethods.handleSubmit(handleBrandingSave);
  const coreDisabled = !isCompanyAdmin || companyLoading || !company || coreMethods.formState.isSubmitting;
  const brandingDisabled =
    !isCompanyAdmin || companyLoading || !company || brandingMethods.formState.isSubmitting;

  return (
    <>
      {companyLoadError ? <Alert severity="error">{companyLoadError}</Alert> : null}

      <Box
        sx={{
          alignItems: "start",
          display: "grid",
          gap: 3,
          gridTemplateColumns: {
            xs: "1fr",
            xl: "repeat(2, minmax(0, 1fr))"
          }
        }}
      >
        <Card sx={{ minWidth: 0 }} variant="outlined">
          <CardHeader
            subheader="Legal and operational company data used across invoices and reports."
            title="Core"
          />
          <Divider />
          <CardContent>
            {companyLoading ? (
              <Stack alignItems="center" py={6}>
                <CircularProgress size={28} />
              </Stack>
            ) : (
              <FormProvider methods={coreMethods} onSubmit={(event) => void coreSubmit(event)}>
                <Stack spacing={1}>
                  <Box sx={getFieldGridSx(1)}>
                    <RHFTextField disabled={coreDisabled} label="Name" name="name" />
                  </Box>

                  <Box sx={getFieldGridSx(2)}>
                    <RHFTextField disabled={coreDisabled} label="PIB" name="pib" />
                    <RHFTextField disabled={coreDisabled} label="MB" name="mb" />
                  </Box>

                  <Box sx={getFieldGridSx(2)}>
                    <RHFTextField disabled={coreDisabled} label="Bank Account" name="bankAccount" />
                  </Box>

                  <Box sx={getFieldGridSx(2)}>
                    <RHFTextField disabled={coreDisabled} label="Email" name="email" />
                    <RHFTextField disabled={coreDisabled} label="Phone" name="phone" />
                  </Box>

                  <Box sx={getFieldGridSx(1)}>
                    <RHFTextField disabled={coreDisabled} label="Address" name="address" />
                  </Box>

                  <Box sx={getFieldGridSx(2)}>
                    <RHFTextField disabled={coreDisabled} label="Postal Code" name="postalCode" />
                    <RHFTextField disabled={coreDisabled} label="City" name="city" />
                  </Box>

                  <Box sx={getFieldGridSx(2)}>
                    <RHFTextField disabled={coreDisabled} label="Country" name="country" />
                  </Box>

                  {coreError ? <Alert severity="error">{coreError}</Alert> : null}
                  {coreNotice ? <Alert severity="success">{coreNotice}</Alert> : null}
                </Stack>
              </FormProvider>
            )}
          </CardContent>
          <CardActions sx={{ justifyContent: "flex-end", px: 3, pb: 3 }}>
            <Button
              disabled={coreDisabled}
              onClick={() => {
                coreMethods.reset(toCompanyCoreValues(company, user));
                setCoreError(null);
                setCoreNotice(null);
              }}
              variant="text"
            >
              Cancel
            </Button>
            <Button disabled={coreDisabled} onClick={() => void coreSubmit()} variant="contained">
              {coreMethods.formState.isSubmitting ? "Saving..." : "Save"}
            </Button>
          </CardActions>
        </Card>

        <Card sx={{ minWidth: 0 }} variant="outlined">
          <CardHeader
            subheader="Workspace identity, logo, and visual theme for this company."
            title="Branding"
          />
          <Divider />
          <CardContent>
            {companyLoading ? (
              <Stack alignItems="center" py={6}>
                <CircularProgress size={28} />
              </Stack>
            ) : (
              <FormProvider methods={brandingMethods} onSubmit={(event) => void brandingSubmit(event)}>
                <Stack spacing={3}>
                  <RHFLogoPicker
                    description="Upload a company logo for the workspace shell and future document templates."
                    disabled={brandingDisabled}
                    name="logo"
                    title="Logo"
                  />

                  <Box sx={getFieldGridSx(1)}>
                    <RHFSelect disabled={brandingDisabled} label="Theme" name="themePreset">
                      {themeOptions.map((option) => (
                        <MenuItem key={option.id} value={option.id}>
                          {option.label}
                        </MenuItem>
                      ))}
                    </RHFSelect>
                  </Box>

                  <Box sx={getFieldGridSx(1)}>
                    <RHFColorPicker
                      disabled={brandingDisabled}
                      helperText="Used as the primary company accent inside the desktop app."
                      label="Brand Color"
                      name="brandColor"
                    />
                  </Box>

                  <Alert severity="info">
                    {themeOptions.find((option) => option.id === brandingMethods.watch("themePreset"))?.description ??
                      "Theme preset controls the default workspace tone."}
                  </Alert>

                  {brandingError ? <Alert severity="error">{brandingError}</Alert> : null}
                  {brandingNotice ? <Alert severity="success">{brandingNotice}</Alert> : null}
                </Stack>
              </FormProvider>
            )}
          </CardContent>
          <CardActions sx={{ justifyContent: "flex-end", px: 3, pb: 3 }}>
            <Button
              disabled={brandingDisabled}
              onClick={() => {
                brandingMethods.reset(toBrandingValues(company, user));
                setBrandingError(null);
                setBrandingNotice(null);
                onBrandingPreviewChange?.(null);
              }}
              variant="text"
            >
              Cancel
            </Button>
            <Button disabled={brandingDisabled} onClick={() => void brandingSubmit()} variant="contained">
              {brandingMethods.formState.isSubmitting ? "Saving..." : "Save"}
            </Button>
          </CardActions>
        </Card>

        {!isCompanyAdmin ? (
          <Alert severity="info" sx={{ gridColumn: { xs: "auto", xl: "1 / -1" } }}>
            Only company admins can edit company settings.
          </Alert>
        ) : null}
      </Box>
    </>
  );
}
