package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.model.ProgrammingLanguage
import com.example.model.SamplePaperType
import java.util.concurrent.ConcurrentHashMap

object SamplePaperGenerator {

    private val bitmapCache = ConcurrentHashMap<SamplePaperType, Bitmap>()

    fun renderHandwrittenNotebookBitmap(sampleType: SamplePaperType): Bitmap {
        bitmapCache[sampleType]?.let { return it }

        val width = 480
        val height = 540
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.parseColor("#FAF8F2"))

        val gridPaint = Paint().apply {
            color = Color.parseColor("#D6E4F0")
            strokeWidth = 1.5f
        }
        var y = 36f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += 26f
        }

        val marginPaint = Paint().apply {
            color = Color.parseColor("#F08080")
            strokeWidth = 2f
        }
        canvas.drawLine(50f, 0f, 50f, height.toFloat(), marginPaint)

        val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 16f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B91C1C")
            textSize = 17f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }

        val lines = getHandwrittenLines(sampleType)
        var lineY = 58f
        lines.forEachIndexed { idx, text ->
            val p = if (idx == 0) headerPaint else inkPaint
            canvas.drawText(text, 60f, lineY, p)
            lineY += 26f
        }

        bitmapCache[sampleType] = bitmap
        return bitmap
    }

    private fun getHandwrittenLines(sampleType: SamplePaperType): List<String> = when (sampleType) {
        SamplePaperType.ALGO_FACTORIAL -> listOf(
            "Feuille 1 : Algorithme Factorielle et Parite",
            "Algorithme FactorielleEtParite",
            "Variables",
            "    n, i, f : Entier",
            "Debut",
            "    Ecrire(\"Donner un nombre N :\")",
            "    Lire(n)",
            "    f ← 1",
            "    Pour i ← 1 a n Faire",
            "        f ← f * i",
            "    FinPour",
            "    Ecrire(\"Factorielle = \", f)",
            "    Si n % 2 = 0 Alors",
            "        Ecrire(n, \" est PAIR\")",
            "    Sinon",
            "        Ecrire(n, \" est IMPAIR\")",
            "    FinSi",
            "Fin"
        )

        SamplePaperType.ALGO_SOMME_MOYENNE -> listOf(
            "Feuille 2 : Moyenne de deux notes",
            "Algorithme CalculMoyenne",
            "Variables",
            "    n1, n2, moy : Reel",
            "Debut",
            "    Ecrire(\"Donner deux notes :\")",
            "    Lire(n1, n2)",
            "    moy ← (n1 + n2) / 2",
            "    Ecrire(\"Moyenne = \", moy)",
            "    Si moy >= 10 Alors",
            "        Ecrire(\"Resultat : ADMIS\")",
            "    Sinon",
            "        Ecrire(\"Resultat : REDOUBLANT\")",
            "    FinSi",
            "Fin"
        )

        SamplePaperType.ALGO_TABLE_MULT -> listOf(
            "Feuille 3 : Table de multiplication",
            "Algorithme TableMultiplication",
            "Variables",
            "    n, i, p : Entier",
            "Debut",
            "    Ecrire(\"Donner un nombre :\")",
            "    Lire(n)",
            "    Pour i ← 1 a 10 Faire",
            "        p ← n * i",
            "        Ecrire(n, \" x \", i, \" = \", p)",
            "    FinPour",
            "Fin"
        )
    }

    fun extractFromSampleOrTranslate(
        sampleType: SamplePaperType,
        targetLanguage: ProgrammingLanguage? = null
    ): ScannedCodeResponse {
        val rawLines = getHandwrittenLines(sampleType).drop(1)
        val baseCode = rawLines.joinToString("\n")
        val suggestedInput = when (sampleType) {
            SamplePaperType.ALGO_FACTORIAL -> "5"
            SamplePaperType.ALGO_SOMME_MOYENNE -> "14\n12"
            SamplePaperType.ALGO_TABLE_MULT -> "7"
        }

        return ScannedCodeResponse(
            detectedLanguage = "ALGO",
            extractedTitle = sampleType.title,
            extractedCode = baseCode,
            explanation = sampleType.description,
            suggestedStdin = suggestedInput
        )
    }
}
