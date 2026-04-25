package com.mythos.ide

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var svLogcat: ScrollView
    private lateinit var spinnerLevel: Spinner
    private lateinit var etFilter: EditText

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isPaused = false
    private var logProcess: Process? = null
    private var filterText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)

        tvOutput = findViewById(R.id.tvLogcatOutput)
        svLogcat = findViewById(R.id.svLogcat)
        spinnerLevel = findViewById(R.id.spinnerLogLevel)
        etFilter = findViewById(R.id.etLogcatFilter)

        val btnBack = findViewById<ImageButton>(R.id.btnLogcatBack)
        val btnClear = findViewById<ImageButton>(R.id.btnLogcatClear)
        val btnPause = findViewById<ImageButton>(R.id.btnLogcatPause)

        // Log level spinner
        val levels = LOG_LEVELS.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
        spinnerLevel.setSelection(0) // Verbose

        btnBack.setOnClickListener { finish() }
        btnClear.setOnClickListener { tvOutput.text = "" }
        btnPause.setOnClickListener {
            isPaused = !isPaused
            btnPause.setImageResource(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        }

        etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterText = s?.toString() ?: "" }
        })

        startLogcat()
    }

    override fun onDestroy() {
        super.onDestroy()
        logProcess?.destroy()
        scope.cancel()
    }

    private fun startLogcat() {
        scope.launch {
            try {
                // Clear logcat buffer first
                Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()

                val process = ProcessBuilder("logcat", "-v", "brief")
                    .redirectErrorStream(true)
                    .start()
                logProcess = process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (isActive && reader.readLine().also { line = it } != null) {
                    if (isPaused) continue
                    val logLine = line ?: continue

                    // Filter by level
                    val levelIndex = spinnerLevel.selectedItemPosition
                    val minLevel = LOG_LEVELS[levelIndex].second
                    val lineLevel = parseLogLevel(logLine)
                    if (lineLevel < minLevel) continue

                    // Filter by text
                    if (filterText.isNotEmpty() && !logLine.contains(filterText, ignoreCase = true)) continue

                    val color = getColorForLevel(lineLevel)

                    withContext(Dispatchers.Main) {
                        appendLogLine(logLine, color)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    appendLogLine("Logcat not available on this device", COLOR_ERROR)
                }
            }
        }
    }

    private fun appendLogLine(text: String, color: Int) {
        val spannable = SpannableStringBuilder(text + "\n")
        spannable.setSpan(
            ForegroundColorSpan(color),
            0, spannable.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvOutput.append(spannable)

        // Auto-scroll to bottom
        svLogcat.post { svLogcat.fullScroll(ScrollView.FOCUS_DOWN) }

        // Trim if too large (keep last ~5000 lines)
        if (tvOutput.text.length > 200_000) {
            val trimmed = tvOutput.text.subSequence(tvOutput.text.length - 100_000, tvOutput.text.length)
            tvOutput.text = trimmed
        }
    }

    private fun parseLogLevel(line: String): Int {
        // Logcat brief format: "D/Tag(pid): message"
        return when {
            line.startsWith("V/") -> LEVEL_VERBOSE
            line.startsWith("D/") -> LEVEL_DEBUG
            line.startsWith("I/") -> LEVEL_INFO
            line.startsWith("W/") -> LEVEL_WARN
            line.startsWith("E/") -> LEVEL_ERROR
            line.startsWith("F/") -> LEVEL_FATAL
            else -> LEVEL_VERBOSE
        }
    }

    private fun getColorForLevel(level: Int): Int {
        return when (level) {
            LEVEL_VERBOSE -> COLOR_VERBOSE
            LEVEL_DEBUG -> COLOR_DEBUG
            LEVEL_INFO -> COLOR_INFO
            LEVEL_WARN -> COLOR_WARN
            LEVEL_ERROR -> COLOR_ERROR
            LEVEL_FATAL -> COLOR_FATAL
            else -> COLOR_VERBOSE
        }
    }

    companion object {
        private const val LEVEL_VERBOSE = 0
        private const val LEVEL_DEBUG = 1
        private const val LEVEL_INFO = 2
        private const val LEVEL_WARN = 3
        private const val LEVEL_ERROR = 4
        private const val LEVEL_FATAL = 5

        private val LOG_LEVELS = listOf(
            "Verbose" to LEVEL_VERBOSE,
            "Debug" to LEVEL_DEBUG,
            "Info" to LEVEL_INFO,
            "Warn" to LEVEL_WARN,
            "Error" to LEVEL_ERROR
        )

        private const val COLOR_VERBOSE = 0xFF888888.toInt()
        private const val COLOR_DEBUG = 0xFF4FC3F7.toInt()
        private const val COLOR_INFO = 0xFF66BB6A.toInt()
        private const val COLOR_WARN = 0xFFFFCA28.toInt()
        private const val COLOR_ERROR = 0xFFEF5350.toInt()
        private const val COLOR_FATAL = 0xFFFF1744.toInt()
    }
}
