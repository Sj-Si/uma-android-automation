package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen


object ButtonBack : ComponentInterface {
    override val TAG: String = "ButtonBack"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/back")
    )
}

object ButtonBackGreen : ComponentInterface {
    override val TAG: String = "ButtonBackGreen"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/back_green", Screen.BOTTOM_HALF)
    )
}

object ButtonCancel : ComponentInterface {
    override val TAG: String = "ButtonCancel"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/cancel", Screen.BOTTOM_HALF)
    )
}

object ButtonChange : ComponentInterface {
    override val TAG: String = "ButtonChange"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/change")
    )
}

object ButtonClose : ComponentInterface {
    override val TAG: String = "ButtonClose"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/close", Screen.BOTTOM_HALF)
    )
}

object ButtonCollectAll : ComponentInterface {
    override val TAG: String = "ButtonCollectAll"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/collect_all", Screen.BOTTOM_HALF)
    )
}



object ButtonConfirm : ComponentInterface {
    override val TAG: String = "ButtonConfirm"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/confirm")
    )
}



object ButtonDailyRaces : ComponentInterface {
    override val TAG: String = "ButtonDailyRaces"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races")
    )
}

object ButtonDailyRacesDisabled : ComponentInterface {
    override val TAG: String = "ButtonDailyRacesDisabled"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_disabled")
    )
}

object ButtonDailyRacesJupiterCup : ComponentInterface {
    override val TAG: String = "ButtonDailyRacesJupiterCup"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_jupiter_cup_logo")
    )
}

object ButtonDailyRacesMoonlightSho : ComponentInterface {
    override val TAG: String = "ButtonDailyRacesMoonlightSho"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/daily_races_moonlight_sho_logo")
    )
}

object ButtonDialogViewStoryRadioScreenOrientationPortrait : ComponentInterface {
    override val TAG: String = "ButtonDialogViewStoryRadioScreenOrientationPortrait"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/dialog_view_story_radio_screen_orientation_portrait")
    )
}

object ButtonDialogViewStoryRadioVoiceOff : ComponentInterface {
    override val TAG: String = "ButtonDialogViewStoryRadioVoiceOff"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/dialog_view_story_radio_voice_off")
    )
}

object ButtonHomeSpecialMissions : ComponentInterface {
    override val TAG: String = "ButtonHomeSpecialMissions"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/home_special_missions")
    )
}



object ButtonLegendRace : ComponentInterface {
    override val TAG: String = "ButtonLegendRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race")
    )
}

object ButtonLegendRaceDisabled : ComponentInterface {
    override val TAG: String = "ButtonLegendRaceDisabled"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race_disabled")
    )
}

object ButtonLegendRaceSpecialMissions : ComponentInterface {
    override val TAG: String = "ButtonLegendRaceSpecialMissions"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race_special_missions")
    )
}

object ButtonNext : ComponentInterface {
    override val TAG: String = "ButtonNext"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/next")
    )
}

object ButtonNextWithImage : ComponentInterface {
    override val TAG: String = "ButtonNextWithImage"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/next_with_image", Screen.BOTTOM_HALF)
    )
}

object ButtonNo : ComponentInterface {
    override val TAG: String = "ButtonNo"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/no")
    )
}

object ButtonOk : ComponentInterface {
    override val TAG: String = "ButtonOk"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/ok")
    )
}

object ButtonRace : ComponentInterface {
    override val TAG: String = "ButtonRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race")
    )
}

object ButtonRaceAgain : ComponentInterface {
    override val TAG: String = "ButtonRaceAgain"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_again", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceDetails : ComponentInterface {
    override val TAG: String = "ButtonRaceDetails"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_details")
    )
}

object ButtonRaceEnd : ComponentInterface {
    override val TAG: String = "ButtonRaceEnd"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_end")
    )
}

object ButtonRaceEvents : ComponentInterface {
    override val TAG: String = "ButtonRaceEvents"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_events")
    )
}

object ButtonRaceExclamation : ComponentInterface {
    override val TAG: String = "ButtonRaceExclamation"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_exclamation")
    )
}

object ButtonRaceExclamationShiftedUp : ComponentInterface {
    override val TAG: String = "ButtonRaceExclamationShiftedUp"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_exclamation_shifted_up")
    )
}

object ButtonRaceManual : ComponentInterface {
    override val TAG: String = "ButtonRaceManual"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_manual")
    )
}

object ButtonRaceResults : ComponentInterface {
    override val TAG: String = "ButtonRaceResults"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_results")
    )
}

object ButtonTryAgain : ComponentInterface {
    override val TAG: String = "ButtonTryAgain"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/try_again", Screen.BOTTOM_HALF)
    )
}

object ButtonRestore : ComponentInterface {
    override val TAG: String = "ButtonRestore"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/restore")
    )
}

object ButtonSeeResults : ComponentInterface {
    override val TAG: String = "ButtonSeeResults"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/see_results", Screen.BOTTOM_HALF)
    )
}

object ButtonShop : ComponentInterface {
    override val TAG: String = "ButtonShop"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/shop")
    )
}

object ButtonSkip : ComponentInterface {
    override val TAG: String = "ButtonSkip"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/skip", Screen.BOTTOM_HALF)
    )
}

object ButtonTeamRace : ComponentInterface {
    override val TAG: String = "ButtonTeamRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/team_race")
    )
}

object ButtonTeamTrials : ComponentInterface {
    override val TAG: String = "ButtonTeamTrials"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/team_trials")
    )
}

object ButtonViewResults : ComponentInterface {
    override val TAG: String = "ButtonViewResults"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/view_results")
    )
}

object ButtonRaceStrategyFront : ComponentInterface {
    override val TAG: String = "ButtonRaceStrategyFront"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/strategy_front_select", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceStrategyPace : ComponentInterface {
    override val TAG: String = "ButtonRaceStrategyPace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/strategy_pace_select", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceStrategyLate : ComponentInterface {
    override val TAG: String = "ButtonRaceStrategyLate"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/strategy_late_select", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceStrategyEnd : ComponentInterface {
    override val TAG: String = "ButtonRaceStrategyEnd"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/strategy_end_select", Screen.BOTTOM_HALF)
    )
}

object Checkbox : ComponentInterface {
    override val TAG: String = "Checkbox"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/checkbox")
    )
}