package com.kieronquinn.app.utag.model

import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction

data class CachedRules(
    val rules: List<Rule>
) {
    data class Rule(val deviceId: String, val actions: List<TagButtonAction>)
}
