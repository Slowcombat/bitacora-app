package co.julian.bitacora.data

import kotlinx.serialization.Serializable

/** Un bloque de la agenda. Las horas van como "H:MM" (24 h) o vacías. */
@Serializable
data class Bloque(
    val inicio: String = "",
    val fin: String = "",
    val tipo: String = "study",
    val titulo: String = "",
    val detalle: String = "",
    /** null = decide según el tipo (estudio, inglés y ejercicio avisan). */
    val avisar: Boolean? = null,
)

@Serializable
data class Fallo(val q: String, val r: String = "")

@Serializable
data class ResultadoExamen(
    val puntaje: Int = 0,
    val total: Int = 0,
    val fallos: List<Fallo> = emptyList(),
)

@Serializable
data class Entrada(
    val tareas: Map<String, Boolean> = emptyMap(),
    val minutos: Int = 0,
    val nota: String = "",
    val acoste: String = "",
    val desperte: String = "",
    val energia: Int = 0,
    val examenes: Map<String, ResultadoExamen> = emptyMap(),
)

/**
 * Alarma para despertar. [dias] usa 0 = domingo … 6 = sábado.
 * [fase]: "todas", "f1" (con universidad) o "f2" (vacaciones).
 * Si [fecha] tiene valor ("2026-11-14"), suena solo ese día.
 */
@Serializable
data class Alarma(
    val id: Int,
    val hora: Int,
    val minuto: Int,
    val dias: List<Int> = emptyList(),
    val etiqueta: String = "",
    val fase: String = "todas",
    val fecha: String? = null,
    val activa: Boolean = true,
)

@Serializable
data class Ajustes(
    val apiKey: String = "",
    val modelo: String = "claude-sonnet-5-5",
    val avisosActivos: Boolean = true,
    val minutosAntes: Int = 10,
    val horaDormir: String = "23:30",
)

@Serializable
data class Estado(
    val entradas: Map<String, Entrada> = emptyMap(),
    /** Horario semanal editado, clave "f1-3" (fase-día de la semana). */
    val semana: Map<String, List<Bloque>> = emptyMap(),
    /** Horario de un día concreto. Lista vacía = usar el horario semanal. */
    val dias: Map<String, List<Bloque>> = emptyMap(),
    val alarmas: List<Alarma> = emptyList(),
    val ajustes: Ajustes = Ajustes(),
    val version: Int = 1,
)

data class Leccion(
    val titulo: String,
    val pasos: List<String>,
    val ingles: String,
    val mejorar: String,
)

data class DiaBase(val tareas: List<String>, val bloques: List<Bloque>)

data class Plantilla(
    val bloques: List<Bloque>,
    val tareas: List<String>,
    /** null, "dia" o "semana" según de dónde salga el horario. */
    val editado: String?,
)

data class Tarea(val id: String, val tipo: String, val titulo: String, val detalle: String)
