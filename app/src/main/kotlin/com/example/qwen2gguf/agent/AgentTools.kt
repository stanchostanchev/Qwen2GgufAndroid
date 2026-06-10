package com.example.qwen2gguf.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.qwen2gguf.ui.FairyTaleTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tools available to the on-device Koog agent.
 *
 * Discovered automatically via reflection through [ToolSet.asTools].
 * Each function annotated with @[Tool] becomes a callable tool that the model
 * can invoke during its reasoning loop.
 */
@LLMDescription("A set of utilities the assistant can use to answer questions and help the user.")
class AgentTools : ToolSet {

    @Tool
    @LLMDescription("Returns today's date in YYYY-MM-DD format.")
    fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @Tool
    @LLMDescription(
        "Lists all available fairy tale themes the user can choose from. " +
        "Call this when the user asks what themes or categories are available."
    )
    fun listFairyTaleThemes(): String {
        return FairyTaleTheme.entries.joinToString(", ") { "${it.emoji} ${it.displayName}" }
    }

    @Tool
    @LLMDescription(
        "Returns a random story prompt suggestion for a given fairy tale theme. " +
        "Valid themes: PRINCESSES, DRAGONS, KNIGHTS, WITCHES, MERMAIDS. " +
        "Use this when the user wants inspiration or a prompt idea for a theme."
    )
    fun getRandomPromptForTheme(
        @LLMDescription("The theme name in uppercase, e.g. DRAGONS or PRINCESSES.")
        theme: String,
    ): String {
        val found = FairyTaleTheme.entries.firstOrNull {
            it.name.equals(theme.trim(), ignoreCase = true) ||
            it.displayName.equals(theme.trim(), ignoreCase = true)
        }
        return if (found != null) {
            found.prompts.random()
        } else {
            "Unknown theme '$theme'. Available: ${FairyTaleTheme.entries.joinToString { it.name }}"
        }
    }

    @Tool
    @LLMDescription(
        "Returns a random base story for a given fairy tale theme. " +
        "Use this when the user asks for a fairy tale or a story, " +
        "then adapt the base story to their specific request."
    )
    fun getBaseStoryForTheme(
        @LLMDescription("The theme name in uppercase, e.g. DRAGONS or PRINCESSES.")
        theme: String,
    ): String {
        val found = FairyTaleTheme.entries.firstOrNull {
            it.name.equals(theme.trim(), ignoreCase = true) ||
            it.displayName.equals(theme.trim(), ignoreCase = true)
        }
        return if (found != null) {
            val (_, story) = found.examples.random()
            story
        } else {
            "Unknown theme '$theme'. Available: ${FairyTaleTheme.entries.joinToString { it.name }}"
        }
    }
}
