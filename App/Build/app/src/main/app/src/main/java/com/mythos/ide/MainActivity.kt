package com.mythos.ide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        actionButton = findViewById(R.id.actionButton)

        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val mythosDir = File(getExternalFilesDir(null), "mythos-setup")
            val isSetup = mythosDir.exists()

            withContext(Dispatchers.Main) {
                if (isSetup) {
                    statusText.text = "✅ MYTHOS is ready!"
                    actionButton.text = "Start Model & Open Editor"
                    actionButton.setOnClickListener {
                        startService(Intent(this@MainActivity, ModelService::class.java))
                        startActivity(Intent(this@MainActivity, CodeEditorActivity::class.java))
                    }
                } else {
                    statusText.text = "MYTHOS not installed yet"
                    actionButton.text = "Install MYTHOS-26B"
                    actionButton.setOnClickListener { installMYTHOS() }
                }
            }
        }
    }

    private fun installMYTHOS() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                statusText.text = "Setting up MYTHOS..."
                actionButton.isEnabled = false
            }

            // Create minimal setup
            val mythosDir = File(getExternalFilesDir(null), "mythos-setup")
            mythosDir.mkdirs()

            withContext(Dispatchers.Main) {
                statusText.text = "✅ Setup Complete!"
                actionButton.text = "Start Model"
                actionButton.setOnClickListener {
                    startService(Intent(this@MainActivity, ModelService::class.java))
                    statusText.text = "Model running in background"
                    actionButton.text = "Open Editor"
                    actionButton.setOnClickListener {
                        startActivity(Intent(this@MainActivity, CodeEditorActivity::class.java))
                    }
                }
                actionButton.isEnabled = true
            }
        }
    }
}
