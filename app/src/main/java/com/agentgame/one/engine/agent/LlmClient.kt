package com.agentgame.one.engine.agent

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenAI-compatible chat client used by the AI agent. Configurable base URL, API key and
 * model, so it works with OpenAI, Anthropic-compatible gateways, Ollama (local) or any
 * OpenAI-compatible endpoint. If no endpoint/key is configured the agent falls back to a fully
 * offline heuristic planner.
 */
class LlmClient(
    var baseUrl: String = "https://api.openai.com/v1/chat/completions",
    var apiKey: String = "",
    var model: String = "gpt-4o-mini",
) {
    var configured: Boolean get() = apiKey.isNotBlank()
    var timeoutMs: Int = 30_000

    /** Sends a chat completion request and returns the assistant text (or null on failure). */
    fun chat(system: String, user: String): String? {
        if (!configured) return null
        return try {
            val payload = JSONObject().apply {
                put("model", model)
                put("temperature", 0.4)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                })
            }
            val conn = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = BufferedReader(InputStreamReader(stream)).readText()
            if (code in 200..299) {
                JSONObject(body).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
            } else {
                "LLM HTTP $code: ${body.take(500)}"
            }
        } catch (t: Throwable) {
            "LLM error: ${t.message}"
        }
    }
}
