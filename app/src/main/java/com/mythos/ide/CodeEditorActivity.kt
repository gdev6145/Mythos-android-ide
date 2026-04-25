package com.mythos.ide

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CodeEditorActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var currentFilePath: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        webView = findViewById(R.id.codeEditor)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(EditorBridge(), "AndroidBridge")

        currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH)

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val fontSize = prefs.getInt(SettingsActivity.KEY_FONT_SIZE, SettingsActivity.DEFAULT_FONT_SIZE)
        val wordWrap = prefs.getBoolean(SettingsActivity.KEY_WORD_WRAP, true)
        val lineNumbers = prefs.getBoolean(SettingsActivity.KEY_LINE_NUMBERS, true)

        val initialContent = loadFileContent()
        val fileName = currentFilePath?.let { File(it).name } ?: "untitled"

        val html = buildEditorHtml(
            content = initialContent,
            fileName = fileName,
            fontSize = fontSize,
            wordWrap = wordWrap,
            lineNumbers = lineNumbers
        )

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun loadFileContent(): String {
        val path = currentFilePath ?: return DEFAULT_CONTENT
        return try {
            File(path).readText()
        } catch (_: Exception) {
            DEFAULT_CONTENT
        }
    }

    private fun saveFileContent(content: String) {
        val path = currentFilePath
        if (path == null) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.error_no_file_open), Toast.LENGTH_SHORT).show()
            }
            return
        }
        try {
            File(path).writeText(content)
            runOnUiThread {
                Toast.makeText(this, getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.error_save_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun save(content: String) {
            saveFileContent(content)
        }

        @JavascriptInterface
        fun getFilePath(): String {
            return currentFilePath ?: ""
        }
    }

    companion object {
        const val EXTRA_FILE_PATH = "file_path"

        private const val DEFAULT_CONTENT = "# MYTHOS Android IDE\n\nprint(\"Hello from MYTHOS!\")\n\n# Start coding here...\n"

        private fun buildEditorHtml(
            content: String,
            fileName: String,
            fontSize: Int,
            wordWrap: Boolean,
            lineNumbers: Boolean
        ): String {
            val escapedContent = content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

            val wrapStyle = if (wordWrap) "pre-wrap" else "pre"

            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>MYTHOS Editor</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: 'Courier New', Consolas, monospace;
                        background: #1e1e1e;
                        color: #d4d4d4;
                        height: 100vh;
                        display: flex;
                        flex-direction: column;
                        overflow: hidden;
                    }
                    .toolbar {
                        background: #252526;
                        padding: 6px 12px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        border-bottom: 1px solid #3c3c3c;
                        flex-shrink: 0;
                    }
                    .toolbar .filename {
                        font-size: 13px;
                        color: #cccccc;
                        flex: 1;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                    .toolbar button {
                        background: #0e639c;
                        color: #fff;
                        border: none;
                        padding: 4px 12px;
                        font-size: 12px;
                        border-radius: 3px;
                        cursor: pointer;
                    }
                    .toolbar button:active {
                        background: #1177bb;
                    }
                    .editor-container {
                        flex: 1;
                        display: flex;
                        overflow: auto;
                    }
                    .line-numbers {
                        display: ${if (lineNumbers) "block" else "none"};
                        background: #1e1e1e;
                        color: #858585;
                        padding: 12px 8px 12px 12px;
                        text-align: right;
                        font-size: ${fontSize}px;
                        line-height: 1.5;
                        user-select: none;
                        flex-shrink: 0;
                        min-width: 40px;
                        border-right: 1px solid #3c3c3c;
                    }
                    .editor {
                        flex: 1;
                        padding: 12px;
                        font-size: ${fontSize}px;
                        line-height: 1.5;
                        white-space: $wrapStyle;
                        outline: none;
                        tab-size: 4;
                        -webkit-tab-size: 4;
                        overflow: auto;
                        color: #d4d4d4;
                        caret-color: #aeafad;
                    }
                    .editor:focus { outline: none; }
                    .status-bar {
                        background: #007acc;
                        padding: 2px 12px;
                        font-size: 11px;
                        color: #fff;
                        display: flex;
                        justify-content: space-between;
                        flex-shrink: 0;
                    }

                    /* Basic syntax highlighting via CSS classes */
                    .keyword { color: #569cd6; }
                    .string { color: #ce9178; }
                    .comment { color: #6a9955; }
                    .number { color: #b5cea8; }
                    .function { color: #dcdcaa; }
                </style>
            </head>
            <body>
                <div class="toolbar">
                    <span class="filename">${fileName}</span>
                    <button onclick="saveFile()">Save</button>
                </div>
                <div class="editor-container">
                    <div class="line-numbers" id="lineNumbers"></div>
                    <div class="editor" id="editor" contenteditable="true" spellcheck="false">${escapedContent}</div>
                </div>
                <div class="status-bar">
                    <span id="cursorInfo">Ln 1, Col 1</span>
                    <span id="fileInfo">${fileName}</span>
                </div>

                <script>
                    var editor = document.getElementById('editor');
                    var lineNumbers = document.getElementById('lineNumbers');
                    var cursorInfo = document.getElementById('cursorInfo');
                    var modified = false;

                    function updateLineNumbers() {
                        var text = editor.innerText || '';
                        var lines = text.split('\n');
                        var nums = '';
                        for (var i = 1; i <= lines.length; i++) {
                            nums += i + '\n';
                        }
                        lineNumbers.textContent = nums;
                    }

                    function updateCursorInfo() {
                        var sel = window.getSelection();
                        if (!sel.rangeCount) return;
                        var text = editor.innerText || '';
                        var range = sel.getRangeAt(0);
                        var preRange = document.createRange();
                        preRange.setStart(editor, 0);
                        preRange.setEnd(range.startContainer, range.startOffset);
                        var preText = preRange.toString();
                        var lines = preText.split('\n');
                        var line = lines.length;
                        var col = lines[lines.length - 1].length + 1;
                        cursorInfo.textContent = 'Ln ' + line + ', Col ' + col;
                    }

                    function saveFile() {
                        var content = editor.innerText;
                        if (window.AndroidBridge) {
                            window.AndroidBridge.save(content);
                            modified = false;
                            updateTitle();
                        }
                    }

                    function updateTitle() {
                        var fn = document.querySelector('.filename');
                        var name = '${fileName}';
                        fn.textContent = modified ? name + ' *' : name;
                    }

                    editor.addEventListener('input', function() {
                        updateLineNumbers();
                        modified = true;
                        updateTitle();
                    });

                    editor.addEventListener('keyup', updateCursorInfo);
                    editor.addEventListener('click', updateCursorInfo);

                    editor.addEventListener('keydown', function(e) {
                        if (e.key === 'Tab') {
                            e.preventDefault();
                            document.execCommand('insertText', false, '    ');
                        }
                        if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                            e.preventDefault();
                            saveFile();
                        }
                    });

                    // Sync scroll between editor and line numbers
                    document.querySelector('.editor-container').addEventListener('scroll', function() {
                        lineNumbers.style.transform = 'translateY(-' + this.scrollTop + 'px)';
                    });

                    updateLineNumbers();
                </script>
            </body>
            </html>
            """.trimIndent()
        }
    }
}
