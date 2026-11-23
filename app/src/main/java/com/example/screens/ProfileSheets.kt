package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrokerRateProfile
import com.example.engine.DefaultLotSizes
import com.example.model.CapitalEntry
import com.example.model.CapitalEntryType
import com.example.ui.theme.*
import com.example.utils.Formatters
import com.example.utils.IndianMarketCalendar
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CapitalLedgerSheet(
    entries: List<CapitalEntry>,
    effectiveCapital: BigDecimal,
    onDismiss: () -> Unit,
    onAddEntry: (CapitalEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var entryType by remember { mutableStateOf(CapitalEntryType.DEPOSIT) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    val today = remember { IndianMarketCalendar.todayIstDateString() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Capital Ledger",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Current Balance: ${Formatters.inr(effectiveCapital)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFont,
                    color = PrimaryCobalt
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CapitalEntryType.entries.forEach { t ->
                    FilterChip(
                        selected = entryType == t,
                        onClick = { entryType = t },
                        label = { Text(t.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (opt)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    val amt = amountText.toBigDecimalOrNull() ?: return@Button
                    if (amt <= BigDecimal.ZERO) return@Button
                    onAddEntry(
                        CapitalEntry(
                            type = entryType,
                            amount = amt,
                            date = today,
                            note = noteText.trim()
                        )
                    )
                    amountText = ""
                    noteText = ""
                },
                enabled = (amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text("Add ${entryType.label}", color = MaterialTheme.colorScheme.background)
            }

            if (entries.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${entry.type.label} · ${entry.date}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isDep = entry.type == CapitalEntryType.DEPOSIT
                            Text(
                                text = "${if (isDep) "+" else "-"}${Formatters.inr(entry.amount)}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (isDep) ProfitGreen else LossRose,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            IconButton(
                                onClick = { onDeleteEntry(entry.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrokerFeeEditorSheet(
    activeBroker: String,
    currentProfile: BrokerRateProfile,
    onDismiss: () -> Unit,
    onSave: (BrokerRateProfile) -> Unit,
    onResetDefaults: () -> Unit
) {
    var optFlat by remember { mutableStateOf(currentProfile.fnoOptionsFlatPerOrder.toPlainString()) }
    var futFlat by remember { mutableStateOf(currentProfile.fnoFuturesFlatPerOrder.toPlainString()) }
    var intradayPct by remember {
        mutableStateOf(
            currentProfile.eqIntradayBrokeragePct.multiply(BigDecimal("100")).stripTrailingZeros().toPlainString()
        )
    }
    var intradayMax by remember { mutableStateOf(currentProfile.eqIntradayFlatPerOrder.toPlainString()) }
    var deliveryPct by remember {
        mutableStateOf(
            currentProfile.eqDeliveryBrokeragePct.multiply(BigDecimal("100")).stripTrailingZeros().toPlainString()
        )
    }
    var dpCharge by remember { mutableStateOf(currentProfile.dpChargesPerSell.toPlainString()) }
    var commFlat by remember { mutableStateOf(currentProfile.commodityFlatPerOrder.toPlainString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Brokerage & Taxes ($activeBroker)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Customize brokerage rates for $activeBroker. Changes update every trade immediately.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = optFlat,
                    onValueChange = { optFlat = it },
                    label = { Text("Options ₹/order") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = futFlat,
                    onValueChange = { futFlat = it },
                    label = { Text("Futures ₹/order") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = intradayPct,
                    onValueChange = { intradayPct = it },
                    label = { Text("Intraday %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = intradayMax,
                    onValueChange = { intradayMax = it },
                    label = { Text("Intraday Cap ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = deliveryPct,
                    onValueChange = { deliveryPct = it },
                    label = { Text("Delivery %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dpCharge,
                    onValueChange = { dpCharge = it },
                    label = { Text("DP Sell ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = commFlat,
                onValueChange = { commFlat = it },
                label = { Text("Commodity ₹/order") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Reset Defaults", color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = {
                        val hundred = BigDecimal("100")
                        val updated = currentProfile.copy(
                            brokerName = activeBroker,
                            fnoOptionsFlatPerOrder = optFlat.toBigDecimalOrNull() ?: currentProfile.fnoOptionsFlatPerOrder,
                            fnoFuturesFlatPerOrder = futFlat.toBigDecimalOrNull() ?: currentProfile.fnoFuturesFlatPerOrder,
                            eqIntradayBrokeragePct = (intradayPct.toBigDecimalOrNull()?.divide(hundred)) ?: currentProfile.eqIntradayBrokeragePct,
                            eqIntradayFlatPerOrder = intradayMax.toBigDecimalOrNull() ?: currentProfile.eqIntradayFlatPerOrder,
                            eqDeliveryBrokeragePct = (deliveryPct.toBigDecimalOrNull()?.divide(hundred)) ?: currentProfile.eqDeliveryBrokeragePct,
                            dpChargesPerSell = dpCharge.toBigDecimalOrNull() ?: currentProfile.dpChargesPerSell,
                            commodityFlatPerOrder = commFlat.toBigDecimalOrNull() ?: currentProfile.commodityFlatPerOrder
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_broker_fees_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("Save Rates", color = MaterialTheme.colorScheme.background, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LotSizeEditorSheet(
    lotSizes: Map<String, Int>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Int>) -> Unit
) {
    val editableMap = remember(lotSizes) {
        mutableStateMapOf<String, String>().apply {
            lotSizes.forEach { (k, v) -> put(k, v.toString()) }
        }
    }
    var newTicker by remember { mutableStateOf("") }
    var newLotSize by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Contract Lot Sizes",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Update exchange lot sizes or add a custom ticker manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            editableMap.keys.toList().forEach { ticker ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticker,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedTextField(
                        value = editableMap[ticker] ?: "",
                        onValueChange = { next ->
                            if (next.all { it.isDigit() }) editableMap[ticker] = next
                        },
                        label = { Text("Lot") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(104.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { editableMap.remove(ticker) },
                        enabled = editableMap.size > 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove $ticker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Add Custom Ticker",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryCobalt
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTicker,
                    onValueChange = { newTicker = it.uppercase() },
                    label = { Text("Ticker (e.g. BANKEX)") },
                    modifier = Modifier.weight(1.2f).testTag("custom_ticker_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newLotSize,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) newLotSize = it },
                    label = { Text("Lot Size") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f).testTag("custom_lot_size_input"),
                    singleLine = true
                )
            }
            OutlinedButton(
                onClick = {
                    val clean = newTicker.trim().uppercase()
                    val size = newLotSize.toIntOrNull() ?: 0
                    if (clean.isNotEmpty() && size > 0) {
                        editableMap[clean] = size.toString()
                        newTicker = ""
                        newLotSize = ""
                    }
                },
                enabled = newTicker.isNotBlank() && (newLotSize.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_custom_ticker_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("Add to Ticker List", color = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        editableMap.clear()
                        DefaultLotSizes.seeded().forEach { (k, v) -> editableMap[k] = v.toString() }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Reset Defaults", color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = {
                        val finalMap = linkedMapOf<String, Int>()
                        editableMap.forEach { (k, v) ->
                            val parsed = v.toIntOrNull()
                            if (k.isNotBlank() && parsed != null && parsed > 0) {
                                finalMap[k.trim().uppercase()] = parsed
                            }
                        }
                        if (finalMap.isNotEmpty()) {
                            onSave(finalMap)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_lot_sizes_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("Save Lot Sizes", color = MaterialTheme.colorScheme.background, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
internal fun NameInputDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Display name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.trim().isNotEmpty()) onSave(text.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun NumberInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (BigDecimal) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Amount in ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = text.toBigDecimalOrNull()
                    if (value != null && value > BigDecimal.ZERO) {
                        onSave(value)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
