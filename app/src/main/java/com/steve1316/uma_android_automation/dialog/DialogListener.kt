package com.steve1316.uma_android_automation.dialog

import android.content.Context
import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.dialog.DialogInterface
import com.steve1316.uma_android_automation.dialog.DialogObjects

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*

import org.opencv.core.Point

object DialogListener {
    private val TAG: String = "DialogListener"

    fun check(imageUtils: ImageUtils, tries: Int = 1): Boolean {
        return imageUtils.findImage("dialog/dialog_title_gradient", tries=tries, region=Screen.TOP_HALF).first != null
    }

    private fun checkDialogTitle(imageUtils: ImageUtils, tries: Int = 1): DialogInterface? {
        for (obj in DialogObjects.items) {
            if (obj.check(imageUtils=imageUtils, tries=tries)) {
                return obj
            }
        }

        return null
    }

    fun getDialog(imageUtils: ImageUtils): DialogInterface? {
        if (imageUtils.findImage("dialog/dialog_title_gradient", tries=1, region=Screen.TOP_HALF).first != null) {
            MessageLog.d(TAG, "FOUND DIALOG GRADIENT")
            for (obj in DialogObjects.items) {
                if (obj.check(imageUtils=imageUtils)) {
                    //EventBus.post(AppEvent.DialogEvent(obj))
                    return obj
                }
            }
            MessageLog.d(TAG, "FAILED TO MATCH DIALOGS")
        }
        return null
    }

    // Run this dialog item as an event producer.
    fun start(scope: CoroutineScope) {
        // This function doesnt currently work.
        // It causes some memory leak which eventually leads to the program crashing.
        MessageLog.e(TAG, "DialogListener::start() not working yet!")
        return
        /*
        MessageLog.d(TAG, "Starting...")
        scope.launch {
            var counter = 0
            while (isActive) {
                counter++
                if (check()) {
                    val dialog: DialogInterface? = checkDialogTitle()
                    MessageLog.i(TAG, "FOUND DIALOG: ${dialog?.name}")
                    if (dialog != null) {
                        val event = AppEvent.DialogEvent(dialog)
                        EventBus.post(event)
                    }
                }
            }
        }
        */
    }
}