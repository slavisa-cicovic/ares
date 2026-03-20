import { Button, Typography } from "@mui/material";
import type { ReactNode } from "react";
import CustomDialog from "./CustomDialog";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description?: string;
  content?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmColor?: "primary" | "error" | "warning";
  isLoading?: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
};

export default function ConfirmDialog({
  open,
  title,
  description,
  content,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  confirmColor = "primary",
  isLoading = false,
  onClose,
  onConfirm
}: ConfirmDialogProps) {
  return (
    <CustomDialog
      actions={
        <>
          <Button disabled={isLoading} onClick={onClose} variant="text">
            {cancelLabel}
          </Button>
          <Button
            color={confirmColor}
            disabled={isLoading}
            onClick={() => void onConfirm()}
            variant="contained"
          >
            {isLoading ? "Working..." : confirmLabel}
          </Button>
        </>
      }
      closeOnBackdrop={!isLoading}
      closeOnEscape={!isLoading}
      onClose={onClose}
      open={open}
      size="sm"
      title={title}
      description={description}
    >
      {content ?? (
        <Typography color="text.secondary" variant="body2">
          Are you sure you want to continue?
        </Typography>
      )}
    </CustomDialog>
  );
}
