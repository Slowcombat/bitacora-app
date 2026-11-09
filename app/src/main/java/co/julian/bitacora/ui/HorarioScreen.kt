package co.julian.bitacora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.julian.bitacora.alarm.Scheduler
import co.julian.bitacora.data.Alarma
import co.julian.bitacora.data.Bloque
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Plan
import co.julian.bitacora.data.Store
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HorarioScreen(e: Estado, fecha: LocalDate, setFecha: (LocalDate) -> Unit) {
    val ctx = LocalContext.current
    val k = fecha.toString()
    val fase = Logica.fase(fecha)
    val dow = Logica.dow(fecha)
    val p = Logica.plantilla(e, fecha)
    var alcance by remember(k) { mutableStateOf(if (p.editado == "semana") "semana" else "dia") }
    var version by remember { mutableStateOf(0) }
    val filas = remember(k, alcance, version) {
        mutableStateListOf<Bloque>().apply {
            addAll(if (alcance == "semana") Logica.bloquesSemana(e, fase, dow) else p.bloques)
        }
    }
    var mensaje by remember(k, alcance) { mutableStateOf<String?>(null) }
    val nombreDia = Logica.NOMBRES_DIAS[dow].lowercase()
    val plural = if (nombreDia == "sábado" || nombreDia == "domingo") nombreDia + "s" else nombreDia

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NavegadorFecha(fecha, setFecha) }
        item {
            Tarjeta("Editar la agenda") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = alcance == "dia",
                        onClick = { alcance = "dia" },
                        label = { Text("Solo el ${fecha.dayOfMonth} de ${Logica.nombreMes(fecha)}") },
                    )
                    FilterChip(
                        selected = alcance == "semana",
                        onClick = { alcance = "semana" },
                        label = { Text("Todos los $plural" + if (fase == "f1") " (con universidad)" else " (vacaciones)") },
                    )
                }
                Text(
                    "Las tareas del día se ajustan solas según los bloques. Activa \"Avisar\" en los bloques que quieras que te notifiquen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(filas, key = { i, _ -> "$k-$alcance-$version-$i" }) { i, b ->
            EditorBloque(
                b = b,
                cambiar = { nuevo -> filas[i] = nuevo },
                quitar = { filas.removeAt(i) },
            )
        }
        item {
            OutlinedButton(onClick = { filas.add(Bloque(tipo = "study")) }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Agregar bloque")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val limpias = Logica.ordenar(filas.filter { it.titulo.isNotBlank() })
                        if (limpias.isEmpty()) {
                            mensaje = "Agrega al menos un bloque con nombre de actividad."
                        } else {
                            Store.update(ctx) { st ->
                                if (alcance == "dia") st.copy(dias = st.dias + (k to limpias))
                                else st.copy(semana = st.semana + ("$fase-$dow" to limpias))
                            }
                            version++
                            mensaje = "Guardado. Los avisos ya usan este horario."
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Guardar") }
                TextButton(onClick = {
                    Store.update(ctx) { st ->
                        if (alcance == "dia") {
                            if (Plan.DIAS_BASE.containsKey(k)) st.copy(dias = st.dias + (k to emptyList()))
                            else st.copy(dias = st.dias - k)
                        } else st.copy(semana = st.semana - "$fase-$dow")
                    }
                    version++
                    mensaje = "Volviste al horario original."
                }) { Text(if (alcance == "dia") "Usar horario normal" else "Restablecer") }
            }
            mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }

        item { Text("Alarmas para despertar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
        item {
            Text(
                "Suenan aunque el celular esté bloqueado. Las de \"Semestre\" funcionan hasta el 20 de noviembre y las de \"Vacaciones\" desde el 21.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val ahora = LocalDateTime.now()
        itemsIndexed(e.alarmas, key = { _, a -> "alarma-${a.id}" }) { _, a ->
            EditorAlarma(
                a = a,
                proxima = Scheduler.proximaAlarma(a, ahora),
                cambiar = { nueva -> Store.update(ctx) { st -> st.copy(alarmas = st.alarmas.map { if (it.id == a.id) nueva else it }) } },
                quitar = { Store.update(ctx) { st -> st.copy(alarmas = st.alarmas.filter { it.id != a.id }) } },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        Store.update(ctx) { st ->
                            val id = (st.alarmas.maxOfOrNull { it.id } ?: 0) + 1
                            st.copy(alarmas = st.alarmas + Alarma(id, 6, 30, listOf(1, 2, 3, 4, 5), "Hora de levantarte"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("+ Alarma semanal") }
                OutlinedButton(
                    onClick = {
                        elegirFecha(ctx, fecha) { d ->
                            Store.update(ctx) { st ->
                                val id = (st.alarmas.maxOfOrNull { it.id } ?: 0) + 1
                                st.copy(alarmas = st.alarmas + Alarma(id, 6, 0, emptyList(), "Alarma del ${Logica.fechaCorta(d)}", fecha = d.toString()))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("+ Solo un día") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorBloque(b: Bloque, cambiar: (Bloque) -> Unit, quitar: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalColoresTipo.current
    var menu by remember { mutableStateOf(false) }
    val franja = c.principal(b.tipo)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind { drawRect(franja, size = Size(4.dp.toPx(), size.height)) },
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { elegirHora(ctx, b.inicio) { cambiar(b.copy(inicio = it)) } }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(b.inicio.ifBlank { "Inicio" })
                }
                Text("–")
                OutlinedButton(onClick = { elegirHora(ctx, b.fin.ifBlank { b.inicio }) { cambiar(b.copy(fin = it)) } }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(b.fin.ifBlank { "Fin" })
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = quitar) { Icon(Icons.Filled.Delete, contentDescription = "Quitar bloque") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { menu = true }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(Plan.nombreTipo(b.tipo), color = c.principal(b.tipo))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Plan.TIPOS.forEach { (id, nombre) ->
                            DropdownMenuItem(text = { Text(nombre) }, onClick = { cambiar(b.copy(tipo = id)); menu = false })
                        }
                    }
                }
                Box(Modifier.weight(1f))
                Text("Avisar", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = Logica.avisa(b),
                    onCheckedChange = { cambiar(b.copy(avisar = it)) },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            OutlinedTextField(
                value = b.titulo,
                onValueChange = { cambiar(b.copy(titulo = it)) },
                label = { Text("Actividad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = b.detalle,
                onValueChange = { cambiar(b.copy(detalle = it)) },
                label = { Text("Detalle (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditorAlarma(a: Alarma, proxima: LocalDateTime?, cambiar: (Alarma) -> Unit, quitar: () -> Unit) {
    val ctx = LocalContext.current
    var etiqueta by remember(a.id) { mutableStateOf(a.etiqueta) }
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                elegirHora(ctx, "${a.hora}:${a.minuto.toString().padStart(2, '0')}") { h ->
                    Logica.parseHora(h)?.let { cambiar(a.copy(hora = it.hour, minuto = it.minute)) }
                }
            }) {
                Text(
                    "${a.hora}:${a.minuto.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(Modifier.weight(1f))
            Switch(checked = a.activa, onCheckedChange = { cambiar(a.copy(activa = it)) })
            IconButton(onClick = quitar) { Icon(Icons.Filled.Delete, contentDescription = "Borrar alarma") }
        }
        OutlinedTextField(
            value = etiqueta,
            onValueChange = { etiqueta = it; cambiar(a.copy(etiqueta = it)) },
            label = { Text("Mensaje") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (a.fecha != null) {
            Text("Solo el ${Logica.fechaLarga(LocalDate.parse(a.fecha))}", style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3, 4, 5, 6, 0).forEach { d ->
                    val sel = d in a.dias
                    FilterChip(
                        selected = sel,
                        onClick = { cambiar(a.copy(dias = if (sel) a.dias - d else (a.dias + d).sorted())) },
                        label = { Text(Logica.LETRAS_DIAS[d]) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("todas" to "Siempre", "f1" to "Semestre", "f2" to "Vacaciones").forEach { (id, nombre) ->
                    FilterChip(selected = a.fase == id, onClick = { cambiar(a.copy(fase = id)) }, label = { Text(nombre) })
                }
            }
        }
        Text(
            if (!a.activa) "Apagada"
            else proxima?.let { "Próxima: ${Logica.fechaLarga(it.toLocalDate())} a las ${Logica.hora(it.toLocalTime())}" }
                ?: "No sonará en los próximos días",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
