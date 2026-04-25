package com.mythos.ide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class NewProjectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project)

        val etName = findViewById<EditText>(R.id.etProjectName)
        val rgTemplate = findViewById<RadioGroup>(R.id.rgTemplate)
        val btnCreate = findViewById<Button>(R.id.btnCreateProject)
        val btnBack = findViewById<ImageButton>(R.id.btnNewProjectBack)

        btnBack.setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_project_name_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_-]*$"))) {
                Toast.makeText(this, getString(R.string.error_project_name_invalid), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val template = when (rgTemplate.checkedRadioButtonId) {
                R.id.rbKotlinConsole -> Template.KOTLIN_CONSOLE
                R.id.rbPythonScript -> Template.PYTHON_SCRIPT
                R.id.rbWebProject -> Template.WEB_PROJECT
                R.id.rbEmptyProject -> Template.EMPTY
                else -> Template.EMPTY
            }

            createProject(name, template)
        }
    }

    private fun createProject(name: String, template: Template) {
        val projectsDir = File(getExternalFilesDir(null), "projects")
        val projectDir = File(projectsDir, name)

        if (projectDir.exists()) {
            Toast.makeText(this, getString(R.string.error_project_exists), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            projectDir.mkdirs()

            when (template) {
                Template.KOTLIN_CONSOLE -> createKotlinProject(projectDir, name)
                Template.PYTHON_SCRIPT -> createPythonProject(projectDir, name)
                Template.WEB_PROJECT -> createWebProject(projectDir, name)
                Template.EMPTY -> createEmptyProject(projectDir, name)
            }

            Toast.makeText(this, getString(R.string.project_created, name), Toast.LENGTH_SHORT).show()

            // Open in file explorer
            val intent = Intent(this, FileExplorerActivity::class.java).apply {
                putExtra(FileExplorerActivity.EXTRA_START_PATH, projectDir.absolutePath)
            }
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_create_project, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun createKotlinProject(dir: File, name: String) {
        File(dir, "src").mkdirs()

        File(dir, "src/Main.kt").writeText("""
            |fun main() {
            |    println("Hello from $name!")
            |}
        """.trimMargin() + "\n")

        File(dir, "README.md").writeText("""
            |# $name
            |
            |A Kotlin console project created with MYTHOS IDE.
            |
            |## Run
            |
            |```bash
            |kotlinc src/Main.kt -include-runtime -d $name.jar
            |java -jar $name.jar
            |```
        """.trimMargin() + "\n")

        File(dir, ".gitignore").writeText("""
            |*.class
            |*.jar
            |*.log
            |build/
            |out/
        """.trimMargin() + "\n")
    }

    private fun createPythonProject(dir: File, name: String) {
        File(dir, "main.py").writeText("""
            |#!/usr/bin/env python3
            |"""".trimMargin() + "\"\"\"$name - created with MYTHOS IDE.\"\"\"\n\n" + """
            |
            |def main():
            |    print("Hello from $name!")
            |
            |
            |if __name__ == "__main__":
            |    main()
        """.trimMargin() + "\n")

        File(dir, "requirements.txt").writeText("# Add dependencies here\n")

        File(dir, "README.md").writeText("""
            |# $name
            |
            |A Python project created with MYTHOS IDE.
            |
            |## Run
            |
            |```bash
            |python3 main.py
            |```
        """.trimMargin() + "\n")

        File(dir, ".gitignore").writeText("""
            |__pycache__/
            |*.pyc
            |.venv/
            |*.egg-info/
        """.trimMargin() + "\n")
    }

    private fun createWebProject(dir: File, name: String) {
        File(dir, "css").mkdirs()
        File(dir, "js").mkdirs()

        File(dir, "index.html").writeText("""
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |    <meta charset="UTF-8">
            |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
            |    <title>$name</title>
            |    <link rel="stylesheet" href="css/style.css">
            |</head>
            |<body>
            |    <h1>$name</h1>
            |    <p>Created with MYTHOS IDE</p>
            |    <script src="js/app.js"></script>
            |</body>
            |</html>
        """.trimMargin() + "\n")

        File(dir, "css/style.css").writeText("""
            |* {
            |    box-sizing: border-box;
            |    margin: 0;
            |    padding: 0;
            |}
            |
            |body {
            |    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            |    line-height: 1.6;
            |    padding: 2rem;
            |    max-width: 800px;
            |    margin: 0 auto;
            |    color: #333;
            |}
            |
            |h1 {
            |    margin-bottom: 1rem;
            |    color: #6200ee;
            |}
        """.trimMargin() + "\n")

        File(dir, "js/app.js").writeText("""
            |// $name - created with MYTHOS IDE
            |
            |document.addEventListener('DOMContentLoaded', function() {
            |    console.log('$name loaded');
            |});
        """.trimMargin() + "\n")

        File(dir, "README.md").writeText("""
            |# $name
            |
            |A web project created with MYTHOS IDE.
            |
            |## Run
            |
            |Open `index.html` in a browser.
        """.trimMargin() + "\n")
    }

    private fun createEmptyProject(dir: File, name: String) {
        File(dir, "README.md").writeText("# $name\n\nCreated with MYTHOS IDE.\n")
        File(dir, ".gitignore").writeText("")
    }

    enum class Template {
        KOTLIN_CONSOLE,
        PYTHON_SCRIPT,
        WEB_PROJECT,
        EMPTY
    }
}
