package com.example.engine

import android.graphics.Bitmap
import com.example.model.ProgrammingLanguage
import com.example.model.SamplePaperType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ScannedCodeResponse(
    val detectedLanguage: String = "ALGO",
    val extractedTitle: String = "Texte Scanné (Google OCR)",
    val extractedCode: String = "",
    val explanation: String = "",
    val suggestedStdin: String = "5"
)

object CodeExecutionEngine {

    // Instance unique réutilisée pour éviter toute allocation lourde lors du scan OCR
    private val sharedRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scanHandwrittenPaper(
        bitmap: Bitmap,
        preferredTargetLanguage: ProgrammingLanguage? = ProgrammingLanguage.ALGO,
        sampleHint: SamplePaperType?
    ): ScannedCodeResponse = withContext(Dispatchers.Default) {
        // Si l'utilisateur clique sur une feuille d'exemple intégrée, réponse instantanée sans latence
        if (sampleHint != null) {
            return@withContext SamplePaperGenerator.extractFromSampleOrTranslate(
                sampleType = sampleHint,
                targetLanguage = ProgrammingLanguage.ALGO
            )
        }

        // Pour toute vraie photo (Caméra ou Galerie), lecture exacte avec Google ML Kit OCR
        val mlKitText = recognizeTextWithGoogleMlKit(bitmap)
        if (mlKitText.isNotBlank()) {
            val cleanedText = normalizeOcrAlgoText(mlKitText)
            return@withContext ScannedCodeResponse(
                detectedLanguage = "ALGO",
                extractedTitle = "Lu par Google OCR",
                extractedCode = cleanedText,
                explanation = "Texte et nombres lus exactement depuis votre image avec Google ML Kit OCR.",
                suggestedStdin = "5"
            )
        }

        ScannedCodeResponse(
            detectedLanguage = "ALGO",
            extractedTitle = "Aucun texte détecté",
            extractedCode = "",
            explanation = "Aucun texte ou chiffre n'a été détecté sur cette photo. Rapprochez la caméra de la feuille avec un bon éclairage.",
            suggestedStdin = "5"
        )
    }

    private suspend fun recognizeTextWithGoogleMlKit(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            try {
                val safeBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }
                val image = InputImage.fromBitmap(safeBitmap, 0)

                sharedRecognizer.process(image)
                    .addOnSuccessListener { visionText: Text ->
                        val allLines = visionText.textBlocks
                            .flatMap { it.lines }
                            .sortedWith(
                                compareBy<Text.Line>(
                                    { it.boundingBox?.top ?: 0 },
                                    { it.boundingBox?.left ?: 0 }
                                )
                            )

                        if (allLines.isEmpty()) {
                            if (cont.isActive) cont.resume(visionText.text.trim())
                            return@addOnSuccessListener
                        }

                        val groupedRows = mutableListOf<MutableList<Text.Line>>()
                        for (line in allLines) {
                            val top = line.boundingBox?.top ?: 0
                            val matchingRow = groupedRows.lastOrNull { row ->
                                val rowTop = row.first().boundingBox?.top ?: 0
                                kotlin.math.abs(top - rowTop) <= 18
                            }
                            if (matchingRow != null) {
                                matchingRow.add(line)
                            } else {
                                groupedRows.add(mutableListOf(line))
                            }
                        }

                        val minLeft = allLines.mapNotNull { it.boundingBox?.left }.minOrNull() ?: 0
                        val reconstructed = groupedRows.joinToString("\n") { row ->
                            row.sortBy { it.boundingBox?.left ?: 0 }
                            val firstLeft = row.first().boundingBox?.left ?: minLeft
                            val indentSpaces = if (firstLeft - minLeft > 35) "    " else ""
                            indentSpaces + row.joinToString(" ") { it.text.trim() }
                        }.trim()

                        if (cont.isActive) {
                            cont.resume(reconstructed)
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume("")
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume("")
            }
        }

    private fun normalizeOcrAlgoText(rawText: String): String {
        return rawText.lines()
            .joinToString("\n") { line ->
                line
                    .replace("<-", "←")
                    .replace("«-", "←")
                    .replace("<—", "←")
                    .replace("<–", "←")
            }
            .trim()
    }
}
