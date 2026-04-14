package no.jamph.ragumami.ragV2

import com.google.gson.JsonObject

class ConstructSQL(
    private val prebuiltSchemas: PrebuiltSchemaProvider
) {
    fun constructSql(
        queryType: String,
        variables: JsonObject,
        siteId: String,
        urlPath: String,
        prefix: String = "fagtorsdag-prod-81a6.umami_student.",
        table: String = "event"
    ): String {
        var sql = prebuiltSchemas.getSqlTemplate(queryType)
        sql = injectPredeterminedVariables(sql, siteId, urlPath, prefix, table)
        sql = replaceVariablesFromJson(sql, variables)
        
        // Clean up optional filters if not provided
        sql = sql.replace("[SELECT_FILTERS]", "")
        sql = sql.replace("[ADD_FILTERS_HERE]", "")
        
        return sql
    }
    

    private fun injectPredeterminedVariables(
        sql: String,
        siteId: String,
        urlPath: String,
        prefix: String,
        table: String
    ): String {
        var result = sql
        
        result = result.replace("[SITE_ID]", siteId)
        result = result.replace("[WEBSITE_ID]", siteId)
        result = result.replace("[URL_PATH]", urlPath)
        result = result.replace("[PATH]", urlPath)
        result = result.replace("[TABLE_NAME]", "`$prefix$table`")
        result = result.replace("[TABLE]", "`$prefix$table`")
        
        return result
    }
    
    private fun replaceVariablesFromJson(
        sql: String,
        variables: JsonObject
    ): String {
        var result = sql
        
        for ((key, value) in variables.entrySet()) {
            val placeholder = "[$key]"
            val sqlValue = when {
                value.isJsonNull -> "NULL"
                value.isJsonPrimitive -> {
                    val primitive = value.asJsonPrimitive
                    when {
                        primitive.isString -> primitive.asString
                        primitive.isNumber -> primitive.asNumber.toString()
                        primitive.isBoolean -> primitive.asBoolean.toString().uppercase()
                        else -> value.toString()
                    }
                }
                else -> value.toString()
            }
            
            result = result.replace(placeholder, sqlValue)
        }
        
        return result
    }
    

    fun validateNoMissingVariables(sql: String): List<String> {
        return Regex("\\[([A-Z_]+)\\]")
            .findAll(sql)
            .map { it.groupValues[1] }
            .toList()
    }
}


// Error Codes Reference:
// 10003: SQL template construction failed. Variable replacement or template processing error.
//        Possible causes: Missing required variables, invalid JSON structure, or template mismatch.
//        Resolution: Verify extracted variables match template placeholders, check JSON schema validity.
