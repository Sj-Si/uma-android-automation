package com.steve1316.uma_android_automation.data

import com.steve1316.uma_android_automation.utils.types.RaceInfo

class RaceData {
    companion object {
        // {
        //      dayNumber: {
        //          "raceName": RaceInfo,
        //          ...
        //      }
        // }
        // Do not confuse dayNumber with turnNumber.
        // dayNumber starts from Junior Jan 1.
        // turnNumber starts from Junior June 1 (or 2nd cant remember)
        var races: MutableMap<Int, MutableMap<String, RaceInfo>> = mutableMapOf()
    }
}