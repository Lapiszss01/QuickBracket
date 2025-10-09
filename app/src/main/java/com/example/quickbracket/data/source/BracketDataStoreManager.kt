package com.example.quickbracket.data.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.quickbracket.model.Bracket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

private const val BRACKET_PREFERENCES_NAME = "bracket_list_preferences"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = BRACKET_PREFERENCES_NAME)

class BracketDataStoreManager(private val context: Context) {

    private val BRACKETS_LIST_KEY = stringPreferencesKey("all_brackets_json_list")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    val getBracketList: Flow<List<Bracket>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[BRACKETS_LIST_KEY]

            if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString(ListSerializer(Bracket.serializer()), jsonString)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

    suspend fun saveBracketList(list: List<Bracket>) {
        val jsonString = json.encodeToString(ListSerializer(Bracket.serializer()), list)
        context.dataStore.edit { preferences ->
            preferences[BRACKETS_LIST_KEY] = jsonString
        }
    }

}