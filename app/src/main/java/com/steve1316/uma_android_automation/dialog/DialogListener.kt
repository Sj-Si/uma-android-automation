package com.steve1316.uma_android_automation.dialog

import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.dialog.DialogInterface
import com.steve1316.uma_android_automation.dialog.DialogObjects

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*

import org.opencv.core.Point

class DialogEventProducer(private val coroutineScope: CoroutineScope) {
    private val TAG: String = "DialogEventProducer"
    private var job: Job? = null

    fun check(tries: Int = 1): Boolean {
        return ImageUtils.findImage("dialog/dialog_title_gradient", tries=tries, region=Screen.TOP_HALF).first != null
    }

    fun checkDialogTitle(): DialogInterface? {
        for (obj in DialogObjects.items) {
            if (obj.check()) {
                return obj
            }
        }

        return null
    }

    // Run this dialog item as an event producer.
    fun start() {
        job?.cancel()
        MessageLog.d(TAG, "Starting...")
        job = coroutineScope.launch(Dispatchers.Default) {
            var counter = 0
            while (isActive) {
                counter++
                if (check()) {
                    val dialog: DialogInterface? = checkDialogTitle()
                    if (dialog != null) {
                        val event = AppEvent.DialogEvent(dialog)
                        EventBus.post(event)
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        MessageLog.d(TAG, "Stopped.")
    }
}