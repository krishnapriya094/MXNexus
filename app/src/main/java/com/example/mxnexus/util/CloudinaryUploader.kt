package com.example.mxnexus.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uploads images to Cloudinary using the unsigned upload API.
 * No SDK required — uses plain HttpURLConnection.
 *
 * ┌──────────────────────────────────────────────────┐
 * │  FILL IN YOUR VALUES HERE:                        │
 * │  1. Go to cloudinary.com and log in              │
 * │  2. Copy your Cloud Name from the dashboard      │
 * │  3. Settings → Upload → Upload Presets →         │
 * │     create an UNSIGNED preset and copy its name  │
 * └──────────────────────────────────────────────────┘
 */
object CloudinaryUploader {

    // ── !! REPLACE THESE WITH YOUR REAL VALUES !! ──────────────────────
    private const val CLOUD_NAME   = "dyvogt302"
    private const val UPLOAD_PRESET = "MXNexus"
    // ───────────────────────────────────────────────────────────────────

    private val UPLOAD_URL get() =
        "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private const val BOUNDARY = "----MXNexusBoundary7MA4YWxkTrZu0gW"
    private const val LINE_FEED = "\r\n"

    /**
     * Uploads raw image bytes to Cloudinary.
     * Must be called from a coroutine (uses IO dispatcher internally).
     *
     * @param bytes  Raw image bytes (JPEG/PNG).
     * @param folder Optional subfolder in your Cloudinary media library.
     * @return The secure HTTPS URL of the uploaded image.
     * @throws Exception on network or server errors.
     */
    suspend fun upload(bytes: ByteArray, folder: String = "mxnexus"): String =
        withContext(Dispatchers.IO) {
            val connection = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
                requestMethod    = "POST"
                doOutput         = true
                doInput          = true
                useCaches        = false
                connectTimeout   = 30_000
                readTimeout      = 60_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            }

            try {
                connection.outputStream.buffered().use { out ->
                    // -- upload_preset field
                    writeField(out, "upload_preset", UPLOAD_PRESET)
                    // -- folder field
                    writeField(out, "folder", folder)
                    // -- file field (raw bytes)
                    writeFilePart(out, bytes)
                    // -- closing boundary
                    out.write(("--$BOUNDARY--$LINE_FEED").toByteArray())
                    out.flush()
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                if (responseCode !in 200..299) {
                    throw Exception("Cloudinary error $responseCode: $response")
                }

                JSONObject(response).getString("secure_url")
            } finally {
                connection.disconnect()
            }
        }

    private fun writeField(out: OutputStream, name: String, value: String) {
        out.write(("--$BOUNDARY$LINE_FEED").toByteArray())
        out.write(("Content-Disposition: form-data; name=\"$name\"$LINE_FEED").toByteArray())
        out.write(LINE_FEED.toByteArray())
        out.write((value + LINE_FEED).toByteArray())
    }

    private fun writeFilePart(out: OutputStream, bytes: ByteArray) {
        out.write(("--$BOUNDARY$LINE_FEED").toByteArray())
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"$LINE_FEED").toByteArray())
        out.write(("Content-Type: image/jpeg$LINE_FEED").toByteArray())
        out.write(LINE_FEED.toByteArray())
        out.write(bytes)
        out.write(LINE_FEED.toByteArray())
    }
}
