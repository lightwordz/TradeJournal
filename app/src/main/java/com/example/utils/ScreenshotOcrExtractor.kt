package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.engine.DefaultLotSizes
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs

object ScreenshotOcrExtractor {

    suspend fun scanAndParse(
        context: Context,
        uri: Uri,
        lotSizes: Map<String, Int> = DefaultLotSizes.seeded()
    ): ScreenshotTradeDraft = withContext(Dispatchers.IO) {
        val savedPath = ScreenshotStorage.saveFromUri(context, uri)
        val inputImage = runCatching {
            if (savedPath != null) {
                InputImage.fromFilePath(context, Uri.fromFile(File(savedPath)))
            } else {
                InputImage.fromFilePath(context, uri)
            }
        }.getOrNull() ?: return@withContext ScreenshotTradeDraft(screenshotPath = savedPath)

        val visionText = recognizeText(inputImage)
        val orderedText = if (visionText != null) {
            reconstructVisualRows(visionText).ifBlank { visionText.text }
        } else {
            ""
        }

        TradeScreenshotParser.parse(
            rawText = orderedText,
            lotSizes = lotSizes,
            screenshotPath = savedPath
        )
    }

    private suspend fun recognizeText(image: InputImage): Text? =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

    /**
     * Broker cards often render labels on the left and values on the right inside separate ML Kit blocks.
     * Group lines with similar vertical center coordinates onto the same text row (left-to-right) so
     * "Buy avg." + "142.50" reconstruct cleanly as "Buy avg. 142.50".
     */
    private fun reconstructVisualRows(visionText: Text): String {
        val lines = visionText.textBlocks
            .flatMap { it.lines }
            .filter { it.text.isNotBlank() }

        if (lines.isEmpty()) return visionText.text

        val sorted = lines.sortedWith(
            compareBy<Text.Line>(
                { it.boundingBox?.centerY() ?: 0 },
                { it.boundingBox?.left ?: 0 }
            )
        )

        val rows = mutableListOf<MutableList<Text.Line>>()
        for (line in sorted) {
            val box = line.boundingBox
            val cy = box?.centerY() ?: 0
            val heightTolerance = ((box?.height() ?: 28) * 0.65f).toInt().coerceAtLeast(14)
            val targetRow = rows.lastOrNull { row ->
                val ref = row.first().boundingBox?.centerY() ?: cy
                abs(cy - ref) <= heightTolerance
            }
            if (targetRow != null) {
                targetRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.boundingBox?.left ?: 0 }
                .joinToString(" ") { it.text.trim() }
        }
    }
}
