package co.julian.bitacora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.julian.bitacora.alarm.Notifs
import co.julian.bitacora.alarm.Scheduler
import co.julian.bitacora.data.Store
import co.julian.bitacora.data.Sync
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import co.julian.bitacora.ui.AjustesScreen
import co.julian.bitacora.ui.BitacoraTheme
import co.julian.bitacora.ui.ExamenScreen
import co.julian.bitacora.ui.HorarioScreen
import co.julian.bitacora.ui.HoyScreen
import co.julian.bitacora.ui.ProgresoScreen
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        Notifs.crearCanales(this)
        Scheduler.reprogramar(this)
        enableEdgeToEdge()
        setContent { BitacoraTheme { AppRaiz() } }
    }

    override fun onResume() {
        super.onResume()
        Scheduler.reprogramar(this)
        // Sube el registro y baja el plan de Claude (como mucho cada 2 horas).
        lifecycleScope.launch { Sync.sincronizar(this@MainActivity) }
    }
}

@Composable
private fun AppRaiz() {
    val ctx = LocalContext.current
    val estado by Store.estado.collectAsState()
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var fechaTexto by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val fecha = LocalDate.parse(fechaTexto)
    val setFecha: (LocalDate) -> Unit = { fechaTexto = it.toString() }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Scheduler.reprogramar(ctx)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pestanas = listOf(
        "Hoy" to Icons.Filled.Home,
        "Horario" to Icons.Filled.DateRange,
        "Examen" to Icons.Filled.Star,
        "Progreso" to Icons.Filled.CheckCircle,
        "Ajustes" to Icons.Filled.Settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                pestanas.forEachIndexed { i, (nombre, icono) ->
                    NavigationBarItem(
                        selected = pestana == i,
                        onClick = { pestana = i },
                        icon = { Icon(icono, contentDescription = null) },
                        label = { Text(nombre) },
                    )
                }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno)) {
            when (pestana) {
                0 -> HoyScreen(estado, fecha, setFecha, irAHorario = { pestana = 1 }, irAExamen = { pestana = 2 })
                1 -> HorarioScreen(estado, fecha, setFecha)
                2 -> ExamenScreen(estado, irAAjustes = { pestana = 4 })
                3 -> ProgresoScreen(estado, abrirDia = { setFecha(it); pestana = 0 })
                else -> AjustesScreen(estado)
            }
        }
    }
}
