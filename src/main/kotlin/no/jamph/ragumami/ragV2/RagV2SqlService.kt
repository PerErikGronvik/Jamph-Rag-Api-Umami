package no.jamph.ragumami.ragV2

import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.bigquery.BigQuerySchemaProvider

class RagV2SqlService(
    private val ollamaClient: OllamaClient,
    private val bigQueryService: BigQuerySchemaProvider
) {
    private val schemaProvider = PrebuiltSchemaService(bigQueryService)
    private val queryTypeClassifier = PickASqlQuestionTypeLlm(ollamaClient, bigQueryService)
    private val variableExtractor = PickVariableJsonLlm(ollamaClient, schemaProvider)
    private val sqlConstructor = ConstructSQL(schemaProvider)
    private val otherLlm = OtherLlm(ollamaClient, bigQueryService)

    suspend fun generateSql(
        userPrompt: String,
        url: String,
        pathOperator: String = "starts-with"
    ): String {
        val classificationResult = queryTypeClassifier.classifyQueryType(
            userPrompt = userPrompt,
            url = url,
            pathOperator = pathOperator
        )
        
        return if (classificationResult.queryType == "default") {
            otherLlm.generateSql(
                userPrompt = classificationResult.userPrompt,
                siteId = classificationResult.siteId,
                urlPath = classificationResult.urlPath
            )
        } else {
            val extractedVariables = variableExtractor.extractVariables(
                queryType = classificationResult.queryType,
                siteId = classificationResult.siteId,
                urlPath = classificationResult.urlPath,
                userPrompt = classificationResult.userPrompt
            )
            
            sqlConstructor.constructSql(
                queryType = classificationResult.queryType,
                variables = extractedVariables.variables,
                siteId = extractedVariables.siteId,
                urlPath = extractedVariables.urlPath
            )
        }
    }
    

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

data class SqlGenerationResult(
    val sql: String,
    val queryType: String,
    val siteId: String,
    val urlPath: String,
    val extractedVariables: String
)
