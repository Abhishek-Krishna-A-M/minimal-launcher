package com.minimal.launcher

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.WallpaperManager
import android.content.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.minimal.launcher.ui.theme.LauncherTheme
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val screenState = mutableStateOf(Screen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            var lightWallpaper by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                lightWallpaper = isWallpaperLight(context)
            }

            LauncherTheme(darkText = lightWallpaper) {
                val view = LocalView.current
                SideEffect {
                    val w = (view.context as Activity).window
                    w.statusBarColor = android.graphics.Color.TRANSPARENT
                    w.navigationBarColor = android.graphics.Color.TRANSPARENT
                    WindowCompat.getInsetsController(w, view).apply {
                        isAppearanceLightStatusBars = lightWallpaper
                        isAppearanceLightNavigationBars = lightWallpaper
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    StaticScanlineOverlay()
                    MainContainer(screenState)
                }
            }
        }
    }

    private fun isWallpaperLight(context: Context): Boolean {
        return try {
            val d = WallpaperManager.getInstance(context).drawable
            if (d is BitmapDrawable) {
                val p = d.bitmap.getPixel(0, 0)
                (0.299 * android.graphics.Color.red(p)
                        + 0.587 * android.graphics.Color.green(p)
                        + 0.114 * android.graphics.Color.blue(p)) > 160
            } else false
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun MainContainer(state: MutableState<Screen>) {
    var current by state
    BackHandler(enabled = current != Screen.HOME) { current = Screen.HOME }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val dx: Float = dragAmount.x
                    val dy: Float = dragAmount.y

                    if (abs(dx) > 80f || abs(dy) > 80f) {
                        change.consume()

                        when {
                            abs(dx) > abs(dy) ->
                                current =
                                    if (dx > 0f) Screen.SETTINGS_SEARCH
                                    else Screen.STATS

                            dy < 0f -> current = Screen.APPS
                            dy > 0f -> current = Screen.HOME
                        }
                    }
                }
            }
    ) {
        when (current) {
            Screen.HOME -> HomeScreen()
            Screen.APPS -> AppDrawer { current = Screen.HOME }
            Screen.STATS -> StatsPage { current = Screen.HOME }
            Screen.SETTINGS_SEARCH -> SettingsSearch { current = Screen.HOME }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    var time by remember { mutableStateOf(timeFormat.format(Date())) }
    var date by remember { mutableStateOf(dateFormat.format(Date())) }

    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val now = Date()
                time = timeFormat.format(now)
                date = dateFormat.format(now)
            }
        }
        context.registerReceiver(r, IntentFilter(Intent.ACTION_TIME_TICK))
        onDispose { context.unregisterReceiver(r) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        LockService.instance?.performGlobalAction(
                            AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                        )
                    }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
Text(
    text = time,
    style = MaterialTheme.typography.displayLarge,
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
)
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StaticScanlineOverlay() {
    val scanlineBitmap: ImageBitmap = remember {
        val bmp = android.graphics.Bitmap.createBitmap(
            400, 800, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            alpha = 18
            strokeWidth = 1f
        }
        for (y in 0 until 800 step 4) {
            canvas.drawLine(0f, y.toFloat(), 400f, y.toFloat(), paint)
        }
        bmp.asImageBitmap()
    }

    Image(
        bitmap = scanlineBitmap,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.06f)
    )
}
