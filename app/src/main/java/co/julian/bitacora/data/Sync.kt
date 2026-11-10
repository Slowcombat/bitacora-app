package co.julian.bitacora.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class DiaRegistro(
    val fecha: String,
    val semana: Int,
    val tema: String,
    val leccion: String,
    val tareasPlan: List<String>,
    val entrada: Entrada,
)

@Serializable
data class Registro(
    val generado: String,
    val versionApp: String,
    val horaDormir: String,
    val planActual: String,
    val dias: List<DiaRegistro>,
)

/**
 * Sube registro.json al repositorio privado y descarga plan.json,
 * que Claude actualiza en cada revisión automática.
 */
object Sync {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true; coerceInputValues = true }

    data class Resultado(val ok: Boolean, val mensaje: String)

    fun configurada(a: Ajustes) = a.githubToken.isNotBlank() && a.githubRepo.contains("/")

    suspend fun sincronizar(ctx: Context, forzar: Boolean = false): Resultado? {
        Store.init(ctx)
        val e = Store.estado.value
        val a = e.ajustes
        if (!configurada(a)) return null
        if (!forzar && e.ultimaSync.isNotBlank()) {
            val ultima = try { LocalDateTime.parse(e.ultimaSync) } catch (_: Exception) { null }
            if (ultima != null && Duration.between(ultima, LocalDateTime.now()).toMinutes() < 120) return null
        }
        return withContext(Dispatchers.IO) {
            try {
                subir(a, "registro.json", json.encodeToString(Registro.serializer(), registro(e)))
                val plan = descargarPlan(a)
                withContext(Dispatchers.Main) {
                    Store.update(ctx) { st ->
                        st.copy(
                            planRemoto = plan ?: st.planRemoto,
                            ultimaSync = LocalDateTime.now().withNano(0).toString(),
                        )
                    }
                }
                Resultado(true, if (plan != null) "Sincronizado. Plan de Claude actualizado." else "Sincronizado. Aún no hay plan de Claude.")
            } catch (err: ErrorClaude) {
                Resultado(false, err.message ?: "No se pudo sincronizar.")
            } catch (err: Exception) {
                Resultado(false, "No se pudo sincronizar: ${err.message ?: err.javaClass.simpleName}")
            }
        }
    }

    private fun registro(e: Estado): Registro {
        val hoy = LocalDate.now()
        val dias = (20 downTo 0).map { i ->
            val d = hoy.minusDays(i.toLong())
            val p = Logica.plantilla(e, d)
            DiaRegistro(
                fecha = d.toString(),
                semana = Logica.semanaN(d),
                tema = Logica.temaSemana(d),
                leccion = Logica.leccion(d)?.titulo ?: "",
                tareasPlan = p.tareas,
                entrada = e.entradas[d.toString()] ?: Entrada(),
            )
        }
        return Registro(
            generado = LocalDateTime.now().withNano(0).toString(),
            versionApp = "1",
            horaDormir = e.ajustes.horaDormir,
            planActual = e.planRemoto?.actualizado ?: "",
            dias = dias,
        )
    }

    private fun api(a: Ajustes, ruta: String) =
        URL("https://api.github.com/repos/${a.githubRepo.trim()}/contents/$ruta")

    private fun abrir(a: Ajustes, ruta: String, metodo: String): HttpURLConnection =
        (api(a, ruta).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer ${a.githubToken.trim()}")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "bitacora-app")
        }

    private fun leer(con: HttpURLConnection): Pair<Int, String> {
        val codigo = con.responseCode
        val texto = (if (codigo in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        return codigo to texto
    }

    private fun errorGitHub(codigo: Int): ErrorClaude = ErrorClaude(
        when (codigo) {
            401 -> "El token de GitHub no es válido o venció. Revísalo en Ajustes."
            403 -> "El token de GitHub no tiene permiso para escribir en el repositorio."
            404 -> "No encuentro el repositorio. Revisa el nombre (usuario/repositorio) y que el token tenga acceso a él."
            else -> "GitHub respondió con el error $codigo."
        },
    )

    /** SHA del archivo si ya existe (GitHub lo pide para reemplazarlo). */
    private fun sha(a: Ajustes, ruta: String): String? {
        val con = abrir(a, ruta, "GET")
        try {
            val (codigo, texto) = leer(con)
            if (codigo == 404) {
                // 404 también puede significar que el repositorio no existe; lo distinguimos al subir.
                return null
            }
            if (codigo !in 200..299) throw errorGitHub(codigo)
            return json.parseToJsonElement(texto).jsonObject["sha"]?.jsonPrimitive?.contentOrNull
        } finally {
            con.disconnect()
        }
    }

    private fun subir(a: Ajustes, ruta: String, contenido: String) {
        val anterior = sha(a, ruta)
        val cuerpo = buildJsonObject {
            put("message", "Registro de la Bitácora ${LocalDateTime.now().withNano(0)}")
            put("content", Base64.encodeToString(contenido.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            if (anterior != null) put("sha", anterior)
        }
        val con = abrir(a, ruta, "PUT").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            con.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            val (codigo, _) = leer(con)
            if (codigo !in 200..299) throw errorGitHub(codigo)
        } catch (e: java.net.UnknownHostException) {
            throw ErrorClaude("No hay conexión a internet.")
        } finally {
            con.disconnect()
        }
    }

    private fun descargarPlan(a: Ajustes): PlanRemoto? {
        val con = abrir(a, "plan.json", "GET")
        try {
            val (codigo, texto) = leer(con)
            if (codigo == 404) return null
            if (codigo !in 200..299) throw errorGitHub(codigo)
            val b64 = json.parseToJsonElement(texto).jsonObject["content"]?.jsonPrimitive?.contentOrNull ?: return null
            val crudo = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
            return try {
                json.decodeFromString(PlanRemoto.serializer(), crudo)
            } catch (_: Exception) {
                null
            }
        } finally {
            con.disconnect()
        }
    }
}
