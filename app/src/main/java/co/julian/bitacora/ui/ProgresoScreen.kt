package co.julian.bitacora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Plan
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun ProgresoScreen(e: Estado, abrirDia: (LocalDate) -> Unit) {
    val hoy = LocalDate.now()
    val lunes = Logica.lunesDe(hoy)

    var minutosSemana = 0
    var ejercicioSemana = 0
    for (i in 0..6) {
        val en = e.entradas[lunes.plusDays(i.toLong()).toString()] ?: continue
        minutosSemana += en.minutos
        if (en.tareas["ejercicio"] == true) ejercicioSemana++
    }
    var horas = 0.0
    var nHoras = 0
    var energia = 0
    var nEnergia = 0
    var aciertos = 0
    var preguntas = 0
    for (i in 0 until 14) {
        val en = e.entradas[hoy.minusDays(i.toLong()).toString()] ?: continue
        if (i < 7) {
            Logica.horasSueno(en)?.let { horas += it; nHoras++ }
            if (en.energia > 0) { energia += en.energia; nEnergia++ }
        }
        en.examenes.values.forEach { aciertos += it.puntaje; preguntas += it.total }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Tarjeta {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Dato("${Logica.racha(e, hoy)}", "días seguidos al 60 % o más", Modifier.weight(1f))
                    Dato("${(minutosSemana / 6.0).roundToInt() / 10.0} h".replace(".", ","), "de estudio esta semana", Modifier.weight(1f))
                    Dato("$ejercicioSemana/3", "sesiones de ejercicio esta semana", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Dato(if (nHoras > 0) Logica.formatoHoras(horas / nHoras) else "–", "de sueño en promedio (7 días)", Modifier.weight(1f))
                    Dato(if (nEnergia > 0) "${((energia.toDouble() / nEnergia) * 10).roundToInt() / 10.0}/5".replace(".", ",") else "–", "energía al despertar", Modifier.weight(1f))
                    Dato(if (preguntas > 0) "${(aciertos * 100.0 / preguntas).roundToInt()} %" else "–", "acierto en exámenes (14 días)", Modifier.weight(1f))
                }
            }
        }
        item {
            val c = LocalColoresTipo.current
            val acento = MaterialTheme.colorScheme.primary
            Tarjeta("Últimas 4 semanas") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Logica.LETRAS_DIAS.let { listOf(it[1], it[2], it[3], it[4], it[5], it[6], it[0]) }.forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val inicio = lunes.minusDays(21)
                for (semana in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (d in 0 until 7) {
                            val dia = inicio.plusDays((semana * 7 + d).toLong())
                            val futuro = dia.isAfter(hoy)
                            val pct = Logica.puntaje(e, dia).pct
                            val nivel = if (futuro) 0 else when {
                                pct >= 0.99f -> 3
                                pct >= 0.6f -> 2
                                pct > 0f -> 1
                                else -> 0
                            }
                            val fondo = when (nivel) {
                                3 -> acento
                                2 -> acento.copy(alpha = 0.55f)
                                1 -> acento.copy(alpha = 0.25f)
                                else -> c.suave("rest")
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .alpha(if (futuro) 0.4f else 1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(fondo)
                                    .then(if (dia == hoy) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(7.dp)) else Modifier)
                                    .clickable { abrirDia(dia) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${dia.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (nivel == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                Text("Toca un día para abrir su registro. Más oscuro significa más tareas cumplidas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            val actual = Logica.semanaN(hoy)
            Tarjeta("Ruta de 16 semanas") {
                var n = 0
                Plan.PLAN.forEach { (etapa, temas) ->
                    Etiqueta(etapa, MaterialTheme.colorScheme.primary)
                    temas.forEach { tema ->
                        n++
                        val ini = Logica.INICIO.plusDays(((n - 1) * 7).toLong())
                        val esActual = n == actual
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (esActual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .alpha(if (n < actual) 0.5f else 1f)
                                .padding(horizontal = if (esActual) 10.dp else 0.dp, vertical = 6.dp),
                        ) {
                            Text("S${n.toString().padStart(2, '0')}", fontFamily = FontFamily.Monospace, color = if (esActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tema, style = MaterialTheme.typography.bodyMedium)
                                Text("${Logica.fechaCorta(ini)} – ${Logica.fechaCorta(ini.plusDays(6))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text(
                    "Regla del proyecto: termina la versión mínima antes de agregar funciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Dato(valor: String, texto: String, modifier: Modifier) {
    Column(modifier) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
