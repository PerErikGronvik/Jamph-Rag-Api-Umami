package no.jamph.ragumami.ragV2

import no.jamph.ragumami.core.llm.OllamaClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.slf4j.LoggerFactory

/**
 * Result of variable extraction.
 * 
 * @property variables JSON object containing extracted variable values
 * @property siteId The website ID (passed through from Step 1)
 * @property urlPath The URL path (passed through from Step 1)
 * @property userPrompt The original prompt (passed through from Step 1)
 */
data class ExtractedVariables(
    val variables: JsonObject,
    val siteId: String,
    val urlPath: String,
    val userPrompt: String
)

/**
 * Extracts variable values from natural language using LLM and prebuilt schemas.
 * 
 * This is Step 2 of the RAG v2 pipeline. It takes a classified query type and uses
 * the LLM to fill in the variable placeholders needed for the SQL template.
 */
class VariableExtractor(
    private val ollamaClient: OllamaClient,
    private val prebuiltSchemas: PrebuiltSchemaProvider
) {
    private val logger = LoggerFactory.getLogger(VariableExtractor::class.java)
    private val gson = Gson()
    
    companion object {
        private const val MAX_RETRIES = 3
    }
    
    /**
     * Extracts variables from the user's prompt using LLM.
     * 
     * @param queryType The classified query type from Step 1
     * @param siteId The website ID from Step 1
     * @param urlPath The URL path from Step 1
     * @param userPrompt The original user question from Step 1
     * @return ExtractedVariables containing the JSON with variable values and context
     * @throws IllegalStateException if variable extraction fails after MAX_RETRIES attempts
     */
    suspend fun extractVariables(
        queryType: String,
        siteId: String,
        urlPath: String,
        userPrompt: String
    ): ExtractedVariables {
        logger.info("Extracting variables for query type '{}' from prompt: '{}'", queryType, userPrompt)
        
        val bigQuerySchema = prebuiltSchemas.getBigQuerySchema(queryType)
        val sqlTemplate = prebuiltSchemas.getSqlTemplate(queryType)
        val jsonSchema = prebuiltSchemas.getJsonSchema(queryType)
        
        for (attempt in 1..MAX_RETRIES) {
            try {
                val extractionPrompt = buildExtractionPrompt(
                    userPrompt = userPrompt,
                    bigQuerySchema = bigQuerySchema,
                    sqlTemplate = sqlTemplate,
                    jsonSchema = jsonSchema,
                    attempt = attempt
                )
                
                val response = ollamaClient.generate(extractionPrompt)
                val extractedJson = parseAndValidateJson(response, jsonSchema)
                
                if (extractedJson != null) {
                    logger.info("Successfully extracted variables on attempt {}", attempt)
                    return ExtractedVariables(
                        variables = extractedJson,
                        siteId = siteId,
                        urlPath = urlPath,
                        userPrompt = userPrompt
                    )
                }
                
                logger.warn("Attempt {}/{}: Invalid JSON response: '{}'", 
                    attempt, MAX_RETRIES, response.take(200))
                
            } catch (e: Exception) {
                logger.error("Attempt {}/{}: Error during variable extraction", attempt, MAX_RETRIES, e)
                if (attempt == MAX_RETRIES) throw e
            }
        }
        
        throw IllegalStateException(
            "Failed to extract variables after $MAX_RETRIES attempts for query type '$queryType'"
        )
    }
    
    /**
     * Builds the LLM prompt for variable extraction.
     * Uses progressively stricter prompts on retry attempts.
     */
    private fun buildExtractionPrompt(
        userPrompt: String,
        bigQuerySchema: String,
        sqlTemplate: String,
        jsonSchema: String,
        attempt: Int
    ): String {
        return if (attempt == 1) {
            """
            You are a SQL BigQuery expert. Your task is to extract variable values from the user's question
            and fill them into the JSON template.
            
            BigQuery Schema:
            $bigQuerySchema
            
            SQL Template:
            $sqlTemplate
            
            User Question: $userPrompt
            
            Fill in the missing variables in this JSON object. Return ONLY the JSON object with values filled in, no other text:
            
            $jsonSchema
            """.trimIndent()
        } else {
            """
            CRITICAL: Return ONLY valid JSON. NO explanations. NO markdown. NO code blocks.
            
            BigQuery Schema:
            $bigQuerySchema
            
            SQL Template:
            $sqlTemplate
            
            User Question: $userPrompt
            
            Return this JSON with values filled in:
            $jsonSchema
            
            JSON:
            """.trimIndent()
        }
    }
    
    /**
     * Parses the LLM response and validates it as JSON.
     * Handles markdown code blocks and other formatting issues.
     * 
     * @return JsonObject if valid, null otherwise
     */
    private fun parseAndValidateJson(response: String, expectedSchema: String): JsonObject? {
        try {
            val jsonText = extractJsonFromResponse(response)
            val parsed = JsonParser.parseString(jsonText)
            
            if (!parsed.isJsonObject) {
                logger.warn("Response is not a JSON object")
                return null
            }
            
            val jsonObject = parsed.asJsonObject
            
            if (jsonObject.size() == 0) {
                logger.warn("JSON object is empty")
                return null
            }
            
            return jsonObject
            
        } catch (e: JsonSyntaxException) {
            logger.warn("Failed to parse JSON: {}", e.message)
            return null
        } catch (e: Exception) {
            logger.warn("Unexpected error parsing JSON", e)
            return null
        }
    }
    
    /**
     * Extracts JSON from various response formats (with or without markdown code blocks).
     */
    private fun extractJsonFromResponse(response: String): String {
        val trimmed = response.trim()
        
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(trimmed)
        
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // If no code block, try to find JSON object boundaries
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1)
        }
        
        // Return as-is if no pattern matches
        return trimmed
    }
}
