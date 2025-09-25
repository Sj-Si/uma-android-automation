package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.bot.DialogType

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class AppEvent {
    data class DialogPopupEvent(val dialogType: DialogType) : AppEvent()
}

class EventBus {
    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow() // Expose as read-only SharedFlow

    suspend fun post(event: AppEvent) {
        _events.emit(event)
    }

    // Example of subscribing within a CoroutineScope
    fun subscribe(scope: CoroutineScope, onEvent: (AppEvent) -> Unit) {
        scope.launch {
            events.collect { event ->
                onEvent(event)
            }
        }
    }
}