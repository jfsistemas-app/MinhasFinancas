package br.com.mazim.financasmodernas.model

import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class EntryType {
    INCOME,
    EXPENSE;

    fun label(): String = if (this == INCOME) "Receita" else "Despesa"
}

enum class StatusFilter {
    ALL,
    SETTLED,
    PENDING;

    fun label(): String = when (this) {
        ALL -> "Todos"
        SETTLED -> "Receb./Pagos"
        PENDING -> "A receber/A pagar"
    }
}

enum class RecurrenceType {
    NONE,
    MONTHLY,
    INSTALLMENTS;

    fun label(): String = when (this) {
        NONE -> "Único"
        MONTHLY -> "Mensal"
        INSTALLMENTS -> "Parcelado"
    }
}

data class FinanceEntry(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amountInCents: Long,
    val date: LocalDate,
    val type: EntryType,
    val isSettled: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val installmentIndex: Int? = null,
    val installmentTotal: Int? = null,
    val seriesId: String? = null,
) {
    val signedAmountInCents: Long
        get() = if (type == EntryType.INCOME) amountInCents else -amountInCents

    val statusLabel: String
        get() = when {
            isSettled && type == EntryType.INCOME -> "Recebido"
            isSettled && type == EntryType.EXPENSE -> "Pago"
            !isSettled && type == EntryType.INCOME -> "A receber"
            else -> "A pagar"
        }

    val displayDescription: String
        get() = when {
            installmentIndex != null && installmentTotal != null -> "$description (${installmentIndex}/${installmentTotal})"
            else -> description
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("description", description)
        put("amountInCents", amountInCents)
        put("date", date.toString())
        put("type", type.name)
        put("isSettled", isSettled)
        put("recurrenceType", recurrenceType.name)
        put("installmentIndex", installmentIndex)
        put("installmentTotal", installmentTotal)
        put("seriesId", seriesId)
    }

    companion object {
        fun fromJson(json: JSONObject): FinanceEntry {
            return FinanceEntry(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                description = json.optString("description"),
                amountInCents = json.optLong("amountInCents"),
                date = LocalDate.parse(json.optString("date")),
                type = EntryType.valueOf(json.optString("type", EntryType.EXPENSE.name)),
                isSettled = json.optBoolean("isSettled", false),
                recurrenceType = RecurrenceType.valueOf(json.optString("recurrenceType", RecurrenceType.NONE.name)),
                installmentIndex = json.optInt("installmentIndex").takeIf { !json.isNull("installmentIndex") },
                installmentTotal = json.optInt("installmentTotal").takeIf { !json.isNull("installmentTotal") },
                seriesId = json.optString("seriesId").takeIf { !json.isNull("seriesId") && it.isNotBlank() },
            )
        }
    }
}

data class FinanceTotals(
    val incomesInCents: Long = 0,
    val expensesInCents: Long = 0,
) {
    val balanceInCents: Long
        get() = incomesInCents - expensesInCents
}
