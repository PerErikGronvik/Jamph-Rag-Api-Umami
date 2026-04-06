package no.jamph.llmValidation

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import no.jamph.bigquery.BigQuerySchemaServiceMock
import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.ragumami.Routes
import no.jamph.ragumami.umami.UmamiRAGService
import kotlinx.coroutines.runBlocking

private const val AKSEL_ID = "fb69e1e9-1bd3-4fd9-b700-9d035cbf44e1"

data class CostTestCase(
    val question: String,
    val url: String,
    val expectedCostMB: Double
)

data class CostResult(
    val question: String,
    val averageCostMB: Double,
    val expectedCostMB: Double,
    val withinRange: Boolean
)

private fun getOptimizedQueries(): List<Pair<String, String>> = listOf(
    "Unique users by device" to """
        SELECT 
            COUNT(DISTINCT session_id) as unique_users,
            device,
            COUNT(*) as total_events
        FROM `fagtorsdag-prod-81a6.umami_student.event`
        WHERE website_id = '$AKSEL_ID'
            AND event_type = 1
            AND created_at >= '2025-01-01'
        GROUP BY device
        ORDER BY total_events DESC
        LIMIT 100
    """.trimIndent(),
    
    "Activity overview" to """
        SELECT 
            DATE(created_at) as date,
            event_type,
            COUNT(*) as event_count,
            COUNT(DISTINCT session_id) as unique_sessions
        FROM `fagtorsdag-prod-81a6.umami_student.event`
        WHERE website_id = '$AKSEL_ID'
            AND created_at >= '2025-01-01'
        GROUP BY date, event_type
        ORDER BY date DESC
        LIMIT 100
    """.trimIndent(),
    
    "URL paths with sessions" to """
        SELECT 
            url_path,
            COUNT(DISTINCT session_id) as unique_sessions,
            COUNT(*) as total_visits
        FROM `fagtorsdag-prod-81a6.umami_student.event`
        WHERE website_id = '$AKSEL_ID'
            AND event_type = 1
            AND created_at >= '2025-01-01'
        GROUP BY url_path
        ORDER BY total_visits DESC
        LIMIT 50
    """.trimIndent(),
    
    "Events per day with variants" to """
        SELECT 
            DATE(created_at) as date,
            COUNT(*) as event_count,
            COUNT(DISTINCT event_name) as unique_event_names,
            COUNT(DISTINCT url_path) as unique_paths
        FROM `fagtorsdag-prod-81a6.umami_student.event`
        WHERE website_id = '$AKSEL_ID'
            AND created_at >= '2025-01-01'
        GROUP BY date
        ORDER BY date DESC
        LIMIT 100
    """.trimIndent(),
    
    "Session journey with time" to """
        WITH session_page_counts AS (
            SELECT 
                session_id,
                COUNT(*) as page_count
            FROM `fagtorsdag-prod-81a6.umami_student.event`
            WHERE website_id = '$AKSEL_ID'
                AND event_type = 1
                AND created_at >= '2025-01-01'
            GROUP BY session_id
            HAVING COUNT(*) > 3
        ),
        session_events AS (
            SELECT 
                e.session_id,
                e.url_path,
                e.created_at,
                LAG(e.created_at) OVER (PARTITION BY e.session_id ORDER BY e.created_at) as prev_time
            FROM `fagtorsdag-prod-81a6.umami_student.event` e
            INNER JOIN session_page_counts spc ON e.session_id = spc.session_id
            WHERE e.website_id = '$AKSEL_ID'
                AND e.event_type = 1
                AND e.created_at >= '2025-01-01'
        )
        SELECT 
            session_id,
            url_path,
            created_at,
            TIMESTAMP_DIFF(created_at, prev_time, SECOND) as seconds_since_prev
        FROM session_events
        ORDER BY session_id, created_at
        LIMIT 500
    """.trimIndent()
)

fun CostValidateLLmEstimator(
    modelName: String,
    iterations: Int = 10,
    bigquery: BigQuery = defaultBigQuery(),
    debugLog: (String) -> Unit = ::println
): Double = runBlocking {
    val schemaService = BigQuerySchemaServiceMock()
    val websites = schemaService.getWebsites()
    
    val ollamaClient = OllamaClient(
        baseUrl = System.getenv("OLLAMA_BASE_URL") ?: Routes.ollamaUrl,
        model = modelName
    )
    val ragService = UmamiRAGService(ollamaClient, schemaService)

    val optimizedQueries = getOptimizedQueries()
    
    debugLog("=== Calculating Expected Costs from Optimized Queries ===")
    val expectedCosts = optimizedQueries.mapIndexed { index, (name, sql) ->
        try {
            val costMB = estimateCostInMB(sql, bigquery)
            debugLog("${index + 1}. $name: ${costMB.format(2)} MB")
            costMB
        } catch (e: Exception) {
            debugLog("${index + 1}. $name: Error - ${e.message}")
            50.0
        }
    }
    debugLog("=============================")
    debugLog("")

    val testCases = listOf(
        CostTestCase(
            question = "Hvor mange unike brukere har vi egentlig hatt, og hvilke enheter brukte de, og hvilke sider besøkte de, i detalj?",
            url = "https://aksel.nav.no",
            expectedCostMB = expectedCosts[0]
        ),
        CostTestCase(
            question = "Gi meg en full oversikt over all aktivitet, inkludert sessions, events, og alle metadata-felter",
            url = "https://aksel.nav.no",
            expectedCostMB = expectedCosts[1]
        ),
        CostTestCase(
            question = "Jeg trenger å se alle url_path som noen gang er besøkt, sammen med alle sessions som har besøkt dem, og alle events i hver session",
            url = "https://aksel.nav.no",
            expectedCostMB = expectedCosts[2]
        ),
        CostTestCase(
            question = "Tell antall events per dag, men jeg vil også se alle event_name variantene og alle url_path variantene per dag",
            url = "https://aksel.nav.no",
            expectedCostMB = expectedCosts[3]
        ),
        CostTestCase(
            question = "Finn alle sessions hvor brukeren har besøkt mer enn 3 sider, og vis meg hele sesjonsforløpet med tid mellom hver side",
            url = "https://aksel.nav.no",
            expectedCostMB = expectedCosts[4]
        ),
    )

    val results = mutableListOf<CostResult>()

    testCases.forEach { testCase ->
        debugLog("Testing: ${testCase.question}")
        
        val costs = mutableListOf<Double>()
        
        repeat(iterations) { iteration ->
            val sql = ragService.generateSQL(testCase.question, testCase.url, websites)
            
            if (isSqlQueryValid(sql)) {
                val costMB = estimateCostInMB(sql, bigquery)
                costs.add(costMB)
                debugLog("  Iteration ${iteration + 1}: ${costMB.format(2)} MB")
            } else {
                debugLog("  Iteration ${iteration + 1}: Invalid SQL")
            }
        }
        
        val avgCost = if (costs.isNotEmpty()) costs.average() else 0.0
        val withinRange = avgCost <= testCase.expectedCostMB * 1.5
        
        results.add(
            CostResult(
                question = testCase.question,
                averageCostMB = avgCost,
                expectedCostMB = testCase.expectedCostMB,
                withinRange = withinRange
            )
        )
        
        debugLog("  → Average: ${avgCost.format(2)} MB (expected: ${testCase.expectedCostMB} MB) ${if (withinRange) "✓" else "✗"}")
        debugLog("")
    }

    val overallAverageCost = if (results.isNotEmpty()) {
        results.map { it.averageCostMB }.average()
    } else {
        0.0
    }
    
    debugLog("=== Overall Average Cost: ${overallAverageCost.format(2)} MB ===")
    
    overallAverageCost
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
