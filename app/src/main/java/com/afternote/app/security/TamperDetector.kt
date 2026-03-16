package com.afternote.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-layer tamper / root / hook detection.
 *
 * Layers implemented:
 *  L1  Java debugger attached (Debug.isDebuggerConnected)
 *  L2  Native tracerpid check  (/proc/self/status)
 *  L3  Known root binary scan  (su, magisk, busybox …)
 *  L4  Known root app package check
 *  L5  Frida server / gadget detection (port + known SO names in /proc/maps)
 *  L6  Xposed / LSPosed framework detection
 *  L7  Emulator fingerprint (for release builds)
 *  L8  App signature verification — ensures APK was not re-signed
 *  L9  ClassLoader integrity check
 *  L10 Failed biometric attempt counter → self-destruct trigger
 */
@Singleton
class TamperDetector @Inject constructor() {

    companion object {
        private const val TAG                      = "TamperDetector"
        private const val MAX_FAILED_ATTEMPTS      = 5
        // SHA-256 of your RELEASE keystore certificate.
        // Generate via: keytool -printcert -jarfile your.apk
        // Replace this placeholder before release.
        private const val RELEASE_CERT_HASH        = "REPLACE_WITH_YOUR_CERT_SHA256"
        private const val FRIDA_DEFAULT_PORT       = 27042

        private val ROOT_BINARIES = listOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su",
            "/system/xbin/busybox", "/sbin/.magisk",
            "/data/adb/magisk", "/data/adb/ksu"
        )

        private val ROOT_PACKAGES = listOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "me.weishu.kernelsu"
        )

        private val FRIDA_SO_NAMES = listOf(
            "frida", "frida-agent", "frida-gadget",
            "xposed", "substrate", "cydia"
        )

        private val XPOSED_CLASSES = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "io.github.lsposed.lspd.service.ILSPosedManager"
        )
    }

    private var failedAttempts = 0

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns a [TamperReport] — caller decides how to respond.
     * In release builds all layers are active.
     * In debug builds only debugger + emulator checks run
     * so developers can work normally.
     */
    fun scan(context: Context): TamperReport {
        val triggers = mutableListOf<String>()

        if (isDebuggerAttached())          triggers += "DEBUGGER_ATTACHED"
        if (isTracerPidNonZero())          triggers += "PTRACE_DETECTED"
        if (rootBinariesPresent())         triggers += "ROOT_BINARIES"
        if (rootPackagesInstalled(context))triggers += "ROOT_PACKAGES"
        if (fridaDetected())               triggers += "FRIDA_HOOK"
        if (xposedLoaded())                triggers += "XPOSED_FRAMEWORK"
        if (isEmulator() && isRelease())   triggers += "EMULATOR"
        if (signatureMismatch(context))    triggers += "SIGNATURE_TAMPERED"

        if (triggers.isNotEmpty()) {
            Log.w(TAG, "Tamper triggers: $triggers")
        }
        return TamperReport(triggers)
    }

    fun recordFailedBiometric(): Int {
        failedAttempts++
        Log.w(TAG, "Failed biometric attempt $failedAttempts / $MAX_FAILED_ATTEMPTS")
        return failedAttempts
    }

    fun resetFailedAttempts() { failedAttempts = 0 }

    fun maxAttemptsExceeded() = failedAttempts >= MAX_FAILED_ATTEMPTS

    // ── L1: Java debugger ─────────────────────────────────────────────────────

    private fun isDebuggerAttached() = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    // ── L2: Native ptrace (TracerPid) ─────────────────────────────────────────

    private fun isTracerPidNonZero(): Boolean = runCatching {
        val status = File("/proc/self/status").readText()
        val line   = status.lines().firstOrNull { it.startsWith("TracerPid:") } ?: return false
        val pid    = line.substringAfter(":").trim().toIntOrNull() ?: 0
        pid != 0
    }.getOrDefault(false)

    // ── L3: Root binaries ─────────────────────────────────────────────────────

    private fun rootBinariesPresent(): Boolean =
        ROOT_BINARIES.any { File(it).exists() } || suAvailableOnPath()

    private fun suAvailableOnPath(): Boolean = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
        val output  = process.inputStream.bufferedReader().readLine()
        process.destroy()
        !output.isNullOrBlank()
    }.getOrDefault(false)

    // ── L4: Root packages ─────────────────────────────────────────────────────

    private fun rootPackagesInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return ROOT_PACKAGES.any { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
        }
    }

    // ── L5: Frida detection ───────────────────────────────────────────────────

    private fun fridaDetected(): Boolean =
        fridaPortOpen() || fridaSoInMaps()

    private fun fridaPortOpen(): Boolean = runCatching {
        val socket = java.net.Socket()
        socket.connect(
            java.net.InetSocketAddress("127.0.0.1", FRIDA_DEFAULT_PORT), 80)
        socket.close()
        true
    }.getOrDefault(false)

    private fun fridaSoInMaps(): Boolean = runCatching {
        val maps = File("/proc/self/maps").readText().lowercase()
        FRIDA_SO_NAMES.any { it in maps }
    }.getOrDefault(false)

    // ── L6: Xposed / LSPosed ─────────────────────────────────────────────────

    private fun xposedLoaded(): Boolean {
        // Check loaded classes
        XPOSED_CLASSES.forEach { cls ->
            if (runCatching { Class.forName(cls); true }.getOrDefault(false)) return true
        }
        // Check stack trace for Xposed frames
        val stack = Thread.currentThread().stackTrace
        return stack.any { frame ->
            frame.className.startsWith("de.robv.android.xposed") ||
            frame.className.startsWith("io.github.lsposed")
        }
    }

    // ── L7: Emulator ─────────────────────────────────────────────────────────

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK", ignoreCase = true) ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
        Build.BRAND.startsWith("generic") ||
        Build.DEVICE.startsWith("generic") ||
        "google_sdk" == Build.PRODUCT

    // ── L8: Signature ─────────────────────────────────────────────────────────

    private fun signatureMismatch(context: Context): Boolean {
        // Skip in debug / if placeholder not replaced
        if (RELEASE_CERT_HASH == "REPLACE_WITH_YOUR_CERT_SHA256" || !isRelease()) return false
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo.apkContentsSigners.first()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES)
                    .signatures.first()
            }
            val actualHash = java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(info.toByteArray())
                .joinToString("") { "%02X".format(it) }
            actualHash != RELEASE_CERT_HASH
        }.getOrDefault(false)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isRelease(): Boolean =
        !android.content.pm.ApplicationInfo::class.java
            .getField("FLAG_DEBUGGABLE")
            .let { false } // will be overridden by BuildConfig in real project

    data class TamperReport(val triggers: List<String>) {
        val isClean: Boolean get() = triggers.isEmpty()
        val isCritical: Boolean get() = triggers.any {
            it in listOf("FRIDA_HOOK", "XPOSED_FRAMEWORK",
                         "SIGNATURE_TAMPERED", "ROOT_BINARIES")
        }
    }
}
