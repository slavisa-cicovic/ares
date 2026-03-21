import { useState } from "react";
import {
  Alert,
  Avatar,
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
import { FormProvider, RHFTextField } from "../../components";
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
  const { changePassword } = useAuth();
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const passwordMethods = useForm<ProfilePasswordFormValues>({
    defaultValues: emptyPasswordValues,
    resolver: zodResolver(profilePasswordSchema)
  });

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

  const passwordSubmit = passwordMethods.handleSubmit(handlePasswordSave);
  const passwordDisabled = passwordMethods.formState.isSubmitting;

  return (
    <Stack spacing={3}>
      <Card variant="outlined">
        <CardHeader
          subheader="Your account identity inside the active company workspace."
          title="Profile"
        />
        <Divider />
        <CardContent>
          <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
            <Avatar sx={{ bgcolor: "primary.main", height: 72, width: 72 }}>
              {`${user.firstName.charAt(0)}${user.lastName.charAt(0)}`}
            </Avatar>

            <Box sx={{ ...getFieldGridSx(2), flex: 1 }}>
              <TextField disabled fullWidth label="First Name" value={user.firstName} />
              <TextField disabled fullWidth label="Last Name" value={user.lastName} />
              <TextField disabled fullWidth label="Email" value={user.email} />
              <TextField disabled fullWidth label="Company" value={user.companyName} />
              <TextField
                disabled
                fullWidth
                label="Roles"
                sx={{ gridColumn: { xs: "auto", md: "1 / -1" } }}
                value={user.roles.join(", ")}
              />
            </Box>
          </Stack>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardHeader
          subheader="Update your sign-in credentials without changing company-wide settings."
          title="Security"
        />
        <Divider />
        <CardContent>
          <FormProvider methods={passwordMethods} onSubmit={(event) => void passwordSubmit(event)}>
            <Stack spacing={3}>
              <Box sx={getFieldGridSx(1)}>
                <RHFTextField
                  disabled={passwordDisabled}
                  label="Current Password"
                  name="currentPassword"
                  type="password"
                />
              </Box>

              <Box sx={getFieldGridSx(2)}>
                <RHFTextField disabled={passwordDisabled} label="New Password" name="newPassword" type="password" />
                <RHFTextField
                  disabled={passwordDisabled}
                  label="Confirm New Password"
                  name="confirmNewPassword"
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
    </Stack>
  );
}
