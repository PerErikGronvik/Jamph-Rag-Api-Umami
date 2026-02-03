package no.jamph.ragumami

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    
    @Test
    fun `test health endpoint returns healthy status`() = testApplication {
        application {
            configureRouting()
            configureSerialization()
        }
        
        val response = client.get("/health")
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("healthy"))
    }
    
    @Test
    fun `test root endpoint returns welcome message`() = testApplication {
        application {
            configureRouting()
        }
        
        val response = client.get("/")
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Jamph-Rag-Api-Umami"))
    }
}