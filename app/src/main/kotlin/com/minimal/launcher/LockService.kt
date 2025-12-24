package com.minimal.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class LockService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for lock functionality
    }

    override fun onInterrupt() {
        // Not needed
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    // This is the key fix for the malfunction
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY tells Android to recreate the service if it's killed due to memory pressure
        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        @Volatile
        var instance: LockService? = null
            private set(value) {
                field = value
            }
    }
}
