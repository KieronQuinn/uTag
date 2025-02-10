package com.kieronquinn.app.utag.ui.screens.tag.picker.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.networking.model.smartthings.SetFavouritesItem
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.LocationRepository
import com.kieronquinn.app.utag.utils.extensions.isLoadingDelayed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class TagDevicePickerFavouritesViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun close()
    abstract fun onFavouriteChanged(deviceId: String, checked: Boolean)
    abstract fun onFavouriteAllClicked()
    abstract fun onUnfavouriteAllClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(val items: List<Item>, val isSending: Boolean): State()
        data object Error: State()
    }

    data class Item(
        val label: String,
        val icon: String,
        val deviceId: String,
        val isFavourite: Boolean
    )

    enum class Event {
        ERROR
    }

}

class TagDevicePickerFavouritesViewModelImpl(
    private val apiRepository: ApiRepository,
    private val navigation: SettingsNavigation,
    deviceRepository: DeviceRepository,
    locationRepository: LocationRepository,
    knownDeviceIds: Array<String>
): TagDevicePickerFavouritesViewModel() {

    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val sending = MutableStateFlow(false)
    private val isLoading = isLoadingDelayed()
    private val sendLock = Mutex()

    private val devices = flow {
        emit(deviceRepository.getDevices())
    }

    private val fmmDevices = refreshBus.mapLatest {
        apiRepository.getDevices()
    }

    private val categories = combine(
        devices,
        fmmDevices
    ) { devices, fmm ->
        if(devices == null || fmm == null) return@combine null
        val owner = fmm.ownerId
        val favourites = fmm.favourites.mapNotNull { it.stDid }
        devices.filter { it.ownerId == owner }.mapNotNull {
            it.toItem(knownDeviceIds, favourites)
        }.sortedBy { it.label.lowercase() }
    }.onEach {
        isLoading.emit(false)
    }

    override val events = MutableSharedFlow<Event>()

    override val state = combine(
        categories,
        sending,
        isLoading
    ) { categories, sending, loading ->
        if(!categories.isNullOrEmpty()) {
            State.Loaded(categories, sending || loading == true)
        }else{
            State.Error
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onFavouriteChanged(deviceId: String, checked: Boolean) {
        viewModelScope.launch {
            val ids = getAllIds() ?: return@launch
            val newList = if(checked) {
                ids.plus(deviceId)
            }else{
                ids.minus(deviceId)
            }
            setFavourites(newList.toList())
        }
    }

    override fun onFavouriteAllClicked() {
        viewModelScope.launch {
            setFavourites(getAllIds()?.toList() ?: return@launch)
        }
    }

    override fun onUnfavouriteAllClicked() {
        viewModelScope.launch {
            setFavourites(emptyList())
        }
    }

    private suspend fun setFavourites(ids: List<String>): Boolean = sendLock.withLock {
        sending.emit(true)
        val items = ids.mapIndexed { index, id ->
            SetFavouritesItem(index, id)
        }
        val result = apiRepository.setFavourites(items)
        if (result) {
            refreshBus.emit(System.currentTimeMillis())
            isLoading.emit(true)
        } else {
            events.emit(Event.ERROR)
        }
        sending.emit(false)
        result
    }

    private fun getAllIds(): Set<String>? {
        return (state.value as? State.Loaded)?.items?.map { it.deviceId }?.toSet()
    }

    private fun DeviceInfo.toItem(
        knownDeviceIds: Array<String>,
        favourites: List<String>
    ): Item? {
        //Ignore all Tags that we don't know about, such as new refreshes (restart to see those)
        if(!knownDeviceIds.contains(deviceId)) return null
        return Item(
            label = label,
            icon = icon,
            deviceId = deviceId,
            isFavourite = favourites.contains(deviceId)
        )
    }

    private data class Member(
        val uuid: String,
        val name: String
    )

}