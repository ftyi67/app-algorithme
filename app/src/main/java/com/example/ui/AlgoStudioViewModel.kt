package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CodeExecutionEngine
import com.example.engine.SamplePaperGenerator
import com.example.engine.ScannedCodeResponse
import com.example.model.ExecutionResult
import com.example.model.ProgrammingLanguage
import com.example.model.SamplePaperType
import com.example.model.VariableSnap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioTab(val routeId: String, val label: String) {
    IDE("ide_workspace", "Algorithme"),
    SCANNER("paper_scanner", "Scanner Feuille")
}

data class AlgoStudioUiState(
    val currentTab: StudioTab = StudioTab.IDE,
    val selectedLanguage: ProgrammingLanguage = ProgrammingLanguage.ALGO,
    val editorTitle: String = "CalculSomme",
    val codeText: String = ProgrammingLanguage.ALGO.defaultTemplate,
    val providedInputs: List<String> = emptyList(),
    val waitingForVariable: String? = null,
    val waitingForVariableType: String = "Entier",
    val isExecuting: Boolean = false,
    val executionOutputText: String = "",
    val executionResult: ExecutionResult? = null,
    val statusBannerMessage: String? = null,
    val isScanningPaper: Boolean = false,
    val scannedBitmap: Bitmap? = null,
    val activeSamplePaper: SamplePaperType = SamplePaperType.ALGO_FACTORIAL,
    val lastScanResponse: ScannedCodeResponse? = null
)

private data class IndexedLine(
    val lineNumber: Int,
    val text: String
)

private data class VirtualExecutionOutcome(
    val stdout: String,
    val stderr: String,
    val variables: List<VariableSnap>,
    val waitingVarName: String? = null,
    val waitingVarType: String = "Entier"
)

class AlgoStudioViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val SI_PREFIX_REGEX = Regex("^(?i)(si|if)\\s*")
        private val ALORS_SUFFIX_REGEX = Regex("(?i)\\s*(alors|then)\\s*$")
        private val POUR_MATCH_REGEX = Regex("(?i)^(?:pour|for)\\s+([a-zA-Z0-9_]+)\\s*(?:<-|←|⟵|:=|=|de)\\s*(.+?)\\s+(?:a|à|to)\\s+(.+?)(?:\\s+faire|\\s+do)?$")
        private val TANTQUE_PREFIX_REGEX = Regex("^(?i)(tant\\s*que|while)\\s+")
        private val FAIRE_SUFFIX_REGEX = Regex("(?i)\\s*(faire|do)\\s*$")
        private val VALID_VAR_NAME_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        private val IO_COMMAND_PREFIX_REGEX = Regex("^(?i)(ecrire|écrire|afficher|print|lire|saisir|read)\\s*")
    }

    private val _uiState = MutableStateFlow(AlgoStudioUiState())
    val uiState: StateFlow<AlgoStudioUiState> = _uiState.asStateFlow()

    init {
        val initialScan = SamplePaperGenerator.extractFromSampleOrTranslate(
            SamplePaperType.ALGO_FACTORIAL
        )
        _uiState.update {
            it.copy(lastScanResponse = initialScan)
        }
    }

    /**
     * Exécute l'algorithme pas à pas :
     * - Affiche les Ecrire(...) au fur et à mesure
     * - Si un Lire(x) est rencontré et que l'utilisateur n'a pas encore saisi la valeur,
     *   l'exécution se met en pause exactement sur ce Lire(x) et demande à l'utilisateur d'entrer la valeur dans la console !
     */
    private fun executePseudocodeInteractive(
        pseudocode: String,
        suppliedInputs: List<String>
    ): VirtualExecutionOutcome {
        val outputLines = mutableListOf<String>()
        val errorLines = mutableListOf<String>()
        val variables = linkedMapOf<String, Any>()
        val variableTypes = linkedMapOf<String, String>()

        val rawLinesWithNumbers = pseudocode.lines().mapIndexedNotNull { idx, raw ->
            val commentIdx = raw.indexOf("//")
            val cleaned = if (commentIdx >= 0) raw.substring(0, commentIdx).trim() else raw.trim()
            if (cleaned.isNotEmpty()) IndexedLine(lineNumber = idx + 1, text = cleaned) else null
        }

        if (rawLinesWithNumbers.isEmpty()) {
            return VirtualExecutionOutcome(
                stdout = "",
                stderr = "❌ Ligne 1 : L'algorithme est vide. Écrivez votre algorithme avant d'exécuter.",
                variables = emptyList()
            )
        }

        // 1. Validation de la structure globale (Debut / Fin)
        val hasDebut = rawLinesWithNumbers.any {
            it.text.equals("Debut", ignoreCase = true) ||
                it.text.equals("Début", ignoreCase = true) ||
                it.text.equals("Begin", ignoreCase = true)
        }
        val hasFin = rawLinesWithNumbers.any {
            it.text.equals("Fin", ignoreCase = true) ||
                it.text.equals("End", ignoreCase = true) ||
                it.text.equals("FinAlgorithme", ignoreCase = true)
        }

        if (!hasDebut) {
            errorLines.add("❌ Erreur de structure : Il manque le mot-clé 'Debut' pour commencer l'algorithme.")
        }
        if (!hasFin) {
            val lastLineNum = rawLinesWithNumbers.last().lineNumber
            errorLines.add("❌ Ligne $lastLineNum : Il manque le mot-clé 'Fin' à la fin de l'algorithme.")
        }

        val openSiStack = mutableListOf<Int>()
        val openPourStack = mutableListOf<Int>()
        val openTantQueStack = mutableListOf<Int>()

        var inVars = false
        var inBody = !hasDebut
        val bodyStatements = mutableListOf<IndexedLine>()

        for (item in rawLinesWithNumbers) {
            val lineNum = item.lineNumber
            val line = item.text
            val lower = line.lowercase()

            val quoteCount = line.count { it == '"' }
            if (quoteCount % 2 != 0) {
                errorLines.add("❌ Ligne $lineNum : Guillemet (\") manquant ou non fermé dans : $line")
            }

            val openParens = line.count { it == '(' }
            val closeParens = line.count { it == ')' }
            if (openParens != closeParens) {
                errorLines.add("❌ Ligne $lineNum : Parenthèse '(' ou ')' manquante dans : $line")
            }

            when {
                lower.startsWith("algorithme") || lower.startsWith("algorithm") -> {
                    val parts = line.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
                    if (parts.size < 2) {
                        errorLines.add("❌ Ligne $lineNum : Vous devez donner un nom après 'Algorithme' (ex: Algorithme MonCalcul).")
                    }
                }
                lower == "variables" || lower == "variable" || lower == "var" -> {
                    inVars = true
                    inBody = false
                }
                lower == "debut" || lower == "début" || lower == "begin" -> {
                    inVars = false
                    inBody = true
                }
                lower == "fin" || lower == "end" || lower == "finalgorithme" -> {
                    inBody = false
                }
                inVars -> {
                    if (!line.contains(":")) {
                        errorLines.add("❌ Ligne $lineNum : Déclaration de variable incorrecte. Utilisez ':' et le type (ex: n : Entier) dans : $line")
                    } else {
                        val parts = line.split(":")
                        val varNames = parts[0].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val typeName = parts.getOrNull(1)?.trim().orEmpty()
                        if (varNames.isEmpty() || typeName.isEmpty()) {
                            errorLines.add("❌ Ligne $lineNum : Précisez le nom et le type de la variable (Entier, Reel, Chaine, Booleen).")
                        } else {
                            val allowedTypes = setOf("entier", "reel", "réel", "chaine", "chaîne", "booleen", "booléen", "caractere", "int", "float", "string", "boolean")
                            if (typeName.lowercase() !in allowedTypes) {
                                errorLines.add("❌ Ligne $lineNum : Type '$typeName' inconnu. Utilisez : Entier, Reel, Chaine ou Booleen.")
                            }
                            parseVarDeclaration(line, variables, variableTypes)
                        }
                    }
                }
                inBody -> {
                    bodyStatements.add(item)
                    validateBodyStatementSyntax(
                        item = item,
                        declaredVars = variables.keys,
                        hasVariablesBlock = variables.isNotEmpty(),
                        openSiStack = openSiStack,
                        openPourStack = openPourStack,
                        openTantQueStack = openTantQueStack,
                        errorLines = errorLines
                    )
                }
                else -> {
                    errorLines.add("❌ Ligne $lineNum : Instruction placée en dehors de 'Variables' ou 'Debut' : $line")
                }
            }
        }

        for (siLine in openSiStack) {
            errorLines.add("❌ Ligne $siLine : Le bloc 'Si' ouvert à la ligne $siLine n'est pas fermé par 'FinSi'.")
        }
        for (pourLine in openPourStack) {
            errorLines.add("❌ Ligne $pourLine : La boucle 'Pour' ouverte à la ligne $pourLine n'est pas fermée par 'FinPour'.")
        }
        for (tqLine in openTantQueStack) {
            errorLines.add("❌ Ligne $tqLine : La boucle 'TantQue' ouverte à la ligne $tqLine n'est pas fermée par 'FinTantQue'.")
        }

        if (errorLines.isNotEmpty()) {
            return VirtualExecutionOutcome(
                stdout = "",
                stderr = errorLines.joinToString("\n\n"),
                variables = emptyList()
            )
        }

        val inputQueue = suppliedInputs.toMutableList()
        var waitingVar: String? = null
        var waitingType = "Entier"
        var stepCounter = 0
        val maxSteps = 2000

        fun executeBlock(statements: List<IndexedLine>) {
            var i = 0
            while (i < statements.size) {
                if (errorLines.isNotEmpty() || waitingVar != null) return
                stepCounter++
                if (stepCounter > maxSteps) {
                    errorLines.add("❌ Ligne ${statements[i].lineNumber} : Boucle infinie détectée (plus de $maxSteps répétitions).")
                    return
                }

                val current = statements[i]
                val lineNum = current.lineNumber
                val stmt = current.text.trim()
                val lower = stmt.lowercase()

                when {
                    // Ecrire(...)
                    lower.startsWith("ecrire") || lower.startsWith("écrire") ||
                        lower.startsWith("afficher") || lower.startsWith("print") -> {
                        val content = extractInsideParentheses(stmt)
                        val parts = splitByTopLevelComma(content)
                        val printed = parts.joinToString("") { part ->
                            formatAlgoValue(evaluateSimpleExpr(part.trim(), variables, lineNum, errorLines))
                        }
                        if (errorLines.isEmpty()) {
                            outputLines.add(printed)
                        }
                        i++
                    }

                    // Lire(...) interactif : si aucune valeur n'est disponible, on met en pause et on demande à l'utilisateur !
                    lower.startsWith("lire") || lower.startsWith("saisir") || lower.startsWith("read") -> {
                        val content = extractInsideParentheses(stmt)
                        val varNames = splitByTopLevelComma(content).map { it.trim() }.filter { it.isNotEmpty() }
                        for (vName in varNames) {
                            val expectedType = variableTypes.entries
                                .find { it.key.equals(vName, ignoreCase = true) }?.value ?: "Entier"

                            if (inputQueue.isEmpty()) {
                                // Pause ici ! On attend que l'utilisateur entre la valeur de vName
                                waitingVar = vName
                                waitingType = expectedType
                                return
                            }

                            val rawToken = inputQueue.removeAt(0).trim()
                            outputLines.add("> $vName ← $rawToken")

                            val typeLower = expectedType.lowercase()
                            if (typeLower in listOf("entier", "integer", "int")) {
                                val parsedLong = rawToken.toLongOrNull()
                                if (parsedLong == null) {
                                    errorLines.add("❌ Ligne $lineNum : Valeur '$rawToken' invalide pour '$vName' (attendu : un nombre Entier).")
                                    return
                                }
                                variables[vName] = parsedLong
                            } else if (typeLower in listOf("reel", "réel", "real", "float", "double")) {
                                val parsedDouble = rawToken.toDoubleOrNull()
                                if (parsedDouble == null) {
                                    errorLines.add("❌ Ligne $lineNum : Valeur '$rawToken' invalide pour '$vName' (attendu : un nombre Réel).")
                                    return
                                }
                                variables[vName] = parsedDouble
                            } else {
                                val numVal = rawToken.toDoubleOrNull()
                                val stored: Any = when {
                                    numVal != null && numVal % 1.0 == 0.0 -> numVal.toLong()
                                    numVal != null -> numVal
                                    else -> rawToken
                                }
                                variables[vName] = stored
                            }

                            if (!variableTypes.containsKey(vName)) {
                                variableTypes[vName] = expectedType
                            }
                        }
                        i++
                    }

                    // Si <condition> Alors ... [Sinon ...] FinSi
                    lower.startsWith("si ") || lower.startsWith("si(") || lower.startsWith("if ") -> {
                        val endIfIdx = findMatchingEnd(
                            statements, i,
                            openPrefixes = listOf("si ", "si(", "if "),
                            closePrefixes = listOf("finsi", "fin si", "endif")
                        )
                        val conditionStr = stmt
                            .replace(Regex("^(?i)(si|if)\\s*"), "")
                            .replace(Regex("(?i)\\s*(alors|then)\\s*$"), "")
                            .trim()

                        val thenBlock = mutableListOf<IndexedLine>()
                        val elseBlock = mutableListOf<IndexedLine>()
                        var inElse = false
                        var nestedDepth = 0

                        for (k in (i + 1) until endIfIdx) {
                            val sub = statements[k]
                            val subLower = sub.text.lowercase()
                            if (subLower.startsWith("si ") || subLower.startsWith("si(") || subLower.startsWith("if ")) {
                                nestedDepth++
                            } else if (subLower.startsWith("finsi") || subLower.startsWith("fin si") || subLower.startsWith("endif")) {
                                nestedDepth--
                            }

                            if (nestedDepth == 0 && (subLower == "sinon" || subLower == "else")) {
                                inElse = true
                            } else if (!inElse) {
                                thenBlock.add(sub)
                            } else {
                                elseBlock.add(sub)
                            }
                        }

                        if (evaluateCondition(conditionStr, variables, lineNum, errorLines)) {
                            executeBlock(thenBlock)
                        } else {
                            executeBlock(elseBlock)
                        }
                        i = endIfIdx + 1
                    }

                    // Pour i <- debut a fin Faire ... FinPour
                    lower.startsWith("pour ") || lower.startsWith("for ") -> {
                        val endForIdx = findMatchingEnd(
                            statements, i,
                            openPrefixes = listOf("pour ", "for "),
                            closePrefixes = listOf("finpour", "fin pour", "endfor")
                        )
                        val loopBody = statements.subList(i + 1, endForIdx)
                        val match = Regex("(?i)^(?:pour|for)\\s+([a-zA-Z0-9_]+)\\s*(?:<-|←|⟵|:=|=|de)\\s*(.+?)\\s+(?:a|à|to)\\s+(.+?)(?:\\s+faire|\\s+do)?$").find(stmt)
                        if (match != null) {
                            val loopVar = match.groupValues[1].trim()
                            val startVal = toNumber(evaluateSimpleExpr(match.groupValues[2], variables, lineNum, errorLines)).toLong()
                            val endVal = toNumber(evaluateSimpleExpr(match.groupValues[3], variables, lineNum, errorLines)).toLong()
                            variableTypes[loopVar] = "Entier"
                            for (counter in startVal..endVal) {
                                if (stepCounter > maxSteps || errorLines.isNotEmpty() || waitingVar != null) break
                                variables[loopVar] = counter
                                executeBlock(loopBody)
                            }
                        }
                        i = endForIdx + 1
                    }

                    // TantQue <condition> Faire ... FinTantQue
                    lower.startsWith("tantque ") || lower.startsWith("tant que ") || lower.startsWith("while ") -> {
                        val endWhileIdx = findMatchingEnd(
                            statements, i,
                            openPrefixes = listOf("tantque ", "tant que ", "while "),
                            closePrefixes = listOf("fintantque", "fin tantque", "fin tant que", "endwhile")
                        )
                        val condStr = stmt
                            .replace(Regex("^(?i)(tant\\s*que|while)\\s+"), "")
                            .replace(Regex("(?i)\\s*(faire|do)\\s*$"), "")
                            .trim()
                        val loopBody = statements.subList(i + 1, endWhileIdx)
                        var guard = 0
                        while (evaluateCondition(condStr, variables, lineNum, errorLines) && guard < 500 && stepCounter <= maxSteps && errorLines.isEmpty() && waitingVar == null) {
                            executeBlock(loopBody)
                            guard++
                        }
                        i = endWhileIdx + 1
                    }

                    // Affectation : x <- expr ou x ← expr ou x := expr ou x = expr
                    stmt.contains("<-") || stmt.contains("←") || stmt.contains("⟵") || stmt.contains(":=") || (stmt.contains("=") && !stmt.contains("==")) -> {
                        val parts = when {
                            stmt.contains("<-") -> stmt.split("<-", limit = 2)
                            stmt.contains("←") -> stmt.split("←", limit = 2)
                            stmt.contains("⟵") -> stmt.split("⟵", limit = 2)
                            stmt.contains(":=") -> stmt.split(":=", limit = 2)
                            else -> stmt.split("=", limit = 2)
                        }
                        if (parts.size == 2) {
                            val name = parts[0].trim()
                            val expr = parts[1].trim()
                            val evaluated = evaluateSimpleExpr(expr, variables, lineNum, errorLines)
                            variables[name] = evaluated
                            if (!variableTypes.containsKey(name)) {
                                variableTypes[name] = if (evaluated is Number) "Entier" else "Chaîne"
                            }
                        }
                        i++
                    }

                    else -> i++
                }
            }
        }

        try {
            executeBlock(bodyStatements)
        } catch (e: Exception) {
            errorLines.add("❌ Erreur d'exécution : ${e.message ?: "Instruction invalide"}")
        }

        val snaps = variables.map { (name, value) ->
            VariableSnap(
                name = name,
                type = variableTypes[name] ?: "Variable",
                value = formatAlgoValue(value)
            )
        }

        val finalStdout = if (outputLines.isEmpty() && errorLines.isEmpty() && waitingVar == null) {
            "Exécution réussie (aucun affichage Ecrire)."
        } else {
            outputLines.joinToString("\n")
        }

        return VirtualExecutionOutcome(
            stdout = finalStdout,
            stderr = errorLines.joinToString("\n\n"),
            variables = snaps,
            waitingVarName = waitingVar,
            waitingVarType = waitingType
        )
    }

    private fun validateBodyStatementSyntax(
        item: IndexedLine,
        declaredVars: Set<String>,
        hasVariablesBlock: Boolean,
        openSiStack: MutableList<Int>,
        openPourStack: MutableList<Int>,
        openTantQueStack: MutableList<Int>,
        errorLines: MutableList<String>
    ) {
        val lineNum = item.lineNumber
        val stmt = item.text.trim()
        val lower = stmt.lowercase()

        when {
            lower.startsWith("ecrire") || lower.startsWith("écrire") || lower.startsWith("afficher") || lower.startsWith("print") -> {
                if (!stmt.contains("(") || !stmt.endsWith(")")) {
                    errorLines.add("❌ Ligne $lineNum : 'Ecrire' doit avoir des parenthèses : Ecrire(...) dans : $stmt")
                } else {
                    val inside = extractInsideParentheses(stmt).trim()
                    if (inside.isEmpty()) {
                        errorLines.add("❌ Ligne $lineNum : 'Ecrire()' est vide à la ligne $lineNum.")
                    }
                }
            }

            lower.startsWith("lire") || lower.startsWith("saisir") || lower.startsWith("read") -> {
                if (!stmt.contains("(") || !stmt.endsWith(")")) {
                    errorLines.add("❌ Ligne $lineNum : 'Lire' doit avoir des parenthèses : Lire(variable) dans : $stmt")
                } else {
                    val inside = extractInsideParentheses(stmt).trim()
                    if (inside.isEmpty()) {
                        errorLines.add("❌ Ligne $lineNum : Précisez le nom de la variable dans Lire(...) à la ligne $lineNum.")
                    } else if (hasVariablesBlock) {
                        val varsToRead = splitByTopLevelComma(inside).map { it.trim() }
                        for (v in varsToRead) {
                            if (declaredVars.none { it.equals(v, ignoreCase = true) }) {
                                errorLines.add("❌ Ligne $lineNum : La variable '$v' dans Lire($v) n'est pas déclarée dans 'Variables'.")
                            }
                        }
                    }
                }
            }

            lower.startsWith("si ") || lower.startsWith("si(") || lower == "si" || lower.startsWith("if ") -> {
                openSiStack.add(lineNum)
                if (!lower.endsWith("alors") && !lower.endsWith("then")) {
                    errorLines.add("❌ Ligne $lineNum : Il manque le mot-clé 'Alors' à la fin de la condition 'Si' : $stmt")
                }
                val cond = stmt.replace(Regex("^(?i)(si|if)\\s*"), "").replace(Regex("(?i)\\s*(alors|then)\\s*$"), "").trim()
                if (cond.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : La condition après 'Si' est vide.")
                }
            }

            lower == "sinon" || lower == "else" || lower.startsWith("sinonsi ") || lower.startsWith("sinon si ") -> {
                if (openSiStack.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : '$stmt' trouvé sans bloc 'Si' correspondant avant.")
                }
            }

            lower == "finsi" || lower == "fin si" || lower == "endif" -> {
                if (openSiStack.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : 'FinSi' en trop (aucun 'Si' ouvert).")
                } else {
                    openSiStack.removeAt(openSiStack.lastIndex)
                }
            }

            lower.startsWith("pour ") || lower == "pour" || lower.startsWith("for ") -> {
                openPourStack.add(lineNum)
                if (!lower.endsWith("faire") && !lower.endsWith("do")) {
                    errorLines.add("❌ Ligne $lineNum : Il manque 'Faire' à la fin de la ligne 'Pour' : $stmt")
                }
                val match = Regex("(?i)^(?:pour|for)\\s+([a-zA-Z0-9_]+)\\s*(?:<-|←|⟵|:=|=|de)\\s*(.+?)\\s+(?:a|à|to)\\s+(.+?)(?:\\s+faire|\\s+do)?$").find(stmt)
                if (match == null) {
                    errorLines.add("❌ Ligne $lineNum : Syntaxe 'Pour' incorrecte. Exemple : Pour i ← 1 a n Faire")
                }
            }

            lower == "finpour" || lower == "fin pour" || lower == "endfor" -> {
                if (openPourStack.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : 'FinPour' en trop (aucune boucle 'Pour' ouverte).")
                } else {
                    openPourStack.removeAt(openPourStack.lastIndex)
                }
            }

            lower.startsWith("tantque ") || lower.startsWith("tant que ") || lower == "tantque" || lower.startsWith("while ") -> {
                openTantQueStack.add(lineNum)
                if (!lower.endsWith("faire") && !lower.endsWith("do")) {
                    errorLines.add("❌ Ligne $lineNum : Il manque 'Faire' à la fin de 'TantQue' : $stmt")
                }
            }

            lower == "fintantque" || lower == "fin tantque" || lower == "fin tant que" || lower == "endwhile" -> {
                if (openTantQueStack.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : 'FinTantQue' en trop (aucune boucle 'TantQue' ouverte).")
                } else {
                    openTantQueStack.removeAt(openTantQueStack.lastIndex)
                }
            }

            stmt.contains("<-") || stmt.contains("←") || stmt.contains("⟵") || stmt.contains(":=") || (stmt.contains("=") && !stmt.contains("==")) -> {
                val parts = when {
                    stmt.contains("<-") -> stmt.split("<-", limit = 2)
                    stmt.contains("←") -> stmt.split("←", limit = 2)
                    stmt.contains("⟵") -> stmt.split("⟵", limit = 2)
                    stmt.contains(":=") -> stmt.split(":=", limit = 2)
                    else -> stmt.split("=", limit = 2)
                }
                val leftVar = parts.getOrNull(0)?.trim().orEmpty()
                val rightExpr = parts.getOrNull(1)?.trim().orEmpty()
                if (leftVar.isEmpty() || !leftVar.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                    errorLines.add("❌ Ligne $lineNum : Nom de variable invalide à gauche de l'affectation '←' : '$leftVar'")
                } else if (hasVariablesBlock && declaredVars.none { it.equals(leftVar, ignoreCase = true) }) {
                    errorLines.add("❌ Ligne $lineNum : La variable '$leftVar' n'est pas déclarée dans la section 'Variables'.")
                }
                if (rightExpr.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : Il manque la valeur ou le calcul après la flèche d'affectation '←' dans : $stmt")
                }
            }

            else -> {
                errorLines.add("❌ Ligne $lineNum : Instruction inconnue ou mal écrite : '$stmt'. Vérifiez l'orthographe (Ecrire, Lire, ←, Si, Pour, TantQue).")
            }
        }
    }

    private fun parseVarDeclaration(
        line: String,
        variables: MutableMap<String, Any>,
        variableTypes: MutableMap<String, String>
    ) {
        val parts = line.split(":")
        if (parts.size == 2) {
            val names = parts[0].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val typeName = parts[1].trim()
            val defaultVal: Any = when (typeName.lowercase()) {
                "entier", "integer", "int" -> 0L
                "reel", "réel", "real", "float", "double" -> 0.0
                "booleen", "booléen", "boolean" -> false
                else -> ""
            }
            for (n in names) {
                variables[n] = defaultVal
                variableTypes[n] = typeName
            }
        }
    }

    private fun findMatchingEnd(
        lines: List<IndexedLine>,
        startIndex: Int,
        openPrefixes: List<String>,
        closePrefixes: List<String>
    ): Int {
        var depth = 0
        for (idx in startIndex until lines.size) {
            val lower = lines[idx].text.trim().lowercase()
            if (openPrefixes.any { lower.startsWith(it) }) {
                depth++
            } else if (closePrefixes.any { lower.startsWith(it) }) {
                depth--
                if (depth == 0) return idx
            }
        }
        return lines.lastIndex
    }

    private fun extractInsideParentheses(stmt: String): String {
        val open = stmt.indexOf('(')
        val close = stmt.lastIndexOf(')')
        return if (open >= 0 && close > open) {
            stmt.substring(open + 1, close)
        } else {
            stmt.replace(Regex("^(?i)(ecrire|écrire|afficher|print|lire|saisir|read)\\s*"), "")
        }
    }

    private fun splitByTopLevelComma(text: String): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in text) {
            if (c == '"') {
                inQuotes = !inQuotes
                sb.append(c)
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) list.add(sb.toString())
        return list
    }

    private fun evaluateCondition(
        condRaw: String,
        vars: Map<String, Any>,
        lineNum: Int,
        errorLines: MutableList<String>
    ): Boolean {
        val cond = condRaw.trim()
        val ops = listOf("<=", ">=", "<>", "!=", "==", "=", "<", ">")
        for (op in ops) {
            val idx = cond.indexOf(op)
            if (idx > 0) {
                if (op == "=" && (cond[idx - 1] in listOf('<', '>', '!', '=') || (idx + 1 < cond.length && cond[idx + 1] == '='))) {
                    continue
                }
                val left = evaluateSimpleExpr(cond.substring(0, idx), vars, lineNum, errorLines)
                val right = evaluateSimpleExpr(cond.substring(idx + op.length), vars, lineNum, errorLines)
                val lNum = toNumber(left)
                val rNum = toNumber(right)
                return when (op) {
                    "=", "==" -> lNum == rNum
                    "<>", "!=" -> lNum != rNum
                    "<" -> lNum < rNum
                    ">" -> lNum > rNum
                    "<=" -> lNum <= rNum
                    ">=" -> lNum >= rNum
                    else -> false
                }
            }
        }
        errorLines.add("❌ Ligne $lineNum : Condition invalide '$cond'. Utilisez un opérateur (=, <>, <, >, <=, >=).")
        return false
    }

    private fun evaluateSimpleExpr(
        exprRaw: String,
        vars: Map<String, Any>,
        lineNum: Int,
        errorLines: MutableList<String>
    ): Any {
        val expr = exprRaw.trim()
        if (expr.isEmpty()) return ""

        if (expr.startsWith("\"") && expr.endsWith("\"") && expr.length >= 2) {
            return expr.substring(1, expr.length - 1)
        }
        if (expr.startsWith("(") && expr.endsWith(")") && expr.length >= 2) {
            val inner = expr.substring(1, expr.length - 1)
            if (!inner.contains("(") && !inner.contains(")")) {
                return evaluateSimpleExpr(inner, vars, lineNum, errorLines)
            }
        }

        // Addition & Subtraction
        for (i in expr.indices.reversed()) {
            val c = expr[i]
            if ((c == '+' || c == '-') && i > 0) {
                val leftPart = expr.substring(0, i).trim()
                val rightPart = expr.substring(i + 1).trim()
                if (rightPart.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : Expression incomplète après '$c' dans '$expr'.")
                    return 0L
                }
                val left = evaluateSimpleExpr(leftPart, vars, lineNum, errorLines)
                val right = evaluateSimpleExpr(rightPart, vars, lineNum, errorLines)
                if (c == '+' && (left is String || right is String)) {
                    return formatAlgoValue(left) + formatAlgoValue(right)
                }
                val res = if (c == '+') toNumber(left) + toNumber(right) else toNumber(left) - toNumber(right)
                return if (res % 1.0 == 0.0) res.toLong() else res
            }
        }

        // Multiplication, Division, Modulo
        for (i in expr.indices.reversed()) {
            val c = expr[i]
            if ((c == '*' || c == '/' || c == '%') && i > 0) {
                val leftPart = expr.substring(0, i).trim()
                val rightPart = expr.substring(i + 1).trim()
                if (rightPart.isEmpty()) {
                    errorLines.add("❌ Ligne $lineNum : Expression incomplète après '$c' dans '$expr'.")
                    return 0L
                }
                val left = toNumber(evaluateSimpleExpr(leftPart, vars, lineNum, errorLines))
                val right = toNumber(evaluateSimpleExpr(rightPart, vars, lineNum, errorLines))
                if ((c == '/' || c == '%') && right == 0.0) {
                    errorLines.add("❌ Ligne $lineNum : Erreur mathématique — Division par zéro impossible ($expr).")
                    return 0L
                }
                val res = when (c) {
                    '*' -> left * right
                    '/' -> left / right
                    '%' -> (left.toLong() % right.toLong()).toDouble()
                    else -> 0.0
                }
                return if (res % 1.0 == 0.0) res.toLong() else res
            }
        }

        val foundVar = vars[expr] ?: vars.entries.find { it.key.equals(expr, ignoreCase = true) }?.value
        if (foundVar != null) return foundVar

        expr.toLongOrNull()?.let { return it }
        expr.toDoubleOrNull()?.let { return it }

        if (expr.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            errorLines.add("❌ Ligne $lineNum : La variable '$expr' n'existe pas ou n'a pas été déclarée.")
            return 0L
        }

        errorLines.add("❌ Ligne $lineNum : Expression ou valeur incorrecte : '$expr'.")
        return 0L
    }

    private fun toNumber(v: Any): Double = when (v) {
        is Number -> v.toDouble()
        is Boolean -> if (v) 1.0 else 0.0
        else -> v.toString().trim().toDoubleOrNull() ?: 0.0
    }

    private fun formatAlgoValue(v: Any): String = when (v) {
        is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab, statusBannerMessage = null) }
        if (tab == StudioTab.SCANNER && _uiState.value.scannedBitmap == null) {
            viewModelScope.launch(Dispatchers.Default) {
                val bmp = SamplePaperGenerator.renderHandwrittenNotebookBitmap(_uiState.value.activeSamplePaper)
                _uiState.update { it.copy(scannedBitmap = bmp) }
            }
        }
    }

    fun resetToDefaultAlgo() {
        _uiState.update {
            it.copy(
                codeText = ProgrammingLanguage.ALGO.defaultTemplate,
                editorTitle = "CalculSomme",
                providedInputs = emptyList(),
                waitingForVariable = null,
                executionResult = null,
                statusBannerMessage = "Exemple d'algorithme chargé"
            )
        }
    }

    fun updateCode(newCode: String) {
        _uiState.update { it.copy(codeText = newCode) }
    }

    fun insertTokenAtEndOrCursor(token: String) {
        _uiState.update { state ->
            val separator = if (state.codeText.endsWith("\n") || state.codeText.endsWith(" ") || state.codeText.isEmpty()) "" else " "
            state.copy(codeText = state.codeText + separator + token)
        }
    }

    /**
     * Démarre une nouvelle exécution depuis le début.
     * S'il y a un Lire(n), l'affichage (Ecrire) avant Lire(n) apparaît d'abord,
     * puis la console demande à l'utilisateur de saisir la valeur de n !
     */
    fun runCurrentCode() {
        executeWithInputs(emptyList())
    }

    /**
     * Quand l'utilisateur entre la valeur demandée par Lire(n) et clique sur Valider,
     * on poursuit l'exécution avec cette valeur !
     */
    fun submitLireValue(userInputValue: String) {
        val cleanVal = userInputValue.trim()
        if (cleanVal.isEmpty()) return
        val updatedInputs = _uiState.value.providedInputs + cleanVal
        executeWithInputs(updatedInputs)
    }

    private fun executeWithInputs(inputs: List<String>) {
        val snapshot = _uiState.value
        val startMs = System.currentTimeMillis()
        val outcome = executePseudocodeInteractive(
            pseudocode = snapshot.codeText,
            suppliedInputs = inputs
        )
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)

        _uiState.update {
            it.copy(
                isExecuting = false,
                providedInputs = inputs,
                waitingForVariable = outcome.waitingVarName,
                waitingForVariableType = outcome.waitingVarType,
                executionOutputText = if (outcome.stderr.isNotEmpty()) outcome.stderr else outcome.stdout,
                executionResult = ExecutionResult(
                    stdout = outcome.stdout,
                    stderr = outcome.stderr,
                    exitCode = if (outcome.stderr.isEmpty()) 0 else 1,
                    executionTimeMs = elapsed,
                    variables = if (outcome.stderr.isEmpty() && outcome.waitingVarName == null) outcome.variables else emptyList()
                )
            )
        }
    }

    fun selectSamplePaperSheet(sampleType: SamplePaperType) {
        _uiState.update { it.copy(activeSamplePaper = sampleType, isScanningPaper = true) }
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                SamplePaperGenerator.renderHandwrittenNotebookBitmap(sampleType)
            }
            scanBitmapPaper(bmp, sampleType)
        }
    }

    fun onPhotoPickedFromUri(uri: Uri) {
        _uiState.update { it.copy(isScanningPaper = true) }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                            val maxDim = 1400
                            val w = info.size.width
                            val h = info.size.height
                            if (w > maxDim || h > maxDim) {
                                val scale = maxDim.toFloat() / maxOf(w, h)
                                decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                scanBitmapPaper(bitmap, null)
            } else {
                _uiState.update {
                    it.copy(
                        isScanningPaper = false,
                        statusBannerMessage = "Impossible de lire l'image."
                    )
                }
            }
        }
    }

    private fun scanBitmapPaper(bitmap: Bitmap, sampleHint: SamplePaperType?) {
        _uiState.update { it.copy(scannedBitmap = bitmap, isScanningPaper = true) }
        viewModelScope.launch {
            val response = CodeExecutionEngine.scanHandwrittenPaper(
                bitmap = bitmap,
                preferredTargetLanguage = ProgrammingLanguage.ALGO,
                sampleHint = sampleHint
            )
            val msg = if (response.extractedCode.isNotBlank()) {
                "Texte lu exactement par Google OCR !"
            } else {
                "Aucun texte trouvé sur l'image. Essayez de cadrer plus près."
            }
            _uiState.update {
                it.copy(
                    isScanningPaper = false,
                    lastScanResponse = response,
                    statusBannerMessage = msg
                )
            }
        }
    }

    fun updateScannedCodeDraft(newCode: String) {
        _uiState.update { state ->
            val currentResp = state.lastScanResponse ?: return@update state
            state.copy(lastScanResponse = currentResp.copy(extractedCode = newCode))
        }
    }

    fun openScannedCodeInIdeAndExecute() {
        val scan = _uiState.value.lastScanResponse ?: return
        _uiState.update {
            it.copy(
                currentTab = StudioTab.IDE,
                editorTitle = scan.extractedTitle,
                codeText = scan.extractedCode,
                providedInputs = emptyList(),
                waitingForVariable = null,
                statusBannerMessage = "Algorithme scanné ! Cliquez sur Exécuter."
            )
        }
        runCurrentCode()
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}
