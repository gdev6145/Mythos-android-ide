package com.mythos.ide

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val btnBack = findViewById<ImageButton>(R.id.btnSettingsBack)
        val spinnerFontSize = findViewById<Spinner>(R.id.spinnerFontSize)
        val switchWordWrap = findViewById<Switch>(R.id.switchWordWrap)
        val switchLineNumbers = findViewById<Switch>(R.id.switchLineNumbers)
        val switchAutoStart = findViewById<Switch>(R.id.switchAutoStartModel)
        val tvModelPath = findViewById<TextView>(R.id.tvModelPath)
        val tvVersion = findViewById<TextView>(R.id.tvVersion)

        btnBack.setOnClickListener { finish() }

        // Font size spinner
        val fontSizes = FONT_SIZES.map { "${it}sp" }
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontSizes)
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFontSize.adapter = fontAdapter

        val currentFontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        val fontIndex = FONT_SIZES.indexOf(currentFontSize).coerceAtLeast(0)
        spinnerFontSize.setSelection(fontIndex)

        // Switches
        switchWordWrap.isChecked = prefs.getBoolean(KEY_WORD_WRAP, true)
        switchLineNumbers.isChecked = prefs.getBoolean(KEY_LINE_NUMBERS, true)
        switchAutoStart.isChecked = prefs.getBoolean(KEY_AUTO_START_MODEL, false)

        // Model path
        val mythosDir = getExternalFilesDir(null)
        tvModelPath.text = mythosDir?.absolutePath ?: getString(R.string.settings_path_unavailable)

        // Version
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            tvVersion.text = getString(R.string.settings_version_format, versionName)
        } catch (_: Exception) {
            tvVersion.text = getString(R.string.settings_version_format, "1.0")
        }
    }

    override fun onPause() {
        super.onPause()
        saveSettings()
    }

    private fun saveSettings() {
        val spinnerFontSize = findViewById<Spinner>(R.id.spinnerFontSize)
        val switchWordWrap = findViewById<Switch>(R.id.switchWordWrap)
        val switchLineNumbers = findViewById<Switch>(R.id.switchLineNumbers)
        val switchAutoStart = findViewById<Switch>(R.id.switchAutoStartModel)

        val selectedFontIndex = spinnerFontSize.selectedItemPosition
        val fontSize = if (selectedFontIndex in FONT_SIZES.indices) FONT_SIZES[selectedFontIndex] else DEFAULT_FONT_SIZE

        prefs.edit()
            .putInt(KEY_FONT_SIZE, fontSize)
            .putBoolean(KEY_WORD_WRAP, switchWordWrap.isChecked)
            .putBoolean(KEY_LINE_NUMBERS, switchLineNumbers.isChecked)
            .putBoolean(KEY_AUTO_START_MODEL, switchAutoStart.isChecked)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "mythos_settings"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_WORD_WRAP = "word_wrap"
        const val KEY_LINE_NUMBERS = "line_numbers"
        const val KEY_AUTO_START_MODEL = "auto_start_model"
        const val DEFAULT_FONT_SIZE = 14

        val FONT_SIZES = listOf(10, 12, 14, 16, 18, 20, 24)
    }
}
