package no.jamph.ragumami

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.http.*
import org.slf4j.event.Level
import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.ragumami.umami.domain.UmamiRAGService

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("API_PORT")?.toIntOrNull() ?: 8004,
        host = System.getenv("API_HOST") ?: "0.0.0.0"
    ) {
        configureLogging()
        configureSerialization()
        configureCORS()
        configureRouting()
    }.start(wait = true)
}

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost("localhost:3000")
        allowHost("localhost:5173")
        allowCredentials = true
    }
}

fun Application.configureRouting() {
    val ollamaBaseUrl = environment.config.propertyOrNull("ollama.baseUrl")?.getString()
        ?: System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434"
    val ollamaModel = environment.config.propertyOrNull("ollama.model")?.getString()
        ?: System.getenv("OLLAMA_MODEL") ?: "llama3.2:3b"
    
    val ollamaClient = OllamaClient(ollamaBaseUrl, ollamaModel)
    val ragService = UmamiRAGService(ollamaClient)
    
    routing {
        get("/") {
            call.respondText("🍜 Jamph-Rag-Api-Umami API is running!")
        }
        
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "healthy",
                    "service" to "rag-umami",
                    "flavor" to "umami"
                )
            )
        }
        
        post("/api/chat") {
            try {
                val request = call.receive<ChatRequest>()
                val response = ragService.chat(request.message)
                call.respond(ChatResponse(response))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Unknown error")
                )
            }
        }
        
        post("/api/sql") {
            try {
                val request = call.receive<SQLRequest>()
                val sql = ragService.generateSQL(request.query)
                call.respond(SQLResponse(sql))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(e.message ?: "Unknown error")
                )
            }
        }
    }
}

data class ChatRequest(val message: String)
data class ChatResponse(val response: String)
data class SQLRequest(val query: String)
data class SQLResponse(val sql: String)
data class ErrorResponse(val error: String)