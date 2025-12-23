package com.minimal.launcher

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.format.Formatter
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
fun StatsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // State for dynamic values
    val batteryInfo = remember { getBatteryInfo(context) }
    val ramInfo = remember { getRamInfo(context) }
    val storageInfo = remember { getStorageInfo() }
    val uptime = remember { getUptime() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("SYSTEM_DIAGNOSTICS", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // We use LazyColumn so it's scrollable if you add even more later
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("--- HARDWARE ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                StatBlock("MODEL", "${Build.MANUFACTURER} ${Build.MODEL}", 1f)
                StatBlock("PROCESSOR", Build.HARDWARE.uppercase(), 1f)
                StatBlock("BATTERY", "${batteryInfo.level}% (${batteryInfo.status})", batteryInfo.level / 100f)
            }
            item {
    Spacer(modifier = Modifier.height(16.dp))
    Text("--- NETWORK ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    StatBlock("CONNECTION", getNetworkInfo(context), 1f)
}

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("--- MEMORY_STORAGE ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                StatBlock("RAM_USAGE", "${ramInfo.used} / ${ramInfo.total} GB", ramInfo.percent)
                StatBlock("STORAGE", "${storageInfo.used} / ${storageInfo.total} GB", storageInfo.percent)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("--- KERNEL_OS ---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                StatBlock("KERNEL", System.getProperty("os.version") ?: "UNKNOWN", 1f)
                StatBlock("ANDROID", "API ${Build.VERSION.SDK_INT}", 1f)
                StatBlock("UPTIME", uptime, 1f)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Swipe down to terminate session...", 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    }
}

@Composable
fun StatBlock(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (progress < 1f || label.contains("USAGE") || label.contains("BATTERY") || label.contains("STORAGE")) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
    }
}

// --- HELPER LOGIC ---

data class InfoResult(val used: String, val total: String, val percent: Float)
data class BatteryResult(val level: Int, val status: String)

private fun getNetworkInfo(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return "OFFLINE"
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "OFFLINE"
    
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
        else -> "ETHERNET/OTHER"
    }
}

private fun getBatteryInfo(context: Context): BatteryResult {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val status = when (statusInt) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
        else -> "DISCHARGING"
    }
    return BatteryResult(level, status)
}

private fun getRamInfo(context: Context): InfoResult {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val total = memInfo.totalMem / (1024 * 1024 * 1024.0)
    val available = memInfo.availMem / (1024 * 1024 * 1024.0)
    val used = total - available
    return InfoResult("%.1f".format(used), "%.1f".format(total), (used / total).toFloat())
}

private fun getStorageInfo(): InfoResult {
    val stat = StatFs(Environment.getDataDirectory().path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    val total = (totalBlocks * blockSize) / (1024 * 1024 * 1024.0)
    val available = (availableBlocks * blockSize) / (1024 * 1024 * 1024.0)
    val used = total - available
    return InfoResult("%.1f".format(used), "%.1f".format(total), (used / total).toFloat())
}

private fun getUptime(): String {
    val millis = SystemClock.elapsedRealtime()
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "${hours}h ${minutes}m"
}
