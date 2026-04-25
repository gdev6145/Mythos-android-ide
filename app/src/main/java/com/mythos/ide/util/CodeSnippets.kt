package com.mythos.ide.util

/**
 * Provides language-specific code snippets that can be inserted into the editor.
 * Each snippet has a trigger name and the code it expands to.
 */
object CodeSnippets {

    data class Snippet(val name: String, val description: String, val code: String)

    fun getSnippets(language: String): List<Snippet> {
        return when (language) {
            "kotlin" -> kotlinSnippets
            "java" -> javaSnippets
            "python" -> pythonSnippets
            "javascript", "typescript" -> jsSnippets
            "html", "xml" -> xmlSnippets
            else -> generalSnippets
        }
    }

    private val kotlinSnippets = listOf(
        Snippet("fun", "Function declaration", "fun name(params): ReturnType {\n    \n}"),
        Snippet("main", "Main function", "fun main(args: Array<String>) {\n    println(\"Hello, World!\")\n}"),
        Snippet("class", "Class declaration", "class ClassName(\n    val property: Type\n) {\n    \n}"),
        Snippet("data", "Data class", "data class Name(\n    val id: Int,\n    val name: String\n)"),
        Snippet("if", "If expression", "if (condition) {\n    \n} else {\n    \n}"),
        Snippet("when", "When expression", "when (value) {\n    is Type -> {}\n    else -> {}\n}"),
        Snippet("for", "For loop", "for (item in collection) {\n    \n}"),
        Snippet("try", "Try-catch", "try {\n    \n} catch (e: Exception) {\n    e.printStackTrace()\n}"),
        Snippet("coroutine", "Coroutine launch", "CoroutineScope(Dispatchers.IO).launch {\n    \n    withContext(Dispatchers.Main) {\n        \n    }\n}"),
        Snippet("singleton", "Object declaration", "object Singleton {\n    \n}")
    )

    private val javaSnippets = listOf(
        Snippet("main", "Main method", "public static void main(String[] args) {\n    System.out.println(\"Hello, World!\");\n}"),
        Snippet("class", "Class declaration", "public class ClassName {\n    \n}"),
        Snippet("method", "Method declaration", "public void methodName() {\n    \n}"),
        Snippet("for", "For loop", "for (int i = 0; i < length; i++) {\n    \n}"),
        Snippet("foreach", "Enhanced for", "for (Type item : collection) {\n    \n}"),
        Snippet("if", "If-else", "if (condition) {\n    \n} else {\n    \n}"),
        Snippet("try", "Try-catch", "try {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}"),
        Snippet("sout", "Print line", "System.out.println();"),
        Snippet("interface", "Interface", "public interface InterfaceName {\n    \n}")
    )

    private val pythonSnippets = listOf(
        Snippet("def", "Function definition", "def function_name(params):\n    pass"),
        Snippet("class", "Class definition", "class ClassName:\n    def __init__(self):\n        pass"),
        Snippet("main", "Main guard", "if __name__ == \"__main__\":\n    main()"),
        Snippet("for", "For loop", "for item in collection:\n    pass"),
        Snippet("if", "If-elif-else", "if condition:\n    pass\nelif other:\n    pass\nelse:\n    pass"),
        Snippet("try", "Try-except", "try:\n    pass\nexcept Exception as e:\n    print(f\"Error: {e}\")"),
        Snippet("with", "Context manager", "with open(\"file.txt\", \"r\") as f:\n    content = f.read()"),
        Snippet("list", "List comprehension", "[x for x in iterable if condition]"),
        Snippet("lambda", "Lambda function", "fn = lambda x: x * 2"),
        Snippet("async", "Async function", "async def function_name():\n    await something()")
    )

    private val jsSnippets = listOf(
        Snippet("fn", "Arrow function", "const name = (params) => {\n    \n};"),
        Snippet("func", "Function declaration", "function name(params) {\n    \n}"),
        Snippet("class", "Class definition", "class ClassName {\n    constructor() {\n        \n    }\n}"),
        Snippet("for", "For loop", "for (let i = 0; i < length; i++) {\n    \n}"),
        Snippet("forof", "For-of loop", "for (const item of iterable) {\n    \n}"),
        Snippet("if", "If-else", "if (condition) {\n    \n} else {\n    \n}"),
        Snippet("try", "Try-catch", "try {\n    \n} catch (error) {\n    console.error(error);\n}"),
        Snippet("async", "Async function", "async function name() {\n    const result = await fetch(url);\n    return result.json();\n}"),
        Snippet("promise", "Promise", "new Promise((resolve, reject) => {\n    \n});"),
        Snippet("log", "Console log", "console.log();"),
        Snippet("import", "ES module import", "import { name } from 'module';"),
        Snippet("export", "Default export", "export default function name() {\n    \n}")
    )

    private val xmlSnippets = listOf(
        Snippet("linear", "LinearLayout", "<LinearLayout\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"wrap_content\"\n    android:orientation=\"vertical\">\n\n</LinearLayout>"),
        Snippet("textview", "TextView", "<TextView\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"\" />"),
        Snippet("button", "Button", "<Button\n    android:id=\"@+id/button\"\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\"\n    android:text=\"\" />"),
        Snippet("edittext", "EditText", "<EditText\n    android:id=\"@+id/editText\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"wrap_content\"\n    android:hint=\"\" />"),
        Snippet("recycler", "RecyclerView", "<androidx.recyclerview.widget.RecyclerView\n    android:id=\"@+id/recyclerView\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\" />")
    )

    private val generalSnippets = listOf(
        Snippet("todo", "TODO comment", "// TODO: "),
        Snippet("fixme", "FIXME comment", "// FIXME: "),
        Snippet("note", "NOTE comment", "// NOTE: "),
        Snippet("doc", "Documentation block", "/**\n * Description\n *\n * @param name Description\n * @return Description\n */")
    )
}
