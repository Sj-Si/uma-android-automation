package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.MainApplication
import com.steve1316.uma_android_automation.utils.UserConfig
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.MyAccessibilityService

/**
    * Performs a tap on the screen at the coordinates and then will wait until the game processes the server request and gets a response back.
    *
    * @param x The x-coordinate.
    * @param y The y-coordinate.
    * @param imageName The template image name to use for tap location randomization.
    * @param taps The number of taps.
    * @param ignoreWaiting Flag to ignore checking if the game is busy loading.
    */
fun tap(x: Double, y: Double, imageName: String, taps: Int = 1, ignoreWaiting: Boolean = false) {
    // Perform the tap.
    MyAccessibilityService.getInstance().tap(x, y, imageName, taps = taps)

    if (!ignoreWaiting) {
        // Now check if the game is waiting for a server response from the tap and wait if necessary.
        GameUtils.wait(0.20)
        GameUtils.waitForLoading()
    }
}

open class Component {
    data class Template(val name: String, val region: IntArray = Screen.FULL)
    
    // List of templates which must all match (logical AND)
    abstract val templates: List<Template>

    //abstract fun mustBeOverridden()

    open fun check(tries: Int = 1, failEarly: Boolean = false): List<Point?>? {
        // If any of the templates are not found, this component doesnt exist on screen.
        var res: MutableList<Point?> = mutableListOf<Point?>()
        for (template in templates) {
            val point = ImageUtils.findImage(template.name, region=template.region, tries=tries).first
            if (failEarly && point == null) {
                return null
            }
            res.add(point)
        }

        return if (res.contains(null)) null else res
    }
}

open class ComplexComponent: Component() {
    // Complex components are not interactive and typically contain
    // more than one template that all need to exist on screen at
    // the same time.
    private val TAG: String = "ComplexComponent"
}

open class InteractiveComponent: Component() {
    // Interactive components will only ever have a single template.
    private val TAG: String = "InteractiveComponent"

    override fun check(tries: Int = 1, failEarly: Boolean = false): Point? {
        val res = super.check(tries=tries, failEarly=failEarly)

        if (res == null) {
            return null
        }

        // Any size other than 1 indicates a programmer error.
        assert(res.size == 1)
        // The only way this should fail is if the parent function is changed.
        assert(res.firstOrNull() != null)

        return res.first()
    }

    fun click(tries: Int = 1): Point? {
        val point = check(tries)
        if (point == null) {
            return null
        }

        tap(point.x, point.y, templates.first().name, taps=1)
        return point
    }
}

object ComponentDetection {

}

object ComponentInteraction {

}

// ===========================================================================
// COMPONENTS
// ===========================================================================


// ---------------------------------------------------------------------------
// BUTTONS
// ---------------------------------------------------------------------------

open class ButtonBack: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/back")
    )
}

open class ButtonBackGreen: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/back_green")
    )
}

open class ButtonCancel: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/cancel")
    )
}

open class ButtonChange: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/change")
    )
}

open class ButtonClose: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/close")
    )
}

open class ButtonCollectAll: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/collect_all")
    )
}

open class ButtonCompleteCareer: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/complete_career")
    )
}

open class ButtonConfirm: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/confirm")
    )
}

open class ButtonCraneGame: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/crane_game")
    )
}

open class ButtonDailyRaces: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races")
    )
}

open class ButtonDailyRacesDisabled: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_disabled")
    )
}

open class ButtonDailyRacesJupiterCup: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_jupiter_cup_logo")
    )
}

open class ButtonDailyRacesMoonlightSho: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_moonlight_sho_logo")
    )
}

open class ButtonDialogViewStoryRadioScreenOrientationPortrait: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/dialog_view_story_radio_screen_orientation_portrait")
    )
}

open class ButtonDialogViewStoryRadioVoiceOff: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/dialog_view_story_radio_voice_off")
    )
}

open class ButtonFullStats: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/full_stats")
    )
}

open class ButtonHomeSpecialMissions: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/home_special_missions")
    )
}

open class ButtonInheritance: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/inheritance")
    )
}

open class ButtonLegendRace: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race")
    )
}

open class ButtonLegendRaceDisabled: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race_disabled")
    )
}

open class ButtonLegendRaceSpecialMissions: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race_special_missions")
    )
}

open class ButtonNext: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/next")
    )
}

open class ButtonNextWithImage: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/next_with_image")
    )
}

open class ButtonNo: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/no")
    )
}

open class ButtonOk: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/ok")
    )
}

open class ButtonRace: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race")
    )
}

open class ButtonRaceAgain: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_again")
    )
}

open class ButtonRaceDetails: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_details")
    )
}

open class ButtonRaceEnd: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_end")
    )
}

open class ButtonRaceEvents: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_events")
    )
}

open class ButtonRaceExclamation: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_exclamation")
    )
}

open class ButtonRaceExclamationShiftedUp: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_exclamation_shifted_up")
    )
}

open class ButtonRaceManual: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_manual")
    )
}

open class ButtonRaceResults: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_results")
    )
}

open class ButtonRaceRetry: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_retry")
    )
}

open class ButtonRestore: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/restore")
    )
}

open class ButtonSeeResults: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/see_results")
    )
}

open class ButtonShop: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/shop")
    )
}

open class ButtonSkip: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/skip")
    )
}

open class ButtonTeamRace: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/team_race")
    )
}

open class ButtonTeamTrials: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/team_trials")
    )
}

open class ButtonViewResults: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/view_results")
    )
}

// ---------------------------------------------------------------------------
// SCREENS
// ---------------------------------------------------------------------------
