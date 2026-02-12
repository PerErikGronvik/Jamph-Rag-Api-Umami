# Jamph-Rag-Api-Umami Maven Setup

**Team JAMPH** - Kotlin API layer with RAG + Ollama



**Summary:**
1. Maven create → quickstart archetype
2. Replace pom.xml → Kotlin 2.1.0, Ktor 3.0.2, LangChain4j 0.36.2, MLflow, WireMock
3. Convert src/main/java → kotlin, create package structure
4. Application.kt → Ktor server, CORS, logging, health endpoint
5. Config → logback.xml, application.conf (no database)
6. Core RAG → interfaces, OllamaClient (timeout/retry for NAIS), in-memory RAG
7. Tests → WireMock scenarios (timeout, 500, retry)
8. Build → mvn clean install, run, verify curl
9. Docker → .env, compose up Ollama service
10. Create MVP API endpoints → POST /api/chat and POST /api/sql to accept queries from Umami frontend and pass through to Ollama LLM


```
Umami (Vite) → Jamph-Rag-Api-Umami API (Kotlin/Ktor) → Ollama (LLM)
                            ↓
                  In-Memory RAG (LangChain4j)
```
## Anbefalte andre verktøy for kanskje senere

**For later implementation (not currently used):**

Kotlin Logging: Bedre syntax enn Java SLF4J
```xml
<kotlin.logging.version>3.0.5</kotlin.logging.version>
```

Log4j Bridge: Forhindrer Log4j-konflikter
```xml
<log4j.over.slf4j.version>2.0.17</log4j.over.slf4j.version>
```

Micrometer: Metrics for Prometheus/Grafana - NAIS standard
```xml
<micrometer.prometheus.version>1.16.2</micrometer.prometheus.version>
```

Kotest: Bedre test DSL enn JUnit for Kotlin
```xml
<kotest.version>6.1.2</kotest.version>
```

**Database (Future - not in current MVP):**
PostgreSQL er en database, Exposed er et ORM rammeverk for Kotlin (ORM = Object Relational Mapping)
```xml
<postgresql.version>42.7.9</postgresql.version>
<exposed.version>0.57.0</exposed.version>
```


## Prerequisites

Complete main setup guide first (Development setup.md).

## Architecture

**Current MVP:** In-memory RAG using LangChain4j - no database required.
**Future:** External PostgreSQL server with Exposed ORM (for persistent vector storage and query history).

## Step 1: Create Maven Project

### Option A: Using VS Code Maven Extension (Recommended)

**Prerequisites:** Install "Extension Pack for Java" from VS Code marketplace (includes Maven support)

1. `Ctrl+Shift+P` → Type `Maven: New Project`
2. If command doesn't appear:
   - First run: `Ctrl+Shift+P` → `Maven: Update Maven Archetype Catalog`
   - Wait for completion, then try again
3. Select `maven-archetype-quickstart`
4. Fill in:
   - Group ID: `no.jamph.ragumami`
   - Artifact ID: `api`
   - Destination: `Jamph-Rag-Api-Umami` folder

Define value for property 'version' 1.0-SNAPSHOT: : Press Enter to accept default.

 Y: : Press Enter to accept default.

 Should say: Build was successful!


## Step 2: Configure pom.xml for Kotlin + Ktor

Replace the generated `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>no.jamph.ragumami</groupId>
    <artifactId>api</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Jamph-Rag-Api-Umami API</name>
    <description>Kotlin API layer for RAG-based queries with Ollama</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <kotlin.version>2.1.0</kotlin.version>
        <flyway.version>10.21.0</flyway.version>
        <ktor.version>3.0.2</ktor.version>
        <logback.version>1.5.12</logback.version>
        <langchain4j.version>0.36.2</langchain4j.version>
        <mlflow.version>2.18.0</mlflow.version>
        <kotlin.code.style>official</kotlin.code.style>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>





    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>

        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-core-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-netty-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-content-negotiation-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-serialization-gson-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-cors-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-call-logging-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>

        <!-- Ktor Client (for Ollama communication) -->
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-core-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-cio-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-content-negotiation-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>

        <!-- Database - Not used in current MVP, for future implementation -->
        <!-- Uncomment when adding persistent storage:
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-core</artifactId>
            <version>0.57.0</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-dao</artifactId>
            <version>0.57.0</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-jdbc</artifactId>
            <version>0.57.0</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.9</version>
        </dependency>
        -->

        <!-- Logging -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>

        <!-- LangChain4j - LLM Orchestration -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-ollama</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-embeddings</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-easy-rag</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- MLflow - Production Monitoring & Experiment Tracking -->
        <dependency>
            <groupId>org.mlflow</groupId>
            <artifactId>mlflow-client</artifactId>
            <version>${mlflow.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-test-junit5</artifactId>
            <version>${kotlin.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-server-tests-jvm</artifactId>
            <version>${ktor.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.mockk</groupId>
            <artifactId>mockk</artifactId>
            <version>1.14.9</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock</artifactId>
            <version>3.9.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>src/main/kotlin</sourceDirectory>
        <testSourceDirectory>src/test/kotlin</testSourceDirectory>

        <plugins>
            <!-- Kotlin Maven Plugin -->
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>test-compile</id>
                        <phase>test-compile</phase>
                        <goals>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <!-- Surefire for JUnit 5 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>

            <!-- Assembly Plugin for Fat JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>no.jamph.ragumami.ApplicationKt</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 3: Convert to Kotlin Structure

```powershell
cd Jamph-Rag-Api-Umami\api
# Windows (PowerShell) - from backend root
# Remove Java directories (if they exist)
Remove-Item -Path "src\main\java" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "src\test\java" -Recurse -Force -ErrorAction SilentlyContinue

# Create Kotlin structure
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\core\rag" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\core\llm" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\core\embeddings" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\core\retrieval" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\umami\api" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\umami\domain" -Force
New-Item -ItemType Directory -Path "src\main\kotlin\no\jamph\ragumami\umami\prompts" -Force
New-Item -ItemType Directory -Path "src\main\resources" -Force
New-Item -ItemType Directory -Path "src\test\kotlin\no\jamph\ragumami" -Force
New-Item -ItemType Directory -Path "src\test\kotlin\no\jamph\ragumami\core\llm" -Force
```

```bash
cd Jamph-Rag-Api-Umami\api
# macOS/Linux - from backend root
# Remove Java directories (if they exist)
rm -rf src/main/java src/test/java

# Create Kotlin structure
mkdir -p src/main/kotlin/no/jamph/ragumami/core/{rag,llm,embeddings,retrieval}
mkdir -p src/main/kotlin/no/jamph/ragumami/umami/{api,domain,prompts}
mkdir -p src/main/resources
mkdir -p src/test/kotlin/no/jamph/ragumami
mkdir -p src/test/kotlin/no/jamph/ragumami/core/llm
```

## Step 4: Create Application Entry Point
from Jamph-Rag-Api-Umami\api
create a file at `src/main/kotlin/no/jamph/ragumami/Application.kt`:
copy the code below into the file:

```kotlin
package no.jamph.ragumami

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.http.*
import org.slf4j.event.Level

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
    }
}
```

## Step 5: Add Configuration Files

### Logback Configuration

Create a file `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="${LOG_LEVEL:-INFO}">
        <appender-ref ref="STDOUT"/>
    </root>
    
    <logger name="io.ktor" level="INFO"/>
    <logger name="no.jamph.ragumami" level="DEBUG"/>
    <logger name="dev.langchain4j" level="INFO"/>
</configuration>
```

### Application Configuration

Create `src/main/resources/application.conf`:

```hocon
ktor {
    deployment {
        port = 8004
        port = ${?API_PORT}
    }
}

# No database configuration needed for MVP - using in-memory RAG
# Future: Add PostgreSQL config when implementing persistent storage

ollama {
    baseUrl = "http://localhost:11434"
    baseUrl = ${?OLLAMA_BASE_URL}
    model = "llama3.2:3b"
    model = ${?OLLAMA_MODEL}
}
```


## Step 6: Create Core RAG Structure (Modular Design)

### Core RAG Interface (Extractable)

Create file `src/main/kotlin/no/jamph/ragumami/core/rag/RAGOrchestrator.kt`:

```kotlin
package no.jamph.ragumami.core.rag

interface RAGOrchestrator {
    suspend fun query(question: String, context: QueryContext): RAGResponse
    suspend fun embedText(text: String): Vector
    suspend fun retrieveContext(query: String, topK: Int): List<Document>
}

data class QueryContext(
    val domain: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class RAGResponse(
    val answer: String,
    val sources: List<Document>,
    val confidence: Double
)

data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, Any>
)

data class Vector(
    val values: List<Float>
)
```

### Ollama Client (Extractable)

Create `src/main/kotlin/no/jamph/ragumami/core/llm/OllamaClient.kt`:

```kotlin
package no.jamph.ragumami.core.llm

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
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
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
            
            response.bodyAsText()
            
        } catch (e: HttpRequestTimeoutException) {
            logger.error("OLLAMA_TIMEOUT: Request timed out after 30s for model: {}", model, e)
            throw e
        } catch (e: Exception) {
            logger.error("OLLAMA_ERROR: Failed to generate response", e)
            throw e
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
```

### Umami-Specific Service

Create `src/main/kotlin/no/jamph/ragumami/umami/domain/UmamiRAGService.kt`:

```kotlin
package no.jamph.ragumami.umami.domain

import no.jamph.ragumami.core.rag.RAGOrchestrator
import no.jamph.ragumami.core.rag.QueryContext
import no.jamph.ragumami.core.llm.OllamaClient

class UmamiRAGService(
    private val ollamaClient: OllamaClient
) {
    suspend fun generateSQL(naturalLanguageQuery: String): String {
        // Umami-specific: Get Prisma schema context
        val schemaContext = getUmamiSchemaContext()
        
        val prompt = buildSQLPrompt(naturalLanguageQuery, schemaContext)
        
        return ollamaClient.generate(prompt)
    }
    
    private fun getUmamiSchemaContext(): String {
        // TODO: Read Umami's Prisma schema
        return """
        -- Umami Analytics Database Schema
        -- Tables: website, session, event, pageview, etc.
        """.trimIndent()
    }
    
    private fun buildSQLPrompt(query: String, schema: String): String {
        return """
        You are a SQL expert. Generate PostgreSQL query for:
        
        Schema:
        $schema
        
        User Query: $query
        
        Return only the SQL query, no explanations.
        """.trimIndent()
    }
}
```

## Step 7: Add JAMPH Testing Standards

Create `src/test/kotlin/no/jamph/ragumami/ApplicationTest.kt`:

```kotlin
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
```

Create `src/test/kotlin/no/jamph/ragumami/core/llm/OllamaClientTest.kt`:

```kotlin
package no.jamph.ragumami.core.llm

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OllamaClientTest {
    private lateinit var wireMock: WireMockServer
    private lateinit var client: OllamaClient
    
    @BeforeEach
    fun setup() {
        wireMock = WireMockServer(WireMockConfiguration.options().dynamicPort())
        wireMock.start()
        client = OllamaClient("http://localhost:${wireMock.port()}", "llama3.2:3b")
    }
    
    @AfterEach
    fun teardown() {
        wireMock.stop()
    }
    
    @Test
    fun `test successful generation`() = runBlocking {
        wireMock.stubFor(
            post(urlEqualTo("/api/generate"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("{\"response\":\"SELECT * FROM website;\"}")
                )
        )
        
        val result = client.generate("Show all websites")
        
        assertEquals(true, result.contains("SELECT"))
    }
    
    @Test
    fun `test timeout handling`() = runBlocking {
        wireMock.stubFor(
            post(urlEqualTo("/api/generate"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // Exceeds 30s timeout
                )
        )
        
        assertFailsWith<Exception> {
            client.generate("Test timeout")
        }
    }
    
    @Test
    fun `test retry on 500 then succeeds`() = runBlocking {
        wireMock.stubFor(
            post(urlEqualTo("/api/generate"))
                .inScenario("Retry")
                .whenScenarioStateIs("Started")
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
                .willSetStateTo("First retry")
        )
        
        wireMock.stubFor(
            post(urlEqualTo("/api/generate"))
                .inScenario("Retry")
                .whenScenarioStateIs("First retry")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("{\"response\":\"Success\"}")
                )
        )
        
        val result = client.generate("Test retry")
        assertEquals(true, result.contains("Success"))
    }
    
    @Test
    fun `test retry gives up after max attempts`() = runBlocking {
        wireMock.stubFor(
            post(urlEqualTo("/api/generate"))
                .willReturn(
                    aResponse()
                        .withStatus(503) // Service Unavailable
                )
        )
        
        assertFailsWith<Exception> {
            client.generate("Test retry failure")
        }
        
        // Verify retried 3 times (initial + 3 retries = 4 total)
        wireMock.verify(4, postRequestedFor(urlEqualTo("/api/generate")))
    }
}
```

## Step 8: Build and Run
Press Ctrl+Shift+P
Type "Maven: Update Maven archetype Catalog"
click on lifecycle phases → clean, run

```bash
mvn clean install
```

**Note:** Tests are skipped by default during build. See section at end for re-enabling integration tests.


```

### Run the Application

```bash
# Using Maven exec
mvn exec:java -Dexec.mainClass="no.jamph.ragumami.ApplicationKt"

# Or run the JAR
java -jar target/api-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Verify

```bash
curl http://localhost:8004/health
curl http://localhost:8004/
```

## Step 9: Environment Configuration

Create `.env` file in `backend/` directory:

```properties
# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:3b

# API Configuration
API_PORT=8004
API_HOST=0.0.0.0

# Environment
ENVIRONMENT=development
LOG_LEVEL=DEBUG

# Database - Not used in current MVP
# Uncomment when adding persistent storage:
# DATABASE_URL=jdbc:postgresql://your-postgres-host:5432/ragumami
# POSTGRES_USER=your-db-user
# POSTGRES_PASSWORD=your-db-password
```

**No Database Setup Required for MVP** - Using in-memory RAG with LangChain4j

Start Ollama service (from `backend/` directory):

```bash
# Start Ollama only
docker compose --profile ollama up -d

# Verify services
docker compose ps

# Verify Ollama
curl http://localhost:11434/api/tags

# Pull model if needed
docker exec -it backend-ollama-1 ollama pull llama3.2:3b
```

## Step 10: VS Code Maven

`Ctrl+Shift+P` → "Maven: Focus on Maven Project View" → Right-click lifecycle phases (clean, compile, test, package)

## Project Structure After Setup

```
backend/  (this repo root)
├── pom.xml
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── no/jamph/ragumami/
│   │   │       ├── Application.kt
│   │   │       ├── core/                    ← Extractable RAG engine
│   │   │       │   ├── rag/
│   │   │       │   │   └── RAGOrchestrator.kt
│   │   │       │   ├── llm/
│   │   │       │   │   └── OllamaClient.kt
│   │   │       │   ├── embeddings/
│   │   │       │   └── retrieval/
│   │   │       └── umami/                   ← Umami-specific
│   │   │           ├── api/
│   │   │           ├── domain/
│   │   │           │   └── UmamiRAGService.kt
│   │   │           └── prompts/
│   │   └── resources/
│   │       ├── logback.xml
│   │       └── application.conf
│   └── test/
│       └── kotlin/
│           └── no/jamph/ragumami/
│               ├── ApplicationTest.kt
│               └── core/
│                   └── llm/
│                       └── OllamaClientTest.kt
└── target/ (generated)
```

## Step 10: Implement MVP API Endpoints

**Goal:** Accept API calls from Umami frontend and pass through to Ollama LLM.

Create `src/main/kotlin/no/jamph/ragumami/umami/api/ChatRoutes.kt`:

```kotlin
package no.jamph.ragumami.umami.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.jamph.ragumami.core.llm.OllamaClient

data class ChatRequest(val message: String)
data class ChatResponse(val response: String)
data class SQLRequest(val naturalLanguageQuery: String)
data class SQLResponse(val sql: String)

fun Application.configureChatRoutes() {
    val ollamaClient = OllamaClient(
        baseUrl = environment.config.property("ollama.baseUrl").getString(),
        model = environment.config.property("ollama.model").getString()
    )
    
    routing {
        route("/api") {
            post("/chat") {
                val request = call.receive<ChatRequest>()
                val response = ollamaClient.generate(request.message)
                call.respond(ChatResponse(response))
            }
            
            post("/sql") {
                val request = call.receive<SQLRequest>()
                val prompt = buildSQLPrompt(request.naturalLanguageQuery)
                val sqlQuery = ollamaClient.generate(prompt)
                call.respond(SQLResponse(sqlQuery))
            }
        }
    }
}

private fun buildSQLPrompt(query: String): String {
    return """
    You are a SQL expert for Umami Analytics.
    Generate a PostgreSQL query for: $query
    
    Return only the SQL query, no explanations.
    """.trimIndent()
}
```

Update `Application.kt` to register the routes:

```kotlin
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
        configureChatRoutes()  // Add this line
    }.start(wait = true)
}
```

Test the endpoints:

```bash
# Test chat endpoint
curl -X POST http://localhost:8004/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What is Umami Analytics?"}'  

# Test SQL generation
curl -X POST http://localhost:8004/api/sql \
  -H "Content-Type: application/json" \
  -d '{"naturalLanguageQuery":"Show total pageviews for all websites"}'
```

## Next Steps (Future Enhancements)

1. **Database Integration:** Add Exposed ORM + PostgreSQL for persistent vector storage
2. **Advanced RAG:** Implement document embeddings and semantic search
3. **Testing:** Add integration tests for chat/SQL endpoints
4. **Documentation:** OpenAPI/Swagger specification
5. **Monitoring:** Add Micrometer metrics for NAIS/Grafana
6. **Authentication:** Add API key validation for production

## Troubleshooting

### Maven Build Fails

```bash
# Clear cache and rebuild
mvn clean install -U

# Check Java version
java -version  # Must be 21+ (LTS)

# Verify JAVA_HOME
echo $env:JAVA_HOME  # Windows PowerShell
echo $JAVA_HOME      # macOS/Linux
```

### Kotlin Compilation Errors

```bash
# Ensure kotlin-maven-plugin version matches kotlin.version
# Check pom.xml properties section

# Force dependency updates
mvn clean install -U
```

### Port Already in Use

```powershell
# Windows: Find process on port 8004
netstat -ano | findstr :8004
taskkill /PID <PID> /F

# Or change port in .env
API_PORT=8081
```

```bash
# macOS/Linux: Kill process on port 8004
lsof -ti:8004 | xargs kill -9

# Or change port in .env
API_PORT=8081
```

### Ollama Connection Fails

```bash
# Verify Ollama container is running
docker compose ps | grep ollama

# Check Ollama logs
docker compose logs ollama

# Test Ollama directly
curl http://localhost:11434/api/tags

# Pull model if missing
docker exec -it backend-ollama-1 ollama pull llama3.2:3b
```

### Database Connection Issues

```bash
# Test external PostgreSQL connection
psql -h your-postgres-host -U your-db-user -d ragumami

# Verify DATABASE_URL format
echo $env:DATABASE_URL  # Windows
echo $DATABASE_URL      # macOS/Linux

# Check connection from application
# Review logs for "Connection refused" or "Authentication failed" errors

# If database doesn't exist, create it on your PostgreSQL server:
psql -h your-postgres-host -U your-db-user -c "CREATE DATABASE ragumami;"
```

## NAIS Monitoring

For overvåkning i NAIS, se etter disse loggmeldingene:

### Retry Events
```
OLLAMA_RETRY: Retrying request to /api/generate due to status 503. Attempt: 2
```
- Viser når Ollama er midlertidig utilgjengelig
- Indikerer potensielle kapasitetsproblemer
- Eksponentiell delay mellom forsøk: 1s, 2s, 4s

### Timeout Events
```
OLLAMA_TIMEOUT: Request timed out after 30s for model: llama3.2:3b
```
- Viser når LLM-generering tar over 30 sekunder
- Kan indikere underdimensjonert Ollama-instans
- Vurder større modell-timeout eller raskere modell

### Success Metrics
```
OLLAMA_SUCCESS: Generate completed in 2341ms
OLLAMA_EMBED_SUCCESS: Embedding completed in 145ms
```
- Normalt responstid: 1-5 sekunder for generate
- Normalt responstid: 100-500ms for embeddings

### Grafana Queries
```promql
# Retry rate
rate(log{app="rag-umami",message=~"OLLAMA_RETRY.*"}[5m])

# Timeout rate  
rate(log{app="rag-umami",message=~"OLLAMA_TIMEOUT.*"}[5m])

# Average response time
avg(log{app="rag-umami",message=~"OLLAMA_SUCCESS.*"} | regexp "(?P<duration>\\d+)ms" | unwrap duration)
```

## Resources

- **Main Setup Guide:** [README.md](../README.md)
- **Installation Guide:** [README_install.MD](README_install.MD)
- **Ktor Documentation:** https://ktor.io/docs
- **Kotlin Coding Conventions:** https://kotlinlang.org/docs/coding-conventions.html
- **Exposed ORM:** https://github.com/JetBrains/Exposed
- **Ollama API:** https://github.com/ollama/ollama/blob/main/docs/api.md
- **JAMPH Internal Docs:** (team documentation)



## Step 11: Re-enabling Integration Tests

**Tests are disabled by default** to allow fast builds during development and rigging. To re-enable tests:

### Option 1: One-time test run
```bash
# Run tests once without changing configuration
mvn test -DskipTests=false

# Or full build with tests
mvn clean install -DskipTests=false
```

### Option 2: Enable tests permanently

Edit `api/pom.xml` and change:
```xml
<properties>
    ...
    <skipTests>true</skipTests>  <!-- Change to false -->
</properties>
```

To:
```xml
<properties>
    ...
    <skipTests>false</skipTests>
</properties>
```

Then build normally:
```bash
mvn clean install
```

### Running specific tests
```bash
# Run all tests
mvn test -DskipTests=false

# Run specific test class
mvn test -DskipTests=false -Dtest=ApplicationTest

# Run specific test method
mvn test -DskipTests=false -Dtest=OllamaClientTest#"test successful generation"
```

### Why tests are disabled by default
- Faster Docker builds during development
- Prevents build failures when Ollama is not available
- Allows quick iteration during initial setup
- Integration tests require external dependencies

**Best Practice:** Re-enable tests before production deployment and in CI/CD pipelines.

---

## Step 12: Update Ollama to NAIS models
`src/main/resources/application.conf`: