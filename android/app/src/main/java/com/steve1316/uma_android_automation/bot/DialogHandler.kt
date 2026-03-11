package com.steve1316.uma_android_automation.bot

import android.graphics.Bitmap
import android.util.Log
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.DiscordUtils
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.*
import com.steve1316.uma_android_automation.types.BoundingBox
import com.steve1316.uma_android_automation.types.DateYear
import com.steve1316.uma_android_automation.types.RunningStyle
import org.opencv.core.Point

sealed class DialogHandlerResult {
    data class Handled(val dialog: DialogInterface) : DialogHandlerResult()
    data class Unhandled(val dialog: DialogInterface) : DialogHandlerResult()
    data class Deferred(val dialog: DialogInterface) : DialogHandlerResult()
    data object NoDialogDetected : DialogHandlerResult()
}

/**
 * Base class for handling various dialogs in the Umamusume game.
 *
 * This class centralizes all dialog handling logic from different bot modules
 * to provide a single, maintainable source of truth for dialog interactions.
 *
 * @param game Reference to the bot's [Game] instance for state access and utilities.
 *
 * Example usage:
 *
 * ```kotlin
 * // Basic usage
 * val (handled, dialog) = game.task.handleDialogs()
 *
 * // Usage with arguments
 * val args = mapOf("overrideIgnoreConsecutiveRaceWarning" to true)
 * game.task.handleDialogs(args = args)
 * ```
 */
open class DialogHandler(val game: Game) {
	private val TAG: String = "[${MainActivity.loggerTag}]${this::class.simpleName}"

	/**
	 * Detects and handles any dialog popups.
	 *
	 * This is the centralized implementation that handles generic, campaign,
	 * racing, and skill-related dialogs.
	 *
	 * To prevent the bot moving too fast, we add a 500ms delay to the
	 * exit of this function whenever we close the dialog.
	 * This gives the dialog time to close since there is a very short
	 * animation that plays when a dialog closes.
     *
     * List of valid arguments passed to the [args] parameter:
     *  - bShouldWait
     *      Whether we should add a delay before handling the dialog.
     *      This is useful for when we click a button that we know opens a dialog.
     *      So we just call [handleDialogs] immediately with this argument.
     *  - bShouldWaitForLoading
     *      Whether we need to wait for the game to load or connect to the server
     *      prior to checking for dialogs.
     *      This is useful for when we click a button that we know opens a dialog.
     *      So we just call [handleDialogs] immediately with this argument.
     *  - dialogNameToDefer
     *      A single dialog name that, if detected, should not be handled here
     *      and instead just sent back to the calling function for them to handle.
     *  - dialogNamesToDefer
     *      A list of dialog names that, if detected, should not be handled here
     *      and instead just sent back to the calling function for them to handle.
     *  - bShouldDefer
     *      Whether we should not handle ANY dialogs that are detected and instead
     *      just send them back to the calling function for them to handle.
	 *
	 * @param dialog An optional dialog to evaluate. This allows chaining
	 * dialog handler calls for improved performance.
	 * @param args Optional arguments mapping for dialog handling.
	 *
	 * @return The [DialogHandlerResult] for this operation.
     *  - [DialogHandlerResult.Handled] if the dialog was successfully handled.
     *  - [DialogHandlerResult.Unhandled] if the dialog wasn't handled.
     *  - [DialogHandlerResult.Deferred] if the detected dialog isn't handled and
     *      instead the dialog is just sent back to the calling function.
     *  - [DialogHandlerResult.NoDialogDetected] if no dialogs were detected.
	 */
	open fun handleDialogs(dialog: DialogInterface? = null, args: Map<String, Any> = mapOf()): DialogHandlerResult {
        val bShouldWait = args["bShouldWait"] as? Boolean ?: false
        val bShouldWaitForLoading = args["bShouldWaitForLoading"] as? Boolean ?: false
        if (bShouldWait || bShouldWaitForLoading) {
            MessageLog.d(TAG, "[DIALOG] Waiting before handling dialog due to passed args: dialogWaitDelay=${game.dialogWaitDelay}, bShouldWait=$bShouldWait, bShouldWaitForLoading=$bShouldWaitForLoading")
            game.wait(game.dialogWaitDelay, skipWaitingForLoading = !bShouldWaitForLoading)
        }

		val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
		if (dialog == null) {
			Log.d(TAG, "[DIALOG] No dialog found.")
			return DialogHandlerResult.NoDialogDetected
		}

		MessageLog.d(TAG, "[DIALOG] Handle dialog: ${dialog.name}")

        val dialogNameToDefer: String? = args["dialogNameToDefer"] as? String ?: null
        var dialogNamesToDefer: List<String> = args["dialogNamesToDefer"] as? List<String> ?: listOf<String>()
        var bShouldDefer = args["bShouldDefer"] as? Boolean ?: false
        if (dialogNamesToDefer.contains(dialog.name) || dialogNameToDefer == dialog.name) {
            bShouldDefer = true
        }

        if (bShouldDefer) {
            MessageLog.d(TAG, "[DIALOG] Dialog handling deferred to calling function.")
            return DialogHandlerResult.Deferred(dialog)
        }

		when (dialog.name) {
			// Generic Dialogs (from Game.kt)
			"connection_error" -> {
				val currTime: Long = System.currentTimeMillis()
				// If the cooldown period has lapsed, reset our count.
				if (currTime - game.lastConnectionErrorRetryTimeMs > game.connectionErrorRetryCooldownTimeMs) {
					game.connectionErrorRetryAttempts = 0
					game.lastConnectionErrorRetryTimeMs = currTime
				}

				if (game.connectionErrorRetryAttempts >= game.maxConnectionErrorRetryAttempts) {
                    if (DiscordUtils.enableDiscordNotifications) {
                        DiscordUtils.queue.add("```diff\n- ${MessageLog.getSystemTimeString()} Max connection error retry attempts reached. Stopping bot...\n```")
                    }
					throw InterruptedException("Max connection error retry attempts reached. Stopping bot...")
				}

				game.connectionErrorRetryAttempts++
				dialog.ok(game.imageUtils)
				game.wait(0.5)
			}
			"display_settings" -> dialog.close(game.imageUtils)
			"help_and_glossary" -> dialog.close(game.imageUtils)
			"session_error" -> {
				throw InterruptedException("Session error. Stopping bot...")
			}
			// Racing Dialogs (from Racing.kt)
            "overwrite" -> dialog.ok(game.imageUtils)
			"race_playback" -> {
				// Select portrait mode to prevent game from switching to landscape.
				RadioPortrait.click(game.imageUtils)
				// Click the checkbox to prevent this popup in the future.
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"runners" -> dialog.close(game.imageUtils)
            "schedule_cancellation" -> dialog.close(game.imageUtils)
            "schedule_race" -> dialog.close(game.imageUtils)
			
			"trophy_won" -> dialog.close(game.imageUtils)
			"unlock_requirements" -> dialog.close(game.imageUtils)
			"agenda_details" -> dialog.close(game.imageUtils)
			"bonus_umamusume_details" -> dialog.close(game.imageUtils)
			"career" -> dialog.close(game.imageUtils)
			"career_event_details" -> dialog.close(game.imageUtils)
			"career_profile" -> dialog.close(game.imageUtils)
			"choices" -> dialog.close(game.imageUtils)
			"concert_skip_confirmation" -> {
				// Click the checkbox to prevent this popup in the future.
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"epithets" -> dialog.close(game.imageUtils)
			"fans" -> dialog.close(game.imageUtils)
			"featured_cards" -> dialog.close(game.imageUtils)
			"give_up" -> dialog.close(game.imageUtils)
			"goals" -> dialog.close(game.imageUtils)
			"infirmary" -> {
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"log" -> dialog.close(game.imageUtils)
			"menu" -> dialog.close(game.imageUtils)
			"mood_effect" -> dialog.close(game.imageUtils)
			"my_agendas" -> dialog.close(game.imageUtils)
			"no_retries" -> dialog.ok(game.imageUtils)
			"options" -> dialog.close(game.imageUtils)
			"perks" -> dialog.close(game.imageUtils)
			"placing" -> dialog.close(game.imageUtils)
			"purchase_alarm_clock" -> {
				throw InterruptedException("Ran out of alarm clocks. Stopping bot...")
			}
			"quick_mode_settings" -> {
				val bbox = BoundingBox(
					x = game.imageUtils.relX(0.0, 160),
					y = game.imageUtils.relY(0.0, 770),
					w = game.imageUtils.relWidth(70),
					h = game.imageUtils.relHeight(460),
				)
				val optionLocations: ArrayList<Point> = IconHorseshoe.findAll(
					game.imageUtils,
					region = bbox.toIntArray(),
					confidence = 0.0,
				)
				if (optionLocations.size == 4) {
					MessageLog.d(TAG, "[DIALOG] quick_mode_settings: Using findAll method.")
					val loc: Point = optionLocations[1]
					game.tap(loc.x, loc.y, IconHorseshoe.template.path)
				} else {
					MessageLog.d(TAG, "[DIALOG] quick_mode_settings: Using image OCR method.")
					// Fallback to image detection.
					RadioCareerQuickShortenAllEvents.click(game.imageUtils)
				}
				
				dialog.ok(game.imageUtils)
			}
			"race_details" -> {
				dialog.ok(game.imageUtils)
			}
			"race_recommendations" -> {
				ButtonRaceRecommendationsCenterStage.click(game.imageUtils)
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"recreation" -> {
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"rest" -> {
				Checkbox.click(game.imageUtils)
				dialog.ok(game.imageUtils)
			}
			"rest_and_recreation" -> {
				// Does not have a checkbox unlike the other rest/rec/etc.
				dialog.ok(game.imageUtils)
			}
			"scheduled_races" -> dialog.close(game.imageUtils)
			"schedule_settings" -> dialog.close(game.imageUtils)
			"skill_details" -> dialog.close(game.imageUtils)
			"song_acquired" -> dialog.close(game.imageUtils)
			"spark_details" -> dialog.close(game.imageUtils)
			"sparks" -> dialog.close(game.imageUtils)
			"team_info" -> dialog.close(game.imageUtils)
			"unity_cup_available" -> dialog.close(game.imageUtils)
			"unmet_requirements" -> dialog.close(game.imageUtils)
			// Skill List Dialogs (from SkillList.kt)
			"skill_list_confirmation" -> {
				dialog.ok(game.imageUtils)
				// This dialog takes longer to close than others.
				// Add an extra delay to make sure we don't skip anything.
				game.wait(1.0)
			}
			"skill_list_confirm_exit" -> dialog.ok(game.imageUtils)
			"skills_learned" -> dialog.close(game.imageUtils)
			else -> {
				Log.w(TAG, "[DIALOG] Unknown dialog \"${dialog.name}\" detected so it will not be handled.")
				return DialogHandlerResult.Unhandled(dialog)
			}
		}

		game.wait(0.5)
		return DialogHandlerResult.Handled(dialog)
	}
}
