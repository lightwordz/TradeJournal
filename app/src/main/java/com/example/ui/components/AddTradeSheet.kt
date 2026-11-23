package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.DefaultLotSizes
import com.example.engine.MatchingMethod
import com.example.model.*
import com.example.ui.theme.*
import com.example.utils.Formatters
import com.example.utils.IndianMarketCalendar
import com.example.utils.ScreenshotOcrExtractor
import com.example.utils.ScreenshotStorage
import com.example.utils.ScreenshotTradeDraft
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTradeSheet(
    initialTrade: Trade? = null,
    initialScreenshotDraft: ScreenshotTradeDraft? = null,
    brokerId: String = "Zerodha",
    brokerProfile: BrokerRateProfile = BrokerRateProfile.forBroker(brokerId),
    matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    lotSizes: Map<String, Int> = DefaultLotSizes.seeded(),
    effectiveCapital: BigDecimal = BigDecimal("500000"),
    todayGuardrail: CalculationEngine.DailyRiskGuardrail? = null,
    onSaveCustomTicker: (String, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSaveTrade: (Trade) -> Unit
) {
    var selectedRiskPct by remember { mutableStateOf(BigDecimal("1.0")) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultTicker = remember(lotSizes) {
        if (lotSizes.containsKey("NIFTY 50")) "NIFTY 50"
        else lotSizes.keys.firstOrNull() ?: "Other"
    }
    val initialMatchedKey = remember(initialTrade, initialScreenshotDraft, lotSizes) {
        val draftMatched = initialScreenshotDraft?.matchedTickerKey
        if (initialTrade == null && draftMatched != null) return@remember draftMatched
        val upper = (initialTrade?.symbol ?: initialScreenshotDraft?.symbol).orEmpty().uppercase()
        lotSizes.keys
            .sortedByDescending { it.length }
            .firstOrNull { upper.startsWith(it) }
            ?: if (upper.startsWith("NIFTY") && lotSizes.containsKey("NIFTY 50")) "NIFTY 50" else null
    }
    var selectedTickerOption by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            when {
                initialTrade == null && initialScreenshotDraft == null -> defaultTicker
                initialTrade == null && initialScreenshotDraft != null -> initialMatchedKey ?: "Other"
                initialMatchedKey != null -> initialMatchedKey
                else -> "Other"
            }
        )
    }
    var saveCustomTicker by remember(initialTrade) { mutableStateOf(false) }

    var symbol by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            initialTrade?.symbol
                ?: initialScreenshotDraft?.symbol?.takeIf { it.isNotBlank() }
                ?: if (defaultTicker == "Other") "" else "$defaultTicker "
        )
    }
    var selectedSegment by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(initialTrade?.segment ?: initialScreenshotDraft?.segment ?: Segment.OPT)
    }
    var selectedSide by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(initialTrade?.side ?: initialScreenshotDraft?.side ?: TradeSide.BUY)
    }
    var entryPrice by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            initialTrade?.orders?.firstOrNull()?.price?.toPlainString()
                ?: initialScreenshotDraft?.entryPrice?.stripTrailingZeros()?.toPlainString()
                ?: ""
        )
    }
    var exitPrice by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            initialTrade?.orders?.lastOrNull()?.price?.toPlainString()
                ?: initialScreenshotDraft?.exitPrice?.stripTrailingZeros()?.toPlainString()
                ?: ""
        )
    }

    val startingMultiplier = remember(initialTrade, initialScreenshotDraft, lotSizes, defaultTicker) {
        initialTrade?.lotSize
            ?: initialScreenshotDraft?.lotSize
            ?: lotSizes[defaultTicker]
            ?: 165
    }
    val startingLots = remember(initialTrade, initialScreenshotDraft) {
        initialTrade?.lots ?: initialScreenshotDraft?.lots ?: 1
    }
    var lotMultiplier by remember(initialTrade, initialScreenshotDraft) { mutableIntStateOf(startingMultiplier) }
    var lotsText by remember(initialTrade, initialScreenshotDraft) { mutableStateOf(startingLots.toString()) }
    var qtyText by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf((initialScreenshotDraft?.quantity ?: (startingLots * startingMultiplier)).toString())
    }

    var stopLossText by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            when {
                initialTrade != null && initialTrade.stopLoss > BigDecimal.ZERO -> initialTrade.stopLoss.toPlainString()
                initialScreenshotDraft?.stopLoss != null -> initialScreenshotDraft.stopLoss.stripTrailingZeros().toPlainString()
                else -> ""
            }
        )
    }
    var targetText by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            when {
                initialTrade != null && initialTrade.target > BigDecimal.ZERO -> initialTrade.target.toPlainString()
                initialScreenshotDraft?.target != null -> initialScreenshotDraft.target.stripTrailingZeros().toPlainString()
                else -> ""
            }
        )
    }
    var setup by remember(initialTrade) { mutableStateOf(initialTrade?.setup ?: "Morning Breakout") }
    var emotionBefore by remember(initialTrade) { mutableStateOf(initialTrade?.emotionBefore ?: "Calm") }
    var selectedMistake by remember(initialTrade) { mutableStateOf(initialTrade?.mistakes?.firstOrNull() ?: "") }
    var rating by remember(initialTrade) { mutableIntStateOf(initialTrade?.rating ?: 4) }
    var notes by remember(initialTrade) { mutableStateOf(initialTrade?.notes ?: "") }
    var showOptionalDetails by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            (initialTrade != null && (initialTrade.stopLoss > BigDecimal.ZERO || initialTrade.target > BigDecimal.ZERO)) ||
                (initialScreenshotDraft?.stopLoss != null || initialScreenshotDraft?.target != null)
        )
    }

    var screenshots by remember(initialTrade, initialScreenshotDraft) {
        mutableStateOf(
            initialTrade?.screenshots ?: listOfNotNull(initialScreenshotDraft?.screenshotPath)
        )
    }
    var isScanningScreenshot by remember { mutableStateOf(false) }

    val applyDraftToForm: (ScreenshotTradeDraft) -> Unit = { draft ->
        if (draft.symbol.isNotBlank()) {
            symbol = draft.symbol
            selectedTickerOption = draft.matchedTickerKey ?: "Other"
        }
        selectedSegment = draft.segment
        selectedSide = draft.side
        draft.entryPrice?.let { entryPrice = it.stripTrailingZeros().toPlainString() }
        draft.exitPrice?.let { exitPrice = it.stripTrailingZeros().toPlainString() }
        draft.lotSize?.let { lotMultiplier = it }
        draft.lots?.let { lotsText = it.toString() }
        draft.quantity?.let { qtyText = it.toString() }
        draft.stopLoss?.let {
            stopLossText = it.stripTrailingZeros().toPlainString()
            showOptionalDetails = true
        }
        draft.target?.let {
            targetText = it.stripTrailingZeros().toPlainString()
            showOptionalDetails = true
        }
        draft.screenshotPath?.let { path ->
            if (path !in screenshots) screenshots = screenshots + path
        }
    }

    val chartPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isScanningScreenshot = true
            coroutineScope.launch {
                val parsed = ScreenshotOcrExtractor.scanAndParse(context, uri, lotSizes)
                if (initialTrade == null && parsed.hasUsefulData) {
                    applyDraftToForm(parsed)
                } else {
                    parsed.screenshotPath?.let { path ->
                        if (path !in screenshots) screenshots = screenshots + path
                    }
                }
                isScanningScreenshot = false
            }
        }
    }

    val todayDate = remember { IndianMarketCalendar.todayIstDateString() }
    val yesterdayDate = remember { IndianMarketCalendar.yesterdayIstDateString() }
    var selectedDate by remember(initialTrade) { mutableStateOf(initialTrade?.date ?: todayDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEquity = selectedSegment == Segment.EQ_INTRADAY || selectedSegment == Segment.EQ_DELIVERY
    val parsedQty = qtyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val parsedLots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val inferredUnitLot = when {
        isEquity -> 1
        lotMultiplier > 1 -> lotMultiplier
        parsedLots > 0 && parsedQty >= parsedLots && parsedQty % parsedLots == 0 -> parsedQty / parsedLots
        else -> 1
    }
    val parsedEntry = entryPrice.toBigDecimalOrNull()
    val parsedExit = exitPrice.toBigDecimalOrNull()
    val canSave = symbol.isNotBlank() && parsedEntry != null && parsedExit != null && parsedQty > 0

    val previewResult = remember(
        symbol, selectedSegment, selectedSide, parsedEntry, parsedExit,
        parsedQty, parsedLots, inferredUnitLot, selectedDate, brokerProfile, matchingMethod, initialTrade
    ) {
        if (parsedEntry == null || parsedExit == null || parsedQty <= 0) null
        else {
            val exchange = when {
                selectedSegment == Segment.COMMODITY_FUT || selectedSegment == Segment.COMMODITY_OPT -> Exchange.MCX
                initialTrade != null && initialTrade.exchange != Exchange.MCX -> initialTrade.exchange
                symbol.uppercase().startsWith("SENSEX") || symbol.uppercase().startsWith("BANKEX") -> Exchange.BSE
                else -> Exchange.NSE
            }
            val draft = Trade(
                segment = selectedSegment,
                exchange = exchange,
                symbol = symbol.ifBlank { "PREVIEW" },
                side = selectedSide,
                orders = listOf(
                    OrderFill(datetime = "${selectedDate}T09:30:00", qty = parsedQty.toLong(), price = parsedEntry),
                    OrderFill(datetime = "${selectedDate}T14:30:00", qty = parsedQty.toLong(), price = parsedExit)
                ),
                lots = parsedLots,
                lotSize = inferredUnitLot,
                multiplier = initialTrade?.multiplier ?: BigDecimal.ONE,
                plannedRisk = BigDecimal("1000"),
                setup = setup,
                tags = emptyList(),
                emotionBefore = emotionBefore,
                emotionDuring = "Calm",
                emotionAfter = "Calm",
                mistakes = emptyList(),
                rating = rating,
                notes = "",
                date = selectedDate
            )
            CalculationEngine.calculateTradeResult(draft, brokerProfile, matchingMethod)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val horizontalPad = if (maxWidth < 360.dp) 16.dp else 20.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Minimalist Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTrade == null) "Log Trade" else "Edit Trade",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (initialTrade == null && todayGuardrail != null && todayGuardrail.level != CalculationEngine.GuardrailLevel.CALM) {
                val warnAccent = if (todayGuardrail.level == CalculationEngine.GuardrailLevel.BREACHED) LossRose else GoldCommodity
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = warnAccent.copy(alpha = 0.08f),
                    border = BorderStroke(0.75.dp, warnAccent.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = warnAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        val msg = if (todayGuardrail.level == CalculationEngine.GuardrailLevel.BREACHED) {
                            "Configured daily loss limit reached (${Formatters.inr(todayGuardrail.todayNetPnl)})."
                        } else {
                            "${todayGuardrail.limitUsedPct}% of your configured daily loss limit reached."
                        }
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Distraction-Free 3-Step Flow: 1. Attach Chart -> 2. Trade & Size -> 3. Reflection
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad)
                    .padding(top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Clean Attach Chart Card at Top
                AttachChartCard(
                    screenshots = screenshots,
                    isScanning = isScanningScreenshot,
                    onPickChart = {
                        chartPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveScreenshot = { path ->
                        ScreenshotStorage.deleteIfOwned(context, path)
                        screenshots = screenshots.filterNot { it == path }
                    }
                )

                // 2. Trade Log Entry Block
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        lotSizes.keys.forEach { ticker ->
                            val selected = selectedTickerOption == ticker
                            val chipMultiplier = lotSizes[ticker] ?: 1
                            MinimalPillChip(
                                label = ticker,
                                selected = selected,
                                onClick = {
                                    selectedTickerOption = ticker
                                    saveCustomTicker = false
                                    symbol = if (DefaultLotSizes.isCommodity(ticker)) ticker else "$ticker "
                                    val seg = if (DefaultLotSizes.isCommodity(ticker)) Segment.COMMODITY_FUT else Segment.OPT
                                    selectedSegment = seg
                                    val lots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    lotsText = lots.toString()
                                    lotMultiplier = chipMultiplier
                                    qtyText = DefaultLotSizes.computeQuantity(lots, chipMultiplier).toString()
                                }
                            )
                        }
                        MinimalPillChip(
                            label = "Other",
                            selected = selectedTickerOption == "Other",
                            onClick = {
                                selectedTickerOption = "Other"
                                if (lotSizes.keys.any { symbol.trim().equals(it, ignoreCase = true) }) {
                                    symbol = ""
                                }
                                lotMultiplier = 1
                                val lots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                qtyText = lots.toString()
                            },
                            modifier = Modifier.testTag("ticker_other_chip")
                        )
                    }

                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { next ->
                            val upper = next.uppercase()
                            symbol = upper
                            if (selectedTickerOption != "Other") {
                                val matched = lotSizes.keys
                                    .sortedByDescending { it.length }
                                    .firstOrNull { upper.startsWith(it) }
                                if (matched != null && initialTrade == null) {
                                    selectedTickerOption = matched
                                    val matchedLot = lotSizes[matched] ?: 1
                                    val lots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    lotMultiplier = matchedLot
                                    qtyText = DefaultLotSizes.computeQuantity(lots, matchedLot).toString()
                                    if (DefaultLotSizes.isCommodity(matched)) {
                                        selectedSegment = Segment.COMMODITY_FUT
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                if (selectedTickerOption == "Other") "Ticker / Contract"
                                else "Contract (e.g. NIFTY 50 24600 CE)"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_symbol"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (selectedTickerOption == "Other") {
                        val baseTicker = symbol.trim().substringBefore(" ").uppercase()
                        if (baseTicker.length >= 2 && !lotSizes.containsKey(baseTicker) && inferredUnitLot > 0) {
                            MinimalPillChip(
                                label = "Remember \"$baseTicker\" in quick tickers",
                                selected = saveCustomTicker,
                                onClick = { saveCustomTicker = !saveCustomTicker },
                                activeContainerColor = PrimaryCobalt.copy(alpha = 0.12f),
                                activeContentColor = PrimaryCobalt,
                                activeBorderColor = PrimaryCobalt.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Market Segment Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(
                            Segment.OPT to "Options",
                            Segment.FUT to "Futures",
                            Segment.EQ_INTRADAY to "Intraday",
                            Segment.EQ_DELIVERY to "Delivery",
                            Segment.COMMODITY_FUT to "Commodity"
                        ).forEach { (seg, label) ->
                            MinimalPillChip(
                                label = label,
                                selected = selectedSegment == seg,
                                onClick = {
                                    selectedSegment = seg
                                    if (seg == Segment.EQ_INTRADAY || seg == Segment.EQ_DELIVERY) {
                                        lotMultiplier = 1
                                        val lots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        qtyText = lots.toString()
                                    } else if (selectedTickerOption != "Other") {
                                        val presetLot = lotSizes[selectedTickerOption] ?: 1
                                        lotMultiplier = presetLot
                                        val lots = lotsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        qtyText = DefaultLotSizes.computeQuantity(lots, presetLot).toString()
                                    }
                                }
                            )
                        }
                    }

                    // Segmented Buy / Sell Control
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(TradeSide.BUY to "Buy", TradeSide.SELL to "Sell").forEach { (side, label) ->
                                val isSelected = selectedSide == side
                                val activeColor = if (side == TradeSide.BUY) ProfitGreen else LossRose
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { selectedSide = side },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, activeColor.copy(alpha = 0.55f)) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2x2 Price & Size Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = entryPrice,
                            onValueChange = { entryPrice = it },
                            label = { Text("Entry ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_entry"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exitPrice,
                            onValueChange = { exitPrice = it },
                            label = { Text("Exit ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_exit"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isEquity) {
                            OutlinedTextField(
                                value = lotsText,
                                onValueChange = { next ->
                                    if (next.all { it.isDigit() }) {
                                        lotsText = next
                                        val l = next.toIntOrNull()
                                        if (l != null && l > 0 && lotMultiplier > 0) {
                                            qtyText = DefaultLotSizes.computeQuantity(l, lotMultiplier).toString()
                                        }
                                    }
                                },
                                label = { Text("Lots") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_lots"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { next ->
                                if (next.all { it.isDigit() }) {
                                    qtyText = next
                                    val q = next.toIntOrNull()
                                    if (q != null && q > 0 && !isEquity && lotMultiplier > 1 && q % lotMultiplier == 0) {
                                        lotsText = (q / lotMultiplier).toString()
                                    }
                                }
                            },
                            label = { Text(if (isEquity) "Shares (Qty)" else "Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_qty"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    if (!isEquity) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1, 2, 3, 5, 10).forEach { presetLots ->
                                val isPresetActive = parsedLots == presetLots && parsedQty == presetLots * lotMultiplier
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            lotsText = presetLots.toString()
                                            qtyText = DefaultLotSizes.computeQuantity(presetLots, lotMultiplier).toString()
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPresetActive) {
                                        PrimaryCobalt.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }
                                ) {
                                    Text(
                                        text = "${presetLots}L",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFont,
                                            fontSize = 11.sp,
                                            fontWeight = if (isPresetActive) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isPresetActive) PrimaryCobalt else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Compact Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        MinimalPillChip(
                            label = "Today",
                            selected = selectedDate == todayDate,
                            onClick = { selectedDate = todayDate },
                            modifier = Modifier.testTag("date_chip_today")
                        )
                        MinimalPillChip(
                            label = "Yesterday",
                            selected = selectedDate == yesterdayDate,
                            onClick = { selectedDate = yesterdayDate },
                            modifier = Modifier.testTag("date_chip_yesterday")
                        )
                        val isCustomDate = selectedDate != todayDate && selectedDate != yesterdayDate
                        MinimalPillChip(
                            label = if (isCustomDate) IndianMarketCalendar.formatReadableDate(selectedDate) else "Pick",
                            selected = isCustomDate,
                            onClick = { showDatePicker = true },
                            leadingIcon = Icons.Outlined.CalendarToday,
                            modifier = Modifier.testTag("date_chip_pick")
                        )
                    }
                }

                // 3. Calm Behavioral Reflection Card (Mindset + Slip + Quick Note)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Mindset before entry",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf("Calm", "Focused", "Rushed", "Anxious").forEach { mood ->
                                    val isPositiveMood = mood == "Calm" || mood == "Focused"
                                    val accent = if (isPositiveMood) PrimaryCobalt else GoldCommodity
                                    MinimalPillChip(
                                        label = mood,
                                        selected = emotionBefore == mood,
                                        onClick = { emotionBefore = mood },
                                        activeContainerColor = accent.copy(alpha = 0.12f),
                                        activeContentColor = accent,
                                        activeBorderColor = accent.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Any slip? (optional)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf("Entered early", "Moved stop", "Oversized").forEach { mistake ->
                                    MinimalPillChip(
                                        label = mistake,
                                        selected = selectedMistake == mistake,
                                        onClick = { selectedMistake = if (selectedMistake == mistake) "" else mistake },
                                        activeContainerColor = LossRose.copy(alpha = 0.1f),
                                        activeContentColor = LossRose,
                                        activeBorderColor = LossRose.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Quick note (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 1,
                            maxLines = 2
                        )
                    }
                }

                // Progressive Disclosure: Setup, Stop Loss, Risk Formula & Discipline Rating
                TradeOptionalDetailsSection(
                    expanded = showOptionalDetails,
                    onToggleExpanded = { showOptionalDetails = !showOptionalDetails },
                    setup = setup,
                    onSetupChange = { setup = it },
                    stopLossText = stopLossText,
                    onStopLossChange = { stopLossText = it },
                    targetText = targetText,
                    onTargetChange = { targetText = it },
                    rating = rating,
                    onRatingChange = { rating = it },
                    effectiveCapital = effectiveCapital,
                    selectedRiskPct = selectedRiskPct,
                    onSelectRiskPct = { selectedRiskPct = it },
                    parsedEntry = parsedEntry,
                    inferredUnitLot = inferredUnitLot,
                    onApplyFormulaSize = { recLots, recQty ->
                        lotsText = recLots.toString()
                        qtyText = recQty.toString()
                    }
                )
            }

            // Sticky Minimalist Submit Footer
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 0.75.dp, color = MaterialTheme.colorScheme.outline)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEquity) {
                                    "$parsedQty shares"
                                } else {
                                    "$parsedLots lot${if (parsedLots > 1) "s" else ""} · $parsedQty qty"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (previewResult != null) {
                                val netColor = if (previewResult.netPnl >= BigDecimal.ZERO) ProfitGreen else LossRose
                                Text(
                                    text = "Net ${Formatters.inr(previewResult.netPnl, showSign = true)} · Fees ${Formatters.inr(previewResult.charges.totalCharges)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFont,
                                        fontWeight = FontWeight.SemiBold,
                                        color = netColor
                                    ),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = "Enter prices for live net P&L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val entry = parsedEntry ?: return@Button
                                val exit = parsedExit ?: return@Button
                                val qty = parsedQty.coerceAtLeast(1)
                                val saveLots: Int
                                val saveLotSize: Int
                                if (isEquity) {
                                    saveLots = 1
                                    saveLotSize = qty
                                } else if (lotMultiplier > 1 && qty % lotMultiplier == 0) {
                                    saveLots = (qty / lotMultiplier).coerceAtLeast(1)
                                    saveLotSize = lotMultiplier
                                } else if (parsedLots > 0 && qty >= parsedLots && qty % parsedLots == 0) {
                                    saveLots = parsedLots
                                    saveLotSize = qty / parsedLots
                                } else {
                                    saveLots = 1
                                    saveLotSize = qty
                                }

                                val sl = stopLossText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val tgt = targetText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                val computedRisk = if (sl > BigDecimal.ZERO) {
                                    entry.subtract(sl).abs().multiply(BigDecimal.valueOf(qty.toLong()))
                                } else {
                                    initialTrade?.plannedRisk ?: BigDecimal("1000")
                                }
                                val exchange = when {
                                    selectedSegment == Segment.COMMODITY_FUT || selectedSegment == Segment.COMMODITY_OPT -> Exchange.MCX
                                    initialTrade != null && initialTrade.exchange != Exchange.MCX -> initialTrade.exchange
                                    symbol.uppercase().startsWith("SENSEX") || symbol.uppercase().startsWith("BANKEX") -> Exchange.BSE
                                    else -> Exchange.NSE
                                }

                                val tradeDate = selectedDate
                                val entryTime = initialTrade?.orders?.firstOrNull()?.datetime?.substringAfter("T", "09:30:00") ?: "09:30:00"
                                val exitTime = initialTrade?.orders?.lastOrNull()?.datetime?.substringAfter("T", "14:30:00") ?: "14:30:00"
                                val trade = Trade(
                                    id = initialTrade?.id ?: UUID.randomUUID().toString(),
                                    segment = selectedSegment,
                                    exchange = exchange,
                                    symbol = symbol.trim(),
                                    expiry = initialTrade?.expiry ?: "",
                                    strike = initialTrade?.strike ?: BigDecimal.ZERO,
                                    optionType = initialTrade?.optionType,
                                    side = selectedSide,
                                    orders = listOf(
                                        OrderFill(datetime = "${tradeDate}T${entryTime}", qty = qty.toLong(), price = entry),
                                        OrderFill(datetime = "${tradeDate}T${exitTime}", qty = qty.toLong(), price = exit)
                                    ),
                                    lots = saveLots,
                                    lotSize = saveLotSize,
                                    multiplier = initialTrade?.multiplier ?: BigDecimal.ONE,
                                    stopLoss = sl,
                                    target = tgt,
                                    plannedRisk = computedRisk,
                                    setup = setup.ifBlank { "Discretionary" },
                                    tags = initialTrade?.tags?.ifEmpty { listOf(selectedSegment.displayName) } ?: listOf(selectedSegment.displayName),
                                    emotionBefore = emotionBefore,
                                    emotionDuring = initialTrade?.emotionDuring ?: "Calm",
                                    emotionAfter = if ((exit - entry) * (if (selectedSide == TradeSide.BUY) BigDecimal.ONE else BigDecimal.ONE.negate()) >= BigDecimal.ZERO) "Satisfied" else "Reflective",
                                    mistakes = if (selectedMistake.isNotBlank()) listOf(selectedMistake) else emptyList(),
                                    rating = rating,
                                    notes = notes.trim(),
                                    screenshots = screenshots,
                                    brokerId = brokerId.uppercase(),
                                    date = tradeDate
                                )
                                if (selectedTickerOption == "Other" && saveCustomTicker) {
                                    val baseTicker = symbol.trim().substringBefore(" ").uppercase()
                                    if (baseTicker.length >= 2 && saveLotSize > 0) {
                                        onSaveCustomTicker(baseTicker, saveLotSize)
                                    }
                                }
                                onSaveTrade(trade)
                                onDismiss()
                            },
                            enabled = canSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_trade_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = if (initialTrade == null) "Submit Trade" else "Update Trade",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (canSave) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = IndianMarketCalendar.isoDateToUtcMillis(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = IndianMarketCalendar.utcMillisToIsoDate(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
