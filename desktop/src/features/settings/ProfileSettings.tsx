import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Divider,
  Stack,
  TextField
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import type { AuthenticatedUser } from "../auth/types";
import { ApiClientError } from "../../lib/api/DesktopApiClient";
import { useAuth } from "../auth/AuthProvider";
import { FormProvider, RHFLogoPicker, RHFTextField } from "../../components";
import {
  profileIdentitySchema,
  type ProfileIdentityFormInput,
  type ProfileIdentityFormValues
} from "./profile-identity.schema";
import { profilePasswordSchema, type ProfilePasswordFormValues } from "./profile-password.schema";

type ProfileSettingsProps = {
  user: AuthenticatedUser;
};

const emptyPasswordValues: ProfilePasswordFormValues = {
  currentPassword: "",
  newPassword: "",
  confirmNewPassword: ""
};

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

export function ProfileSettings({ user }: ProfileSettingsProps) {
  const { changePassword, updateOwnProfile, uploadUserLogo } = useAuth();
  const [profileNotice, setProfileNotice] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const profileMethods = useForm<ProfileIdentityFormInput, undefined, ProfileIdentityFormValues>({
    defaultValues: {
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? "",
      profileLogo: user.userLogoUrl ?? null
    },
    resolver: zodResolver(profileIdentitySchema)
  });
  const passwordMethods = useForm<ProfilePasswordFormValues>({
    defaultValues: emptyPasswordValues,
    resolver: zodResolver(profilePasswordSchema)
  });

  useEffect(() => {
    profileMethods.reset({
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? "",
      profileLogo: user.userLogoUrl ?? null
    });
  }, [profileMethods, user]);

  async function handlePasswordSave(values: ProfilePasswordFormValues) {
    setPasswordNotice(null);
    setPasswordError(null);

    try {
      await changePassword(values);
      passwordMethods.reset(emptyPasswordValues);
      setPasswordNotice("Password updated.");
    } catch (error) {
      setPasswordError(
        error instanceof ApiClientError ? error.message : "Unable to change password right now."
      );
    }
  }

  async function handleProfileSave(values: ProfileIdentityFormValues) {
    setProfileNotice(null);
    setProfileError(null);

    try {
      let nextUserLogoUrl: string | null =
        typeof values.profileLogo === "string" ? (values.profileLogo.trim() || null) : null;

      if (values.profileLogo instanceof File) {
        nextUserLogoUrl = await uploadUserLogo(values.profileLogo);
      }

      await updateOwnProfile({
        email: values.email,
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone.trim() ? values.phone.trim() : null,
        userLogoUrl: nextUserLogoUrl
      });
      profileMethods.reset({
        email: values.email.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        phone: values.phone.trim(),
        profileLogo: nextUserLogoUrl
      });
      setProfileNotice("Profile updated.");
    } catch (error) {
      setProfileError(
        error instanceof ApiClientError ? error.message : "Unable to update profile right now."
      );
    }
  }

  const profileSubmit = profileMethods.handleSubmit(handleProfileSave);
  const passwordSubmit = passwordMethods.handleSubmit(handlePasswordSave);
  const profileDisabled = profileMethods.formState.isSubmitting;
  const passwordDisabled = passwordMethods.formState.isSubmitting;

  return (
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
          subheader="Your account identity inside the active company workspace."
          title="Profile"
        />
        <Divider />
        <CardContent>
          <FormProvider methods={profileMethods} onSubmit={(event) => void profileSubmit(event)}>
            <Stack spacing={2}>
              <RHFLogoPicker
                description="Select a profile image for future account personalization."
                disabled={profileDisabled}
                name="profileLogo"
                title="Profile Image"
              />

              <Box sx={getFieldGridSx(2)}>
                <RHFTextField disabled={profileDisabled} label="First Name" maxLength={100} name="firstName" size="small" />
                <RHFTextField disabled={profileDisabled} label="Last Name" maxLength={100} name="lastName" size="small" />
              </Box>

              <Box sx={getFieldGridSx(2)}>
                <RHFTextField disabled={profileDisabled} label="Email" name="email" size="small" />
                <RHFTextField disabled={profileDisabled} label="Phone" maxLength={64} name="phone" size="small" />
              </Box>

              <Box sx={getFieldGridSx(1)}>
                <TextField
                  disabled
                  fullWidth
                  label="Roles"
                  size="small"
                  value={user.roles.join(", ")}
                />
              </Box>

              {profileError ? <Alert severity="error">{profileError}</Alert> : null}
              {profileNotice ? <Alert severity="success">{profileNotice}</Alert> : null}
            </Stack>
          </FormProvider>
        </CardContent>
        <CardActions sx={{ justifyContent: "flex-end", px: 3, pb: 3 }}>
          <Button
            disabled={profileDisabled}
            onClick={() => {
              profileMethods.reset({
                email: user.email,
                firstName: user.firstName,
                lastName: user.lastName,
                phone: user.phone ?? "",
                profileLogo: user.userLogoUrl ?? null
              });
              setProfileError(null);
              setProfileNotice(null);
            }}
            variant="text"
          >
            Cancel
          </Button>
          <Button disabled={profileDisabled} onClick={() => void profileSubmit()} variant="contained">
            {profileMethods.formState.isSubmitting ? "Saving..." : "Save"}
          </Button>
        </CardActions>
      </Card>

      <Card sx={{ minWidth: 0 }} variant="outlined">
        <CardHeader
          subheader="Update your sign-in credentials without changing company-wide settings."
          title="Security"
        />
        <Divider />
        <CardContent>
          <FormProvider methods={passwordMethods} onSubmit={(event) => void passwordSubmit(event)}>
            <Stack spacing={2}>
              <Box sx={getFieldGridSx(2)}>
                <RHFTextField
                  disabled={passwordDisabled}
                  label="Current Password"
                  name="currentPassword"
                  size="small"
                  type="password"
                />
              </Box>

              <Box sx={getFieldGridSx(2)}>
                <RHFTextField disabled={passwordDisabled} label="New Password" name="newPassword" size="small" type="password" />
                <RHFTextField
                  disabled={passwordDisabled}
                  label="Confirm New Password"
                  name="confirmNewPassword"
                  size="small"
                  type="password"
                />
              </Box>

              {passwordError ? <Alert severity="error">{passwordError}</Alert> : null}
              {passwordNotice ? <Alert severity="success">{passwordNotice}</Alert> : null}
            </Stack>
          </FormProvider>
        </CardContent>
        <CardActions sx={{ justifyContent: "flex-end", px: 3, pb: 3 }}>
          <Button
            disabled={passwordDisabled}
            onClick={() => {
              passwordMethods.reset(emptyPasswordValues);
              setPasswordError(null);
              setPasswordNotice(null);
            }}
            variant="text"
          >
            Cancel
          </Button>
          <Button disabled={passwordDisabled} onClick={() => void passwordSubmit()} variant="contained">
            {passwordMethods.formState.isSubmitting ? "Saving..." : "Save"}
          </Button>
        </CardActions>
      </Card>
    </Box>
  );
}
