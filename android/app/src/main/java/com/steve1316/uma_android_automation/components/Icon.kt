/** Defines icon components.
 *
 * These are images which are typically not clickable, however they DO
 * have click functionality; it just isn't their primary purpose. This is
 * why we classify them as Icons instead of Buttons.
 */

package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.Region

object IconMoodGreat : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconMoodGreat"
    override val template = Template("components/icon/mood_great", region = Region.topHalf)
}

object IconMoodGood : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconMoodGood"
    override val template = Template("components/icon/mood_good", region = Region.topHalf)
}

object IconMoodNormal : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconMoodNormal"
    override val template = Template("components/icon/mood_normal", region = Region.topHalf)
}

object IconMoodBad : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconMoodBad"
    override val template = Template("components/icon/mood_bad", region = Region.topHalf)
}

object IconMoodAwful : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconMoodAwful"
    override val template = Template("components/icon/mood_awful", region = Region.topHalf)
}

object IconTrainingHeaderSpeed : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingHeaderSpeed"
    override val template = Template("components/icon/training_header_speed", region = Region.topHalf)
}

object IconTrainingHeaderStamina : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingHeaderStamina"
    override val template = Template("components/icon/training_header_stamina", region = Region.topHalf)
}

object IconTrainingHeaderPower : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingHeaderPower"
    override val template = Template("components/icon/training_header_power", region = Region.topHalf)
}

object IconTrainingHeaderGuts : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingHeaderGuts"
    override val template = Template("components/icon/training_header_guts", region = Region.topHalf)
}

object IconTrainingHeaderWit : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingHeaderWit"
    override val template = Template("components/icon/training_header_wit", region = Region.topHalf)
}

object IconHorseshoe : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconHorseshoe"
    override val template = Template("components/icon/horseshoe")
}

object IconDoubleCircle : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconDoubleCircle"
    override val template = Template("components/icon/double_circle")
}

object IconUnityCupRaceEndLogo : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconUnityCupRaceEndLogo"
    override val template = Template("components/icon/unity_cup_race_end_logo", region = Region.topHalf)
}

object IconTazuna : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTazuna"
    override val template = Template("components/icon/tazuna", region = Region.topHalf)
}

object IconRaceDayRibbon : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceDayRibbon"
    override val template = Template("components/icon/race_day_ribbon", region = Region.bottomHalf)
}

object IconGoalRibbon : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconGoalRibbon"
    override val template = Template("components/icon/goal_ribbon", region = Region.leftHalf)
}

object IconRaceListPredictionDoubleStar : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceListPredictionDoubleStar"
    override val template = Template("components/icon/race_list_prediction_double_star", region = Region.rightHalf)
}

object IconRaceListSelectionBracketBottomRight : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceListSelectionBracketBottomRight"
    override val template = Template("components/icon/race_list_selection_bracket_bottom_right", region = Region.rightHalf)
}

object IconRaceListMaidenPill : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceListMaidenPill"
    override val template = Template("components/icon/race_list_maiden_pill", region = Region.bottomHalf)
}

object IconScrollListTopLeft : ComponentInterface {
    override val TAG: String = "IconScrollListTopLeft"
    override val template = Template("components/icon/scroll_list_top_left", region = Region.leftHalf)
}

object IconScrollListBottomRight : ComponentInterface {
    override val TAG: String = "IconScrollListBottomRight"
    override val template = Template("components/icon/scroll_list_bottom_right", region = Region.rightHalf)
}

object IconObtainedPill : ComponentInterface {
    override val TAG: String = "IconObtainedPill"
    override val template = Template("components/icon/obtained_pill", region = Region.rightHalf)
}

object IconSkillTitleDoubleCircle : ComponentInterface {
    override val TAG: String = "IconSkillTitleDoubleCircle"
    override val template = Template("components/icon/skill_title_double_circle")
}

object IconSkillTitleCircle : ComponentInterface {
    override val TAG: String = "IconSkillTitleCircle"
    override val template = Template("components/icon/skill_title_circle")
}

object IconSkillTitleX : ComponentInterface {
    override val TAG: String = "IconSkillTitleX"
    override val template = Template("components/icon/skill_title_x")
}

object IconRaceListTopLeft : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceListTopLeft"
    override val template = Template("components/icon/race_list_top_left", region = Region.leftHalf)
}

object IconRaceListBottomRight : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceListBottomRight"
    override val template = Template("components/icon/race_list_bottom_right", region = Region.rightHalf)
}

object IconTeamTrialsOpponentSelectionTeamRank : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTeamTrialsOpponentSelectionTeamRank"
    override val template = Template("components/icon/team_trials_opponent_selection_team_rank", region = Region.leftHalf)
}

object IconExtraRacePill : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconExtraRacePill"
    override val template = Template("components/icon/extra_race_pill")
}

object IconTeamTrialsHorseshoe : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTeamTrialsHorseshoe"
    override val template = Template("components/icon/team_trials_horseshoe")
}

object IconPleasingParfait : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconPleasingParfait"
    override val template = Template("components/icon/pleasing_parfait")
}

object IconOneFreePerDayTooltip : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconOneFreePerDayTooltip"
    override val template = Template("components/icon/one_free_per_day_tooltip", region = Region.middle)
}

object IconChampionsMeetingRewardChest : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconChampionsMeetingRewardChest"
    override val template = Template("components/icon/champions_meeting_reward_chest", region = Region.middle)
}

object IconTaskClearToast : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTaskClearToast"
    override val template = Template("components/icon/task_clear_toast", region = Region.topHalf)
}

object IconDialogScrollListTopLeft : ComponentInterface {
    override val TAG: String = "IconDialogScrollListTopLeft"
    override val template = Template("components/icon/dialog_scroll_list_top_left", region = Region.leftHalf)
}

object IconDialogScrollListBottomRight : ComponentInterface {
    override val TAG: String = "IconDialogScrollListBottomRight"
    override val template = Template("components/icon/dialog_scroll_list_bottom_right", region = Region.rightHalf)
}

object IconNotificationExclamationHalfHeight : ComponentInterface {
    override val TAG: String = "IconNotificationExclamationHalfHeight"
    override val template = Template("components/icon/notification_exclamation_half_height")
}

object IconScenarioSelectUraFinale : ComponentInterface {
    override val TAG: String = "IconScenarioSelectUraFinale"
    override val template = Template("components/icon/scenario_select_ura_finale", region = Region.middle)
}

object IconScenarioSelectUnityCup : ComponentInterface {
    override val TAG: String = "IconScenarioSelectUnityCup"
    override val template = Template("components/icon/scenario_select_unity_cup", region = Region.middle)
}

object IconFollowingPill : ComponentInterface {
    override val TAG: String = "IconFollowingPill"
    override val template = Template("components/icon/following_pill")
}

object IconTraineePill : ComponentInterface {
    override val TAG: String = "IconTraineePill"
    override val template = Template("components/icon/trainee_pill")
}

object IconDuplicateSupportPill : ComponentInterface {
    override val TAG: String = "IconDuplicateSupportPill"
    override val template = Template("components/icon/duplicate_support_pill")
}

object IconSelectedPill : ComponentInterface {
    override val TAG: String = "IconSelectedPill"
    override val template = Template("components/icon/selected_pill")
}

object IconEnergyBarLeftPart : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconEnergyBarLeftPart"
    override val template = Template("components/icon/energy_bar_left_part", region = Region.topHalf)
}

object IconEnergyBarRightPart0 : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconEnergyBarRightPart0"
    override val template = Template("components/icon/energy_bar_right_part_0", region = Region.topHalf)
}

object IconEnergyBarRightPart1 : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconEnergyBarRightPart1"
    override val template = Template("components/icon/energy_bar_right_part_1", region = Region.topHalf)
}

object IconRaceNotEnoughFans : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceNotEnoughFans"
    override val template = Template("components/icon/race_not_enough_fans", region = Region.middle)
}

object IconStatBlockSpeed : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockSpeed"
    override val template = Template("components/icon/stat_block_speed")
}

object IconStatBlockStamina : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockStamina"
    override val template = Template("components/icon/stat_block_stamina")
}

object IconStatBlockPower : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockPower"
    override val template = Template("components/icon/stat_block_power")
}

object IconStatBlockGuts : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockGuts"
    override val template = Template("components/icon/stat_block_guts")
}

object IconStatBlockWit : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockWit"
    override val template = Template("components/icon/stat_block_wit")
}

object IconStatBlockTrainer : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatBlockTrainer"
    override val template = Template("components/icon/stat_block_trainer")
}

object IconStatSupportEtsukoOtonashi : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatSupportEtsukoOtonashi"
    override val template = Template("components/icon/stat_support_etsuko_otonashi")
}

object IconStatSupportRikoKashimoto : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatSupportRikoKashimoto"
    override val template = Template("components/icon/stat_support_riko_kashimoto")
}

object IconStatSupportYayoiAkikawa : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatSupportYayoiAkikawa"
    override val template = Template("components/icon/stat_support_yayoi_akikawa")
}

object IconStatSkillHint : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconStatSkillHint"
    override val template = Template("components/icon/stat_skill_hint")
}

object IconRecreationDate : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRecreationDate"
    override val template = Template("components/icon/recreation_date", region = Region.bottomHalf)
}

object IconTrainingEventHorseshoe : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconTrainingEventHorseshoe"
    override val template = Template("components/icon/training_event_horseshoe", region = Region.leftHalf)
}

object IconEventTitleSpacer : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconEventTitleSpacer"
    override val template = Template("components/icon/event_title_spacer")
}

object IconUnityCupSpiritExplosion : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconUnityCupSpiritExplosion"
    override val template = Template("components/icon/unitycup_spirit_explosion", region = Region.topRightThird)
}

object IconUnityCupSpiritTraining : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconUnityCupSpiritTraining"
    override val template = Template("components/icon/unitycup_spirit_training", region = Region.topRightThird)
}

object IconUnityCupTutorialHeader : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconUnityCupTutorialHeader"
    override val template = Template("components/icon/unitycup_tutorial_header", region = Region.topHalf)
}

object IconInfirmaryEventHeader : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconInfirmaryEventHeader"
    override val template = Template("components/icon/infirmary_event_header", region = Region.topHalf)
}

object IconRaceAgendaEmpty : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]IconRaceAgendaEmpty"
    override val template = Template("components/icon/race_agenda_empty", region = Region.topHalf)
}
