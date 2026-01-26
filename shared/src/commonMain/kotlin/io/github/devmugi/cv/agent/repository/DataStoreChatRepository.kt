package io.github.devmugi.cv.agent.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreChatRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ChatRepository {

    companion object {
        private val KEY_MESSAGES = stringPreferencesKey("chat_messages")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_TURN_NUMBER = intPreferencesKey("turn_number")
    }

    override suspend fun getMessages(): List<Message> {
        val prefs = dataStore.data.first()
        val messagesJson = prefs[KEY_MESSAGES] ?: return emptyList()
        return try {
            json.decodeFromString<List<Message>>(messagesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveMessages(messages: List<Message>) {
        dataStore.edit { prefs ->
            prefs[KEY_MESSAGES] = json.encodeToString(messages)
        }
    }

    override suspend fun clearMessages() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGES)
        }
    }

    override suspend fun getSessionId(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_SESSION_ID]
    }

    override suspend fun saveSessionId(sessionId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
        }
    }

    override suspend fun getTurnNumber(): Int {
        val prefs = dataStore.data.first()
        return prefs[KEY_TURN_NUMBER] ?: 0
    }

    override suspend fun saveTurnNumber(turnNumber: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_TURN_NUMBER] = turnNumber
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGES)
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_TURN_NUMBER)
        }
    }
}
