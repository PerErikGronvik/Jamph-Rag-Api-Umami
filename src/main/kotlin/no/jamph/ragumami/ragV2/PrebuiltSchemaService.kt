package no.jamph.ragumami.ragV2

import no.jamph.bigquery.BigQuerySchemaProvider

class PrebuiltSchemaService(
    private val schemaProvider: BigQuerySchemaProvider
) : PrebuiltSchemaProvider {
    
    override fun getBigQuerySchema(queryType: String) = PrebuiltSchemas.getBigQuerySchema(queryType, schemaProvider)
    override fun getSqlTemplate(queryType: String) = PrebuiltSchemas.getSqlTemplate(queryType, schemaProvider)
    override fun getSimplifiedSql(queryType: String) = PrebuiltSchemas.getSimplifiedSql(queryType, schemaProvider)
    override fun getJsonSchema(queryType: String) = PrebuiltSchemas.getJsonSchema(queryType, schemaProvider)
}
