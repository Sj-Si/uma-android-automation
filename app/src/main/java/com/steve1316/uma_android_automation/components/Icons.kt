package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

object IconSelectionBracket  : ComponentInterface {
    override val TAG: String = "IconSelectionBracket"
    override val templates: List<Template> = listOf<Template>(
        Template("selection_bracket"),
    )
}