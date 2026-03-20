import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("aresDesktop", {
  platform: "desktop",
  version: "0.0.1",
  previewPdf: (payload: { fileName: string; base64Data: string }) => ipcRenderer.invoke("pdf:preview", payload),
  savePdf: (payload: { fileName: string; base64Data: string }) => ipcRenderer.invoke("pdf:save", payload)
});
