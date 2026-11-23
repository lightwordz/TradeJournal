package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.MatchingMethod
import com.example.model.*
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.io.File
import java.math.BigDecimal

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun LedgerTradeRow(
    trade: Trade,
    result: TradeResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWin = result.netPnl >= BigDecimal.ZERO
    val accent = if (isWin) ProfitGreen else LossRose
    val entryPrice = trade.orders.firstOrNull()?.price ?: BigDecimal.ZERO
    val exitPrice = trade.orders.lastOrNull()?.price ?: BigDecimal.ZERO
    val hasPricePair = trade.orders.size >= 2 && entryPrice > BigDecimal.ZERO
    val isDerivative = trade.segment != Segment.EQ_INTRADAY && trade.segment != Segment.EQ_DELIVERY
    val totalQty = trade.lots * trade.lotSize
    val lotLabel = if (isDerivative) {
        "${trade.side.displayName} · ${trade.lots} lot${if (trade.lots > 1) "s" else ""} ($totalQty)"
    } else {
        "${trade.side.displayName} · $totalQty qty"
    }
    // Keep front row clean: segment + logged prices only; strategy & notes stay inside detail sheet
    val loggedPriceLine = if (hasPricePair) {
        "${trade.segment.displayName} · ${Formatters.compactPrice(entryPrice)} → ${Formatters.compactPrice(exitPrice)}"
    } else {
        trade.segment.displayName
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("trade_card_${trade.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimalist activity icon replaces vertical profit/loss lines & letter badges
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = trade.symbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        letterSpacing = (-0.15).sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trade.screenshots.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Chart screenshot attached",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = loggedPriceLine,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = Formatters.inr(result.netPnl, showSign = true),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontFamily = JetBrainsMonoFont,
                    fontWeight = FontWeight.Bold,
                    color = accent
                ),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = lotLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    fontFamily = JetBrainsMonoFont
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDetailSheet(
    trade: Trade,
    brokerProfile: BrokerRateProfile,
    matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    onDismiss: () -> Unit,
    onEdit: (Trade) -> Unit,
    onDelete: (String) -> Unit
) {
    val result = remember(trade, brokerProfile, matchingMethod) {
        CalculationEngine.calculateTradeResult(trade, brokerProfile, matchingMethod)
    }
    val isWin = result.netPnl >= BigDecimal.ZERO
    val accent = if (isWin) ProfitGreen else LossRose
    val entryPrice = trade.orders.firstOrNull()?.price ?: BigDecimal.ZERO
    val exitPrice = trade.orders.lastOrNull()?.price ?: BigDecimal.ZERO
    var previewScreenshot by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = trade.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${trade.exchange.displayName} · ${trade.segment.displayName} · ${trade.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = {
                            onDismiss()
                            onEdit(trade)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit trade",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            onDelete(trade.id)
                            onDismiss()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete trade",
                            tint = LossRose
                        )
                    }
                }
            }

            // Net Result Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isWin) ProfitGreen.copy(alpha = 0.08f) else LossRose.copy(alpha = 0.08f),
                border = BorderStroke(0.75.dp, accent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.inr(result.netPnl, showSign = true, showDecimals = true),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (star in 1..5) {
                            Icon(
                                imageVector = if (star <= trade.rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (star <= trade.rating) GoldCommodity else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Logged Price Strip
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = Formatters.inr(entryPrice, showDecimals = true),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFont, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = Formatters.inr(exitPrice, showDecimals = true),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFont, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.75f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${trade.lots * trade.lotSize}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFont, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Taxes & Fees Breakdown
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Taxes & Fees",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DetailLine("Gross Profit", Formatters.inr(result.grossPnl, showSign = true, showDecimals = true))
                    DetailLine("Brokerage", Formatters.inr(result.charges.brokerage, showDecimals = true))
                    DetailLine("Securities Tax", Formatters.inr(result.charges.stt, showDecimals = true))
                    DetailLine("Exchange & SEBI", Formatters.inr(result.charges.exchangeCharges.add(result.charges.sebiFee), showDecimals = true))
                    DetailLine("Stamp Duty & GST", Formatters.inr(result.charges.stampDuty.add(result.charges.gst), showDecimals = true))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    DetailLine("Total Fees", Formatters.inr(result.charges.totalCharges, showDecimals = true), bold = true)
                    if (trade.stopLoss > BigDecimal.ZERO || trade.target > BigDecimal.ZERO) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        DetailLine(
                            label = "R-Multiple / Plan R:R",
                            value = "${if (result.rMultiple >= BigDecimal.ZERO) "+" else ""}${result.rMultiple}R · 1:${result.riskReward}"
                        )
                    }
                    Text(
                        text = "Estimated using ${brokerProfile.brokerName} rates. Verify against contract note.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trade.notes.isNotBlank() || trade.emotionBefore.isNotBlank()) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notes & Mindset",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (trade.setup.isNotBlank()) {
                            DetailLine("Strategy", trade.setup)
                        }
                        if (trade.emotionBefore.isNotBlank()) {
                            DetailLine("Mindset", "${trade.emotionBefore} → ${trade.emotionAfter}")
                        }
                        if (trade.mistakes.isNotEmpty()) {
                            DetailLine("Habits Flagged", trade.mistakes.joinToString(", "))
                        }
                        if (trade.notes.isNotBlank()) {
                            Text(
                                text = trade.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (trade.screenshots.isNotEmpty()) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Screenshots",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            trade.screenshots.forEach { path ->
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Trade screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 132.dp, height = 88.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable { previewScreenshot = path }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    previewScreenshot?.let { path ->
        Dialog(onDismissRequest = { previewScreenshot = null }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Full screenshot preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { previewScreenshot = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close preview",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.1f, fill = false)
        )
    }
}
