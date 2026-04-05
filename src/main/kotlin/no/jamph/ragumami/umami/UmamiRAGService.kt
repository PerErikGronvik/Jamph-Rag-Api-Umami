package no.jamph.ragumami.umami

import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.bigquery.BigQuerySchemaService
import no.jamph.bigquery.Website
import no.jamph.bigquery.urlToSiteIdAndPath

class UmamiRAGService(
    private val ollamaClient: OllamaClient,
    private val bigQueryService: BigQuerySchemaService? = null
) {
    suspend fun generateSQL(question: String, url: String, websites: List<Website>): String {
        val schemaContext = getSchemaContext()
        val parsed = urlToSiteIdAndPath(url, websites)
        
        val schemaAddition = RagSchemaSelector.selectSchema(question, ollamaClient)
        val prompt = SqlPrompt.buildPrompt(question, parsed.siteId, parsed.urlPath, schemaContext, schemaAddition)
        
        val raw = ollamaClient.generate(prompt)
        return extractSql(raw)
    }

    private suspend fun getSchemaContext(): String {
        if (bigQueryService == null) {
            throw IllegalStateException("BigQuery is not configured.")
        }
        
        return try {
            bigQueryService.getSchemaContext()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to retrieve BigQuery schema: ${e.message}", e)
        }
    }

    private fun extractSql(response: String): String {
        val codeBlock = Regex("```(?:sql)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .find(response)?.groupValues?.get(1)?.trim()
        return codeBlock ?: response.trim()
    }
}