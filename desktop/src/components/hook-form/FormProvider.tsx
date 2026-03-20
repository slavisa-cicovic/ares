import type { ComponentPropsWithoutRef, ReactNode } from "react";
import {
  FormProvider as ReactHookFormProvider,
  type FieldValues,
  type UseFormReturn
} from "react-hook-form";

type FormProviderProps<TFieldValues extends FieldValues> = {
  children: ReactNode;
  methods: UseFormReturn<TFieldValues>;
  className?: string;
  onSubmit?: ComponentPropsWithoutRef<"form">["onSubmit"];
};

export default function FormProvider<TFieldValues extends FieldValues>({
  children,
  className,
  onSubmit,
  methods
}: FormProviderProps<TFieldValues>) {
  return (
    <ReactHookFormProvider {...methods}>
      <form className={className} noValidate onSubmit={onSubmit}>
        {children}
      </form>
    </ReactHookFormProvider>
  );
}
