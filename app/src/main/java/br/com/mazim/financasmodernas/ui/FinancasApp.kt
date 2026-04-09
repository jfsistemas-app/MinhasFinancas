package br.com.mazim.financasmodernas.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.mazim.financasmodernas.model.EntryType
import br.com.mazim.financasmodernas.model.FinanceEntry
import br.com.mazim.financasmodernas.model.FinanceTotals
import br.com.mazim.financasmodernas.model.RecurrenceType
import br.com.mazim.financasmodernas.model.StatusFilter
import br.com.mazim.financasmodernas.ui.theme.ExpenseColor
import br.com.mazim.financasmodernas.ui.theme.IncomeColor
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AppScreen {
    LAUNCHES,
    REPORTS,
}

private val brazilCurrency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val uiDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun FinancasApp(viewModel: MainViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.LAUNCHES) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.LAUNCHES,
                    onClick = { currentScreen = AppScreen.LAUNCHES },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Lançamentos") },
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.REPORTS,
                    onClick = { currentScreen = AppScreen.REPORTS },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text("Relatórios") },
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.LAUNCHES) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingEntryId = null
                        showForm = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Novo lançamento") },
                )
            }
        },
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.LAUNCHES -> LaunchesScreen(
                modifier = Modifier.padding(innerPadding),
                entries = viewModel.launchEntries(),
                totals = viewModel.launchTotals(),
                selectedFilter = viewModel.launchStatusFilter,
                onFilterSelected = viewModel::setLaunchStatusFilter,
                onEdit = {
                    editingEntryId = it
                    showForm = true
                },
                onDelete = {
                    viewModel.deleteEntry(it)
                    Toast.makeText(context, "Lançamento excluído.", Toast.LENGTH_SHORT).show()
                },
                onToggleSettled = { entryId ->
                    viewModel.toggleSettled(entryId)
                },
            )

            AppScreen.REPORTS -> ReportsScreen(
                modifier = Modifier.padding(innerPadding),
                entries = viewModel.reportEntries(),
                totals = viewModel.reportTotals(),
                selectedFilter = viewModel.reportStatusFilter,
                startDate = viewModel.reportStartDate,
                endDate = viewModel.reportEndDate,
                onFilterSelected = viewModel::setReportStatusFilter,
                onStartDateSelected = viewModel::setReportStartDate,
                onEndDateSelected = viewModel::setReportEndDate,
                onShareReport = {
                    val result = viewModel.shareReport()
                    result.exceptionOrNull()?.let {
                        Toast.makeText(context, it.message ?: "Não foi possível compartilhar o PDF.", Toast.LENGTH_LONG).show()
                    }
                },
            )
        }
    }

    if (showForm) {
        EntryFormDialog(
            existingEntry = viewModel.findEntry(editingEntryId),
            onDismiss = {
                showForm = false
                editingEntryId = null
            },
            onSave = { editingId, description, amountInCents, date, type, recurrenceType, installments ->
                viewModel.upsertEntry(
                    editingEntryId = editingId,
                    description = description,
                    amountInCents = amountInCents,
                    date = date,
                    type = type,
                    recurrenceType = recurrenceType,
                    installments = installments,
                )
                showForm = false
                editingEntryId = null
                Toast.makeText(context, "Lançamento salvo.", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LaunchesScreen(
    modifier: Modifier = Modifier,
    entries: List<FinanceEntry>,
    totals: FinanceTotals,
    selectedFilter: StatusFilter,
    onFilterSelected: (StatusFilter) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleSettled: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeaderCard(
                title = "Sua visão geral",
                subtitle = "Receitas, despesas e saldo atualizados pelos filtros da tela.",
            )
        }

        item {
            TotalsRow(totals = totals)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Filtrar situação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.label()) },
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nada por aqui ainda",
                    subtitle = "Use o botão + para cadastrar sua primeira receita ou despesa.",
                )
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                LaunchItemCard(
                    entry = entry,
                    onEdit = { onEdit(entry.id) },
                    onDelete = { onDelete(entry.id) },
                    onToggleSettled = { onToggleSettled(entry.id) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(76.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportsScreen(
    modifier: Modifier = Modifier,
    entries: List<FinanceEntry>,
    totals: FinanceTotals,
    selectedFilter: StatusFilter,
    startDate: LocalDate,
    endDate: LocalDate,
    onFilterSelected: (StatusFilter) -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onShareReport: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeaderCard(
                title = "Relatórios compactos",
                subtitle = "Filtre o período, revise os totais e compartilhe um PDF limpo e enxuto.",
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        modifier = Modifier.weight(1f),
                        label = "Data inicial",
                        date = startDate,
                        onDateSelected = onStartDateSelected,
                    )
                    DateField(
                        modifier = Modifier.weight(1f),
                        label = "Data final",
                        date = endDate,
                        onDateSelected = onEndDateSelected,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.label()) },
                        )
                    }
                }
            }
        }

        item {
            TotalsRow(totals = totals)
        }

        item {
            FilledTonalButton(
                onClick = {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "Sem dados no período selecionado.", Toast.LENGTH_SHORT).show()
                    } else {
                        onShareReport()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar PDF")
            }
        }

        if (entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nenhum lançamento no relatório",
                    subtitle = "Ajuste o período ou a situação para visualizar dados e compartilhar o PDF.",
                )
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                ReportItemCard(entry = entry)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TotalsRow(totals: FinanceTotals) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TotalCard(
            modifier = Modifier.weight(1f),
            title = "Receitas",
            value = brazilCurrency.format(totals.incomesInCents / 100.0),
            accent = IncomeColor,
        )
        TotalCard(
            modifier = Modifier.weight(1f),
            title = "Despesas",
            value = brazilCurrency.format(totals.expensesInCents / 100.0),
            accent = ExpenseColor,
        )
        TotalCard(
            modifier = Modifier.weight(1f),
            title = "Saldo",
            value = brazilCurrency.format(totals.balanceInCents / 100.0),
            accent = if (totals.balanceInCents >= 0) IncomeColor else ExpenseColor,
        )
    }
}

@Composable
private fun TotalCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape),
            )
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

@Composable
private fun LaunchItemCard(
    entry: FinanceEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSettled: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = entry.displayDescription,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.date.format(uiDateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = brazilCurrency.format(entry.amountInCents / 100.0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.type == EntryType.INCOME) IncomeColor else ExpenseColor,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(entry.type.label()) })
                AssistChip(onClick = {}, label = { Text(entry.statusLabel) })
            }

            if (entry.recurrenceType != RecurrenceType.NONE) {
                Text(
                    text = if (entry.recurrenceType == RecurrenceType.MONTHLY) {
                        "Gerado como recorrência mensal"
                    } else {
                        "Gerado como lançamento parcelado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                }
                FilledTonalButton(onClick = onToggleSettled) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (entry.isSettled) "Reabrir" else if (entry.type == EntryType.INCOME) "Marcar recebido" else "Marcar pago")
                }
            }
        }
    }
}

@Composable
private fun ReportItemCard(entry: FinanceEntry) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.displayDescription,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = brazilCurrency.format(entry.amountInCents / 100.0),
                    color = if (entry.type == EntryType.INCOME) IncomeColor else ExpenseColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${entry.date.format(uiDateFormatter)} • ${entry.type.label()} • ${entry.statusLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryFormDialog(
    existingEntry: FinanceEntry?,
    onDismiss: () -> Unit,
    onSave: (
        editingEntryId: String?,
        description: String,
        amountInCents: Long,
        date: LocalDate,
        type: EntryType,
        recurrenceType: RecurrenceType,
        installments: Int,
    ) -> Unit,
) {
    val context = LocalContext.current
    var description by remember(existingEntry) { mutableStateOf(existingEntry?.description ?: "") }
    var amountText by remember(existingEntry) { mutableStateOf(existingEntry?.amountInCents?.toCurrencyInput() ?: "") }
    var selectedDate by remember(existingEntry) { mutableStateOf(existingEntry?.date ?: LocalDate.now()) }
    var selectedType by remember(existingEntry) { mutableStateOf(existingEntry?.type ?: EntryType.EXPENSE) }
    var selectedRecurrence by remember(existingEntry) { mutableStateOf(existingEntry?.recurrenceType ?: RecurrenceType.NONE) }
    var installments by remember(existingEntry) { mutableIntStateOf(existingEntry?.installmentTotal ?: 2) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (existingEntry == null) "Novo lançamento" else "Editar lançamento",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (existingEntry != null) {
                    Text(
                        text = "A edição altera somente este item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )

                DateField(
                    label = "Data",
                    date = selectedDate,
                    onDateSelected = { selectedDate = it },
                )

                SingleChoiceChips(
                    title = "Tipo",
                    current = selectedType,
                    values = EntryType.entries,
                    label = { it.label() },
                    onSelected = { selectedType = it },
                )

                if (existingEntry == null) {
                    SingleChoiceChips(
                        title = "Recorrência",
                        current = selectedRecurrence,
                        values = RecurrenceType.entries,
                        label = { it.label() },
                        onSelected = { selectedRecurrence = it },
                    )

                    if (selectedRecurrence == RecurrenceType.INSTALLMENTS) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Quantidade de parcelas", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "O valor informado será repetido em cada parcela.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { installments = (installments - 1).coerceAtLeast(2) }) {
                                        Text("-")
                                    }
                                    Text(
                                        text = installments.toString(),
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    OutlinedButton(onClick = { installments = (installments + 1).coerceAtMost(48) }) {
                                        Text("+")
                                    }
                                }
                            }
                        }
                    }

                    if (selectedRecurrence == RecurrenceType.MONTHLY) {
                        Text(
                            text = "A opção mensal gera automaticamente 12 lançamentos futuros com o mesmo valor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val parsedValue = parseCurrencyToCents(amountText)
                            when {
                                description.isBlank() -> Toast.makeText(context, "Informe a descrição.", Toast.LENGTH_SHORT).show()
                                parsedValue == null || parsedValue <= 0L -> Toast.makeText(context, "Informe um valor válido.", Toast.LENGTH_SHORT).show()
                                else -> onSave(
                                    existingEntry?.id,
                                    description.trim(),
                                    parsedValue,
                                    selectedDate,
                                    selectedType,
                                    if (existingEntry == null) selectedRecurrence else RecurrenceType.NONE,
                                    installments,
                                )
                            }
                        },
                    ) {
                        Text(if (existingEntry == null) "Salvar" else "Atualizar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SingleChoiceChips(
    title: String,
    current: T,
    values: List<T>,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { option ->
                FilterChip(
                    selected = option == current,
                    onClick = { onSelected(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
private fun DateField(
    modifier: Modifier = Modifier,
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val dialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth,
                )
                dialog.show()
            },
        value = date.format(uiDateFormatter),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
    )
}

private fun parseCurrencyToCents(value: String): Long? {
    val normalized = value
        .trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")

    return runCatching {
        BigDecimal(normalized)
            .setScale(2, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .longValueExact()
    }.getOrNull()
}

private fun Long.toCurrencyInput(): String {
    return String.format(Locale("pt", "BR"), "%.2f", this / 100.0)
}
