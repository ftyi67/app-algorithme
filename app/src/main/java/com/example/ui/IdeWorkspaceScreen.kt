package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutionResult
import com.example.model.ProgrammingLanguage
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSlateSurface
import com.example.ui.theme.CodeBackgroundDark
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarmAmber

private data class AffectationKey(
    val label: String,
    val insertText: String,
    val isPrimaryArrow: Boolean = false
)

private val phoneMissingSymbols = listOf(
    AffectationKey(label = "← Affectation", insertText = "← ", isPrimaryArrow = true),
    AffectationKey(label = "<- Flèche", insertText = "<- ", isPrimaryArrow = true),
    AffectationKey(label = ":=", insertText = ":= ", isPrimaryArrow = true),
    AffectationKey(label = "<=", insertText = "<= "),
    AffectationKey(label = ">=", insertText = ">= "),
    AffectationKey(label = "<> Différent", insertText = "<> "),
    AffectationKey(label = "\" \"", insertText = "\"\""),
    AffectationKey(label = "( )", insertText = "()"),
    AffectationKey(label = ":", insertText = " : ")
)

@Composable
fun IdeWorkspaceScreen(
    uiState: AlgoStudioUiState,
    onUpdateCode: (String) -> Unit,
    onSubmitLireValue: (String) -> Unit,
    onInsertToken: (String) -> Unit,
    onRunCode: () -> Unit,
    onResetCode: () -> Unit,
    onJumpToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    // État local ultra-rapide pour éviter de recomposer toute l'application à chaque caractère tapé
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = uiState.codeText,
                selection = TextRange(uiState.codeText.length)
            )
        )
    }

    // Compteur léger du nombre de lignes (sans créer de listes ni plusieurs Text composables)
    var lineCount by remember {
        mutableIntStateOf(uiState.codeText.count { it == '\n' } + 1)
    }

    val lineNumbersColumnText by remember {
        derivedStateOf {
            (1..lineCount.coerceAtLeast(1)).joinToString("\n")
        }
    }

    LaunchedEffect(uiState.codeText) {
        if (uiState.codeText != textFieldValue.text) {
            val safeCursor = textFieldValue.selection.start.coerceIn(0, uiState.codeText.length)
            textFieldValue = TextFieldValue(
                text = uiState.codeText,
                selection = TextRange(safeCursor)
            )
            lineCount = uiState.codeText.count { it == '\n' } + 1
        }
    }

    val insertAtCursor: (String) -> Unit = remember {
        { token ->
            val currentText = textFieldValue.text
            val start = textFieldValue.selection.min.coerceIn(0, currentText.length)
            val end = textFieldValue.selection.max.coerceIn(0, currentText.length)

            val newText = buildString(currentText.length + token.length) {
                append(currentText, 0, start)
                append(token)
                append(currentText, end, currentText.length)
            }
            val cursorOffset = if (token == "\"\"" || token == "()") {
                start + 1
            } else {
                start + token.length
            }

            textFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(cursorOffset.coerceIn(0, newText.length))
            )
            lineCount = newText.count { it == '\n' } + 1
            onUpdateCode(newText)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Bouton pour scanner une feuille
        Button(
            onClick = {
                onUpdateCode(textFieldValue.text)
                onJumpToScanner()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("jump_to_scanner_button")
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Scanner",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Scanner l'Algorithme depuis une Feuille",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        // 2. Barre des flèches d'affectation & mots-clés (sans LazyRow lourd)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlateSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Flèches d'Affectation & Symboles Rapides :",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryLight,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("affectation_arrows_row")
                ) {
                    phoneMissingSymbols.forEach { item ->
                        val bgColor = if (item.isPrimaryArrow) WarmAmber else CodeBackgroundDark
                        val txtColor = if (item.isPrimaryArrow) Color.White else TextPrimaryLight
                        val borderColor = if (item.isPrimaryArrow) WarmAmber else BorderSubtle

                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable { insertAtCursor(item.insertText) }
                                .testTag("symbol_btn_${item.label}")
                        ) {
                            Text(
                                text = item.label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = txtColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("quick_tokens_row")
                ) {
                    ProgrammingLanguage.ALGO.quickTokens.forEach { token ->
                        Surface(
                            color = CodeBackgroundDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { insertAtCursor(token) }
                        ) {
                            Text(
                                text = token,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Carte Éditeur fluide avec 1 seul composant Text pour tous les numéros de lignes
        Card(
            colors = CardDefaults.cardColors(containerColor = CodeBackgroundDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSlateSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(CoralError)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(WarmAmber)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cahier d'Algorithme",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                textFieldValue = TextFieldValue("", TextRange(0))
                                lineCount = 1
                                onUpdateCode("")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("clear_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = CoralError,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Effacer", fontSize = 12.sp, color = TextPrimaryLight)
                        }

                        OutlinedButton(
                            onClick = onResetCode,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("reset_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Exemple",
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exemple", fontSize = 12.sp, color = TextPrimaryLight)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = lineNumbersColumnText,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmAmber
                        ),
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 2.dp)
                    )

                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newVal ->
                            val textChanged = newVal.text != textFieldValue.text
                            textFieldValue = newVal
                            if (textChanged) {
                                lineCount = newVal.text.count { it == '\n' } + 1
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Algorithme MonAlgo\nVariables\n    n : Entier\nDebut\n    Ecrire(\"Donner n :\")\n    Lire(n)\nFin",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                color = TextSecondaryMuted.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            color = TextPrimaryLight
                        ),
                        minLines = 9,
                        maxLines = 18,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CodeBackgroundDark,
                            unfocusedContainerColor = CodeBackgroundDark,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = ElectricCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 200.dp)
                            .testTag("code_editor_input")
                    )
                }
            }
        }

        // 4. Bouton Exécuter
        Button(
            onClick = {
                onUpdateCode(textFieldValue.text)
                onRunCode()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonEmerald,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("execute_code_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Exécuter")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exécuter l'Algorithme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // 5. Console d'Exécution Interactive
        if (uiState.executionResult != null || uiState.waitingForVariable != null) {
            InteractiveExecutionConsoleCard(
                result = uiState.executionResult,
                waitingVarName = uiState.waitingForVariable,
                waitingVarType = uiState.waitingForVariableType,
                onSubmitLireValue = onSubmitLireValue
            )
        }
    }
}

@Composable
private fun InteractiveExecutionConsoleCard(
    result: ExecutionResult?,
    waitingVarName: String?,
    waitingVarType: String,
    onSubmitLireValue: (String) -> Unit
) {
    var currentLireInput by remember(waitingVarName, result?.stdout) { mutableStateOf("") }
    val hasError = result != null && result.exitCode != 0

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = when {
                    hasError -> CoralError
                    waitingVarName != null -> WarmAmber
                    else -> NeonEmerald
                },
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("execution_console_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when {
                    hasError -> "Attention : Erreur détectée dans l'Algorithme !"
                    waitingVarName != null -> "Exécution en cours — En attente de Lire($waitingVarName)"
                    else -> "Résultat de l'Exécution :"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (hasError) CoralError else TextPrimaryLight,
                fontWeight = FontWeight.Bold
            )

            if (result != null && result.stdout.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CodeBackgroundDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = result.stdout,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonEmerald,
                        modifier = Modifier.testTag("console_stdout_text")
                    )
                }
            }

            if (waitingVarName != null) {
                Surface(
                    color = CodeBackgroundDark,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, WarmAmber, RoundedCornerShape(10.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Instruction Lire($waitingVarName) : Entrez la valeur de '$waitingVarName' ($waitingVarType) :",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmAmber,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentLireInput,
                                onValueChange = { currentLireInput = it },
                                placeholder = { Text("Valeur de $waitingVarName...") },
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (currentLireInput.isNotBlank()) {
                                            onSubmitLireValue(currentLireInput)
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CardSlateSurface,
                                    unfocusedContainerColor = CardSlateSurface,
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("interactive_lire_input")
                            )

                            Button(
                                onClick = {
                                    if (currentLireInput.isNotBlank()) {
                                        onSubmitLireValue(currentLireInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("submit_lire_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Entrer",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Entrer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (result != null && result.stderr.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CoralError.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = result.stderr,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        color = CoralError
                    )
                }
            }

            if (result != null && result.variables.isNotEmpty() && waitingVarName == null) {
                Text(
                    text = "Valeurs finales des Variables :",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmAmber
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    result.variables.forEach { v ->
                        Surface(
                            color = CodeBackgroundDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "${v.name} ← ${v.value}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
