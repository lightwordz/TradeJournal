package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RoomTradeRepository
import com.example.data.StartupBootCache
import com.example.db.AppDatabase
import com.example.model.Trade
import com.example.screens.*
import com.example.ui.components.AddTradeSheet
import com.example.ui.components.CsvImportSheet
import com.example.ui.components.LogTradeFab
import com.example.ui.components.Screen
import com.example.ui.components.TradeBottomNavBar
import com.example.ui.components.TradeDetailSheet
import com.example.ui.components.TradeNavigationRail
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.CsvExporter
import com.example.utils.ScreenshotOcrExtractor
import com.example.utils.ScreenshotStorage
import com.example.utils.ScreenshotTradeDraft
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialFastPrefs = StartupBootCache.readInitialPreferences(applicationContext)
        val initialStyle = if (initialFastPrefs?.isDarkTheme == true) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = initialStyle, navigationBarStyle = initialStyle)
        val repository = RoomTradeRepository(AppDatabase.getInstance(applicationContext))

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository, initialFastPrefs)
            )
            val preferencesState by viewModel.preferencesState.collectAsStateWithLifecycle()
            val trades by viewModel.trades.collectAsStateWithLifecycle()
            val capitalEntries by viewModel.capitalEntries.collectAsStateWithLifecycle()
            val effectiveCapital by viewModel.effectiveCapital.collectAsStateWithLifecycle()
            val brokerProfile by viewModel.brokerProfile.collectAsStateWithLifecycle()

            val preferences = preferencesState

            LaunchedEffect(preferences) {
                if (preferences != null) {
                    StartupBootCache.sync(applicationContext, preferences)
                }
            }

            SideEffect {
                val isDark = preferences?.isDarkTheme == true
                val style = if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            var currentRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
            var showAddSheet by remember { mutableStateOf(false) }
            var showImportSheet by remember { mutableStateOf(false) }
            var editingTrade by remember { mutableStateOf<Trade?>(null) }
            var scannedDraft by remember { mutableStateOf<ScreenshotTradeDraft?>(null) }
            var selectedTrade by remember { mutableStateOf<Trade?>(null) }

            val csvSaveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                val prefs = preferences ?: return@rememberLauncherForActivityResult
                if (uri != null) {
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val csv = CsvExporter.toCsv(
                                    trades = trades,
                                    profile = brokerProfile,
                                    matchingMethod = prefs.matchingMethod
                                )
                                contentResolver.openOutputStream(uri)?.use { out ->
                                    out.write(csv.toByteArray(Charsets.UTF_8))
                                } ?: error("Could not open destination file")
                            }
                        }
                        snackbarHostState.showSnackbar(
                            if (result.isSuccess) "Saved CSV export (${trades.size} trades)."
                            else "Could not save CSV file. Please try another folder."
                        )
                    }
                }
            }

            val journalScanLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    val activeLots = preferences?.lotSizes ?: com.example.engine.DefaultLotSizes.seeded()
                    coroutineScope.launch {
                        val draft = runCatching {
                            ScreenshotOcrExtractor.scanAndParse(
                                context = applicationContext,
                                uri = uri,
                                lotSizes = activeLots
                            )
                        }.getOrNull()
                        if (draft != null) {
                            editingTrade = null
                            scannedDraft = draft
                            showAddSheet = true
                        } else {
                            snackbarHostState.showSnackbar("Could not read screenshot. You can enter trade values manually.")
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = preferences?.isDarkTheme == true) {
                val rootMode = when {
                    preferences == null -> 0
                    !preferences.hasCompletedOnboarding -> 1
                    else -> 2
                }
                Crossfade(
                    targetState = rootMode,
                    animationSpec = tween(durationMillis = 180),
                    label = "root_launch_transition"
                ) { mode ->
                when (mode) {
                    0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    1 -> {
                        OnboardingScreen(
                            onComplete = { name, broker, capital ->
                                viewModel.completeOnboarding(name, broker, capital)
                            }
                        )
                    }

                    else -> {
                        val activePreferences = preferences ?: return@Crossfade
                        BackHandler(
                            enabled = currentRoute != Screen.Dashboard.route &&
                                !showAddSheet &&
                                !showImportSheet &&
                                editingTrade == null &&
                                selectedTrade == null
                        ) {
                            currentRoute = Screen.Dashboard.route
                        }

                        val openLogTradeSheet = {
                            editingTrade = null
                            scannedDraft = null
                            showAddSheet = true
                        }

                        val renderActiveScreen: @Composable () -> Unit = {
                            when (currentRoute) {
                                Screen.Dashboard.route -> DashboardScreen(
                                    trades = trades,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod,
                                    traderName = activePreferences.traderName,
                                    effectiveCapital = effectiveCapital,
                                    dailyLossLimit = activePreferences.dailyLossLimit,
                                    onViewAllClick = { currentRoute = Screen.Journal.route },
                                    onAddTradeClick = openLogTradeSheet,
                                    onTradeClick = { selectedTrade = it }
                                )
                                Screen.Analytics.route -> AnalyticsScreen(
                                    trades = trades,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod
                                )
                                Screen.Calendar.route -> CalendarScreen(
                                    trades = trades,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod,
                                    onTradeClick = { selectedTrade = it }
                                )
                                Screen.Journal.route -> JournalScreen(
                                    trades = trades,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod,
                                    onTradeClick = { selectedTrade = it },
                                    onAddTradeClick = openLogTradeSheet,
                                    onImportCsvClick = { showImportSheet = true },
                                    onScanScreenshotClick = {
                                        journalScanLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                Screen.Profile.route -> ProfileScreen(
                                    preferences = activePreferences,
                                    effectiveCapital = effectiveCapital,
                                    capitalEntries = capitalEntries,
                                    tradesCount = trades.size,
                                    onUpdateTraderName = { viewModel.updateTraderName(it) },
                                    onToggleTheme = { viewModel.toggleTheme(it) },
                                    onUpdateCapital = { viewModel.updateStartingCapital(it) },
                                    onUpdateDailyLimit = { viewModel.updateDailyLossLimit(it) },
                                    onUpdateBroker = { viewModel.updateBroker(it) },
                                    onUpdateCustomRates = { viewModel.updateCustomRateProfile(it) },
                                    onUpdateLotSizes = { viewModel.updateLotSizes(it) },
                                    onAddCapitalEntry = { viewModel.addCapitalEntry(it) },
                                    onDeleteCapitalEntry = { viewModel.deleteCapitalEntry(it) },
                                    onExportCsv = {
                                        csvSaveLauncher.launch("tradejournal_trades.csv")
                                    },
                                    onClearAll = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                ScreenshotStorage.clearAll(applicationContext)
                                            }
                                            viewModel.wipeAllData()
                                            snackbarHostState.showSnackbar("All local trades, capital entries & screenshots deleted.")
                                        }
                                    }
                                )
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth >= 600.dp

                            if (isWideScreen) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    TradeNavigationRail(
                                        currentRoute = currentRoute,
                                        onTabSelected = { currentRoute = it },
                                        onLogTradeClick = openLogTradeSheet
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .windowInsetsPadding(
                                                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                                            ),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 720.dp)
                                                .fillMaxSize()
                                        ) {
                                            renderActiveScreen()
                                        }
                                    }
                                }
                            } else {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    contentWindowInsets = WindowInsets.safeDrawing,
                                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                                    floatingActionButtonPosition = FabPosition.Center,
                                    floatingActionButton = {
                                        LogTradeFab(onClick = openLogTradeSheet)
                                    },
                                    bottomBar = {
                                        TradeBottomNavBar(
                                            currentRoute = currentRoute,
                                            onTabSelected = { currentRoute = it }
                                        )
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .padding(innerPadding)
                                            .fillMaxSize()
                                    ) {
                                        renderActiveScreen()
                                    }
                                }
                            }

                            if (isWideScreen) {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                )
                            }

                            if (showAddSheet) {
                                val todayIso = remember {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                                    }.format(java.util.Date())
                                }
                                val todayGuardrail = remember(trades, brokerProfile, activePreferences.matchingMethod, activePreferences.dailyLossLimit, todayIso) {
                                    val todayResults = trades
                                        .filter { it.date == todayIso }
                                        .reversed()
                                        .map { com.example.engine.CalculationEngine.calculateTradeResult(it, brokerProfile, activePreferences.matchingMethod) }
                                    com.example.engine.CalculationEngine.evaluateDailyRiskGuardrail(todayResults, activePreferences.dailyLossLimit)
                                }
                                AddTradeSheet(
                                    initialTrade = editingTrade,
                                    initialScreenshotDraft = scannedDraft,
                                    brokerId = activePreferences.defaultBroker,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod,
                                    lotSizes = activePreferences.lotSizes,
                                    effectiveCapital = effectiveCapital,
                                    todayGuardrail = todayGuardrail,
                                    onSaveCustomTicker = { ticker, lotSize ->
                                        val updated = activePreferences.lotSizes.toMutableMap()
                                        updated[ticker] = lotSize
                                        viewModel.updateLotSizes(updated)
                                    },
                                    onDismiss = {
                                        showAddSheet = false
                                        editingTrade = null
                                        scannedDraft = null
                                    },
                                    onSaveTrade = { viewModel.saveTrade(it) }
                                )
                            }

                            if (showImportSheet) {
                                CsvImportSheet(
                                    existingTrades = trades,
                                    onDismiss = { showImportSheet = false },
                                    onImportTrades = { viewModel.saveTrades(it) }
                                )
                            }

                            selectedTrade?.let { trade ->
                                TradeDetailSheet(
                                    trade = trade,
                                    brokerProfile = brokerProfile,
                                    matchingMethod = activePreferences.matchingMethod,
                                    onDismiss = { selectedTrade = null },
                                    onEdit = { toEdit ->
                                        editingTrade = toEdit
                                        scannedDraft = null
                                        showAddSheet = true
                                    },
                                    onDelete = { viewModel.deleteTrade(it) }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
