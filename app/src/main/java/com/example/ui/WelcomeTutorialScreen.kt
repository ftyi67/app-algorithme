package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSlateSurface
import com.example.ui.theme.CodeBackgroundDark
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarmAmber

private data class TutorialStepItem(
    val stepNumber: Int,
    val badge: String,
    val title: String,
    val description: String,
    val codeExample: String,
    val accentColor: Color,
    val icon: ImageVector
)

private val tutorialSteps = listOf(
    TutorialStepItem(
        stepNumber = 1,
        badge = "ÉTAPE 1 • ÉCRIRE FACILEMENT",
        title = "Écrivez votre Algorithme comme sur un Cahier",
        description = "Utilisez les boutons rapides pour insérer les flèches d'affectation (←, <-, :=) et les mots-clés (Variables, Debut, Ecrire, Lire, Si, Pour, Fin) exactement là où se trouve votre curseur.",
        codeExample = "Algorithme Somme\nVariables\n    a, b, s : Entier\nDebut\n    s ← a + b\nFin",
        accentColor = ElectricCyan,
        icon = Icons.Default.Edit
    ),
    TutorialStepItem(
        stepNumber = 2,
        badge = "ÉTAPE 2 • SCANNER UNE FEUILLE",
        title = "Scannez votre Algorithme écrit sur Papier",
        description = "Prenez en photo votre cahier ou choisissez une image de la galerie. Google OCR lit les mots et les nombres pour les placer directement dans l'éditeur.",
        codeExample = "📷 Photo du Cahier  ➔  Algorithme modifiable",
        accentColor = WarmAmber,
        icon = Icons.Default.Search
    ),
    TutorialStepItem(
        stepNumber = 3,
        badge = "ÉTAPE 3 • EXÉCUTER & LIRE EN DIRECT",
        title = "Exécution Interactive & Détection d'Erreurs",
        description = "Cliquez sur Exécuter : l'application affiche d'abord Ecrire(...), vous demande la valeur de Lire(n) en direct, et vous indique le numéro exact de la ligne s'il y a une faute !",
        codeExample = "Donner un nombre n :\n> n ← 5\nRésultat : La somme = 15",
        accentColor = NeonEmerald,
        icon = Icons.Default.PlayArrow
    )
)

@Composable
fun WelcomeTutorialScreen(
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = tutorialSteps[currentStepIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("welcome_tutorial_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Barre du haut avec bouton Passer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CardSlateSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = "Guide de démarrage • ${currentStepIndex + 1}/${tutorialSteps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmAmber,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "Passer ➔",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFinishTutorial() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("skip_tutorial_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logo de l'application + Nom DaftrAlgo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardSlateSurface)
                    .border(2.dp, BorderSubtle, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_daftralgo_icon_1791564716650),
                    contentDescription = "Logo DaftrAlgo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Text(
                text = "DaftrAlgo",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            Text(
                text = "Votre Cahier d'Algorithme Simple, Intelligent & Rapide",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Carte de l'étape actuelle du tutoriel
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlateSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, currentStep.accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentStep.accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentStep.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = currentStep.accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryLight,
                    lineHeight = 21.sp
                )

                // Aperçu visuel style feuille de code
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CodeBackgroundDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentStep.codeExample,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentStep.accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Indicateurs de progression (3 points cliquables)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tutorialSteps.indices.forEach { idx ->
                val isSelected = idx == currentStepIndex
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(if (isSelected) 28.dp else 10.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricCyan else BorderSubtle)
                        .clickable { currentStepIndex = idx }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons Suivant / Commencer
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentStepIndex < tutorialSteps.lastIndex) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Précédent", color = TextPrimaryLight, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { currentStepIndex++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp)
                            .testTag("next_tutorial_step_button")
                    ) {
                        Text(
                            text = "Suivant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Suivant",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onFinishTutorial,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Commencer",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Commencer sur DaftrAlgo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}
