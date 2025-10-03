package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ScreenInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

object ScreenRace : ScreenInterface {
    override val TAG: String = "ScreenRace"
    override val templates: List<Template> = listOf<Template>(
        Template("buttons/change", Screen.BOTTOM_HALF),
    )
}
