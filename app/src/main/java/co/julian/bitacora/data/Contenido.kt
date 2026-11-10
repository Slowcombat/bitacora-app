package co.julian.bitacora.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Exámenes escritos por adelantado y publicados en el repositorio de la app.
 * Se descargan en uno o dos segundos y no gastan el cupo de la IA gratuita.
 */
object Contenido {
    private const val BASE = "https://raw.githubusercontent.com/Slowcombat/bitacora-app/main/contenido/examenes/"

    /** Devuelve las preguntas listas para ese día y tipo, o null si no hay (entonces se usa la IA). */
    suspend fun examen(fecha: LocalDate, tipo: String): List<Pregunta>? = withContext(Dispatchers.IO) {
        val con = try {
            URL("$BASE$fecha-$tipo.json").openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return@withContext null
        }
        try {
            con.connectTimeout = 6_000
            con.readTimeout = 8_000
            con.setRequestProperty("Cache-Control", "no-cache")
            if (con.responseCode != 200) return@withContext null
            val texto = con.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            Claude.preguntasDe(Claude.extraerJson(texto))
        } catch (_: Exception) {
            null
        } finally {
            con.disconnect()
        }
    }
}
