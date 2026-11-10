package co.julian.bitacora.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object Logica {
    val FIN_UNI: LocalDate = LocalDate.of(2026, 11, 20)
    val INICIO: LocalDate = LocalDate.of(2026, 11, 9)
    val META: LocalDate = LocalDate.of(2027, 3, 1)

    /** Los exámenes diarios son obligatorios desde el sábado 7 de noviembre. */
    val INICIO_EXAMENES: LocalDate = LocalDate.of(2026, 11, 7)
    private val ES = Locale("es", "CO")

    /** 0 = domingo … 6 = sábado. */
    fun dow(d: LocalDate) = d.dayOfWeek.value % 7

    fun fase(d: LocalDate) = if (d.isAfter(FIN_UNI)) "f2" else "f1"

    fun nombreDia(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.FULL, ES)

    fun nombreMes(d: LocalDate) = d.month.getDisplayName(TextStyle.FULL, ES)

    fun fechaLarga(d: LocalDate) = "${nombreDia(d)} ${d.dayOfMonth} de ${nombreMes(d)}"

    fun fechaCorta(d: LocalDate) = "${d.dayOfMonth} ${nombreMes(d).take(3)}"

    val NOMBRES_DIAS = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val LETRAS_DIAS = listOf("D", "L", "M", "M", "J", "V", "S")

    fun parseHora(s: String): LocalTime? {
        val m = Regex("""^(\d{1,2}):(\d{2})$""").find(s.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].toInt()
        if (h !in 0..23 || mi !in 0..59) return null
        return LocalTime.of(h, mi)
    }

    fun hora(t: LocalTime) = "${t.hour}:${t.minute.toString().padStart(2, '0')}"

    fun base(f: String, dow: Int): DiaBase = (if (f == "f1") Plan.F1 else Plan.F2).getValue(dow)

    fun bloquesSemana(e: Estado, f: String, dow: Int): List<Bloque> =
        e.semana["$f-$dow"] ?: base(f, dow).bloques

    private fun overrideDia(e: Estado, k: String): List<Bloque>? {
        if (e.dias.containsKey(k)) return e.dias[k]?.ifEmpty { null }
        return Plan.DIAS_BASE[k]
    }

    fun tareasDe(bloques: List<Bloque>): List<String> {
        val tiene = mutableSetOf("dormir")
        val estudio = bloques.count { it.tipo == "study" }
        if (bloques.any { it.tipo == "gym" }) tiene += "ejercicio"
        if (bloques.any { it.tipo == "eng" }) tiene += "ingles"
        if (estudio >= 1) tiene += "logica"
        if (estudio >= 2) tiene += "proyecto"
        return Plan.TAREAS.map { it.id }.filter { it in tiene }
    }

    fun plantilla(e: Estado, d: LocalDate): Plantilla {
        val k = d.toString()
        val f = fase(d)
        val dw = dow(d)
        overrideDia(e, k)?.let { return Plantilla(it, tareasDe(it), "dia") }
        e.semana["$f-$dw"]?.let { return Plantilla(it, tareasDe(it), "semana") }
        val b = base(f, dw)
        return Plantilla(b.bloques, b.tareas, null)
    }

    fun avisa(b: Bloque): Boolean = b.avisar ?: (b.tipo in setOf("study", "eng", "gym"))

    fun ordenar(bloques: List<Bloque>) =
        bloques.sortedBy { parseHora(it.inicio)?.toSecondOfDay() ?: Int.MAX_VALUE }

    fun semanaN(d: LocalDate): Int =
        Math.floorDiv(ChronoUnit.DAYS.between(INICIO, d), 7L).toInt() + 1

    fun semanaPlan(d: LocalDate) = semanaN(d).coerceIn(1, 16)

    fun temaSemana(d: LocalDate) = Plan.TEMAS[semanaPlan(d) - 1]

    fun temasHasta(n: Int) = Plan.TEMAS.take(n.coerceIn(1, 16))

    fun etapaDe(n: Int): Triple<String, List<String>, IntRange> {
        var c = 0
        for ((nombre, temas) in Plan.PLAN) {
            val ini = c + 1
            c += temas.size
            if (n <= c) return Triple(nombre, temas, ini..c)
        }
        val ultimo = Plan.PLAN.last()
        return Triple(ultimo.first, ultimo.second, 13..16)
    }

    /** Primero la lección que ajustó Claude en la última revisión; si no hay, la del plan base. */
    fun leccion(d: LocalDate): Leccion? {
        val k = d.toString()
        Store.estado.value.planRemoto?.lecciones?.get(k)?.let {
            if (it.titulo.isNotBlank()) return Leccion(it.titulo, it.pasos, it.ingles, it.mejorar)
        }
        return Plan.LECCIONES[k]
    }

    fun entrada(e: Estado, d: LocalDate) = e.entradas[d.toString()] ?: Entrada()

    data class Puntaje(val hechas: Int, val total: Int) {
        val pct get() = if (total == 0) 0f else hechas.toFloat() / total
    }

    fun puntaje(e: Estado, d: LocalDate): Puntaje {
        val p = plantilla(e, d)
        val en = e.entradas[d.toString()] ?: return Puntaje(0, p.tareas.size)
        return Puntaje(p.tareas.count { en.tareas[it] == true }, p.tareas.size)
    }

    fun racha(e: Estado, hoy: LocalDate): Int {
        var d = hoy
        if (puntaje(e, d).pct < 0.6f) d = d.minusDays(1)
        var n = 0
        while (puntaje(e, d).pct >= 0.6f && n < 400) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun lunesDe(d: LocalDate): LocalDate = d.minusDays(((d.dayOfWeek.value + 6) % 7).toLong())

    fun horasSueno(en: Entrada): Double? {
        val a = parseHora(en.acoste) ?: return null
        val b = parseHora(en.desperte) ?: return null
        var m = (b.toSecondOfDay() - a.toSecondOfDay()) / 60
        if (m <= 0) m += 1440
        return m / 60.0
    }

    fun formatoHoras(h: Double): String {
        var hh = h.toInt()
        var mm = ((h - hh) * 60).roundToInt()
        if (mm == 60) { hh++; mm = 0 }
        return if (mm == 0) "$hh h" else "$hh h ${mm.toString().padStart(2, '0')}"
    }

    fun fallosRecientes(e: Estado, hoy: LocalDate, dias: Int): List<Fallo> {
        val out = mutableListOf<Fallo>()
        for (i in 0 until dias) {
            val en = e.entradas[hoy.minusDays(i.toLong()).toString()] ?: continue
            en.examenes.values.forEach { out += it.fallos }
        }
        return out.take(15)
    }

    /** Días anteriores (desde el 7 de noviembre y como mucho 14) sin examen diario, del más antiguo al más reciente. */
    fun examenesPendientes(e: Estado, hoy: LocalDate): List<LocalDate> {
        var d = listOf(INICIO_EXAMENES, hoy.minusDays(14)).maxOrNull() ?: hoy
        val out = mutableListOf<LocalDate>()
        while (d.isBefore(hoy)) {
            if (e.entradas[d.toString()]?.examenes?.containsKey("diario") != true) out += d
            d = d.plusDays(1)
        }
        return out
    }

    const val PREFIJO_REPASO = "Repaso de tus errores · "

    /**
     * Preguntas falladas en los últimos 14 días que todavía no ha acertado en un repaso,
     * de la más antigua a la más reciente.
     */
    fun repasosPendientes(e: Estado, hoy: LocalDate): List<Pregunta> {
        val resueltas = e.entradas.values.flatMap { it.repasosAcertados }.toSet()
        val out = mutableListOf<Pregunta>()
        for (i in 14 downTo 0) {
            val en = e.entradas[hoy.minusDays(i.toLong()).toString()] ?: continue
            en.examenes.values.forEach { r -> out += r.falladas }
        }
        return out.filter { it.pregunta !in resueltas }.distinctBy { it.pregunta }
    }

    /** Misma pregunta con las opciones en otro orden, marcada como repaso. */
    fun comoRepaso(p: Pregunta): Pregunta {
        val orden = p.opciones.indices.shuffled()
        return p.copy(
            tema = PREFIJO_REPASO + p.tema.removePrefix(PREFIJO_REPASO),
            opciones = orden.map { p.opciones[it] },
            correcta = orden.indexOf(p.correcta),
        )
    }

    fun diasParaMeta(hoy: LocalDate) = ChronoUnit.DAYS.between(hoy, META).coerceAtLeast(0)

    /** Resumen de los últimos días en texto, para pegarlo en el chat con Claude. */
    fun resumen(e: Estado, hoy: LocalDate, dias: Int = 7): String {
        val sb = StringBuilder()
        sb.appendLine("Resumen de mi Bitácora (últimos $dias días, semana ${semanaPlan(hoy)} de 16: ${temaSemana(hoy)})")
        sb.appendLine("Racha: ${racha(e, hoy)} días. Hora de dormir objetivo: ${e.ajustes.horaDormir}.")
        sb.appendLine()
        for (i in dias - 1 downTo 0) {
            val d = hoy.minusDays(i.toLong())
            val p = plantilla(e, d)
            val en = e.entradas[d.toString()] ?: Entrada()
            val hechas = p.tareas.filter { en.tareas[it] == true }
            val faltan = p.tareas.filter { en.tareas[it] != true }
            sb.appendLine("${fechaLarga(d)}:")
            sb.appendLine("  Tareas: ${hechas.size}/${p.tareas.size}" + (if (faltan.isNotEmpty()) " (faltó: ${faltan.joinToString(", ")})" else ""))
            if (en.minutos > 0) sb.appendLine("  Estudio: ${en.minutos} min")
            val h = horasSueno(en)
            if (h != null) sb.appendLine("  Sueño: ${en.acoste} a ${en.desperte} (${formatoHoras(h)})")
            if (en.energia > 0) sb.appendLine("  Energía al despertar: ${en.energia}/5")
            en.examenes.forEach { (tipo, r) ->
                sb.appendLine("  Examen $tipo: ${r.puntaje}/${r.total}")
                r.fallos.forEach { f -> sb.appendLine("    Falló: ${f.q}" + if (f.r.isNotBlank()) " → ${f.r}" else "") }
            }
            en.reportes.forEach { sb.appendLine("  Pregunta reportada: $it") }
            if (en.nota.isNotBlank()) sb.appendLine("  Nota: ${en.nota}")
        }
        sb.appendLine()
        sb.append("Revisa mi progreso y ajusta el plan según mis falencias.")
        return sb.toString()
    }
}
