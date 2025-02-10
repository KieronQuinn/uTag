package com.kieronquinn.app.utag.ui.screens.unknowntag.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.UnknownTagNavigation
import com.kieronquinn.app.utag.repositories.GeocoderRepository
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.screens.unknowntag.tag.UnknownTagViewModel.State.Loaded.Location
import com.kieronquinn.app.utag.utils.extensions.groupConsecutiveBy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

abstract class UnknownTagViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onSafeClicked()
    abstract fun close()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val privacyId: String,
            val locations: List<Location>,
            val mapStyle: MapStyle,
            val mapTheme: MapTheme
        ): State() {
            data class Location(
                val timestamp: LocalDateTime,
                val location: LatLng,
                val address: String?
            )
        }
        data object Error: State()
    }

}

class UnknownTagViewModelImpl(
    private val nonOwnerTagRepository: NonOwnerTagRepository,
    private val standaloneNavigation: UnknownTagNavigation,
    private val settingsNavigation: SettingsNavigation,
    settings: SettingsRepository,
    geocoderRepository: GeocoderRepository,
    privacyId: String,
    private val isStandalone: Boolean
): UnknownTagViewModel() {

    init {
        nonOwnerTagRepository.acknowledgeUnknownTag(privacyId)
    }

    private val tag = nonOwnerTagRepository.getUnknownTags().map {
        it.firstOrNull { tag -> tag.privacyId == privacyId }
    }

    private val locations = tag.mapLatest {
        if(it == null) return@mapLatest null
        it.detections.map { location ->
            Location(
                Instant.ofEpochMilli(location.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                location.location,
                geocoderRepository.geocode(location.location, true)
            )
        }.groupConsecutiveBy { a, b ->
            //Group by either the address being the same if not null, or the LatLngs matching
            (a.address != null && b.address != null && a.address == b.address) || a.location == b.location
        }.let { list ->
            list.mapIndexed { index, locations ->
                if (index == 0) {
                    locations.first()
                } else {
                    locations.last()
                }
            }
        }
    }

    override val state = combine(
        locations,
        settings.mapStyle.asFlow(),
        settings.mapTheme.asFlow()
    ) { locations, style, theme ->
        if(locations != null) {
            State.Loaded(privacyId, locations, style, theme)
        }else{
            State.Error
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onSafeClicked() {
        val privacyId = (state.value as? State.Loaded)?.privacyId ?: return
        nonOwnerTagRepository.markUnknownTagAsSafe(privacyId)
        close()
    }

    override fun close() {
        viewModelScope.launch {
            if(isStandalone) {
                standaloneNavigation.navigateBack()
            }else{
                settingsNavigation.navigateBack()
            }
        }
    }

}