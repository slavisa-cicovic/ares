import { alpha } from "@mui/material/styles";
import type { Shadows } from "@mui/material/styles";

export function shadows(mode: "light" | "dark"): Shadows {
  const base = mode === "dark" ? "#02060a" : "#1b2430";
  const list = Array.from({ length: 25 }, (_, index) => {
    if (index === 0) {
      return "none";
    }

    const y = Math.max(4, index * 2);
    const blur = Math.max(12, index * 4);
    const spread = Math.max(0, Math.floor(index / 4) - 2);
    const opacity = mode === "dark" ? 0.32 : 0.06 + index * 0.005;
    return `0 ${y}px ${blur}px ${spread}px ${alpha(base, Math.min(opacity, 0.16))}`;
  });

  return list as Shadows;
}
