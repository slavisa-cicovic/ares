import type { TypographyOptions } from "@mui/material/styles/createTypography";

function pxToRem(value: number) {
  return `${value / 16}rem`;
}

function responsiveFontSizes({
  sm,
  md,
  lg
}: {
  sm: number;
  md: number;
  lg: number;
}) {
  return {
    "@media (min-width:600px)": {
      fontSize: pxToRem(sm)
    },
    "@media (min-width:900px)": {
      fontSize: pxToRem(md)
    },
    "@media (min-width:1200px)": {
      fontSize: pxToRem(lg)
    }
  };
}

export const typography: TypographyOptions = {
  fontFamily: '"IBM Plex Sans", "Segoe UI", sans-serif',
  fontWeightRegular: 400,
  fontWeightMedium: 500,
  fontWeightBold: 700,
  h1: {
    fontWeight: 700,
    lineHeight: 1.04,
    fontSize: pxToRem(40),
    ...responsiveFontSizes({ sm: 46, md: 54, lg: 60 })
  },
  h2: {
    fontWeight: 700,
    lineHeight: 1.1,
    fontSize: pxToRem(32),
    ...responsiveFontSizes({ sm: 36, md: 40, lg: 44 })
  },
  h3: {
    fontWeight: 700,
    lineHeight: 1.14,
    fontSize: pxToRem(26),
    ...responsiveFontSizes({ sm: 28, md: 32, lg: 34 })
  },
  h4: {
    fontWeight: 700,
    lineHeight: 1.2,
    fontSize: pxToRem(22),
    ...responsiveFontSizes({ sm: 22, md: 24, lg: 26 })
  },
  h5: {
    fontWeight: 700,
    lineHeight: 1.24,
    fontSize: pxToRem(18),
    ...responsiveFontSizes({ sm: 19, md: 20, lg: 20 })
  },
  h6: {
    fontWeight: 700,
    lineHeight: 1.3,
    fontSize: pxToRem(16),
    ...responsiveFontSizes({ sm: 17, md: 18, lg: 18 })
  },
  subtitle1: {
    fontWeight: 600,
    lineHeight: 1.45,
    fontSize: pxToRem(16)
  },
  subtitle2: {
    fontWeight: 600,
    lineHeight: 1.45,
    fontSize: pxToRem(14)
  },
  body1: {
    lineHeight: 1.55,
    fontSize: pxToRem(16)
  },
  body2: {
    lineHeight: 1.55,
    fontSize: pxToRem(14)
  },
  caption: {
    lineHeight: 1.45,
    fontSize: pxToRem(12)
  },
  overline: {
    fontWeight: 700,
    lineHeight: 1.45,
    fontSize: pxToRem(11),
    letterSpacing: "0.14em",
    textTransform: "uppercase"
  },
  button: {
    fontWeight: 700,
    lineHeight: 1.45,
    fontSize: pxToRem(14),
    textTransform: "none"
  }
};
