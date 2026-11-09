package co.julian.bitacora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

class ErrorClaude(message: String) : Exception(message)

@Serializable
data class Pregunta(
    val tema: String = "",
    val pregunta: String,
    val codigo: String = "",
    val opciones: List<String>,
    val correcta: Int,
    val explicacion: String = "",
)

data class Practico(val formato: String, val enunciado: String, val ejemplo: String, val pista: String)

data class Evaluacion(val nota: Int, val bien: String, val mejorar: String, val solucion: String)

/** Llamadas a la API de Anthropic con la clave del usuario. */
object Claude {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun pedir(
        apiKey: String,
        modelo: String,
        prompt: String,
        imagenJpeg: ByteArray? = null,
        maxTokens: Int = 4000,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ErrorClaude("Agrega tu clave de API en Ajustes para usar los exámenes.")
        val cuerpo = buildJsonObject {
            put("model", modelo)
            put("max_tokens", maxTokens)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        if (imagenJpeg != null) {
                            addJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", Base64.encodeToString(imagenJpeg, Base64.NO_WRAP))
                                }
                            }
                        }
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                    }
                }
            }
        }
        val con = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.connectTimeout = 20_000
            con.readTimeout = 180_000
            con.doOutput = true
            con.setRequestProperty("content-type", "application/json")
            con.setRequestProperty("x-api-key", apiKey.trim())
            con.setRequestProperty("anthropic-version", "2023-06-01")
            con.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            val codigo = con.responseCode
            val texto = (if (codigo in 200..299) con.inputStream else con.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (codigo !in 200..299) throw ErrorClaude(mensajeError(codigo, texto))
            val raiz = json.parseToJsonElement(texto).jsonObject
            raiz["content"]?.jsonArray
                ?.mapNotNull { bloque ->
                    val o = bloque.jsonObject
                    if (o["type"]?.jsonPrimitive?.contentOrNull == "text") o["text"]?.jsonPrimitive?.contentOrNull else null
                }
                ?.joinToString("")
                ?: throw ErrorClaude("La respuesta llegó vacía. Intenta de nuevo.")
        } catch (e: ErrorClaude) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            throw ErrorClaude("No hay conexión a internet.")
        } catch (e: java.net.SocketTimeoutException) {
            throw ErrorClaude("La respuesta tardó demasiado. Intenta de nuevo.")
        } catch (e: Exception) {
            throw ErrorClaude("No se pudo completar: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            con.disconnect()
        }
    }

    private fun mensajeError(codigo: Int, cuerpo: String): String {
        val detalle = try {
            json.parseToJsonElement(cuerpo).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            null
        } ?: ""
        return when {
            codigo == 401 -> "La clave de API no es válida. Revísala en Ajustes."
            codigo == 403 -> "Tu clave de API no tiene permiso para esto."
            detalle.contains("credit", ignoreCase = true) -> "Tu cuenta de API no tiene saldo. Recárgala en console.anthropic.com."
            codigo == 404 -> "El modelo elegido no está disponible. Cambia el modelo en Ajustes."
            codigo == 429 -> "Llegaste al límite de uso por ahora. Intenta en unos minutos."
            codigo >= 500 -> "El servicio está ocupado. Intenta en unos minutos."
            else -> "Error $codigo: $detalle"
        }
    }

    fun extraerJson(texto: String): JsonObject {
        val i = texto.indexOf('{')
        val j = texto.lastIndexOf('}')
        if (i < 0 || j <= i) throw ErrorClaude("La respuesta llegó incompleta. Intenta de nuevo.")
        return try {
            json.parseToJsonElement(texto.substring(i, j + 1)).jsonObject
        } catch (e: Exception) {
            throw ErrorClaude("La respuesta llegó incompleta. Intenta de nuevo.")
        }
    }

    fun preguntasDe(o: JsonObject): List<Pregunta> {
        val arr = o["preguntas"] ?: throw ErrorClaude("Las preguntas llegaron incompletas. Intenta de nuevo.")
        val lista = try {
            json.decodeFromJsonElement(ListSerializer(Pregunta.serializer()), arr)
        } catch (e: Exception) {
            throw ErrorClaude("Las preguntas llegaron incompletas. Intenta de nuevo.")
        }
        return lista.filter { it.opciones.size == 4 && it.correcta in 0..3 && it.pregunta.isNotBlank() }
            .ifEmpty { throw ErrorClaude("Las preguntas llegaron incompletas. Intenta de nuevo.") }
    }

    private fun txt(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.contentOrNull ?: ""

    fun practicoDe(o: JsonObject, formato: String): Practico {
        val enunciado = txt(o, "enunciado")
        if (enunciado.isBlank()) throw ErrorClaude("El ejercicio llegó incompleto. Intenta de nuevo.")
        return Practico(formato, enunciado, txt(o, "ejemplo"), txt(o, "pista"))
    }

    fun evaluacionDe(o: JsonObject): Evaluacion {
        val nota = o["nota"]?.jsonPrimitive?.doubleOrNull
            ?: throw ErrorClaude("No llegó la calificación completa. Intenta de nuevo.")
        return Evaluacion(nota.roundToInt().coerceIn(0, 10), txt(o, "bien"), txt(o, "mejorar"), txt(o, "solucion"))
    }

    /** Lee una foto, la reduce y la comprime a JPEG para enviarla. */
    fun cargarImagen(ctx: Context, uri: Uri): ByteArray? = try {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        var muestra = 1
        while (max(opciones.outWidth, opciones.outHeight) / muestra > 3000) muestra *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        }
        bmp?.let {
            val lado = max(it.width, it.height)
            val escala = if (lado > 1568) 1568f / lado else 1f
            val final = if (escala < 1f) {
                Bitmap.createScaledBitmap(it, (it.width * escala).toInt(), (it.height * escala).toInt(), true)
            } else it
            val salida = ByteArrayOutputStream()
            final.compress(Bitmap.CompressFormat.JPEG, 85, salida)
            salida.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}
