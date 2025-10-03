package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

object AoHaruStatTableHeaderSpeed : ComponentInterface {
    override val TAG: String = "AoHaruStatTableHeaderSpeed"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_stat_table_header_speed", Screen.BOTTOM_HALF)
    )
}

object ButtonAoHaruRace : ComponentInterface {
    override val TAG: String = "ButtonAoHaruRace"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_race", Screen.BOTTOM_HALF)
    )
}

object ButtonAoHaruRunRace : ComponentInterface {
    override val TAG: String = "ButtonAoHaruRunRace"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_run_race", Screen.BOTTOM_HALF)
    )
}

object ButtonAoHaruFinalRace : ComponentInterface {
    override val TAG: String = "ButtonAoHaruFinalRace"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_final_race", Screen.BOTTOM_HALF)
    )
}

object ButtonAoHaruSelectRace : ComponentInterface {
    override val TAG: String = "ButtonAoHaruSelectRace"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_select_race", Screen.BOTTOM_HALF)
    )
}

object ButtonAoHaruRaceOption : ComponentInterface {
    override val TAG: String = "ButtonAoHaruRaceOption"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_race_option", Screen.BOTTOM_HALF)
    )
}

object LabelAoHaruRaceHeader : ComponentInterface {
    override val TAG: String = "LabelAoHaruRaceHeader"
    override val templates: List<Template> = listOf<Template>(
        Template("aoharu/aoharu_race_header", Screen.TOP_HALF)
    )
}
