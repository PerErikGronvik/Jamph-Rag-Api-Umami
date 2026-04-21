package no.jamph.bigquery

interface BigQuerySchemaProvider {
    fun getWebsites(): List<Website>
    fun getSchemaContext(): String
    fun timeNow(): String
}
