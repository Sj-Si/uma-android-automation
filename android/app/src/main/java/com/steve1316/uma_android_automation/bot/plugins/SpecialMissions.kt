package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.ScrollListEntry

import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonEventMissions
import com.steve1316.uma_android_automation.components.ButtonEventMissionsTabLimitedTime
import com.steve1316.uma_android_automation.components.ButtonHomeSpecialMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabDaily
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabMain
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabSpecial
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabTitles
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.IconEventExclusiveMissionsListBottomRight
import com.steve1316.uma_android_automation.components.IconEventExclusiveMissionsListTopLeft
import com.steve1316.uma_android_automation.components.IconNotificationExclamationHalfHeight
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageSpecialMissions

class SpecialMissions(
    game: Game,
    menuBar: MenuBar,
    maxRuntimeMinutes: Int = 5,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, maxRuntimeMinutes, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]SpecialMissions"

    private val missionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabMain,
        ButtonSpecialMissionsTabTitles,
        ButtonSpecialMissionsTabSpecial,
        ButtonEventMissionsTabLimitedTime,
    )

    private var bHasHandledSpecialMissions: Boolean = false
    private var bHasHandledEventExclusiveMissions: Boolean = false

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "event_exclusive_missions" -> {
                // TODO: Need to add logic for other entries in this dialog.
                // Only event mission is currently in the dialog. Need to wait
                // for legend races to become available again.
                if (!bHasHandledEventExclusiveMissions) {
                    handleEventExclusiveMisisons()
                } else {
                    result.dialog.close(game.imageUtils)
                }
            }
            // Logic handled in [handleEventExclusiveMisisons]
            "event_missions" -> result.dialog.close(game.imageUtils)
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            // Logic handled in [handleEventExclusiveMisisons]
            "special_missions" -> result.dialog.close(game.imageUtils)
            "story_unlocked" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageSpecialMissions,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    /** Handle each entry in the list.
     * 
     * @return False to continue the list processing. True to end list processing early.
     */
    private fun onListEntry(scrollList: ScrollList, entry: ScrollListEntry): Boolean {
        MessageLog.d(TAG, "[$name] Handling EventExclusiveMissions entry #${entry.index}.")

        if (!IconNotificationExclamationHalfHeight.check(game.imageUtils, sourceBitmap = entry.bitmap, tries = 10)) {
            MessageLog.d(TAG, "[$name] Skipping event exclusive missions entry #${entry.index} due to missing notification exclamation icon.")
            return false
        }

        val x: Double = (entry.bbox.x + (entry.bbox.w / 2).toInt()).toDouble()
        val y: Double = (entry.bbox.y + (entry.bbox.h / 2).toInt()).toDouble()
        game.tap(x, y)
        game.wait(1.0)
        if (waitForButton(missionsTabs, bShouldHandleDialogs = false) == null) {
            // Special case for Champions meeting. Clicking the event mission entry
            // brings us to the champions meeting home page and not a mission tabs screen.
            if (ButtonSpecialMissions.click(game.imageUtils)) {
                MessageLog.i(TAG, "[$name] Handling event missions page with Special Missions button.")
                val dialogResult = game.campaign.handleDialogs(
                    args = mapOf<String, Any>(
                        "dialogNameToDefer" to "special_missions",
                        "bShouldWait" to true,
                        "bShouldWaitForLoading" to true,
                    ),
                )
                if (dialogResult is DialogHandlerResult.Deferred) {
                    // Collect rewards. Causes connecting to server.
                    dialogResult.dialog.ok(game.imageUtils)
                    game.waitForLoading()
                    // Handle all dialogs. This confirms rewards and also closes this dialog.
                    // Can also catch connection errors.
                    handleDialogsUntilNoneRemain()
                }

                // If we fail to click the back button, then we need to stop the loop
                // immediately since we won't be back at the Event Missions dialog.
                return waitForButton(ButtonBack, bShouldClickButton = true) == null
            }

            MessageLog.w(TAG, "[$name] Timed out waiting for mission tabs to appear for event exclusive mission entry #${entry.index}.")
            // Need to bail out since we're stuck in an unknown state.
            return true
        }
        handleMissionsTabs()

        // Let the dialog handler take care of any missions screens that are
        // inside a dialog. It will close the dialog when complete.
        val dialogResult: DialogHandlerResult = handleDialogs()
        if (dialogResult is DialogHandlerResult.Handled) {
            return false
        }

        // If missions screen isn't in a dialog, we need to return by clicking
        // the back button which should exist in all non-dialog missions screens.
        waitForButton(ButtonBack, bShouldClickButton = true)
        // Now wait for game to load.
        game.wait(1.0)
        return false
    }

    private fun handleEventExclusiveMisisons() {
        val scrollList: ScrollList? = ScrollList.create(
            game,
            listTopLeftComponent = IconEventExclusiveMissionsListTopLeft,
            listBottomRightComponent = IconEventExclusiveMissionsListBottomRight,
        )
        if (scrollList == null) {
            MessageLog.e(TAG, "[$name] Failed to detect EventExclusiveMissions list.")
            return
        }

        scrollList.process(onEntry = ::onListEntry)

        bHasHandledEventExclusiveMissions = true
    }

    private fun handleTab(tab: ComponentInterface) {
        game.wait(0.25, skipWaitingForLoading = true)
        // Not an error since we check against all possible tabs to make this
        // function as general as possible.
        if (!tab.click(game.imageUtils)) {
            return
        }
        MessageLog.d(TAG, "[$name] Handling tab: ${tab::class.simpleName}")
        game.wait(0.25, skipWaitingForLoading = true)
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
            MessageLog.d(TAG, "[$name] Tab has no rewards to collect.")
            return
        }
        ButtonCollectAll.click(game.imageUtils)
        MessageLog.d(TAG, "[$name] Collected rewards for tab.")
        game.wait(0.5)
        handleDialogs()
    }

    private fun handleMissionsTabs() {
        MessageLog.d(TAG, "[$name] Handling Missions tabs...")

        // Click CollectAll on the current tab before proceeding.
        // Otherwise we'll fail to detect the active tab and won't ever collect it.
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == false) {
            ButtonCollectAll.click(game.imageUtils)
            MessageLog.d(TAG, "[$name] Collected rewards for initial tab.")
            game.wait(0.5)
            handleDialogs()
        }

        missionsTabs.forEach { handleTab(it) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageSpecialMissions -> {
                if (!bHasHandledSpecialMissions) {
                    MessageLog.d(TAG, "[$name] PageSpecialMissions: Handling special missions...")
                    handleMissionsTabs()
                    bHasHandledSpecialMissions = true
                } else {
                    MessageLog.d(TAG, "[$name] PageSpecialMissions: Handling event missions...")
                    if (!ButtonEventMissions.click(game.imageUtils)) {
                        MessageLog.d(TAG, "[$name] PageSpecialMissions: No available event missions.")
                        bHasHandledEventExclusiveMissions = true
                    }
                }
            }
            else -> {}
        }

        bIsComplete = bHasHandledSpecialMissions && bHasHandledEventExclusiveMissions

        if (bIsComplete) {
            MessageLog.d(TAG, "[$name] SpecialMissions is complete. Returning...")
            ButtonBack.click(game.imageUtils)
        }

        return null
    }

     override fun goToStart(): Boolean {
        super.goToStart()

        if (PageSpecialMissions.check(game.imageUtils)) {
            return true
        }

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (waitForButton(ButtonHomeSpecialMissions, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "[$name] Failed to find Special Missions button.")
            return false
        }

        return waitForPage(PageSpecialMissions) != null
    }
}
