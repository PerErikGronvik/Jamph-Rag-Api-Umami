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