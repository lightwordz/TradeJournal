package com.example.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object Formatters {
    private val indianSymbols = DecimalFormatSymbols(Locale.forLanguageTag("en-IN"))
    private val indianIntFormat = DecimalFormat("#,##,##0", indianSymbols)
    private val indianDecFormat = DecimalFormat("#,##,##0.00", indianSymbols)

    fun inr(amount: BigDecimal, showSign: Boolean = false, showDecimals: Boolean = false): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val absVal = scaled.abs()
        val prefix = when {
            scaled < BigDecimal.ZERO -> "-"
            showSign && scaled > BigDecimal.ZERO -> "+"
            else -> ""
        }
        val formatted = if (showDecimals || absVal.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            indianDecFormat.format(absVal)
        } else {
            indianIntFormat.format(absVal)
        }
        return "${prefix}₹$formatted"
    }

    fun compactInr(amount: BigDecimal, showSign: Boolean = false): String {
        val absVal = amount.abs().toDouble()
        val prefix = when {
            amount < BigDecimal.ZERO -> "-"
            showSign && amount > BigDecimal.ZERO -> "+"
            else -> ""
        }
        return when {
            absVal >= 1_00_00_000 -> "${prefix}₹${String.format(Locale.US, "%.2f", absVal / 1_00_00_000)}Cr"
            absVal >= 1_00_000 -> "${prefix}₹${String.format(Locale.US, "%.2f", absVal / 1_00_000)}L"
            absVal >= 1_000 -> "${prefix}₹${String.format(Locale.US, "%.1f", absVal / 1_000)}k"
            else -> inr(amount, showSign = showSign, showDecimals = false)
        }
    }

    fun friendlyDate(isoDate: String): String {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val parsed = runCatching { parser.parse(isoDate) }.getOrNull() ?: return isoDate
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val currentYear = Calendar.getInstance(ist).get(Calendar.YEAR)
        val dateYear = Calendar.getInstance(ist).apply { time = parsed }.get(Calendar.YEAR)
        val pattern = if (dateYear == currentYear) "EEE, d MMM" else "EEE, d MMM ''yy"
        return SimpleDateFormat(pattern, Locale.US).format(parsed)
    }

    fun compactPrice(price: BigDecimal): String {
        val scaled = price.setScale(2, RoundingMode.HALF_UP)
        val hasFraction = scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
        val text = if (hasFraction) {
            indianDecFormat.format(scaled).trimEnd('0').trimEnd('.')
        } else {
            indianIntFormat.format(scaled)
        }
        return "₹$text"
    }
}
