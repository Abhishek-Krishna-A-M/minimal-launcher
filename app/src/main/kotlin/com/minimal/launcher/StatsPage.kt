package com.minimal.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minimal.launcher.ui.theme.DeepNavy
import com.minimal.launcher.ui.theme.TerminalBlue
import java.util.concurrent.TimeUnit

// --- DATA STRUCTURES (Fixes the "Unresolved reference" errors) ---
data class InfoResult(val used: String, val total: String, val percent: Float)
data class BatteryResult(val level: Int, val status: String)

@Composable
fun StatsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    
    val batteryInfo = remember { getBatteryInfo(context) }
    val ramInfo = remember { getRamInfo(context) }
    val storageInfo = remember { getStorageInfo() }
    val uptime = remember { getUptime() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy.copy(alpha = 0.9f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text("SYSTEM_DIAGNOSTICS", 
            style = MaterialTheme.typography.labelSmall, 
            color = TerminalBlue.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("--- IDENTITY ---", style = MaterialTheme.typography.labelSmall, color = TerminalBlue.copy(alpha = 0.4f))
                StatBlock("MODEL", "${Build.MANUFACTURER} ${Build.MODEL}", 1f)
                StatBlock("OS_VER", "ANDROID ${Build.VERSION.RELEASE}", 1f)
                StatBlock("UPTIME", uptime, 1f)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text("--- RESOURCES ---", style = MaterialTheme.typography.labelSmall, color = TerminalBlue.copy(alpha = 0.4f))
                StatBlock("BATTERY", "${batteryInfo.level}% [${batteryInfo.status}]", batteryInfo.level / 100f)
                StatBlock("MEMORY", "${ramInfo.used} / ${ramInfo.total} GB", ramInfo.percent)
                StatBlock("STORAGE", "${storageInfo.used} / ${storageInfo.total} GB", storageInfo.percent)
                StatBlock("NETWORK", getNetworkInfo(context), 1f)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("TERMINATE_SESSION: SWIPE_DOWN", 
            style = MaterialTheme.typography.labelSmall, 
            color = TerminalBlue.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
fun StatBlock(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TerminalBlue)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TerminalBlue)
        }
        if (label == "BATTERY" || label == "MEMORY" || label == "STORAGE") {
            // FIX: Changed from lambda to direct Float value for older M3 versions
            LinearProgressIndicator(
                progress = progress, 
                modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 6.dp),
                color = TerminalBlue,
                trackColor = TerminalBlue.copy(alpha = 0.1f)
            )
        }
    }
}

// --- HELPERS ---

private fun getUptime(): String {
    val millis = SystemClock.elapsedRealtime()
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "${hours}h ${minutes}m"
}

private fun getNetworkInfo(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return "OFFLINE"
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "OFFLINE"
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI_CONNECT"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL_CARRIER"
        else -> "LINK_ESTABLISHED"
    }
}

private fun getBatteryInfo(context: Context): BatteryResult {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val status = if (statusInt == BatteryManager.BATTERY_STATUS_CHARGING) "CHR" else "BAT"
    return BatteryResult(level, status)
}

private fun getRamInfo(context: Context): InfoResult {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val total = memInfo.totalMem / 1073741824.0 
    val used = total - (memInfo.availMem / 1073741824.0)
    return InfoResult("%.1f".format(used), "%.1f".format(total), (used / total).toFloat())
}

private fun getStorageInfo(): InfoResult {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = (stat.blockCountLong * stat.blockSizeLong) / 1073741824.0
    val used = total - ((stat.availableBlocksLong * stat.blockSizeLong) / 1073741824.0)
    return InfoResult("%.1f".format(used), "%.1f".format(total), (used / total).toFloat())
}
