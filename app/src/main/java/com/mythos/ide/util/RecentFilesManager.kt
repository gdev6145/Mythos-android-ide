package com.mythos.ide.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.io.File

/**
 * Tracks recently opened files and persists them via SharedPreferences.
 * The list is capped at [MAX_RECENT] entries and ordered most-recent-first.
 */
object RecentFilesManager {

    private const val PREFS_NAME = "mythos_recent_files"
    private const val KEY_FILES = "recent_files"
    private const val MAX_RECENT = 20

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Record that a file was opened. Moves it to the top if already present. */
    fun addFile(context: Context, path: String) {
        val list = getRecentFiles(context).toMutableList()
        list.remove(path)
        list.add(0, path)
        if (list.size > MAX_RECENT) {
            list.subList(MAX_RECENT, list.size).clear()
        }
        save(context, list)
    }

    /** Return the list of recent file paths, most-recent first. */
    fun getRecentFiles(context: Context): List<String> {
        val json = prefs(context).getString(KEY_FILES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Return only the files that still exist on disk. */
    fun getExistingRecentFiles(context: Context): List<String> {
        return getRecentFiles(context).filter { File(it).exists() }
    }

    /** Remove a single entry (e.g. after deletion). */
    fun removeFile(context: Context, path: String) {
        val list = getRecentFiles(context).toMutableList()
        list.remove(path)
        save(context, list)
    }

    /** Clear all recent files. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_FILES).apply()
    }

    private fun save(context: Context, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_FILES, arr.toString()).apply()
    }
}
