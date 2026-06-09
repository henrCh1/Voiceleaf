package com.v2j.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App settings + secrets. DataStore is the modern replacement for SharedPreferences;
 *  for a personal single-user app this is sufficient (no EncryptedSharedPreferences needed). */
class SettingsRepository(private val context: Context) {

    data class Settings(
        val deepseekKey: String = "",
        val nutstoreEmail: String = "",
        val nutstoreAppPassword: String = "",
        val webdavBasePath: String = "",
        val allowRawPublish: Boolean = false,
        // 高级设置：自定义 OpenAI 兼容接口。useCustomProvider 决定优先级——
        // 关＝用 DeepSeek 默认（即使下面三项也填了）；开＝用自定义。
        val useCustomProvider: Boolean = false,
        val customBaseUrl: String = "",
        val customApiKey: String = "",
        val customModel: String = ""
    ) {
        /** Whether the *active* polish provider has everything it needs. */
        val polishConfigured: Boolean
            get() = if (useCustomProvider) {
                customBaseUrl.isNotBlank() && customApiKey.isNotBlank() && customModel.isNotBlank()
            } else {
                deepseekKey.isNotBlank()
            }

        val isComplete: Boolean
            get() = polishConfigured &&
                nutstoreEmail.isNotBlank() &&
                nutstoreAppPassword.isNotBlank() &&
                webdavBasePath.isNotBlank()
    }

    private object Keys {
        val KEY = stringPreferencesKey("deepseek_api_key")
        val EMAIL = stringPreferencesKey("nutstore_email")
        val APP_PW = stringPreferencesKey("nutstore_app_password")
        val PATH = stringPreferencesKey("webdav_base_path")
        val ALLOW_RAW = booleanPreferencesKey("allow_raw_publish")
        val USE_CUSTOM = booleanPreferencesKey("use_custom_provider")
        val CUSTOM_URL = stringPreferencesKey("custom_base_url")
        val CUSTOM_KEY = stringPreferencesKey("custom_api_key")
        val CUSTOM_MODEL = stringPreferencesKey("custom_model")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            deepseekKey = p[Keys.KEY].orEmpty(),
            nutstoreEmail = p[Keys.EMAIL].orEmpty(),
            nutstoreAppPassword = p[Keys.APP_PW].orEmpty(),
            webdavBasePath = p[Keys.PATH].orEmpty(),
            allowRawPublish = p[Keys.ALLOW_RAW] ?: false,
            useCustomProvider = p[Keys.USE_CUSTOM] ?: false,
            customBaseUrl = p[Keys.CUSTOM_URL].orEmpty(),
            customApiKey = p[Keys.CUSTOM_KEY].orEmpty(),
            customModel = p[Keys.CUSTOM_MODEL].orEmpty()
        )
    }

    suspend fun get(): Settings = flow.first()

    suspend fun save(settings: Settings) {
        context.dataStore.edit { p ->
            p[Keys.KEY] = settings.deepseekKey.trim()
            p[Keys.EMAIL] = settings.nutstoreEmail.trim()
            p[Keys.APP_PW] = settings.nutstoreAppPassword.trim()
            p[Keys.PATH] = settings.webdavBasePath.trim().trim('/')
            p[Keys.ALLOW_RAW] = settings.allowRawPublish
            p[Keys.USE_CUSTOM] = settings.useCustomProvider
            p[Keys.CUSTOM_URL] = settings.customBaseUrl.trim()
            p[Keys.CUSTOM_KEY] = settings.customApiKey.trim()
            p[Keys.CUSTOM_MODEL] = settings.customModel.trim()
        }
    }
}
