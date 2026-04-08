package no.jamph.ragumami.ragV2

import com.google.gson.JsonObject
import org.slf4j.LoggerFactory

/**
 * Constructs executable SQL by replacing variable placeholders in templates.
 * 
 * This is Step 3 of the RAG v2 pipeline. It takes the SQL template with [VARIABLE_NAME]
 * placeholders and replaces them with actual values extracted in Step 2.
 */
class SqlConstructor(
    private val prebuiltSchemas: PrebuiltSchemaProvider
) {
    private val logger = LoggerFactory.getLogger(SqlConstructor::class.java)
    
    /**
     * Constructs the final SQL query by replacing variable placeholders.
     * 
     * Variable placeholders in the SQL template should be in the format: [VARIABLE_NAME]
     * These are replaced with corresponding values from the variables JSON object.
     * 
     * @param queryType The classified query type (used to fetch the SQL template)
     * @param variables JSON object containing variable names and their values
     * @param siteId Website ID to inject (predetermined variable)
     * @param urlPath URL path to inject (predetermined variable)
     * @return The complete SQL query with all variables replaced
     * @throws IllegalStateException if required variables are missing or replacement fails
     */
    fun constructSql(
        queryType: String,
        variables: JsonObject,
        siteId: String,
        urlPath: String
    ): String {
        logger.info("Constructing SQL for query type '{}'", queryType)
        
        var sql = prebuiltSchemas.getSqlTemplate(queryType)
        sql = injectPredeterminedVariables(sql, siteId, urlPath)
        sql = replaceVariablesFromJson(sql, variables)
        
        logger.info("Successfully constructed SQL query")
        logger.debug("Final SQL: {}", sql)
        
        return sql
    }
    
    /**
     * Injects predetermined variables that are always available.
     * These come from the request context, not from the LLM extraction.
     */
    private fun injectPredeterminedVariables(
        sql: String,
        siteId: String,
        urlPath: String
    ): String {
        var result = sql
        
        result = result.replace("[SITE_ID]", siteId)
        result = result.replace("[WEBSITE_ID]", siteId)
        result = result.replace("[URL_PATH]", urlPath)
        result = result.replace("[PATH]", urlPath)
        
        return result
    }
    
    /**
     * Replaces variables from the extracted JSON object.
     * Handles different data types (strings, numbers, booleans).
     */
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
                else -> {
                    logger.warn("Complex JSON value for variable '{}' will be converted to string", key)
                    value.toString()
                }
            }
            
            result = result.replace(placeholder, sqlValue)
            logger.debug("Replaced {} with '{}'", placeholder, sqlValue)
        }
        
        val remainingPlaceholders = Regex("\\[([A-Z_]+)\\]").findAll(result).toList()
        if (remainingPlaceholders.isNotEmpty()) {
            val missingVars = remainingPlaceholders.map { it.groupValues[1] }
            logger.warn("SQL still contains unreplaced placeholders: {}", missingVars)
        }
        
        return result
    }
    
    /**
     * Validates that all required variable placeholders have been replaced.
     * Can be called by consumers if strict validation is needed.
     * 
     * @return List of missing variable names (empty if all replaced)
     */
    fun validateNoMissingVariables(sql: String): List<String> {
        return Regex("\\[([A-Z_]+)\\]")
            .findAll(sql)
            .map { it.groupValues[1] }
            .toList()
    }
}
