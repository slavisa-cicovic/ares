import RHFNumberField from "./RHFNumberField";

type RHFCurrencyFieldProps = {
  name: string;
  label?: string;
  helperText?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  currency?: string;
  className?: string;
};

export default function RHFCurrencyField({
  name,
  currency = "RSD",
  ...other
}: RHFCurrencyFieldProps) {
  return <RHFNumberField decimals={2} name={name} suffix={currency} {...other} />;
}
