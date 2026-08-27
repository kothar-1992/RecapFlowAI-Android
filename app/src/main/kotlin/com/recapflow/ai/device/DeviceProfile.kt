package com.recapflow.ai.device

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import java.util.Locale

enum class DeviceCapabilityTier {
    LIGHT,
    BALANCED,
    HIGH,
}

data class DeviceProfile(
    val deviceName: String,
    val deviceType: String,
    val screenSummary: String,
    val cpuSummary: String,
    val memorySummary: String,
    val storageSummary: String,
    val networkSummary: String,
    val capabilityTier: DeviceCapabilityTier,
    val recommendation: String,
)

object DeviceProfileReader {

    fun read(context: Context): DeviceProfile {
        val configuration = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val smallestWidthDp = configuration.smallestScreenWidthDp
        val deviceType = if (smallestWidthDp >= 600) "Tablet" else "Phone"

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val storage = StatFs(context.filesDir.absolutePath)
        val tier = capabilityTier(
            totalMemoryBytes = memoryInfo.totalMem,
            cpuCores = cores,
            lowMemory = memoryInfo.lowMemory,
        )

        return DeviceProfile(
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter(String::isNotBlank)
                .joinToString(" ")
                .replaceFirstChar { it.titlecase(Locale.getDefault()) },
            deviceType = "$deviceType • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            screenSummary = "${metrics.widthPixels} × ${metrics.heightPixels} px • " +
                "${configuration.screenWidthDp} × ${configuration.screenHeightDp} dp • " +
                "sw${smallestWidthDp}dp",
            cpuSummary = "$cores cores • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown ABI"}",
            memorySummary = "${formatBytes(memoryInfo.availMem)} available / " +
                "${formatBytes(memoryInfo.totalMem)} total",
            storageSummary = "${formatBytes(storage.availableBytes)} free / " +
                "${formatBytes(storage.totalBytes)} app volume",
            networkSummary = networkSummary(context),
            capabilityTier = tier,
            recommendation = when (tier) {
                DeviceCapabilityTier.LIGHT ->
                    "Light profile: smaller preview effects and 720p-first rendering."
                DeviceCapabilityTier.BALANCED ->
                    "Balanced profile: adaptive preview effects and 720p-first rendering."
                DeviceCapabilityTier.HIGH ->
                    "High profile: expanded preview effects; verify 720p before 1080p."
            },
        )
    }

    internal fun capabilityTier(
        totalMemoryBytes: Long,
        cpuCores: Int,
        lowMemory: Boolean,
    ): DeviceCapabilityTier {
        val gib = totalMemoryBytes.toDouble() / GIB.toDouble()
        return when {
            lowMemory || gib < 4.0 || cpuCores <= 4 -> DeviceCapabilityTier.LIGHT
            gib >= 8.0 && cpuCores >= 8 -> DeviceCapabilityTier.HIGH
            else -> DeviceCapabilityTier.BALANCED
        }
    }

    private fun networkSummary(context: Context): String {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
                ?: return "Offline"
            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Connected"
            }
            val status = if (
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                "internet available"
            } else {
                "not validated"
            }
            return "$transport • $status"
        }

        @Suppress("DEPRECATION")
        val network = manager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return if (network?.isConnected == true) {
            "${network.typeName.orEmpty().ifBlank { "Connected" }} • internet available"
        } else {
            "Offline"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val gib = bytes.toDouble() / GIB.toDouble()
        return if (gib >= 1.0) {
            String.format(Locale.US, "%.1f GB", gib)
        } else {
            String.format(Locale.US, "%.0f MB", bytes.toDouble() / MIB.toDouble())
        }
    }

    private const val MIB = 1_048_576L
    private const val GIB = 1_073_741_824L
}
