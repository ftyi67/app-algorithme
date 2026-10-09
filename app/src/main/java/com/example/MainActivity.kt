package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AlgoStudioViewModel
import com.example.ui.IdeWorkspaceScreen
import com.example.ui.PaperScannerScreen
import com.example.ui.StudioTab
import com.example.ui.WelcomeTutorialScreen
import com.example.ui.theme.CardSlateSurface
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlateSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AlgoScanStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoScanStudioApp(
    viewModel: AlgoStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWelcomeTutorial by rememberSaveable { mutableStateOf(true) }

    if (showWelcomeTutorial) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DeepNavyBg,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            WelcomeTutorialScreen(
                onFinishTutorial = { showWelcomeTutorial = false },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepNavyBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_daftralgo_icon_1791564716650),
                            contentDescription = "Logo DaftrAlgo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DaftrAlgo",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimaryLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cahier d'Algorithme & Scanner Feuille",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showWelcomeTutorial = true },
                        modifier = Modifier.testTag("reopen_tutorial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Revoir le tutoriel",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ElevatedSlateSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ElevatedSlateSurface
            ) {
                StudioTab.entries.forEach { tab ->
                    val selected = uiState.currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            val iconVec = when (tab) {
                                StudioTab.IDE -> Icons.Filled.Edit
                                StudioTab.SCANNER -> Icons.Filled.Search
                            }
                            Icon(imageVector = iconVec, contentDescription = tab.label)
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan,
                            unselectedIconColor = TextSecondaryMuted,
                            unselectedTextColor = TextSecondaryMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.routeId}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            uiState.statusBannerMessage?.let { msg ->
                Surface(
                    color = CardSlateSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = TextSecondaryMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            when (uiState.currentTab) {
                StudioTab.IDE -> IdeWorkspaceScreen(
                    uiState = uiState,
                    onUpdateCode = viewModel::updateCode,
                    onSubmitLireValue = viewModel::submitLireValue,
                    onInsertToken = viewModel::insertTokenAtEndOrCursor,
                    onRunCode = viewModel::runCurrentCode,
                    onResetCode = viewModel::resetToDefaultAlgo,
                    onJumpToScanner = { viewModel.selectTab(StudioTab.SCANNER) },
                    modifier = Modifier.weight(1f)
                )

                StudioTab.SCANNER -> PaperScannerScreen(
                    uiState = uiState,
                    onSelectSampleSheet = viewModel::selectSamplePaperSheet,
                    onPhotoPicked = viewModel::onPhotoPickedFromUri,
                    onUpdateScannedCodeDraft = viewModel::updateScannedCodeDraft,
                    onOpenInIdeAndRun = viewModel::openScannedCodeInIdeAndExecute,
                    onBackToIde = { viewModel.selectTab(StudioTab.IDE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
