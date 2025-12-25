package com.minimal.launcher

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
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
import androidx.core.content.ContextCompat
import com.minimal.launcher.ui.theme.DeepNavy
import com.minimal.launcher.ui.theme.TerminalBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/* -------------------------------------------------- */
/* MODELS                                            */
/* -------------------------------------------------- */

data class ContactEntry(
    val name: String,
    val number: String,
    val nameLower: String = name.lowercase()
)

/* -------------------------------------------------- */
/* GLOBAL CACHES                                     */
/* -------------------------------------------------- */

private var appCache: List<AppInfo>? = null
private var contactsCache: List<ContactEntry>? = null

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
    var recentCalls by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }

    /* ---------- LOAD APPS ONCE ---------- */
    LaunchedEffect(Unit) {
        if (appCache == null) {
            appCache = loadApps(context)
        }
        apps = appCache!!
        focusRequester.requestFocus()
    }

    /* ---------- CONTACT LOADING (ON DEMAND) ---------- */
    LaunchedEffect(query) {
        when {
            query == "p:" -> {
                if (recentCalls.isEmpty()) {
                    recentCalls = loadRecentCalls(context)
                }
            }

            query.startsWith("p:") && query.length > 3 -> {
                if (contactsCache == null) {
                    contactsCache = loadAllContacts(context)
                }
            }

            !query.startsWith("p:") -> {
                recentCalls = emptyList()
            }
        }
    }

    /* ---------- RESULTS ---------- */
    val results by remember(query, apps, recentCalls) {
        derivedStateOf {
            val q = query.trim().lowercase()

            when {
                q == "p:" ->
                    recentCalls

                q.startsWith("p:") -> {
                    val key = q.drop(2).trim()

                    when {
                        key.isEmpty() ->
                            recentCalls

                        key.any(Char::isDigit) ->
                            listOf(ContactEntry(key, normalizeNumber(key)))

                        contactsCache != null ->
                            fuzzyContacts(key, contactsCache!!)

                        else -> emptyList()
                    }
                }

                q.isEmpty() -> {
                    val now = System.currentTimeMillis()
                    apps.filter {
                        now - prefs.getLong(it.packageName, 0L) < 3_600_000
                    }
                }
q.startsWith("rm") -> {
    val key = q.removePrefix("rm").trim()

    if (key.isEmpty()) {
        apps
    } else {
        fuzzyApps(key, apps)
    }
}

                q == "ls" -> apps

                q.startsWith("g:") ||
                q.startsWith("install ") ||
                q == "cls" ||
                q == "exit" ||
                q == "quit" ->
                    emptyList()

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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
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
                        query.startsWith("rm") -> "--- UNINSTALL ---"
                        query.startsWith("p:") -> "--- CONTACTS ---"
                        query.isEmpty() -> "--- SESSION_ACTIVE (1H) ---"
                        query == "ls" -> "--- APPLICATIONS ---"
                        else -> "--- SEARCH_RESULTS ---"
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

q.startsWith("rm") && results.isNotEmpty() -> {
    val app = results.first() as AppInfo
    context.startActivity(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
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
            callOrDial(context, item.number)
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

private fun loadRecentCalls(context: Context): List<ContactEntry> {
    val list = ArrayList<ContactEntry>()
    val seen = HashSet<String>()

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        while (it.moveToNext() && list.size < 25) {
            val raw = it.getString(0) ?: continue
            if (!seen.add(raw)) continue
            val name = it.getString(1) ?: raw
            list += ContactEntry(name, normalizeNumber(raw))
        }
    }
    return list
}

private fun loadAllContacts(context: Context): List<ContactEntry> {
    val list = ArrayList<ContactEntry>()

    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(0) ?: continue
            val number = normalizeNumber(it.getString(1) ?: continue)
            list += ContactEntry(name, number)
        }
    }
    return list
}

/* -------------------------------------------------- */
/* FUZZY                                             */
/* -------------------------------------------------- */

private fun fuzzyApps(q: String, apps: List<AppInfo>): List<AppInfo> =
    apps.mapNotNull {
        val s = fuzzyScore(q, it.labelLower)
        if (s >= 0) it to s else null
    }.sortedByDescending { it.second }.map { it.first }

private fun fuzzyContacts(q: String, contacts: List<ContactEntry>): List<ContactEntry> =
    contacts.mapNotNull {
        val s = fuzzyScore(q, it.nameLower)
        if (s >= 0) it to s else null
    }.sortedByDescending { it.second }.map { it.first }

private fun fuzzyScore(q: String, t: String): Int {
    var score = 0
    var qi = 0
    var lastMatch = -1

    for (i in t.indices) {
        if (qi < q.length && t[i] == q[qi]) {
            score += 10

            // Penalize gaps
            if (lastMatch >= 0) {
                score -= (i - lastMatch - 1)
            }

            // Early match bonus
            if (qi == 0) {
                score += (t.length - i)
            }

            lastMatch = i
            qi++
        }
    }

    if (qi != q.length) return -1

    // Strong prefix bonus
    if (t.startsWith(q)) score += 50

    return score
}

/* -------------------------------------------------- */
/* UTILS                                             */
/* -------------------------------------------------- */

private fun normalizeNumber(raw: String): String {
    val d = raw.filter(Char::isDigit)
    return when {
        d.length == 10 -> d
        d.length > 10 -> "+$d"
        else -> d
    }
}

private fun callOrDial(context: Context, number: String) {
    val uri = Uri.parse("tel:$number")
    val canCall =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

    val intent = Intent(
        if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL,
        uri
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(intent)
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
