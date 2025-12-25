package com.minimal.launcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal AccessibilityService used ONLY for locking the screen.
 *
 * Design goals:
 * - Zero event listening
 * - Zero background work
 * - No sticky behavior
 * - No wakeups
 * - Let Android fully manage lifecycle
 */
class LockService : AccessibilityService() {

    override fun onServiceConnected() {
        // Explicitly tell Android we want NOTHING except global actions
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = 0                    // listen to nothing
            feedbackType = 0                 // no feedback
            notificationTimeout = 0
            flags = 0                        // no default flags
        }

        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Will never be called because eventTypes = 0
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: LockService? = null
            private set
    }
}
