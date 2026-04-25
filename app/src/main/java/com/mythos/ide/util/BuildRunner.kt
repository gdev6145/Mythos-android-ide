package com.mythos.ide.util

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Detects project type and provides run commands for different project types.
 * Runs build/execute commands in the project directory and streams output.
 */
object BuildRunner {

    data class ProjectType(
        val name: String,
        val runCommand: List<String>,
        val buildCommand: List<String>? = null
    )

    /** Detect the project type from the contents of a directory. */
    fun detectProjectType(projectDir: File): ProjectType? {
        return when {
            File(projectDir, "build.gradle").exists() || File(projectDir, "build.gradle.kts").exists() ->
                ProjectType(
                    name = "Gradle/Android",
                    runCommand = listOf("./gradlew", "assembleDebug"),
                    buildCommand = listOf("./gradlew", "build")
                )
            File(projectDir, "pom.xml").exists() ->
                ProjectType(
                    name = "Maven",
                    runCommand = listOf("mvn", "compile", "exec:java"),
                    buildCommand = listOf("mvn", "package")
                )
            File(projectDir, "package.json").exists() ->
                ProjectType(
                    name = "Node.js",
                    runCommand = listOf("node", findMainJs(projectDir)),
                    buildCommand = listOf("npm", "install")
                )
            File(projectDir, "requirements.txt").exists() || findPythonMain(projectDir) != null ->
                ProjectType(
                    name = "Python",
                    runCommand = listOf("python3", findPythonMain(projectDir) ?: "main.py")
                )
            findMainKt(projectDir) != null ->
                ProjectType(
                    name = "Kotlin Script",
                    runCommand = listOf("kotlinc", "-script", findMainKt(projectDir)!!)
                )
            File(projectDir, "index.html").exists() ->
                ProjectType(
                    name = "Static Web",
                    runCommand = listOf("python3", "-m", "http.server", "8080")
                )
            File(projectDir, "Makefile").exists() ->
                ProjectType(
                    name = "Make",
                    runCommand = listOf("make", "run"),
                    buildCommand = listOf("make")
                )
            else -> null
        }
    }

    /**
     * Execute a command in the given directory and return the output line by line
     * via the callback. Returns the exit code.
     */
    fun execute(
        command: List<String>,
        workingDir: File,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit
    ): Int {
        return try {
            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                onOutput(line!!)
            }

            process.waitFor()
        } catch (e: Exception) {
            onError("Build error: ${e.message}")
            -1
        }
    }

    /** Get a display-friendly command string. */
    fun commandToString(command: List<String>): String {
        return command.joinToString(" ")
    }

    private fun findMainJs(dir: File): String {
        // Check package.json for "main" field
        val pkgJson = File(dir, "package.json")
        if (pkgJson.exists()) {
            val content = pkgJson.readText()
            val mainMatch = Regex("\"main\"\\s*:\\s*\"([^\"]+)\"").find(content)
            if (mainMatch != null) return mainMatch.groupValues[1]
        }
        // Fallback
        return when {
            File(dir, "index.js").exists() -> "index.js"
            File(dir, "app.js").exists() -> "app.js"
            File(dir, "main.js").exists() -> "main.js"
            File(dir, "src/index.js").exists() -> "src/index.js"
            else -> "index.js"
        }
    }

    private fun findPythonMain(dir: File): String? {
        return when {
            File(dir, "main.py").exists() -> "main.py"
            File(dir, "app.py").exists() -> "app.py"
            File(dir, "__main__.py").exists() -> "__main__.py"
            File(dir, "src/main.py").exists() -> "src/main.py"
            else -> null
        }
    }

    private fun findMainKt(dir: File): String? {
        return dir.walkTopDown()
            .filter { it.extension == "kt" || it.extension == "kts" }
            .firstOrNull()?.relativeTo(dir)?.path
    }
}
