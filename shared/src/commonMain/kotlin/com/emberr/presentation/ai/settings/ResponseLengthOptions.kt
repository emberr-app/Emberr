package com.emberr.presentation.ai.settings

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_response_length_balanced
import emberr.shared.generated.resources.ai_response_length_balanced_subtitle
import emberr.shared.generated.resources.ai_response_length_long
import emberr.shared.generated.resources.ai_response_length_long_subtitle
import emberr.shared.generated.resources.ai_response_length_max
import emberr.shared.generated.resources.ai_response_length_max_subtitle
import emberr.shared.generated.resources.ai_response_length_short
import emberr.shared.generated.resources.ai_response_length_short_subtitle
import org.jetbrains.compose.resources.StringResource

internal data class ResponseLengthOption(val label: StringResource, val subtitle: StringResource, val tokens: Int)

internal val responseLengthOptions = listOf(
    ResponseLengthOption(Res.string.ai_response_length_short, Res.string.ai_response_length_short_subtitle, 512),
    ResponseLengthOption(Res.string.ai_response_length_balanced, Res.string.ai_response_length_balanced_subtitle, 1024),
    ResponseLengthOption(Res.string.ai_response_length_long, Res.string.ai_response_length_long_subtitle, 2048),
    ResponseLengthOption(Res.string.ai_response_length_max, Res.string.ai_response_length_max_subtitle, 4096)
)
