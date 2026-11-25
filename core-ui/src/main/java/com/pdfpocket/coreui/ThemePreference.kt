package com.pdfpocket.coreui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 0 = follow system, 1 = light, 2 = dark. */
object ThemePreference {
    private const val PREFS = "pdfpocket_prefs"
    private const val KEY = "theme_mode"
    private val _state = MutableStateFlow(0)
    val state: StateFlow<Int> = _state

    fun load(context: Context) {
        _state.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0)
    }

    fun set(context: Context, value: Int) {
        _state.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, value).apply()
    }
}
