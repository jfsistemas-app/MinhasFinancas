package br.com.mazim.financasmodernas.data

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import br.com.mazim.financasmodernas.model.EntryType
import br.com.mazim.financasmodernas.model.FinanceEntry
import br.com.mazim.financasmodernas.model.FinanceTotals
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PdfReportGenerator {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val generationFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun generate(
        context: Context,
        entries: List<FinanceEntry>,
        totals: FinanceTotals,
        startDate: LocalDate,
        endDate: LocalDate,
        statusLabel: String,
    ) = runCatching {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 28f
        val rowHeight = 16f
        val contentBottomLimit = 760f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var currentY = drawHeader(
            canvas = canvas,
            titlePaint = titlePaint,
            textPaint = textPaint,
            linePaint = linePaint,
            pageWidth = pageWidth.toFloat(),
            margin = margin,
            startDate = startDate,
            endDate = endDate,
            statusLabel = statusLabel,
        )

        drawColumns(canvas, margin, currentY, boldPaint)
        currentY += 14f

        entries.forEachIndexed { index, entry ->
            if (currentY > contentBottomLimit) {
                pdf.finishPage(page)
                pageNumber += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                currentY = drawHeader(
                    canvas = canvas,
                    titlePaint = titlePaint,
                    textPaint = textPaint,
                    linePaint = linePaint,
                    pageWidth = pageWidth.toFloat(),
                    margin = margin,
                    startDate = startDate,
                    endDate = endDate,
                    statusLabel = statusLabel,
                )
                drawColumns(canvas, margin, currentY, boldPaint)
                currentY += 14f
            }

            val dateText = entry.date.format(dateFormatter)
            val typeText = if (entry.type == EntryType.INCOME) "Receita" else "Despesa"
            val valueText = currencyFormatter.format(entry.amountInCents / 100.0)

            canvas.drawText(dateText, margin, currentY, textPaint)
            canvas.drawText(limit(entry.displayDescription, 28), 98f, currentY, textPaint)
            canvas.drawText(typeText, 324f, currentY, textPaint)
            canvas.drawText(entry.statusLabel, 400f, currentY, textPaint)
            canvas.drawText(valueText, 500f, currentY, if (entry.type == EntryType.INCOME) boldPaint else textPaint)
            currentY += rowHeight

            if (index == entries.lastIndex) {
                currentY += 4f
            }
        }

        if (currentY > 710f) {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            currentY = margin + 40f
        }

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 18f
        canvas.drawText("Totais logo abaixo dos itens", margin, currentY, boldPaint)
        currentY += 16f
        canvas.drawText("Total receitas: ${currencyFormatter.format(totals.incomesInCents / 100.0)}", margin, currentY, boldPaint)
        currentY += 14f
        canvas.drawText("Total despesas: ${currencyFormatter.format(totals.expensesInCents / 100.0)}", margin, currentY, boldPaint)
        currentY += 14f
        canvas.drawText("Saldo final: ${currencyFormatter.format(totals.balanceInCents / 100.0)}", margin, currentY, boldPaint)

        pdf.finishPage(page)

        val reportsDir = File(context.cacheDir, "shared_reports").apply { mkdirs() }
        val reportFile = File(reportsDir, "relatorio-financas-${System.currentTimeMillis()}.pdf")
        reportFile.outputStream().use { output -> pdf.writeTo(output) }
        pdf.close()

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            reportFile,
        )
    }

    private fun drawHeader(
        canvas: android.graphics.Canvas,
        titlePaint: Paint,
        textPaint: Paint,
        linePaint: Paint,
        pageWidth: Float,
        margin: Float,
        startDate: LocalDate,
        endDate: LocalDate,
        statusLabel: String,
    ): Float {
        var y = margin + 10f
        canvas.drawText("Relatório Financeiro", margin, y, titlePaint)
        y += 18f
        canvas.drawText(
            "Período: ${startDate.format(dateFormatter)} até ${endDate.format(dateFormatter)}    Situação: $statusLabel",
            margin,
            y,
            textPaint,
        )
        y += 14f
        canvas.drawText(
            "Gerado em ${LocalDateTime.now().format(generationFormatter)}",
            margin,
            y,
            textPaint,
        )
        y += 12f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        return y + 18f
    }

    private fun drawColumns(
        canvas: android.graphics.Canvas,
        margin: Float,
        startY: Float,
        paint: Paint,
    ) {
        canvas.drawText("Data", margin, startY, paint)
        canvas.drawText("Descrição", 98f, startY, paint)
        canvas.drawText("Tipo", 324f, startY, paint)
        canvas.drawText("Situação", 400f, startY, paint)
        canvas.drawText("Valor", 500f, startY, paint)
    }

    private fun limit(text: String, max: Int): String {
        return if (text.length <= max) text else text.take(max - 1) + "…"
    }
}
