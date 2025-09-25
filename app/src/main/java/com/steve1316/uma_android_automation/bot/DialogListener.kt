package com.steve1316.uma_android_automation.bot

import android.util.Log
import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*

import org.opencv.core.Point

enum class DialogType {
	CONFIRM_RESTORE_RP,
	DAILY_SALE,
	EXTERNAL_LINK,
	ITEMS_SELECTED,
	NOTICES,
	OPEN_SOON,
	PRESENTS,
	PURCHASE_DAILY_RACE_TICKET,
	RACE_DETAILS,
	REWARDS_COLLECTED,
	SPECIAL_MISSIONS,
	VIEW_STORY,
}

enum class DialogButtonType {
	// Cancel/Close/etc.
	CLOSE,
	CANCEL,
	NO,
	// Confirm/Ok/etc.
	OK,
	ITEMS_SELECTED_RACE, // same as race but text is shifted up a bit
	RACE,
	COLLECT_ALL,
	SHOP,
	RESTORE,
}

val DIALOG_TO_BUTTON_MAP: Map<DialogType, Pair<DialogButtonType?, DialogButtonType?>> = mapOf(
	DialogType.CONFIRM_RESTORE_RP 			to Pair(DialogButtonType.NO, DialogButtonType.RESTORE),
	DialogType.DAILY_SALE 					to Pair(DialogButtonType.CANCEL, DialogButtonType.SHOP),
	DialogType.EXTERNAL_LINK 				to Pair(DialogButtonType.CANCEL, DialogButtonType.OK),
	DialogType.ITEMS_SELECTED 				to Pair(DialogButtonType.CANCEL, DialogButtonType.ITEMS_SELECTED_RACE),
	DialogType.NOTICES 						to Pair(DialogButtonType.CLOSE, null),
	DialogType.OPEN_SOON 					to Pair(DialogButtonType.CLOSE, null),
	DialogType.PRESENTS 					to Pair(DialogButtonType.CLOSE, DialogButtonType.COLLECT_ALL),
	DialogType.PURCHASE_DAILY_RACE_TICKET 	to Pair(DialogButtonType.CANCEL, DialogButtonType.OK),
	DialogType.RACE_DETAILS 				to Pair(DialogButtonType.CANCEL, DialogButtonType.RACE),
	DialogType.REWARDS_COLLECTED 			to Pair(DialogButtonType.CLOSE, null),
	DialogType.SPECIAL_MISSIONS 			to Pair(DialogButtonType.CLOSE, DialogButtonType.COLLECT_ALL),
	DialogType.VIEW_STORY 					to Pair(DialogButtonType.CANCEL, DialogButtonType.OK),
)

fun MessageLog.log(message: String, tag: String = "untagged", isError: Boolean = false, isOption: Boolean = false) {
	if (!isError) {
		Log.d(tag, message)
	} else {
		Log.e(tag, message)
	}

	// Remove the newline prefix if needed and place it where it should be.
	if (message.startsWith("\n")) {
		val newMessage = message.removePrefix("\n")
		if (isOption) {
			MessageLog.addMessage("\n" + newMessage)
		} else {
			MessageLog.addMessage("\n" + newMessage)
		}
	} else {
		if (isOption) {
			MessageLog.addMessage(message)
		} else {
			MessageLog.addMessage(message)
		}
	}
}


data class DialogButton(val ctx: Game, val type: DialogButtonType, var point: Point?) {
	private val templateName = "dialog_button_${type.name.lowercase()}"

	fun find(): Boolean {
		point = ctx.imageUtils.findImage(templateName, 1, ctx.imageUtils.regionBottomHalf).first
		if (point == null) {
			ctx.MessageLog.log("\n[WARN] Failed to find \"${type}\" button for dialog.")
		}
		return point != null
	}
}

data class Dialog(val ctx: Game, val type: DialogType) {
	private val templateName = "dialog_title_${type.name.lowercase()}"
	private var buttonNo: DialogButton? = null
	private var buttonYes: DialogButton? = null

	init {
		val typeNo: DialogButtonType? = DIALOG_TO_BUTTON_MAP[type]?.first
		if (typeNo != null) {
			buttonNo = DialogButton(ctx, typeNo, null)
		}
		val typeYes: DialogButtonType? = DIALOG_TO_BUTTON_MAP[type]?.second
		if (typeYes != null) {
			buttonYes = DialogButton(ctx, typeYes, null)
		}
	}

	fun findButtons() {
		buttonNo?.find()
		buttonYes?.find()
	}

	fun find (ctx: Game): Boolean {
		if (ctx.imageUtils.findImage(templateName, 1, ctx.imageUtils.regionTopHalf).first != null) {
			findButtons()
			return true
		}
		return false
	}
}

class DialogEventProducer(private val game: Game, private val coroutineScope: CoroutineScope, private val eventBus: EventBus) {
	private val tag: String = "[${MainActivity.Companion.loggerTag}]Dialog"

	private var dialog: Dialog? = null

	private var job: Job? = null

	fun checkDialogTitleGradient(): Boolean {
		return game.imageUtils.findImage("dialog_title_gradient", tries = 1).first != null
	}

	fun checkDialogType(): DialogType? {
		if (!checkDialogTitleGradient()) {
			return null
		}

		// TODO: Maybe find text in the title bar region and use switch based on that.
		// Would be quicker than trying to find an image for each case.
		for (dialogType in DialogType.entries) {
			val dialogTitle = "dialog_title_${dialogType.name.lowercase()}"

			if (game.imageUtils.findImage(dialogTitle, tries = 1, region = game.imageUtils.regionTopHalf).first != null) {
				return dialogType
			}
		}
		return null
	}

	fun checkDialog(): Dialog? {
		val dialogType = checkDialogType()
		if (dialogType == null) {
			return null
		}

		return Dialog(game, dialogType)
	}

	fun start() {
		job?.cancel()
		MessageLog.log("\n[DEBUG] DialogListener: STARTING", tag)
		job = coroutineScope.launch(Dispatchers.Default) {
			var counter = 0
			while (isActive) {
				counter++
				MessageLog.log("\n[DEBUG] DialogListener: Loop $counter", tag)

				val prevType: DialogType? = dialog?.type
				val currDialog: Dialog? = checkDialog()
				if (prevType != null && currDialog != null && currDialog.type == prevType) {
					//delay(100)
					//continue
				}

				if (currDialog != null) {
					MessageLog.log("\n[DEBUG] DialogListener: Found Dialog ${currDialog.type}", tag)
					val event = AppEvent.DialogPopupEvent(currDialog.type)
					eventBus.post(event)
				}
				//delay(100)
			}
		}
	}

	fun stop() {
		job?.cancel()
	}
}

class DialogEventConsumer(private val game: Game, private val coroutineScope: CoroutineScope, private val eventBus: EventBus) {
	fun start() {
		coroutineScope.launch {
			eventBus.events.collect { event ->
				MessageLog.log("\n[EVENT] Got Event: $event", "DialogEventConsumer")
			}
		}
	}
}