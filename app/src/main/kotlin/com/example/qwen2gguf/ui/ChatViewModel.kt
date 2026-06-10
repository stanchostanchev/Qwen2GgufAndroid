package com.example.qwen2gguf.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwen2gguf.AssetExtractor
import com.example.qwen2gguf.DeviceInfo
import com.example.qwen2gguf.LlamaAndroid
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val llama: LlamaAndroid,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    // ── model switching ───────────────────────────────────────────────────────

    fun selectModel(model: QwenModel) {
        if (_uiState.value.selectedModel == model &&
            _uiState.value.modelState is ModelState.Ready) return

        generateJob?.cancel()
        llama.resetCache()
        llama.close()

        _uiState.update {
            it.copy(
                selectedModel = model,
                modelState = ModelState.NotLoaded,
                messages = emptyList(),
                isGenerating = false,
                error = null,
            )
        }
        loadModel(model)
    }

    fun loadCurrentModel() {
        val state = _uiState.value.modelState
        if (state is ModelState.Ready || state is ModelState.Loading) return
        // Atomically flip to Loading so concurrent calls skip
        val swapped = _uiState.compareAndSet(
            _uiState.value,
            _uiState.value.copy(modelState = ModelState.Loading),
        )
        if (!swapped) return
        loadModel(_uiState.value.selectedModel)
    }

    private fun loadModel(model: QwenModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(modelState = ModelState.Loading) }
            try {
                val modelPath = withContext(Dispatchers.IO) {
                    AssetExtractor.extract(context, model.assetName)
                }
                withContext(Dispatchers.IO) {
                    llama.load(
                        modelPath = modelPath,
                        nCtx = 4096,
                        nThreads = 6,
                        nGpuLayers = DeviceInfo.defaultGpuLayers,
                    )
                }
                _uiState.update { it.copy(modelState = ModelState.Ready) }
            } catch (e: Exception) {
                Log.e(TAG, "Model load failed", e)
                _uiState.update {
                    it.copy(modelState = ModelState.Failed(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // ── inference ─────────────────────────────────────────────────────────────

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isGenerating) return

        val userMsg = ChatMessage(ChatMessage.Role.User, userText.trim())
        val isFairyTale = _uiState.value.selectedSkill == Skill.FAIRY_TALE
        val assistantSeed = if (isFairyTale) "Once upon a time," else ""
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg + ChatMessage(ChatMessage.Role.Assistant, assistantSeed),
                isGenerating = true,
                error = null,
            )
        }

        generateJob = viewModelScope.launch {
            // Fairy tale prompts are always single-turn — reset the KV cache so nPast
            // never exceeds the new prompt length.
            if (_uiState.value.selectedSkill == Skill.FAIRY_TALE) {
                withContext(Dispatchers.IO) { llama.resetCache() }
            }
            val (prompt, baseTale) = buildQwen2Prompt(_uiState.value.messages.dropLast(1))

            // Fairy tale: model edits an existing base story so needs fewer tokens.
            val maxTokens = if (_uiState.value.selectedSkill == Skill.FAIRY_TALE) 180 else 512
            val temperature = if (_uiState.value.selectedSkill == Skill.FAIRY_TALE) 0.5f else 0.7f
            llama.generate(prompt = prompt, maxNewTokens = maxTokens, temperature = temperature)
                .catch { e ->
                    Log.e(TAG, "Generation failed", e)
                    _uiState.update { it.copy(error = e.message, isGenerating = false) }
                }
                .onCompletion {
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        val last = updated.last()
                        if (last.role == ChatMessage.Role.Assistant) {
                            // For fairy tales: trim to 4 complete sentences at a sentence boundary
                            val finalContent = if (state.selectedSkill == Skill.FAIRY_TALE)
                                capToSentences(last.content, maxSentences = 4)
                            else last.content
                            updated[updated.lastIndex] = last.copy(
                                content = finalContent,
                                baseTale = baseTale,
                            )
                        }
                        state.copy(
                            messages = updated,
                            isGenerating = false,
                            gpuFallback = llama.gpuFailed,
                        )
                    }
                }
                .collect { piece ->
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        val last = updated.last()
                        updated[updated.lastIndex] = last.copy(content = last.content + piece)
                        state.copy(messages = updated)
                    }
                }
        }
    }

    fun selectSkill(skill: Skill) {
        llama.resetCache()
        _uiState.update {
            it.copy(selectedSkill = skill, messages = emptyList(), error = null)
        }
    }

    fun selectTheme(theme: FairyTaleTheme) {
        llama.resetCache()
        _uiState.update {
            it.copy(selectedTheme = theme, messages = emptyList(), error = null)
        }
    }

    fun stopGeneration() {
        generateJob?.cancel()
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun clearChat() {
        llama.resetCache()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    // ── Qwen2 chat template ───────────────────────────────────────────────────

    /** Returns the full prompt string plus the base tale used (null if not fairy-tale mode). */
    private fun buildQwen2Prompt(
        history: List<ChatMessage>,
    ): Pair<String, Pair<String, String>?> {
        val state = _uiState.value
        val isFairyTale = state.selectedSkill == Skill.FAIRY_TALE
        var chosenBaseTale: Pair<String, String>? = null

        val prompt = buildString {
            append("<|im_start|>system\n${state.selectedSkill.systemPrompt}<|im_end|>\n")

            if (isFairyTale) {
                // Each fairy tale request is independent — pick a fresh base tale and
                // build a single-turn prompt from only the latest user message.
                chosenBaseTale = state.selectedTheme.examples.random()
                val baseTale = chosenBaseTale!!
                val latestUserMsg = history.lastOrNull { it.role == ChatMessage.Role.User }
                val userRequest = latestUserMsg?.content ?: ""
                val augmented = buildString {
                    appendLine("Base story: ${baseTale.second.trim()}")
                    appendLine("Request: $userRequest")
                    append("Retell the base story above keeping the same plot. Only swap the character types to match the request.")
                }
                append("<|im_start|>user\n$augmented<|im_end|>\n")
            } else {
                for (msg in history) {
                    val role = if (msg.role == ChatMessage.Role.User) "user" else "assistant"
                    append("<|im_start|>$role\n${msg.content}<|im_end|>\n")
                }
            }

            // Pre-fill the assistant turn for fairy tales so the model continues
            // directly into the story without repeating instructions.
            if (isFairyTale) append("<|im_start|>assistant\nOnce upon a time,")
            else append("<|im_start|>assistant\n")
        }

        return prompt to chosenBaseTale
    }

    /**
     * Returns the text up to and including the Nth sentence boundary (`.`, `!`, `?`).
     * Always returns a logically complete sentence — never cuts mid-phrase.
     */
    private fun capToSentences(text: String, maxSentences: Int = 4): String {
        val sentenceEnd = Regex("""[.!?](?:\s|$)""")
        val matches = sentenceEnd.findAll(text).toList()
        if (matches.size <= maxSentences) return text
        val cutAt = matches[maxSentences - 1].range.last + 1
        return text.substring(0, cutAt).trimEnd()
    }

    override fun onCleared() {
        super.onCleared()
        llama.close()
    }
}
