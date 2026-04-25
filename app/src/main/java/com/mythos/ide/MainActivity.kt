package com.mythos.ide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mythos.ide.services.ModelService
import com.mythos.ide.util.RecentFilesManager
import com.mythos.ide.util.TermuxBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var actionButton: Button
    private lateinit var btnOpenFiles: Button
    private lateinit var btnNewProject: Button
    private lateinit var btnTerminal: Button
    private lateinit var btnGit: Button
    private lateinit var btnLogcat: Button
    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        progressText = findViewById(R.id.progressText)
        actionButton = findViewById(R.id.actionButton)
        btnOpenFiles = findViewById(R.id.btnOpenFiles)
        btnNewProject = findViewById(R.id.btnNewProject)
        btnTerminal = findViewById(R.id.btnTerminal)
        btnGit = findViewById(R.id.btnGit)
        btnLogcat = findViewById(R.id.btnLogcat)
        btnSettings = findViewById(R.id.btnSettings)

        btnOpenFiles.setOnClickListener {
            startActivity(Intent(this, FileExplorerActivity::class.java))
        }

        btnNewProject.setOnClickListener {
            startActivity(Intent(this, NewProjectActivity::class.java))
        }

        btnTerminal.setOnClickListener {
            startActivity(Intent(this, TerminalActivity::class.java))
        }

        btnGit.setOnClickListener {
            startActivity(Intent(this, GitActivity::class.java))
        }

        btnLogcat.setOnClickListener {
            startActivity(Intent(this, LogcatActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestStoragePermission()
        checkSetupStatus()
    }

    override fun onResume() {
        super.onResume()
        checkSetupStatus()
        populateRecentFiles()
    }

    private fun populateRecentFiles() {
        val tvRecentTitle = findViewById<TextView>(R.id.tvRecentTitle)
        val llRecentFiles = findViewById<LinearLayout>(R.id.llRecentFiles)
        llRecentFiles.removeAllViews()

        val recentFiles = RecentFilesManager.getExistingRecentFiles(this)
        if (recentFiles.isEmpty()) {
            tvRecentTitle.visibility = View.GONE
            return
        }

        tvRecentTitle.visibility = View.VISIBLE

        // Show up to 5 most recent
        recentFiles.take(5).forEach { path ->
            val file = File(path)
            val tv = TextView(this).apply {
                text = file.name
                textSize = 14f
                setPadding(16, 12, 16, 12)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                typeface = Typeface.create("monospace", Typeface.NORMAL)
                setBackgroundResource(android.R.attr.selectableItemBackground)
                setOnClickListener {
                    val intent = Intent(context, CodeEditorActivity::class.java).apply {
                        putExtra(CodeEditorActivity.EXTRA_FILE_PATH, path)
                    }
                    startActivity(intent)
                }
            }
            llRecentFiles.addView(tv)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_STORAGE
                )
            }
        }
    }

    private fun checkSetupStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val isSetup = TermuxBridge.isModelInstalled(this@MainActivity)

            withContext(Dispatchers.Main) {
                if (isSetup) {
                    showReadyState()
                } else {
                    showInstallState()
                }
            }
        }
    }

    private fun showReadyState() {
        statusText.text = getString(R.string.status_ready)
        progressText.text = getString(R.string.progress_ready)
        actionButton.text = getString(R.string.action_start)
        actionButton.setOnClickListener {
            startService(Intent(this, ModelService::class.java))
            startActivity(Intent(this, CodeEditorActivity::class.java))
        }
    }

    private fun showInstallState() {
        statusText.text = getString(R.string.app_name)
        progressText.text = getString(R.string.progress_install_prompt)
        actionButton.text = getString(R.string.action_install)
        actionButton.setOnClickListener { installMythos() }
    }

    private fun installMythos() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                actionButton.isEnabled = false
            }

            TermuxBridge.installModel(this@MainActivity) { progress ->
                CoroutineScope(Dispatchers.Main).launch {
                    progressText.text = progress
                }
            }

            withContext(Dispatchers.Main) {
                progressText.text = getString(R.string.progress_complete)
                actionButton.isEnabled = true
                showReadyState()
            }
        }
    }

    companion object {
        private const val REQUEST_STORAGE = 100
    }
}
