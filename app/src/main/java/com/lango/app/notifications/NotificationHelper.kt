package com.lango.app.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lango.app.MainActivity

object NotificationHelper {

    const val CHANNEL_REMINDER   = "lango_reminder"
    const val CHANNEL_TRAINING   = "lango_training"
    const val CHANNEL_SYNC       = "lango_sync"

    const val NOTIF_REMINDER  = 1001
    const val NOTIF_TRAINING  = 1002
    const val NOTIF_SYNC      = 1003

    fun createChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_REMINDER,
            "Напоминание об учёбе",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ежедневное напоминание повторить слова"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_TRAINING,
            "Результаты тренировки",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Показывает итоги завершённой тренировки"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_SYNC,
            "Синхронизация",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Уведомления о синхронизации колод с облаком"
            manager.createNotificationChannel(this)
        }
    }

    @SuppressLint("MissingPermission")
    fun showDailyReminder(context: Context, dueCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (dueCount > 0)
            "У вас $dueCount слов ждут повторения. Не забудьте потренироваться!"
        else
            "Время учить новые слова! Открывайте Lango."

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🧠 Время учиться!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (hasNotificationPermission(context)) {
            NotificationManagerCompat.from(context).notify(NOTIF_REMINDER, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun showTrainingResult(context: Context, correct: Int, total: Int, deckTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = if (total > 0) (correct * 100 / total) else 0
        val emoji = when {
            percent >= 80 -> "🎉"
            percent >= 50 -> "👍"
            else -> "💪"
        }
        val text = "Правильно: $correct из $total ($percent%). Колода: $deckTitle"

        val notification = NotificationCompat.Builder(context, CHANNEL_TRAINING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$emoji Тренировка завершена!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (hasNotificationPermission(context)) {
            NotificationManagerCompat.from(context).notify(NOTIF_TRAINING, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun showSyncComplete(context: Context, deckCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Синхронизировано колод: $deckCount. Все данные актуальны."

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("☁️ Колоды синхронизированы")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (hasNotificationPermission(context)) {
            NotificationManagerCompat.from(context).notify(NOTIF_SYNC, notification)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
