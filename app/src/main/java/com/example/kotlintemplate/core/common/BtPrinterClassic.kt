package com.example.kotlintemplate.core.common

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PairedPrinter(val name: String?, val mac: String)

object BtPrinterClassic {
    private val sockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private fun adapter(ctx: Context): BluetoothAdapter? {
        val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return mgr.adapter
    }

    private fun hasConnectPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    fun listPaired(ctx: Context): Result<List<PairedPrinter>> {
        val ad = adapter(ctx) ?: return Result.failure(IllegalStateException("Bluetooth adapter null"))
        if (!ad.isEnabled) return Result.failure(IllegalStateException("Bluetooth is OFF"))
        if (!hasConnectPermission(ctx)) return Result.failure(SecurityException("Missing BLUETOOTH_CONNECT"))

        val bonded = ad.bondedDevices ?: emptySet()
        return Result.success(bonded.map { PairedPrinter(it.name, it.address) })
    }

    @SuppressLint("MissingPermission")
    fun connect(ctx: Context, mac: String): Result<Unit> {
        val ad = adapter(ctx) ?: return Result.failure(IllegalStateException("Bluetooth adapter null"))
        if (!ad.isEnabled) return Result.failure(IllegalStateException("Bluetooth is OFF"))
        if (!hasConnectPermission(ctx)) return Result.failure(SecurityException("Missing BLUETOOTH_CONNECT"))

        val device = ad.getRemoteDevice(mac)

        sockets.remove(mac)?.runCatching { close() }
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

        ad.cancelDiscovery()
        socket.connect()

        sockets[mac] = socket
        return Result.success(Unit)
    }

    fun disconnect(mac: String) {
        sockets.remove(mac)?.runCatching { close() }
    }

    fun printAndDisconnect(mac: String, bytes: ByteArray): Result<Unit> {
        val socket = sockets[mac] ?: return Result.failure(IllegalStateException("Not connected"))
        return try {
            socket.outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            disconnect(mac) // auto
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect(mac)
            Result.failure(e)
        }
    }
}
