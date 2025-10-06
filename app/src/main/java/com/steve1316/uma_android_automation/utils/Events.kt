package com.steve1316.uma_android_automation.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.steve1316.uma_android_automation.dialog.DialogInterface

sealed class AppEvent {
    data class DialogEvent(val dialog: DialogInterface?) : AppEvent()
}

object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow() // Expose as read-only SharedFlow

    suspend fun post(event: Any) {
        _events.emit(event)
    }

    inline fun <reified T> subscribe(
        scope: CoroutineScope,
        crossinline onEvent: (T) -> Unit,
    ) {
        scope.launch {
            events.filterIsInstance<T>().collectLatest { event ->
                onEvent(event)
            }
        }
    }

    // Example of subscribing within a CoroutineScope
    fun subscribe2(scope: CoroutineScope, onEvent: (Any) -> Unit) {
        scope.launch {
            events.collect { event ->
                onEvent(event)
            }
        }
    }
}