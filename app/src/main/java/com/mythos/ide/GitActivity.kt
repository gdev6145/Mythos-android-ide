package com.mythos.ide

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class GitActivity : AppCompatActivity() {

    private lateinit var tvBranch: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private var projectDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_git)

        tvBranch = findViewById(R.id.tvBranch)
        tvStatus = findViewById(R.id.tvGitStatus)
        tvLog = findViewById(R.id.tvGitLog)

        val btnBack = findViewById<ImageButton>(R.id.btnGitBack)
        val btnRefresh = findViewById<ImageButton>(R.id.btnGitRefresh)
        val btnInit = findViewById<Button>(R.id.btnGitInit)
        val btnAdd = findViewById<Button>(R.id.btnGitAdd)
        val btnCommit = findViewById<Button>(R.id.btnGitCommit)

        val path = intent.getStringExtra(EXTRA_PROJECT_PATH)
        projectDir = if (path != null) File(path) else getExternalFilesDir(null)

        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { refreshStatus() }
        btnInit.setOnClickListener { gitInit() }
        btnAdd.setOnClickListener { gitAddAll() }
        btnCommit.setOnClickListener { showCommitDialog() }

        refreshStatus()
    }

    private fun refreshStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val branch = runGit("rev-parse", "--abbrev-ref", "HEAD")
            val status = runGit("status", "--short")
            val log = runGit("log", "--oneline", "-15")

            withContext(Dispatchers.Main) {
                if (branch.startsWith("fatal:")) {
                    tvBranch.text = getString(R.string.git_not_initialized)
                    tvStatus.text = getString(R.string.git_init_hint)
                    tvLog.text = ""
                } else {
                    tvBranch.text = getString(R.string.git_branch_format, branch.trim())
                    tvStatus.text = status.ifEmpty { getString(R.string.git_clean) }
                    tvLog.text = log.ifEmpty { getString(R.string.git_no_commits) }
                }
            }
        }
    }

    private fun gitInit() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runGit("init")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GitActivity, result.trim(), Toast.LENGTH_SHORT).show()
                refreshStatus()
            }
        }
    }

    private fun gitAddAll() {
        CoroutineScope(Dispatchers.IO).launch {
            runGit("add", "-A")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GitActivity, getString(R.string.git_added), Toast.LENGTH_SHORT).show()
                refreshStatus()
            }
        }
    }

    private fun showCommitDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.git_commit_message_hint)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.git_commit))
            .setView(input)
            .setPositiveButton(getString(R.string.git_commit)) { _, _ ->
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    gitCommit(message)
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun gitCommit(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runGit("commit", "-m", message)
            withContext(Dispatchers.Main) {
                val summary = result.lines().firstOrNull() ?: result
                Toast.makeText(this@GitActivity, summary.trim(), Toast.LENGTH_SHORT).show()
                refreshStatus()
            }
        }
    }

    private fun runGit(vararg args: String): String {
        return try {
            val cmd = listOf("git") + args.toList()
            val process = ProcessBuilder(cmd)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    companion object {
        const val EXTRA_PROJECT_PATH = "project_path"
    }
}
