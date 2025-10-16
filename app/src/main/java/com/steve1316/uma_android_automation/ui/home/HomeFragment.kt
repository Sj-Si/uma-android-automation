package com.steve1316.uma_android_automation.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.beust.klaxon.JsonReader
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.R
import com.steve1316.uma_android_automation.data.CharacterData
import com.steve1316.uma_android_automation.data.RaceData
import com.steve1316.uma_android_automation.data.SkillData
import com.steve1316.uma_android_automation.data.SupportData
import com.steve1316.uma_android_automation.utils.MediaProjectionService
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.MyAccessibilityService
import java.io.StringReader
import androidx.core.net.toUri
import com.steve1316.uma_android_automation.utils.UserConfig

import com.steve1316.uma_android_automation.utils.types.RaceInfo


class HomeFragment : Fragment() {
	private val TAG: String = "HomeFragment"
	private var firstBoot = false
	private var firstRun = true
	
	private lateinit var myContext: Context
	private lateinit var homeFragmentView: View
	private lateinit var startButton: Button
	
	private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		myContext = requireContext()
		
		// Initialize the ActivityResultLauncher
		mediaProjectionLauncher = registerForActivityResult(
			ActivityResultContracts.StartActivityForResult()
		) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				// Start up the MediaProjection service after the user accepts the onscreen prompt.
				result.data?.let { data ->
					myContext.startService(MediaProjectionService.getStartIntent(myContext, result.resultCode, data))
				}
			}
		}
	}
	
	@SuppressLint("SetTextI18n")
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		myContext = requireContext()
		
		homeFragmentView = inflater.inflate(R.layout.fragment_home, container, false)
		
		// Start or stop the MediaProjection service via this button.
		startButton = homeFragmentView.findViewById(R.id.start_button)
		startButton.setOnClickListener {
			val readyCheck = startReadyCheck()
			if (readyCheck && !MediaProjectionService.isRunning) {
				startProjection()
				startButton.text = getString(R.string.stop)
				
				// This is needed because onResume() is immediately called right after accepting the MediaProjection and it has not been properly
				// initialized yet so it would cause the button's text to revert back to "Start".
				firstBoot = true
			} else if (MediaProjectionService.isRunning) {
				stopProjection()
				startButton.text = getString(R.string.start)
			}
		}


        // Set default values in SharedPreferences just in case these keys do not exist yet.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		
		sharedPreferences.edit {
			// Set the default stat prioritization order if it does not exist.
			putString("statPriority", UserConfig.config.training.statPriority.joinToString("|"))

			// Set default stat targets for each distance type if they do not exist.
            listOf("Sprint", "Mile", "Medium", "Long").forEach { distance ->
                listOf("speed", "stamina", "power", "guts", "wit").forEach { stat ->
                    putInt(
                        "training${distance}StatTarget_${stat}StatTarget",
                        UserConfig.config.training.getTrainingStatTargets(distance).get(stat),
					)
                }
            }

			commit()
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Update the TextView here based on the information in the UserConfig.

		// Add visual indicators for character and support card selections
		val characterString: String = if (UserConfig.config.events.bSelectAllCharacters) {
			"👥 All Characters Selected"
		} else if (
            UserConfig.config.events.selectedCharacter == "" ||
            UserConfig.config.events.selectedCharacter.contains("Please select")
        ) {
			"⚠️ Please select one in the Training Event Settings"
		} else {
			"👤 ${UserConfig.config.events.selectedCharacter}"
		}
		
		// Add visual indicator for campaign selection
		val campaignString: String = if (UserConfig.config.campaign != "") {
			"🎯 ${UserConfig.config.campaign}"
		} else {
			"⚠️ Please select one in the Select Campaign option"
		}

        val strategyString: String = if (UserConfig.config.strategy != "") {
			"🎯 ${UserConfig.config.strategy}"
		} else {
			"⚠️ Please select one in the Select Race Strategy option"
		}
		
		val settingsStatusTextView: TextView = homeFragmentView.findViewById(R.id.settings_status)
		settingsStatusTextView.text = UserConfig.getConfigString()
		
		// Now construct the data files if this is the first time.
		if (firstRun) {
			constructDataClasses()
			firstRun = false
		}
		
		// Force the user to go through the Settings in order to set this required setting.
		startButton.isEnabled = !campaignString.contains("Please select") && !characterString.contains("Please select")

		return homeFragmentView
	}
	
	override fun onResume() {
		super.onResume()
		
		// Update the button's text depending on if the MediaProjection service is running.
		if (!firstBoot) {
			if (MediaProjectionService.isRunning) {
				startButton.text = getString(R.string.stop)
			} else {
				startButton.text = getString(R.string.start)
			}
		}
		
		// Setting this false here will ensure that stopping the MediaProjection Service outside of this application will update this button's text.
		firstBoot = false
		
		// Now update the Message Log inside the ScrollView with the latest logging messages from the bot.
		MessageLog.d(TAG, "Now updating the Message Log TextView...")
		val messageLogTextView = homeFragmentView.findViewById<TextView>(R.id.message_log)
		messageLogTextView.text = ""

		// Get a thread-safe copy of the message log.
		val messageLog: List<String> = MessageLog.getMessageLogCopy()

        var filteredLog: List<String> = messageLog.filter {
            val logLevel = if (it.contains("[DEBUG]")) {
                0
            } else if (it.contains("[INFO]")) {
                1
            } else if (it.contains("[WARN]")) {
                2
            } else if (it.contains("[ERROR]")) {
                3
            } else {
                0
            }

            logLevel >= UserConfig.config.logLevelIndex
        }

        if (filteredLog.size > UserConfig.config.maxLogLines) {
            messageLogTextView.append("\n${filteredLog.size - UserConfig.config.maxLogLines} lines omitted.")
            messageLogTextView.append("\nPlease see the main log file for entire log contents.")
        }
		filteredLog.takeLast(UserConfig.config.maxLogLines).forEach { message ->
			messageLogTextView.append("\n$message")
		}
		
		// Set up the app updater to check for the latest update from GitHub.
		AppUpdater(myContext)
			.setUpdateFrom(UpdateFrom.XML)
			.setUpdateXML("https://raw.githubusercontent.com/steve1316/uma-android-automation/master/app/update.xml")
			.start()
	}

	
	/**
	 * Checks to see if the application is ready to start.
	 *
	 * @return True if the application has overlay permission and has enabled the Accessibility Service for it. Otherwise, return False.
	 */
	private fun startReadyCheck(): Boolean {
		return !(!checkForOverlayPermission() || !checkForAccessibilityPermission())
	}
	
	/**
	 * Starts the MediaProjection Service.
	 */
	private fun startProjection() {
		val mediaProjectionManager = context?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
		mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
	}
	
	/**
	 * Stops the MediaProjection Service.
	 */
	private fun stopProjection() {
		context?.startService(MediaProjectionService.getStopIntent(requireContext()))
	}
	
	/**
	 * Checks if the application has permission to draw overlays. If not, it will direct the user to enable it.
	 *
	 * Source is from https://github.com/Fate-Grand-Automata/FGA/blob/master/app/src/main/java/com/mathewsachin/fategrandautomata/ui/MainFragment.kt
	 *
	 * @return True if it has permission. False otherwise.
	 */
	private fun checkForOverlayPermission(): Boolean {
		if (!Settings.canDrawOverlays(requireContext())) {
			MessageLog.e(TAG, "Application is missing overlay permission.")
			
			AlertDialog.Builder(requireContext()).apply {
				setTitle(R.string.overlay_disabled)
				setMessage(R.string.overlay_disabled_message)
				setPositiveButton(R.string.go_to_settings) { _, _ ->
					// Send the user to the Overlay Settings.
					val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${requireContext().packageName}".toUri())
					startActivity(intent)
				}
				setNegativeButton(android.R.string.cancel, null)
			}.show()
			
			return false
		}
		
		MessageLog.d(TAG, "Application has permission to draw overlay.")
		return true
	}
	
	/**
	 * Checks if the Accessibility Service for this application is enabled. If not, it will direct the user to enable it.
	 *
	 * Source is from https://stackoverflow.com/questions/18094982/detect-if-my-accessibility-service-is-enabled/18095283#18095283
	 *
	 * @return True if it is enabled. False otherwise.
	 */
	private fun checkForAccessibilityPermission(): Boolean {
		val prefString = Settings.Secure.getString(myContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
		
		if (prefString != null && prefString.isNotEmpty()) {
			// Check the string of enabled accessibility services to see if this application's accessibility service is there.
			val enabled = prefString.contains(myContext.packageName.toString() + "/" + MyAccessibilityService::class.java.name)
			
			if (enabled) {
				MessageLog.d(TAG, "This application's Accessibility Service is currently turned on.")
				return true
			}
		}

		// Shows a dialog explaining how to enable Accessibility Service when restricted settings are detected.
		// The dialog provides options to navigate to App Info or Accessibility Settings to complete the setup.
		AlertDialog.Builder(myContext).apply {
			setTitle(R.string.accessibility_disabled)
			setMessage(
				"""
            To enable Accessibility Service:
            
            1. Tap "Go to App Info".
            2. Tap the 3-dot menu in the top right. If not available, you can skip to step 4.
            3. Tap "Allow restricted settings".
            4. Return to Accessibility Settings and enable the service.
            """.trimIndent()
			)
			setPositiveButton("Go to App Info") { _, _ ->
				val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
					data = "package:${myContext.packageName}".toUri()
				}
				startActivity(intent)
			}
			setNeutralButton("Accessibility Settings") { _, _ ->
				val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
				startActivity(intent)
			}
			setNegativeButton(android.R.string.cancel, null)
		}.show()

		return false
	}
	
	/**
	 * Construct the data classes associated with Characters, Support Cards and Skills from the provided JSON data files.
	 */
	private fun constructDataClasses() {
		// Construct the data class for Characters and Support Cards.
		val fileList = arrayListOf("characters.json", "supports.json")
		while (fileList.isNotEmpty()) {
			val fileName = fileList[0]
			fileList.removeAt(0)
			val objectString = myContext.assets.open("data/$fileName").bufferedReader().use { it.readText() }
			
			JsonReader(StringReader(objectString)).use { reader ->
				reader.beginObject {
					while (reader.hasNext()) {
						// Grab the name.
						val name = reader.nextName()
						
						// Now iterate through each event and collect all of them and their option rewards into a map.
						val eventOptionRewards = mutableMapOf<String, ArrayList<String>>()
						reader.beginObject {
							while (reader.hasNext()) {
								// Grab the event name.
								val eventName = reader.nextName()
								eventOptionRewards.putIfAbsent(eventName, arrayListOf())
								
								reader.beginArray {
									// Grab all of the event option rewards for this event and add them to the map.
									while (reader.hasNext()) {
										val optionReward = reader.nextString()
										eventOptionRewards[eventName]?.add(optionReward)
									}
								}
							}
						}
						
						// Finally, put into the MutableMap the key value pair depending on the current category.
						if (fileName == "characters.json") {
							CharacterData.characters[name] = eventOptionRewards
						} else {
							SupportData.supports[name] = eventOptionRewards
						}
					}
				}
			}
		}
		
		// Now construct the data class for Skills.
		val objectString = myContext.assets.open("data/skills.json").bufferedReader().use { it.readText() }
		JsonReader(StringReader(objectString)).use { reader ->
			reader.beginObject {
				while (reader.hasNext()) {
					// Grab the name.
					val skillName = reader.nextName()
					SkillData.skills.putIfAbsent(skillName, mutableMapOf())
					
					reader.beginObject {
						// Skip the id.
						reader.nextName()
						reader.nextInt()
						
						// Grab the English name and description.
						reader.nextName()
						val skillEnglishName = reader.nextString()
						reader.nextName()
						val skillEnglishDescription = reader.nextString()
						
						// Finally, collect them into a map and put them into the data class.
						val tempMap = mutableMapOf<String, String>()
						tempMap["englishName"] = skillEnglishName
						tempMap["englishDescription"] = skillEnglishDescription
						SkillData.skills[skillName] = tempMap
					}
				}
			}
		}

        // Now construct data class for Races
        val racesObjectString = myContext.assets.open("data/races.json").bufferedReader().use { it.readText() }
        JsonReader(StringReader(racesObjectString)).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val day: String = reader.nextName()
                    MessageLog.i("HERE", "$day")
                    val raceList = mutableMapOf<String, RaceInfo>()
                    reader.beginArray {
                        while(reader.hasNext()) {
                            reader.beginObject {
                                var name: String = ""
                                var grade: String = ""
                                var year: String = ""
                                var month: String = ""
                                var phase: String = ""
                                var trackName: String = ""
                                var trackSurface: String = ""
                                var trackDistance: String = ""
                                var lengthMeters: String = ""
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "name" -> name = reader.nextString()
                                        "grade" -> grade = reader.nextString()
                                        "year" -> year = reader.nextString()
                                        "month" -> month = reader.nextString()
                                        "phase" -> phase = reader.nextString()
                                        "trackName" -> trackName = reader.nextString()
                                        "trackSurface" -> trackSurface = reader.nextString()
                                        "trackDistance" -> trackDistance = reader.nextString()
                                        "lengthMeters" -> lengthMeters = reader.nextString()
                                    }
                                }
                                var raceInfo: RaceInfo = RaceInfo(
                                    name,
                                    grade,
                                    year,
                                    month,
                                    phase,
                                    trackName,
                                    trackSurface,
                                    trackDistance,
                                    lengthMeters,
                                )
								raceList.put(name, raceInfo)
                            }
                        }
                    }
                    RaceData.races[day.toIntOrNull() ?: -1] = raceList
                }
            }
        }

		/* Print out the RaceData for debugging purposes.
        for ((date, raceList) in RaceData.races) {
			for ((raceName, raceInfo) in raceList) {
                MessageLog.d(TAG, "[RaceData] [${date}] [${raceName}] ${raceInfo}")
            }
        }
        */
	}
}
