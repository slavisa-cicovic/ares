import { useState } from "react";
import { Box, Stack, Tab, Tabs, Typography } from "@mui/material";
import type { AuthenticatedUser } from "../auth/types";
import { CompanySettings } from "./CompanySettings";
import { ProfileSettings } from "./ProfileSettings";

type SettingsTab = "company" | "profile";

type SettingsScreenProps = {
  user: AuthenticatedUser;
  onBrandingPreviewChange?: (preview: {
    themePreset: string;
    brandColor: string | null;
    logoUrl: string | null;
  } | null) => void;
};

export function SettingsScreen({ user, onBrandingPreviewChange }: SettingsScreenProps) {
  const [activeTab, setActiveTab] = useState<SettingsTab>("company");

  return (
    <Stack spacing={3}>
      <Box>
        <Typography color="text.secondary" variant="overline">
          Settings
        </Typography>
        <Typography variant="h4">Workspace Settings</Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
          Keep company identity and personal account settings separate.
        </Typography>
      </Box>

      <Tabs
        onChange={(_event, nextValue: SettingsTab) => setActiveTab(nextValue)}
        value={activeTab}
      >
        <Tab label="Company" value="company" />
        <Tab label="Profile" value="profile" />
      </Tabs>

      {activeTab === "company" ? (
        <CompanySettings onBrandingPreviewChange={onBrandingPreviewChange} user={user} />
      ) : null}

      {activeTab === "profile" ? <ProfileSettings user={user} /> : null}
    </Stack>
  );
}
