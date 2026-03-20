export {};

declare global {
  interface Window {
    aresDesktop: {
      platform: string;
      version: string;
    };
  }
}
