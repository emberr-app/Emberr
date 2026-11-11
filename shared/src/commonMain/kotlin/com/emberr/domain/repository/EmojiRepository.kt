package com.emberr.domain.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emberr.domain.util.search.searchFormOf
import emberr.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmojiCategoryDto(
    val name: String,
    val slug: String? = null,
    val emojis: List<EmojiDto> = emptyList()
)

@Serializable
data class EmojiDto(
    val emoji: String,
    val name: String,
    val slug: String? = null,
    val skin_tone_support: Boolean? = null,
    val unicode_version: String? = null,
    val emoji_version: String? = null
)

/**
 * Handles parsing the massive JSON file off the main UI thread.
 * Holds the parsed data in memory so it's instantaneous after the first load.
 */
object EmojiRepository {
    var isLoaded by mutableStateOf(false)
        private set

    var categories by mutableStateOf<List<String>>(emptyList())
        private set
    var flatList by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var categoryEmojiLists by mutableStateOf<List<List<String>>>(emptyList())
        private set

    var translatedNames by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    private var translatedNamesFile: String? = null

    private const val EMOJI_FILE_PATH = "files/data-by-group.json"
    private const val EMOJI_NAMES_FOLDER = "files/emoji-names"
    private val languagesWithEmojiNames = setOf(
        "es", "pt", "fr", "it", "de", "nl", "pl", "uk", "ru", "tr", "id", "vi", "hi", "ja", "ko", "zh"
    )

    // Ignore extra fields in the JSON to prevent crashes
    private val json = Json { ignoreUnknownKeys = true }

    fun emojiNamesFileFor(language: String, region: String): String? {
        val languageCode = if (language == "in") "id" else language
        return when {
            languageCode == "pt" && region == "PT" -> "pt-PT"
            languageCode in languagesWithEmojiNames -> languageCode
            else -> null
        }
    }

    suspend fun loadIfNeeded(emojiNamesFile: String?) {
        loadEmojisIfNeeded()
        loadTranslatedNamesIfNeeded(emojiNamesFile)
    }

    private suspend fun loadTranslatedNamesIfNeeded(emojiNamesFile: String?) {
        if (emojiNamesFile == translatedNamesFile) return
        withContext(Dispatchers.IO) {
            translatedNames = emojiNamesFile?.let { fileName ->
                try {
                    val jsonString = Res.readBytes("$EMOJI_NAMES_FOLDER/$fileName.json").decodeToString()
                    json.decodeFromString<Map<String, String>>(jsonString).mapValues { (_, words) -> searchFormOf(words) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.orEmpty()
            translatedNamesFile = emojiNamesFile
        }
    }

    private suspend fun loadEmojisIfNeeded() {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            try {
                val jsonString = Res.readBytes(EMOJI_FILE_PATH).decodeToString()
                val data = json.decodeFromString<List<EmojiCategoryDto>>(jsonString)

                categories = data.map { it.name }
                categoryEmojiLists = data.map { cat -> cat.emojis.map { it.emoji } }
                // Flatten to (emoji -> name) pairs for ultra-fast searching
                flatList = data.flatMap { cat -> cat.emojis.map { it.emoji to searchFormOf(it.name) } }

                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}