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

object ButtonConfirmExclamation : ComponentInterface {
    override val TAG: String = "ButtonConfirmExclamation"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/confirm_exclamation")
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

object ButtonEditTeam : ComponentInterface {
    override val TAG: String = "ButtonEditTeam"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/edit_team")
    )
}

object ButtonFollow : ComponentInterface {
    override val TAG: String = "ButtonFollow"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/follow")
    )
}

object ButtonGiveUp : ComponentInterface {
    override val TAG: String = "ButtonGiveUp"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/give_up", Screen.TOP_HALF)
    )
}

object ButtonToHome : ComponentInterface {
    override val TAG: String = "ButtonToHome"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/to_home")
    )
}

object RadioPortrait : ComponentInterface {
    override val TAG: String = "RadioPortrait"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/radio_portrait")
    )
}

object RadioLandscape : ComponentInterface {
    override val TAG: String = "RadioLandscape"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/radio_landscape")
    )
}

object RadioVoiceOff : ComponentInterface {
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

object ButtonHomePresents : ComponentInterface {
    override val TAG: String = "ButtonHomePresents"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/home_presents")
    )
}

object ButtonSpecialMissionsTabDaily : ComponentInterface {
    override val TAG: String = "ButtonSpecialMissionsTabDaily"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/special_missions_tab_daily")
    )
}

object ButtonSpecialMissionsTabMain : ComponentInterface {
    override val TAG: String = "ButtonSpecialMissionsTabMain"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/special_missions_tab_main")
    )
}

object ButtonSpecialMissionsTabTitles : ComponentInterface {
    override val TAG: String = "ButtonSpecialMissionsTabTitles"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/special_missions_tab_titles")
    )
}

object ButtonSpecialMissionsTabSpecial : ComponentInterface {
    override val TAG: String = "ButtonSpecialMissionsTabSpecial"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/special_missions_tab_special")
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

object ButtonRaceHardInactive : ComponentInterface {
    override val TAG: String = "ButtonRaceHardInactive"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_hard_inactive")
    )
}

object ButtonRaceHardActive : ComponentInterface {
    override val TAG: String = "ButtonRaceHardActive"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_hard_active")
    )
}

object ButtonLegendRaceHomeSpecialMissions : ComponentInterface {
    override val TAG: String = "ButtonLegendRaceHomeSpecialMissions"
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

object ButtonOptions : ComponentInterface {
    override val TAG: String = "ButtonOptions"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/options")
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

object ButtonRaceRecommendationsCenterStage : ComponentInterface {
    override val TAG: String = "ButtonRaceRecommendationsCenterStage"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_recommendations_center_stage")
    )
}

object ButtonRaceRecommendationsPathToFame : ComponentInterface {
    override val TAG: String = "ButtonRaceRecommendationsPathToFame"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_recommendations_path_to_fame")
    )
}

object ButtonRaceRecommendationsForgeYourOwnPath : ComponentInterface {
    override val TAG: String = "ButtonRaceRecommendationsForgeYourOwnPath"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_recommendations_forge_your_own_path")
    )
}

object ButtonRaceResults : ComponentInterface {
    override val TAG: String = "ButtonRaceResults"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/race_results")
    )
}

object ButtonRestore : ComponentInterface {
    override val TAG: String = "ButtonRestore"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/restore")
    )
}

object ButtonSaveAndExit : ComponentInterface {
    override val TAG: String = "ButtonSaveAndExit"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/save_and_exit", Screen.TOP_HALF)
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

object ButtonTitleScreen : ComponentInterface {
    override val TAG: String = "ButtonTitleScreen"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/title_screen", Screen.BOTTOM_HALF)
    )
}

object ButtonTryAgain : ComponentInterface {
    override val TAG: String = "ButtonTryAgain"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/try_again", Screen.BOTTOM_HALF)
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

object CheckboxDoNotShowAgain : ComponentInterface {
    override val TAG: String = "CheckboxDoNotShowAgain"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/checkbox_do_not_show_again")
    )
}

// More complex buttons

object ButtonMenuBarHomeSelected : ComponentInterface {
    override val TAG: String = "ButtonMenuBarHomeSelected"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_home_selected", Screen.BOTTOM_HALF)
    )
}

object ButtonMenuBarHomeUnselected : ComponentInterface {
    override val TAG: String = "ButtonMenuBarHomeUnselected"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_home_unselected", Screen.BOTTOM_HALF)
    )
}

object ButtonMenuBarHome : MultiStateButtonInterface {
    override val TAG: String = "ButtonMenuBarHome"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_home_unselected", Screen.BOTTOM_HALF),
        Template("buttons/menu_bar_home_selected", Screen.BOTTOM_HALF),
    )
}

object ButtonMenuBarRaceSelected : ComponentInterface {
    override val TAG: String = "ButtonMenuBarRaceSelected"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_race_selected", Screen.BOTTOM_HALF)
    )
}

object ButtonMenuBarRaceUnselected : ComponentInterface {
    override val TAG: String = "ButtonMenuBarRaceUnselected"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_race_unselected", Screen.BOTTOM_HALF)
    )
}

object ButtonMenuBarRace : MultiStateButtonInterface {
    override val TAG: String = "ButtonMenuBarRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/menu_bar_race_unselected", Screen.BOTTOM_HALF),
        Template("buttons/menu_bar_race_selected", Screen.BOTTOM_HALF),
    )
}
