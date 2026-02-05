package no.jamph.ragumami.umami.domain

import no.jamph.ragumami.core.rag.RAGOrchestrator
import no.jamph.ragumami.core.rag.QueryContext
import no.jamph.ragumami.core.llm.OllamaClient

class UmamiRAGService(
    private val ollamaClient: OllamaClient
) {
    suspend fun chat(message: String): String {
        val prompt = """
        Du er en hjelpsom AI-assistent for Umami Analytics.
        Brukerens spørsmål: $message
        
        Gi et kort og nyttig svar.
        """.trimIndent()
        
        return ollamaClient.generate(prompt)
    }
    
    suspend fun generateSQL(naturalLanguageQuery: String): String {
        // Umami-specific: Get Prisma schema context
        val schemaContext = getUmamiSchemaContext()
        
        val prompt = buildSQLPrompt(naturalLanguageQuery, schemaContext)
        
        return ollamaClient.generate(prompt)
    }
    
    private fun getUmamiSchemaContext(): String {
        return """
        -- Umami Analytics Database Schema (BigQuery)
        -- Database: fagtorsdag-prod-81a6.umami_student.public_website
        
        TABLE: `fagtorsdag-prod-81a6.umami_student.public_website`
        Columns:
          - website_id (STRING/INT): Unique identifier for website
          - name (STRING): Website name
        
        Default columns to use: website_id, name
        
        Example query:
        SELECT 
          website_id,
          name
        FROM 
          `fagtorsdag-prod-81a6.umami_student.public_website`
        """.trimIndent()
    }
    
    private fun buildSQLPrompt(query: String, schema: String): String {
        return """
        You are a BigQuery SQL expert for Umami Analytics.
        
        IMPORTANT INSTRUCTIONS:
        - Always use the full table name: `fagtorsdag-prod-81a6.umami_student.public_website`
        - Use backticks (`) for table names in BigQuery
        - Default columns: website_id, name (use these unless user specifies others)
        - Return ONLY the SQL query, no explanations or markdown
        
        Database Schema:
        $schema
        
        User Query: $query
        
        Generate the BigQuery SQL query:
        """.trimIndent()
    }
}