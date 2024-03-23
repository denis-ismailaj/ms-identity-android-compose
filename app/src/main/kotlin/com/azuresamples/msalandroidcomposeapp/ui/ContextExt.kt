package com.azuresamples.msalandroidcomposeapp.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw RuntimeException("Cannot find activity")
}
