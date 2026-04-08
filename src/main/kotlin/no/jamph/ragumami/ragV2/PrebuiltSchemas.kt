package no.jamph.ragumami.ragV2

import no.jamph.bigquery.BigQuerySchemaProvider

object PrebuiltSchemas {
    
    fun bigquery(type: String, schemaProvider: BigQuerySchemaProvider): String {
        return schemaProvider.getSchemaContext()
    }
    
    fun sql(type: String): String = when (type) {
        "linear" -> """
            SELECT DATE([TIME_FIELD]) as date, COUNT([COUNT_FIELD]) as daily_count
            FROM `fagtorsdag-prod-81a6.umami_student.[TABLE_NAME]`
            WHERE website_id = '[SITE_ID]' AND [TIME_FIELD] BETWEEN '[START_DATE]' AND '[END_DATE]' [PATH_FILTER]
            GROUP BY date ORDER BY date ASC
        """.trimIndent()
        
        else -> """
            SELECT [GROUPING_FIELD] as grouping_dimension, [AGGREGATION_FUNCTION] as metric_value
            FROM `fagtorsdag-prod-81a6.umami_student.[TABLE_NAME]`
            WHERE website_id = '[SITE_ID]' [DATE_FILTER] [PATH_FILTER]
            GROUP BY grouping_dimension ORDER BY [ORDER_BY] [LIMIT_CLAUSE]
        """.trimIndent()
    }
    
    fun json(type: String): String = when (type) {
        "linear" -> """{"TIME_FIELD":"created_at","COUNT_FIELD":"event_id","TABLE_NAME":"event","START_DATE":"2025-01-01","END_DATE":"2025-12-31","PATH_FILTER":""}"""
        else -> """{"GROUPING_FIELD":"","AGGREGATION_FUNCTION":"","TABLE_NAME":"event","DATE_FILTER":"","PATH_FILTER":"","ORDER_BY":"metric_value DESC","LIMIT_CLAUSE":"LIMIT 100"}"""
    }
}
