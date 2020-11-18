package com.example.chatbot

import android.content.Context
import android.os.Build
import java.util.*
import java.util.Calendar.*

private fun getCurrentLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        context.resources.configuration.locale
    }
}


val Date.calendar: Calendar
    get() {
        val cal = getInstance()
        cal.time = this
        return cal
    }

fun Date.formatAsTime(): String {
    val hour = calendar.get(HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(MINUTE).toString().padStart(2, '0')
    return "$hour:$minute"
}



