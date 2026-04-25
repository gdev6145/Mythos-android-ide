package com.mythos.ide

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileExplorerActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var tvCurrentPath: TextView
    private lateinit var adapter: FileAdapter

    private var currentDir: File = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explorer)

        rvFiles = findViewById(R.id.rvFiles)
        tvCurrentPath = findViewById(R.id.tvCurrentPath)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnNewFile = findViewById<ImageButton>(R.id.btnNewFile)

        adapter = FileAdapter(
            onFileClick = { file -> openFile(file) },
            onDirClick = { dir -> navigateTo(dir) }
        )

        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = adapter

        btnBack.setOnClickListener { navigateUp() }
        btnNewFile.setOnClickListener { showNewFileDialog() }

        val startPath = intent.getStringExtra(EXTRA_START_PATH)
        if (startPath != null) {
            currentDir = File(startPath)
        } else {
            val projectDir = getExternalFilesDir(null)
            if (projectDir != null) {
                currentDir = projectDir
            }
        }

        navigateTo(currentDir)
    }

    private fun navigateTo(dir: File) {
        if (!dir.exists() || !dir.isDirectory) {
            Toast.makeText(this, getString(R.string.error_cannot_open_dir), Toast.LENGTH_SHORT).show()
            return
        }
        currentDir = dir
        tvCurrentPath.text = dir.absolutePath

        val files = dir.listFiles()?.toList() ?: emptyList()
        val sorted = files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        adapter.submitList(sorted)
    }

    private fun navigateUp() {
        val parent = currentDir.parentFile
        if (parent != null && parent.canRead()) {
            navigateTo(parent)
        } else {
            finish()
        }
    }

    private fun openFile(file: File) {
        if (!file.canRead()) {
            Toast.makeText(this, getString(R.string.error_cannot_read_file), Toast.LENGTH_SHORT).show()
            return
        }

        if (isTextFile(file)) {
            val intent = Intent(this, CodeEditorActivity::class.java).apply {
                putExtra(CodeEditorActivity.EXTRA_FILE_PATH, file.absolutePath)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.error_binary_file), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNewFileDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.hint_filename)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_new_file_title))
            .setView(input)
            .setPositiveButton(getString(R.string.dialog_create)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createNewFile(name)
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun createNewFile(name: String) {
        val newFile = File(currentDir, name)
        try {
            if (name.endsWith("/")) {
                val dir = File(currentDir, name.trimEnd('/'))
                dir.mkdirs()
            } else {
                newFile.createNewFile()
            }
            navigateTo(currentDir)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_create_file, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_START_PATH = "start_path"

        private val TEXT_EXTENSIONS = setOf(
            "kt", "java", "xml", "json", "txt", "md", "gradle", "properties",
            "py", "js", "ts", "html", "css", "sh", "bash", "yml", "yaml",
            "toml", "cfg", "ini", "conf", "pro", "gitignore", "editorconfig"
        )

        fun isTextFile(file: File): Boolean {
            val ext = file.extension.lowercase()
            if (ext in TEXT_EXTENSIONS) return true
            if (file.name.startsWith(".") && !file.name.contains(".")) return true
            if (file.length() > 2 * 1024 * 1024) return false

            return try {
                file.bufferedReader().use { reader ->
                    val buffer = CharArray(512)
                    val read = reader.read(buffer)
                    if (read <= 0) return true
                    buffer.take(read).none { it == '\u0000' }
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onDirClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private var items: List<File> = emptyList()

    fun submitList(list: List<File>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val ivIcon: ImageView = root.findViewById(R.id.ivFileIcon)
        private val tvName: TextView = root.findViewById(R.id.tvFileName)
        private val tvInfo: TextView = root.findViewById(R.id.tvFileInfo)

        fun bind(file: File) {
            tvName.text = file.name

            if (file.isDirectory) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_more)
                val count = file.listFiles()?.size ?: 0
                tvInfo.text = root.context.getString(R.string.file_info_dir, count)
                root.setOnClickListener { onDirClick(file) }
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_menu_edit)
                tvInfo.text = formatFileSize(file.length())
                root.setOnClickListener { onFileClick(file) }
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }
    }
}
