package com.example.qwen2gguf.agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.example.qwen2gguf.LlamaAndroid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log

private const val TAG = "LlamaPromptExecutor"

/** Identifies our local Qwen3 model to Koog. */
val LocalQwen3Provider = LLMProvider("local-llama", "Local llama.cpp")
val LocalQwen3Model = LLModel(
    provider = LocalQwen3Provider,
    id = "qwen3-local",
    capabilities = null,
)

/**
 * Koog [PromptExecutor] that routes prompts to the on-device llama.cpp runtime.
 *
 * Tool-calling contract (Qwen3 plain-text format):
 *  - Available tools are injected into the system prompt as a JSON schema block inside <tools>…</tools>.
 *  - The model signals a tool call by outputting:
 *      <tool_call>
 *      {"name": "toolName", "arguments": {"arg": "value"}}
 *      </tool_call>
 *  - Tool results are fed back as <|im_start|>tool … <|im_end|> turns.
 */
class LlamaPromptExecutor(
    private val llama: LlamaAndroid,
    private val temperature: Float = 0.7f,
    private val maxTokens: Int = 1024,
) : PromptExecutor() {

    // ── Public Koog API ───────────────────────────────────────────────────────

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant {
        val chatMl = buildChatMl(prompt, tools)
        Log.d(TAG, "execute() prompt length=${chatMl.length}, tools=${tools.map { it.name }}")

        val fullText = llama.generate(chatMl, maxTokens, temperature).toList().joinToString("")
        Log.d(TAG, "execute() raw response: $fullText")

        return parseResponse(fullText)
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = flow {
        val chatMl = buildChatMl(prompt, tools)
        Log.d(TAG, "executeStreaming() prompt length=${chatMl.length}")

        val buffer = StringBuilder()
        llama.generate(chatMl, maxTokens, temperature).collect { token ->
            buffer.append(token)
            emit(StreamFrame.TextDelta(token))
        }
        emit(StreamFrame.End())
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        throw UnsupportedOperationException("Moderation not supported by local llama.cpp")

    override fun close() { /* llama lifecycle managed by ViewModel */ }

    // ── ChatML builder ────────────────────────────────────────────────────────

    /**
     * Converts a Koog [Prompt] (list of [Message]) into a Qwen3 ChatML string.
     * If tools are provided they are injected into the system message.
     */
    private fun buildChatMl(prompt: Prompt, tools: List<ToolDescriptor>): String = buildString {
        for (msg in prompt.messages) {
            when (msg) {
                is Message.System -> {
                    val systemText = msg.textContent()
                    val toolSection = if (tools.isNotEmpty()) buildToolSection(tools) else ""
                    append("<|im_start|>system\n$systemText$toolSection<|im_end|>\n")
                }
                is Message.User -> {
                    // A User message may carry tool results (MessagePart.Tool.Result) or plain text.
                    // Koog has no separate Message.Tool type — tool results arrive as Message.User.
                    val toolResults = msg.parts.filterIsInstance<MessagePart.Tool.Result>()
                    if (toolResults.isNotEmpty()) {
                        for (result in toolResults) {
                            append("<|im_start|>tool\n<tool_response>\n${result.output}\n</tool_response>\n<|im_end|>\n")
                        }
                    } else {
                        val text = msg.parts.filterIsInstance<MessagePart.Text>().joinToString("\n") { it.text }
                        append("<|im_start|>user\n$text /no_think<|im_end|>\n")
                    }
                }
                is Message.Assistant -> {
                    val toolCalls = msg.parts.filterIsInstance<MessagePart.Tool.Call>()
                    val textParts = msg.parts.filterIsInstance<MessagePart.Text>()
                    if (toolCalls.isNotEmpty()) {
                        append("<|im_start|>assistant\n")
                        for (call in toolCalls) {
                            append("<tool_call>\n{\"name\": \"${call.tool}\", \"arguments\": ${call.args}}\n</tool_call>\n")
                        }
                        append("<|im_end|>\n")
                    } else {
                        val text = textParts.joinToString("\n") { it.text }
                        append("<|im_start|>assistant\n$text<|im_end|>\n")
                    }
                }
                else -> {
                    Log.w(TAG, "Unhandled message type: ${msg::class.simpleName}")
                }
            }
        }
        // Open the assistant turn for generation
        append("<|im_start|>assistant\n")
    }

    /**
     * Builds the tools block injected into the system prompt.
     * Uses the Qwen3 official tool-calling format.
     */
    private fun buildToolSection(tools: List<ToolDescriptor>): String = buildString {
        append("\n\n# Tools\n\n")
        append("You may call one or more of the following functions to help answer the user.\n")
        append("When you need to call a function, output ONLY the tool call block — no other text before it:\n\n")
        append("<tools>\n")
        for (tool in tools) {
            val schema = tool.requiredParameters
                .joinToString(", ") { p -> "\"${p.name}\": {\"type\": \"string\", \"description\": \"${p.description}\"}" }
            val required = tool.requiredParameters.joinToString(", ") { "\"${it.name}\"" }
            append("""{"type": "function", "function": {"name": "${tool.name}", "description": "${tool.description}", "parameters": {"type": "object", "properties": {$schema}, "required": [$required]}}}""")
            append("\n")
        }
        append("</tools>\n\n")
        append("To call a function, output:\n")
        append("<tool_call>\n{\"name\": \"function_name\", \"arguments\": {\"arg1\": \"value1\"}}\n</tool_call>\n")
    }

    // ── Response parser ───────────────────────────────────────────────────────

    private val toolCallRegex = Regex(
        """<tool_call>\s*(\{.*?})\s*</tool_call>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    /**
     * Parses the raw generated text into a [Message.Assistant].
     * If the text contains `<tool_call>` blocks they are extracted as [MessagePart.Tool.Call] parts.
     * Otherwise the text is returned as a plain [MessagePart.Text].
     */
    private fun parseResponse(raw: String): Message.Assistant {
        // Strip Qwen3 <think>…</think> block
        val text = raw.replace(Regex("""^\s*<think>.*?</think>\s*""", RegexOption.DOT_MATCHES_ALL), "").trim()

        val toolMatches = toolCallRegex.findAll(text).toList()
        return if (toolMatches.isNotEmpty()) {
            val toolCallParts = toolMatches.mapNotNull { match ->
                runCatching {
                    val json = Json.parseToJsonElement(match.groupValues[1]) as JsonObject
                    val name = json["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val args = json["arguments"]?.toString() ?: "{}"
                    MessagePart.Tool.Call(tool = name, args = args)
                }.getOrNull()
            }
            if (toolCallParts.isNotEmpty()) {
                Log.d(TAG, "Parsed ${toolCallParts.size} tool call(s): ${toolCallParts.map { it.tool }}")
                Message.Assistant(parts = toolCallParts, metaInfo = ResponseMetaInfo.Empty)
            } else {
                textAssistantMessage(text)
            }
        } else {
            textAssistantMessage(text)
        }
    }

    private fun textAssistantMessage(text: String) =
        Message.Assistant(
            parts = listOf(MessagePart.Text(text)),
            metaInfo = ResponseMetaInfo.Empty,
        )
}
