package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TraderPreferences
import com.example.engine.BrokerRateProfile
import com.example.model.CapitalEntry
import com.example.ui.components.EditorialCard
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.math.BigDecimal

@Composable
fun ProfileScreen(
    preferences: TraderPreferences,
    effectiveCapital: BigDecimal,
    capitalEntries: List<CapitalEntry>,
    tradesCount: Int,
    onUpdateTraderName: (String) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onUpdateCapital: (BigDecimal) -> Unit,
    onUpdateDailyLimit: (BigDecimal) -> Unit,
    onUpdateBroker: (String) -> Unit,
    onUpdateCustomRates: (BrokerRateProfile?) -> Unit,
    onUpdateLotSizes: (Map<String, Int>) -> Unit,
    onAddCapitalEntry: (CapitalEntry) -> Unit,
    onDeleteCapitalEntry: (String) -> Unit,
    onExportCsv: () -> Unit,
    onClearAll: () -> Unit
) {
    var editingName by remember { mutableStateOf(false) }
    var editingCapital by remember { mutableStateOf(false) }
    var editingLimit by remember { mutableStateOf(false) }
    var editingBroker by remember { mutableStateOf(false) }
    var showFeeEditor by remember { mutableStateOf(false) }
    var showLotEditor by remember { mutableStateOf(false) }
    var showCapitalLedger by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var activeLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val horizontalPad = when {
            maxWidth >= 840.dp -> 28.dp
            maxWidth >= 600.dp -> 24.dp
            maxWidth < 360.dp -> 16.dp
            else -> 20.dp
        }
        val bottomClearance = if (maxWidth >= 600.dp) 32.dp else 96.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPad,
                end = horizontalPad,
                top = 12.dp,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Account, broker & risk limits",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

        // Account Card (tappable to edit display name)
        item {
            val displayName = preferences.traderName.ifBlank { "Trader" }
            EditorialCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { editingName = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initials = displayName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .ifBlank { "T" }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.background,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Net capital ${Formatters.inr(effectiveCapital)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.5.sp,
                                    fontFamily = JetBrainsMonoFont
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit your name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Capital & Risk
        item {
            SettingSection(title = "CAPITAL & RISK") {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        label = "Starting Capital",
                        value = Formatters.inr(preferences.startingCapital),
                        onClick = { editingCapital = true }
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Deposits & Withdrawals",
                        value = when (capitalEntries.size) {
                            0 -> "None"
                            1 -> "1 entry"
                            else -> "${capitalEntries.size} entries"
                        },
                        onClick = { showCapitalLedger = true }
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Daily Loss Limit",
                        value = Formatters.inr(preferences.dailyLossLimit),
                        onClick = { editingLimit = true }
                    )
                }
            }
        }

        // Broker & Charges
        item {
            SettingSection(title = "BROKER & CHARGES") {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        label = "Primary Broker",
                        value = preferences.defaultBroker,
                        onClick = { editingBroker = true }
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Brokerage & Taxes",
                        value = if (preferences.customRateProfile != null) "Custom" else "Standard",
                        onClick = { showFeeEditor = true },
                        testTag = "row_broker_fee_editor"
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Contract Lot Sizes",
                        value = "${preferences.lotSizes.size} tickers",
                        onClick = { showLotEditor = true },
                        testTag = "row_lot_size_editor"
                    )
                }
            }
        }

        // Appearance
        item {
            SettingSection(title = "APPEARANCE") {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onToggleTheme(!preferences.isDarkTheme) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )
                        Switch(
                            checked = preferences.isDarkTheme,
                            onCheckedChange = onToggleTheme,
                            modifier = Modifier.testTag("theme_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryCobalt
                            )
                        )
                    }
                }
            }
        }

        // Data
        item {
            SettingSection(title = "DATA") {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    ActionSettingRow(
                        label = "Export Trades (CSV)",
                        trailingText = when (tradesCount) {
                            0 -> "No trades"
                            1 -> "1 trade"
                            else -> "$tradesCount trades"
                        },
                        icon = Icons.Outlined.Download,
                        enabled = tradesCount > 0,
                        onClick = onExportCsv,
                        testTag = "export_csv_button"
                    )
                    SettingsDivider()
                    ActionSettingRow(
                        label = "Delete All Local Data",
                        icon = Icons.Outlined.DeleteOutline,
                        tint = LossRose,
                        enabled = tradesCount > 0 || capitalEntries.isNotEmpty(),
                        onClick = { confirmClearAll = true },
                        testTag = "clear_all_button"
                    )
                }
            }
        }

        // Legal & Privacy
        item {
            SettingSection(title = "LEGAL & PRIVACY") {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        label = "Privacy Policy",
                        value = "View",
                        onClick = { activeLegalDoc = LegalDocType.PRIVACY },
                        testTag = "row_privacy_policy"
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Terms of Use",
                        value = "View",
                        onClick = { activeLegalDoc = LegalDocType.TERMS },
                        testTag = "row_terms_of_use"
                    )
                    SettingsDivider()
                    SettingRow(
                        label = "Permissions & Data",
                        value = "View",
                        onClick = { activeLegalDoc = LegalDocType.DATA_AND_SDKS },
                        testTag = "row_permissions_data"
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = FinanceComplianceCopy.DISCLAIMER_SHORT,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${FinanceComplianceCopy.AFFILIATION_NOTICE} Does not execute trades or hold funds.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "All records stay on-device. CSV import is read-only; any future broker sync will be strictly read-only.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Delete all local data?") },
            text = {
                Text("This permanently erases all logged trades, capital ledger entries, and attached chart images from this device. Export a CSV first if you want a backup.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        onClearAll()
                    }
                ) {
                    Text("Delete All", color = LossRose, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }

    if (editingName) {
        NameInputDialog(
            initial = preferences.traderName,
            onDismiss = { editingName = false },
            onSave = {
                onUpdateTraderName(it)
                editingName = false
            }
        )
    }

    if (editingCapital) {
        NumberInputDialog(
            title = "Starting Capital",
            initial = preferences.startingCapital.toPlainString(),
            onDismiss = { editingCapital = false },
            onSave = {
                onUpdateCapital(it)
                editingCapital = false
            }
        )
    }

    if (editingLimit) {
        NumberInputDialog(
            title = "Daily Loss Limit",
            initial = preferences.dailyLossLimit.toPlainString(),
            onDismiss = { editingLimit = false },
            onSave = {
                onUpdateDailyLimit(it)
                editingLimit = false
            }
        )
    }

    if (editingBroker) {
        AlertDialog(
            onDismissRequest = { editingBroker = false },
            title = { Text("Select Broker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Zerodha", "Upstox", "Groww", "Angel One").forEach { broker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onUpdateBroker(broker)
                                    editingBroker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(broker, style = MaterialTheme.typography.bodyLarge)
                            if (preferences.defaultBroker == broker) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = PrimaryCobalt
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingBroker = false }) { Text("Close") }
            }
        )
    }

    if (showFeeEditor) {
        BrokerFeeEditorSheet(
            activeBroker = preferences.defaultBroker,
            currentProfile = preferences.activeRateProfile,
            onDismiss = { showFeeEditor = false },
            onSave = { profile ->
                onUpdateCustomRates(profile)
                showFeeEditor = false
            },
            onResetDefaults = {
                onUpdateCustomRates(null)
                showFeeEditor = false
            }
        )
    }

    if (showLotEditor) {
        LotSizeEditorSheet(
            lotSizes = preferences.lotSizes,
            onDismiss = { showLotEditor = false },
            onSave = { updated ->
                onUpdateLotSizes(updated)
                showLotEditor = false
            }
        )
    }

    if (showCapitalLedger) {
        CapitalLedgerSheet(
            entries = capitalEntries,
            effectiveCapital = effectiveCapital,
            onDismiss = { showCapitalLedger = false },
            onAddEntry = onAddCapitalEntry,
            onDeleteEntry = onDeleteCapitalEntry
        )
    }

    activeLegalDoc?.let { doc ->
        LegalDocumentSheet(
            docType = doc,
            onDismiss = { activeLegalDoc = null }
        )
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionSettingRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingText: String? = null,
    tint: Color? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val labelColor = tint ?: MaterialTheme.colorScheme.onSurface
    val rowAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(rowAlpha)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
