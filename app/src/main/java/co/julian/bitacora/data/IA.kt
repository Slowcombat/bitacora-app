package co.julian.bitacora.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Elige el proveedor de IA según las claves que haya en Ajustes:
 * texto → Groq, luego Gemini, luego Claude; fotos → Gemini, luego Claude.
 */
object IA {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun hayTexto(a: Ajustes) = a.groqKey.isNotBlank() || a.geminiKey.isNotBlank() || a.apiKey.isNotBlank()

    fun hayFotos(a: Ajustes) = a.geminiKey.isNotBlank() || a.apiKey.isNotBlank()

    fun nombreTexto(a: Ajustes) = when {
        a.groqKey.isNotBlank() -> "Groq"
        a.geminiKey.isNotBlank() -> "Gemini"
        a.apiKey.isNotBlank() -> "Claude"
        else -> null
    }

    suspend fun pedir(a: Ajustes, prompt: String, imagenJpeg: ByteArray? = null, maxTokens: Int = 4000): String {
        if (imagenJpeg != null) {
            return when {
                a.geminiKey.isNotBlank() -> gemini(a.geminiKey, a.geminiModelo, prompt, imagenJpeg, maxTokens)
                a.apiKey.isNotBlank() -> Claude.pedir(a.apiKey, a.modelo, prompt, imagenJpeg, maxTokens)
                else -> throw ErrorClaude("Para revisar fotos agrega tu clave gratuita de Gemini en Ajustes.")
            }
        }
        return when {
            a.groqKey.isNotBlank() -> groq(a.groqKey, a.groqModelo, prompt, maxTokens)
            a.geminiKey.isNotBlank() -> gemini(a.geminiKey, a.geminiModelo, prompt, null, maxTokens)
            a.apiKey.isNotBlank() -> Claude.pedir(a.apiKey, a.modelo, prompt, null, maxTokens)
            else -> throw ErrorClaude("Agrega tu clave gratuita de Groq en Ajustes para usar los exámenes.")
        }
    }

    private fun post(url: String, cuerpo: String, cabeceras: Map<String, String>): Pair<Int, String> {
        val con = URL(url).openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.connectTimeout = 20_000
            con.readTimeout = 180_000
            con.doOutput = true
            con.setRequestProperty("content-type", "application/json")
            cabeceras.forEach { (k, v) -> con.setRequestProperty(k, v) }
            con.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            val codigo = con.responseCode
            val texto = (if (codigo in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return codigo to texto
        } catch (e: java.net.UnknownHostException) {
            throw ErrorClaude("No hay conexión a internet.")
        } catch (e: java.net.SocketTimeoutException) {
            throw ErrorClaude("La respuesta tardó demasiado. Intenta de nuevo.")
        } finally {
            con.disconnect()
        }
    }

    private fun detalle(cuerpo: String): String = try {
        val err = json.parseToJsonElement(cuerpo).jsonObject["error"]
        (err as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun error(servicio: String, codigo: Int, cuerpo: String): ErrorClaude {
        val d = detalle(cuerpo)
        return ErrorClaude(
            when (codigo) {
                400 -> if (d.contains("API key", true) || d.contains("api_key", true)) "La clave de $servicio no es válida. Revísala en Ajustes." else "$servicio rechazó la solicitud: $d"
                401, 403 -> "La clave de $servicio no es válida. Revísala en Ajustes."
                404 -> "El modelo de $servicio no existe o cambió de nombre. Revísalo en Ajustes."
                413 -> "La foto es demasiado grande para $servicio."
                429 -> "Llegaste al límite gratuito de $servicio por ahora. Intenta más tarde."
                in 500..599 -> "$servicio está ocupado. Intenta en unos minutos." + if (d.isNotBlank()) " ($codigo: ${d.take(120)})" else " ($codigo)"
                else -> "Error $codigo de $servicio: $d"
            },
        )
    }

    suspend fun groq(clave: String, modelo: String, prompt: String, maxTokens: Int): String = withContext(Dispatchers.IO) {
        val cuerpo = buildJsonObject {
            put("model", modelo)
            put("max_completion_tokens", maxTokens)
            put("temperature", 0.6)
            putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", "Respondes siempre en español y solo con un objeto JSON válido, sin texto adicional.")
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
        }
        val (codigo, texto) = post(
            "https://api.groq.com/openai/v1/chat/completions",
            cuerpo.toString(),
            mapOf("Authorization" to "Bearer ${clave.trim()}"),
        )
        if (codigo !in 200..299) throw error("Groq", codigo, texto)
        try {
            json.parseToJsonElement(texto).jsonObject["choices"]!!.jsonArray[0]
                .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
        } catch (e: Exception) {
            throw ErrorClaude("La respuesta de Groq llegó vacía. Intenta de nuevo.")
        }
    }

    private val MODELOS_GEMINI = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash", "gemini-2.5-flash-lite")

    /** Prueba el modelo elegido y, si está ocupado o no existe, los otros modelos gratuitos. */
    suspend fun gemini(clave: String, modelo: String, prompt: String, imagenJpeg: ByteArray?, maxTokens: Int): String {
        val modelos = (listOf(modelo.trim()) + MODELOS_GEMINI).filter { it.isNotBlank() }.distinct()
        var ultimo: ErrorClaude? = null
        for (m in modelos) {
            try {
                return geminiUno(clave, m, prompt, imagenJpeg, maxTokens)
            } catch (e: ReintentarConOtro) {
                ultimo = e.error
            }
        }
        throw ultimo ?: ErrorClaude("Gemini no respondió. Intenta más tarde.")
    }

    private class ReintentarConOtro(val error: ErrorClaude) : Exception()

    private suspend fun geminiUno(clave: String, modelo: String, prompt: String, imagenJpeg: ByteArray?, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val cuerpo = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            if (imagenJpeg != null) {
                                addJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", "image/jpeg")
                                        put("data", Base64.encodeToString(imagenJpeg, Base64.NO_WRAP))
                                    }
                                }
                            }
                            addJsonObject { put("text", prompt) }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", maxTokens)
                    put("temperature", 0.6)
                }
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                URLEncoder.encode(modelo.trim(), "UTF-8") + ":generateContent"
            val (codigo, texto) = post(url, cuerpo.toString(), mapOf("x-goog-api-key" to clave.trim()))
            if (codigo !in 200..299) {
                val err = error("Gemini", codigo, texto)
                // Ocupado, sin cupo en este modelo o modelo inexistente: probar el siguiente.
                if (codigo == 404 || codigo == 429 || codigo >= 500) throw ReintentarConOtro(err)
                throw err
            }
            try {
                json.parseToJsonElement(texto).jsonObject["candidates"]!!.jsonArray[0]
                    .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray
                    .mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                    .joinToString("")
                    .ifBlank { throw IllegalStateException() }
            } catch (e: Exception) {
                throw ErrorClaude("La respuesta de Gemini llegó vacía. Intenta de nuevo.")
            }
        }
}
