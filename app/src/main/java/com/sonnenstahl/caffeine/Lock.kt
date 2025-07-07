package com.sonnenstahl.caffeine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager.WakeLock
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

object Lock {

    suspend fun lock(context: Context) {
        mutex.withLock {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                LOCK_LABEL
            )

            wakeLock?.acquire((maxTimeSeconds.value*1000).toLong())
            isScreenOn.value = true
        }
    }

    suspend fun unlock() {
        mutex.withLock {
            wakeLock?.release()
            isScreenOn.value = false
        }
    }

    fun setMaxTimer(timerSeconds: Int) {
        maxTimeSeconds.value = timerSeconds
    }



    var onTimeSeconds = MutableStateFlow(0)
    var maxTimeSeconds = MutableStateFlow(60*60) // max would be 60 minutes

    private var wakeLock: WakeLock? = null
    private var LOCK_LABEL: String = "Caffeine::ScreenLock"
    private var mutex = Mutex()

    var isScreenOn = MutableStateFlow(wakeLock?.isHeld == true)

}

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            CoroutineScope(Dispatchers.Main).launch {
                Lock.unlock()
            }
        }
    }
}