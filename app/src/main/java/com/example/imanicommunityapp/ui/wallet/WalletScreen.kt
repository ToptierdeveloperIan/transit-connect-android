package com.example.imanicommunityapp.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imanicommunityapp.ui.theme.ImaniColor
import com.example.imanicommunityapp.ui.theme.ImaniTheme
import com.example.imanicommunityapp.wallet.WalletIntentDto
import com.example.imanicommunityapp.wallet.WalletLedgerEntryDto

@Composable
fun WalletRoute(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.refresh(showFullLoading = true)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ImaniTheme {
        WalletScreen(
            state = state,
            onBack = onBack,
            onRefresh = { viewModel.refresh(showFullLoading = false) },
            onOpenDeposit = viewModel::openDepositSheet,
            onDismissDeposit = viewModel::dismissDepositSheet,
            onDepositAmountChange = viewModel::setDepositAmount,
            onDepositChannelChange = viewModel::setDepositChannel,
            onSubmitDeposit = viewModel::submitDeposit,
            onClearFeedback = viewModel::clearDepositFeedback,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    state: WalletUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDeposit: () -> Unit,
    onDismissDeposit: () -> Unit,
    onDepositAmountChange: (String) -> Unit,
    onDepositChannelChange: (DepositChannel) -> Unit,
    onSubmitDeposit: () -> Unit,
    onClearFeedback: () -> Unit,
) {
    val currency = state.balance?.currency ?: "KES"
    val available = formatMoney(state.balance?.availableBalance)
    val spendable = formatMoney(state.balance?.spendable)
    val held = formatMoney(state.balance?.heldBalance)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wallet",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isLoading && !state.isRefreshing) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ImaniColor.BluePrimary,
                    titleContentColor = ImaniColor.White,
                    navigationIconContentColor = ImaniColor.White,
                    actionIconContentColor = ImaniColor.White,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDeposit,
                containerColor = ImaniColor.BluePrimary,
                contentColor = ImaniColor.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Top up")
            }
        },
        containerColor = ImaniColor.Surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.balance == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ImaniColor.BluePrimary)
                    }
                }
                state.error != null && state.balance == null -> {
                    ErrorPanel(
                        message = state.error,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            BalanceHeroCard(
                                currency = currency,
                                available = available,
                                spendable = spendable,
                                held = held,
                                isActive = state.balance?.isActive != false,
                            )
                        }

                        item {
                            ActionHintRow(onTopUp = onOpenDeposit)
                        }

                        item {
                            InfoBanner(
                                text = "Pay a ride from wallet at checkout after a fare quote. " +
                                    "Top-ups credit after M-Pesa / Airtel confirms payment.",
                            )
                        }

                        state.depositMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                            item {
                                FeedbackCard(
                                    message = msg,
                                    isError = false,
                                    onDismiss = onClearFeedback,
                                )
                            }
                        }

                        state.error?.takeIf { it.isNotBlank() && state.balance != null }?.let { err ->
                            item {
                                FeedbackCard(
                                    message = err,
                                    isError = true,
                                    onDismiss = onRefresh,
                                )
                            }
                        }

                        item {
                            SectionHeader(
                                title = "Activity",
                                icon = Icons.Default.History,
                            )
                        }

                        if (state.ledger.isEmpty()) {
                            item {
                                EmptyCard(
                                    title = "No ledger entries yet",
                                    subtitle = "Top up to fund your wallet. Credits appear here after confirmation.",
                                )
                            }
                        } else {
                            items(
                                items = state.ledger,
                                key = { it.id ?: "${it.createdAt}-${it.amount}" },
                            ) { entry ->
                                LedgerRow(entry = entry, currency = currency)
                            }
                        }

                        if (state.intents.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                SectionHeader(
                                    title = "Recent intents",
                                    icon = Icons.Default.Schedule,
                                )
                            }
                            items(
                                items = state.intents.take(8),
                                key = { "intent-${it.id}" },
                            ) { intent ->
                                IntentRow(intent = intent, currency = currency)
                            }
                        }
                    }
                }
            }

            if (state.isRefreshing) {
                LinearishLoader(Modifier.align(Alignment.TopCenter))
            }
        }
    }

    if (state.showDepositSheet) {
        DepositBottomSheet(
            amount = state.depositAmount,
            channel = state.depositChannel,
            isDepositing = state.isDepositing,
            error = state.depositError,
            onAmountChange = onDepositAmountChange,
            onChannelChange = onDepositChannelChange,
            onSubmit = onSubmitDeposit,
            onDismiss = onDismissDeposit,
        )
    }
}

@Composable
private fun BalanceHeroCard(
    currency: String,
    available: String,
    spendable: String,
    held: String,
    isActive: Boolean,
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            ImaniColor.BluePrimary,
            ImaniColor.BlueLight,
            Color(0xFF1A237E),
        ),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ImaniColor.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = ImaniColor.White,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Available balance",
                            color = ImaniColor.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            color = ImaniColor.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "$currency $available",
                    color = ImaniColor.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniStatChip(
                        label = "Spendable",
                        value = "$currency $spendable",
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatChip(
                        label = "Held",
                        value = "$currency $held",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ImaniColor.White.copy(alpha = 0.14f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = ImaniColor.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                color = ImaniColor.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionHintRow(onTopUp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ImaniColor.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Top up with M-Pesa or Airtel",
                    fontWeight = FontWeight.SemiBold,
                    color = ImaniColor.BluePrimary,
                )
                Text(
                    "Funds land after provider confirmation",
                    color = ImaniColor.OnSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onTopUp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImaniColor.BluePrimary,
                    contentColor = ImaniColor.White,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Top up")
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ImaniColor.BlueLight.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ImaniColor.BluePrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = ImaniColor.OnSurfaceMuted,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ImaniColor.BluePrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ImaniColor.BluePrimary,
        )
    }
}

@Composable
private fun LedgerRow(entry: WalletLedgerEntryDto, currency: String) {
    val isCredit = (entry.signedAmount?.toDoubleOrNull() ?: 0.0) >= 0 ||
        (entry.entryType?.contains("CREDIT", ignoreCase = true) == true)
    val accent = if (isCredit) ImaniColor.Green else ImaniColor.Red
    val sign = if (isCredit) "+" else "−"
    val amount = formatMoney(entry.amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImaniColor.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = humanizeEntryType(entry.entryType),
                    fontWeight = FontWeight.SemiBold,
                    color = ImaniColor.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.description?.takeIf { it.isNotBlank() }
                        ?: entry.channel
                        ?: "Wallet",
                    color = ImaniColor.OnSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatIsoDate(entry.createdAt),
                    color = ImaniColor.Gray,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign $currency $amount",
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                entry.balanceAfter?.let {
                    Text(
                        text = "Bal $currency ${formatMoney(it)}",
                        color = ImaniColor.Gray,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntentRow(intent: WalletIntentDto, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImaniColor.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${intent.kind ?: "INTENT"} · ${intent.channel ?: "—"}",
                    fontWeight = FontWeight.SemiBold,
                    color = ImaniColor.Black,
                )
                Text(
                    text = intent.status ?: "—",
                    color = statusColor(intent.status),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatIsoDate(intent.createdAt),
                    color = ImaniColor.Gray,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = "$currency ${formatMoney(intent.amount)}",
                fontWeight = FontWeight.Bold,
                color = ImaniColor.BluePrimary,
            )
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImaniColor.White),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = ImaniColor.BluePrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = ImaniColor.OnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeedbackCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) ImaniColor.Red.copy(alpha = 0.1f) else ImaniColor.Green.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = if (isError) ImaniColor.Red else Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDismiss) {
                Text(if (isError) "Retry" else "OK")
            }
        }
    }
}

@Composable
private fun ErrorPanel(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = ImaniColor.BluePrimary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Couldn't load wallet",
            fontWeight = FontWeight.Bold,
            color = ImaniColor.BluePrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message ?: "Check your connection and try again.",
            color = ImaniColor.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ImaniColor.BluePrimary),
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun LinearishLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 3.dp,
            color = ImaniColor.BluePrimary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositBottomSheet(
    amount: String,
    channel: DepositChannel,
    isDepositing: Boolean,
    error: String?,
    onAmountChange: (String) -> Unit,
    onChannelChange: (DepositChannel) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ImaniColor.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Top up wallet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ImaniColor.BluePrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "We'll create a deposit intent. Balance credits after the provider confirms.",
                color = ImaniColor.OnSurfaceMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Channel",
                fontWeight = FontWeight.SemiBold,
                color = ImaniColor.Black,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DepositChannel.entries.forEach { option ->
                    FilterChip(
                        selected = channel == option,
                        onClick = { onChannelChange(option) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ImaniColor.BluePrimary,
                            selectedLabelColor = ImaniColor.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount (KES)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ImaniColor.BluePrimary,
                    focusedLabelColor = ImaniColor.BluePrimary,
                    cursorColor = ImaniColor.BluePrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = ImaniColor.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSubmit,
                enabled = !isDepositing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImaniColor.BluePrimary,
                    contentColor = ImaniColor.White,
                ),
            ) {
                if (isDepositing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = ImaniColor.White,
                    )
                } else {
                    Text("Continue", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatMoney(raw: String?): String {
    if (raw.isNullOrBlank()) return "0.00"
    return try {
        val value = raw.toDouble()
        String.format("%.2f", value)
    } catch (_: Exception) {
        raw
    }
}

private fun formatIsoDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    // "2026-07-19T12:34:56.789Z" → short local-ish display without full parser dependency
    return raw.replace('T', ' ').take(16)
}

private fun humanizeEntryType(type: String?): String {
    if (type.isNullOrBlank()) return "Movement"
    return type
        .lowercase()
        .split('_')
        .joinToString(" ") { part ->
            part.replaceFirstChar { c -> c.uppercase() }
        }
}

private fun statusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "SUCCEEDED" -> ImaniColor.Green
        "FAILED", "EXPIRED", "CANCELLED" -> ImaniColor.Red
        "PENDING_PROVIDER", "PROVIDER_ACCEPTED", "CREATED" -> ImaniColor.BlueLight
        "REVERSED" -> ImaniColor.Gray
        else -> ImaniColor.OnSurfaceMuted
    }
}
