package no.jamph.ragumami.core.rag

interface RAGOrchestrator {
    suspend fun query(question: String, context: QueryContext): RAGResponse
    suspend fun embedText(text: String): Vector
    suspend fun retrieveContext(query: String, topK: Int): List<Document>
}

data class QueryContext(
    val domain: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class RAGResponse(
    val answer: String,
    val sources: List<Document>,
    val confidence: Double
)

data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, Any>
)

data class Vector(
    val values: List<Float>
)