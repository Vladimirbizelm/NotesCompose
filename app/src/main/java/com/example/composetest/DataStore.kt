package com.example.composetest

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object DataStore {
    private val NOTES_KEY = stringPreferencesKey("notes")

    fun getNotes(context: Context): Flow<Map<String, String>> {
        return context.datastore.data.map { preferences ->
            preferences[NOTES_KEY]?.let { serializedNotes ->
                serializedNotes.split(";").associate {
                    val (key, value) = it.split(":")
                    key to value
                }
            } ?: emptyMap()
        }
    }

    suspend fun saveNotes(context: Context, notes: Map<String, String>) {
        val serializedNotes = notes.entries.joinToString(";") { "${it.key}:${it.value}" }
        context.datastore.edit { preferences ->
            preferences[NOTES_KEY] = serializedNotes
        }
    }
}