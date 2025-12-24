package com.minimal.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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

data class AppInfo(val label: String, val packageName: String)

@Composable
fun AppDrawer(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val prefs = remember { context.getSharedPreferences("app_usage_cache", Context.MODE_PRIVATE) }
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfo>>(getInstalledApps(context)) }

    // BroadcastReceiver for real-time list updates
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                context?.let { allApps = getInstalledApps(it) }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val aliases = remember {
        mapOf(
            "c" to "com.android.camera",
            "t" to "com.termux",
            "s" to "com.android.settings"
        )
    }

    // High-speed filtering logic
    val filteredApps = remember(searchQuery, allApps) {
        val query = searchQuery.trim().lowercase()
        val now = System.currentTimeMillis()
        val oneHour = 3600000L

        when {
            query == "ls" -> allApps
            query.isEmpty() -> {
                allApps.filter { (now - prefs.getLong(it.packageName, 0L)) < oneHour }
            }
            query in listOf("cls", "exit", "quit") -> emptyList()
            query.startsWith("rm ") || query.startsWith("install ") -> emptyList()
            else -> {
                // Better search: matches starting with query first, then matches containing query
                val startsWith = allApps.filter { it.label.lowercase().startsWith(query) }
                val contains = allApps.filter { it.label.lowercase().contains(query) && !it.label.lowercase().startsWith(query) }
                startsWith + contains
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(DeepNavy).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RUN: ", color = TerminalBlue, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TerminalBlue),
                cursorBrush = SolidColor(TerminalBlue),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { 
                        handleCommand(searchQuery, allApps, aliases, context, prefs, onBack)
                        searchQuery = "" 
                    }
                )
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = TerminalBlue.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                val headerText = when {
                    searchQuery.isEmpty() -> "--- SESSION_ACTIVE (1H) ---"
                    searchQuery == "ls" -> "--- ALL_PACKAGES ---"
                    else -> "--- SEARCH_RESULTS ---"
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalBlue.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(filteredApps, key = { it.packageName }) { app ->
                Text(
                    text = "> ${app.label.lowercase()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            prefs.edit().putLong(app.packageName, System.currentTimeMillis()).apply()
                            launchApp(context, app.packageName)
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

private fun handleCommand(
    query: String, 
    allApps: List<AppInfo>, 
    aliases: Map<String, String>, 
    context: Context, 
    prefs: android.content.SharedPreferences,
    onBack: () -> Unit
) {
    val input = query.trim().lowercase()
    when {
        input == "exit" || input == "quit" -> onBack()
        input == "cls" -> prefs.edit().clear().apply()
        
        // --- RESTORED GOOGLE SEARCH ---
        input.startsWith("google ") -> {
            val q = query.substringAfter("google ").trim()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        
        // --- FIXED RM LOGIC (Triggers System Popup) ---
        input.startsWith("rm ") -> {
            val target = input.removePrefix("rm ").trim()
            // Find by label or package name
            val app = allApps.find { it.label.equals(target, ignoreCase = true) } 
                   ?: allApps.find { it.packageName.equals(target, ignoreCase = true) }
            
            app?.let {
                // This triggers the standard Android "Do you want to uninstall this app?" dialog
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${it.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                // We don't remove from cache here; the BroadcastReceiver will handle it 
                // once the system actually finishes the uninstallation.
            }
        }
        
        input.startsWith("install ") -> {
            val q = input.removePrefix("install ").trim()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        aliases.containsKey(input) -> {
            val pkg = aliases[input]!!
            prefs.edit().putLong(pkg, System.currentTimeMillis()).apply()
            launchApp(context, pkg)
            onBack()
        }

        else -> {
            val match = allApps.find { it.label.lowercase().startsWith(input) } 
                ?: allApps.find { it.label.contains(input, ignoreCase = true) }
            
            match?.let {
                prefs.edit().putLong(it.packageName, System.currentTimeMillis()).apply()
                launchApp(context, it.packageName)
                onBack()
            }
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}

private fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0)
        .asSequence() // Use sequence for faster mapping on large app lists
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .sortedBy { it.label.lowercase() }
        .toList()
}
