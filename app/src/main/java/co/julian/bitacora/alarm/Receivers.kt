package co.julian.bitacora.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.julian.bitacora.data.Store

class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        when (intent.action) {
            Scheduler.ACCION_AVISO -> {
                val titulo = intent.getStringExtra("titulo")
                if (titulo != null) Notifs.aviso(ctx, titulo, intent.getStringExtra("texto") ?: "")
                Scheduler.reprogramar(ctx)
            }
            Scheduler.ACCION_ALARMA -> {
                Notifs.alarma(ctx, intent.getStringExtra("etiqueta") ?: "Hora de levantarte")
                Scheduler.reprogramar(ctx)
            }
            Scheduler.ACCION_APAGAR -> Notifs.apagarAlarma(ctx)
            Scheduler.ACCION_POSPONER -> {
                Notifs.apagarAlarma(ctx)
                Scheduler.posponer(ctx, intent.getStringExtra("etiqueta") ?: "Alarma pospuesta")
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        Notifs.crearCanales(ctx)
        Scheduler.reprogramar(ctx)
    }
}
