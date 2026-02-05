package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageClubHome
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonClub
import com.steve1316.uma_android_automation.components.ButtonClubLocked
import com.steve1316.uma_android_automation.components.ButtonClubItemRequest
import com.steve1316.uma_android_automation.components.ButtonClubViewRequests
import com.steve1316.uma_android_automation.components.ButtonConfirm
import com.steve1316.uma_android_automation.components.ButtonHome
import com.steve1316.uma_android_automation.components.ButtonShoesSprint
import com.steve1316.uma_android_automation.components.ButtonShoesMile
import com.steve1316.uma_android_automation.components.ButtonShoesMedium
import com.steve1316.uma_android_automation.components.ButtonShoesLong
import com.steve1316.uma_android_automation.components.ButtonShoesDirt

enum class ShoeType {
    SPRINT,
    MILE,
    MEDIUM,
    LONG,
    DIRT;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): ShoeType? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): ShoeType? = ordinalMap[ordinal]
    }
}

class ClubActivity(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]ClubActivity"

    // TODO: Load from settings.
    private val clubDonationShoeTypeString: String = "medium"
    private val clubDonationShoeType: ShoeType = ShoeType.fromName(clubDonationShoeTypeString) ?: ShoeType.MEDIUM

    private var bHasSelectedShoes: Boolean = false
    private var bHasRequestedItems: Boolean = false
    private var bHasDonatedItems: Boolean = false

    private val shoeButtons: Map<ShoeType, ComponentInterface> = mapOf(
        ShoeType.SPRINT to ButtonShoesSprint,
        ShoeType.MILE to ButtonShoesMile,
        ShoeType.MEDIUM to ButtonShoesMedium,
        ShoeType.LONG to ButtonShoesLong,
        ShoeType.DIRT to ButtonShoesDirt,
    )

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "confirm_donations" -> result.dialog.ok(game.imageUtils)
            "donation_complete" -> {
                result.dialog.close(game.imageUtils)
                bHasDonatedItems = true
            }
            "item_request" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()

                if (shoeButtons.values.all { it.check(game.imageUtils, sourceBitmap = bitmap) }) {
                    val button: ComponentInterface = shoeButtons[clubDonationShoeType]!!
                    button.click(game.imageUtils, sourceBitmap = bitmap)
                    bHasSelectedShoes = true
                }

                if (bHasSelectedShoes && ButtonConfirm.check(game.imageUtils)) {
                    bHasRequestedItems = true
                }
                result.dialog.ok(game.imageUtils)
            }
            "item_request_error" -> {
                if (ButtonHome.check(game.imageUtils)) {
                    result.dialog.close(game.imageUtils)
                    game.waitForLoading()
                    // We want to return to the club menu if we get this error.
                    waitForButton(ButtonClub)
                    return DialogHandlerResult.Handled(result.dialog)
                }
                result.dialog.close(game.imageUtils)
            }
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return if (PageClubHome.check(game.imageUtils, bitmap)) PageClubHome else null
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageClubHome -> {
                if (!bHasRequestedItems) {
                    ButtonClubItemRequest.click(game.imageUtils)
                } else if (!bHasDonatedItems) {
                    ButtonClubViewRequests.click(game.imageUtils)
                } else if (bHasRequestedItems && bHasDonatedItems) {
                    bIsComplete = true
                }
            }
            else -> handleDialogs()
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        var dialogResult: DialogHandlerResult = handleDialogs()
        while (dialogResult is DialogHandlerResult.Handled) {
            dialogResult = handleDialogs()
        }

        if (dialogResult is DialogHandlerResult.Unhandled) {
            MessageLog.e(TAG, "Unhandled dialog prevented plugin execution: ${dialogResult.dialog.name}")
            return false
        }

        if (PageClubHome.check(game.imageUtils)) {
            return true
        }

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (ButtonClubLocked.check(game.imageUtils)) {
            MessageLog.i(TAG, "Club is locked. Cannot proceed.")
            return false
        }

        if (!waitForButton(ButtonClub)) {
            MessageLog.w(TAG, "Failed to find Club button.")
            return false
        }

        game.wait(0.5)
        game.waitForLoading()

        return waitForPage(PageClubHome)
    }
}
