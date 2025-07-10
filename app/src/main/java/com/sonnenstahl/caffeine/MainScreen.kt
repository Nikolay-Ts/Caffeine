package com.sonnenstahl.caffeine

import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnenstahl.caffeine.ui.theme.Background
import com.sonnenstahl.caffeine.ui.theme.InactiveColor
import com.sonnenstahl.caffeine.ui.theme.PrimaryColor
import com.sonnenstahl.caffeine.ui.theme.TextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val coroutine = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isScreenOn = Caffeine.isScreenOn.collectAsStateWithLifecycle()
    val maxTimeSeconds = Caffeine.maxTimeSeconds.collectAsStateWithLifecycle()
    val turnOfOnLock   = Caffeine.sleepOnLock.collectAsStateWithLifecycle()
    val customMaxTimeSeconds = remember { mutableIntStateOf(maxTimeSeconds.value) }
    val countdownTime = remember { mutableIntStateOf(customMaxTimeSeconds.intValue * 60) }

    val viewWidth = configuration.screenWidthDp*0.80


    DisposableEffect(lifecycleOwner, context) {
        val screenOffReceiver = ScreenOffReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(screenOffReceiver, intentFilter)
        val activityLifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                coroutine.launch {
                    Caffeine.decaf()
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
                Caffeine.decaf()
            }
        } else {
            countdownTime.intValue = customMaxTimeSeconds.intValue * 60
        }
    }

    Column(
        modifier =
            Modifier.Companion
                .fillMaxSize()
                .background(Background)
                .width(viewWidth.dp)
                .padding(horizontal = 5.dp)
                .padding(bottom = (configuration.screenWidthDp * 0.1).dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Text(
            text = convertMinutesToMMSS(countdownTime.intValue),
            color = TextColor,
            fontWeight = FontWeight.Companion.Bold,
            fontSize = 50.sp,
            textAlign = TextAlign.Companion.Center,
            modifier =
                Modifier.Companion
                    .width(viewWidth.dp)
                    .align(Alignment.Companion.CenterHorizontally)
        )

        Spacer(
            modifier =
                Modifier.Companion
                    .padding(vertical = 100.dp)
                    .weight(
                        0.5F,
                        fill = false
                    )
        )

        Slider(
            value = customMaxTimeSeconds.intValue.toFloat(),
            valueRange = 0f..60f,
            onValueChange = {
                if (!isScreenOn.value) {
                    customMaxTimeSeconds.intValue = it.toInt()
                }
            },
            onValueChangeFinished = {
                if (isScreenOn.value) {
                    alert(context, "cannot change that when caffeinated")
                    vibrateDevice(context)
                }
            },
            colors = SliderDefaults.colors(
                // Added colors property
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = PrimaryColor,
                inactiveTrackColor = InactiveColor,
            ),
            modifier =
                Modifier.Companion
                    .width((configuration.screenWidthDp * 0.80).dp)
        )

        Row(
            modifier =
                Modifier.Companion
                    .padding(vertical = 20.dp)
                    .padding(horizontal = 10.dp)
                    .width(viewWidth.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = "Turn off when locked",
                color = TextColor
            )

            Switch(
                checked = turnOfOnLock.value,
                onCheckedChange = {
                    if (!isScreenOn.value) {
                        Caffeine.toggleSleepOnLock()
                    } else {
                        alert(context, "cannot change that when caffeinated")
                        vibrateDevice(context)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                    checkedTrackColor = PrimaryColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = InactiveColor,
                )
            )
        }

        Button(
            onClick = {
                Caffeine.setMaxTimer(customMaxTimeSeconds.intValue * 60)
                coroutine.launch {
                    when (isScreenOn.value) {
                        true -> Caffeine.decaf()
                        false -> Caffeine.caffeinate(context, customMaxTimeSeconds.intValue)
                    }
                }
            },
            colors = ButtonColors(
                contentColor = Color.Companion.White,
                containerColor = PrimaryColor,
                disabledContentColor = PrimaryColor,
                disabledContainerColor = Color.Companion.White
            ),
            modifier =
                Modifier.Companion
                    .padding(bottom = 50.dp)
        ) {
            when (isScreenOn.value) {
                true ->
                    Text(
                        text = "Decaf",
                        color = TextColor
                    )

                false -> Text(
                    text = "Caffeinate",
                    color = TextColor
                )
            }
        }
    }
}