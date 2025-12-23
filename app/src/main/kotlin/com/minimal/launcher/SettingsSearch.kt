package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.minimal.launcher.ui.theme.DeepNavy
import com.minimal.launcher.ui.theme.TerminalBlue

@Composable
fun SettingsSearch(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    
    // Comprehensive Power-User Settings Map
    val settingsMap = remember {
        mapOf(
            "wifi" to Settings.ACTION_WIFI_SETTINGS,
            "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "display" to Settings.ACTION_DISPLAY_SETTINGS,
            "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "apps" to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
            "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "developer" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "security" to Settings.ACTION_SECURITY_SETTINGS,
            "date" to Settings.ACTION_DATE_SETTINGS,
            "sound" to Settings.ACTION_SOUND_SETTINGS,
            "network" to Settings.ACTION_WIRELESS_SETTINGS,
            "input" to Settings.ACTION_INPUT_METHOD_SETTINGS,
            "access" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            "privacy" to Settings.ACTION_PRIVACY_SETTINGS,
            "biometrics" to Settings.ACTION_FINGERPRINT_ENROLL,
            "nfc" to Settings.ACTION_NFC_SETTINGS,
            "vpn" to Settings.ACTION_VPN_SETTINGS,
            "data_usage" to Settings.ACTION_DATA_USAGE_SETTINGS,
            "info" to Settings.ACTION_DEVICE_INFO_SETTINGS
        ).toSortedMap()
    }

    val filteredSettings = remember(searchQuery) {
        settingsMap.filter { it.key.contains(searchQuery, ignoreCase = true) }.toList()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(DeepNavy).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SET: ", color = TerminalBlue, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TerminalBlue),
                cursorBrush = SolidColor(TerminalBlue),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (filteredSettings.isNotEmpty()) {
                            launchSettings(context, filteredSettings.first().second)
                            onBack()
                        }
                    }
                )
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = TerminalBlue.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            items(filteredSettings) { (name, action) ->
                Text(
                    text = "[ ${name.lowercase()} ]",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            launchSettings(context, action)
                            onBack()
                        }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TerminalBlue
                )
            }
        }
    }
}

private fun launchSettings(context: Context, action: String) {
    val intent = Intent(action).apply { 
        flags = Intent.FLAG_ACTIVITY_NEW_TASK 
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Log error if needed for debugging
    }
}
