package com.example.model

import java.math.BigDecimal
import java.util.UUID

enum class CapitalEntryType(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal")
}

data class CapitalEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: CapitalEntryType,
    val amount: BigDecimal,
    val date: String,
    val note: String = ""
)
