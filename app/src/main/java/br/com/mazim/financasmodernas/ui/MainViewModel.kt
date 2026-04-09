package br.com.mazim.financasmodernas.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.AndroidViewModel
import br.com.mazim.financasmodernas.data.FinanceStore
import br.com.mazim.financasmodernas.data.PdfReportGenerator
import br.com.mazim.financasmodernas.model.EntryType
import br.com.mazim.financasmodernas.model.FinanceEntry
import br.com.mazim.financasmodernas.model.FinanceTotals
import br.com.mazim.financasmodernas.model.RecurrenceType
import br.com.mazim.financasmodernas.model.StatusFilter
import java.time.LocalDate
import java.util.UUID
import kotlin.math.absoluteValue

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FinanceStore(application.applicationContext)
    private val pdfReportGenerator = PdfReportGenerator()

    private val allEntries = mutableStateListOf<FinanceEntry>()

    var launchStatusFilter by mutableStateOf(StatusFilter.ALL)
        private set

    var reportStatusFilter by mutableStateOf(StatusFilter.ALL)
        private set

    var reportStartDate by mutableStateOf(LocalDate.now().withDayOfMonth(1))
        private set

    var reportEndDate by mutableStateOf(LocalDate.now())
        private set

    init {
        allEntries += store.load().sortedByDescending { it.date }
    }

    fun currentEntries(): List<FinanceEntry> = allEntries.sortedWith(compareByDescending<FinanceEntry> { it.date }.thenByDescending { it.id })

    fun launchEntries(): List<FinanceEntry> = currentEntries().filterByStatus(launchStatusFilter)

    fun reportEntries(): List<FinanceEntry> = currentEntries()
        .filter { !it.date.isBefore(reportStartDate) && !it.date.isAfter(reportEndDate) }
        .filterByStatus(reportStatusFilter)

    fun launchTotals(): FinanceTotals = launchEntries().toTotals()

    fun reportTotals(): FinanceTotals = reportEntries().toTotals()

    fun setLaunchStatusFilter(filter: StatusFilter) {
        launchStatusFilter = filter
    }

    fun setReportStatusFilter(filter: StatusFilter) {
        reportStatusFilter = filter
    }

    fun setReportStartDate(date: LocalDate) {
        reportStartDate = date
        if (reportEndDate.isBefore(date)) {
            reportEndDate = date
        }
    }

    fun setReportEndDate(date: LocalDate) {
        reportEndDate = if (date.isBefore(reportStartDate)) reportStartDate else date
    }

    fun upsertEntry(
        editingEntryId: String?,
        description: String,
        amountInCents: Long,
        date: LocalDate,
        type: EntryType,
        recurrenceType: RecurrenceType,
        installments: Int,
    ) {
        if (editingEntryId != null) {
            val updated = allEntries.map {
                if (it.id == editingEntryId) {
                    it.copy(
                        description = description,
                        amountInCents = amountInCents.absoluteValue,
                        date = date,
                        type = type,
                    )
                } else {
                    it
                }
            }
            allEntries.clear()
            allEntries += updated
            persist()
            return
        }

        val seriesId = UUID.randomUUID().toString()
        val normalizedAmount = amountInCents.absoluteValue
        val generated = when (recurrenceType) {
            RecurrenceType.NONE -> listOf(
                FinanceEntry(
                    description = description,
                    amountInCents = normalizedAmount,
                    date = date,
                    type = type,
                )
            )

            RecurrenceType.MONTHLY -> List(12) { index ->
                FinanceEntry(
                    description = description,
                    amountInCents = normalizedAmount,
                    date = date.plusMonths(index.toLong()),
                    type = type,
                    recurrenceType = recurrenceType,
                    seriesId = seriesId,
                )
            }

            RecurrenceType.INSTALLMENTS -> {
                val safeInstallments = installments.coerceIn(2, 48)
                List(safeInstallments) { index ->
                    FinanceEntry(
                        description = description,
                        amountInCents = normalizedAmount,
                        date = date.plusMonths(index.toLong()),
                        type = type,
                        recurrenceType = recurrenceType,
                        installmentIndex = index + 1,
                        installmentTotal = safeInstallments,
                        seriesId = seriesId,
                    )
                }
            }
        }

        allEntries += generated
        persist()
    }

    fun toggleSettled(entryId: String) {
        replaceById(entryId) { it.copy(isSettled = !it.isSettled) }
    }

    fun deleteEntry(entryId: String) {
        allEntries.removeAll { it.id == entryId }
        persist()
    }

    fun findEntry(entryId: String?): FinanceEntry? {
        if (entryId == null) return null
        return allEntries.firstOrNull { it.id == entryId }
    }

    fun shareReport(): Result<Unit> {
        val appContext = getApplication<Application>().applicationContext
        val entries = reportEntries()
        if (entries.isEmpty()) {
            return Result.failure(IllegalStateException("Não há lançamentos no relatório para compartilhar."))
        }

        return pdfReportGenerator.generate(
            context = appContext,
            entries = entries,
            totals = reportTotals(),
            startDate = reportStartDate,
            endDate = reportEndDate,
            statusLabel = reportStatusFilter.label(),
        ).mapCatching { reportUri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, reportUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório financeiro")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(appContext, Intent.createChooser(intent, "Compartilhar PDF"), null)
        }
    }

    private fun List<FinanceEntry>.filterByStatus(filter: StatusFilter): List<FinanceEntry> = when (filter) {
        StatusFilter.ALL -> this
        StatusFilter.SETTLED -> filter { it.isSettled }
        StatusFilter.PENDING -> filter { !it.isSettled }
    }

    private fun List<FinanceEntry>.toTotals(): FinanceTotals {
        val incomes = filter { it.type == EntryType.INCOME }.sumOf { it.amountInCents }
        val expenses = filter { it.type == EntryType.EXPENSE }.sumOf { it.amountInCents }
        return FinanceTotals(
            incomesInCents = incomes,
            expensesInCents = expenses,
        )
    }

    private fun replaceById(entryId: String, transform: (FinanceEntry) -> FinanceEntry) {
        val updated = allEntries.map {
            if (it.id == entryId) transform(it) else it
        }
        allEntries.clear()
        allEntries += updated
        persist()
    }

    private fun persist() {
        val sorted = allEntries.sortedWith(compareByDescending<FinanceEntry> { it.date }.thenByDescending { it.id })
        allEntries.clear()
        allEntries += sorted
        store.save(allEntries)
    }
}
