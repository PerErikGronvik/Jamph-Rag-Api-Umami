 RAG v2 SQL Generation Pipeline

Architecture

Two-path system for SQL generation:
- Template-based (linear, rankings) → Fast, consistent, predictable
- Schema-based (default) → Flexible, full LLM generation

Pipeline Steps

Step 1: PickASqlQuestionTypeLlm
Classifies user question into query type:
- `linear` - Trend analysis with regression
- `rankings` - Top/bottom lists
- `default` - Everything else

Step 2: PickVariableJsonLlm (template types only)
Extracts variable values from natural language into JSON.
Skipped for `default` type.

Step 3: ConstructSQL (template types only)
Replaces [PLACEHOLDERS] in SQL templates with extracted values.
Skipped for `default` type.

Step 4: OtherLlm (default type only)
Full schema-based LLM SQL generation for queries that don't fit templates.

Query Types

1. rankings

Top/bottom lists with ORDER BY + LIMIT
Examples: Top pages, top sources, most visited OS/browser, most searched terms
Pattern: GROUP BY + ORDER BY DESC/ASC + LIMIT
How: Standard step 1.2.3.

2. linear - Schema

Regression analysis for trends
Examples: "trend i daglige sidevisninger"
How: Give variables put into formula.
How: Standard step 1.2(simplified, the model does not need to know about rmse etc).3.

3. funnel

Multi-step conversion paths
Examples: "fra start til fullført søknad", "fra forsiden til linkcardkomponentsiden"
Pattern: Multiple CTEs with step filtering, conversion rates
SQL: Window functions or step-by-step JOINs
How: 1.2.3 Code ?? node search?? first find shortest path between the sites. Then count for each step. fint more routes. etc??
Or respond, cannot do that right now, but look in grafbyggeren.

4. Search terms popularity
Finsished logic for finding search term. instert term. BAM. Done.

5. Referrers
Example "Hvor navigerer brukere etter å ha søkt" "hvor kommer brukere fra"
How: Not sure why the models cant handle it.

6. Actions on the page.
Example Hva gjør brukere på siden.
Finsished logic for listing actions. BAM. Done.

7. other/default

Everything else: Simple aggregations, time grouping, custom metrics
Examples: Daily/monthly counts, OS breakdown, 404 errors, search counts, time on page
Entire Schema available for any other type of query that doesn't fit the above categories.

Direct llm generation



Faster categorisation: (out of scope !!!)

The simple setup

Use Ollama only to generate embeddings.

Pipeline:

Define 8 categories
Write 10 to 30 example questions per category
Ask Ollama for embeddings for those examples once
Store the vectors in memory or a JSON file
For each new question, get its embedding from Ollama
Compute cosine similarity
Pick the best category

That is enough.

mxbai-embed-large
nomic-embed-text


Dependencies

Gradle Kotlin DSL:

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.3")
    implementation("org.json:json:20240303")
}
Files

Put these in src/main/resources/model/:

model.onnx
tokenizer.json
special_tokens_map.json
tokenizer_config.json
Minimal classifier structure
data class Example(val category: String, val text: String)
data class Encoded(val inputIds: LongArray, val attentionMask: LongArray)
1. Load ONNX model
import ai.onnxruntime.*

class Embedder(modelPath: String) {
    private val env = OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelPath, OrtSession.SessionOptions())

    fun embed(inputIds: LongArray, attentionMask: LongArray): FloatArray {
        val inputShape = longArrayOf(1, inputIds.size.toLong())

        OnnxTensor.createTensor(env, arrayOf(inputIds), inputShape).use { idsTensor ->
            OnnxTensor.createTensor(env, arrayOf(attentionMask), inputShape).use { maskTensor ->
                val inputs = mapOf(
                    "input_ids" to idsTensor,
                    "attention_mask" to maskTensor
                )
                session.run(inputs).use { results ->
                    val output = results[0].value as Array<Array<FloatArray>>
                    val tokenEmbeddings = output[0]
                    return meanPool(tokenEmbeddings, attentionMask)
                }
            }
        }
    }

    private fun meanPool(tokens: Array<FloatArray>, mask: LongArray): FloatArray {
        val dim = tokens[0].size
        val out = FloatArray(dim)
        var count = 0f

        for (i in tokens.indices) {
            if (mask[i] == 1L) {
                val t = tokens[i]
                for (j in 0 until dim) out[j] += t[j]
                count += 1f
            }
        }

        for (j in 0 until dim) out[j] /= count
        return normalize(out)
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0.0
        for (x in v) sum += x * x
        val norm = kotlin.math.sqrt(sum).toFloat().coerceAtLeast(1e-12f)
        for (i in v.indices) v[i] /= norm
        return v
    }
}
2. Tokenizer

Do not write your own BPE tokenizer from scratch.

Use a JVM tokenizer library that can load tokenizer.json, or export tokenization elsewhere and keep it fixed.

Expected API:

interface Tokenizer {
    fun encode(text: String, maxLength: Int = 128): Encoded
}

If you already have no tokenizer library ready, the fastest practical route is:

use a Hugging Face compatible Java tokenizer lib
or use DJL tokenizer only, while keeping ONNX for inference
3. Cosine similarity
fun cosine(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    var na = 0f
    var nb = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]
        na += a[i] * a[i]
        nb += b[i] * b[i]
    }
    return (dot / (kotlin.math.sqrt(na.toDouble()).toFloat() *
            kotlin.math.sqrt(nb.toDouble()).toFloat())).coerceIn(-1f, 1f)
}

If you normalize embeddings first, cosine becomes just dot product:

fun dot(a: FloatArray, b: FloatArray): Float {
    var s = 0f
    for (i in a.indices) s += a[i] * b[i]
    return s
}
4. Precompute category example embeddings
class SqlClassifier(
    private val tokenizer: Tokenizer,
    private val embedder: Embedder,
    examples: List<Example>
) {
    private val indexed: List<Pair<Example, FloatArray>> = examples.map { ex ->
        val enc = tokenizer.encode("passage: ${ex.text}")
        ex to embedder.embed(enc.inputIds, enc.attentionMask)
    }

    fun classify(query: String): Pair<String, Float> {
        val q = tokenizer.encode("query: $query")
        val qEmb = embedder.embed(q.inputIds, q.attentionMask)

        var bestCategory = ""
        var bestScore = -1f

        for ((ex, emb) in indexed) {
            val score = dot(qEmb, emb)
            if (score > bestScore) {
                bestScore = score
                bestCategory = ex.category
            }
        }

        return bestCategory to bestScore
    }
}
5. Example usage
fun main() {
    val tokenizer: Tokenizer = YourTokenizer("src/main/resources/model/tokenizer.json")
    val embedder = Embedder("src/main/resources/model/model.onnx")

    val examples = listOf(
        Example("linear", "Hvordan endrer sidevisninger seg over tid"),
        Example("linear", "Korleis endrar sidevisninga over tid"),
        Example("ranking", "Hvilke sider har flest visninger"),
        Example("ranking", "Kva sider har mest trafikk")
    )

    val classifier = SqlClassifier(tokenizer, embedder, examples)

    val result = classifier.classify("Korleis utviklar sidevisningane seg over tid")
    println(result)
}
Important

For E5-style models:

examples use passage: ...
query uses query: ...
Minimum viable approach
Get ONNX model
Get working tokenizer in JVM
Precompute 10 to 20 examples per category
Dot product against normalized embeddings
Return best category
Recommendation

Start with max similarity over examples, not centroid averaging. It is simpler and usually better for only 8 categories.