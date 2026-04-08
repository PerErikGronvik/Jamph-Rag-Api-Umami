package no.jamph.ragumami.ragV2

import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.bigquery.BigQuerySchemaProvider
import org.slf4j.LoggerFactory

/**
 * Main orchestrator for the RAG v2 SQL generation pipeline.
 * 
 * This service coordinates the three-step process:
 * 1. QueryTypeClassifier - Classify the query type
 * 2. VariableExtractor - Extract variables from natural language
 * 3. SqlConstructor - Build the final SQL query
 * 
 * Usage example:
 * ```kotlin
 * val ragV2Service = RagV2SqlService(ollamaClient, bigQueryService)
 * val sql = ragV2Service.generateSql(
 *     userPrompt = "Show me pageviews per day in 2025",
 *     url = "https://aksel.nav.no"
 * )
 * ```
 */
class RagV2SqlService(
    private val ollamaClient: OllamaClient,
    private val bigQueryService: BigQuerySchemaProvider
) {
    private val logger = LoggerFactory.getLogger(RagV2SqlService::class.java)
    
    // Initialize the pipeline components
    private val schemaProvider = PrebuiltSchemaService(bigQueryService)
    private val queryTypeClassifier = QueryTypeClassifier(ollamaClient, bigQueryService)
    private val variableExtractor = VariableExtractor(ollamaClient, schemaProvider)
    private val sqlConstructor = SqlConstructor(schemaProvider)
    
    /**
     * Generates a SQL query from a natural language prompt.
     * 
     * This is the main entry point for the RAG v2 pipeline. It orchestrates
     * all three steps: classification, variable extraction, and SQL construction.
     * 
     * @param userPrompt The natural language question from the user
     * @param url The full URL (e.g., "https://aksel.nav.no/designsystemet")
     * @param pathOperator "starts-with" (default) or "equals" for URL path matching
     * @return The generated SQL query ready for execution
     * @throws IllegalStateException if any step fails after retries
     * @throws IllegalArgumentException if URL parsing fails
     */
    suspend fun generateSql(
        userPrompt: String,
        url: String,
        pathOperator: String = "starts-with"
    ): String {
        logger.info("Starting RAG v2 SQL generation for prompt: '{}'", userPrompt)
        
        try {
            // Step 1: Classify the query type
            logger.debug("Step 1: Classifying query type...")
            val classificationResult = queryTypeClassifier.classifyQueryType(
                userPrompt = userPrompt,
                url = url,
                pathOperator = pathOperator
            )
            logger.info("Classified as query type: '{}'", classificationResult.queryType)
            
            // Step 2: Extract variables using LLM
            logger.debug("Step 2: Extracting variables...")
            val extractedVariables = variableExtractor.extractVariables(
                queryType = classificationResult.queryType,
                siteId = classificationResult.siteId,
                urlPath = classificationResult.urlPath,
                userPrompt = classificationResult.userPrompt
            )
            logger.info("Successfully extracted {} variables", extractedVariables.variables.size())
            
            // Step 3: Construct the final SQL
            logger.debug("Step 3: Constructing SQL...")
            val sql = sqlConstructor.constructSql(
                queryType = classificationResult.queryType,
                variables = extractedVariables.variables,
                siteId = extractedVariables.siteId,
                urlPath = extractedVariables.urlPath
            )
            
            logger.info("Successfully generated SQL query")
            return sql
            
        } catch (e: Exception) {
            logger.error("Failed to generate SQL for prompt: '{}'", userPrompt, e)
            throw IllegalStateException("SQL generation failed: ${e.message}", e)
        }
    }
    
    /**
     * Generates SQL and returns detailed debug information about each step.
     * Useful for debugging and understanding how the pipeline made decisions.
     */
    suspend fun generateSqlWithDebugInfo(
        userPrompt: String,
        url: String,
        pathOperator: String = "starts-with"
    ): SqlGenerationResult {
        val classificationResult = queryTypeClassifier.classifyQueryType(userPrompt, url, pathOperator)
        val extractedVariables = variableExtractor.extractVariables(
            classificationResult.queryType,
            classificationResult.siteId,
            classificationResult.urlPath,
            classificationResult.userPrompt
        )
        val sql = sqlConstructor.constructSql(
            classificationResult.queryType,
            extractedVariables.variables,
            extractedVariables.siteId,
            extractedVariables.urlPath
        )
        
        return SqlGenerationResult(
            sql = sql,
            queryType = classificationResult.queryType,
            siteId = classificationResult.siteId,
            urlPath = classificationResult.urlPath,
            extractedVariables = extractedVariables.variables.toString()
        )
    }
}

/**
 * Result object containing the SQL and debug information.
 */
data class SqlGenerationResult(
    val sql: String,
    val queryType: String,
    val siteId: String,
    val urlPath: String,
    val extractedVariables: String
)
