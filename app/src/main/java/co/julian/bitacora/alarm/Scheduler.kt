package co.julian.bitacora.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import co.julian.bitacora.MainActivity
import co.julian.bitacora.data.Alarma
import co.julian.bitacora.data.Estado
import co.julian.bitacora.data.Logica
import co.julian.bitacora.data.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Programa las alarmas de despertar y el próximo recordatorio de la agenda. */
object Scheduler {
    const val ACCION_AVISO = "co.julian.bitacora.AVISO"
    const val ACCION_ALARMA = "co.julian.bitacora.ALARMA"
    const val ACCION_APAGAR = "co.julian.bitacora.APAGAR"
    const val ACCION_POSPONER = "co.julian.bitacora.POSPONER"
    private const val ID_POSPUESTA = 999
    private const val ID_PRUEBA = 998

    data class Evento(val cuando: LocalDateTime, val titulo: String, val texto: String)

    private fun ms(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    fun exactasPermitidas(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || (am(ctx)?.canScheduleExactAlarms() ?: false)

    private fun piAlarma(ctx: Context, id: Int, etiqueta: String) = PendingIntent.getBroadcast(
        ctx, 1000 + id,
        Intent(ctx, AvisoReceiver::class.java).setAction(ACCION_ALARMA).putExtra("etiqueta", etiqueta),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun piAviso(ctx: Context, ev: Evento?) = PendingIntent.getBroadcast(
        ctx, 1,
        Intent(ctx, AvisoReceiver::class.java).setAction(ACCION_AVISO).apply {
            if (ev != null) {
                putExtra("titulo", ev.titulo)
                putExtra("texto", ev.texto)
            }
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun programarAlarma(ctx: Context, id: Int, etiqueta: String, t: LocalDateTime) {
        val manager = am(ctx) ?: return
        val pi = piAlarma(ctx, id, etiqueta)
        val mostrar = PendingIntent.getActivity(
            ctx, 2000 + id, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            if (exactasPermitidas(ctx)) {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(ms(t), mostrar), pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms(t), pi)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms(t), pi)
        }
    }

    private fun programarExacta(ctx: Context, t: LocalDateTime, pi: PendingIntent) {
        val manager = am(ctx) ?: return
        try {
            if (exactasPermitidas(ctx)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms(t), pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms(t), pi)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms(t), pi)
        }
    }

    fun reprogramar(ctx: Context) {
        Store.init(ctx)
        val manager = am(ctx) ?: return
        val e = Store.estado.value
        val ahora = LocalDateTime.now()
        val prefs = ctx.getSharedPreferences("programador", Context.MODE_PRIVATE)

        prefs.getStringSet("alarmas", emptySet())?.forEach { id ->
            id.toIntOrNull()?.let { manager.cancel(piAlarma(ctx, it, "")) }
        }
        val programadas = mutableSetOf<String>()
        for (a in e.alarmas) {
            if (!a.activa) continue
            val t = proximaAlarma(a, ahora) ?: continue
            programarAlarma(ctx, a.id, a.etiqueta, t)
            programadas += a.id.toString()
        }
        prefs.edit().putStringSet("alarmas", programadas).apply()

        manager.cancel(piAviso(ctx, null))
        if (e.ajustes.avisosActivos) {
            proximoAviso(e, ahora)?.let { programarExacta(ctx, it.cuando, piAviso(ctx, it)) }
        }
    }

    fun posponer(ctx: Context, etiqueta: String) =
        programarAlarma(ctx, ID_POSPUESTA, etiqueta, LocalDateTime.now().plusMinutes(10))

    fun alarmaDePrueba(ctx: Context) =
        programarAlarma(ctx, ID_PRUEBA, "Alarma de prueba", LocalDateTime.now().plusMinutes(1).withSecond(0).withNano(0))

    fun proximaAlarma(a: Alarma, ahora: LocalDateTime): LocalDateTime? {
        for (i in 0..8) {
            val d: LocalDate = ahora.toLocalDate().plusDays(i.toLong())
            if (a.fecha != null) {
                if (a.fecha != d.toString()) continue
            } else {
                if (Logica.dow(d) !in a.dias) continue
                if (a.fase != "todas" && a.fase != Logica.fase(d)) continue
            }
            val t = d.atTime(a.hora, a.minuto)
            if (t.isAfter(ahora)) return t
        }
        return null
    }

    fun proximoAviso(e: Estado, ahora: LocalDateTime): Evento? {
        val lista = mutableListOf<Evento>()
        val antes = e.ajustes.minutosAntes.toLong()
        for (i in 0..2) {
            val d = ahora.toLocalDate().plusDays(i.toLong())
            val p = Logica.plantilla(e, d)
            for (b in p.bloques) {
                if (!Logica.avisa(b)) continue
                val h = Logica.parseHora(b.inicio) ?: continue
                val t = d.atTime(h).minusMinutes(antes)
                if (!t.isAfter(ahora)) continue
                val rango = if (b.fin.isNotBlank()) "${b.inicio}–${b.fin}" else b.inicio
                val extra = if (b.tipo == "study") {
                    Logica.leccion(d)?.let { " · Hoy: ${it.titulo}" } ?: " · Tema: ${Logica.temaSemana(d)}"
                } else ""
                val texto = (if (b.detalle.isNotBlank()) "${b.detalle}. " else "") + "De $rango$extra"
                lista += Evento(t, "En ${e.ajustes.minutosAntes} min: ${b.titulo}", texto)
            }
            Logica.parseHora(e.ajustes.horaDormir)?.let {
                val t = d.atTime(it).minusMinutes(30)
                if (t.isAfter(ahora)) {
                    lista += Evento(
                        t, "Prepárate para dormir",
                        "Deja el celular lejos de la cama. La meta es estar dormido a las ${e.ajustes.horaDormir}.",
                    )
                }
            }
        }
        return lista.minByOrNull { it.cuando }
    }
}
