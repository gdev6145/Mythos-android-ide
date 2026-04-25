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
        val language = detectLanguage(fileName)

        val html = buildEditorHtml(
            content = initialContent,
            fileName = fileName,
            fontSize = fontSize,
            wordWrap = wordWrap,
            lineNumbers = lineNumbers,
            language = language
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

        private fun detectLanguage(fileName: String): String {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "kt", "kts" -> "kotlin"
                "java" -> "java"
                "py" -> "python"
                "js", "jsx" -> "javascript"
                "ts", "tsx" -> "typescript"
                "xml", "html", "htm" -> "xml"
                "json" -> "json"
                "css" -> "css"
                "sh", "bash" -> "shell"
                "gradle" -> "gradle"
                "md", "markdown" -> "markdown"
                "yaml", "yml" -> "yaml"
                else -> "plain"
            }
        }

        private fun buildEditorHtml(
            content: String,
            fileName: String,
            fontSize: Int,
            wordWrap: Boolean,
            lineNumbers: Boolean,
            language: String
        ): String {
            val escapedContent = content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

            val wrapStyle = if (wordWrap) "pre-wrap" else "pre"
            val lineNumsDisplay = if (lineNumbers) "block" else "none"

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
            flex-shrink: 0;
        }
        .toolbar button:active { background: #1177bb; }
        .toolbar button.secondary { background: #3c3c3c; }
        .toolbar button.secondary:active { background: #505050; }

        /* Search bar */
        .search-bar {
            display: none;
            background: #252526;
            padding: 6px 12px;
            border-bottom: 1px solid #3c3c3c;
            gap: 6px;
            align-items: center;
            flex-shrink: 0;
        }
        .search-bar.visible { display: flex; }
        .search-bar input {
            background: #3c3c3c;
            border: 1px solid #555;
            color: #d4d4d4;
            padding: 4px 8px;
            font-size: 13px;
            border-radius: 3px;
            outline: none;
            flex: 1;
            min-width: 0;
        }
        .search-bar input:focus { border-color: #0e639c; }
        .search-bar .search-info {
            color: #888;
            font-size: 12px;
            white-space: nowrap;
        }
        .search-bar button {
            background: #3c3c3c;
            color: #ccc;
            border: none;
            padding: 4px 8px;
            font-size: 12px;
            border-radius: 3px;
            cursor: pointer;
            flex-shrink: 0;
        }
        .search-bar button:active { background: #505050; }

        .editor-wrapper {
            flex: 1;
            display: flex;
            overflow: hidden;
            position: relative;
        }
        .line-numbers {
            display: $lineNumsDisplay;
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
            overflow: hidden;
        }
        .editor-scroll {
            flex: 1;
            overflow: auto;
        }
        .editor {
            min-height: 100%;
            padding: 12px;
            font-size: ${fontSize}px;
            line-height: 1.5;
            white-space: $wrapStyle;
            outline: none;
            tab-size: 4;
            -webkit-tab-size: 4;
            color: #d4d4d4;
            caret-color: #aeafad;
        }
        .editor:focus { outline: none; }

        /* Highlighted overlay for syntax */
        .highlighted {
            position: absolute;
            top: 0; left: 0; right: 0;
            padding: 12px;
            font-size: ${fontSize}px;
            line-height: 1.5;
            white-space: $wrapStyle;
            tab-size: 4;
            -webkit-tab-size: 4;
            pointer-events: none;
            color: transparent;
        }

        .status-bar {
            background: #007acc;
            padding: 2px 12px;
            font-size: 11px;
            color: #fff;
            display: flex;
            justify-content: space-between;
            flex-shrink: 0;
        }

        /* Syntax colors */
        .syn-keyword { color: #569cd6; }
        .syn-type { color: #4ec9b0; }
        .syn-string { color: #ce9178; }
        .syn-comment { color: #6a9955; }
        .syn-number { color: #b5cea8; }
        .syn-function { color: #dcdcaa; }
        .syn-annotation { color: #d7ba7d; }
        .syn-operator { color: #d4d4d4; }
        .syn-tag { color: #569cd6; }
        .syn-attr { color: #9cdcfe; }
        .syn-punctuation { color: #808080; }

        /* Search highlight */
        mark { background: #515c6a; color: inherit; border-radius: 2px; }
        mark.current { background: #613214; }
    </style>
</head>
<body>
    <div class="toolbar">
        <span class="filename" id="fileLabel">$fileName</span>
        <button class="secondary" onclick="toggleSearch()">Find</button>
        <button onclick="saveFile()">Save</button>
    </div>
    <div class="search-bar" id="searchBar">
        <input type="text" id="searchInput" placeholder="Find..." oninput="doSearch()" />
        <input type="text" id="replaceInput" placeholder="Replace..." />
        <span class="search-info" id="searchInfo"></span>
        <button onclick="findNext()">Next</button>
        <button onclick="findPrev()">Prev</button>
        <button onclick="replaceCurrent()">Replace</button>
        <button onclick="replaceAll()">All</button>
        <button onclick="toggleSearch()">X</button>
    </div>
    <div class="editor-wrapper">
        <div class="line-numbers" id="lineNumbers"></div>
        <div class="editor-scroll" id="editorScroll">
            <div class="editor" id="editor" contenteditable="true" spellcheck="false"></div>
        </div>
    </div>
    <div class="status-bar">
        <span id="cursorInfo">Ln 1, Col 1</span>
        <span id="langInfo">$language</span>
    </div>

    <script>
    var editor = document.getElementById('editor');
    var lineNumbers = document.getElementById('lineNumbers');
    var cursorInfo = document.getElementById('cursorInfo');
    var editorScroll = document.getElementById('editorScroll');
    var searchBar = document.getElementById('searchBar');
    var searchInput = document.getElementById('searchInput');
    var replaceInput = document.getElementById('replaceInput');
    var searchInfo = document.getElementById('searchInfo');
    var fileLabel = document.getElementById('fileLabel');
    var modified = false;
    var language = '$language';
    var searchMatches = [];
    var currentMatchIndex = -1;

    // Set initial content
    editor.textContent = ${escapeForJsString(escapedContent)};

    // ----- Syntax highlighting rules by language -----
    var rules = {
        kotlin: [
            { pattern: /(\/\/.*)/g, cls: 'syn-comment' },
            { pattern: /(\/\*[\s\S]*?\*\/)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"\\]|\\.)*")/g, cls: 'syn-string' },
            { pattern: /('(?:[^'\\]|\\.)')/g, cls: 'syn-string' },
            { pattern: /\b(fun|val|var|class|object|interface|if|else|when|for|while|do|return|import|package|override|private|public|protected|internal|open|abstract|sealed|data|companion|suspend|coroutine|lateinit|lazy|by|in|is|as|try|catch|finally|throw|null|true|false|this|super|it)\b/g, cls: 'syn-keyword' },
            { pattern: /\b(Int|String|Boolean|Long|Float|Double|Unit|Any|Nothing|List|Map|Set|Array)\b/g, cls: 'syn-type' },
            { pattern: /(@\w+)/g, cls: 'syn-annotation' },
            { pattern: /\b(\d+\.?\d*[fFL]?)\b/g, cls: 'syn-number' },
            { pattern: /\b(\w+)\s*\(/g, cls: 'syn-function', group: 1 }
        ],
        java: [
            { pattern: /(\/\/.*)/g, cls: 'syn-comment' },
            { pattern: /(\/\*[\s\S]*?\*\/)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"\\]|\\.)*")/g, cls: 'syn-string' },
            { pattern: /('(?:[^'\\]|\\.)')/g, cls: 'syn-string' },
            { pattern: /\b(public|private|protected|static|final|abstract|class|interface|extends|implements|if|else|for|while|do|return|import|package|void|new|try|catch|finally|throw|throws|null|true|false|this|super|synchronized|volatile|transient|native|instanceof|switch|case|default|break|continue|enum)\b/g, cls: 'syn-keyword' },
            { pattern: /\b(int|long|float|double|boolean|char|byte|short|String|Object|Integer|Long|Float|Double|Boolean|List|Map|Set|Array)\b/g, cls: 'syn-type' },
            { pattern: /(@\w+)/g, cls: 'syn-annotation' },
            { pattern: /\b(\d+\.?\d*[fFdDlL]?)\b/g, cls: 'syn-number' },
            { pattern: /\b(\w+)\s*\(/g, cls: 'syn-function', group: 1 }
        ],
        python: [
            { pattern: /(#.*)/g, cls: 'syn-comment' },
            { pattern: /("""[\s\S]*?"""|'''[\s\S]*?''')/g, cls: 'syn-string' },
            { pattern: /("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')/g, cls: 'syn-string' },
            { pattern: /\b(def|class|if|elif|else|for|while|return|import|from|as|with|try|except|finally|raise|pass|break|continue|lambda|yield|global|nonlocal|and|or|not|in|is|None|True|False|self|async|await)\b/g, cls: 'syn-keyword' },
            { pattern: /\b(int|str|float|bool|list|dict|set|tuple|bytes|type|object|range|print|len|map|filter|zip|enumerate|sorted|reversed|open|super|isinstance|hasattr|getattr|setattr)\b/g, cls: 'syn-type' },
            { pattern: /@(\w+)/g, cls: 'syn-annotation' },
            { pattern: /\b(\d+\.?\d*[jJ]?)\b/g, cls: 'syn-number' },
            { pattern: /\b(\w+)\s*\(/g, cls: 'syn-function', group: 1 }
        ],
        javascript: [
            { pattern: /(\/\/.*)/g, cls: 'syn-comment' },
            { pattern: /(\/\*[\s\S]*?\*\/)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|`(?:[^`\\]|\\.)*`)/g, cls: 'syn-string' },
            { pattern: /\b(var|let|const|function|class|if|else|for|while|do|return|import|export|from|default|new|try|catch|finally|throw|null|undefined|true|false|this|super|typeof|instanceof|async|await|yield|switch|case|break|continue|of|in)\b/g, cls: 'syn-keyword' },
            { pattern: /\b(Array|Object|String|Number|Boolean|Map|Set|Promise|RegExp|Date|Error|JSON|Math|console|document|window)\b/g, cls: 'syn-type' },
            { pattern: /\b(\d+\.?\d*)\b/g, cls: 'syn-number' },
            { pattern: /\b(\w+)\s*\(/g, cls: 'syn-function', group: 1 }
        ],
        typescript: null, // filled below
        xml: [
            { pattern: /(&lt;!--[\s\S]*?--&gt;)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"]*)")/g, cls: 'syn-string' },
            { pattern: /(&lt;\/?)([\w:-]+)/g, cls: 'syn-tag', group: 2 },
            { pattern: /\b([\w:-]+)=/g, cls: 'syn-attr', group: 1 }
        ],
        json: [
            { pattern: /("(?:[^"\\]|\\.)*")\s*:/g, cls: 'syn-attr', group: 1 },
            { pattern: /:\s*("(?:[^"\\]|\\.)*")/g, cls: 'syn-string', group: 1 },
            { pattern: /\b(true|false|null)\b/g, cls: 'syn-keyword' },
            { pattern: /\b(-?\d+\.?\d*(?:[eE][+-]?\d+)?)\b/g, cls: 'syn-number' }
        ],
        shell: [
            { pattern: /(#.*)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"\\]|\\.)*"|'[^']*')/g, cls: 'syn-string' },
            { pattern: /\b(if|then|else|elif|fi|for|while|do|done|case|esac|function|return|exit|export|source|alias|echo|cd|ls|grep|sed|awk|cat|chmod|chown|mkdir|rm|cp|mv|find|xargs|pipe|sudo)\b/g, cls: 'syn-keyword' },
            { pattern: /(\$\{?\w+\}?)/g, cls: 'syn-type' }
        ],
        css: [
            { pattern: /(\/\*[\s\S]*?\*\/)/g, cls: 'syn-comment' },
            { pattern: /("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')/g, cls: 'syn-string' },
            { pattern: /([\w-]+)\s*:/g, cls: 'syn-attr', group: 1 },
            { pattern: /(#[0-9a-fA-F]{3,8})\b/g, cls: 'syn-number' },
            { pattern: /\b(\d+\.?\d*(?:px|em|rem|%|vh|vw|pt|cm|mm|in|s|ms)?)\b/g, cls: 'syn-number' }
        ],
        markdown: [
            { pattern: /^(#{1,6}\s.*)/gm, cls: 'syn-keyword' },
            { pattern: /(\*\*.*?\*\*|__.*?__)/g, cls: 'syn-type' },
            { pattern: /(\*.*?\*|_.*?_)/g, cls: 'syn-annotation' },
            { pattern: /(`[^`]+`)/g, cls: 'syn-string' },
            { pattern: /(```[\s\S]*?```)/g, cls: 'syn-string' },
            { pattern: /(\[.*?\]\(.*?\))/g, cls: 'syn-function' }
        ],
        yaml: [
            { pattern: /(#.*)/g, cls: 'syn-comment' },
            { pattern: /^(\s*[\w.-]+)\s*:/gm, cls: 'syn-attr', group: 1 },
            { pattern: /("(?:[^"\\]|\\.)*"|'[^']*')/g, cls: 'syn-string' },
            { pattern: /\b(true|false|null|yes|no)\b/gi, cls: 'syn-keyword' },
            { pattern: /\b(\d+\.?\d*)\b/g, cls: 'syn-number' }
        ]
    };
    rules.typescript = rules.javascript;
    rules.gradle = rules.kotlin;

    function highlightText(text) {
        var langRules = rules[language];
        if (!langRules) return escapeHtml(text);

        var escaped = escapeHtml(text);
        // Tokenize to avoid overlapping replacements
        var tokens = [];
        for (var i = 0; i < langRules.length; i++) {
            var rule = langRules[i];
            var re = new RegExp(rule.pattern.source, rule.pattern.flags);
            var m;
            while ((m = re.exec(escaped)) !== null) {
                var matchText = rule.group ? m[rule.group] : m[1] || m[0];
                var start = rule.group ? m.index + m[0].indexOf(matchText) : m.index;
                tokens.push({ start: start, end: start + matchText.length, cls: rule.cls, text: matchText });
            }
        }

        // Sort by start position, longer matches first
        tokens.sort(function(a, b) { return a.start - b.start || b.end - a.end; });

        // Remove overlapping
        var filtered = [];
        var lastEnd = 0;
        for (var i = 0; i < tokens.length; i++) {
            if (tokens[i].start >= lastEnd) {
                filtered.push(tokens[i]);
                lastEnd = tokens[i].end;
            }
        }

        // Build result
        var result = '';
        var pos = 0;
        for (var i = 0; i < filtered.length; i++) {
            var t = filtered[i];
            result += escaped.substring(pos, t.start);
            result += '<span class="' + t.cls + '">' + t.text + '</span>';
            pos = t.end;
        }
        result += escaped.substring(pos);
        return result;
    }

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    // ----- Line numbers -----
    function updateLineNumbers() {
        var text = editor.innerText || '';
        var lines = text.split('\n');
        var nums = '';
        for (var i = 1; i <= lines.length; i++) {
            nums += i + '\n';
        }
        lineNumbers.textContent = nums;
    }

    // ----- Cursor info -----
    function updateCursorInfo() {
        var sel = window.getSelection();
        if (!sel.rangeCount) return;
        try {
            var range = sel.getRangeAt(0);
            var preRange = document.createRange();
            preRange.setStart(editor, 0);
            preRange.setEnd(range.startContainer, range.startOffset);
            var preText = preRange.toString();
            var lines = preText.split('\n');
            cursorInfo.textContent = 'Ln ' + lines.length + ', Col ' + (lines[lines.length - 1].length + 1);
        } catch(e) {}
    }

    // ----- Save -----
    function saveFile() {
        var content = editor.innerText;
        if (window.AndroidBridge) {
            window.AndroidBridge.save(content);
            modified = false;
            updateTitle();
        }
    }

    function updateTitle() {
        var name = '$fileName';
        fileLabel.textContent = modified ? name + ' *' : name;
    }

    // ----- Search / Replace -----
    function toggleSearch() {
        var visible = searchBar.classList.toggle('visible');
        if (visible) {
            searchInput.focus();
        } else {
            clearSearchHighlights();
        }
    }

    function doSearch() {
        clearSearchHighlights();
        var query = searchInput.value;
        if (!query) { searchInfo.textContent = ''; return; }

        var text = editor.innerText;
        searchMatches = [];
        var lower = text.toLowerCase();
        var q = query.toLowerCase();
        var idx = 0;
        while ((idx = lower.indexOf(q, idx)) !== -1) {
            searchMatches.push(idx);
            idx += q.length;
        }
        currentMatchIndex = searchMatches.length > 0 ? 0 : -1;
        searchInfo.textContent = searchMatches.length + ' found';
        highlightSearchMatches();
    }

    function highlightSearchMatches() {
        // Use window.find for navigation; visual feedback via status
        if (searchMatches.length > 0 && currentMatchIndex >= 0) {
            searchInfo.textContent = (currentMatchIndex + 1) + ' of ' + searchMatches.length;
            scrollToMatch(currentMatchIndex);
        }
    }

    function scrollToMatch(index) {
        // Use Selection API to select the match
        var text = editor.innerText;
        var query = searchInput.value;
        var pos = searchMatches[index];
        if (pos === undefined) return;

        var walker = document.createTreeWalker(editor, NodeFilter.SHOW_TEXT, null, false);
        var charCount = 0;
        var startNode = null, startOffset = 0, endNode = null, endOffset = 0;

        while (walker.nextNode()) {
            var node = walker.currentNode;
            var nodeLen = node.textContent.length;
            if (!startNode && charCount + nodeLen > pos) {
                startNode = node;
                startOffset = pos - charCount;
            }
            if (!endNode && charCount + nodeLen >= pos + query.length) {
                endNode = node;
                endOffset = pos + query.length - charCount;
                break;
            }
            charCount += nodeLen;
        }

        if (startNode && endNode) {
            var sel = window.getSelection();
            var range = document.createRange();
            range.setStart(startNode, startOffset);
            range.setEnd(endNode, endOffset);
            sel.removeAllRanges();
            sel.addRange(range);

            // Scroll into view
            var rect = range.getBoundingClientRect();
            var container = editorScroll;
            if (rect.top < container.getBoundingClientRect().top || rect.bottom > container.getBoundingClientRect().bottom) {
                var scrollTarget = container.scrollTop + rect.top - container.getBoundingClientRect().top - container.clientHeight / 3;
                container.scrollTo({ top: scrollTarget, behavior: 'smooth' });
            }
        }
    }

    function clearSearchHighlights() {
        searchMatches = [];
        currentMatchIndex = -1;
    }

    function findNext() {
        if (searchMatches.length === 0) { doSearch(); return; }
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.length;
        highlightSearchMatches();
    }

    function findPrev() {
        if (searchMatches.length === 0) { doSearch(); return; }
        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.length) % searchMatches.length;
        highlightSearchMatches();
    }

    function replaceCurrent() {
        if (currentMatchIndex < 0 || searchMatches.length === 0) return;
        var sel = window.getSelection();
        if (sel.rangeCount > 0 && sel.toString().toLowerCase() === searchInput.value.toLowerCase()) {
            document.execCommand('insertText', false, replaceInput.value);
            modified = true;
            updateTitle();
            updateLineNumbers();
            doSearch();
        }
    }

    function replaceAll() {
        var query = searchInput.value;
        var replacement = replaceInput.value;
        if (!query) return;
        var text = editor.innerText;
        var re = new RegExp(query.replace(/[.*+?^${'$'}{}()|[\]\\]/g, '\\${'$'}&'), 'gi');
        var newText = text.replace(re, replacement);
        editor.textContent = newText;
        modified = true;
        updateTitle();
        updateLineNumbers();
        doSearch();
    }

    // ----- Event listeners -----
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
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            toggleSearch();
        }
    });

    searchInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') { e.shiftKey ? findPrev() : findNext(); }
        if (e.key === 'Escape') { toggleSearch(); }
    });

    // Sync line numbers scroll
    editorScroll.addEventListener('scroll', function() {
        lineNumbers.style.marginTop = (-this.scrollTop) + 'px';
    });

    updateLineNumbers();
    </script>
</body>
</html>
            """.trimIndent()
        }

        private fun escapeForJsString(content: String): String {
            val sb = StringBuilder()
            sb.append('"')
            for (ch in content) {
                when (ch) {
                    '\\' -> sb.append("\\\\")
                    '"' -> sb.append("\\\"")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> sb.append(ch)
                }
            }
            sb.append('"')
            return sb.toString()
        }
    }
}
