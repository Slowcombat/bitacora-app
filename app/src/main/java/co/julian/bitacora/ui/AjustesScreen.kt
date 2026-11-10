package co.julian.bitacora.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import co.julian.bitacora.alarm.Notifs
import co.julian.bitacora.alarm.Scheduler
import co.julian.bitacora.data.Claude
import co.julian.bitacora.data.ErrorClaude
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.IA
import co.julian.bitacora.data.Sync
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Prompts
import co.julian.bitacora.data.Store
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AjustesScreen(e: Estado) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresco by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresco++
        onPauseOrDispose { }
    }

    fun abrir(intent: Intent) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

        item {
            Tarjeta("Exámenes con IA") {
                Text(
                    "Groq hace los exámenes y es gratis. Gemini revisa las fotos de tus diagramas y también es gratis, con límite diario. Claude es opcional y de pago. Las claves solo se guardan en este celular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CampoClave(
                    titulo = "Groq · exámenes",
                    ayuda = "Créala gratis en console.groq.com → API Keys. Empieza por gsk_.",
                    inicial = e.ajustes.groqKey,
                    modeloInicial = e.ajustes.groqModelo,
                    guardar = { k, m -> Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(groqKey = k, groqModelo = m.ifBlank { "openai/gpt-oss-120b" })) } },
                    probar = { k, m -> IA.groq(k, m, Prompts.PRUEBA, 200) },
                )
                CampoClave(
                    titulo = "Gemini · fotos de diagramas",
                    ayuda = "Créala gratis en aistudio.google.com → Get API key. Empieza por AIza.",
                    inicial = e.ajustes.geminiKey,
                    modeloInicial = e.ajustes.geminiModelo,
                    guardar = { k, m -> Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(geminiKey = k, geminiModelo = m.ifBlank { "gemini-flash-latest" })) } },
                    probar = { k, m -> IA.gemini(k, m, Prompts.PRUEBA, null, 200) },
                )
                CampoClave(
                    titulo = "Claude · opcional, de pago",
                    ayuda = "Solo si algún día quieres usar Claude por API (console.anthropic.com). Se cobra aparte del plan Pro.",
                    inicial = e.ajustes.apiKey,
                    modeloInicial = e.ajustes.modelo,
                    guardar = { k, m -> Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(apiKey = k, modelo = m.ifBlank { "claude-sonnet-5-5" })) } },
                    probar = { k, m -> Claude.pedir(k, m, Prompts.PRUEBA, maxTokens = 50) },
                )
                Text(
                    "Ahora los exámenes usan: " + (IA.nombreTexto(e.ajustes) ?: "ninguno (falta una clave)") +
                        ". Fotos: " + (if (e.ajustes.geminiKey.isNotBlank()) "Gemini" else if (e.ajustes.apiKey.isNotBlank()) "Claude" else "sin configurar") + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        item {
            var repo by remember { mutableStateOf(e.ajustes.githubRepo) }
            var token by remember { mutableStateOf(e.ajustes.githubToken) }
            var verToken by remember { mutableStateOf(false) }
            var estadoSync by remember { mutableStateOf<String?>(null) }
            var sincronizando by remember { mutableStateOf(false) }
            Tarjeta("Revisión automática con Claude") {
                Text(
                    "La app sube tu registro a un repositorio privado de GitHub. Cada 3 días Claude lo revisa, ajusta tus lecciones según tus fallos y publica el plan nuevo, que la app descarga sola.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it.trim() },
                    label = { Text("Repositorio privado (usuario/nombre)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text("Token de GitHub (github_pat_…)") },
                    singleLine = true,
                    visualTransformation = if (verToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { verToken = !verToken }) { Text(if (verToken) "Ocultar" else "Ver") } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !sincronizando && token.isNotBlank() && repo.contains("/"),
                    onClick = {
                        Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(githubRepo = repo, githubToken = token)) }
                        sincronizando = true
                        estadoSync = "Sincronizando…"
                        scope.launch {
                            val r = Sync.sincronizar(ctx, forzar = true)
                            estadoSync = r?.mensaje ?: "Revisa el repositorio y el token."
                            sincronizando = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar y sincronizar ahora") }
                estadoSync?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                Text(
                    (if (e.ultimaSync.isNotBlank()) "Última sincronización: ${e.ultimaSync.replace("T", " a las ").dropLast(3)}." else "Todavía no se ha sincronizado.") +
                        (e.planRemoto?.actualizado?.takeIf { it.isNotBlank() }?.let { " Plan de Claude del $it." } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val proximo = remember(e, refresco) { Scheduler.proximoAviso(e, LocalDateTime.now()) }
            Tarjeta("Recordatorios") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisos de la agenda", fontWeight = FontWeight.SemiBold)
                        Text("Antes de los bloques con \"Avisar\" y de la hora de dormir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = e.ajustes.avisosActivos, onCheckedChange = { v -> Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(avisosActivos = v)) } })
                }
                Etiqueta("Minutos antes")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { m ->
                        FilterChip(
                            selected = e.ajustes.minutosAntes == m,
                            onClick = { Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(minutosAntes = m)) } },
                            label = { Text("$m") },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hora de dormir", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        elegirHora(ctx, e.ajustes.horaDormir) { h -> Store.update(ctx) { it.copy(ajustes = it.ajustes.copy(horaDormir = h)) } }
                    }) { Text(e.ajustes.horaDormir) }
                }
                Text(
                    if (!e.ajustes.avisosActivos) "Los avisos están apagados."
                    else proximo?.let { "Próximo aviso: ${it.titulo}, ${Logica.fechaCorta(it.cuando.toLocalDate())} a las ${Logica.hora(it.cuando.toLocalTime())}" }
                        ?: "No hay avisos en los próximos días.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            @Suppress("UNUSED_VARIABLE") val r = refresco
            val notifs = Notifs.permitidas(ctx)
            val exactas = Scheduler.exactasPermitidas(ctx)
            val pantalla = Notifs.pantallaCompletaPermitida(ctx)
            Tarjeta("Permisos") {
                Permiso("Notificaciones", "Para los avisos y el sonido de las alarmas.", notifs) {
                    abrir(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
                Permiso("Alarmas exactas", "Para que suenen a la hora justa.", exactas) {
                    if (Build.VERSION.SDK_INT >= 31) abrir(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                }
                Permiso("Pantalla completa", "Para que la alarma aparezca con el celular bloqueado.", pantalla) {
                    if (Build.VERSION.SDK_INT >= 34) abrir(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")))
                }
                Permiso("Sin restricción de batería", "Algunos celulares retrasan las alarmas para ahorrar batería.", null) {
                    abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                }
            }
        }

        item {
            var aviso by remember { mutableStateOf<String?>(null) }
            Tarjeta("Probar") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        Scheduler.alarmaDePrueba(ctx)
                        aviso = "La alarma de prueba sonará en un minuto. Puedes bloquear el celular."
                    }, modifier = Modifier.weight(1f)) { Text("Alarma en 1 min") }
                    OutlinedButton(onClick = {
                        Notifs.aviso(ctx, "Aviso de prueba", "Así te llegan los recordatorios de la agenda.")
                        aviso = if (Notifs.permitidas(ctx)) "Revisa tus notificaciones." else "Activa las notificaciones primero."
                    }, modifier = Modifier.weight(1f)) { Text("Aviso ahora") }
                }
                aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun Permiso(nombre: String, detalle: String, activo: Boolean?, abrir: () -> Unit) {
    val c = LocalColoresTipo.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(nombre, fontWeight = FontWeight.SemiBold)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (activo) {
            true -> Pastilla("Activo", c.bien, c.suave("gym"))
            false -> TextButton(onClick = abrir) { Text("Activar") }
            null -> TextButton(onClick = abrir) { Text("Revisar") }
        }
    }
}

@Composable
private fun CampoClave(
    titulo: String,
    ayuda: String,
    inicial: String,
    modeloInicial: String,
    guardar: (String, String) -> Unit,
    probar: suspend (String, String) -> String,
) {
    val scope = rememberCoroutineScope()
    var clave by remember { mutableStateOf(inicial) }
    var modelo by remember { mutableStateOf(modeloInicial) }
    var ver by remember { mutableStateOf(false) }
    var verModelo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var probando by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, fontWeight = FontWeight.SemiBold)
        Text(ayuda, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it.trim() },
            label = { Text("Clave") },
            singleLine = true,
            visualTransformation = if (ver) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { ver = !ver }) { Text(if (ver) "Ocultar" else "Ver") } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (verModelo) {
            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it.trim() },
                label = { Text("Modelo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                enabled = !probando && clave.isNotBlank(),
                onClick = {
                    probando = true
                    mensaje = "Probando…"
                    scope.launch {
                        mensaje = try {
                            probar(clave, modelo)
                            guardar(clave, modelo)
                            "Funciona. Clave guardada."
                        } catch (err: ErrorClaude) {
                            err.message
                        } catch (err: Exception) {
                            "No se pudo probar: ${err.message ?: "error desconocido"}"
                        }
                        probando = false
                    }
                },
            ) { Text("Probar y guardar") }
            if (clave.isBlank() && inicial.isNotBlank()) {
                TextButton(onClick = { guardar("", modelo); mensaje = "Clave borrada." }) { Text("Borrar") }
            }
            TextButton(onClick = { verModelo = !verModelo }) { Text(if (verModelo) "Ocultar modelo" else "Modelo") }
        }
        mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
}
