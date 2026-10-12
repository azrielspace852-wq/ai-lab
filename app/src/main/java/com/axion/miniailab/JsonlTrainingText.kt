package com.axion.miniailab

import org.json.JSONArray
import org.json.JSONObject

/** Converts JSONL records to training text, extracting user/assistant content only. JSON keys,
 * role labels, braces, and serialization syntax are never passed to the tokenizer. */
object JsonlTrainingText {
    fun extract(raw: String): String {
        val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<String>()
        var parsedJsonl = false
        for (line in lines) {
            if (line.startsWith("{") || line.startsWith("[")) {
                try {
                    val obj = JSONObject(line)
                    val extracted = extractObject(obj)
                    if (extracted.isNotBlank()) { result += extracted; parsedJsonl = true; continue }
                } catch (_: Exception) { }
                try {
                    val arr = JSONArray(line); val items=mutableListOf<String>()
                    for(i in 0 until arr.length()) if(arr.opt(i) is JSONObject) items += extractObject(arr.getJSONObject(i))
                    if(items.isNotEmpty()) { result += items.filter{it.isNotBlank()}; parsedJsonl=true; continue }
                } catch (_: Exception) { }
            }
            // A malformed JSONL line is skipped rather than teaching punctuation/metadata.
            if (line.startsWith("{") || line.startsWith("[")) continue
            result += line
        }
        return result.joinToString("\n").trim()
    }
    private fun extractObject(o: JSONObject): String {
        val parts=mutableListOf<String>()
        val messages=o.optJSONArray("messages")
        if(messages!=null) for(i in 0 until messages.length()) {
            val m=messages.optJSONObject(i) ?: continue
            val role=m.optString("role", "")
            if(role.equals("assistant",true) || role.equals("user",true) || role.equals("system",true)) {
                val c=content(m.opt("content")); if(c.isNotBlank()) parts += c
            }
        }
        if(parts.isNotEmpty()) return parts.joinToString("\n")
        for(key in listOf("text","content","prompt","completion","response","output","instruction","input")) {
            val value=o.opt(key); val s=content(value); if(s.isNotBlank()) parts += s
        }
        return parts.joinToString("\n")
    }
    private fun content(v: Any?): String = when(v) {
        is String -> v
        is JSONArray -> (0 until v.length()).mapNotNull { i -> val x=v.optJSONObject(i); if(x!=null) content(x.opt("text")).takeIf{it.isNotBlank()} else null }.joinToString(" ")
        is JSONObject -> content(v.opt("text"))
        else -> ""
    }
}
