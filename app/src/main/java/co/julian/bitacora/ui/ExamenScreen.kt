package co.julian.bitacora.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.julian.bitacora.data.Claude
import co.julian.bitacora.data.ErrorClaude
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.Evaluacion
import co.julian.bitacora.data.Fallo
import co.julian.bitacora.data.IA
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Practico
import co.julian.bitacora.data.Pregunta
import co.julian.bitacora.data.Prompts
import co.julian.bitacora.data.ResultadoExamen
import co.julian.bitacora.data.Store
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Estado del examen en curso. Vive fuera de la pantalla para no perderse al cambiar de pestaña. */
private object ExamenEnCurso {
    var tipo by mutableStateOf("diario")
    var cargando by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var preguntas by mutableStateOf<List<Pregunta>?>(null)
    var tipoPreguntas by mutableStateOf("diario")
    var diaExamen by mutableStateOf(LocalDate.now())
    val respuestas = mutableStateListOf<Int>()
    var calificado by mutableStateOf(false)
    var practico by mutableStateOf<Practico?>(null)
    var respuestaPractico by mutableStateOf("")
    var foto by mutableStateOf<ByteArray?>(null)
    var evaluacion by mutableStateOf<Evaluacion?>(null)
}

@Composable
fun ExamenScreen(e: Estado, irAAjustes: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val x = ExamenEnCurso
    val hoyReal = LocalDate.now()
    val pendientes = Logica.examenesPendientes(e, hoyReal)
    /** Día del examen: el pendiente más antiguo, o hoy si está al día. */
    val hoy = pendientes.firstOrNull() ?: hoyReal
    val hoyK = hoy.toString()
    val bloqueado = pendientes.isNotEmpty()
    val tipos = Prompts.tipos(hoy).map {
        if (bloqueado && it.id != "diario") it.copy(disponible = false, descripcion = "Primero ponte al día con los pendientes") else it
    }
    if (tipos.none { it.id == x.tipo && it.disponible }) x.tipo = "diario"
    val hechosHoy = Logica.entrada(e, hoy).examenes
    val sinClave = !IA.hayTexto(e.ajustes)

    fun guardarResultado(tipo: String, r: ResultadoExamen, aprobado: Boolean) {
        Store.editarEntrada(ctx, x.diaExamen.toString()) { en ->
            en.copy(
                examenes = en.examenes + (tipo to r),
                tareas = if (aprobado) en.tareas + ("logica" to true) else en.tareas,
            )
        }
    }

    fun generar() {
        x.cargando = true
        x.error = null
        val tipo = x.tipo
        x.diaExamen = hoy
        scope.launch {
            try {
                if (tipo == "practico") {
                    val txt = IA.pedir(e.ajustes, Prompts.practico(hoy), maxTokens = 2000)
                    x.practico = Claude.practicoDe(Claude.extraerJson(txt), Prompts.formatoPractico(hoy))
                    x.respuestaPractico = ""
                    x.foto = null
                    x.evaluacion = null
                } else {
                    val txt = IA.pedir(e.ajustes, Prompts.examen(tipo, e, hoy), maxTokens = 8000)
                    val ps = Claude.preguntasDe(Claude.extraerJson(txt))
                    x.preguntas = ps
                    x.tipoPreguntas = tipo
                    x.respuestas.clear()
                    repeat(ps.size) { x.respuestas.add(-1) }
                    x.calificado = false
                }
            } catch (err: ErrorClaude) {
                x.error = err.message
            } catch (err: Exception) {
                x.error = "No se pudo completar. Intenta de nuevo."
            } finally {
                x.cargando = false
            }
        }
    }

    fun evaluar() {
        val p = x.practico ?: return
        if (x.respuestaPractico.isBlank() && x.foto == null) {
            x.error = "Escribe tu solución o sube la foto de tu diagrama."
            return
        }
        if (x.foto != null && !IA.hayFotos(e.ajustes)) {
            x.error = "Para revisar fotos agrega tu clave gratuita de Gemini en Ajustes, o escribe tu solución y quita la foto."
            return
        }
        x.cargando = true
        x.error = null
        scope.launch {
            try {
                val txt = IA.pedir(
                    e.ajustes,
                    Prompts.evaluar(p, x.respuestaPractico, x.foto != null),
                    imagenJpeg = x.foto, maxTokens = 3000,
                )
                val ev = Claude.evaluacionDe(Claude.extraerJson(txt))
                x.evaluacion = ev
                val fallos = if (ev.nota < 6) listOf(Fallo(("Ejercicio: " + p.enunciado).take(160), ev.mejorar.take(120))) else emptyList()
                guardarResultado("practico", ResultadoExamen(ev.nota, 10, fallos), ev.nota >= 6)
            } catch (err: ErrorClaude) {
                x.error = err.message
            } catch (err: Exception) {
                x.error = "No se pudo completar. Intenta de nuevo."
            } finally {
                x.cargando = false
            }
        }
    }

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = Claude.cargarImagen(ctx, uri)
            if (bytes == null) x.error = "No se pudo abrir la foto." else x.foto = bytes
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Mini examen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Semana ${Logica.semanaPlan(hoy)}: ${Logica.temaSemana(hoy)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bloqueado) {
            item {
                val c = LocalColoresTipo.current
                Tarjeta(fondo = c.suave("uni")) {
                    Etiqueta("Examen pendiente", c.alerta)
                    Text(
                        "Tienes ${pendientes.size} " + (if (pendientes.size == 1) "examen diario pendiente" else "exámenes diarios pendientes") +
                            ". Haz primero el del ${Logica.fechaLarga(hoy)}: es sobre lo que tocaba estudiar ese día. Hasta ponerte al día no se abren los demás exámenes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (sinClave) {
            item {
                Tarjeta("Falta tu clave gratuita") {
                    Text(
                        "Los exámenes usan Groq, que es gratis. Agrega tu clave en Ajustes; para revisar fotos de diagramas agrega también la de Gemini.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = irAAjustes, modifier = Modifier.fillMaxWidth()) { Text("Ir a Ajustes") }
                    TextButton(onClick = { abrirBitacoraWeb(ctx) }, contentPadding = PaddingValues(0.dp)) {
                        Text("Mientras tanto, usar la Bitácora web")
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tipos.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { t ->
                            val sel = x.tipo == t.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = t.disponible && !x.cargando) { x.tipo = t.id; x.error = null },
                                shape = RoundedCornerShape(12.dp),
                                color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        t.nombre + if (hechosHoy.containsKey(t.id)) " ✓" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (t.disponible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(t.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (x.tipo == "practico") {
            val p = x.practico
            if (p == null) {
                item {
                    Text(
                        if (p == null && Prompts.formatoPractico(hoy) == "diagrama")
                            "Te propongo un problema corto. Lo resuelves con un diagrama de flujo (en papel o en PSeInt) y subes la foto, o escribes el pseudocódigo."
                        else "Te propongo un problema corto para resolver en Java o pseudocódigo. Lo escribes aquí y te digo qué está bien y qué mejorar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item { BotonPrincipal(if (x.cargando) "Preparando el ejercicio…" else "Dame un ejercicio", x.cargando || sinClave) { generar() } }
            } else {
                item {
                    Tarjeta(fondo = MaterialTheme.colorScheme.surfaceVariant) {
                        Etiqueta(if (p.formato == "diagrama") "Ejercicio · diagrama de flujo" else "Ejercicio · código")
                        Text(p.enunciado, style = MaterialTheme.typography.bodyLarge)
                        if (p.ejemplo.isNotBlank()) Text("Ejemplo: ${p.ejemplo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val ev = x.evaluacion
                if (ev == null) {
                    item {
                        OutlinedTextField(
                            value = x.respuestaPractico,
                            onValueChange = { x.respuestaPractico = it },
                            label = { Text("Tu solución (pseudocódigo o Java)") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            minLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Text(if (x.foto == null) "Subir foto del diagrama" else "Cambiar foto") }
                            if (x.foto != null) TextButton(onClick = { x.foto = null }) { Text("Quitar") }
                        }
                        x.foto?.let { bytes ->
                            val bmp = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                            if (bmp != null) Image(bmp, contentDescription = "Foto de tu diagrama", modifier = Modifier.height(140.dp).padding(top = 8.dp))
                        }
                    }
                    if (p.pista.isNotBlank()) {
                        item {
                            var ver by remember(p) { mutableStateOf(false) }
                            TextButton(onClick = { ver = !ver }, contentPadding = PaddingValues(0.dp)) { Text(if (ver) "Ocultar pista" else "Ver una pista") }
                            if (ver) Text(p.pista, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { BotonPrincipal(if (x.cargando) "Revisando tu solución…" else "Revisar mi solución", x.cargando || sinClave) { evaluar() } }
                } else {
                    item {
                        val c = LocalColoresTipo.current
                        Tarjeta {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${ev.nota}/10", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "  " + when {
                                        ev.nota >= 8 -> "Muy bien resuelto."
                                        ev.nota >= 6 -> "Funciona, pero se puede mejorar."
                                        else -> "Todavía no está. Revisa los comentarios e inténtalo de nuevo."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Comentario("Lo que hiciste bien", ev.bien, c.bien)
                            Comentario("Qué mejorar", ev.mejorar, c.alerta)
                            if (ev.solucion.isNotBlank()) {
                                Etiqueta("Una solución posible")
                                Text(
                                    ev.solucion,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .horizontalScroll(rememberScrollState())
                                        .padding(10.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { x.evaluacion = null }) { Text("Intentar de nuevo") }
                                OutlinedButton(onClick = { x.practico = null; generar() }, enabled = !x.cargando && !sinClave) { Text("Otro ejercicio") }
                            }
                        }
                    }
                }
            }
        } else {
            val ps = x.preguntas
            if (ps == null || x.tipoPreguntas != x.tipo) {
                hechosHoy[x.tipo]?.let { r ->
                    item { Text("Ya hiciste este examen: ${r.puntaje}/${r.total}. Puedes repetirlo con preguntas nuevas.", style = MaterialTheme.typography.bodyMedium) }
                }
                item { BotonPrincipal(if (x.cargando) "Preparando preguntas…" else "Empezar examen", x.cargando || sinClave) { generar() } }
            } else {
                itemsIndexed(ps) { i, q ->
                    TarjetaPregunta(
                        i, q, x.respuestas.getOrElse(i) { -1 }, x.calificado,
                        reportar = {
                            Store.editarEntrada(ctx, hoyK) { en ->
                                en.copy(reportes = (en.reportes + "${q.pregunta.take(200)} (marcada correcta: ${q.opciones[q.correcta].take(80)})").takeLast(20))
                            }
                        },
                    ) { j ->
                        if (!x.calificado && i < x.respuestas.size) x.respuestas[i] = j
                    }
                }
                item {
                    if (!x.calificado) {
                        val faltan = x.respuestas.count { it < 0 }
                        BotonPrincipal(if (faltan > 0) "Te faltan $faltan por responder" else "Calificar", faltan > 0) {
                            x.calificado = true
                            val buenas = ps.indices.count { x.respuestas[it] == ps[it].correcta }
                            val fallos = ps.indices.filter { x.respuestas[it] != ps[it].correcta }.map {
                                Fallo(ps[it].pregunta.take(160), ps[it].opciones[ps[it].correcta].take(120))
                            }
                            guardarResultado(x.tipoPreguntas, ResultadoExamen(buenas, ps.size, fallos.take(10)), buenas.toFloat() / ps.size >= 0.6f)
                        }
                    } else {
                        val buenas = ps.indices.count { x.respuestas[it] == ps[it].correcta }
                        val pct = buenas.toFloat() / ps.size
                        Tarjeta {
                            Text("$buenas/${ps.size}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    pct >= 0.8f -> "Muy bien. Sigue con el siguiente tema."
                                    pct >= 0.6f -> "Vas bien. Repasa las explicaciones de las que fallaste."
                                    else -> "Vuelve a repasar el tema antes de avanzar. Las que fallaste entran en tus próximos exámenes."
                                },
                            )
                            OutlinedButton(onClick = { x.preguntas = null }) { Text("Hacer otro examen") }
                        }
                    }
                }
            }
        }

        x.error?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }

        item {
            val historial = (0 until 14).flatMap { i ->
                val d = hoyReal.minusDays(i.toLong())
                (e.entradas[d.toString()]?.examenes ?: emptyMap()).map { (t, r) -> Triple(d, t, r) }
            }.take(6)
            if (historial.isNotEmpty()) {
                Tarjeta("Últimos resultados") {
                    historial.forEach { (d, t, r) ->
                        Row {
                            Text("${Prompts.nombre(t)} · ${Logica.fechaCorta(d)}", modifier = Modifier.weight(1f))
                            Text("${r.puntaje}/${r.total}", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

const val URL_BITACORA_WEB = "https://claude.ai/artifact/UmbjLANMzBCcXE6zjB2e15"

fun abrirBitacoraWeb(ctx: android.content.Context) {
    try {
        ctx.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(URL_BITACORA_WEB))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
    }
}

@Composable
private fun BotonPrincipal(texto: String, deshabilitado: Boolean, accion: () -> Unit) {
    Button(onClick = accion, enabled = !deshabilitado, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (texto.endsWith("…")) {
            CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
        }
        Text(texto)
    }
}

@Composable
private fun Comentario(titulo: String, texto: String, color: androidx.compose.ui.graphics.Color) {
    if (texto.isBlank()) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Etiqueta(titulo, color)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TarjetaPregunta(
    i: Int,
    q: Pregunta,
    elegida: Int,
    calificado: Boolean,
    reportar: () -> Unit,
    elegir: (Int) -> Unit,
) {
    val c = LocalColoresTipo.current
    var reportada by remember(q) { mutableStateOf(false) }
    Tarjeta {
        Etiqueta("${(i + 1).toString().padStart(2, '0')} · ${q.tema}")
        Text(q.pregunta, fontWeight = FontWeight.SemiBold)
        if (q.codigo.isNotBlank()) {
            Text(
                q.codigo,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            )
        }
        q.opciones.forEachIndexed { j, o ->
            val fondo = when {
                calificado && j == q.correcta -> c.suave("gym")
                calificado && j == elegida -> c.suave("uni")
                j == elegida -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borde = when {
                calificado && j == q.correcta -> c.bien
                calificado && j == elegida -> c.alerta
                j == elegida -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !calificado) { elegir(j) },
                shape = RoundedCornerShape(10.dp),
                color = fondo,
                border = BorderStroke(1.dp, borde),
            ) {
                Text("${"ABCD"[j]}. $o", modifier = Modifier.padding(12.dp))
            }
        }
        if (calificado) {
            Text(
                (if (elegida == q.correcta) "Correcto. " else "La respuesta era la ${"ABCD"[q.correcta]}. ") + q.explicacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { reportar(); reportada = true },
                enabled = !reportada,
                contentPadding = PaddingValues(0.dp),
            ) { Text(if (reportada) "Reportada: Claude la revisará" else "Reportar pregunta mal planteada") }
        }
    }
}
