package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun chatMessageSerializesToJson() {
        val message = ChatMessage(role = "user", content = "Hello")
        val serialized = json.encodeToString(ChatMessage.serializer(), message)
        assertEquals("""{"role":"user","content":"Hello"}""", serialized)
    }

    @Test
    fun chatRequestSerializesWithDefaults() {
        val request = ChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(ChatMessage("user", "Hi"))
        )
        val serialized = json.encodeToString(ChatRequest.serializer(), request)
        assertTrue(serialized.contains(""""stream":true"""))
        assertTrue(serialized.contains(""""temperature":0.7"""))
        assertTrue(serialized.contains(""""max_tokens":1024"""))
    }

    @Test
    fun streamChunkParsesContentDelta() {
        val chunkJson = """{"choices":[{"delta":{"content":"Hello"},"index":0}]}"""
        val chunk = json.decodeFromString(StreamChunk.serializer(), chunkJson)
        assertEquals("Hello", chunk.choices.first().delta.content)
    }

    @Test
    fun streamChunkHandlesEmptyDelta() {
        val chunkJson = """{"choices":[{"delta":{},"index":0}]}"""
        val chunk = json.decodeFromString(StreamChunk.serializer(), chunkJson)
        assertEquals(null, chunk.choices.first().delta.content)
    }
}
