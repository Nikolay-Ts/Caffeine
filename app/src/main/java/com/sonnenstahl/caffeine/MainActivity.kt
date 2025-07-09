package com.sonnenstahl.caffeine

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnenstahl.caffeine.ui.theme.CaffeineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class MainActivity : ComponentActivity() {
    private lateinit var screenOffReceiver: ScreenOffReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenOffReceiver = ScreenOffReceiver()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        enableEdgeToEdge()
        setContent {
            CaffeineTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
    }
}

@Composable
fun Greeting() {
    val coroutine = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isScreenOn = Lock.isScreenOn.collectAsStateWithLifecycle()
    val maxTimeSeconds = Lock.maxTimeSeconds.collectAsStateWithLifecycle()
    val turnOfOnLock   = Lock.sleepOnLock.collectAsStateWithLifecycle()
    val customMaxTimeSeconds = remember { mutableIntStateOf(maxTimeSeconds.value) }
    val viewWidth = configuration.screenWidthDp*0.80
    val countdownTime = remember { mutableIntStateOf(customMaxTimeSeconds.intValue * 60) }


    DisposableEffect(lifecycleOwner, context) {
        val screenOffReceiver = ScreenOffReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(screenOffReceiver, intentFilter)
        val activityLifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                coroutine.launch {
                    Lock.unlock()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(activityLifecycleObserver)
        onDispose {
            context.unregisterReceiver(screenOffReceiver)
            lifecycleOwner.lifecycle.removeObserver(activityLifecycleObserver)
        }
    }

    LaunchedEffect(isScreenOn.value, customMaxTimeSeconds.intValue) {
        if (isScreenOn.value) {
            countdownTime.intValue = customMaxTimeSeconds.intValue * 60
            while (countdownTime.intValue > 0 && isScreenOn.value) {
                delay(1000L)
                countdownTime.intValue--
            }
            if (countdownTime.intValue <= 0 && isScreenOn.value) {
                Lock.unlock()
            }
        } else {
            countdownTime.intValue = customMaxTimeSeconds.intValue * 60
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .width(viewWidth.dp)
                .padding(horizontal = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = convertMinutesToMMSS(countdownTime.intValue),
            modifier =
                Modifier
                    .size(50.dp)
        )

        Slider(
            value = customMaxTimeSeconds.intValue.toFloat(),
            valueRange = 0f..60f,
            onValueChange = {
                if (! isScreenOn.value) {
                    customMaxTimeSeconds.intValue = it.toInt()
                }
            },
            onValueChangeFinished = {
                if (isScreenOn.value) {
                    vibrateDevice(context)
                }
            },
            modifier =
                Modifier
                    .width((configuration.screenWidthDp*0.80).dp)
        )

        Row(
            modifier =
                Modifier
                    .padding(vertical = 20.dp)
                    .padding(horizontal = 10.dp)
                    .width(viewWidth.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text ("Turn off when locked")

            Switch(
                checked = turnOfOnLock.value,
                onCheckedChange = {
                    if (! isScreenOn.value) {
                        Lock.toggleSleepOnLock()
                    } else { vibrateDevice(context) }
                }
            )
        }



        Button(
            onClick = {
                Lock.setMaxTimer(customMaxTimeSeconds.intValue*60)
                coroutine.launch {
                    when (isScreenOn.value) {
                        true -> Lock.unlock()
                        false -> Lock.lock(context, customMaxTimeSeconds.intValue)
                    }
                }
            }
        ) {
          when (isScreenOn.value) {
              true ->  Text("The Lock is ON")
              false -> Text("The Lock is OFF")
          }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CaffeineTheme {
        Greeting()
    }
}

