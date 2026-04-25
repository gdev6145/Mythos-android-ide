package com.mythos.ide.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility for communicating with Termux and running the MYTHOS-26B model.
 *
 * The bridge manages a local socket or pipe to send prompts to a llama.cpp
 * process running inside the Termux environment and reads back completions.
 * When Termux is not available it falls back to a stub that returns a
 * placeholder response so the rest of the app keeps working.
 */
object TermuxBridge {

    private const val MODEL_DIR_NAME = "mythos-setup"
    private const val MODEL_BINARY = "llama-server"
    private const val DEFAULT_PORT = 8081

    /** Whether the model directory has been set up on this device. */
    fun isModelInstalled(context: Context): Boolean {
        val dir = getModelDir(context)
        return dir.exists() && dir.isDirectory
    }

    /** Returns the directory where the model artefacts are stored. */
    fun getModelDir(context: Context): File {
        return File(context.getExternalFilesDir(null), MODEL_DIR_NAME)
    }

    /**
     * Bootstrap the model directory. In a real implementation this would
     * download the GGUF weights and the llama.cpp binary; for now it creates
     * the directory structure so the rest of the setup flow works.
     */
    suspend fun installModel(context: Context, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            val dir = getModelDir(context)
            onProgress("Creating model directory...")
            dir.mkdirs()

            val binDir = File(dir, "bin")
            binDir.mkdirs()

            val modelsDir = File(dir, "models")
            modelsDir.mkdirs()

            // Write a small marker file so we can tell setup completed
            File(dir, ".installed").writeText("installed_at=${System.currentTimeMillis()}\n")

            onProgress("Model directory ready")
        }
    }

    /**
     * Send a code-completion prompt to the running model and return the
     * response text. Falls back to a placeholder when the model process
     * is not reachable.
     */
    suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://127.0.0.1:$DEFAULT_PORT/completion")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 10000

            val body = """{"prompt":"${escapeJson(prompt)}","n_predict":128}"""
            conn.outputStream.bufferedWriter().use { it.write(body) }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            response
        } catch (_: Exception) {
            // Model server is not running; return a helpful stub
            "// MYTHOS model is not running. Start the model service first."
        }
    }

    /** Check whether the model server process is reachable. */
    suspend fun isModelRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://127.0.0.1:$DEFAULT_PORT/health")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1500
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
