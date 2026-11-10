package co.julian.bitacora.data

import android.content.Context
import co.julian.bitacora.alarm.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda todo en un archivo JSON privado de la app. */
object Store {
    private var archivo: File? = null
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    @Synchronized
    fun init(ctx: Context) {
        if (archivo != null) return
        val f = File(ctx.applicationContext.filesDir, "bitacora.json")
        archivo = f
        _estado.value = try {
            if (f.exists()) json.decodeFromString(Estado.serializer(), f.readText()) else inicial()
        } catch (e: Exception) {
            inicial()
        }
        if (_estado.value.fechaInicioApp.isBlank()) {
            _estado.value = _estado.value.copy(fechaInicioApp = java.time.LocalDate.now().toString())
            guardar(_estado.value)
        }
        if (!f.exists()) guardar(_estado.value)
    }

    private fun inicial() = Estado(alarmas = Plan.ALARMAS_INICIALES)

    @Synchronized
    fun update(ctx: Context, cambio: (Estado) -> Estado) {
        init(ctx)
        val nuevo = cambio(_estado.value)
        _estado.value = nuevo
        guardar(nuevo)
        Scheduler.reprogramar(ctx)
    }

    fun editarEntrada(ctx: Context, fecha: String, cambio: (Entrada) -> Entrada) = update(ctx) { e ->
        e.copy(entradas = e.entradas + (fecha to cambio(e.entradas[fecha] ?: Entrada())))
    }

    private fun guardar(e: Estado) {
        val f = archivo ?: return
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(json.encodeToString(Estado.serializer(), e))
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
