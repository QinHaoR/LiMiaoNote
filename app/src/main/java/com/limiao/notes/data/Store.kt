package com.limiao.notes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "limiao_data")

/** 数据仓库：DataStore 存整个 AppData JSON（与 Web 版 localStorage 同一格式） */
class AppRepository(private val context: Context) {
    private val KEY_DATA = stringPreferencesKey("app_data")

    /** 数据流：任何变化自动通知 UI */
    val data: Flow<AppData> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DATA]
        if (raw.isNullOrBlank()) AppData.empty() else parseAppData(raw)
    }

    suspend fun save(data: AppData) {
        context.dataStore.edit { it[KEY_DATA] = data.toJson() }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_DATA) }
    }
}
