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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

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

	// Thread-safe set of currently active WebSocket client sessions.
	private val clients = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

	// Circular buffer for storing the most recent log messages.
	private val messageBuffer = ArrayList<String>()

	// Synchronization lock for thread-safe access to the message buffer.
	private val bufferLock = Object()

	// Maximum number of messages to retain in the history buffer.
	private val maxBufferSize = 15000

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
		server?.stop(1000, 2000)
		server = null

		// Cancel the coroutine scope to clean up background tasks.
		serverScope?.cancel()
		serverScope = null

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
	 * Retrieves the device's local IP address as a human-readable string.
	 *
	 * Prioritizes actual network interfaces over the WifiManager, which can be unreliable
	 * or restricted on newer Android versions.
	 *
	 * @param context The application context.
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
	 * Sends buffered history on connect and keeps the session alive until the client disconnects.
	 *
	 * @param session The active WebSocket server session.
	 */
	private suspend fun handleWebSocketSession(session: DefaultWebSocketServerSession) {
		Log.d(TAG, "WebSocket client connected. Total clients: ${clients.size + 1}")
		clients.add(session)

		try {
			// Create a snapshot of the current history buffer and reverse it to send newest first.
			val historyToSync = synchronized(bufferLock) {
				messageBuffer.toList().asReversed()
			}

			// Send historical logs in batches to optimize network throughput.
			val batched = historyToSync.chunked(100)
			for (batch in batched) {
				val combined = "HISTORY_BATCH:" + batch.joinToString("---LOG_SEPARATOR---")
				session.send(Frame.Text(combined))
			}
			session.send(Frame.Text("HISTORY_DONE"))

			// Keep the session alive by consuming incoming frames until the client disconnects.
			for (frame in session.incoming) {
				// The server does not expect or handle incoming messages from clients.
			}
		} catch (e: Exception) {
			Log.w(TAG, "WebSocket exception: ${e.message}")
		} finally {
			clients.remove(session)
			Log.d(TAG, "WebSocket client disconnected. Remaining clients: ${clients.size}")
		}
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
	 * Broadcasts a log message to all connected clients and adds it to the history buffer.
	 *
	 * @param message The log message string to be sent to clients.
	 */
	private fun broadcast(message: String) {
		// Persist the message in the history buffer for late joiners.
		synchronized(bufferLock) {
			messageBuffer.add(message)
			// Maintain the buffer size within the specified limit.
			if (messageBuffer.size > maxBufferSize) {
				messageBuffer.removeAt(0)
			}
		}

		// If no clients are connected, there is no need to proceed with broadcasting.
		if (clients.isEmpty()) {
			return
		}

		// Launch a coroutine to send to all active clients without blocking the EventBus thread.
		serverScope?.launch {
			for (client in clients) {
				try {
					client.send(Frame.Text(message))
				} catch (e: Exception) {
					// Remove the client if delivery fails (likely disconnected).
					Log.w(TAG, "Failed to send message to client: ${e.message}")
					clients.remove(client)
				}
			}
		}
	}
}
