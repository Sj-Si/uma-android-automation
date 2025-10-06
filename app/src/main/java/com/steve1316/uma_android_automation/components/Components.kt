package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentUtils
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.Screen

import android.graphics.Bitmap
import org.opencv.core.Point

data class Template(val name: String, val region: IntArray = Screen.FULL)

interface BaseComponentInterface {
    // Logging tag.
    val TAG: String
    // List of templates which must all match (logical AND)
    val templates: List<Template>

    fun check(tries: Int = 1): Boolean
}

interface ComponentInterface : BaseComponentInterface {
    /** Finds image on screen and returns its location if it exists. */
    fun find(tries: Int = 1): Pair<Point?, Bitmap> {
        val template = templates.first()
        val (point, bitmap) = ImageUtils.findImage(template.name, region=template.region, tries=tries)

        if (point == null) {
            return Pair(null, bitmap)
        }

        return Pair(point, bitmap)
    }

    /** Finds image on screen and returns boolean whether it exists. */
    override fun check(tries: Int): Boolean {
        return find(tries=tries).first != null
    }

    fun confirm(tries: Int = 1): Boolean {
        return ImageUtils.confirmLocation(templates.first().name, tries=tries, region=templates.first().region)
    }

    fun click(tries: Int = 1, taps: Int = 1): Boolean {
        val point = find(tries).first
        if (point == null) {
            return false
        }

        ComponentUtils.tap(point.x, point.y, templates.first().name, taps=taps)
        return true
    }
}

interface ScreenInterface : ComponentInterface {
    override fun check(tries: Int): Boolean {
        // If any of the templates are not found, this component doesnt exist on screen.
        for (template in templates) {
            val (point, _) = ImageUtils.findImage(template.name, region=template.region, tries=tries)
            MessageLog.i("ScreenInterface::check()", "Checking: ${template.name} -> ${point == null}")
            if (point == null) {
                return false
            }
        }
        return true
    }
}