package no.jamph.llmValidation

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.cookies.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder

private data class NettskjemaAnswer(
    val questionId: Long,
    val type: String = "TEXT",
    val textAnswer: String
)

private data class NettskjemaMetadata(
    val language: String = "nb"
)

private data class NettskjemaSubmission(
    val metadata: NettskjemaMetadata = NettskjemaMetadata(),
    val answers: List<NettskjemaAnswer>
)

class NettskjemaBenchmarkWriter(
    private val formId: Long = 614069L,
    private val baseUrl: String = "https://nettskjema.no",
    private val qModel:           Long = 10239299,
    private val qTimestamp:       Long = 10239300,
    private val qSqlAccuracy:     Long = 10239301,
    private val qDialectAccuracy: Long = 10239302,
    private val qTokensPerSec:    Long = 10239303,
    private val qPromptTokens:    Long = 10239304,
    private val qResponseTokens:  Long = 10239305,
    private val qEvalDurationMs:  Long = 10239306
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { gson() }
        install(HttpCookies)   // ← stores session cookie from CSRF GET and resends it with POST
    }

    fun appendRows(results: List<ModelBenchmarkResult>) = runBlocking {

        // Fetch CSRF token required by the deliver endpoint
        val csrfResponse = client.get("$baseUrl/api/v3/form/$formId/csrf")
        val csrfToken = csrfResponse.bodyAsText().trim().removeSurrounding("\"")
        println("  → CSRF token: $csrfToken")

        results.forEach { r ->
            val submission = NettskjemaSubmission(
                answers = listOf(
                    NettskjemaAnswer(qModel,           textAnswer = r.model),
                    NettskjemaAnswer(qTimestamp,       textAnswer = r.timestamp),
                    NettskjemaAnswer(qSqlAccuracy,     textAnswer = "%.1f".format(r.sqlAccuracy * 100)),
                    NettskjemaAnswer(qDialectAccuracy, textAnswer = "%.1f".format(r.dialectAccuracy * 100)),
                    NettskjemaAnswer(qTokensPerSec,    textAnswer = "%.2f".format(r.tokensPerSecond)),
                    NettskjemaAnswer(qPromptTokens,    textAnswer = r.promptTokens.toString()),
                    NettskjemaAnswer(qResponseTokens,  textAnswer = r.responseTokens.toString()),
                    NettskjemaAnswer(qEvalDurationMs,  textAnswer = r.evalDurationMs.toString())
                )
            )

            val debugJson = GsonBuilder().setPrettyPrinting().create().toJson(submission)
            println("  → Sending JSON:\n$debugJson")

            val response = client.post("$baseUrl/api/v3/form/$formId/submission") {
                contentType(ContentType.Application.Json)
                setBody(submission)
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val submissionId = Gson().fromJson(body, Map::class.java)["submissionId"]
                println("  ✓ Created submission $submissionId for ${r.model} — delivering...")

                val deliverResponse = client.post("$baseUrl/api/v3/form/$formId/submission/$submissionId/deliver") {
                    contentType(ContentType.Application.Json)
                    header("X-XSRF-TOKEN", csrfToken)
                }
                if (deliverResponse.status.isSuccess()) {
                    println("  ✓ Delivered ${r.model}: ${deliverResponse.bodyAsText()}")
                } else {
                    println("  ✗ Deliver failed for ${r.model}: ${deliverResponse.status} — ${deliverResponse.bodyAsText()}")
                }
            } else {
                println("  ✗ Failed for ${r.model}: ${response.status} — ${response.bodyAsText()}")
            }
        }
    }
}

fun main() {
    val dummyResult = ModelBenchmarkResult(
        model           = "TEST-MODEL",
        timestamp       = "2026-03-31T12:00:00Z",
        sqlAccuracy     = 0.99,
        dialectAccuracy = 0.88,
        tokensPerSecond = 42.0,
        promptTokens    = 10,
        responseTokens  = 20,
        evalDurationMs  = 500L
    )
    NettskjemaBenchmarkWriter().appendRows(listOf(dummyResult))
}