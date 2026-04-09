package no.jamph.ragumami.ragV2

interface PrebuiltSchemaProvider {
    fun getBigQuerySchema(queryType: String): String
    fun getSqlTemplate(queryType: String): String
    fun getSimplifiedSql(queryType: String): String
    fun getJsonSchema(queryType: String): String
}
