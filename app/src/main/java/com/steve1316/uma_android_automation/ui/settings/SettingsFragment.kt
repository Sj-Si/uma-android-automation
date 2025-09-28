package com.steve1316.uma_android_automation.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.R
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.UserConfig

class SettingsFragment : PreferenceFragmentCompat() {
	private val TAG: String = "SettingsFragment"
	
	private lateinit var sharedPreferences: SharedPreferences
	
	// This listener is triggered whenever the user changes a Preference setting in the Settings Page.
	private val onSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		if (key != null) {
			when (key) {
				"campaign" -> {
					val campaignListPreference = findPreference<ListPreference>("campaign")!!
					campaignListPreference.summary = "Selected: ${campaignListPreference.value}"
					sharedPreferences.edit {
						putString("campaign", campaignListPreference.value)
						commit()
					}
				}
				"strategy" -> {
					val strategyListPreference = findPreference<ListPreference>("strategy")!!
					strategyListPreference.summary = "Selected ${strategyListPreference.value}"
					sharedPreferences.edit {
						putString("strategy", strategyListPreference.value)
						commit()
					}
				}
				"bEnableFarmingFans" -> {
					val bEnableFarmingFansPreference = findPreference<CheckBoxPreference>("bEnableFarmingFans")!!
					val daysToRunExtraRacesPreference = findPreference<SeekBarPreference>("daysToRunExtraRaces")!!
					
					daysToRunExtraRacesPreference.isEnabled = bEnableFarmingFansPreference.isChecked
					
					sharedPreferences.edit {
						putBoolean("bEnableFarmingFans", bEnableFarmingFansPreference.isChecked)
						commit()
					}
				}
				"daysToRunExtraRaces" -> {
					val daysToRunExtraRacesPreference = findPreference<SeekBarPreference>("daysToRunExtraRaces")!!
					
					sharedPreferences.edit {
						putInt("daysToRunExtraRaces", daysToRunExtraRacesPreference.value)
						commit()
					}
				}
				"bEnableSkillPointCheck" -> {
					val bEnableSkillPointCheckPreference = findPreference<CheckBoxPreference>("bEnableSkillPointCheck")!!
					val skillPointCheckThresholdPreference = findPreference<SeekBarPreference>("skillPointCheckThreshold")!!
					skillPointCheckThresholdPreference.isEnabled = bEnableSkillPointCheckPreference.isChecked
					
					sharedPreferences.edit {
						putBoolean("bEnableSkillPointCheck", bEnableSkillPointCheckPreference.isChecked)
						commit()
					}
				}
				"skillPointCheckThreshold" -> {
					val skillPointCheckThresholdPreference = findPreference<SeekBarPreference>("skillPointCheckThreshold")!!
					
					sharedPreferences.edit {
						putInt("skillPointCheckThreshold", skillPointCheckThresholdPreference.value)
						commit()
					}
				}
				"bEnablePopupCheck" -> {
					val bEnablePopupCheckPreference = findPreference<CheckBoxPreference>("bEnablePopupCheck")!!
					
					sharedPreferences.edit {
						putBoolean("bEnablePopupCheck", bEnablePopupCheckPreference.isChecked)
						commit()
					}
				}
				"bEnableRaceRetries" -> {
					val bEnableRaceRetriesPreference = findPreference<CheckBoxPreference>("bEnableRaceRetries")!!

					sharedPreferences.edit {
						putBoolean("bEnableRaceRetries", bEnableRaceRetriesPreference.isChecked)
						commit()
					}
				}
				"bEnableStopOnMandatoryRace" -> {
					val bEnableStopOnMandatoryRacePreference = findPreference<CheckBoxPreference>("bEnableStopOnMandatoryRace")!!
					
					sharedPreferences.edit {
						putBoolean("bEnableStopOnMandatoryRace", bEnableStopOnMandatoryRacePreference.isChecked)
						commit()
					}
				}
				"bEnablePrioritizeEnergy" -> {
					val bEnablePrioritizeEnergyPreference = findPreference<CheckBoxPreference>("bEnablePrioritizeEnergy")!!

					sharedPreferences.edit {
						putBoolean("bEnablePrioritizeEnergy", bEnablePrioritizeEnergyPreference.isChecked)
						commit()
					}
				}
                "bEnableSkipCraneGame" -> {
                    val bEnableSkipCraneGamePreference = findPreference<CheckBoxPreference>("bEnableSkipCraneGame")!!

					sharedPreferences.edit {
						putBoolean("bEnableSkipCraneGame", bEnableSkipCraneGamePreference.isChecked)
						commit()
					}
                }
				"bEnableForceRacing" -> {
					val bEnableForceRacingPreference = findPreference<CheckBoxPreference>("bEnableForceRacing")!!

					sharedPreferences.edit {
						putBoolean("bEnableForceRacing", bEnableForceRacingPreference.isChecked)
						commit()
					}
				}
				"bEnableDebugMode" -> {
					val bEnableDebugModePreference = findPreference<CheckBoxPreference>("bEnableDebugMode")!!
					
					sharedPreferences.edit {
						putBoolean("bEnableDebugMode", bEnableDebugModePreference.isChecked)
						commit()
					}
				}
				"debugOcrConfidence" -> {
					val debugOcrConfidencePreference = findPreference<SeekBarPreference>("debugOcrConfidence")!!

					sharedPreferences.edit {
						putInt("debugOcrConfidence", debugOcrConfidencePreference.value)
						commit()
					}
				}
				"debugOcrScale" -> {
					val debugOcrScalePreference = findPreference<SeekBarPreference>("debugOcrScale")!!

					sharedPreferences.edit {
						putInt("debugOcrScale", debugOcrScalePreference.value)
						commit()
					}
				}
				"bRunTemplateMatchingTest" -> {
					val bRunTemplateMatchingTestPreference = findPreference<CheckBoxPreference>("bRunTemplateMatchingTest")!!

					sharedPreferences.edit {
						putBoolean("bRunTemplateMatchingTest", bRunTemplateMatchingTestPreference.isChecked)
						commit()
					}
				}
				"bRunSingleTrainingFailureOcrTest" -> {
					val bRunSingleTrainingFailureOcrTestPreference = findPreference<CheckBoxPreference>("bRunSingleTrainingFailureOcrTest")!!

					sharedPreferences.edit {
						putBoolean("bRunSingleTrainingFailureOcrTest", bRunSingleTrainingFailureOcrTestPreference.isChecked)
						commit()
					}
				}
				"bRunComprehensiveTrainingFailureOcrTest" -> {
					val bRunComprehensiveTrainingFailureOcrTestPreference = findPreference<CheckBoxPreference>("bRunComprehensiveTrainingFailureOcrTest")!!

					sharedPreferences.edit {
						putBoolean("bRunComprehensiveTrainingFailureOcrTest", bRunComprehensiveTrainingFailureOcrTestPreference.isChecked)
						commit()
					}
				}
				"bHideComparisonResults" -> {
					val bHideComparisonResultsPreference = findPreference<CheckBoxPreference>("bHideComparisonResults")!!
					
					sharedPreferences.edit {
						putBoolean("bHideComparisonResults", bHideComparisonResultsPreference.isChecked)
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
	
	// This function is called right after the user navigates to the SettingsFragment.
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		// Display the layout using the preferences xml.
		setPreferencesFromResource(R.xml.preferences, rootKey)
		
		// Get the SharedPreferences.
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		
		// Grab the saved preferences from the previous time the user used the app.
		val campaign: String = sharedPreferences.getString("campaign", "")!!
		val strategy: String = sharedPreferences.getString("strategy", "")!!
		val bEnableFarmingFans: Boolean = sharedPreferences.getBoolean("bEnableFarmingFans", false)
		val daysToRunExtraRaces: Int = sharedPreferences.getInt("daysToRunExtraRaces", 4)
		val bEnableSkillPointCheck: Boolean = sharedPreferences.getBoolean("bEnableSkillPointCheck", false)
		val skillPointCheckThreshold: Int = sharedPreferences.getInt("skillPointCheckThreshold", 750)
		val bEnablePopupCheck: Boolean = sharedPreferences.getBoolean("bEnablePopupCheck", false)
		val bEnableRaceRetries: Boolean = sharedPreferences.getBoolean("bEnableRaceRetries", true)
		val bEnableStopOnMandatoryRace: Boolean = sharedPreferences.getBoolean("bEnableStopOnMandatoryRace", false)
		val bEnablePrioritizeEnergy: Boolean = sharedPreferences.getBoolean("bEnablePrioritizeEnergy", false)
        val bEnableSkipCraneGame: Boolean = sharedPreferences.getBoolean("bEnableSkipCraneGame", false)
		val bEnableForceRacing: Boolean = sharedPreferences.getBoolean("bEnableForceRacing", false)
		val bEnableDebugMode: Boolean = sharedPreferences.getBoolean("bEnableDebugMode", false)
		val debugOcrConfidence: Int = sharedPreferences.getInt("debugOcrConfidence", 80)
		val debugOcrScale: Int = sharedPreferences.getInt("debugOcrScale", 100)
		val bRunTemplateMatchingTest: Boolean = sharedPreferences.getBoolean("bRunTemplateMatchingTest", false)
		val bRunSingleTrainingFailureOcrTest: Boolean = sharedPreferences.getBoolean("bRunSingleTrainingFailureOcrTest", false)
		val bRunComprehensiveTrainingFailureOcrTest: Boolean = sharedPreferences.getBoolean("bRunComprehensiveTrainingFailureOcrTest", false)
		val bHideComparisonResults: Boolean = sharedPreferences.getBoolean("bHideComparisonResults", true)
		
		// Get references to the Preference components.
		val campaignListPreference = findPreference<ListPreference>("campaign")!!
		val strategyListPreference = findPreference<ListPreference>("strategy")!!
		val bEnableFarmingFansPreference = findPreference<CheckBoxPreference>("bEnableFarmingFans")!!
		val daysToRunExtraRacesPreference = findPreference<SeekBarPreference>("daysToRunExtraRaces")!!
		val bEnableSkillPointCheckPreference = findPreference<CheckBoxPreference>("bEnableSkillPointCheck")!!
		val skillPointCheckThresholdPreference = findPreference<SeekBarPreference>("skillPointCheckThreshold")!!
		val bEnablePopupCheckPreference = findPreference<CheckBoxPreference>("bEnablePopupCheck")!!
		val bEnableRaceRetriesPreference = findPreference<CheckBoxPreference>("bEnableRaceRetries")!!
		val bEnableStopOnMandatoryRacePreference = findPreference<CheckBoxPreference>("bEnableStopOnMandatoryRace")!!
		val bEnablePrioritizeEnergyPreference = findPreference<CheckBoxPreference>("bEnablePrioritizeEnergy")!!
        val bEnableSkipCraneGamePreference = findPreference<CheckBoxPreference>("bEnableSkipCraneGame")!!
		val bEnableForceRacingPreference = findPreference<CheckBoxPreference>("bEnableForceRacing")!!
		val bEnableDebugModePreference = findPreference<CheckBoxPreference>("bEnableDebugMode")!!
		val debugOcrConfidencePreference = findPreference<SeekBarPreference>("debugOcrConfidence")!!
		val debugOcrScalePreference = findPreference<SeekBarPreference>("debugOcrScale")!!
		val bRunTemplateMatchingTestPreference = findPreference<CheckBoxPreference>("bRunTemplateMatchingTest")!!
		val bRunSingleTrainingFailureOcrTestPreference = findPreference<CheckBoxPreference>("bRunSingleTrainingFailureOcrTest")!!
		val bRunComprehensiveTrainingFailureOcrTestPreference = findPreference<CheckBoxPreference>("bRunComprehensiveTrainingFailureOcrTest")!!
		val bHideComparisonResultsPreference = findPreference<CheckBoxPreference>("bHideComparisonResults")!!
		
		// Now set the following values from the shared preferences.
		campaignListPreference.value = campaign
		if (campaign != "") {
			campaignListPreference.summary = "Selected: ${campaignListPreference.value}"
		}
		strategyListPreference.value = strategy
		if (strategy != "") {
			strategyListPreference.summary = "Selected: ${strategyListPreference.value}"
		}
		bEnableFarmingFansPreference.isChecked = bEnableFarmingFans
		daysToRunExtraRacesPreference.isEnabled = bEnableFarmingFansPreference.isChecked
		daysToRunExtraRacesPreference.value = daysToRunExtraRaces
		bEnableSkillPointCheckPreference.isChecked = bEnableSkillPointCheck
		skillPointCheckThresholdPreference.value = skillPointCheckThreshold
		bEnablePopupCheckPreference.isChecked = bEnablePopupCheck
		bEnableRaceRetriesPreference.isChecked = bEnableRaceRetries
		bEnableStopOnMandatoryRacePreference.isChecked = bEnableStopOnMandatoryRace
		bEnablePrioritizeEnergyPreference.isChecked = bEnablePrioritizeEnergy
        bEnableSkipCraneGamePreference.isChecked = bEnableSkipCraneGame
		bEnableForceRacingPreference.isChecked = bEnableForceRacing
		bEnableDebugModePreference.isChecked = bEnableDebugMode
		debugOcrConfidencePreference.value = debugOcrConfidence
		debugOcrScalePreference.value = debugOcrScale
		bRunTemplateMatchingTestPreference.isChecked = bRunTemplateMatchingTest
		bRunSingleTrainingFailureOcrTestPreference.isChecked = bRunSingleTrainingFailureOcrTest
		bRunComprehensiveTrainingFailureOcrTestPreference.isChecked = bRunComprehensiveTrainingFailureOcrTest
		bHideComparisonResultsPreference.isChecked = bHideComparisonResults
		skillPointCheckThresholdPreference.isEnabled = bEnableSkillPointCheckPreference.isChecked
		
		// Solution courtesy of https://stackoverflow.com/a/63368599
		// In short, Fragments via the mobile_navigation.xml are children of NavHostFragment, not MainActivity's supportFragmentManager.
		// This is why using the method described in official Google docs via OnPreferenceStartFragmentCallback and using the supportFragmentManager is not correct for this instance.
		findPreference<Preference>("trainingOptions")?.setOnPreferenceClickListener {
			// Navigate to the TrainingFragment.
			findNavController().navigate(R.id.nav_training)
			true
		}
		findPreference<Preference>("trainingEventOptions")?.setOnPreferenceClickListener {
			// Navigate to the TrainingEventFragment.
			findNavController().navigate(R.id.nav_training_event)
			true
		}
		findPreference<Preference>("ocrOptions")?.setOnPreferenceClickListener {
			// Navigate to the OCRFragment.
			findNavController().navigate(R.id.nav_ocr)
			true
		}
		
		MessageLog.d(TAG, "Main Preferences created successfully.")
	}
}