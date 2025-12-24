package com.minimal.launcher

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.minimal.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.abs

enum class Screen { HOME, STATS, APPS, SETTINGS_SEARCH }

class MainActivity : ComponentActivity() {
    private var screenState = mutableStateOf(Screen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Remove black bars by allowing content to flow behind status/nav bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 2. Force the UI to use the notch/cutout area
        window.attributes.layoutInDisplayCutoutMode = 
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        setContent {
            val context = LocalContext.current
            val isDarkText = remember { checkWallpaperLuminance(context) }
            
            LauncherTheme(darkText = isDarkText) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        // Make bars completely transparent
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = isDarkText
                        insetsController.isAppearanceLightNavigationBars = isDarkText
                    }
                }

                // Root Box: No padding here so Scanlines fill the GLASS edges
                Box(modifier = Modifier.fillMaxSize()) {
                    ScanlineOverlay() 
                    MainContainer(screenState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Return to home screen when 'Home' button is pressed
        screenState.value = Screen.HOME
    }

    private fun checkWallpaperLuminance(context: android.content.Context): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val d = wm.drawable
            if (d is BitmapDrawable) {
                val b = Bitmap.createScaledBitmap(d.bitmap, 1, 1, true)
                val p = b.getPixel(0, 0)
                (0.299 * android.graphics.Color.red(p) + 0.587 * android.graphics.Color.green(p) + 0.114 * android.graphics.Color.blue(p)) > 160
            } else false
        } catch (e: Exception) { false }
    }
}

@Composable
fun MainContainer(state: MutableState<Screen>) {
    var current by state
    BackHandler(enabled = current != Screen.HOME) { current = Screen.HOME }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(onDrag = { change, dragAmount ->
                change.consume()
                val (x, y) = dragAmount
                val s = 40f
                if (abs(x) > abs(y)) {
                    if (x > s) current = Screen.SETTINGS_SEARCH 
                    if (x < -s) current = Screen.STATS
                } else {
                    if (y < -s) current = Screen.APPS
                    if (y > s) current = Screen.HOME
                }
            })
        }) {
            // Apply Safe Area padding only to the content (Text/Buttons), not the background
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
                when (current) {
                    Screen.HOME -> HomeScreen()
                    Screen.STATS -> StatsPage { current = Screen.HOME }
                    Screen.APPS -> AppDrawer { current = Screen.HOME }
                    Screen.SETTINGS_SEARCH -> SettingsSearch { current = Screen.HOME }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Calendar.getInstance().time
            time = timeFormat.format(now)
            date = dateFormat.format(now)
            delay(10000)
        }
    }

    // Double Tap to Lock Logic
    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    val locked = LockService.instance?.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                    )
                    if (locked != true) {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )
        }
        .padding(32.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(text = time, style = MaterialTheme.typography.displayLarge)
            Text(
                text = date, 
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.15f)) {
        for (y in 0..size.height.toInt() step 6) {
            drawLine(
                color = Color.Black,
                start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                strokeWidth = 2f
            )
        }
    }
}
