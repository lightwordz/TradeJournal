package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.CalculationEngine
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.io.File
import java.math.BigDecimal

@Composable
internal fun AttachChartCard(
    screenshots: List<String>,
    isScanning: Boolean,
    onPickChart: () -> Unit,
    onRemoveScreenshot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasCharts = screenshots.isNotEmpty()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isScanning) { onPickChart() }
            .testTag("screenshot_scan_card"),
        shape = RoundedCornerShape(14.dp),
        color = if (hasCharts) {
            PrimaryCobalt.copy(alpha = 0.04f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            0.75.dp,
            if (hasCharts) PrimaryCobalt.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryCobalt.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Attach chart",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = when {
                                isScanning -> "Reading chart screenshot…"
                                hasCharts -> "${screenshots.size} chart${if (screenshots.size > 1) "s" else ""} attached"
                                else -> "Attach chart"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasCharts) {
                                "On-device scan only — check values before saving"
                            } else {
                                "On-device scan pre-fills prices (verify manually)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryCobalt.copy(alpha = 0.1f),
                    modifier = Modifier.testTag("scan_screenshot_button")
                ) {
                    Text(
                        text = if (hasCharts) "+ Add" else "Choose",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryCobalt
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PrimaryCobalt
                )
            }

            if (hasCharts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screenshots.forEach { path ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Attached trade chart",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { onRemoveScreenshot(path) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(22.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove chart",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MinimalPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    activeContainerColor: Color = MaterialTheme.colorScheme.onBackground,
    activeContentColor: Color = MaterialTheme.colorScheme.background,
    activeBorderColor: Color? = null
) {
    Surface(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeContainerColor else MaterialTheme.colorScheme.surface,
        border = when {
            selected && activeBorderColor != null -> BorderStroke(0.75.dp, activeBorderColor)
            selected -> null
            else -> BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun TradeOptionalDetailsSection(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    setup: String,
    onSetupChange: (String) -> Unit,
    stopLossText: String,
    onStopLossChange: (String) -> Unit,
    targetText: String,
    onTargetChange: (String) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    effectiveCapital: BigDecimal,
    selectedRiskPct: BigDecimal,
    onSelectRiskPct: (BigDecimal) -> Unit,
    parsedEntry: BigDecimal?,
    inferredUnitLot: Int,
    onApplyFormulaSize: (lots: Int, qty: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleExpanded() }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup, Stop Loss & Rating (optional)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    color = PrimaryCobalt,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = "Toggle optional details",
                tint = PrimaryCobalt,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = setup,
                    onValueChange = onSetupChange,
                    label = { Text("Setup / Strategy") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = onStopLossChange,
                        label = { Text("Stop Loss ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = onTargetChange,
                        label = { Text("Target ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                val parsedSl = stopLossText.toBigDecimalOrNull()
                val riskFormula = remember(
                    effectiveCapital, selectedRiskPct, parsedEntry, parsedSl, inferredUnitLot
                ) {
                    if (parsedEntry == null || parsedSl == null || parsedSl <= BigDecimal.ZERO) null
                    else CalculationEngine.calculateRiskQuantityFormula(
                        capital = effectiveCapital,
                        riskPercent = selectedRiskPct,
                        entryPrice = parsedEntry,
                        stopLossPrice = parsedSl,
                        lotSize = inferredUnitLot.coerceAtLeast(1)
                    )
                }
                if (riskFormula != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryCobalt.copy(alpha = 0.06f),
                        border = BorderStroke(0.75.dp, PrimaryCobalt.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Risk Formula (${selectedRiskPct.stripTrailingZeros().toPlainString()}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryCobalt
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(BigDecimal("0.5"), BigDecimal("1.0"), BigDecimal("2.0")).forEach { pct ->
                                        val isPicked = selectedRiskPct.compareTo(pct) == 0
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onSelectRiskPct(pct) },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isPicked) PrimaryCobalt else MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = "${pct.stripTrailingZeros().toPlainString()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (riskFormula.calculatedLots > 0) {
                                        "Formula: ${riskFormula.calculatedLots}L (${riskFormula.calculatedQty} qty) · Risk ${Formatters.inr(riskFormula.estimatedRiskRupees)}"
                                    } else {
                                        "1 lot risk exceeds ${Formatters.inr(riskFormula.maxRiskRupees)} cap"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFont,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (riskFormula.calculatedLots > 0) {
                                    TextButton(
                                        onClick = {
                                            onApplyFormulaSize(
                                                riskFormula.calculatedLots,
                                                riskFormula.calculatedQty
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Use Qty",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = PrimaryCobalt,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discipline Rating",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { onRatingChange(star) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) GoldCommodity else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
