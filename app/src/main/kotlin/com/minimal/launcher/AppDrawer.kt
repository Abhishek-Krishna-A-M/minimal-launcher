package com.minimal.launcher

import android.content.*
import android.net.Uri
import android.provider.CallLog
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.minimal.launcher.ui.theme.DeepNavy
import com.minimal.launcher.ui.theme.TerminalBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.provider.ContactsContract

/* -------------------------------------------------- */
/* MODELS                                            */
/* -------------------------------------------------- */

data class ContactEntry(
    val name: String,
    val number: String,
    val nameLower: String = name.lowercase()
)

/* -------------------------------------------------- */
/* GLOBAL CACHE                                      */
/* -------------------------------------------------- */

private var globalAppCache: List<AppInfo>? = null
private var globalContactsCache: List<ContactEntry>? = null

/* -------------------------------------------------- */
/* APP DRAWER                                        */
/* -------------------------------------------------- */

@Composable
fun AppDrawer(onBack: () -> Unit) {

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val prefs = remember {
        context.getSharedPreferences("app_usage_cache", Context.MODE_PRIVATE)
    }

    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var recentContacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }

    /* ---------- LOAD APPS ONCE ---------- */
    LaunchedEffect(Unit) {
        if (globalAppCache == null) {
            globalAppCache = loadApps(context)
        }
        apps = globalAppCache!!
        focusRequester.requestFocus()
    }

/* ---------- LOAD CONTACT SOURCES (TIERED) ---------- */
LaunchedEffect(query) {

    when {
        // Tier-1: just "p:" or "wa:" → recent calls only
        (query == "p:" || query == "wa:") -> {
            if (recentContacts.isEmpty()) {
                recentContacts = loadRecentContacts(context)
            }
        }

        // Tier-2: "p:d", "p:dad", etc → full contacts (once)
        query.startsWith("p:") && query.length > 3 -> {
            if (globalContactsCache == null) {
                globalContactsCache = loadAllContacts(context)
            }
        }

        // Leaving contact mode → clear recent list
else -> {
    if (!query.startsWith("p:") && !query.startsWith("wa:")) {
        recentContacts = emptyList()
    }
}
    }
}
    /* ---------- RESULTS ---------- */
    val results by remember(query, apps, recentContacts) {
        derivedStateOf {
            val q = query.trim().lowercase()

            when {
                q == "p:" || q == "wa:" ->
                    recentContacts

q.startsWith("p:") -> {
    val key = q.drop(2).trim()

    when {
        key.isEmpty() ->
            recentContacts                         // Tier-1

        key.any { it.isDigit() } ->
            listOf(ContactEntry(key, normalizePhoneNumber(key)))

        globalContactsCache != null ->
            fuzzyContacts(key, globalContactsCache!!)

        else ->
            emptyList()
    }
}

                q.isEmpty() -> {
                    val now = System.currentTimeMillis()
                    apps.filter {
                        now - prefs.getLong(it.packageName, 0L) < 3_600_000
                    }
                }

                q == "ls" -> apps

                q.startsWith("g:") ||
                q.startsWith("m:") ||
                q.startsWith("yt:") ||
                q.startsWith("rm ") ||
                q.startsWith("install ") ||
                q == "cls" ||
                q == "exit" ||
                q == "quit" -> emptyList()

                else -> fuzzyApps(q, apps)
            }
        }
    }

    /* ---------- STYLE ---------- */
    val terminalStyle = TextStyle(
        color = TerminalBlue,
        fontFamily = FontFamily.Monospace,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize
    )

    /* ---------- UI ---------- */
    Column(
        Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RUN: ", style = terminalStyle)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = terminalStyle,
                cursorBrush = SolidColor(TerminalBlue),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        execute(query, results, context, prefs, onBack)
                        query = ""
                    }
                )
            )
        }

        Spacer(Modifier.height(8.dp))
        Divider(color = TerminalBlue.copy(alpha = 0.3f))
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {

            item {
                Text(
                    text = when {
                        query.startsWith("p:") || query.startsWith("wa:") ->
                            "--- RECENT CONTACTS ---"
                        query.isEmpty() ->
                            "--- SESSION_ACTIVE (1H) ---"
                        query == "ls" ->
                            "--- APPLICATIONS ---"
                        else ->
                            "--- SEARCH_RESULTS ---"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalBlue.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(results) { item ->
                Text(
                    text = when (item) {
                        is AppInfo -> "> ${item.labelLower}"
                        is ContactEntry -> "> ${item.nameLower}"
                        else -> ""
                    },
                    style = terminalStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            executeItem(item, query, context, prefs, onBack)
                        }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

/* -------------------------------------------------- */
/* EXECUTION                                         */
/* -------------------------------------------------- */

private fun execute(
    query: String,
    results: List<Any>,
    context: Context,
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    val q = query.trim()

    when {
        q == "exit" || q == "quit" -> onBack()
        q == "cls" -> prefs.edit().clear().apply()

        q.startsWith("g:") ->
            openWeb(context, "https://www.google.com/search?q=${q.drop(2)}")

        q.startsWith("install ") ->
            openWeb(context, "market://search?q=${q.drop(8)}")

        q.startsWith("rm ") -> {
            val target = q.drop(3)
            results.filterIsInstance<AppInfo>()
                .firstOrNull { it.labelLower == target || it.packageName == target }
                ?.let {
                    context.startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${it.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
        }

        results.isNotEmpty() ->
            executeItem(results.first(), q, context, prefs, onBack)
    }
}

private fun executeItem(
    item: Any,
    query: String,
    context: Context,
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    when (item) {
        is AppInfo ->
            launchAndTrack(context, item.packageName, prefs, onBack)

        is ContactEntry -> {
            if (query.startsWith("wa:")) {
                openWeb(context, "https://wa.me/${item.number}")
            } else {
callOrDial(context, item.number)
            }
            onBack()
        }
    }
}

/* -------------------------------------------------- */
/* LOADERS                                           */
/* -------------------------------------------------- */

private suspend fun loadApps(context: Context): List<AppInfo> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .sortedBy { it.labelLower }
    }

private fun loadRecentContacts(context: Context): List<ContactEntry> {
    val result = ArrayList<ContactEntry>()
    val seen = HashSet<String>()

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        while (it.moveToNext() && result.size < 25) {
            val number = it.getString(0) ?: continue
            if (!seen.add(number)) continue
            val name = it.getString(1) ?: number
result += ContactEntry(
    name = name,
    number = normalizePhoneNumber(number)
)
        }
    }
    return result
}

/* -------------------------------------------------- */
/* FUZZY                                             */
/* -------------------------------------------------- */

private fun fuzzyApps(query: String, apps: List<AppInfo>): List<AppInfo> =
    apps.mapNotNull {
        val score = fuzzyScore(query, it.labelLower)
        if (score >= 0) it to score else null
    }.sortedByDescending { it.second }.map { it.first }

private fun fuzzyContacts(query: String, contacts: List<ContactEntry>): List<ContactEntry> =
    if (query.isEmpty()) contacts else
        contacts.mapNotNull {
            val score = fuzzyScore(query, it.nameLower)
            if (score >= 0) it to score else null
        }.sortedByDescending { it.second }.map { it.first }

private fun fuzzyScore(query: String, target: String): Int {
    var score = 0
    var qi = 0
    for (c in target) {
        if (qi < query.length && c == query[qi]) {
            score += 10
            qi++
        }
    }
    return if (qi == query.length) score else -1
}

/* -------------------------------------------------- */
/* UTILS                                             */
/* -------------------------------------------------- */
private fun normalizePhoneNumber(raw: String): String {
    val digits = raw.filter(Char::isDigit)

    return when {
        digits.length == 10 -> digits
        digits.length == 12 && digits.startsWith("91") -> "+$digits"
        digits.length > 10 -> "+$digits"
        else -> digits
    }
}

private fun openWeb(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun launchAndTrack(
    context: Context,
    pkg: String,
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    prefs.edit().putLong(pkg, System.currentTimeMillis()).apply()
    context.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
        onBack()
    }
}

private fun callOrDial(
    context: Context,
    number: String
) {
    val uri = Uri.parse("tel:$number")

    if (
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        context.startActivity(
            Intent(Intent.ACTION_CALL, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } else {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
private fun canDirectCall(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
}
private fun loadAllContacts(context: Context): List<ContactEntry> {
    val list = ArrayList<ContactEntry>()

    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(0) ?: continue
            val number = normalizePhoneNumber(it.getString(1) ?: continue)
            list += ContactEntry(name, number)
        }
    }
    return list
}

