package no.jamph.llmValidation

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking

private data class NettskjemaAnswer(
    val questionId: Long,
    val answers: List<Map<String, String>>
)

private data class NettskjemaSubmission(
    val answers: List<NettskjemaAnswer>
)

class NettskjemaBenchmarkWriter(
    private val formId: Long = 614069L,
    private val qModel:           Long = TODO("fyll inn elementId for Model"),
    private val qTimestamp:       Long = TODO("fyll inn elementId for Timestamp"),
    private val qSqlAccuracy:     Long = TODO("fyll inn elementId for SQL Accuracy (%)"),
    private val qDialectAccuracy: Long = TODO("fyll inn elementId for Dialect Accuracy (%)"),
    private val qTokensPerSec:    Long = TODO("fyll inn elementId for Tokens/sec"),
    private val qPromptTokens:    Long = TODO("fyll inn elementId for Prompt Tokens"),
    private val qResponseTokens:  Long = TODO("fyll inn elementId for Response Tokens"),
    private val qEvalDurationMs:  Long = TODO("fyll inn elementId for Eval Duration (ms)")
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { gson() }
    }

    fun appendRows(results: List<ModelBenchmarkResult>) = runBlocking {
        results.forEach { r ->
            val submission = NettskjemaSubmission(
                answers = listOf(
                    NettskjemaAnswer(qModel,           listOf(mapOf("text" to r.model))),
                    NettskjemaAnswer(qTimestamp,       listOf(mapOf("text" to r.timestamp))),
                    NettskjemaAnswer(qSqlAccuracy,     listOf(mapOf("text" to "%.1f".format(r.sqlAccuracy * 100)))),
                    NettskjemaAnswer(qDialectAccuracy, listOf(mapOf("text" to "%.1f".format(r.dialectAccuracy * 100)))),
                    NettskjemaAnswer(qTokensPerSec,    listOf(mapOf("text" to "%.2f".format(r.tokensPerSecond)))),
                    NettskjemaAnswer(qPromptTokens,    listOf(mapOf("text" to r.promptTokens.toString()))),
                    NettskjemaAnswer(qResponseTokens,  listOf(mapOf("text" to r.responseTokens.toString()))),
                    NettskjemaAnswer(qEvalDurationMs,  listOf(mapOf("text" to r.evalDurationMs.toString())))
                )
            )

            val response = client.post("https://nettskjema.no/api/v3/form/$formId/submission") {
                contentType(ContentType.Application.Json)
                setBody(submission)
            }

            if (response.status.isSuccess()) {
                println("  ✓ Submitted ${r.model} to nettskjema")
            } else {
                println("  ✗ Failed for ${r.model}: ${response.status} — ${response.bodyAsText()}")
            }
        }
    }
}