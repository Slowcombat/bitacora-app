package co.julian.bitacora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.julian.bitacora.data.Entrada
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Plan
import co.julian.bitacora.data.Store
import java.time.LocalDate

@Composable
fun HoyScreen(
    e: Estado,
    fecha: LocalDate,
    setFecha: (LocalDate) -> Unit,
    irAHorario: () -> Unit,
    irAExamen: () -> Unit,
) {
    val ctx = LocalContext.current
    val k = fecha.toString()
    val p = Logica.plantilla(e, fecha)
    val en = Logica.entrada(e, fecha)
    val hoy = LocalDate.now()
    fun editar(cambio: (Entrada) -> Entrada) = Store.editarEntrada(ctx, k, cambio)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Etiqueta("Bitácora · Marzo 2027")
                    Text("Faltan ${Logica.diasParaMeta(hoy)} días", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                val n = Logica.semanaN(fecha)
                if (n in 1..16) Pastilla("Semana $n de 16", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
            }
        }
        item { NavegadorFecha(fecha, setFecha) }

        e.planRemoto?.takeIf { it.mensaje.isNotBlank() && fecha == hoy }?.let { plan ->
            item {
                val c = LocalColoresTipo.current
                Tarjeta(fondo = c.suave("eng")) {
                    Etiqueta("Revisión de Claude" + if (plan.actualizado.isNotBlank()) " · ${plan.actualizado}" else "", c.principal("eng"))
                    Text(plan.mensaje, style = MaterialTheme.typography.bodyMedium)
                    if (plan.refuerzo.isNotEmpty()) {
                        Text("A reforzar: ${plan.refuerzo.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            val l = Logica.leccion(fecha)
            val c = LocalColoresTipo.current
            Tarjeta(fondo = c.suave("study")) {
                Etiqueta("Qué hacer hoy", c.principal("study"))
                if (l != null) {
                    Text(l.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    l.pasos.forEachIndexed { i, paso ->
                        Row {
                            Text("${i + 1}.", fontFamily = FontFamily.Monospace, color = c.principal("study"), modifier = Modifier.width(24.dp))
                            Text(paso, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    BloqueNota("Inglés", l.ingles, "eng")
                    BloqueNota("En qué mejorar", l.mejorar, "gym")
                } else {
                    Text(Logica.temaSemana(fecha), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Estudia el tema de la semana en tus bloques y, al terminar, pide un ejercicio práctico en la pestaña Examen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (fecha == hoy) {
                    TextButton(onClick = irAExamen, contentPadding = PaddingValues(0.dp)) { Text("Ir al examen de hoy") }
                }
            }
        }

        if (fecha == hoy) {
            val fallos = Logica.fallosRecientes(e, hoy, 7).take(3)
            if (fallos.isNotEmpty()) {
                item {
                    Tarjeta("Repasa lo que fallaste esta semana") {
                        fallos.forEach { f ->
                            Text("• ${f.q}", style = MaterialTheme.typography.bodyMedium)
                            if (f.r.isNotBlank()) Text("  → ${f.r}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            val c = LocalColoresTipo.current
            val pts = Logica.puntaje(e, fecha)
            Tarjeta("Tareas del día") {
                Plan.TAREAS.forEach { t ->
                    val esperada = t.id in p.tareas
                    val hecha = en.tareas[t.id] == true
                    val color = c.principal(t.tipo)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.suave(t.tipo).copy(alpha = if (esperada) 1f else 0.5f))
                            .clickable { editar { it.copy(tareas = it.tareas + (t.id to !hecha)) } }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (hecha) color else Color.Transparent)
                                .border(2.dp, color, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hecha) Text("✓", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val titulo = if (t.id == "dormir") "A dormir antes de las ${e.ajustes.horaDormir}" else t.titulo
                            Text(
                                titulo,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (hecha) TextDecoration.LineThrough else null,
                            )
                            Text(
                                if (esperada) t.detalle else "Opcional hoy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { pts.pct },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    )
                    Text("  ${pts.hechas}/${pts.total}", fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            var nota by remember(k) { mutableStateOf(en.nota) }
            Tarjeta("Estudio") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Minutos", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { editar { it.copy(minutos = (it.minutos - 15).coerceAtLeast(0)) } }) { Text("−15") }
                    Text("${en.minutos}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { editar { it.copy(minutos = it.minutos + 15) } }) { Text("+15") }
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { v -> nota = v; editar { it.copy(nota = v) } },
                    label = { Text("Qué aprendí o qué me costó") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        }

        item {
            val horas = Logica.horasSueno(en)
            Tarjeta("Sueño") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { elegirHora(ctx, en.acoste.ifBlank { "23:30" }) { h -> editar { it.copy(acoste = h) } } }, modifier = Modifier.weight(1f)) {
                        Text("Me acosté: ${en.acoste.ifBlank { "—" }}")
                    }
                    OutlinedButton(onClick = { elegirHora(ctx, en.desperte.ifBlank { "6:00" }) { h -> editar { it.copy(desperte = h) } } }, modifier = Modifier.weight(1f)) {
                        Text("Me levanté: ${en.desperte.ifBlank { "—" }}")
                    }
                }
                Etiqueta("Energía al despertar")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { n ->
                        val sel = en.energia == n
                        val c = LocalColoresTipo.current
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) c.principal("eng") else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { editar { it.copy(energia = if (it.energia == n) 0 else n) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$n", color = if (sel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    when {
                        horas == null -> "1 es agotado y 5 es con toda la energía."
                        horas < 7 -> "Dormiste ${Logica.formatoHoras(horas)}. Apunta a 7 horas o más cuando puedas."
                        else -> "Dormiste ${Logica.formatoHoras(horas)}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Tarjeta(
                "Agenda del día",
                accion = { TextButton(onClick = irAHorario) { Text("Editar") } },
            ) {
                when (p.editado) {
                    "dia" -> Pastilla("Horario solo de este día", LocalColoresTipo.current.alerta, LocalColoresTipo.current.suave("uni"))
                    "semana" -> Pastilla("Horario editado", LocalColoresTipo.current.alerta, LocalColoresTipo.current.suave("uni"))
                }
                Column { p.bloques.forEach { FilaBloque(it) } }
            }
        }
    }
}

@Composable
private fun BloqueNota(titulo: String, texto: String, tipo: String) {
    val c = LocalColoresTipo.current
    Row {
        Box(Modifier.width(3.dp).height(36.dp).background(c.principal(tipo)))
        Column(Modifier.padding(start = 10.dp)) {
            Etiqueta(titulo)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
