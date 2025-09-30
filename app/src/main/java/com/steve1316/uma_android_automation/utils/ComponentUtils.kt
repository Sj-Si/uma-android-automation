package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.MainApplication
import com.steve1316.uma_android_automation.utils.UserConfig
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog

open class Component {
    data class Template(val name: String, val region: IntArray)
    
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
}

object ComponentDetection {

}

object ComponentInteraction {

}

// ===========================================================================
// COMPONENTS
// ===========================================================================

open class ButtonNext: InteractiveComponent() {
    override val templates: List<Template> = listOf<Template>(
        Template()
    )
}