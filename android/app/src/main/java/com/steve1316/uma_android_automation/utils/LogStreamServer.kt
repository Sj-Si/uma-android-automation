package com.steve1316.uma_android_automation.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.events.JSEvent
import com.steve1316.automation_library.utils.MessageLog
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.regex.Pattern

/**
 * Embedded WebSocket server that streams MessageLog entries in real-time
 * to any browser on the local network. Built on Ktor Server CIO.
 *
 * When running, the server serves:
 * - HTTP GET "/" → the log viewer HTML page (from assets).
 * - WebSocket "/" → real-time log message streaming.
 */
object LogStreamServer {
	private const val TAG: String = "${SharedData.loggerTag}LogStreamServer"

	private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

	// Coroutine scope for the server and background tasks.
	private var serverScope: CoroutineScope? = null

	@Volatile
	var isRunning = false
		private set

	// Mute flag to stop broadcasting logs after a run concludes.
	@Volatile
	private var isMuted = false

	// Thread-safe set of currently active WebSocket client sessions.
	private val clients = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

	// Circular buffer for storing the most recent log messages.
	private val messageBuffer = ArrayList<String>()

	// Synchronization lock for thread-safe access to the message buffer.
	private val bufferLock = Object()

	// Maximum number of messages to retain in the history buffer.
	private val maxBufferSize = 15000

	// Pattern for matching logs: "00:12:34.567 [DEBUG] message content".
	private val logPattern = Pattern.compile("^(\\n?)([\\d]{2}:[\\d]{2}:[\\d]{2}\\.[\\d]{3})\\s*\\[(VERBOSE|DEBUG|INFO|WARN|ERROR)\\]\\s*(.*)", Pattern.DOTALL)

	// Regex patterns for structured detail extraction.
	private val actionTrainingPattern = Pattern.compile("\\[TRAINING\\] Now starting process to execute training")
	private val actionRacePattern = Pattern.compile("\\[RACE\\] Racing process for .*? Race is completed")
	private val actionMoodPattern = Pattern.compile("Recovering mood now", Pattern.CASE_INSENSITIVE)
	private val actionEnergyPattern = Pattern.compile("\\[ENERGY\\] Successfully recovered energy")
	private val actionInjuryPattern = Pattern.compile("\\[INJURY\\] Injury detected and attempted to heal")
	private val traineeDetailedPattern = Pattern.compile("\\[TRAINEE_DETAILED\\] ([^:]+): (.*)")
	private val dateNewPattern = Pattern.compile("\\[DATE\\] New date: (.*?) \\(Turn (\\d+)\\)", Pattern.CASE_INSENSITIVE)

	/**
	 * Represents a parsed log entry for structured transmission.
	 *
	 * @property newline Leading newline if present.
	 * @property timestamp Extracted HH:mm:ss.SSS timestamp.
	 * @property level Log level (DEBUG, INFO, WARN, ERROR, VERBOSE).
	 * @property text The actual log message content.
	 * @property action Identified bot action (training, race, injury, mood).
	 * @property trainee Trainee detailed info (category and raw data).
	 * @property dateInfo Extracted date and turn information.
	 */
	private data class LogEntry(
		val newline: String,
		val timestamp: String,
		val level: String,
		val text: String,
		val action: String? = null,
		val trainee: JSONObject? = null,
		val dateInfo: JSONObject? = null
	) {
		/**
		 * Converts the log entry into a JSON object for WebSocket transmission.
		 */
		fun toJSON(): JSONObject {
			return JSONObject().apply {
				put("newline", newline)
				put("timestamp", timestamp)
				put("level", level)
				put("message", text)
				action?.let { put("action", it) }
				trainee?.let { put("trainee", it) }
				dateInfo?.let { put("dateInfo", it) }
			}
		}
	}

	// Sealed class representing different log actions to ensure sequential processing.
	private sealed class LogAction {
		data class NewClient(val session: DefaultWebSocketServerSession) : LogAction()
		data class Broadcast(val message: String) : LogAction()
		object Clear : LogAction()
	}

	// Channel for serializing log actions.
	private var actionChannel: Channel<LogAction>? = null

	/**
	 * Resets the mute flag and clears the buffer to allow log broadcasting for a new run.
	 */
	fun resetMute() {
		Log.i(TAG, "Log stream mute reset requested.")
		serverScope?.launch {
			actionChannel?.send(LogAction.Clear)
		}
	}

	/**
	 * Starts the log streaming server on the specified port and registers with EventBus.
	 *
	 * @param context The application context used for accessing assets and system services.
	 * @param port The network port number to listen on.
	 */
	fun start(context: Context, port: Int) {
		// Prevent starting multiple instances of the server.
		if (isRunning) {
			Log.w(TAG, "Server is already running.")
			return
		}

		try {
			serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
			actionChannel = Channel(Channel.UNLIMITED)

			// Start the core log worker that serializes all history syncs and live broadcasts.
			serverScope?.launch {
				actionChannel?.let { channel ->
					for (action in channel) {
						when (action) {
							is LogAction.NewClient -> {
								handleNewClientAction(action.session)
							}
							is LogAction.Broadcast -> {
								handleBroadcastAction(action.message)
							}
							is LogAction.Clear -> {
								handleClearAction()
							}
						}
					}
				}
			}

			// Bind to all interfaces so devices on the local network can connect.
			server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
				// Install the WebSockets plugin with default configuration.
				install(WebSockets)

				routing {
					// Serve the main log viewer HTML application.
					get("/") {
						serveLogViewerHtml(call, context)
					}
					get("/index.html") {
						serveLogViewerHtml(call, context)
					}

					// Provide a health check endpoint for monitoring the server status.
					get("/health") {
						call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
					}

					// Serve the full message log for download.
					get("/logs/download") {
						try {
							val fullLogs = MessageLog.getMessageLogCopy().joinToString("\n")

							// Set headers to trigger a file download in the browser.
							val datePart = java.text.SimpleDateFormat(
								"yyyy-MM-dd-HH-mm-ss",
								java.util.Locale.getDefault()
							).format(java.util.Date())

							call.response.header(
								HttpHeaders.ContentDisposition,
								"attachment; filename=\"uaa_logs_$datePart.txt\""
							)
							call.respondText(fullLogs, ContentType.Text.Plain)
						} catch (e: Exception) {
							Log.e(TAG, "Failed to generate log download: ${e.message}")
							call.respondText(
								"Failed to generate log download.",
								ContentType.Text.Plain,
								HttpStatusCode.InternalServerError
							)
						}
					}

					// Handle WebSocket connections on root path (matches the HTML client).
					webSocket("/") {
						handleWebSocketSession(this)
					}
				}
			}

			// Start the server without blocking the calling thread.
			server?.start(wait = false)
			isRunning = true

			// Register with EventBus to receive real-time log messages.
			if (!EventBus.getDefault().isRegistered(this)) {
				EventBus.getDefault().register(this)
			}

			// Determine and log the device IP address for easy access.
			val ip = getDeviceIpAddress(context)
			Log.i(TAG, "Log stream server started on http://$ip:$port")

			// Populate the initial buffer from existing logs so late-joining clients see the history.
			populateBuffer(MessageLog.getMessageLogCopy())

			Log.d(TAG, "LogStreamServer registered with EventBus: ${EventBus.getDefault().isRegistered(this)}")
			MessageLog.i(TAG, "Remote Log Viewer started at http://$ip:$port")
		} catch (e: Exception) {
			// Handle cases where the server fails to start.
			Log.e(TAG, "Failed to start log stream server: ${e.message}")
			MessageLog.e(TAG, "Failed to start Remote Log Viewer: ${e.message}")
			isRunning = false
			serverScope?.cancel()
			serverScope = null
			actionChannel?.close()
			actionChannel = null
		}
	}

	/**
	 * Stops the log streaming server, clears the buffer, and unregisters from EventBus.
	 */
	fun stop() {
		if (!isRunning) return

		try {
			// Unregister to stop receiving log events.
			EventBus.getDefault().unregister(this)
		} catch (_: Exception) {
			// Ignore exceptions if the server was not registered.
		}

		// Shutdown the Ktor server instance with a brief grace period.
		server?.stop(500, 1000)
		server = null

		// Cancel the coroutine scope to clean up background tasks.
		serverScope?.cancel()
		serverScope = null

		// Close the action channel.
		actionChannel?.close()
		actionChannel = null

		// Clear the message buffer to free up memory.
		clearBuffer()

		// Disconnect all tracked clients.
		clients.clear()

		isRunning = false
		Log.i(TAG, "Log stream server stopped.")
	}

	/**
	 * Subscriber for MessageLog events via EventBus.
	 *
	 * Broadcasts each received log message to all currently connected WebSocket clients.
	 *
	 * @param event The JSEvent object containing the log message metadata and content.
	 */
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
	fun onMessageLogEvent(event: JSEvent) {
		// Only process events that are identified as MessageLog entries.
		if (event.eventName == "MessageLog") {
			broadcast(event.message)
		}
	}

	/**
	 * Subscriber for MediaProjectionService events via EventBus.
	 *
	 * Stops the log server if the projection service is stopped via notification or UI.
	 *
	 * @param event The JSEvent object containing the event details.
	 */
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
	fun onMediaProjectionEvent(event: JSEvent) {
		if (event.eventName == "MediaProjectionService" && event.message == "Not Running") {
			Log.i(TAG, "MediaProjectionService stopped. Initiating LogStreamServer shutdown.")
			stop()
		}
	}

	/**
	 * Retrieves the device's local IP address as a human-readable string.
	 *
	 * Prioritizes actual network interfaces over the WifiManager, which can be unreliable
	 * or restricted on newer Android versions.
	 *
	 * @param context The application context.
	 *
	 * @return The device's local IP address or "0.0.0.0" if unavailable.
	 */
	fun getDeviceIpAddress(context: Context): String {
		val foundIps = mutableListOf<String>()
		try {
			// Enumerate all network interfaces on the device.
			val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
			for (intf in interfaces) {
				val addrs = java.util.Collections.list(intf.inetAddresses)
				for (addr in addrs) {
					// Skip loopback addresses to find real network IPs.
					if (!addr.isLoopbackAddress) {
						val sAddr = addr.hostAddress
						val isIPv4 = sAddr.indexOf(':') < 0
						if (isIPv4) {
							foundIps.add(sAddr)
						}
					}
				}
			}
		} catch (e: Exception) {
			Log.w(TAG, "Could not determine IP address via NetworkInterface: ${e.message}")
		}

		if (foundIps.isNotEmpty()) {
			Log.d(TAG, "Found local IPs: $foundIps")

			// Prioritize private network address ranges common in local networks.
			val bestIp = foundIps.find { it.startsWith("192.168.") }
				?: foundIps.find { it.startsWith("10.") && it != "10.0.2.15" }
				?: foundIps.find { it.startsWith("172.16.") || it.startsWith("172.31.") }
				?: foundIps[0]

			return bestIp
		}

		// Fallback to the WifiManager for older devices or if the interface scan fails.
		try {
			val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
			val wifiInfo = wifiManager.connectionInfo
			val ipInt = wifiInfo.ipAddress

			if (ipInt != 0) {
				// Convert the integer representation of the IP address to a dotted string.
				return String.format(
					"%d.%d.%d.%d",
					ipInt and 0xff,
					ipInt shr 8 and 0xff,
					ipInt shr 16 and 0xff,
					ipInt shr 24 and 0xff
				)
			}
		} catch (e: Exception) {
			Log.w(TAG, "Could not determine WiFi IP address via WifiManager: ${e.message}")
		}

		return "0.0.0.0"
	}

	/**
	 * Serves the log_viewer.html page from the Android assets directory.
	 *
	 * @param call The Ktor application call to respond to.
	 * @param context The application context used for accessing assets.
	 */
	private suspend fun serveLogViewerHtml(call: ApplicationCall, context: Context) {
		try {
			val htmlStream = context.assets.open("log_viewer.html")
			val html = htmlStream.bufferedReader().use { it.readText() }
			call.respondText(html, ContentType.Text.Html)
		} catch (e: IOException) {
			Log.e(TAG, "Failed to load log_viewer.html asset: ${e.message}")
			call.respondText(
				"Failed to load log viewer page.",
				ContentType.Text.Plain,
				HttpStatusCode.InternalServerError
			)
		}
	}

	/**
	 * Handles an individual WebSocket session lifecycle.
	 *
	 * Enqueues a NewClient action to ensure sequential history delivery relative to live broadcasts.
	 *
	 * @param session The active WebSocket server session.
	 */
	private suspend fun handleWebSocketSession(session: DefaultWebSocketServerSession) {
		Log.d(TAG, "WebSocket client connection initiated.")

		// Enqueue the registration action to the background worker.
		actionChannel?.send(LogAction.NewClient(session))

		try {
			// Keep the session alive until the client disconnects.
			for (frame in session.incoming) {
				// The server does not handle incoming messages from clients.
			}
		} catch (e: Exception) {
			Log.w(TAG, "WebSocket session exception: ${e.message}")
		} finally {
			clients.remove(session)
			Log.d(TAG, "WebSocket client disconnected.")
		}
	}

	/**
	 * Performs history synchronization for a new client session.
	 * Called by the background worker to ensure no live logs are sent before history is done.
	 *
	 * @param session The active WebSocket server session.
	 */
	private suspend fun handleNewClientAction(session: DefaultWebSocketServerSession) {
		try {
			val historyToSync = synchronized(bufferLock) {
				messageBuffer.toList() // Send chronological (oldest to newest)
			}

			if (historyToSync.isNotEmpty()) {
				val jsonArray = JSONArray()
				for (msg in historyToSync) {
					jsonArray.put(parseLogToJSON(msg))
				}
				session.send(Frame.Text("HB:$jsonArray"))
			}
			session.send(Frame.Text("HISTORY_DONE"))

			// Now that history is synced, add to clients for real-time broadcasts.
			clients.add(session)
			Log.d(TAG, "WebSocket client history sync complete and added to active set.")
		} catch (e: Exception) {
			Log.e(TAG, "Failed to sync history for new client: ${e.message}")
		}
	}

	/**
	 * Parses a raw log string into a JSON object.
	 *
	 * @param message The raw log message to parse.
	 *
	 * @return A JSONObject containing the parsed log data.
	 */
	private fun parseLogToJSON(message: String): JSONObject {
		val matcher = logPattern.matcher(message)
		return if (matcher.find()) {
			val newline = matcher.group(1) ?: ""
			val timestamp = matcher.group(2) ?: ""
			val level = matcher.group(3) ?: "DEBUG"
			val text = matcher.group(4) ?: ""

			// Extract high-level details for dashboard updates.
			val action = detectAction(text)
			val trainee = parseTraineeInfo(text)
			val dateInfo = if (text.contains("[DATE]") ||
				text.contains("New date:", ignoreCase = true) ||
				text.contains("Turn", ignoreCase = true)
			) {
				parseDateInfo(text)
			} else null

			LogEntry(newline, timestamp, level, text, action, trainee, dateInfo).toJSON()
		} else {
			// Fallback: detect level and treat entire message as text.
			val level = when {
				message.contains("[ERROR]") -> "ERROR"
				message.contains("[WARN]") -> "WARN"
				message.contains("[INFO]") -> "INFO"
				message.contains("[VERBOSE]") -> "VERBOSE"
				else -> "DEBUG"
			}
			LogEntry("", "", level, message).toJSON()
		}
	}

	/**
	 * Detects common bot actions for session counters.
	 *
	 * @param text The log message text to check.
	 *
	 * @return The action name if detected, otherwise NULL.
	 */
	private fun detectAction(text: String): String? {
		return when {
			actionTrainingPattern.matcher(text).find() -> "training"
			actionRacePattern.matcher(text).find() -> "race"
			actionMoodPattern.matcher(text).find() -> "mood"
			actionEnergyPattern.matcher(text).find() -> "energy"
			actionInjuryPattern.matcher(text).find() -> "injury"
			else -> null
		}
	}

	/**
	 * Parses trainee detailed info for the dashboard.
	 *
	 * @param text The log message text to check.
	 *
	 * @return A JSONObject containing the trainee details if detected, otherwise NULL.
	 */
	private fun parseTraineeInfo(text: String): JSONObject? {
		val matcher = traineeDetailedPattern.matcher(text)
		return if (matcher.find()) {
			JSONObject().apply {
				put("category", matcher.group(1))
				put("data", matcher.group(2))
			}
		} else null
	}

	/**
	 * Parses date and turn information for the dashboard.
	 *
	 * @param text The log message text to check.
	 *
	 * @return A JSONObject containing the date and turn if detected, otherwise NULL.
	 */
	private fun parseDateInfo(text: String): JSONObject? {
		val matcher = dateNewPattern.matcher(text)
		return if (matcher.find()) {
			val date = matcher.group(1)?.trim() ?: ""
			val turn = matcher.group(2) ?: ""

			JSONObject().apply {
				put("date", date)
				put("turn", turn)
			}
		} else {
			// Fallback: If no "[DATE] New date:" prefix but contains "(Turn X)", try extracting just the turn.
			val turnOnlyPattern = Pattern.compile("\\(Turn (\\d+)\\)", Pattern.CASE_INSENSITIVE)
			val turnMatcher = turnOnlyPattern.matcher(text)
			if (turnMatcher.find()) {
				JSONObject().apply {
					put("turn", turnMatcher.group(1))
				}
			} else null
		}
	}

	/**
	 * Converts a turn number to a descriptive date string.
	 *
	 * @param day The turn number (day) to convert.
	 *
	 * @return The descriptive date string.
	 */
	private fun dateFromDay(day: Int): String {
		val d = day - 1
		val y = d / 24
		val m = (d % 24) / 2
		val p = d % 2

		val years = listOf("Junior Year", "Classic Year", "Senior Year")
		val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
		val phases = listOf("Early", "Late")

		val yearStr = years.getOrElse(y) { "" }
		val monthStr = months.getOrElse(m) { "" }
		val phaseStr = phases.getOrElse(p) { "" }

		return "$yearStr $phaseStr $monthStr"
	}

	/**
	 * Clears all messages from the history buffer.
	 */
	private fun clearBuffer() {
		synchronized(bufferLock) {
			messageBuffer.clear()
		}
	}

	/**
	 * Handles a clear action by clearing the history buffer and notifying all clients.
	 *
	 * Called by the background worker.
	 */
	private suspend fun handleClearAction() {
		Log.i(TAG, "Executing log clear action.")
		clearBuffer()
		isMuted = false // Ensure we are unmuted for the new run.
		
		// Signal all connected clients to clear their displays.
		for (client in clients) {
			try {
				client.send(Frame.Text("CMD:CLEAR"))
			} catch (e: Exception) {
				clients.remove(client)
			}
		}
	}

	/**
	 * Pre-fills the history buffer with a given list of existing log messages.
	 *
	 * @param history A list of log message strings to initialize the buffer.
	 */
	private fun populateBuffer(history: List<String>) {
		synchronized(bufferLock) {
			messageBuffer.clear()
			// Only retain the last maxBufferSize messages to avoid overflow.
			val start = (history.size - maxBufferSize).coerceAtLeast(0)
			for (i in start until history.size) {
				messageBuffer.add(history[i])
			}
			Log.d(TAG, "Populated buffer with ${messageBuffer.size} historical logs.")
		}
	}

	/**
	 * Enqueues a log message for sequential processing.
	 *
	 * @param message The log message to broadcast.
	 */
	private fun broadcast(message: String) {
		if (!isRunning) return

		// Enqueue the broadcast action to ensure it is processed chronologically relative to new sessions.
		serverScope?.launch {
			actionChannel?.send(LogAction.Broadcast(message))
		}
	}

	/**
	 * Processes a broadcast action by updating the buffer and sending to all active clients.
	 *
	 * Called by the background worker.
	 *
	 * @param message The log message to broadcast.
	 */
	private suspend fun handleBroadcastAction(message: String) {
		if (isMuted) return

		// Update history buffer.
		synchronized(bufferLock) {
			messageBuffer.add(message)
			if (messageBuffer.size > maxBufferSize) {
				messageBuffer.removeAt(0)
			}
		}

		// Detect end-of-run markers to mute subsequent logs after this one is buffered/sent.
		if (message.contains("Campaign main loop exiting") || message.contains("Total runtime of")) {
			Log.i(TAG, "End of run detected in logs. Muting subsequent broadcasts.")
			isMuted = true
		}

		// Skip transmission if no clients.
		if (clients.isEmpty()) return

		// Broadcast as JSON to all active clients.
		val jsonResponse = parseLogToJSON(message).toString()
		for (client in clients) {
			try {
				client.send(Frame.Text(jsonResponse))
			} catch (e: Exception) {
				clients.remove(client)
			}
		}
	}
}
