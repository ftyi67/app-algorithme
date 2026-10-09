package com.example.model

enum class ProgrammingLanguage(
    val id: String,
    val displayName: String,
    val badge: String,
    val pistonLanguage: String,
    val pistonVersion: String,
    val fileExtension: String,
    val quickTokens: List<String>,
    val defaultTemplate: String
) {
    ALGO(
        id = "ALGO",
        displayName = "Algorithme",
        badge = "ALGO",
        pistonLanguage = "algo",
        pistonVersion = "1.0",
        fileExtension = ".algo",
        quickTokens = listOf(
            "Algorithme",
            "Variables",
            "Debut",
            "Fin",
            "Ecrire(",
            "Lire(",
            "<-",
            "Si",
            "Alors",
            "Sinon",
            "FinSi",
            "Pour",
            "FinPour",
            "TantQue",
            "FinTantQue"
        ),
        defaultTemplate = """Algorithme CalculSomme
Variables
    n, i, somme : Entier
Debut
    Ecrire("Donner un nombre N :")
    Lire(n)
    somme ← 0
    Pour i ← 1 a n Faire
        somme ← somme + i
    FinPour
    Ecrire("La somme de 1 a ", n, " est : ", somme)
Fin"""
    );

    companion object {
        fun fromId(id: String): ProgrammingLanguage = ALGO
    }
}

data class VariableSnap(
    val name: String,
    val type: String,
    val value: String
)

data class ExecutionResult(
    val language: ProgrammingLanguage = ProgrammingLanguage.ALGO,
    val stdout: String,
    val stderr: String = "",
    val exitCode: Int = 0,
    val executionTimeMs: Long = 0L,
    val engineUsed: String = "Moteur Algorithme",
    val variables: List<VariableSnap> = emptyList(),
    val htmlPreviewContent: String? = null
)

enum class SamplePaperType(
    val title: String,
    val subtitle: String,
    val targetLanguage: ProgrammingLanguage = ProgrammingLanguage.ALGO,
    val description: String
) {
    ALGO_FACTORIAL(
        title = "Exercice 1 : Factorielle & Pair/Impair",
        subtitle = "Algorithme avec Pour et Si/Sinon",
        description = "Algorithme écrit sur feuille calculant la factorielle d'un entier N et testant s'il est pair ou impair."
    ),
    ALGO_SOMME_MOYENNE(
        title = "Exercice 2 : Moyenne de 2 Notes",
        subtitle = "Algorithme simple avec Lire et Si",
        description = "Algorithme qui lit 2 notes, calcule la moyenne et affiche Admis ou Redoublant."
    ),
    ALGO_TABLE_MULT(
        title = "Exercice 3 : Table de Multiplication",
        subtitle = "Algorithme avec boucle Pour",
        description = "Algorithme qui affiche la table de multiplication d'un nombre N de 1 à 10."
    )
}

data class LessonTopic(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String = "Simple",
    val summary: String,
    val explanation: String,
    val codeByLanguage: Map<ProgrammingLanguage, String>,
    val defaultStdin: String = "5"
) {
    val algoCode: String
        get() = codeByLanguage[ProgrammingLanguage.ALGO] ?: ProgrammingLanguage.ALGO.defaultTemplate
}
