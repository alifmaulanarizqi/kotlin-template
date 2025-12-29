/**
 * Android Bridge Helper untuk Thermal Printer dengan Printooth
 * Copy file ini ke project Next.js Anda
 */

"use client";

type BridgeMsg = {
  requestId: string;
  type: string;
  data?: any;
};

function initBridgeOnce() {
  if (typeof window === "undefined") return;
  if (window.__bridgeInitialized) return;

  window.__bridgePending = new Map();

  window.__onBridgeResult = (payload: string) => {
    try {
      const msg = JSON.parse(payload) as BridgeMsg;
      const rid = msg?.requestId;
      if (!rid) return;

      const cb = window.__bridgePending?.get(rid);
      if (cb) {
        window.__bridgePending?.delete(rid);
        cb(msg);
      }
    } catch {
      // ignore
    }
  };

  window.__bridgeInitialized = true;
}

export function isAndroidBridgeReady(): boolean {
  return typeof window !== "undefined" && !!window.AndroidBridge;
}

function callBridge<T>(invoke: (requestId: string) => void): Promise<T> {
  initBridgeOnce();

  if (!isAndroidBridgeReady()) {
    return Promise.resolve({
      ok: false,
      error: "AndroidBridge not available (not running inside Android WebView?)",
    } as any);
  }

  const requestId = globalThis.crypto?.randomUUID?.() ?? String(Date.now());

  return new Promise<T>((resolve) => {
    window.__bridgePending!.set(requestId, (msg: BridgeMsg) => {
      resolve((msg?.data ?? msg) as T);
    });
    invoke(requestId);
  });
}

export type PairedPrinter = { name: string; mac: string };

// ==========================================
// BLUETOOTH PRINTER - MANUAL ESCPOS
// ==========================================

export function btListPairedPrinters() {
  return callBridge<{ ok: boolean; devices?: any[]; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;
    if (!bridge || !bridge.btListPaired) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_PAIRED_LIST",
          data: { ok: false, error: "AndroidBridge.btListPaired not available" },
        })
      );
      return;
    }
    bridge.btListPaired(rid);
  });
}

export function btConnectPrinter(mac: string) {
  return callBridge<{ ok: boolean; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;

    if (!bridge?.btConnect) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_CONNECT_RESULT",
          data: { ok: false, error: "AndroidBridge.btConnect not available" },
        })
      );
      return;
    }

    bridge.btConnect(rid, mac);
  });
}

export function btPrintToPrinter(mac: string, base64: string) {
  return callBridge<{ ok: boolean; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;
    if (!bridge?.btPrint) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_PRINT_RESULT",
          data: { ok: false, error: "AndroidBridge.btPrint not available" },
        })
      );
      return;
    }
    bridge.btPrint(rid, mac, base64);
  });
}

export function btDisconnectPrinter(mac: string) {
  return callBridge<{ ok: boolean; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;
    if (!bridge?.btDisconnect) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_DISCONNECT_RESULT",
          data: { ok: false, error: "AndroidBridge.btDisconnect not available" },
        })
      );
      return;
    }
    bridge.btDisconnect(rid, mac);
  });
}

// ==========================================
// BLUETOOTH PRINTER - PRINTOOTH TEMPLATE
// ==========================================

/**
 * Cek apakah printer sudah dipasangkan via Bluetooth Settings
 */
export function btHasPairedPrinter() {
  return callBridge<{ ok: boolean; hasPaired?: boolean; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;
    if (!bridge?.btHasPairedPrinter) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_HAS_PAIRED_RESULT",
          data: { ok: false, error: "AndroidBridge.btHasPairedPrinter not available" },
        })
      );
      return;
    }
    bridge.btHasPairedPrinter(rid);
  });
}

/**
 * Print dengan Printooth Template
 * Mendukung: text, QR code, raw bytes
 *
 * CATATAN PENTING:
 * - Printer HARUS sudah dipasangkan (paired) terlebih dahulu via Settings > Bluetooth
 * - Tidak perlu connect/disconnect manual
 * - Library akan handle koneksi otomatis
 *
 * Template format:
 * {
 *   items: [
 *     {
 *       type: "text",
 *       text: "Hello",
 *       alignment: "center",     // "left" | "center" | "right"
 *       fontSize: "large",       // "normal" | "large" | "wide" | "tall" | "big"
 *       bold: true,              // true | false
 *       underline: false,        // true | false
 *       newLinesAfter: 1,        // number
 *       lineSpacing: 30,         // 30 | 60
 *       charCode: 0              // character code (default: PC1252)
 *     },
 *     {
 *       type: "qr",
 *       data: "QR Content",      // string to encode
 *       size: 200,               // QR size in pixels
 *       alignment: "center"      // "left" | "center" | "right"
 *     },
 *     {
 *       type: "raw",
 *       bytes: [27, 100, 4]      // raw ESC/POS commands
 *     }
 *   ]
 * }
 */
export type PrintTemplate = {
  items: Array<
    | {
        type: "text";
        text: string;
        alignment?: "left" | "center" | "right";
        fontSize?: "normal" | "large" | "wide" | "tall" | "big";
        bold?: boolean;
        underline?: boolean;
        newLinesAfter?: number;
        lineSpacing?: number;
        charCode?: number;
      }
    | {
        type: "qr";
        data: string;
        size?: number;
        alignment?: "left" | "center" | "right";
      }
    | {
        type: "raw";
        bytes: number[];
      }
  >;
};

export function btPrintWithTemplate(template: PrintTemplate) {
  return callBridge<{ ok: boolean; error?: string }>((rid) => {
    const bridge = window.AndroidBridge;
    if (!bridge?.btPrintWithTemplate) {
      window.__bridgePending?.delete(rid);
      window.__onBridgeResult?.(
        JSON.stringify({
          requestId: rid,
          type: "BT_PRINT_TEMPLATE_RESULT",
          data: { ok: false, error: "AndroidBridge.btPrintWithTemplate not available" },
        })
      );
      return;
    }
    bridge.btPrintWithTemplate(rid, JSON.stringify(template));
  });
}

// ==========================================
// HELPER FUNCTIONS
// ==========================================

/**
 * Print simple text (shortcut)
 */
export async function printSimpleText(text: string) {
  return btPrintWithTemplate({
    items: [
      {
        type: "text",
        text: text,
        newLinesAfter: 2,
      },
      {
        type: "raw",
        bytes: [27, 100, 4], // Feed lines
      },
    ],
  });
}

/**
 * Print receipt example
 */
export async function printReceipt(data: {
  storeName: string;
  storeAddress: string;
  items: Array<{ name: string; qty: number; price: number; total: number }>;
  total: number;
}) {
  return btPrintWithTemplate({
    items: [
      // Header
      {
        type: "text",
        text: data.storeName,
        alignment: "center",
        fontSize: "large",
        bold: true,
        newLinesAfter: 1,
      },
      {
        type: "text",
        text: data.storeAddress,
        alignment: "center",
        newLinesAfter: 2,
      },
      // Separator
      {
        type: "text",
        text: "--------------------------------",
        newLinesAfter: 1,
      },
      // Items
      ...data.items.flatMap((item) => [
        {
          type: "text" as const,
          text: item.name,
          newLinesAfter: 0,
        },
        {
          type: "text" as const,
          text: `  ${item.qty} x ${item.price.toLocaleString()} = ${item.total.toLocaleString()}`,
          newLinesAfter: 1,
        },
      ]),
      // Separator
      {
        type: "text",
        text: "--------------------------------",
        newLinesAfter: 1,
      },
      // Total
      {
        type: "text",
        text: `TOTAL: Rp ${data.total.toLocaleString()}`,
        bold: true,
        fontSize: "large",
        newLinesAfter: 2,
      },
      // QR Code
      {
        type: "qr",
        data: `Total: ${data.total}`,
        size: 200,
        alignment: "center",
      },
      // Footer
      {
        type: "text",
        text: "Terima Kasih",
        alignment: "center",
        newLinesAfter: 1,
      },
      // Feed paper
      {
        type: "raw",
        bytes: [27, 100, 4],
      },
    ],
  });
}

/**
 * Print with QR Code
 */
export async function printWithQR(text: string, qrData: string) {
  return btPrintWithTemplate({
    items: [
      {
        type: "text",
        text: text,
        alignment: "center",
        fontSize: "large",
        bold: true,
        newLinesAfter: 2,
      },
      {
        type: "qr",
        data: qrData,
        size: 200,
        alignment: "center",
      },
      {
        type: "text",
        text: "Scan QR Code di atas",
        alignment: "center",
        newLinesAfter: 2,
      },
      {
        type: "raw",
        bytes: [27, 100, 4],
      },
    ],
  });
}

