import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Typography
} from "@mui/material";
import type { PropsWithChildren, ReactNode } from "react";

type CustomDialogProps = PropsWithChildren<{
  open: boolean;
  title: string;
  description?: string;
  size?: "sm" | "md" | "lg";
  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
  actions?: ReactNode;
  onClose: () => void;
}>;

export default function CustomDialog({
  open,
  title,
  description,
  size = "md",
  closeOnBackdrop = true,
  closeOnEscape = true,
  actions,
  onClose,
  children
}: CustomDialogProps) {
  return (
    <Dialog
      fullWidth
      maxWidth={size === "sm" ? "sm" : size === "lg" ? "lg" : "md"}
      onClose={(_event, reason) => {
        if (reason === "backdropClick" && !closeOnBackdrop) {
          return;
        }

        if (reason === "escapeKeyDown" && !closeOnEscape) {
          return;
        }

        onClose();
      }}
      open={open}
    >
      <DialogTitle sx={{ pb: description ? 1 : 2 }}>
        <Stack alignItems="flex-start" direction="row" justifyContent="space-between" spacing={2}>
          <BoxWithText title={title} description={description} />
          <IconButton aria-label="Close dialog" onClick={onClose}>
            <CloseRoundedIcon />
          </IconButton>
        </Stack>
      </DialogTitle>

      <DialogContent dividers>{children}</DialogContent>

      {actions ? <DialogActions sx={{ px: 3, py: 2 }}>{actions}</DialogActions> : null}
    </Dialog>
  );
}

function BoxWithText({ title, description }: { title: string; description?: string }) {
  return (
    <Stack spacing={0.5}>
      <Typography variant="h5">{title}</Typography>
      {description ? (
        <Typography color="text.secondary" variant="body2">
          {description}
        </Typography>
      ) : null}
    </Stack>
  );
}
