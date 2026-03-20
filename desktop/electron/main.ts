import { app, BrowserWindow, dialog, ipcMain, shell } from "electron";
import { mkdir, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";

const devServerUrl = "http://127.0.0.1:5173";
const tempPdfDirectory = path.join(os.tmpdir(), "ares-pdf");

function sanitizeFileName(fileName: string): string {
  return fileName.replace(/[^a-zA-Z0-9._-]/g, "-");
}

function createMainWindow(): BrowserWindow {
  const window = new BrowserWindow({
    width: 1440,
    height: 940,
    minWidth: 1100,
    minHeight: 760,
    backgroundColor: "#f4efe5",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  if (!app.isPackaged) {
    void window.loadURL(devServerUrl);
    window.webContents.openDevTools({ mode: "detach" });
  } else {
    void window.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  }

  return window;
}

ipcMain.handle("pdf:preview", async (_event, payload: { fileName: string; base64Data: string }) => {
  await mkdir(tempPdfDirectory, { recursive: true });

  const filePath = path.join(tempPdfDirectory, `${Date.now()}-${sanitizeFileName(payload.fileName)}`);
  await writeFile(filePath, Buffer.from(payload.base64Data, "base64"));

  void shell.openPath(filePath).then((openResult) => {
    if (openResult) {
      console.error("[electron] Unable to preview PDF", {
        filePath,
        error: openResult
      });
    }
  });

  return filePath;
});

ipcMain.handle("pdf:save", async (_event, payload: { fileName: string; base64Data: string }) => {
  const window = BrowserWindow.getFocusedWindow() ?? BrowserWindow.getAllWindows()[0];
  const result = await dialog.showSaveDialog(window, {
    defaultPath: sanitizeFileName(payload.fileName),
    filters: [{ name: "PDF", extensions: ["pdf"] }]
  });

  if (result.canceled || !result.filePath) {
    return null;
  }

  await writeFile(result.filePath, Buffer.from(payload.base64Data, "base64"));
  return result.filePath;
});

app.whenReady().then(() => {
  createMainWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
