package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.PrimaryCobalt
import java.math.BigDecimal

@Composable
fun OnboardingScreen(
    onComplete: (name: String, broker: String, capital: BigDecimal) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var selectedBroker by rememberSaveable { mutableStateOf("Zerodha") }
    var capitalInput by rememberSaveable { mutableStateOf("500000") }
    var acceptedDisclaimer by rememberSaveable { mutableStateOf(false) }
    var activeLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }

    BackHandler(enabled = step > 1) {
        step -= 1
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val horizontalPad = if (maxWidth < 360.dp) 18.dp else 24.dp
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .padding(horizontal = horizontalPad, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(
                        onClick = { step -= 1 },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Text(
                        text = "TRADEJOURNAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryCobalt
                        )
                    )
                }

                if (step > 1) {
                    TextButton(
                        onClick = {
                            val finalName = nameInput.trim().ifBlank { "Trader" }
                            val cap = capitalInput.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
                                ?: BigDecimal("500000")
                            onComplete(finalName, selectedBroker, cap)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // 3-segment hairline progress (1: First-run Disclaimer, 2: Name, 3: Fee & Capital Preset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..3).forEach { seg ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (step >= seg) PrimaryCobalt else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
        }

        // Single-focus progressive step content
        AnimatedContent(
            targetState = step,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            transitionSpec = {
                (fadeIn() + slideInHorizontally { if (targetState > initialState) it / 4 else -it / 4 })
                    .togetherWith(fadeOut() + slideOutHorizontally { if (targetState > initialState) -it / 4 else it / 4 })
            },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal trading diary\nonly",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = FinanceComplianceCopy.DISCLAIMER_SHORT,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "• Records, calculates fees, and summarizes your own logged trades offline.\n• Does not execute trades, hold funds, or provide tips, signals, or buy/sell advice.\n• ${FinanceComplianceCopy.AFFILIATION_NOTICE}\n• ${FinanceComplianceCopy.READ_ONLY_IMPORT_NOTICE}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { acceptedDisclaimer = !acceptedDisclaimer }
                                    .testTag("disclaimer_checkbox_row"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = acceptedDisclaimer,
                                    onCheckedChange = { acceptedDisclaimer = it },
                                    modifier = Modifier.testTag("disclaimer_checkbox")
                                )
                                Text(
                                    text = "I understand this is a record-keeping tool only and not investment advice.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { activeLegalDoc = LegalDocType.PRIVACY },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Text(
                                        text = "Privacy Policy",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = PrimaryCobalt,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                TextButton(
                                    onClick = { activeLegalDoc = LegalDocType.TERMS },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Text(
                                        text = "Terms of Use",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = PrimaryCobalt,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "What should we\ncall you?",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your private local diary for historical trade review and fee estimates.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = {
                            Text(
                                text = "Your first name",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (nameInput.isNotBlank()) step = 3
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val greetName = nameInput.trim().substringBefore(" ").ifBlank { "there" }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nice to meet you,\n$greetName",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Pick your broker & starting capital. You can fine-tune fees or lot sizes anytime in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "PRIMARY BROKER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Zerodha", "Upstox", "Groww", "Angel One").forEach { broker ->
                                val selected = selectedBroker == broker
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedBroker = broker },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) PrimaryCobalt else MaterialTheme.colorScheme.surface,
                                    border = if (selected) null else BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = broker,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "TRADING CAPITAL (₹)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = capitalInput,
                            onValueChange = { next ->
                                if (next.all { it.isDigit() }) capitalInput = next
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.SemiBold
                            ),
                            prefix = { Text("₹ ", style = MaterialTheme.typography.titleLarge) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_capital_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "₹1L" to "100000",
                                "₹2.5L" to "250000",
                                "₹5L" to "500000",
                                "₹10L" to "1000000"
                            ).forEach { (label, raw) ->
                                val isMatch = capitalInput == raw
                                FilterChip(
                                    selected = isMatch,
                                    onClick = { capitalInput = raw },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Plain-text broker fee presets (not affiliated with any broker).",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }
        }

        // Anchored Primary CTA + persistent compact legal note
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when (step) {
                        1 -> step = 2
                        2 -> step = 3
                        else -> {
                            val finalName = nameInput.trim().ifBlank { "Trader" }
                            val cap = capitalInput.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
                                ?: BigDecimal("500000")
                            onComplete(finalName, selectedBroker, cap)
                        }
                    }
                },
                enabled = when (step) {
                    1 -> acceptedDisclaimer
                    2 -> nameInput.trim().isNotEmpty()
                    else -> true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_continue_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCobalt,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when (step) {
                        1 -> "I Understand & Continue"
                        2 -> "Continue"
                        else -> "Open My Journal"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }
        }
    }

    activeLegalDoc?.let { doc ->
        LegalDocumentSheet(docType = doc, onDismiss = { activeLegalDoc = null })
    }
}
