package com.sonnenstahl.caffeine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager.WakeLock
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object Caffeine {
    suspend fun caffeinate(context: Context, time: Int) {
        mutex.withLock {
            maxTimeSeconds.value = time*60
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            if (powerManager.isPowerSaveMode) {
                alert(context, "cannot caffeinate when in power saving mode")
                vibrateDevice(context)
                return
            }

            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                LOCK_LABEL
            )

            wakeLock?.acquire((maxTimeSeconds.value*1000).toLong())
            isScreenOn.value = true
        }
    }

    suspend fun decaf() {
        mutex.withLock {
            wakeLock?.release()
            isScreenOn.value = false
        }
    }

    fun setMaxTimer(timerSeconds: Int) {
        maxTimeSeconds.value = timerSeconds
    }

    fun toggleSleepOnLock() {
        sleepOnLock.value = !sleepOnLock.value
    }

    var maxTimeSeconds = MutableStateFlow(30) // max would be 60 minutes
    var sleepOnLock = MutableStateFlow(false)

    private var wakeLock: WakeLock? = null
    private var LOCK_LABEL: String = "Caffeine::ScreenLock"
    private var mutex = Mutex()

    var isScreenOn = MutableStateFlow(wakeLock?.isHeld == true)

}

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF && Caffeine.sleepOnLock.value == true) {
            CoroutineScope(Dispatchers.Main).launch {
                Caffeine.decaf()
            }
        }
    }
}