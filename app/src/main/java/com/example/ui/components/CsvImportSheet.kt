package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Trade
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.ProfitGreen
import com.example.utils.BrokerPreset
import com.example.utils.CsvImporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportSheet(
    existingTrades: List<Trade>,
    onDismiss: () -> Unit,
    onImportTrades: (List<Trade>) -> Unit
) {
    val context = LocalContext.current
    var selectedPreset by remember { mutableStateOf(BrokerPreset.AUTO) }
    var csvText by remember { mutableStateOf("") }
    var fileLabel by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                csvText = content
                fileLabel = uri.lastPathSegment ?: "Selected CSV"
            }
        }
    }

    val preview = remember(csvText, existingTrades, selectedPreset) {
        if (csvText.isBlank()) null else CsvImporter.parseCsv(csvText, existingTrades, selectedPreset)
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        text = "Import Trades",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Read-only file import (Zerodha, Upstox, Groww, Angel One formats — not affiliated)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Broker Format Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrokerPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { selectedPreset = preset },
                        label = { Text(preset.label) }
                    )
                }
            }

            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pick_csv_file_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = PrimaryCobalt)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fileLabel ?: "Choose CSV File from Device",
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedTextField(
                value = csvText,
                onValueChange = { csvText = it },
                label = { Text("Or paste CSV rows here") },
                placeholder = {
                    Text("Date,Symbol,Side,Qty,Entry,Exit\n2026-03-07,INFY,BUY,20,1620,1645")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("csv_paste_input"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 6
            )

            if (preview != null) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${preview.importedTrades.size} new trade${if (preview.importedTrades.size == 1) "" else "s"} ready to import",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (preview.importedTrades.isNotEmpty()) ProfitGreen else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (preview.duplicateCount > 0 || preview.skippedRows > 0) {
                            Text(
                                text = "${preview.duplicateCount} duplicate${if (preview.duplicateCount == 1) "" else "s"} skipped · ${preview.skippedRows} invalid rows",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val canImport = (preview?.importedTrades?.isNotEmpty() == true)
            Button(
                onClick = {
                    val toSave = preview?.importedTrades.orEmpty()
                    if (toSave.isNotEmpty()) {
                        onImportTrades(toSave)
                        onDismiss()
                    }
                },
                enabled = canImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_csv_import_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(
                    text = "Import ${preview?.importedTrades?.size ?: 0} Trades",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}
