package com.steve1316.uma_android_automation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.START_TIME_MS
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * Returns a formatted string of the elapsed time since the bot started as HH:MM:SS format.
 *
 * Source is from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
 *
 * @return String of HH:MM:SS format of the elapsed time.
 */
@SuppressLint("DefaultLocale")
fun printElapsedTime(): String {
	val elapsedMillis: Long = System.currentTimeMillis() - START_TIME_MS

	return String.format(
		"%02d:%02d:%02d",
		TimeUnit.MILLISECONDS.toHours(elapsedMillis),
		TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedMillis)),
		TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMillis))
	)
}

/**
 * Returns a formatted string of the current system time as HH:MM:SS format.
 *
 * Source is from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
 *
 * @return String of HH:MM:SS formatted time.
 */
@SuppressLint("DefaultLocale")
fun printSystemTime(): String {
	val timeMs: Long = System.currentTimeMillis()

	return String.format(
		"%02d:%02d:%02d",
		TimeUnit.MILLISECONDS.toHours(timeMs),
		TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeMs)),
		TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMs))
	)
}

/**
 * This class is in charge of holding the Message Log to which all logging messages from the bot goes to and also saves it all into a file when the bot has finished.
 */
class MessageLog {
	companion object {
		private const val TAG: String = "MessageLog"
		var messageLog = arrayListOf<String>()
		
		// Add synchronization object for thread-safe access
		private val messageLogLock = Object()
		
		var saveCheck = false
		
		/**
		 * Save the current Message Log into a new file inside internal storage's /logs/ folder.
		 *
		 * @param context The context for the application.
		 */
		fun saveLogToFile(context: Context) {
			cleanLogsFolder(context)
			
			if (!saveCheck) {
				Log.d(TAG, "Now beginning process to save current Message Log to internal storage...")
				
				// Generate file path to save to. All message logs will be saved to the /logs/ folder inside internal storage. Create the /logs/ folder if needed.
				val path = File(context.getExternalFilesDir(null)?.absolutePath + "/logs/")
				if (!path.exists()) {
					path.mkdirs()
				}
				
				// Generate the file name.
				val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					val current = LocalDateTime.now()
					val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
					"log @ ${current.format(formatter)}"
				} else {
					val current = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
					val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
					"log @ ${current.format(sdf)}"
				}
				
				// Now save the Message Log to the new text file.
				Log.d(TAG, "Now saving Message Log to file named \"$fileName\" at $path")
				val file = File(path, "$fileName.txt")
				
				if (!file.exists()) {
					file.createNewFile()
					file.printWriter().use { out ->
						// Synchronize access to messageLog to prevent concurrent modification
						synchronized(messageLogLock) {
							messageLog.forEach {
								out.println(it)
							}
						}
					}
					
					saveCheck = true
				}
			}
		}
		
		/**
		 * Add a message to the log in a thread-safe manner.
		 *
		 * @param message The message to add to the log.
		 */
		fun addMessage(message: String) {
			synchronized(messageLogLock) {
				messageLog.add(message)
			}
		}
		
		/**
		 * Clear the message log in a thread-safe manner.
		 */
		fun clearLog() {
			synchronized(messageLogLock) {
				messageLog.clear()
			}
		}
		
		/**
		 * Get a copy of the current message log in a thread-safe manner.
		 *
		 * @return A copy of the current message log.
		 */
		fun getMessageLogCopy(): List<String> {
			synchronized(messageLogLock) {
				return ArrayList(messageLog)
			}
		}
		
		/**
		 * Clean up the logs folder if the amount of logs inside is greater than the specified amount.
		 *
		 * @param context The context for the application.
		 */
		private fun cleanLogsFolder(context: Context) {
			val directory = File(context.getExternalFilesDir(null)?.absolutePath + "/logs/")
			
			// Delete all logs if the amount inside is greater than 50.
			val files = directory.listFiles()
			if (files != null && files.size > 50) {
				files.forEach { file ->
					file.delete()
				}
			}
		}

		/**
		 * Print the specified message to debug console and then saves the message to the log.
		 *
		 * @param message Message to be saved.
		 * @param tag Distinguishes between messages for where they came from. Defaults to Game's TAG.
         * @param level The log level of the message. String added to beginning of message in brackets.
		 * @param isOption Flag to determine whether to append a newline right after the time in the string.
		 */
		fun log(tag: String = TAG, message: String, level: LogLevel = LogLevel.DEBUG, isOption: Boolean = false) {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARN -> Log.w(tag, message)
                LogLevel.ERROR -> Log.e(tag, message)
            }

            var msg = message.removePrefix("\n")
            msg = msg.trimEnd()
            var prefix = "[${level}]"
            if (tag != "") {
                prefix += " [${MainActivity.TAG}::${tag}]"
            }
            msg = "${prefix} ${msg}"

            if (isOption) {
                msg = "${printElapsedTime()}\n${msg}"
            } else {
                msg = "${printElapsedTime()} ${msg}"
            }

            addMessage(msg)
		}

        // Wrappers arouns the log() function.
        fun d(tag: String = TAG, message: String, isOption: Boolean = false) {
            log(tag, message, LogLevel.DEBUG, isOption)
        }

        fun i(tag: String = TAG, message: String, isOption: Boolean = false) {
            log(tag, message, LogLevel.INFO, isOption)
        }

        fun w(tag: String = TAG, message: String, isOption: Boolean = false) {
            log(tag, message, LogLevel.WARN, isOption)
        }

        fun e(tag: String = TAG, message: String, isOption: Boolean = false) {
            log(tag, message, LogLevel.ERROR, isOption)
        }
	}
}