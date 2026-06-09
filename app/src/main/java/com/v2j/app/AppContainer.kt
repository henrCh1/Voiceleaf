package com.v2j.app

import android.content.Context
import androidx.room.Room
import com.v2j.app.data.AppDatabase
import com.v2j.app.data.SettingsRepository
import com.v2j.app.net.DeepSeekClient
import com.v2j.app.net.WebDavClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Tiny manual dependency container — no DI framework needed for an app this small. */
class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "v2j.db"
    ).build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val settings = SettingsRepository(context.applicationContext)

    val repository = JournalRepository(
        entryDao = database.entryDao(),
        draftDao = database.draftDao(),
        deepSeek = DeepSeekClient(okHttp),
        webDav = WebDavClient(okHttp),
        settings = settings
    )
}
