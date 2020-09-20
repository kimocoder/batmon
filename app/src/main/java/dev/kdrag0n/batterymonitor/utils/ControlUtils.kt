package dev.kdrag0n.batterymonitor.utils

import android.os.Build
import android.service.controls.Control
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
fun Control.StatefulBuilder.setColoredTitle(text: String, color: Int): Control.StatefulBuilder {
    val styledText = SpannableString(text)
    styledText.setSpan(ForegroundColorSpan(color), 0, text.length, 0)
    return setTitle(styledText)
}

@RequiresApi(Build.VERSION_CODES.R)
fun Control.StatefulBuilder.setColoredStatusText(text: String, color: Int): Control.StatefulBuilder {
    val styledText = SpannableString(text)
    styledText.setSpan(ForegroundColorSpan(color), 0, text.length, 0)
    return setStatusText(styledText)
}