package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.model.CachedRules
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.networking.model.smartthings.GetRulesResponse
import com.kieronquinn.app.utag.networking.model.smartthings.GetRulesResponse.Item
import com.kieronquinn.app.utag.networking.services.RulesService
import com.kieronquinn.app.utag.repositories.CacheRepository.Companion.getCache
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.utils.extensions.get
import retrofit2.Retrofit
import java.io.IOException

interface RulesRepository {

    suspend fun getInUseActions(): Map<String, List<TagButtonAction>>?

    enum class TagButtonAction {
        PRESS, HOLD
    }

}

class RulesRepositoryImpl(
    private val cacheRepository: CacheRepository,
    retrofit: Retrofit,
    context: Context
): RulesRepository {

    private val service = RulesService.createService(context, retrofit)

    override suspend fun getInUseActions(): Map<String, List<TagButtonAction>>? {
        //Because of the potential size of the actions response, we cache just what we need
        return try {
            val rules = service.getRules().get(throwIO = true, name = "rules")?.getActions()?.also {
                val rules = it.map { rule -> CachedRules.Rule(rule.key, rule.value) }
                cacheRepository.setCache(CacheType.RULES, data = CachedRules(rules))
            }
            rules
        }catch (e: IOException) {
            cacheRepository.getCache<CachedRules>(CacheType.RULES)?.let { rules ->
                rules.rules.associate { Pair(it.deviceId, it.actions) }
            }
        }
    }

    private fun GetRulesResponse.getActions(): Map<String, List<TagButtonAction>> {
        return items?.asSequence()?.mapNotNull { item ->
            item.actions?.mapNotNull { action ->
                Pair(
                    action.getDeviceId() ?: return@mapNotNull null,
                    action.getTagButtonAction().takeIf { item.status == "Enabled" }
                )
            }
        }?.flatten()?.groupBy {
            it.first
        }?.map {
            Pair(it.key, it.value.mapNotNull { item -> item.second })
        }?.toList()?.toMap() ?: emptyMap()
    }

    private fun Item.Action.getDeviceId(): String? {
        if(`if`?.equals?.left?.device?.capability != "tag.tagButton") return null
        if(`if`.equals.left.device.attribute != "tagButton") return null
        if(`if`.equals.left.device.trigger != "Always") return null
        return `if`.equals.left.device.deviceIds?.firstOrNull()
    }

    private fun Item.Action.getTagButtonAction(): TagButtonAction? {
        return when(`if`?.equals?.right?.string) {
            "pushed" -> TagButtonAction.PRESS
            "held" -> TagButtonAction.HOLD
            else -> null
        }
    }

}