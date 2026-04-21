package no.jamph.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import no.jamph.ragumami.Routes

class BigQueryAvailableValuesFinder(
    private val bigQuery: BigQuery,
    private val prefix: String = Routes.prefixUmami
) {
    
    fun findValues(websiteId: String, tableName: String, columnName: String): String {
        val query = """
            SELECT DISTINCT $columnName
            FROM `$prefix$tableName`
            WHERE website_id = '$websiteId'
              AND $columnName IS NOT NULL
            ORDER BY $columnName
            LIMIT 100
        """.trimIndent()
        
        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .setUseLegacySql(false)
            .build()
        
        val results = bigQuery.query(queryConfig)
        
        return results.iterateAll()
            .map { it.get(0).stringValue }
            .joinToString(", ")
    }
}
