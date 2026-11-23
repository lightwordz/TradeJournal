package com.example.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LegalDocType(val title: String) {
    PRIVACY("Privacy Policy"),
    TERMS("Terms of Use"),
    DATA_AND_SDKS("Permissions & Data")
}

object FinanceComplianceCopy {
    const val DISCLAIMER_SHORT =
        "Record-keeping tool. Not investment advice. Not SEBI-registered. Calculations are estimates; verify with your broker's contract note."
    const val AFFILIATION_NOTICE =
        "Independent app — not affiliated with, endorsed by, or sponsored by NSE, BSE, MCX, Zerodha, Upstox, Groww, or Angel One."
    const val READ_ONLY_IMPORT_NOTICE =
        "CSV import only reads files you choose. Any future broker sync integration will be strictly read-only (no order placement or fund access)."
    const val AI_SYSTEM_PROMPT_POLICY =
        "Informational historical summary only. Never recommend securities, never predict prices, and never tell users what to buy, sell, or hold."
    const val GRIEVANCE_EMAIL = "sauravojha124@gmail.com"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentSheet(
    docType: LegalDocType,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = docType.title,
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (docType) {
                    LegalDocType.PRIVACY -> {
                        LegalSection(
                            heading = "1. Offline-First & Zero Server Collection",
                            body = "TradeJournal stores all trade entries, capital ledger logs, custom fee presets, and attached chart screenshots exclusively in your device's private internal storage (Room SQLite database and app files directory). We do not operate user accounts or upload your trading logs to any cloud server."
                        )
                        LegalSection(
                            heading = "2. Purpose & Data Stored Locally",
                            body = "Data you enter—display name, starting capital, daily loss threshold, broker fee preset, order prices, setup tags, mood/rating notes, and optional chart images—is processed strictly on-device for your personal trading diary, fee estimates, and descriptive analytics."
                        )
                        LegalSection(
                            heading = "3. Storage, Retention & Deletion (DPDP Act 2023)",
                            body = "Your data stays on your device until you delete individual trades, tap 'Delete All Local Data' in Settings (which erases all trades, capital entries, and screenshot files), or uninstall the app. Unencrypted cloud backup of the local database and chart screenshots is disabled in backup rules."
                        )
                        LegalSection(
                            heading = "4. On-Device Libraries & Third Parties",
                            body = "No advertising SDKs, analytics trackers, or third-party ad networks are included. Optional screenshot text scanning uses bundled on-device ML Kit Latin Text Recognition purely to pre-fill numeric fields locally on your phone (informational only; verify manually). CSV exports happen only when you explicitly choose a destination via Android Files."
                        )
                        LegalSection(
                            heading = "5. Contact & Grievance Officer",
                            body = "For privacy requests, corrections, or Digital Personal Data Protection Act (DPDP 2023) inquiries, contact the Grievance Officer at ${FinanceComplianceCopy.GRIEVANCE_EMAIL}."
                        )
                    }
                    LegalDocType.TERMS -> {
                        LegalSection(
                            heading = "1. Record-Keeping Utility Only",
                            body = "TradeJournal is a personal trading diary, analytics, and record-keeping tool only. It does not execute trades, hold funds, or provide tips, signals, or predictions. ${FinanceComplianceCopy.DISCLAIMER_SHORT}"
                        )
                        LegalSection(
                            heading = "2. No Advisory or Research Services",
                            body = "Nothing inside TradeJournal—including fee estimates, historical win-rate breakdowns, risk-math formulas, or informational summaries—constitutes investment advice, buy/sell signals, research reports, or portfolio management under SEBI regulations. All trading decisions are solely yours."
                        )
                        LegalSection(
                            heading = "3. Unaffiliated & Read-Only Imports",
                            body = "${FinanceComplianceCopy.AFFILIATION_NOTICE} Brokerage, STT, CTT, SEBI fees, stamp duty, exchange turnover charges, and GST rates change periodically and are provided only as editable estimates; always reconcile against your broker's official contract note. ${FinanceComplianceCopy.READ_ONLY_IMPORT_NOTICE}"
                        )
                        LegalSection(
                            heading = "4. Data Responsibility & Age",
                            body = "TradeJournal is intended for adults (18+). Because records remain solely on your device, please export CSV backups regularly before resetting or switching phones."
                        )
                    }
                    LegalDocType.DATA_AND_SDKS -> {
                        LegalSection(
                            heading = "Android Permissions",
                            body = "• Zero dangerous runtime permissions requested (no Camera, Location, Contacts, Storage, or Notification permission prompts).\n• Chart attachment uses the Android Photo Picker (grants temporary access only to the single image you pick).\n• CSV import & export use the Android Storage Access Framework (read-only access to the single file you select or create).\n• INTERNET & ACCESS_NETWORK_STATE (normal install-time permissions): Merged solely by Google ML Kit / DataTransport dependencies; TradeJournal itself makes zero network calls."
                        )
                        LegalSection(
                            heading = "Bundled Libraries & Data Touched",
                            body = "• Jetpack Room (SQLite): Stores trade records, preferences, and capital ledger entries locally.\n• Coil: Displays chart screenshots saved inside app-private storage.\n• ML Kit Text Recognition (On-Device): Reads text locally from screenshots you attach to pre-fill order fields."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
