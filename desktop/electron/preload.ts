import { contextBridge } from "electron";

contextBridge.exposeInMainWorld("aresDesktop", {
  platform: "desktop",
  version: "0.0.1"
});
