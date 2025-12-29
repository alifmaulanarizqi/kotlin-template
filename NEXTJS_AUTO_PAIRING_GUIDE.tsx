/**
 * QUICK GUIDE: Auto Pairing Printer di Next.js
 *
 * Tidak ada perubahan di sisi Next.js!
 * Cukup panggil btPrintWithTemplate seperti biasa.
 *
 * Jika printer belum paired:
 * - App akan otomatis buka pairing screen
 * - User pilih printer
 * - App auto-pair dan lanjut print
 * - Promise resolved dengan hasil print
 */

import { btPrintWithTemplate, btHasPairedPrinter } from './android-printer-bridge';

// ============================================
// CONTOH 1: Print Langsung (Recommended)
// ============================================
// App akan handle pairing otomatis jika belum paired
export async function printReceipt() {
  try {
    const result = await btPrintWithTemplate({
      items: [
        {
          type: "text",
          text: "TOKO SAYA",
          alignment: "center",
          fontSize: "large",
          bold: true,
          newLinesAfter: 2
        },
        {
          type: "text",
          text: "Total: Rp 50,000",
          bold: true,
          newLinesAfter: 2
        },
        {
          type: "qr",
          data: "ORDER#12345",
          size: 200,
          alignment: "center"
        },
        {
          type: "raw",
          bytes: [27, 100, 4] // Feed paper
        }
      ]
    });

    if (result.ok) {
      console.log('✅ Print berhasil!');
      alert('Print berhasil!');
    } else {
      console.error('❌ Error:', result.error);
      alert(`Print gagal: ${result.error}`);
    }
  } catch (error) {
    console.error('Exception:', error);
    alert('Terjadi error saat print');
  }
}

// ============================================
// CONTOH 2: Check Status Dulu (Optional)
// ============================================
// Anda bisa cek dulu apakah printer sudah paired
// Tapi ini OPSIONAL karena app akan auto-handle
export async function printWithStatusCheck() {
  try {
    // Check printer status
    const statusResult = await btHasPairedPrinter();

    if (statusResult.hasPaired) {
      console.log('✅ Printer sudah paired');
    } else {
      console.log('ℹ️ Printer belum paired - akan dibuka pairing screen');
    }

    // Print (app akan auto-handle pairing)
    const printResult = await btPrintWithTemplate({
      items: [
        { type: "text", text: "Test Print", alignment: "center" }
      ]
    });

    if (printResult.ok) {
      console.log('✅ Print berhasil!');
    } else {
      console.log('❌ Print gagal:', printResult.error);
    }
  } catch (error) {
    console.error('Exception:', error);
  }
}

// ============================================
// CONTOH 3: Print Button di React Component
// ============================================
export default function PrintButton() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handlePrint = async () => {
    setLoading(true);
    setMessage('');

    try {
      const result = await btPrintWithTemplate({
        items: [
          {
            type: "text",
            text: "Receipt",
            alignment: "center",
            fontSize: "large",
            bold: true,
            newLinesAfter: 2
          },
          {
            type: "text",
            text: `Date: ${new Date().toLocaleDateString()}`,
            newLinesAfter: 1
          },
          {
            type: "text",
            text: `Time: ${new Date().toLocaleTimeString()}`,
            newLinesAfter: 2
          },
          {
            type: "raw",
            bytes: [27, 100, 4]
          }
        ]
      });

      if (result.ok) {
        setMessage('✅ Print berhasil!');
      } else {
        setMessage(`❌ Error: ${result.error}`);
      }
    } catch (error) {
      setMessage(`❌ Exception: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        onClick={handlePrint}
        disabled={loading}
        className="bg-blue-500 text-white px-4 py-2 rounded disabled:bg-gray-400"
      >
        {loading ? 'Printing...' : '🖨️ Print Receipt'}
      </button>

      {message && (
        <div className={`mt-2 p-3 rounded ${
          message.startsWith('✅') ? 'bg-green-100' : 'bg-red-100'
        }`}>
          {message}
        </div>
      )}
    </div>
  );
}

// ============================================
// CATATAN PENTING
// ============================================

/**
 * 1. TIDAK PERLU CHECK PAIRED MANUAL
 *    App akan otomatis handle pairing jika belum paired
 *
 * 2. PROMISE AKAN RESOLVED SETELAH:
 *    - Print berhasil (jika sudah paired), atau
 *    - User selesai pairing + print berhasil, atau
 *    - User cancel pairing / error
 *
 * 3. TIMEOUT
 *    Tidak ada timeout di sisi Next.js
 *    Promise akan menunggu sampai user selesai pairing
 *
 * 4. ERROR MESSAGES
 *    - "Pairing dibatalkan. Printer belum terpasang." → User cancel
 *    - "Print gagal: ..." → Printer error
 *    - Lainnya → Technical error
 *
 * 5. USER FLOW
 *    Printer Sudah Paired:
 *      Print → ✅ Langsung print (0.5s)
 *
 *    Printer Belum Paired:
 *      Print → Pairing Screen → User pilih printer →
 *      Auto pair (2s) → Auto resume print → ✅ Done (5-10s total)
 *
 *    User Cancel:
 *      Print → Pairing Screen → User klik Batal →
 *      ❌ Error "Pairing dibatalkan"
 */

// ============================================
// BEST PRACTICES
// ============================================

/**
 * ✅ DO:
 * - Call btPrintWithTemplate langsung tanpa cek paired
 * - Handle result.ok dan result.error
 * - Show loading state saat print
 * - Give feedback ke user (sukses/gagal)
 *
 * ❌ DON'T:
 * - Jangan panggil btConnectPrinter manual
 * - Jangan cek paired sebelum print (not needed)
 * - Jangan set timeout - biarkan user selesai pairing
 */

