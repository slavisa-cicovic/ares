import { useEffect, type PropsWithChildren, type ReactNode } from "react";

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
  useEffect(() => {
    if (!open || !closeOnEscape) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [closeOnEscape, onClose, open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="custom-dialog-backdrop"
      onClick={() => {
        if (closeOnBackdrop) {
          onClose();
        }
      }}
      role="presentation"
    >
      <div
        aria-labelledby="custom-dialog-title"
        aria-modal="true"
        className={`custom-dialog custom-dialog-${size}`}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <div className="custom-dialog-header">
          <div className="custom-dialog-copy">
            <span className="eyebrow">Dialog</span>
            <h3 id="custom-dialog-title">{title}</h3>
            {description ? <p>{description}</p> : null}
          </div>

          <button className="secondary-button custom-dialog-close" onClick={onClose} type="button">
            Close
          </button>
        </div>

        <div className="custom-dialog-content">{children}</div>

        {actions ? <div className="custom-dialog-actions">{actions}</div> : null}
      </div>
    </div>
  );
}
