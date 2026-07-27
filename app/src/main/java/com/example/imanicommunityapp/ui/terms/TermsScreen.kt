package com.example.imanicommunityapp.ui.terms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.imanicommunityapp.ui.theme.ImaniColor
import com.example.imanicommunityapp.ui.theme.ImaniTheme

@Composable
fun TermsRoute(
    viewModel: TermsViewModel,
    requireAccept: Boolean,
    onBack: () -> Unit,
    onAccepted: () -> Unit,
) {
    LaunchedEffect(requireAccept) {
        viewModel.bootstrap(requireAccept)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ImaniTheme {
        TermsScreen(
            state = state,
            onBack = onBack,
            onLocale = viewModel::selectLocale,
            onRetry = viewModel::refresh,
            onAccept = { viewModel.accept(onAccepted) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    state: TermsUiState,
    onBack: () -> Unit,
    onLocale: (TermsLocale) -> Unit,
    onRetry: () -> Unit,
    onAccept: () -> Unit,
) {
    val scroll = rememberScrollState()
    var scrolledNearEnd by remember { mutableStateOf(false) }

    LaunchedEffect(scroll.value, scroll.maxValue) {
        scrolledNearEnd = scroll.maxValue == 0 || scroll.value >= (scroll.maxValue * 0.85f)
    }

    val mustAccept = state.requireAccept || (state.status?.mustAccept == true)
    val alreadyAccepted = state.status?.mustAccept == false && !state.requireAccept
    val canAccept = !state.isLoading &&
        state.document?.version != null &&
        (mustAccept || state.status?.mustAccept == true) &&
        scrolledNearEnd &&
        !state.isAccepting

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.locale == TermsLocale.SW) {
                            "Masharti ya Huduma"
                        } else {
                            "Terms of Service"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (!state.requireAccept) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ImaniColor.BluePrimary,
                    titleContentColor = ImaniColor.White,
                    navigationIconContentColor = ImaniColor.White,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (!scrolledNearEnd && state.document != null && mustAccept) {
                        Text(
                            text = if (state.locale == TermsLocale.SW) {
                                "Scroll chini kusoma kabla ya kukubali"
                            } else {
                                "Scroll to the bottom before accepting"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    AnimatedVisibility(visible = state.acceptError != null) {
                        Text(
                            text = state.acceptError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    when {
                        state.acceptedJustNow || (alreadyAccepted && state.status?.mustAccept != true) -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ImaniColor.Green,
                                )
                                Text(
                                    text = if (state.locale == TermsLocale.SW) {
                                        "Umekubali toleo hili"
                                    } else {
                                        "You have accepted this version"
                                    },
                                    color = ImaniColor.Green,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            if (!state.requireAccept) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ImaniColor.BluePrimary,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        if (state.locale == TermsLocale.SW) "Rudi" else "Done",
                                    )
                                }
                            }
                        }
                        mustAccept || state.status?.mustAccept == true -> {
                            Button(
                                onClick = onAccept,
                                enabled = canAccept,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImaniColor.BluePrimary,
                                    disabledContainerColor = ImaniColor.LineGray,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (state.isAccepting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = ImaniColor.White,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = if (state.locale == TermsLocale.SW) {
                                            "Kubali Masharti"
                                        } else {
                                            "Accept Terms"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                        }
                        else -> {
                            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    if (state.locale == TermsLocale.SW) "Rudi" else "Close",
                                    color = ImaniColor.BluePrimary,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            LocaleSwitcher(
                selected = state.locale,
                onSelect = onLocale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            MetaStrip(state)

            when {
                state.isLoading && state.document == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ImaniColor.BluePrimary)
                    }
                }
                state.error != null && state.document == null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Gavel,
                            contentDescription = null,
                            tint = ImaniColor.BluePrimary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.error,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = ImaniColor.BluePrimary),
                        ) {
                            Text(if (state.locale == TermsLocale.SW) "Jaribu tena" else "Retry")
                        }
                    }
                }
                else -> {
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = ImaniColor.BluePrimary,
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ImaniColor.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            Modifier
                                .verticalScroll(scroll)
                                .padding(20.dp),
                        ) {
                            Text(
                                text = state.document?.title.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImaniColor.BluePrimary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.document?.body.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocaleSwitcher(
    selected: TermsLocale,
    onSelect: (TermsLocale) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TermsLocale.entries.forEach { locale ->
            FilterChip(
                selected = selected == locale,
                onClick = { onSelect(locale) },
                label = {
                    Text(
                        text = if (locale == TermsLocale.SW) "Kiswahili" else "English",
                        fontWeight = if (selected == locale) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ImaniColor.BluePrimary,
                    selectedLabelColor = ImaniColor.White,
                    containerColor = ImaniColor.Surface,
                    labelColor = ImaniColor.BluePrimary,
                ),
            )
        }
    }
}

@Composable
private fun MetaStrip(state: TermsUiState) {
    val version = state.document?.version
    val effective = state.document?.effectiveAt?.take(10)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ImaniColor.BluePrimary.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (version != null) "v$version" else "—",
            style = MaterialTheme.typography.labelLarge,
            color = ImaniColor.BluePrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = effective ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
