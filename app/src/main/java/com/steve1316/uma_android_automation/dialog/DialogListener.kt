package com.steve1316.uma_android_automation.bot.dialog

import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.dialog.DialogInterface
import com.steve1316.uma_android_automation.dialog.DialogObjects

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*

import org.opencv.core.Point

class DialogEventProducer(private val coroutineScope: CoroutineScope, private val eventBus: EventBus) {
    private val TAG: String = "DialogEventProducer"
    private var job: Job? = null

    fun check(): Boolean {
        return ImageUtils.findImage("dialog_title_gradient", tries=tries, region=Screen.TOP_HALF)
    }

    fun checkDialogTitle(): String? {
        for (obj in DialogObjects.items) {
            if (obj.check()) {
                return obj.name
            }
        }

        return null
    }

    // Run this dialog item as an event producer.
    fun start() {
        job?.cancel()
        MessageLog.d(TAG, "Listening for dialog.")
        job = coroutineScope.launch(Dispatchers.Default) {
            var counter = 0
            while (isActive) {
                counter++
                MessageLog.d(TAG, "Listening for dialog. Loop: ${counter}")
                if (check()) {
                    val event = AppEvent.DialogEvent(checkDialogTitle())
                    eventBus.post(event)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}