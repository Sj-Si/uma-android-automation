package com.steve1316.uma_android_automation.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.*
import androidx.navigation.fragment.findNavController
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.R
import com.steve1316.uma_android_automation.utils.MessageLog

class TrainingFragment : PreferenceFragmentCompat() {
	private val TAG: String = "[${MainActivity.loggerTag}]TrainingFragment"
	
	private lateinit var sharedPreferences: SharedPreferences
	
	private lateinit var builder: AlertDialog.Builder
	
	private lateinit var items: Array<String>
	private lateinit var checkedItems: BooleanArray
	private var userSelectedOptions: ArrayList<Int> = arrayListOf()
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		// Display the layout using the preferences xml.
		setPreferencesFromResource(R.xml.preferences_training, rootKey)
		
		// Get the SharedPreferences.
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		
		// Grab the saved preferences from the previous time the user used the app.
		val trainingBlacklist = sharedPreferences.getStringSet("trainingBlacklist", setOf())
		val maximumFailureChance = sharedPreferences.getInt("maximumFailureChance", 15)
		val disableTrainingOnMaxedStat = sharedPreferences.getBoolean("disableTrainingOnMaxedStat", true)
		val focusOnSparkStatTarget = sharedPreferences.getBoolean("focusOnSparkStatTarget", false)
		
		// Get references to the Preference components.
		val trainingBlacklistPreference = findPreference<MultiSelectListPreference>("trainingBlacklist")!!
		val maximumFailureChancePreference = findPreference<SeekBarPreference>("maximumFailureChance")!!
		val disableTrainingOnMaxedStatPreference = findPreference<CheckBoxPreference>("disableTrainingOnMaxedStat")!!
		val focusOnSparkStatTargetPreference = findPreference<CheckBoxPreference>("focusOnSparkStatTarget")!!
		
		// Now set the following values from the SharedPreferences.
		trainingBlacklistPreference.values = trainingBlacklist
		maximumFailureChancePreference.value = maximumFailureChance
		disableTrainingOnMaxedStatPreference.isChecked = disableTrainingOnMaxedStat
		focusOnSparkStatTargetPreference.isChecked = focusOnSparkStatTarget
		createMultiSelectAlertDialog()
		setupStatTargetPreferences()
		
		// Set this Preference listener to prevent users from blacklisting all 5 Trainings.
		trainingBlacklistPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any? ->
			if ((newValue as Set<*>).size != 5) {
				true
			} else {
				Toast.makeText(context, "Cannot blacklist all 5 Trainings!", Toast.LENGTH_SHORT).show()
				false
			}
		}
		
		// Update the summaries of the Preference components.
		updateSummaries()
		
		MessageLog.d(TAG, "Training Preferences created successfully.")
	}
	
	/**
	 * Setup click listeners for the stat target preferences to navigate to TrainingStatTargetFragment.
	 */
	private fun setupStatTargetPreferences() {
		// Sprint distance
		val sprintPreference = findPreference<Preference>("trainingSprintStatTarget")!!
		sprintPreference.setOnPreferenceClickListener {
			val bundle = Bundle().apply {
				putString("distanceType", "trainingSprintStatTarget")
				putString("distanceTitle", "Sprint")
			}
			findNavController().navigate(R.id.nav_training_stat_target, bundle)
			true
		}
		
		// Mile distance
		val milePreference = findPreference<Preference>("trainingMileStatTarget")!!
		milePreference.setOnPreferenceClickListener {
			val bundle = Bundle().apply {
				putString("distanceType", "trainingMileStatTarget")
				putString("distanceTitle", "Mile")
			}
			findNavController().navigate(R.id.nav_training_stat_target, bundle)
			true
		}
		
		// Medium distance
		val mediumPreference = findPreference<Preference>("trainingMediumStatTarget")!!
		mediumPreference.setOnPreferenceClickListener {
			val bundle = Bundle().apply {
				putString("distanceType", "trainingMediumStatTarget")
				putString("distanceTitle", "Medium")
			}
			findNavController().navigate(R.id.nav_training_stat_target, bundle)
			true
		}
		
		// Long distance
		val longPreference = findPreference<Preference>("trainingLongStatTarget")!!
		longPreference.setOnPreferenceClickListener {
			val bundle = Bundle().apply {
				putString("distanceType", "trainingLongStatTarget")
				putString("distanceTitle", "Long")
			}
			findNavController().navigate(R.id.nav_training_stat_target, bundle)
			true
		}
	}
	
	// This listener is triggered whenever the user changes a Preference setting in the Training Settings Page.
	private val onSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		if (key != null) {
			when (key) {
				"trainingBlacklist" -> {
					val trainingBlacklistPreference = findPreference<MultiSelectListPreference>("trainingBlacklist")!!
					
					sharedPreferences.edit {
						putStringSet("trainingBlacklist", trainingBlacklistPreference.values)
						commit()
					}
				}
				"maximumFailureChance" -> {
					val maximumFailureChancePreference = findPreference<SeekBarPreference>("maximumFailureChance")!!
					
					sharedPreferences.edit {
						putInt("maximumFailureChance", maximumFailureChancePreference.value)
						commit()
					}
				}
				"disableTrainingOnMaxedStat" -> {
					val disableTrainingOnMaxedStatPreference = findPreference<CheckBoxPreference>("disableTrainingOnMaxedStat")!!
					sharedPreferences.edit {
						putBoolean("disableTrainingOnMaxedStat", disableTrainingOnMaxedStatPreference.isChecked)
						commit()
					}
				}
				"focusOnSparkStatTarget" -> {
					val focusOnSparkStatTargetPreference = findPreference<CheckBoxPreference>("focusOnSparkStatTarget")!!
					sharedPreferences.edit {
						putBoolean("focusOnSparkStatTarget", focusOnSparkStatTargetPreference.isChecked)
						commit()
					}
				}
			}
			
			updateSummaries()
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		// Makes sure that OnSharedPreferenceChangeListener works properly and avoids the situation where the app suddenly stops triggering the listener.
		preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
		updateSummaries()
	}
	
	override fun onPause() {
		super.onPause()
		preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
	}
	
	/**
	 * Update the summaries of the Preference components on this page.
	 */
	private fun updateSummaries() {
		val trainingBlacklistPreference = findPreference<MultiSelectListPreference>("trainingBlacklist")!!
		trainingBlacklistPreference.summary = if (trainingBlacklistPreference.values.isNotEmpty()) {
			"Select Training(s) to blacklist from being selected in order to narrow the focus of overall Training. Note that the blacklist is ignored during Junior Year to focus on building bond levels.\n\nBlacklisted: ${trainingBlacklistPreference.values.joinToString(", ")}"
		} else {
			"Select Training(s) to blacklist from being selected in order to narrow the focus of overall Training. Note that the blacklist is ignored during Junior Year to focus on building bond levels.\n\nNone Selected"
		}
		
		val statPrioritizationPreference = findPreference<Preference>("statPrioritization")!!
		val statPrioritization: List<String> = sharedPreferences.getString("statPrioritization", "")!!.split("|")
		statPrioritizationPreference.summary = if (statPrioritization.isNotEmpty() && statPrioritization[0] != "") {
			var summaryBody = "Select Stat(s) to prioritize in order from highest priority to lowest. Any stat not selected will be assigned the lowest priority.\n\nOrder of Stat Prioritisation:"
			
			var count = 1
			statPrioritization.forEach { stat ->
				summaryBody += "\n$count. $stat"
				count++
			}
			
			summaryBody
		} else {
			"Select Stat(s) to prioritize in order from highest priority to lowest. Any stat not selected will be assigned the lowest priority.\n\nFollowing Default Prioritisation Order:\n1. " +
					"Speed\n2. Stamina\n3. Power\n4. Wit\n5. Guts"
		}
		
		// Update the stat target summaries for each distance type.
		// Sprint distance
		val sprintPreference = findPreference<Preference>("trainingSprintStatTarget")!!
		val sprintSpeed = sharedPreferences.getInt("trainingSprintStatTarget_speedStatTarget", 900)
		val sprintStamina = sharedPreferences.getInt("trainingSprintStatTarget_staminaStatTarget", 300)
		val sprintPower = sharedPreferences.getInt("trainingSprintStatTarget_powerStatTarget", 600)
		val sprintGuts = sharedPreferences.getInt("trainingSprintStatTarget_gutsStatTarget", 300)
		val sprintWit = sharedPreferences.getInt("trainingSprintStatTarget_witStatTarget", 300)
		sprintPreference.summary = "Set the stat targets for Sprint distance.\n\nCurrent Targets:\nSpeed: $sprintSpeed\nStamina: $sprintStamina\nPower: $sprintPower\nGuts: $sprintGuts\nWit: $sprintWit"

		// Mile distance
		val milePreference = findPreference<Preference>("trainingMileStatTarget")!!
		val mileSpeed = sharedPreferences.getInt("trainingMileStatTarget_speedStatTarget", 900)
		val mileStamina = sharedPreferences.getInt("trainingMileStatTarget_staminaStatTarget", 300)
		val milePower = sharedPreferences.getInt("trainingMileStatTarget_powerStatTarget", 600)
		val mileGuts = sharedPreferences.getInt("trainingMileStatTarget_gutsStatTarget", 300)
		val mileWit = sharedPreferences.getInt("trainingMileStatTarget_witStatTarget", 300)
		milePreference.summary = "Set the stat targets for Mile distance.\n\nCurrent Targets:\nSpeed: $mileSpeed\nStamina: $mileStamina\nPower: $milePower\nGuts: $mileGuts\nWit: $mileWit"

		// Medium distance
		val mediumPreference = findPreference<Preference>("trainingMediumStatTarget")!!
		val mediumSpeed = sharedPreferences.getInt("trainingMediumStatTarget_speedStatTarget", 800)
		val mediumStamina = sharedPreferences.getInt("trainingMediumStatTarget_staminaStatTarget", 450)
		val mediumPower = sharedPreferences.getInt("trainingMediumStatTarget_powerStatTarget", 550)
		val mediumGuts = sharedPreferences.getInt("trainingMediumStatTarget_gutsStatTarget", 300)
		val mediumWit = sharedPreferences.getInt("trainingMediumStatTarget_witStatTarget", 300)
		mediumPreference.summary = "Set the stat targets for Medium distance.\n\nCurrent Targets:\nSpeed: $mediumSpeed\nStamina: $mediumStamina\nPower: $mediumPower\nGuts: $mediumGuts\nWit: $mediumWit"

		// Long distance
		val longPreference = findPreference<Preference>("trainingLongStatTarget")!!
		val longSpeed = sharedPreferences.getInt("trainingLongStatTarget_speedStatTarget", 700)
		val longStamina = sharedPreferences.getInt("trainingLongStatTarget_staminaStatTarget", 600)
		val longPower = sharedPreferences.getInt("trainingLongStatTarget_powerStatTarget", 450)
		val longGuts = sharedPreferences.getInt("trainingLongStatTarget_gutsStatTarget", 300)
		val longWit = sharedPreferences.getInt("trainingLongStatTarget_witStatTarget", 300)
		longPreference.summary = "Set the stat targets for Long distance.\n\nCurrent Targets:\nSpeed: $longSpeed\nStamina: $longStamina\nPower: $longPower\nGuts: $longGuts\nWit: $longWit"
	}
	
	/**
	 * Builds and displays a AlertDialog for multi-selection that retains its order.
	 * This also serves the purpose of populating the Preference with previously selected values from SharedPreferences.
	 */
	private fun createMultiSelectAlertDialog() {
		val multiplePreference = findPreference<Preference>("statPrioritization")!!
		val key = "statPrioritization"
		val savedOptions: List<String> = sharedPreferences.getString("statPrioritization", "")!!.split("|")
		val selectedOptions: List<String> = sharedPreferences.getString("selectedOptions", "")!!.split("|")
		
		// Update the Preference's summary to reflect the order of options selected if the user did it before.
		updateSummaries()
		
		multiplePreference.setOnPreferenceClickListener {
			// Create the AlertDialog that pops up after clicking on this Preference.
			builder = AlertDialog.Builder(context)
			builder.setTitle("Select Option(s)")
			
			// Grab the Stats items array.
			items = resources.getStringArray(R.array.stats)
			
			// Populate the list for multiple options if this is the first time.
			if (savedOptions.isEmpty() || savedOptions[0] == "") {
				checkedItems = BooleanArray(items.size)
				var index = 0
				items.forEach { _ ->
					checkedItems[index] = false
					index++
				}
			} else {
				checkedItems = BooleanArray(items.size)
				var index = 0
				items.forEach {
					// Populate the checked items BooleanArray with true or false depending on what the user selected before.
					checkedItems[index] = savedOptions.contains(it)
					index++
				}
				
				// Repopulate the user selected options according to its order selected.
				userSelectedOptions.clear()
				selectedOptions.filter { it.isNotEmpty() }.forEach {
					userSelectedOptions.add(it.toInt())
				}
			}
			
			// Set the selectable items for this AlertDialog.
			builder.setMultiChoiceItems(items, checkedItems) { _, position, isChecked ->
				if (isChecked) {
					MessageLog.d(TAG, "Adding $position")
					userSelectedOptions.add(position)
				} else {
					MessageLog.d(TAG, "Removing $position")
					userSelectedOptions.remove(position)
				}
			}
			
			// Set the AlertDialog's PositiveButton.
			builder.setPositiveButton("OK") { _, _ ->
				// Grab the options using the acquired indexes. This will put them in order from the user's highest to lowest priority.
				val values: ArrayList<String> = arrayListOf()
				
				userSelectedOptions.forEach {
					values.add(items[it])
				}
				
				// Join the elements together into a String with the "|" delimiter in order to keep its order when storing into SharedPreferences.
				val newValues = values.joinToString("|")
				val newSelectedOptions = userSelectedOptions.joinToString("|")
				
				// Note: putStringSet does not support ordering or duplicate values. If you need ordering/duplicate values, either concatenate the values together as a String separated by a
				// delimiter or think of another way.
				sharedPreferences.edit {
					putString(key, newValues)
					putString("selectedOptions", newSelectedOptions)
					apply()
				}
				
				// Recreate the AlertDialog again to update it with the newly selected items.
				createMultiSelectAlertDialog()
				updateSummaries()
			}
			
			// Set the AlertDialog's NegativeButton.
			builder.setNegativeButton("Dismiss") { dialog, _ -> dialog?.dismiss() }
			
			// Set the behavior of the AlertDialog canceling.
			builder.setOnCancelListener {
				it.cancel()
				
				// Clear any newly selected options.
				userSelectedOptions.clear()
			}
			
			// Set the AlertDialog's NeutralButton.
			builder.setNeutralButton("Clear all") { _, _ ->
				// Go through every checked item and set them to false.
				for (i in checkedItems.indices) {
					checkedItems[i] = false
				}
				
				// After that, clear the list of user-selected options and the one in SharedPreferences.
				userSelectedOptions.clear()
				sharedPreferences.edit {
					remove(key)
					apply()
				}
				
				// Recreate the AlertDialog again to update it with the newly selected items and reset its summary.
				createMultiSelectAlertDialog()
				updateSummaries()
			}
			
			// Finally, show the AlertDialog to the user.
			builder.create().show()
			
			true
		}
	}
}