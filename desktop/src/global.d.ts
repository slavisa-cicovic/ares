export {};

declare global {
  interface Window {
    aresDesktop: {
      platform: string;
      version: string;
      previewPdf: (payload: { fileName: string; base64Data: string }) => Promise<string>;
      savePdf: (payload: { fileName: string; base64Data: string }) => Promise<string | null>;
    };
  }
}
