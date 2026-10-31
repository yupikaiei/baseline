package com.baseline.app.data

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bring-your-own-key client for any OpenAI-compatible chat endpoint.
 * No key configured means no network call: the app falls back to the offline,
 * retrieval-based coach, so the free path always works.
 */
object LlmClient {

    data class Config(
        val apiKey: String,
        val baseUrl: String,
        val model: String
    ) {
        val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
    }

    suspend fun ask(config: Config, systemPrompt: String, question: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(config.baseUrl.trimEnd('/') + "/chat/completions")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }

                val body = JSONObject().apply {
                    put("model", config.model)
                    put("temperature", 0.3)
                    put("max_tokens", 700)
                    put(
                        "messages",
                        org.json.JSONArray().apply {
                            put(JSONObject().put("role", "system").put("content", systemPrompt))
                            put(JSONObject().put("role", "user").put("content", question))
                        }
                    )
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

                if (code !in 200..299) {
                    error("HTTP $code: ${text.take(180)}")
                }

                val parsed = JSONObject(text)
                val content = parsed.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                content.trim()
            }
        }
}
