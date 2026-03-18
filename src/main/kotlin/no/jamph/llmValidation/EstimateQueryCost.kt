package no.jamph.llmValidation

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.JobStatistics

fun estimateQueryCostInMB(sql: String): Double {
    
    if (!isSqlQueryValid(sql)) {
        throw IllegalArgumentException("Invalid SQL query")
    }


    val bigQuery = BigQueryOptions.getDefaultInstance().service
        
    val queryConfig = QueryJobConfiguration.newBuilder(sql)
        .setDryRun(true)
        .build()
    
    val job = bigQuery.create(JobInfo.of(queryConfig))

    val stats = job.getStatistics<JobStatistics.QueryStatistics>()
        ?: throw RuntimeException("No estimate available")
    
    val bytes = stats.totalBytesProcessed ?: 0L
    return bytes / (1024.0 * 1024.0)
}