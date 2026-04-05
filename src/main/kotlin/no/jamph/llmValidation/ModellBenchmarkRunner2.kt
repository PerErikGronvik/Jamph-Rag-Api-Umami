package no.jamph.llmValidation

import kotlinx.coroutines.runBlocking
import java.time.Instant
import no.jamph.ragumami.Routes

data class ModelBenchmarkResult(
    val model: String,
    val timestamp: String,
    val sqlAccuracy: Double,
    val dialectAccuracy: Double,
    val averageCostMB: Double,
    val endToEndMs: Long,
    val longPromptMs: Long,
    val shortPromptMs: Long,
    val tokensPerSecond: Double,
    val promptTokens: Int,
    val responseTokens: Int,
    val evalDurationMs: Long
)

private const val SPEED_PROBE = "Write a BigQuery SQL query that counts rows in a table."
private const val TIMER_PROBE = "Show me pageviews per day for https://aksel.nav.no"

fun runBenchmark(
    models: List<String>,
    ollamaBaseUrl: String = System.getenv("OLLAMA_BASE_URL") ?: Routes.ollamaUrl,
    llmSqlLogicFn: (String, (String) -> Unit) -> Double = { model, log -> LlmSqlLogic(model, debugLog = log) },
    dialectValidateFn: (String, (String) -> Unit) -> Double = { model, log -> DialectValidetaLlmToSql(model, debugLog = log) },
    costValidateFn: (String, (String) -> Unit) -> Double = { model, log -> CostValidateLLmEstimator(model, debugLog = log) },
    debugLog: (String) -> Unit = ::println
): List<ModelBenchmarkResult> = runBlocking {
    models.map { model ->
    debugLog("▶ Benchmarking: $model")

    val sqlAccuracy = llmSqlLogicFn(model, debugLog)
    debugLog("  SQL accuracy:     ${"%.0f".format(sqlAccuracy * 100)}%")

    val dialectAccuracy = dialectValidateFn(model, debugLog)
    debugLog("  Dialect accuracy: ${"%.0f".format(dialectAccuracy * 100)}%")

    val averageCostMB = costValidateFn(model, debugLog)
    debugLog("  Avg cost:         ${"%.2f".format(averageCostMB)} MB")

    // Timer tests
    val ollamaClient = no.jamph.ragumami.core.llm.OllamaClient(ollamaBaseUrl, model)
    val schemaService = no.jamph.bigquery.BigQuerySchemaServiceMock()
    val websites = schemaService.getWebsites()
    val ragService = no.jamph.ragumami.umami.UmamiRAGService(ollamaClient, schemaService)
    
    val endToEndResult = EndToEndTimer(ragService).measureFullPipeline(TIMER_PROBE, "https://aksel.nav.no", websites)
    debugLog("  End-to-end:       ${endToEndResult.durationMs} ms")
    
    val longPromptResult = LongPromptTimer(ollamaClient).measureLlmWithLargeSchema(TIMER_PROBE)
    debugLog("  Long prompt:      ${longPromptResult.averageDurationMs} ms (avg of ${longPromptResult.iterations})")
    
    val shortPromptResult = ShortPromptTimer(ollamaClient).measureLlmWithSmallSchema(TIMER_PROBE)
    debugLog("  Short prompt:     ${shortPromptResult.averageDurationMs} ms (avg of ${shortPromptResult.iterations})")

    val speedResult = TokenSpeedMeasurer(ollamaBaseUrl, model).measure(SPEED_PROBE)
    debugLog("  Tokens/sec:       ${"%.1f".format(speedResult.tokensPerSecond)} (avg of 5)")

        ModelBenchmarkResult(
            model           = model,
            timestamp       = Instant.now().toString(),
            sqlAccuracy     = sqlAccuracy,
            dialectAccuracy = dialectAccuracy,
            averageCostMB   = averageCostMB,
            endToEndMs      = endToEndResult.durationMs,
            longPromptMs    = longPromptResult.averageDurationMs,
            shortPromptMs   = shortPromptResult.averageDurationMs,
            tokensPerSecond = speedResult.tokensPerSecond,
            promptTokens    = speedResult.promptTokens,
            responseTokens  = speedResult.responseTokens,
            evalDurationMs  = speedResult.evalDurationMs
        )
    }
}

fun main() {
    val models = listOf("deepseek-coder-v2:16b")  // test one first

    val results = runBenchmark(models)
}