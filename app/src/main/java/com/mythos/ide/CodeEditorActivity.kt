package com.mythos.ide

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class CodeEditorActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val webView = findViewById<WebView>(R.id.codeEditor)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.loadData(
            EDITOR_HTML,
            "text/html",
            "UTF-8"
        )
    }

    companion object {
        private const val EDITOR_HTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MYTHOS Code Editor</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: 'Courier New', monospace;
                        background: #1e1e1e;
                        color: #d4d4d4;
                        height: 100vh;
                        display: flex;
                        flex-direction: column;
                    }
                    .toolbar {
                        background: #333;
                        padding: 8px 12px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        border-bottom: 1px solid #555;
                    }
                    .toolbar span {
                        font-size: 14px;
                        color: #ccc;
                    }
                    .editor {
                        flex: 1;
                        padding: 16px;
                        font-size: 14px;
                        line-height: 1.6;
                        overflow: auto;
                        white-space: pre-wrap;
                        outline: none;
                        tab-size: 4;
                    }
                    .editor:focus {
                        outline: none;
                    }
                </style>
            </head>
            <body>
                <div class="toolbar">
                    <span>MYTHOS Editor</span>
                </div>
                <div class="editor" contenteditable="true" spellcheck="false">
# MYTHOS Android IDE

print("Hello from MYTHOS!")

# Start coding here...
                </div>
            </body>
            </html>
        """
    }
}
