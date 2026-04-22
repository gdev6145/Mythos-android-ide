package com.mythos.ide

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class CodeEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val webView = findViewById<WebView>(R.id.codeEditor)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.loadData("""
            <!DOCTYPE html>
            <html>
            <head><title>MYTHOS Code Editor</title>
            <style>body{margin:0;padding:0;font-family:monospace;background:#f5f5f5;}</style>
            </head>
            <body>
                <div style="padding:20px;height:100vh;overflow:auto;" contenteditable="true">
                    # MYTHOS Android IDE<br><br>
                    print("Hello from MYTHOS!")<br><br>
                    # Start coding here...
                </div>
            </body>
            </html>
        """.trimIndent(), "text/html", "UTF-8")
    }
}
