package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SamplePaperType
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSlateSurface
import com.example.ui.theme.CodeBackgroundDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarmAmber

@Composable
fun PaperScannerScreen(
    uiState: AlgoStudioUiState,
    onSelectSampleSheet: (SamplePaperType) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onUpdateScannedCodeDraft: (String) -> Unit,
    onOpenInIdeAndRun: () -> Unit,
    onBackToIde: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToIde()
    }

    var showCameraScreen by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoPicked(uri)
        }
    }

    if (showCameraScreen) {
        CameraScreen(
            onImageCaptured = { capturedUri ->
                showCameraScreen = false
                onPhotoPicked(capturedUri)
            },
            onClose = {
                showCameraScreen = false
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Scanner un Algorithme écrit sur Papier",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimaryLight,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Prenez en photo votre algorithme écrit sur cahier/feuille pour le lire, le modifier et l'exécuter directement.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryMuted
        )

        // 1. Boutons Caméra OCR & Galerie Photo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCameraScreen = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("camera_scan_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Caméra", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Caméra OCR", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("gallery_scan_button")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Galerie",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galerie Photo", color = TextPrimaryLight)
            }
        }

        // 2. Feuilles d'algorithme d'exemple
        Text(
            text = "Ou choisissez une feuille d'algorithme d'exemple :",
            style = MaterialTheme.typography.labelLarge,
            color = WarmAmber
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("sample_sheets_row")
        ) {
            SamplePaperType.entries.forEach { sample ->
                val isSelected = uiState.activeSamplePaper == sample
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else CardSlateSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(210.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else BorderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectSampleSheet(sample) }
                        .testTag("sample_sheet_${sample.name.lowercase()}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sample.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sample.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 3. Image de la feuille scannée
        uiState.scannedBitmap?.let { bmp ->
            val cachedImageBitmap = remember(bmp) { bmp.asImageBitmap() }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Feuille scannée :",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryLight
                    )
                    Image(
                        bitmap = cachedImageBitmap,
                        contentDescription = "Feuille d'algorithme",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        if (uiState.isScanningPaper) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Lecture de l'algorithme...", color = TextPrimaryLight)
            }
        }

        // 4. Algorithme lu (modifiable) + Bouton Exécuter
        uiState.lastScanResponse?.let { scanResp ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CodeBackgroundDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonEmerald, RoundedCornerShape(12.dp))
                    .testTag("scanned_code_result_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Texte & Chiffres lus par Google OCR (modifiable) :",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonEmerald,
                        fontWeight = FontWeight.Bold
                    )

                    if (scanResp.explanation.isNotBlank()) {
                        Text(
                            text = scanResp.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = scanResp.extractedCode,
                        onValueChange = onUpdateScannedCodeDraft,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextPrimaryLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                            .testTag("scanned_code_edit_field")
                    )

                    Button(
                        onClick = onOpenInIdeAndRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_scanned_in_ide_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mettre dans l'Éditeur & Exécuter",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
