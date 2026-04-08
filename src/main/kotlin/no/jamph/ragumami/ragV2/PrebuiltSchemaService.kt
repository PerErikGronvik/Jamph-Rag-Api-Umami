package no.jamph.ragumami.ragV2

import no.jamph.bigquery.BigQuerySchemaProvider

class PrebuiltSchemaService(
    private val schemaProvider: BigQuerySchemaProvider
) : PrebuiltSchemaProvider {
    
    override fun getBigQuerySchema(queryType: String) = PrebuiltSchemas.bigquery(queryType, schemaProvider)
    override fun getSqlTemplate(queryType: String) = PrebuiltSchemas.sql(queryType)
    override fun getJsonSchema(queryType: String) = PrebuiltSchemas.json(queryType)
}
