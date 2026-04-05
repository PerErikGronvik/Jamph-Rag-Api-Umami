package no.jamph.ragumami.umami

import no.jamph.ragumami.core.llm.OllamaClient
import no.jamph.bigquery.BigQuerySchemaService

class UmamiRAGService(
    private val ollamaClient: OllamaClient,
    private val bigQueryService: BigQuerySchemaService? = null
) {
    suspend fun chat(message: String): String {
        val prompt = """
        Du er en hjelpsom AI-assistent for Umami Analytics.
        Brukerens spørsmål: $message
        
        Gi et kort og nyttig svar.
        """.trimIndent()
        
        return ollamaClient.generate(prompt)
    }
    
    suspend fun generateSQL(naturalLanguageQuery: String, url: String? = null): String {
        // Require BigQuery to be configured
        if (bigQueryService == null) {
            throw IllegalStateException("BigQuery is not configured. Cannot generate SQL queries.")
        }
        
        // Get schema context from BigQuery
        val schemaContext = try {
            bigQueryService.getSchemaContext()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to retrieve BigQuery schema: ${e.message}", e)
        }
        
        // Parse siteId and urlPath from the query (frontend embeds them in natural language)
        val siteId = extractSiteId(naturalLanguageQuery)
        val urlPath = extractUrlPath(naturalLanguageQuery)
        
        // Extract the actual question after "Spørsmål:" or use full query
        val question = naturalLanguageQuery.substringAfter("Spørsmål:", naturalLanguageQuery).trim()
        
        // Use universal prompt with 13 critical rules
        val prompt = SqlPrompt.buildPrompt(question, siteId, urlPath, schemaContext)
        val raw = ollamaClient.generate(prompt)
        return extractSql(raw)
    }

    private fun extractSql(response: String): String {
        val codeBlock = Regex("```(?:sql)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .find(response)?.groupValues?.get(1)?.trim()
        return codeBlock ?: response.trim()
    }
    
    /**
     * Extract website_id from query string (frontend embeds it as: website_id = 'xxx')
     */
    private fun extractSiteId(query: String): String {
        val regex = Regex("website_id\\s*=\\s*'([^']+)'")
        return regex.find(query)?.groupValues?.get(1) ?: "fb69e1e9-1bd3-4fd9-b700-9d035cbf44e1" // default to Aksel
    }
    
    /**
     * Extract url_path filter from query string (frontend embeds it as: url_path LIKE 'xxx' or url_path = 'xxx')
     */
    private fun extractUrlPath(query: String): String {
        val likeRegex = Regex("url_path\\s+LIKE\\s+'([^']+)'")
        val equalsRegex = Regex("url_path\\s*=\\s*'([^']+)'")
        return likeRegex.find(query)?.groupValues?.get(1)
            ?: equalsRegex.find(query)?.groupValues?.get(1)
            ?: "/" // default to root
    }
}