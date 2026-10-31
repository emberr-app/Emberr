package com.emberr.domain.credentials

import com.emberr.data.local.prefs.SettingsManager
import kotlinx.serialization.json.Json

class PasswordGeneratorOptionsStore(private val settingsManager: SettingsManager) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): PasswordGeneratorOptions {
        val rawJson = settingsManager.getPasswordGeneratorOptionsJson() ?: return PasswordGeneratorOptions()
        return try {
            json.decodeFromString<PasswordGeneratorOptions>(rawJson)
                .takeIf { options -> options.enabledCharacterSetCount > 0 }
                ?: PasswordGeneratorOptions()
        } catch (_: Exception) {
            PasswordGeneratorOptions()
        }
    }

    fun save(options: PasswordGeneratorOptions) {
        settingsManager.savePasswordGeneratorOptionsJson(json.encodeToString(options))
    }
}
