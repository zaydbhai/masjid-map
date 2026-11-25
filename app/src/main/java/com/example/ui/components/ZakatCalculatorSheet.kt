package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyPreset
import com.example.domain.NisabStandard
import com.example.domain.ZakatCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorSheet(
    initialCurrency: CurrencyPreset,
    initialNisab: NisabStandard,
    onSavePreferences: (CurrencyPreset, NisabStandard) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    var currency by remember(initialCurrency) { mutableStateOf(initialCurrency) }
    var nisab by remember(initialNisab) { mutableStateOf(initialNisab) }

    var pricePerGramText by remember(currency, nisab) {
        mutableStateOf(
            if (nisab == NisabStandard.SILVER) {
                currency.defaultSilverPerGram.toString()
            } else {
                currency.defaultGoldPerGram.toString()
            }
        )
    }

    var cashText by remember { mutableStateOf("") }
    var goldSilverText by remember { mutableStateOf("") }
    var investmentsText by remember { mutableStateOf("") }
    var inventoryText by remember { mutableStateOf("") }
    var receivablesText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }

    var householdCount by remember { mutableIntStateOf(1) }
    var fitrRateText by remember(currency) { mutableStateOf(currency.defaultFitrPerPerson.toString()) }
    var missedFastsCount by remember { mutableIntStateOf(0) }
    var fidyahRateText by remember(currency) { mutableStateOf(currency.defaultFitrPerPerson.toString()) }

    val summary = ZakatCalculator.compute(
        cashAndBank = cashText.toDoubleOrNull() ?: 0.0,
        goldAndSilverValue = goldSilverText.toDoubleOrNull() ?: 0.0,
        investmentsAndStocks = investmentsText.toDoubleOrNull() ?: 0.0,
        businessInventory = inventoryText.toDoubleOrNull() ?: 0.0,
        receivables = receivablesText.toDoubleOrNull() ?: 0.0,
        shortTermDebts = debtsText.toDoubleOrNull() ?: 0.0,
        nisabStandard = nisab,
        metalPricePerGram = pricePerGramText.toDoubleOrNull() ?: 0.0
    )

    val (fitrDue, fidyahDue) = ZakatCalculator.computeFitrAndFidyah(
        householdMembers = householdCount,
        fitrRatePerPerson = fitrRateText.toDoubleOrNull() ?: 0.0,
        missedFastingDays = missedFastsCount,
        fidyahRatePerDay = fidyahRateText.toDoubleOrNull() ?: 0.0
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .testTag("zakat_calculator_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Zakat, Fitr & Fidyah Calculator",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "100% offline · adjust local metal/fitr rates anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Currency Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurrencyPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = currency == preset,
                        onClick = {
                            currency = preset
                            onSavePreferences(preset, nisab)
                        },
                        label = { Text(preset.code) }
                    )
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Annual Zakat (2.5%)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Zakat al-Fitr & Fidyah") }
                )
            }

            if (selectedTab == 0) {
                // Result Summary Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (summary.meetsNisab) "ZAKAT DUE (ABOVE NISAB)" else "BELOW NISAB THRESHOLD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = formatMoney(currency, summary.zakatDue),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        )
                        Text(
                            text = "Net Wealth: ${formatMoney(currency, summary.netZakatableWealth)} · Nisab (${nisab.name.lowercase()}): ${formatMoney(currency, summary.nisabThreshold)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                // Nisab Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NisabStandard.entries.forEach { std ->
                        FilterChip(
                            selected = nisab == std,
                            onClick = {
                                nisab = std
                                onSavePreferences(currency, std)
                            },
                            label = { Text(std.label) }
                        )
                    }
                }
                Text(
                    text = nisab.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NumericField(
                    value = pricePerGramText,
                    onValueChange = { pricePerGramText = it },
                    label = "${if (nisab == NisabStandard.SILVER) "Silver" else "Gold"} price per gram (${currency.symbol})"
                )

                NumericField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = "Cash on hand & bank balances (${currency.symbol})"
                )
                NumericField(
                    value = goldSilverText,
                    onValueChange = { goldSilverText = it },
                    label = "Gold & silver market value (${currency.symbol})"
                )
                NumericField(
                    value = investmentsText,
                    onValueChange = { investmentsText = it },
                    label = "Stocks, crypto & liquid investments (${currency.symbol})"
                )
                NumericField(
                    value = inventoryText,
                    onValueChange = { inventoryText = it },
                    label = "Business trade inventory (${currency.symbol})"
                )
                NumericField(
                    value = receivablesText,
                    onValueChange = { receivablesText = it },
                    label = "Money owed to you (expected repaid)"
                )
                NumericField(
                    value = debtsText,
                    onValueChange = { debtsText = it },
                    label = "Short-term debts & bills due (${currency.symbol})"
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Zakat al-Fitr: ${formatMoney(currency, fitrDue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Fidyah: ${formatMoney(currency, fidyahDue)} · Combined Total: ${formatMoney(currency, fitrDue + fidyahDue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                StepperCard(
                    title = "Household Members (Zakat al-Fitr)",
                    subtitle = "Obligatory for every member before Eid prayer (~1 Sa' staple food)",
                    count = householdCount,
                    onDecrement = { if (householdCount > 1) householdCount-- },
                    onIncrement = { householdCount++ }
                )
                NumericField(
                    value = fitrRateText,
                    onValueChange = { fitrRateText = it },
                    label = "Local Fitr amount per person (${currency.symbol})"
                )

                Spacer(modifier = Modifier.height(4.dp))

                StepperCard(
                    title = "Missed Fasts Eligible for Fidyah",
                    subtitle = "For those permanently unable to fast due to chronic illness or old age",
                    count = missedFastsCount,
                    onDecrement = { if (missedFastsCount > 0) missedFastsCount-- },
                    onIncrement = { missedFastsCount++ }
                )
                NumericField(
                    value = fidyahRateText,
                    onValueChange = { fidyahRateText = it },
                    label = "Fidyah meal rate per day (${currency.symbol})"
                )
            }
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (next.isEmpty() || next.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                onValueChange(next)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepperCard(
    title: String,
    subtitle: String,
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

private fun formatMoney(currency: CurrencyPreset, amount: Double): String =
    currency.symbol + String.format(Locale.getDefault(), "%,.2f", amount)
