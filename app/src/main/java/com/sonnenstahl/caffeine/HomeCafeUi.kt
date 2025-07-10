package com.sonnenstahl.caffeine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import com.sonnenstahl.caffeine.ui.theme.Background
import com.sonnenstahl.caffeine.ui.theme.PrimaryColor
import com.sonnenstahl.caffeine.ui.theme.TextColor
import kotlinx.coroutines.launch

@Composable
fun HomeCafeUi() {
    val couroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val caffeinated = Caffeine.isScreenOn.collectAsState()
    val buttonText = remember(caffeinated.value) {
        when(caffeinated.value) {
            true -> "turn off"
            false -> "turn on"
        }

    }

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(Background)
                .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            text= buttonText,
            onClick = {
                couroutine.launch {
                    when (caffeinated.value) {
                        true -> Caffeine.decaf()
                        false -> Caffeine.caffeinate(context, 60)
                    }
                }
            },
           colors = ButtonDefaults.buttonColors(
               backgroundColor = ColorProvider(PrimaryColor, PrimaryColor),
               contentColor = ColorProvider(TextColor, TextColor)
           )
        )


    }

}