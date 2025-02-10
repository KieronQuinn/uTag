package com.kieronquinn.app.utag.ui.screens.widget.picker

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigation
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.LocationRepository
import com.kieronquinn.app.utag.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WidgetDevicePickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onSelectedChanged(deviceId: String, selected: Boolean)
    abstract fun close()

    sealed class State {
        data object Loading: State()
        data class Loaded(val categories: List<Category>, val selected: List<String>): State()
        data object Error: State()
    }

    sealed class Category(open val items: List<Item>) {
        data class Mine(override val items: List<Item>): Category(items)
        data class Shared(val ownerName: String?, override val items: List<Item>): Category(items)
    }

    data class Item(
        val label: String,
        val icon: String,
        val shortcutIcon: Bitmap?,
        val deviceId: String,
        val deviceOwner: String?,
        val deviceOwnerId: String?,
        val isDifferentOwner: Boolean
    )

}

class WidgetDevicePickerViewModelImpl(
    private val navigation: WidgetContainerNavigation,
    deviceRepository: DeviceRepository,
    apiRepository: ApiRepository,
    userRepository: UserRepository,
    locationRepository: LocationRepository,
    selectedDeviceIds: Array<String>,
    private val popUpTo: Int,
    knownDeviceIds: Array<String>
): WidgetDevicePickerViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val selectedDeviceIds = MutableStateFlow(selectedDeviceIds.toSet())
    private val filterDeviceIds = knownDeviceIds.takeIf { it.isNotEmpty() }

    private val devices = flow {
        emit(deviceRepository.getDevices())
    }

    private val fmmDevices = resumeBus.mapLatest {
        apiRepository.getDevices()
    }

    private val user = flow {
        emit(userRepository.getUserInfo()?.toMember())
    }

    private val users = flow {
        emit(locationRepository.getAllUsers())
    }

    private val categories = combine(
        devices,
        fmmDevices,
        user,
        users
    ) { devices, fmm, user, users ->
        if(devices == null || fmm == null || user == null) return@combine null
        val owner = fmm.ownerId
        val members = users ?: emptyMap()
        val mine = devices.filter { it.ownerId == owner }.mapNotNull {
            it.toItem(filterDeviceIds, members, owner)
        }.sortedBy { it.label.lowercase() }.let { Category.Mine(it) }
        val shared = devices.asSequence().filterNot { it.ownerId == owner }.mapNotNull {
            it.toItem(filterDeviceIds, members, owner)
        }.sortedBy { it.label.lowercase() }.groupBy { it.deviceOwnerId }.map {
            Category.Shared(it.value.first().deviceOwner, it.value)
        }.toList().toTypedArray()
        listOf(mine, *shared)
    }

    override val state = combine(categories, this.selectedDeviceIds) { categories, selected ->
        if(!categories.isNullOrEmpty()) {
            State.Loaded(categories, selected.toList())
        }else{
            State.Error
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onSelectedChanged(deviceId: String, selected: Boolean) {
        viewModelScope.launch {
            if(selected) {
                selectedDeviceIds.emit(selectedDeviceIds.value.plus(deviceId))
            }else{
                selectedDeviceIds.emit(selectedDeviceIds.value.minus(deviceId))
            }
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateUpTo(popUpTo)
        }
    }

    private fun DeviceInfo.toItem(
        knownDeviceIds: Array<String>?,
        members: Map<String, String>,
        myId: String
    ): Item? {
        if(knownDeviceIds != null && !knownDeviceIds.contains(deviceId)) return null
        val deviceOwner = members[ownerId]
        return Item(
            label = label,
            icon = icon,
            shortcutIcon = markerIcons.first,
            deviceId = deviceId,
            deviceOwner = deviceOwner,
            deviceOwnerId = ownerId.takeIf { deviceOwner != null },
            isDifferentOwner = ownerId != myId
        )
    }

    private fun UserInfoResponse.toMember(): Member {
        return Member(uuid, fullName)
    }

    private data class Member(
        val uuid: String,
        val name: String
    )

}