package co.julian.bitacora.alarm

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.julian.bitacora.ui.BitacoraTheme
import java.time.LocalTime

/** Pantalla que aparece cuando suena una alarma, incluso con el celular bloqueado. */
class AlarmActivity : ComponentActivity() {
    private var tono: Ringtone? = null

    override fun onDestroy() {
        tono?.stop()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val etiqueta = intent.getStringExtra("etiqueta") ?: "Hora de levantarte"
        // Sin permiso de notificaciones no hay sonido del canal: sonamos aquí.
        if (!Notifs.permitidas(this)) {
            tono = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.apply {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }
        setContent {
            BitacoraTheme {
                PantallaAlarma(
                    etiqueta = etiqueta,
                    apagar = {
                        Notifs.apagarAlarma(this)
                        finish()
                    },
                    posponer = {
                        Notifs.apagarAlarma(this)
                        Scheduler.posponer(this, etiqueta)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun PantallaAlarma(etiqueta: String, apagar: () -> Unit, posponer: () -> Unit) {
    val ahora = LocalTime.now()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${ahora.hour}:${ahora.minute.toString().padStart(2, '0')}",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            etiqueta,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Abre la cortina o prende la luz y toma agua.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = apagar, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text("Apagar", fontSize = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = posponer, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Posponer 10 minutos")
        }
    }
}
