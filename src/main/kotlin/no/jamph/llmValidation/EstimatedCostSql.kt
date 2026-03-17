package no.jamph.llmValidation

import com.google.cloud.bigquery.*

fun estimateCostInMB(sql: String): Double {
    if (!isSqlQueryValid(sql)) return 0.0

    val bigquery: BigQuery = BigQueryOptions.getDefaultInstance().service
    val config = QueryJobConfiguration.newBuilder(sql)
        .setDryRun(true)
        .setUseLegacySql(false)
        .build()

    return try {
        val job = bigquery.create(JobInfo.of(config))
        val stats = job.getStatistics<JobStatistics.QueryStatistics>()
        val bytesProcessed: Long = stats.totalBytesProcessed ?: 0L
        bytesProcessed.toDouble() / (1024.0 * 1024.0) // MB
    } catch (e: Exception) {
        println("Error estimating query size: ${e.message}")
        0.0
    }
}