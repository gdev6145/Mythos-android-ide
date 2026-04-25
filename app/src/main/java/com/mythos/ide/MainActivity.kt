package com.mythos.ide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mythos.ide.services.ModelService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var actionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        progressText = findViewById(R.id.progressText)
        actionButton = findViewById(R.id.actionButton)

        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val mythosDir = File(getExternalFilesDir(null), "mythos-setup")
            val isSetup = mythosDir.exists()

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
                progressText.text = getString(R.string.progress_installing)
                actionButton.isEnabled = false
            }

            val mythosDir = File(getExternalFilesDir(null), "mythos-setup")
            mythosDir.mkdirs()

            withContext(Dispatchers.Main) {
                progressText.text = getString(R.string.progress_complete)
                actionButton.isEnabled = true
                showReadyState()
            }
        }
    }
}
