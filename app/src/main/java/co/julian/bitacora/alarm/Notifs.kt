package co.julian.bitacora.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.julian.bitacora.MainActivity
import co.julian.bitacora.R

object Notifs {
    const val CANAL_AVISOS = "avisos"
    const val CANAL_ALARMAS = "alarmas_v1"
    const val ID_ALARMA = 7001

    fun crearCanales(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val avisos = NotificationChannel(
            CANAL_AVISOS, "Recordatorios de la agenda", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos unos minutos antes de cada bloque de estudio, inglés o ejercicio"
            enableVibration(true)
        }
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmas = NotificationChannel(
            CANAL_ALARMAS, "Alarmas para despertar", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarmas con sonido que siguen hasta que las apagas"
            setSound(sonido, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600, 800, 600, 800)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(avisos, alarmas))
    }

    fun permitidas(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun pantallaCompletaPermitida(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return ctx.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: false
    }

    private fun abrirApp(ctx: Context) = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @SuppressLint("MissingPermission")
    fun aviso(ctx: Context, titulo: String, texto: String) {
        if (!permitidas(ctx)) return
        crearCanales(ctx)
        val n = NotificationCompat.Builder(ctx, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val id = 100 + (System.currentTimeMillis() / 1000 % 5000).toInt()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    @SuppressLint("MissingPermission")
    fun alarma(ctx: Context, etiqueta: String) {
        crearCanales(ctx)
        val pantalla = PendingIntent.getActivity(
            ctx, 77,
            Intent(ctx, AlarmActivity::class.java)
                .putExtra("etiqueta", etiqueta)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val apagar = PendingIntent.getBroadcast(
            ctx, 78,
            Intent(ctx, AvisoReceiver::class.java).setAction(Scheduler.ACCION_APAGAR),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val posponer = PendingIntent.getBroadcast(
            ctx, 79,
            Intent(ctx, AvisoReceiver::class.java).setAction(Scheduler.ACCION_POSPONER).putExtra("etiqueta", etiqueta),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CANAL_ALARMAS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Alarma")
            .setContentText(etiqueta)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pantalla, true)
            .setContentIntent(pantalla)
            .setOngoing(true)
            .addAction(0, "Apagar", apagar)
            .addAction(0, "Posponer 10 min", posponer)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        if (permitidas(ctx)) NotificationManagerCompat.from(ctx).notify(ID_ALARMA, n)
        // Si la pantalla está encendida, abrimos directamente la vista de la alarma.
        try {
            ctx.startActivity(
                Intent(ctx, AlarmActivity::class.java)
                    .putExtra("etiqueta", etiqueta)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
        }
    }

    fun apagarAlarma(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(ID_ALARMA)
    }
}
