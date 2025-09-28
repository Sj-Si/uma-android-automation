package com.steve1316.uma_android_automation.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.R
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.UserConfig

class OCRFragment : PreferenceFragmentCompat() {
	private val TAG: String = "OCRFragment"
	
	private lateinit var sharedPreferences: SharedPreferences
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		// Display the layout using the preferences xml.
		setPreferencesFromResource(R.xml.preferences_ocr, rootKey)
		
		// Get the SharedPreferences.
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		
        // Grab the saved preferences from the previous time the user used the app.
		val ocrThreshold: Int = sharedPreferences.getInt("ocrThreshold", 230)
		val bEnableOcrAutomaticRetry: Boolean = sharedPreferences.getBoolean("bEnableOcrAutomaticRetry", true)
		val ocrConfidence: Int = sharedPreferences.getInt("ocrConfidence", 80)

		// Get references to the Preference components.
		val ocrThresholdPreference = findPreference<SeekBarPreference>("ocrThreshold")!!
		val bEnableOcrAutomaticRetryPreference = findPreference<CheckBoxPreference>("bEnableOcrAutomaticRetry")!!
		val ocrConfidencePreference = findPreference<SeekBarPreference>("ocrConfidence")!!
		
		// Now set the following values from the SharedPreferences.
		ocrThresholdPreference.value = ocrThreshold
		bEnableOcrAutomaticRetryPreference.isChecked = bEnableOcrAutomaticRetry
		ocrConfidencePreference.value = ocrConfidence
		
		MessageLog.d(TAG, "OCR Preferences created successfully.")
	}
	
	// This listener is triggered whenever the user changes a Preference setting in the Settings Page.
	private val onSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		if (key != null) {
			when (key) {
				"ocrThreshold" -> {
					val ocrThresholdPreference = findPreference<SeekBarPreference>("ocrThreshold")!!
					
					sharedPreferences.edit {
						putInt("ocrThreshold", ocrThresholdPreference.value)
						commit()
					}
				}
				"bEnableOcrAutomaticRetry" -> {
					val bEnableOcrAutomaticRetryPreference = findPreference<CheckBoxPreference>("bEnableOcrAutomaticRetry")!!
					
					sharedPreferences.edit {
						putBoolean("bEnableOcrAutomaticRetry", bEnableOcrAutomaticRetryPreference.isChecked)
						commit()
					}
				}
				"ocrConfidence" -> {
					val ocrConfidencePreference = findPreference<SeekBarPreference>("ocrConfidence")!!
					
					sharedPreferences.edit {
						putInt("oceConfidence", ocrConfidencePreference.value)
						commit()
					}
				}
			}

            // Re-initialize our UserConfig now that we have committed changes.
            UserConfig.reloadPreferences()
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		// Makes sure that OnSharedPreferenceChangeListener works properly and avoids the situation where the app suddenly stops triggering the listener.
		preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
	}
	
	override fun onPause() {
		super.onPause()
		preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
	}
}