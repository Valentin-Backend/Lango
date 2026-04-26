package com.lango.app

import android.app.Application
import com.lango.app.data.local.LangoDatabase
import com.lango.app.data.repository.LangoRepository
import com.lango.app.notifications.DailyReminderWorker
import com.lango.app.notifications.NotificationHelper

class LangoApplication : Application() {
    val database by lazy { LangoDatabase.getInstance(this) }
    val repository by lazy { LangoRepository(database.deckDao(), database.wordDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        DailyReminderWorker.scheduleDailyReminder(this)
    }
}
