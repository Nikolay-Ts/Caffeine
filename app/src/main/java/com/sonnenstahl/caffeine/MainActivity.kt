package com.sonnenstahl.caffeine

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnenstahl.caffeine.ui.theme.CaffeineTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val receiver = ScreenOffReceiver()
    val isScreenOn = Lock.isScreenOn.collectAsStateWithLifecycle()
    val maxTimeSeconds = Lock.maxTimeSeconds.collectAsStateWithLifecycle()
    val customMaxTimeSeconds = remember { mutableFloatStateOf(30F) }

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
          value = customMaxTimeSeconds.floatValue,
          valueRange = 0f..60f,
          onValueChange = {customMaxTimeSeconds.floatValue = it }
        )

        Text("${customMaxTimeSeconds.floatValue.toInt()}")

        Button(
            onClick = {
                Lock.setMaxTimer(customMaxTimeSeconds.floatValue.toInt()*60)

                coroutine.launch {
                    when (isScreenOn.value) {
                        true -> Lock.unlock()
                        false -> Lock.lock(context)
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
        Greeting("Android")
    }
}