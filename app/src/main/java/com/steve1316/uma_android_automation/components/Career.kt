package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.ScreenInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

// ===========================================================================
// SCREENS
// ===========================================================================

object ScreenCareerHome : ScreenInterface {
    override val TAG: String = "ScreenHome"
    override val templates: List<Template> = listOf<Template>(
        Template("career/tazuna", Screen.TOP_HALF),
        Template("buttons/log", Screen.BOTTOM_HALF),
    )
}

object ScreenCareerTrainingEvent : ScreenInterface {
    override val TAG: String = "ScreenTrainingEvent"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_event_active", Screen.MIDDLE),
    )
}

object LabelCareerHomeRaceDay : ComponentInterface {
    override val TAG: String = "LabelCareerHomeRaceDay"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_day", Screen.BOTTOM_HALF),
    )
}

object ButtonCareerHomeRaceDayRace : ComponentInterface {
    override val TAG: String = "ButtonCareerHomeRaceDayRace"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_day_race", Screen.BOTTOM_HALF),
    )
}

object ScreenCareerRaceSelectMandatoryGoal : ScreenInterface {
    override val TAG: String = "ScreenCareerRaceSelectMandatoryGoal"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_select_mandatory_goal", Screen.MIDDLE),
    )
}

object ScreenCareerEnd : ScreenInterface {
    override val TAG: String = "ScreenRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/change", Screen.BOTTOM_HALF),
    )
}

// ===========================================================================
// BUTTONS
// ===========================================================================

object ButtonCareerQuick : ComponentInterface {
    override val TAG: String = "ButtonCareerQuick"
    override val templates: List<Template> = listOf<Template>(
        Template("career/career_quick", Screen.BOTTOM_HALF)
    )
}

object ButtonCareerQuickEnabled : ComponentInterface {
    override val TAG: String = "ButtonCareerQuickEnabled"
    override val templates: List<Template> = listOf<Template>(
        Template("career/career_quick_enabled", Screen.BOTTOM_HALF)
    )
}

object ButtonCareerSkip1 : ComponentInterface {
    override val TAG: String = "ButtonCareerSkip1"
    override val templates: List<Template> = listOf<Template>(
        Template("career/career_skip_1", Screen.BOTTOM_HALF)
    )
}
object ButtonCareerSkip2 : ComponentInterface {
    override val TAG: String = "ButtonCareerSkip2"
    override val templates: List<Template> = listOf<Template>(
        Template("career/career_skip_2", Screen.BOTTOM_HALF)
    )
}

object ButtonCareerSkipOff : ComponentInterface {
    override val TAG: String = "ButtonCareerSkipOff"
    override val templates: List<Template> = listOf<Template>(
        Template("career/career_skip_off", Screen.BOTTOM_HALF)
    )
}

object ButtonCompleteCareer : ComponentInterface {
    override val TAG: String = "ButtonCompleteCareer"
    override val templates: List<Template> = listOf<Template>(
        Template("career/complete_career", Screen.BOTTOM_HALF)
    )
}

object ButtonCraneGame : ComponentInterface {
    override val TAG: String = "ButtonCraneGame"
    override val templates: List<Template> = listOf<Template>(
        Template("career/crane_game", Screen.BOTTOM_HALF)
    )
}

object ButtonInheritance : ComponentInterface {
    override val TAG: String = "ButtonInheritance"
    override val templates: List<Template> = listOf<Template>(
        Template("career/inheritance", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceSelectExtra : ComponentInterface {
    override val TAG: String = "ButtonRaceSelectExtra"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_select_extra", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceSelectExtraLocked : ComponentInterface {
    override val TAG: String = "ButtonRaceSelectExtraLocked"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_select_extra_locked", Screen.BOTTOM_HALF)
    )
}

object ButtonRaceSelectExtraLockedUmaFinals : ComponentInterface {
    override val TAG: String = "ButtonRaceSelectExtraLockedUmaFinals"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_select_extra_locked_uma_finals", Screen.BOTTOM_HALF)
    )
}

object ButtonRest : ComponentInterface {
    override val TAG: String = "ButtonRest"
    override val templates: List<Template> = listOf<Template>(
        Template("career/rest", Screen.BOTTOM_HALF)
    )
}

object ButtonRestAndRecreation : ComponentInterface {
    override val TAG: String = "ButtonRestAndRecreation"
    override val templates: List<Template> = listOf<Template>(
        Template("career/rest_and_recreation", Screen.BOTTOM_HALF)
    )
}

object ButtonInfirmary : ComponentInterface {
    override val TAG: String = "ButtonInfirmary"
    override val templates: List<Template> = listOf<Template>(
        Template("career/infirmary", Screen.BOTTOM_HALF)
    )
}

object ButtonRecreation : ComponentInterface {
    override val TAG: String = "ButtonRecreation"
    override val templates: List<Template> = listOf<Template>(
        Template("career/recreation", Screen.BOTTOM_HALF)
    )
}

// ===========================================================================
// OTHER
// ===========================================================================

object StatTableHeaderSpeed : ComponentInterface {
    override val TAG: String = "StatTableHeaderSpeed"
    override val templates: List<Template> = listOf<Template>(
        Template("career/stat_table_header_speed", Screen.MIDDLE)
    )
}

object StatTableHeaderSkillPoints : ComponentInterface {
    override val TAG: String = "StatTableHeaderSkillPoints"
    override val templates: List<Template> = listOf<Template>(
        Template("career/stat_table_header_skillpoints", Screen.MIDDLE)
    )
}

object TrainingFailureChance : ComponentInterface {
    override val TAG: String = "TrainingFailureChance"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_failure_chance", Screen.BOTTOM_HALF)
    )
}

object MoodGreat : ComponentInterface {
    override val TAG: String = "MoodGreat"
    override val templates: List<Template> = listOf<Template>(
        Template("career/mood_great", Screen.TOP_HALF)
    )
}

object MoodGood : ComponentInterface {
    override val TAG: String = "MoodGood"
    override val templates: List<Template> = listOf<Template>(
        Template("career/mood_good", Screen.TOP_HALF)
    )
}

object MoodNormal : ComponentInterface {
    override val TAG: String = "MoodNormal"
    override val templates: List<Template> = listOf<Template>(
        Template("career/mood_normal", Screen.TOP_HALF)
    )
}

object RaceNoneAvailable : ComponentInterface {
    override val TAG: String = "RaceNoneAvailable"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_none_available", Screen.MIDDLE)
    )
}

object RaceSkipLocked : ComponentInterface {
    override val TAG: String = "RaceSkipLocked"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_skip_locked", Screen.BOTTOM_HALF)
    )
}

object ButtonViewResultsLocked : ComponentInterface {
    override val TAG: String = "ButtonViewResultsLocked"
    override val templates: List<Template> = listOf<Template>(
        Template("career/view_results_locked", Screen.BOTTOM_HALF)
    )
}

object ButtonEndCareer : ComponentInterface {
    override val TAG: String = "ButtonEndCareer"
    override val templates: List<Template> = listOf<Template>(
        Template("career/end_career", Screen.BOTTOM_HALF)
    )
}

object RaceRepeatWarning : ComponentInterface {
    override val TAG: String = "RaceRepeatWarning"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_repeat_warning")
    )
}

object ButtonRaceListFullStats : ComponentInterface {
    override val TAG: String = "ButtonRaceListFullStats"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_list_full_stats")
    )
}

object ButtonHomeFullStats : ComponentInterface {
    override val TAG: String = "ButtonHomeFullStats"
    override val templates: List<Template> = listOf<Template>(
        Template("career/home_full_stats")
    )
}

object ButtonRecreationDate : ComponentInterface {
    override val TAG: String = "ButtonRecreationDate"
    override val templates: List<Template> = listOf<Template>(
        Template("career/recreation_date", Screen.BOTTOM_HALF)
    )
}

object LabelOrdinaryCuties : ComponentInterface {
    override val TAG: String = "LabelOrdinaryCuties"
    override val templates: List<Template> = listOf<Template>(
        Template("career/ordinary_cuties", Screen.BOTTOM_HALF)
    )
}

object IconRaceNotEnoughFans : ComponentInterface {
    override val TAG: String = "IconRaceNotEnoughFans"
    override val templates: List<Template> = listOf<Template>(
        Template("career/race_not_enough_fans", Screen.MIDDLE)
    )
}

object IconTrainingHeaderSpeed : ComponentInterface {
    override val TAG: String = "IconTrainingHeaderSpeed"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_header_speed", Screen.TOP_HALF)
    )
}

object ButtonTrainingSpeed : ComponentInterface {
    override val TAG: String = "ButtonTrainingSpeed"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_speed", Screen.BOTTOM_HALF)
    )
}

object ButtonTrainingStamina : ComponentInterface {
    override val TAG: String = "ButtonTrainingStamina"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_stamina", Screen.BOTTOM_HALF)
    )
}

object ButtonTrainingPower : ComponentInterface {
    override val TAG: String = "ButtonTrainingPower"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_power", Screen.BOTTOM_HALF)
    )
}

object ButtonTrainingGuts : ComponentInterface {
    override val TAG: String = "ButtonTrainingGuts"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_guts", Screen.BOTTOM_HALF)
    )
}

object ButtonTrainingWit : ComponentInterface {
    override val TAG: String = "ButtonTrainingWit"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training_wit", Screen.BOTTOM_HALF)
    )
}

object ButtonTraining : ComponentInterface {
    override val TAG: String = "ButtonTraining"
    override val templates: List<Template> = listOf<Template>(
        Template("career/training", Screen.BOTTOM_HALF)
    )
}

object LabelEnergy : ComponentInterface {
    override val TAG: String = "LabelEnergy"
    override val templates: List<Template> = listOf<Template>(
        Template("career/energy", Screen.TOP_HALF)
    )
}