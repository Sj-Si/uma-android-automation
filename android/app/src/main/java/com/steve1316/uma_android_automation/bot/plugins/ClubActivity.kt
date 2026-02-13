package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageClubHome
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonClub
import com.steve1316.uma_android_automation.components.ButtonClubItemRequest
import com.steve1316.uma_android_automation.components.ButtonClubViewRequests
import com.steve1316.uma_android_automation.components.ButtonConfirm
import com.steve1316.uma_android_automation.components.ButtonHome
import com.steve1316.uma_android_automation.components.ButtonShoesSprint
import com.steve1316.uma_android_automation.components.ButtonShoesMile
import com.steve1316.uma_android_automation.components.ButtonShoesMedium
import com.steve1316.uma_android_automation.components.ButtonShoesLong
import com.steve1316.uma_android_automation.components.ButtonShoesDirt
import com.steve1316.uma_android_automation.components.ButtonDonateToAll0
import com.steve1316.uma_android_automation.components.LabelItemRequestExpired
import com.steve1316.uma_android_automation.components.LabelItemRequestCooldown
import com.steve1316.uma_android_automation.components.LabelItemRequestMaxDonations
import com.steve1316.uma_android_automation.components.LabelItemRequestSelectItem
import com.steve1316.uma_android_automation.components.LabelItemRequestConfirm
import com.steve1316.uma_android_automation.components.LabelCurrentItemRequestStatus
import com.steve1316.uma_android_automation.components.MenuBar

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
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]ClubActivity"

    private val clubRequestShoeTypeString: String = SettingsHelper.getStringSetting("dailyTasks", "clubRequestShoeType")
    private val clubRequestShoeType: ShoeType = ShoeType.fromName(clubRequestShoeTypeString)!!
    private val bShouldDonateItems: Boolean = SettingsHelper.getBooleanSetting("dailyTasks", "enableClubDonation")

    private var bHasRequestedItems: Boolean = false
    // If we arent supposed to donate, then we just say that it is already completed.
    private var bHasDonatedItems: Boolean = !bShouldDonateItems

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
            // No dialog.ok() for this dialog.
            // See note in [Dialog.kt::DialogItemRequest].
            "item_request" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()

                val identifiers: List<ComponentInterface> = listOf(
                    LabelCurrentItemRequestStatus,
                    LabelItemRequestExpired,
                    LabelItemRequestCooldown,
                    LabelItemRequestMaxDonations,
                    LabelItemRequestSelectItem,
                    LabelItemRequestConfirm,
                    ButtonDonateToAll0,
                )
                val identifier: ComponentInterface? = identifiers.firstOrNull {
                    it.check(game.imageUtils, sourceBitmap = bitmap)
                }
                when (identifier) {
                    is LabelCurrentItemRequestStatus -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Item request still active.")
                        bHasRequestedItems = true
                        result.dialog.close(game.imageUtils)
                    }
                    is LabelItemRequestExpired -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Item request expired.")
                        result.dialog.close(game.imageUtils)
                    }
                    is LabelItemRequestCooldown -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: It hasn't been 8 hours since last item request.")
                        // Can't request items yet so we just treat this
                        // as if we successfully requested items.
                        bHasRequestedItems = true
                        result.dialog.close(game.imageUtils)
                    }
                    is LabelItemRequestMaxDonations -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Max donations received.")
                        // This dialog appears one time after receiving all
                        // requested items.
                        // This doesn't necessarily mean that we can't request
                        // any more items at this time.
                        // Thus we just close the dialog then proceed.
                        result.dialog.close(game.imageUtils)
                    }
                    is LabelItemRequestSelectItem -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Selecting shoe to request.")
                        val shoeButton: ComponentInterface = shoeButtons[clubRequestShoeType]!!
                        shoeButton.click(game.imageUtils, sourceBitmap = bitmap)
                        ButtonConfirm.click(game.imageUtils, sourceBitmap = bitmap)
                    }
                    is LabelItemRequestConfirm -> {
                        MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Confirming item request.")
                        ButtonConfirm.click(game.imageUtils, sourceBitmap = bitmap)
                        bHasRequestedItems = true
                    }
                    is ButtonDonateToAll0 -> {
                        // If we can't donate anything, then just treat this as if we
                        // finished donating and back out.
                        if (ButtonDonateToAll0.checkDisabled(game.imageUtils, bitmap)) {
                            MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: DonateToAll button is disabled.")
                            bHasDonatedItems = true
                            result.dialog.close(game.imageUtils)
                        } else {
                            MessageLog.d(TAG, "[DIALOG] ${result.dialog.name}: Clicking DonateToAll button.")
                            ButtonDonateToAll0.click(game.imageUtils, sourceBitmap = bitmap)
                        }
                    }
                    else -> MessageLog.w(TAG, "[DIALOG] ${result.dialog.name}: Could not determine which variant of this dialog is on screen.")
                }
            }
            "item_request_error" -> {
                if (ButtonHome.check(game.imageUtils)) {
                    result.dialog.close(game.imageUtils)
                    // Handling this dialog sends us back to the home screen.
                    if (waitForPage(PageHome) == null) {
                        throw IllegalStateException("Failed to detect Home screen after handling item_request_error dialog.")
                    }
                    // We want to return to the club menu since we aren't
                    // done with our tasks yet.
                    if (!goToStart()) {
                        throw IllegalStateException("Failed to go to club home page after handling item_request_error dialog.")
                    }
                    return DialogHandlerResult.Handled(result.dialog)
                }
                result.dialog.close(game.imageUtils)
            }
            "practice_partner_list" -> result.dialog.close(game.imageUtils)
            "trainer_info" -> result.dialog.close(game.imageUtils)
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
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageClubHome -> {
                if (!bHasRequestedItems) {
                    waitForButton(ButtonClubItemRequest, bShouldClickButton = true)
                } else if (!bHasDonatedItems) {
                    waitForButton(ButtonClubViewRequests, bShouldClickButton = true)
                    // After timing out, we can assume that this button just doesn't
                    // exist meaning there are no available requests.
                    bHasDonatedItems = true
                } else if (bHasRequestedItems && bHasDonatedItems) {
                    bIsComplete = true
                }
            }
            else -> {}
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageClubHome.check(game.imageUtils)) {
            return true
        }

        if (waitForButton(ButtonClub, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "Failed to find Club button. Cannot proceed.")
            return false
        }

        if (ButtonClub.checkDisabled(game.imageUtils)) {
            MessageLog.i(TAG, "Club is locked. Cannot proceed.")
            return false
        }

        if (!ButtonClub.click(game.imageUtils)) {
            MessageLog.w(TAG, "Failed to click Club button.")
            return false
        }

        return waitForPage(PageClubHome) != null
    }
}
