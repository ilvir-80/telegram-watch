package com.ilvir.telegramwatch

import android.app.Application

class TelegramWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация TDLib-клиента при старте процесса.
        TdlibClient.init(this)
    }
}
