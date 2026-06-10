package com.example.qwen2gguf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modelAssetName: String,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val promptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showPromptSheet by rememberSaveable { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(modelAssetName) {
        viewModel.loadCurrentModel()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    ModelSelector(
                        selected = uiState.selectedModel,
                        onSelect = viewModel::selectModel,
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::clearChat) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            SkillPicker(
                selected = uiState.selectedSkill,
                selectedTheme = uiState.selectedTheme,
                onSelectSkill = viewModel::selectSkill,
                onThemeClick = { showThemeSheet = true },
                onPromptsClick = { showPromptSheet = true },
            )

            when (val state = uiState.modelState) {
                ModelState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is ModelState.Failed -> Text(
                    text = "Model error: ${state.reason}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                else -> Unit
            }

            if (uiState.gpuFallback) {
                Text(
                    text = "⚠️ GPU inference failed — switched to CPU",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            MessageList(
                messages = uiState.messages,
                isGenerating = uiState.isGenerating,
                modifier = Modifier.weight(1f),
            )

            InputBar(
                enabled = uiState.modelState is ModelState.Ready && !uiState.isGenerating,
                isGenerating = uiState.isGenerating,
                text = inputText,
                onTextChange = { inputText = it },
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
            )
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            ThemeBottomSheet(
                selected = uiState.selectedTheme,
                onSelect = { theme ->
                    viewModel.selectTheme(theme)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showThemeSheet = false
                    }
                },
            )
        }
    }

    if (showPromptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPromptSheet = false },
            sheetState = promptSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            PromptPickerBottomSheet(
                theme = uiState.selectedTheme,
                onSelect = { prompt ->
                    inputText = prompt
                    scope.launch { promptSheetState.hide() }.invokeOnCompletion {
                        showPromptSheet = false
                    }
                },
            )
        }
    }
}

// ── theme bottom sheet ────────────────────────────────────────────────────────

@Composable
private fun ThemeBottomSheet(
    selected: FairyTaleTheme,
    onSelect: (FairyTaleTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = "Fairy Tale Theme",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Text(
            text = "Choosing a theme clears the current chat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()

        FairyTaleTheme.entries.forEach { theme ->
            ListItem(
                headlineContent = {
                    Text("${theme.emoji}  ${theme.displayName}", fontWeight = FontWeight.Medium)
                },
                supportingContent = {
                    Text(
                        text = theme.promptFragment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                },
                trailingContent = {
                    RadioButton(
                        selected = theme == selected,
                        onClick = { onSelect(theme) },
                    )
                },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (theme == selected) mod else mod
                    },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// ── skill picker ──────────────────────────────────────────────────────────────

@Composable
private fun SkillPicker(
    selected: Skill,
    selectedTheme: FairyTaleTheme,
    onSelectSkill: (Skill) -> Unit,
    onThemeClick: () -> Unit,
    onPromptsClick: () -> Unit,
) {
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(Skill.entries) { skill ->
                FilterChip(
                    selected = skill == selected,
                    onClick = { onSelectSkill(skill) },
                    label = { Text(skill.displayName) },
                )
            }
        }

        if (selected == Skill.FAIRY_TALE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                TextButton(onClick = onThemeClick) {
                    Text(
                        text = "${selectedTheme.emoji} ${selectedTheme.displayName}  ▾",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(onClick = onPromptsClick) {
                    Text(
                        text = "✨ Prompts",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

// ── model selector ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    selected: QwenModel,
    onSelect: (QwenModel) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = selected.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select model",
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QwenModel.entries.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(model)
                    },
                )
            }
        }
    }
}

// ── message list ──────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages.size, key = { idx -> idx }) { idx ->
            MessageBubble(messages[idx])
        }
        if (isGenerating && messages.lastOrNull()?.content?.isEmpty() == true) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.Role.User
    var showBaseTaleSheet by rememberSaveable { mutableStateOf(false) }
    val baseTaleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    if (showBaseTaleSheet && message.baseTale != null) {
        ModalBottomSheet(
            onDismissRequest = { showBaseTaleSheet = false },
            sheetState = baseTaleSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            BaseTaleBottomSheet(baseTale = message.baseTale)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column {
                // "📖 Base tale" button — only on assistant messages that have one
                if (!isUser && message.baseTale != null) {
                    TextButton(
                        onClick = { showBaseTaleSheet = true },
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 0.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "📖 Base tale",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                    )
                }

                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── base tale bottom sheet ────────────────────────────────────────────────────

@Composable
private fun BaseTaleBottomSheet(baseTale: Pair<String, String>) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "📖 Base tale",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "\"${baseTale.first}\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Text(
                text = baseTale.second.trim(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── prompt picker bottom sheet ────────────────────────────────────────────────

@Composable
private fun PromptPickerBottomSheet(
    theme: FairyTaleTheme,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = "✨ Prompt ideas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Text(
            text = "${theme.emoji} ${theme.displayName} — tap one to fill the message box",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        theme.prompts.forEach { prompt ->
            ListItem(
                headlineContent = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(prompt) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// ── input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    enabled: Boolean,
    isGenerating: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    fun submit() {
        if (text.isNotBlank()) {
            onSend(text)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = { Text("Message…") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
        )
        if (isGenerating) {
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = "Stop generation")
            }
        } else {
            IconButton(onClick = ::submit, enabled = enabled && text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
