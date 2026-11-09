package co.julian.bitacora.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.julian.bitacora.data.Bloque
import co.julian.bitacora.data.Logica
import java.time.LocalDate

@Composable
fun Tarjeta(
    titulo: String? = null,
    accion: (@Composable () -> Unit)? = null,
    fondo: Color = MaterialTheme.colorScheme.surface,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null || accion != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (titulo != null) {
                        Text(
                            titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    accion?.invoke()
                }
            }
            contenido()
        }
    }
}

@Composable
fun Etiqueta(texto: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
fun Pastilla(texto: String, color: Color, fondo: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun NavegadorFecha(fecha: LocalDate, setFecha: (LocalDate) -> Unit) {
    val hoy = LocalDate.now()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (fecha == hoy) "Hoy, " else "") + Logica.fechaLarga(fecha),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (Logica.fase(fecha) == "f1") "Etapa con universidad" else "Etapa de vacaciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { setFecha(fecha.minusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
        }
        if (fecha != hoy) TextButton(onClick = { setFecha(hoy) }) { Text("Hoy") }
        IconButton(onClick = { setFecha(fecha.plusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
        }
    }
}

@Composable
fun FilaBloque(b: Bloque) {
    val c = LocalColoresTipo.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            if (b.inicio.isNotBlank() && b.fin.isNotBlank()) "${b.inicio}–${b.fin}" else b.inicio.ifBlank { "Sin hora" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp).padding(top = 2.dp),
        )
        Box(
            Modifier
                .width(3.dp)
                .height(40.dp)
                .background(c.principal(b.tipo), RoundedCornerShape(2.dp)),
        )
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(b.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (b.detalle.isNotBlank()) {
                Text(b.detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun elegirHora(ctx: Context, actual: String, alElegir: (String) -> Unit) {
    val t = Logica.parseHora(actual) ?: java.time.LocalTime.of(7, 0)
    TimePickerDialog(ctx, { _, h, m -> alElegir("$h:${m.toString().padStart(2, '0')}") }, t.hour, t.minute, true).show()
}

fun elegirFecha(ctx: Context, actual: LocalDate, alElegir: (LocalDate) -> Unit) {
    DatePickerDialog(
        ctx,
        { _, y, m, d -> alElegir(LocalDate.of(y, m + 1, d)) },
        actual.year, actual.monthValue - 1, actual.dayOfMonth,
    ).show()
}
