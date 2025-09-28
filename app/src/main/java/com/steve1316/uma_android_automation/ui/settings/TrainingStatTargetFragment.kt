package com.steve1316.uma_android_automation.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.*
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.R
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.UserConfig

class TrainingStatTargetFragment : PreferenceFragmentCompat() {
	private val TAG: String = "TrainingStatTargetFragment"
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var distanceType: String
	
	// This listener is triggered whenever the user changes a Preference setting in the Training Stat Targets Settings Page.
	private val onSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		if (key != null) {
			when (key) {
				"speedStatTarget" -> {
					val speedStatTargetPreference = findPreference<SeekBarPreference>("speedStatTarget")!!
					sharedPreferences.edit {
						putInt("${distanceType}_speedStatTarget", speedStatTargetPreference.value)
						commit()
					}
				}
				"staminaStatTarget" -> {
					val staminaStatTargetPreference = findPreference<SeekBarPreference>("staminaStatTarget")!!
					sharedPreferences.edit {
						putInt("${distanceType}_staminaStatTarget", staminaStatTargetPreference.value)
						commit()
					}
				}
				"powerStatTarget" -> {
					val powerStatTargetPreference = findPreference<SeekBarPreference>("powerStatTarget")!!
					sharedPreferences.edit {
						putInt("${distanceType}_powerStatTarget", powerStatTargetPreference.value)
						commit()
					}
				}
				"gutsStatTarget" -> {
					val gutsStatTargetPreference = findPreference<SeekBarPreference>("gutsStatTarget")!!
					sharedPreferences.edit {
						putInt("${distanceType}_gutsStatTarget", gutsStatTargetPreference.value)
						commit()
					}
				}
				"witStatTarget" -> {
					val witStatTargetPreference = findPreference<SeekBarPreference>("witStatTarget")!!
					sharedPreferences.edit {
						putInt("${distanceType}_witStatTarget", witStatTargetPreference.value)
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
		setPreferencesFromResource(R.xml.preferences_training_stat_target, rootKey)
		
		// Get the SharedPreferences.
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		
		// Get the distance type from arguments
		distanceType = arguments?.getString("distanceType") ?: "trainingSprintStatTarget"
		
		// Load the saved stat targets for this distance type
		loadStatTargets()
		
		// Setup preset buttons
		setupPresetButtons()
		
		MessageLog.d(TAG, "Training Stat Target Preferences created successfully for $distanceType.")
	}
	
	/**
	 * Load the saved stat targets for the current distance type.
	 */
	private fun loadStatTargets() {
		// Get references to the SeekBarPreference components
		val speedStatTargetPreference = findPreference<SeekBarPreference>("speedStatTarget")!!
		val staminaStatTargetPreference = findPreference<SeekBarPreference>("staminaStatTarget")!!
		val powerStatTargetPreference = findPreference<SeekBarPreference>("powerStatTarget")!!
		val gutsStatTargetPreference = findPreference<SeekBarPreference>("gutsStatTarget")!!
		val witStatTargetPreference = findPreference<SeekBarPreference>("witStatTarget")!!
		
		// Load saved values or use defaults based on distance type
		val (defaultSpeed, defaultStamina, defaultPower, defaultGuts, defaultWit) = getDefaultTargets()
		
		val savedSpeed = sharedPreferences.getInt("${distanceType}_speedStatTarget", defaultSpeed)
		val savedStamina = sharedPreferences.getInt("${distanceType}_staminaStatTarget", defaultStamina)
		val savedPower = sharedPreferences.getInt("${distanceType}_powerStatTarget", defaultPower)
		val savedGuts = sharedPreferences.getInt("${distanceType}_gutsStatTarget", defaultGuts)
		val savedWit = sharedPreferences.getInt("${distanceType}_witStatTarget", defaultWit)
		
		// Set the values
		speedStatTargetPreference.value = savedSpeed
		staminaStatTargetPreference.value = savedStamina
		powerStatTargetPreference.value = savedPower
		gutsStatTargetPreference.value = savedGuts
		witStatTargetPreference.value = savedWit
	}
	
	/**
	 * Get the default stat targets based on the distance type.
	 *
	 * @return The ArrayList of stat targets for training.
	 */
	private fun getDefaultTargets(): ArrayList<Int> {
		return when (distanceType) {
			"trainingSprintStatTarget" -> arrayListOf(900, 300, 600, 300, 300)
			"trainingMileStatTarget" -> arrayListOf(900, 300, 600, 300, 300)
			"trainingMediumStatTarget" -> arrayListOf(800, 450, 550, 300, 300)
			"trainingLongStatTarget" -> arrayListOf(700, 600, 450, 300, 300)
			else -> arrayListOf(900, 300, 600, 300, 300)
		}
	}
	
	/**
	 * Setup click listeners for preset buttons.
	 */
	private fun setupPresetButtons() {
		// Recommended preset button
		val recommendedPreset = findPreference<Preference>("presetRecommended")
		recommendedPreset?.setOnPreferenceClickListener {
			applyPreset(getRecommendedPreset(), "Recommended")
			true
		}
		
		// Balanced preset button
		val balancedPreset = findPreference<Preference>("presetBalanced")
		balancedPreset?.setOnPreferenceClickListener {
			applyPreset(getBalancedPreset(), "Balanced")
			true
		}
		
		// Speed focus preset button
		val speedPreset = findPreference<Preference>("presetSpeedFocus")
		speedPreset?.setOnPreferenceClickListener {
			applyPreset(getSpeedFocusPreset(), "Speed Focus")
			true
		}
		
		// Stamina focus preset button
		val staminaPreset = findPreference<Preference>("presetStaminaFocus")
		staminaPreset?.setOnPreferenceClickListener {
			applyPreset(getStaminaFocusPreset(), "Stamina Focus")
			true
		}
		
		// Reset to defaults button
		val resetDefaults = findPreference<Preference>("presetDefaults")
		resetDefaults?.setOnPreferenceClickListener {
			applyPreset(getDefaultTargets(), "Default")
			true
		}
	}
	
	/**
	 * Apply a preset configuration to all stat targets.
	 */
	private fun applyPreset(values: ArrayList<Int>, presetName: String) {
		// Show confirmation dialog
		AlertDialog.Builder(context)
			.setTitle("Apply $presetName Preset")
			.setMessage("This will override your current stat targets:\n" +
				"Speed: ${values[0]}\n" +
				"Stamina: ${values[1]}\n" +
				"Power: ${values[2]}\n" +
				"Guts: ${values[3]}\n" +
				"Wit: ${values[4]}\n\n" +
				"Continue?")
			.setPositiveButton("Apply") { _, _ ->
				// Update all the SeekBarPreferences
				findPreference<SeekBarPreference>("speedStatTarget")?.value = values[0]
				findPreference<SeekBarPreference>("staminaStatTarget")?.value = values[1]
				findPreference<SeekBarPreference>("powerStatTarget")?.value = values[2]
				findPreference<SeekBarPreference>("gutsStatTarget")?.value = values[3]
				findPreference<SeekBarPreference>("witStatTarget")?.value = values[4]
				
				// Save to SharedPreferences
				sharedPreferences.edit {
					putInt("${distanceType}_speedStatTarget", values[0])
					putInt("${distanceType}_staminaStatTarget", values[1])
					putInt("${distanceType}_powerStatTarget", values[2])
					putInt("${distanceType}_gutsStatTarget", values[3])
					putInt("${distanceType}_witStatTarget", values[4])
					apply()
				}
				
				MessageLog.d(TAG, "Applied $presetName preset for $distanceType")
			}
			.setNegativeButton("Cancel", null)
			.show()
	}
	
	/**
	 * Get recommended preset values based on the training guide.
	 */
	private fun getRecommendedPreset(): ArrayList<Int> {
		return when (distanceType) {
			"trainingSprintStatTarget" -> arrayListOf(1200, 350, 850, 350, 600)
			"trainingMileStatTarget" -> arrayListOf(1050, 450, 750, 450, 600)
			"trainingMediumStatTarget" -> arrayListOf(950, 650, 650, 450, 600)
			"trainingLongStatTarget" -> arrayListOf(750, 950, 550, 550, 600)
			else -> arrayListOf(900, 600, 600, 400, 600)
		}
	}
	
	/**
	 * Get balanced preset values.
	 */
	private fun getBalancedPreset(): ArrayList<Int> {
		return when (distanceType) {
			"trainingSprintStatTarget" -> arrayListOf(800, 400, 700, 400, 500)
			"trainingMileStatTarget" -> arrayListOf(800, 500, 700, 400, 500)
			"trainingMediumStatTarget" -> arrayListOf(700, 600, 600, 500, 500)
			"trainingLongStatTarget" -> arrayListOf(600, 700, 600, 500, 500)
			else -> arrayListOf(700, 600, 600, 400, 400)
		}
	}
	
	/**
	 * Get speed-focused preset values.
	 */
	private fun getSpeedFocusPreset(): ArrayList<Int> {
		return when (distanceType) {
			"trainingSprintStatTarget" -> arrayListOf(1200, 300, 700, 300, 400)
			"trainingMileStatTarget" -> arrayListOf(1100, 400, 600, 300, 400)
			"trainingMediumStatTarget" -> arrayListOf(1000, 500, 500, 300, 400)
			"trainingLongStatTarget" -> arrayListOf(900, 600, 400, 300, 400)
			else -> arrayListOf(1000, 400, 500, 300, 400)
		}
	}
	
	/**
	 * Get stamina-focused preset values.
	 */
	private fun getStaminaFocusPreset(): ArrayList<Int> {
		return when (distanceType) {
			"trainingSprintStatTarget" -> arrayListOf(700, 400, 800, 400, 500)
			"trainingMileStatTarget" -> arrayListOf(700, 600, 700, 400, 500)
			"trainingMediumStatTarget" -> arrayListOf(600, 800, 600, 400, 500)
			"trainingLongStatTarget" -> arrayListOf(600, 1000, 500, 500, 500)
			else -> arrayListOf(600, 700, 600, 400, 400)
		}
	}
}