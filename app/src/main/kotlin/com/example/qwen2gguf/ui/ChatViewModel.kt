package com.example.qwen2gguf.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAgentBuilder
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistryBuilder
import com.example.qwen2gguf.AssetExtractor
import com.example.qwen2gguf.DeviceInfo
import com.example.qwen2gguf.LlamaAndroid
import com.example.qwen2gguf.agent.AgentTools
import com.example.qwen2gguf.agent.LlamaPromptExecutor
import com.example.qwen2gguf.agent.LocalQwen3Model
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
                        nCtx = if (model.isQwen3) 8192 else 4096,
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
        val isAgent = _uiState.value.selectedSkill == Skill.AGENT
        val assistantSeed = if (isFairyTale) "Once upon a time," else ""
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg + ChatMessage(ChatMessage.Role.Assistant, assistantSeed),
                isGenerating = true,
                error = null,
            )
        }

        generateJob = viewModelScope.launch {
            if (isAgent) {
                runAgentTurn(userText.trim())
            } else {
                runDirectGeneration(isFairyTale)
            }
        }
    }

    // ── Agent turn via Koog ───────────────────────────────────────────────────

    private suspend fun runAgentTurn(userText: String) {
        try {
            // Koog builds a fresh prompt each turn — the old KV cache from the previous
            // agent run would cause n_new = n_tokens - n_past to go negative, skipping
            // prefill entirely and producing garbage on the second and later turns.
            withContext(Dispatchers.IO) { llama.resetCache() }
            val toolSteps = mutableListOf<ToolStep>()
            val executor = LlamaPromptExecutor(
                llama = llama,
                temperature = 0.7f,
                maxTokens = 1024,
                onToolStep = { name, args, result -> toolSteps.add(ToolStep(name, args, result)) },
            )
            val registry = ToolRegistryBuilder().tools(AgentTools()).build()

            val systemPrompt = _uiState.value.selectedSkill.systemPrompt
            val agentConfig = AIAgentConfig.withSystemPrompt(
                prompt = systemPrompt,
                llm = LocalQwen3Model,
                maxAgentIterations = 10,
            )

            val agent: AIAgent<String, String> = GraphAgentBuilder<String, String>(
                strategy = singleRunStrategy(),
                promptExecutor = executor,
                toolRegistry = registry,
                config = agentConfig,
            ).build()

            // Run the agent — it will call tools and produce a final answer
            val answer = withContext(Dispatchers.IO) { agent.run(userText) }

            _uiState.update { state ->
                val updated = state.messages.toMutableList()
                val last = updated.last()
                if (last.role == ChatMessage.Role.Assistant) {
                    updated[updated.lastIndex] = last.copy(
                        content = stripThinkingBlock(answer ?: ""),
                        toolSteps = toolSteps,
                    )
                }
                state.copy(messages = updated, isGenerating = false, gpuFallback = llama.gpuFailed)
            }

            agent.close()
        } catch (e: Exception) {
            Log.e(TAG, "Agent run failed", e)
            // Remove the empty placeholder assistant bubble so the chat doesn't show a blank bubble
            _uiState.update { state ->
                val trimmed = state.messages.dropLastWhile {
                    it.role == ChatMessage.Role.Assistant && it.content.isBlank()
                }
                state.copy(messages = trimmed, error = e.message, isGenerating = false)
            }
        }
    }

    // ── Direct generation (Assistant + Fairy Tale skills) ────────────────────

    private suspend fun runDirectGeneration(isFairyTale: Boolean) {
        if (isFairyTale) {
            withContext(Dispatchers.IO) { llama.resetCache() }
        }
        val (prompt, baseTale) = buildPrompt(_uiState.value.messages.dropLast(1))

        val isQwen3 = _uiState.value.selectedModel.isQwen3
        val maxTokens = when {
            isFairyTale -> 180
            isQwen3 -> 1024
            else -> 512
        }
        val temperature = if (isFairyTale) 0.5f else 0.7f
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
                        val withoutThinking = if (state.selectedModel.isQwen3)
                            stripThinkingBlock(last.content)
                        else last.content
                        val finalContent = if (state.selectedSkill == Skill.FAIRY_TALE)
                            capToSentences(withoutThinking, maxSentences = 4)
                        else withoutThinking
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

    // ── Chat template (Qwen2 & Qwen3 both use ChatML) ────────────────────────

    /**
     * Builds the full prompt and returns it alongside the base tale used (null outside fairy-tale
     * mode).
     *
     * Qwen3 specifics:
     *  - Append " /no_think" to the last user message to suppress the <think> chain-of-thought
     *    block for tasks where speed matters (fairy tales, simple assistant replies).
     *  - For agentic/reasoning tasks leave thinking ON — omit /no_think so the model reasons
     *    step-by-step before answering. The <think>…</think> block is then stripped from the
     *    visible bubble by [stripThinkingBlock].
     */
    private fun buildPrompt(
        history: List<ChatMessage>,
    ): Pair<String, Pair<String, String>?> {
        val state = _uiState.value
        val isFairyTale = state.selectedSkill == Skill.FAIRY_TALE
        val isQwen3 = state.selectedModel.isQwen3
        // For fairy tales and simple assistant mode suppress Qwen3 thinking to save tokens.
        val suppressThinking = isQwen3

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
                val noThinkSuffix = if (suppressThinking) " /no_think" else ""
                val augmented = buildString {
                    appendLine("Base story: ${baseTale.second.trim()}")
                    appendLine("Request: $userRequest$noThinkSuffix")
                    append("Retell the base story above keeping the same plot. Only swap the character types to match the request.")
                }
                append("<|im_start|>user\n$augmented<|im_end|>\n")
            } else {
                for ((idx, msg) in history.withIndex()) {
                    val role = if (msg.role == ChatMessage.Role.User) "user" else "assistant"
                    val isLastUser = msg.role == ChatMessage.Role.User && idx == history.lastIndex
                    val content = if (isLastUser && suppressThinking)
                        "${msg.content} /no_think"
                    else
                        msg.content
                    append("<|im_start|>$role\n$content<|im_end|>\n")
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
     * Cleans raw model output:
     * 1. Strips ChatML role tokens the model sometimes echoes into its output
     *    (<|im_start|>role, <|im_end|>, bare "assistant"/"user" lines).
     * 2. Strips Qwen3 <think>…</think> blocks wherever they appear.
     */
    private fun stripThinkingBlock(text: String): String {
        return text
            .replace(Regex("""<\|im_start\|>(assistant|user|system|tool)\s*"""), "")
            .replace("<|im_end|>", "")
            .replace(Regex("""^(assistant|user|system)\s*\n""", RegexOption.MULTILINE), "")
            .replace(Regex("""<think>.*?</think>\s*""", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
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
        generateJob?.cancel()   // cancel coroutine first
        llama.close()           // sets stopRequested=true, then frees native pointers
    }
}
