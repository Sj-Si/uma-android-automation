package com.steve1316.uma_android_automation.utils

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import android.content.Context

class TesseractApi(private val context: Context) {
    private val TAG: String = "TesseractApi"

	private val tesseractLanguages = arrayListOf("eng")

    // Root directory of external files.
    private var externalFilesPath: String? = null
    // Stores the file path to the saved match image file for debugging purposes.
    lateinit var matchFilePath: String
    private lateinit var tesseractFilePath: String
    private lateinit var tesseractTempDataFilePath: String

    private lateinit var tess: TessBaseAPI

    init {
        setupTessFiles()
        tess = TessBaseAPI()
        tess.init(tesseractFilePath, "eng")
        MessageLog.d(TAG, "Initialized")
    }

    fun destroy() {
        //tess?.end()
    }

    fun ocr(bitmap: Bitmap, singleLineMode: Boolean = false): String {
        return try { 
            tess.setImage(bitmap)
            if (singleLineMode) {
                // Set the Page Segmentation Mode to '--psm 7' or "Treat the image as a single text line"
                // https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html#page-segmentation-method
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
            }
            val res = tess.utF8Text
            MessageLog.d(TAG, "OCR Detected text: ${res}")
            res
        } catch (e: Exception) {
            MessageLog.e(TAG, "OCR Detection error: ${e.stackTraceToString()}")
            ""
        } finally {
            tess.clear()
        }
    }

    /**
    * Initialize Tesseract for future OCR operations. Make sure to put your .traineddata inside the root of the /assets/ folder.
    */
	private fun setupTessFiles() {
        externalFilesPath = context.getExternalFilesDir(null)?.absolutePath
        matchFilePath = "$externalFilesPath/temp"
        tesseractFilePath = "$externalFilesPath/tesseract"
        tesseractTempDataFilePath = "$tesseractFilePath/tessdata"

		val newTempDirectory = File(tesseractTempDataFilePath)

		// If the /files/temp/ folder does not exist, create it.
		if (!newTempDirectory.exists()) {
			val successfullyCreated: Boolean = newTempDirectory.mkdirs()

			// If the folder was not able to be created for some reason, log the error and stop the MediaProjection Service.
			if (!successfullyCreated) {
				MessageLog.e(TAG, "Failed to create the /files/tesseract/tessdata/ folder.")
			} else {
				MessageLog.i(TAG, "Successfully created /files/tesseract/tessdata/ folder.")
			}
		} else {
			MessageLog.i(TAG, "/files/tesseract/tessdata/ folder already exists.")
		}

		// If the traineddata is not in the application folder, copy it there from assets.
		tesseractLanguages.forEach { lang ->
			val trainedDataPath = File(tesseractTempDataFilePath, "$lang.traineddata")
			if (!trainedDataPath.exists()) {
				try {
					MessageLog.i(TAG, "Starting Tesseract initialization.")
					val input = context.assets?.open("$lang.traineddata")
                    if (input == null) {
                        throw IOException("Failed to open asset: $lang.traineddata")
                    }

					val output = FileOutputStream("$tesseractTempDataFilePath/$lang.traineddata")

					val buffer = ByteArray(1024)
					var read: Int
					while (input.read(buffer).also { read = it } != -1) {
						output.write(buffer, 0, read)
					}

					input.close()
					output.flush()
					output.close()
					MessageLog.i(TAG, "Finished Tesseract file structure initialization.")
				} catch (e: IOException) {
					MessageLog.e(TAG, "IOException: ${e.stackTraceToString()}")
				}
			}
		}
	}
}