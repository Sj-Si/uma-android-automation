package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ScreenInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.TemplateComparisonMode
import com.steve1316.uma_android_automation.utils.Screen

object ScreenRace : ScreenInterface {
    override val TAG: String = "ScreenRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/change", Screen.BOTTOM_HALF),
    )
}

object ScreenTitle : ScreenInterface {
    override val TAG: String = "ScreenTitle"
    override val templates: List<Template> = listOf<Template>(
        Template("title_screen_logo", Screen.MIDDLE),
    )
}

object ScreenHome : ScreenInterface {
    override val TAG: String = "ScreenHome"
    override val comparisonMode: TemplateComparisonMode = TemplateComparisonMode.OR
    override val templates: List<Template> = listOf<Template>(
        Template("menu_bar_home_unselected", Screen.BOTTOM_HALF),
        Template("menu_bar_home_selected", Screen.BOTTOM_HALF),
    )
}

object ScreenLegendRaceHome : ScreenInterface {
    override val TAG: String = "ScreenLegendRaceHome"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/legend_race_special_missions", Screen.MIDDLE),
    )
}
