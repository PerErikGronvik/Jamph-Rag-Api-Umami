package no.jamph.llmValidation

import java.io.File

private val HEADER = "model,timestamp,sqlAccuracy,dialectAccuracy,tokensPerSecond,promptTokens,responseTokens,evalDurationMs"

class CsvBenchmarkWriter(private val filePath: String = "benchmark_results.csv") {

    fun appendRows(results: List<ModelBenchmarkResult>) {
        val file = File(filePath)
        if (!file.exists()) file.writeText(HEADER + "\n")

        results.forEach { r ->
            file.appendText(
                "${r.model},${r.timestamp},${"%.4f".format(r.sqlAccuracy)},${"%.4f".format(r.dialectAccuracy)},${"%.2f".format(r.tokensPerSecond)},${r.promptTokens},${r.responseTokens},${r.evalDurationMs}\n"
            )
            println("  ✓ Wrote ${r.model} to $filePath")
        }
    }
}
