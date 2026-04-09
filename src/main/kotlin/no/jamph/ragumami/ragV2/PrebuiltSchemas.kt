package no.jamph.ragumami.ragV2

import no.jamph.bigquery.BigQuerySchemaProvider

data class SchemaTriple(
    val bigQuerySchema: String,
    val sqlTemplate: String,
    val simplifiedSql: String,
    val jsonSchema: String
)

object PrebuiltSchemas {
    private val cache = mutableMapOf<String, SchemaTriple>()
    
    private fun get(type: String, schemaProvider: BigQuerySchemaProvider?): SchemaTriple {
        return cache.getOrPut(type) {
            when (type) {
                "linear" -> linearSchema(schemaProvider!!)
                "rankings" -> rankingsSchema(schemaProvider!!)
                else -> defaultSchema(schemaProvider!!)
            }
        }
    }
    
    fun getBigQuerySchema(type: String, schemaProvider: BigQuerySchemaProvider) = get(type, schemaProvider).bigQuerySchema
    fun getSqlTemplate(type: String) = get(type, null).sqlTemplate
    fun getSimplifiedSql(type: String) = get(type, null).simplifiedSql
    fun getJsonSchema(type: String) = get(type, null).jsonSchema
    
    private fun linearSchema(schemaProvider: BigQuerySchemaProvider) = SchemaTriple(
        bigQuerySchema = """**bigquery schema**""".trimIndent(),
        sqlTemplate = """**sql template**""".trimIndent(),
        simplifiedSql = """**sql for llm**""".trimIndent(),
        jsonSchema = """**json schema**""".trimIndent()
    )
    
    private fun rankingsSchema(schemaProvider: BigQuerySchemaProvider) = SchemaTriple(
        bigQuerySchema = """**bigquery schema**""".trimIndent(),
        sqlTemplate = """**sql template**""".trimIndent(),
        simplifiedSql = """**sql for llm**""".trimIndent(),
        jsonSchema = """**json schema**""".trimIndent()
    )
    
    private fun defaultSchema(schemaProvider: BigQuerySchemaProvider) = SchemaTriple(
        bigQuerySchema = """**bigquery schema**""".trimIndent(),
        sqlTemplate = """**sql template**""".trimIndent(),
        simplifiedSql = """**sql for llm**""".trimIndent(),
        jsonSchema = """**json schema**""".trimIndent()
    )
}
