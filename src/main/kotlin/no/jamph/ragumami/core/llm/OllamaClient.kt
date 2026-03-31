package no.jamph.ragumami.core.llm

import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.gson.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val resolveLogger = LoggerFactory.getLogger("OllamaModelResolver")

suspend fun resolveActiveOllamaModel(baseUrl: String): String? {
    val client = HttpClient(CIO)
    return try {
        // First: check currently loaded (running) model
        val psResponse = client.get("$baseUrl/api/ps")
        val psJson = JsonParser.parseString(psResponse.bodyAsText()).asJsonObject
        val running = psJson.getAsJsonArray("models")
        if (running != null && running.size() > 0) {
            val model = running[0].asJsonObject.get("name").asString
            resolveLogger.info("OLLAMA_MODEL_RESOLVED: Using running model '{}'" , model)
            return model
        }

        // Fallback: first locally available model
        val tagsResponse = client.get("$baseUrl/api/tags")
        val tagsJson = JsonParser.parseString(tagsResponse.bodyAsText()).asJsonObject
        val models = tagsJson.getAsJsonArray("models")
        if (models != null && models.size() > 0) {
            val model = models[0].asJsonObject.get("name").asString
            resolveLogger.info("OLLAMA_MODEL_RESOLVED: No running model, using first available '{}'" , model)
            model
        } else {
            resolveLogger.warn("OLLAMA_MODEL_RESOLVED: No models found locally")
            null
        }
    } catch (e: Exception) {
        resolveLogger.warn("OLLAMA_MODEL_RESOLVED: Could not resolve active model: {}", e.message)
        null
    } finally {
        client.close()
    }
}

class OllamaClient(
    private val baseUrl: String,
    private val model: String
) {
    private val logger = LoggerFactory.getLogger(OllamaClient::class.java)
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 120_000
        }
        
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
            
            retryIf { request, response ->
                val shouldRetry = response.status.value in 500..599
                if (shouldRetry) {
                    logger.warn(
                        "OLLAMA_RETRY: Retrying request to {} due to status {}. Attempt: {}",
                        request.url.encodedPath,
                        response.status.value,
                        retryCount
                    )
                }
                shouldRetry
            }
        }
    }

    suspend fun generate(prompt: String): String {
        return try {
            val startTime = System.currentTimeMillis()
            
            val response = client.post("$baseUrl/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "model" to model,
                    "prompt" to prompt,
                    "stream" to false
                ))
            }
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("OLLAMA_SUCCESS: Generate completed in {}ms", duration)
            
            val body = response.bodyAsText()
            val json = JsonParser.parseString(body).asJsonObject
            json.get("response")?.asString ?: body
            
        } catch (e: HttpRequestTimeoutException) {
            logger.error("OLLAMA_TIMEOUT: Request timed out after 120s for model: {}", model, e)
            return "Integrasjon til api rag virker, men ollama virker ikke (timeout)"
        } catch (e: Exception) {
            logger.error("OLLAMA_ERROR: Failed to generate response", e)
            return "Integrasjon til api rag virker, men ollama virker ikke"
        }
    }

    suspend fun generateRaw(prompt: String): String {
        return try {
            val response = client.post("$baseUrl/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "model" to model,
                    "prompt" to prompt,
                    "stream" to false
                ))
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("OLLAMA_ERROR: generateRaw failed", e)
            "{}"
        }
    }

    suspend fun embed(text: String): List<Float> {
        return try {
            val startTime = System.currentTimeMillis()
            
            val response = client.post("$baseUrl/api/embeddings") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "model" to model,
                    "prompt" to text
                ))
            }
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("OLLAMA_EMBED_SUCCESS: Embedding completed in {}ms", duration)
            
            emptyList() // TODO: Parse actual response
            
        } catch (e: HttpRequestTimeoutException) {
            logger.error("OLLAMA_EMBED_TIMEOUT: Embedding timed out after 30s", e)
            throw e
        } catch (e: Exception) {
            logger.error("OLLAMA_EMBED_ERROR: Failed to create embedding", e)
            throw e
        }
    }
}