package no.jamph.llmValidation

import com.google.cloud.bigquery.*

fun estimateCostInMB(sql: String): Double {

    if (!isSqlQueryValid(sql)) return 0.0

    // Initialize the BigQuery client
    val bigquery: BigQuery = BigQueryOptions.getDefaultInstance().service

    val config = QueryJobConfiguration.newBuilder(sql)
        .setDryRun(true)
        .setUseLegacySql(false)
        .build()

    // Execute the query as a dry run to get the statistics without actually running it
    val job = bigquery.create(JobInfo.of(config))
    val stats = job.getStatistics<QueryStatistics>()
    val bytesProcessed: Double = (stats.totalBytesProcessed ?: 0L).toDouble()

    return bytesProcessed / (1024 * 1024) // Convert to MB
}