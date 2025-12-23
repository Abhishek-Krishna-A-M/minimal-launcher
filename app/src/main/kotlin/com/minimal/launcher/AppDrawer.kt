package com.minimal.launcher

import android.content.Context
import android.content.Intent
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
    var searchQuery by remember { mutableStateOf("") }
    
    val allApps = remember { getInstalledApps(context) }
    
    // DEFINE YOUR ALIASES HERE
    val aliases = remember {
        mapOf(
            "c" to "com.android.camera",
            "t" to "com.termux",
            "s" to "com.android.settings" // Alias 'm' for main settings
        )
    }

    val filteredApps = remember(searchQuery) {
        when {
            searchQuery.trim() == "ls" -> allApps
            searchQuery.startsWith("google ") -> emptyList()
            else -> allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
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
                    onGo = { handleCommand(searchQuery, filteredApps, aliases, context, onBack) }
                )
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = TerminalBlue.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            if (searchQuery.isEmpty()) {
                item {
                    Text(
                        text = "aliases: ${aliases.keys.joinToString(", ")} | type 'ls' for all",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalBlue.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(filteredApps) { app ->
                Text(
                    text = "> ${app.label.lowercase()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchApp(context, app.packageName) }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TerminalBlue
                )
            }
        }
    }
}

private fun handleCommand(query: String, filtered: List<AppInfo>, aliases: Map<String, String>, context: Context, onBack: () -> Unit) {
    val input = query.trim().lowercase()
    when {
        input == "exit" || input == "quit" -> onBack()
        aliases.containsKey(input) -> {
            launchApp(context, aliases[input]!!)
            onBack()
        }
        input.startsWith("google ") -> {
            val q = query.substringAfter("google ")
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
        }
        filtered.isNotEmpty() -> {
            launchApp(context, filtered.first().packageName)
            onBack()
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent?.let { context.startActivity(it) }
}

private fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .sortedBy { it.label.lowercase() }
}
