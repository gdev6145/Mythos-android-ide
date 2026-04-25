package com.mythos.ide.util

import java.io.File

/**
 * Maps file extensions to Android built-in drawable resource IDs for
 * visual differentiation in the file explorer.
 */
object FileIcons {

    fun getIconResource(file: File): Int {
        if (file.isDirectory) return android.R.drawable.ic_menu_more

        return when (file.extension.lowercase()) {
            // Source code
            "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx",
            "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb", "php",
            "swift", "dart" -> android.R.drawable.ic_menu_edit

            // Markup / config
            "xml", "html", "htm", "css", "json", "yaml", "yml",
            "toml", "ini", "cfg", "conf" -> android.R.drawable.ic_menu_agenda

            // Build files
            "gradle", "properties", "pro", "mk", "cmake" ->
                android.R.drawable.ic_menu_manage

            // Documents
            "md", "txt", "rtf", "csv", "log" ->
                android.R.drawable.ic_menu_my_calendar

            // Media
            "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp",
            "ico" -> android.R.drawable.ic_menu_gallery

            // Archives
            "zip", "tar", "gz", "rar", "7z", "jar", "aar",
            "apk" -> android.R.drawable.ic_menu_save

            // Shell
            "sh", "bash", "zsh", "bat", "cmd", "ps1" ->
                android.R.drawable.ic_menu_set_as

            // Git
            "gitignore", "gitattributes", "gitmodules" ->
                android.R.drawable.ic_menu_rotate

            else -> android.R.drawable.ic_menu_info_details
        }
    }

    fun getIconTintColor(file: File): Int {
        if (file.isDirectory) return 0xFFFFA000.toInt() // amber

        return when (file.extension.lowercase()) {
            "kt", "kts" -> 0xFF7C4DFF.toInt()   // purple (Kotlin)
            "java" -> 0xFFE65100.toInt()          // orange (Java)
            "py" -> 0xFF2196F3.toInt()            // blue (Python)
            "js", "jsx" -> 0xFFFDD835.toInt()     // yellow (JS)
            "ts", "tsx" -> 0xFF1976D2.toInt()     // dark blue (TS)
            "xml", "html", "htm" -> 0xFFFF5722.toInt() // deep orange
            "json" -> 0xFF66BB6A.toInt()          // green
            "css" -> 0xFF42A5F5.toInt()           // light blue
            "md", "txt" -> 0xFF90A4AE.toInt()     // grey
            "gradle" -> 0xFF00897B.toInt()        // teal
            "sh", "bash" -> 0xFF4CAF50.toInt()    // green
            "png", "jpg", "jpeg", "gif", "svg" -> 0xFFAB47BC.toInt() // purple
            else -> 0xFF78909C.toInt()            // blue grey
        }
    }
}
