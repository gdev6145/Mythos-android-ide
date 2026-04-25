package com.mythos.ide

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TerminalActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var etInput: EditText
    private lateinit var svOutput: ScrollView

    private var workingDir: File? = null
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        tvOutput = findViewById(R.id.tvTerminalOutput)
        etInput = findViewById(R.id.etCommandInput)
        svOutput = findViewById(R.id.svTerminalOutput)

        val btnBack = findViewById<ImageButton>(R.id.btnTerminalBack)
        val btnClear = findViewById<ImageButton>(R.id.btnClearTerminal)
        val btnRun = findViewById<ImageButton>(R.id.btnRunCommand)

        workingDir = getExternalFilesDir(null)

        btnBack.setOnClickListener { finish() }
        btnClear.setOnClickListener { tvOutput.text = "" }
        btnRun.setOnClickListener { executeCurrentCommand() }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                executeCurrentCommand()
                true
            } else false
        }

        appendOutput("MYTHOS Terminal\n", COLOR_INFO)
        appendOutput("Type commands below. Built-in: cd, pwd, clear, help, model-status\n\n", COLOR_COMMENT)
    }

    private fun executeCurrentCommand() {
        val cmd = etInput.text.toString().trim()
        if (cmd.isEmpty()) return

        etInput.text.clear()
        commandHistory.add(cmd)
        historyIndex = commandHistory.size

        appendOutput("$ $cmd\n", COLOR_PROMPT)

        when {
            cmd == "clear" -> tvOutput.text = ""
            cmd == "pwd" -> appendOutput("${workingDir?.absolutePath ?: "/"}\n\n", COLOR_OUTPUT)
            cmd == "help" -> showHelp()
            cmd == "model-status" -> checkModelStatus()
            cmd.startsWith("cd ") -> changeDirectory(cmd.removePrefix("cd ").trim())
            else -> runShellCommand(cmd)
        }
    }

    private fun changeDirectory(path: String) {
        val target = if (path.startsWith("/")) {
            File(path)
        } else {
            File(workingDir, path)
        }.canonicalFile

        if (target.exists() && target.isDirectory) {
            workingDir = target
            appendOutput("${target.absolutePath}\n\n", COLOR_OUTPUT)
        } else {
            appendOutput("cd: no such directory: $path\n\n", COLOR_ERROR)
        }
    }

    private fun showHelp() {
        appendOutput("""
            |Built-in commands:
            |  cd <dir>        Change directory
            |  pwd             Print working directory
            |  clear           Clear terminal output
            |  model-status    Check if MYTHOS model is running
            |  help            Show this help
            |
            |Other commands are executed as shell processes.
            |
        """.trimMargin() + "\n", COLOR_COMMENT)
    }

    private fun checkModelStatus() {
        appendOutput("Checking model status...\n", COLOR_COMMENT)
        CoroutineScope(Dispatchers.IO).launch {
            val running = com.mythos.ide.util.TermuxBridge.isModelRunning()
            withContext(Dispatchers.Main) {
                if (running) {
                    appendOutput("Model server is running on port 8081\n\n", COLOR_INFO)
                } else {
                    appendOutput("Model server is not running\n\n", COLOR_ERROR)
                }
            }
        }
    }

    private fun runShellCommand(command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parts = listOf("/bin/sh", "-c", command)
                val processBuilder = ProcessBuilder(parts)
                    .directory(workingDir)
                    .redirectErrorStream(true)

                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append('\n')
                    // Stream output line by line for long-running commands
                    val currentLine = line
                    if (output.length > 1000) {
                        val chunk = output.toString()
                        output.clear()
                        withContext(Dispatchers.Main) {
                            appendOutput(chunk, COLOR_OUTPUT)
                        }
                    }
                }

                val exitCode = process.waitFor()

                withContext(Dispatchers.Main) {
                    if (output.isNotEmpty()) {
                        appendOutput(output.toString(), COLOR_OUTPUT)
                    }
                    if (exitCode != 0) {
                        appendOutput("Process exited with code $exitCode\n", COLOR_ERROR)
                    }
                    appendOutput("\n", COLOR_OUTPUT)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("Error: ${e.message}\n\n", COLOR_ERROR)
                }
            }
        }
    }

    private fun appendOutput(text: String, color: Int) {
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(
            ForegroundColorSpan(color),
            0, text.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvOutput.append(spannable)
        svOutput.post { svOutput.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    companion object {
        private const val COLOR_PROMPT = 0xFF4EC9B0.toInt()
        private const val COLOR_OUTPUT = 0xFFD4D4D4.toInt()
        private const val COLOR_ERROR = 0xFFF44747.toInt()
        private const val COLOR_INFO = 0xFF569CD6.toInt()
        private const val COLOR_COMMENT = 0xFF6A9955.toInt()
    }
}
