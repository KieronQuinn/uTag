package com.kieronquinn.app.utag.ui.screens.tag.picker

import android.content.Context
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.components.navigation.TagPickerNavigation
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.networking.model.smartthings.DevicesResponse
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.LocationRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagDevicePickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun close()
    abstract fun onFavouritesClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val categories: List<Category>,
            val selected: String,
            val favouritesAvailable: Boolean
        ): State()
        data object Error: State()
    }

    sealed class Category(open val items: List<Item>) {
        data class Favourites(override val items: List<Item>): Category(items)
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
        val isDifferentOwner: Boolean,
        val isShortcutAvailable: Boolean,
        val isAllowed: Boolean,
        val isAgreementNeeded: Boolean
    )

}

class TagDevicePickerViewModelImpl(
    private val containerNavigation: TagContainerNavigation,
    private val navigation: TagPickerNavigation,
    deviceRepository: DeviceRepository,
    apiRepository: ApiRepository,
    userRepository: UserRepository,
    locationRepository: LocationRepository,
    smartTagRepository: SmartTagRepository,
    context: Context,
    selectedDeviceId: String,
    private val knownDeviceIds: Array<String>
): TagDevicePickerViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val shortcutManager =
        context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

    private val addedShortcutIds = resumeBus.mapLatest {
        shortcutManager.pinnedShortcuts.map { it.id }
    }

    private val shortcutsAvailable = resumeBus.mapLatest {
        shortcutManager.isRequestPinShortcutSupported
    }

    private val devices = flow {
        emit(deviceRepository.getDevices(true))
    }

    private val fmmDevices = resumeBus.mapLatest {
        apiRepository.getDevices()
    }

    private val tags = devices.flatMapLatest {
        if(it.isNullOrEmpty()) return@flatMapLatest flowOf(emptyList())
        val states = it.map { deviceId ->
            smartTagRepository.getTagState(deviceId.deviceId)
        }
        combine(
            *states.toTypedArray()
        ) { items ->
            items.toList()
        }
    }

    private val user = flow {
        emit(userRepository.getUserInfo()?.toMember())
    }

    private val users = flow {
        emit(locationRepository.getAllUsers())
    }

    private val shortcut = combine(
        shortcutsAvailable,
        addedShortcutIds
    ) { available, addedIds ->
        Pair(available, addedIds)
    }

    private val deviceInfo = combine(
        devices,
        fmmDevices
    ) { devices, fmmDevices ->
        Pair(devices, fmmDevices)
    }

    private val categories = combine(
        deviceInfo,
        user,
        users,
        shortcut,
        tags
    ) { deviceInfo, user, users, shortcut, tags ->
        val devices = deviceInfo.first
        val fmm = deviceInfo.second
        if(devices == null || fmm == null || user == null) return@combine null
        val shortcutsAvailable = shortcut.first
        val shortcutIds = shortcut.second
        val owner = fmm.ownerId
        val members = users ?: emptyMap()
        val fmmDevices = devices.associateBy { it.deviceId }
        val shortcutAvailable = { deviceId: String ->
            shortcutsAvailable && !shortcutIds.contains(deviceId)
        }
        val favourites = fmm.favourites.mapNotNull {
            val deviceId = it.stDid ?: return@mapNotNull null
            fmmDevices[deviceId]?.toItem(
                knownDeviceIds,
                members,
                tags,
                owner,
                shortcutAvailable(deviceId)
            ) ?: return@mapNotNull null
        }.sortedBy { it.label.lowercase() }.let { Category.Favourites(it) }
        val mine = devices.filter { it.ownerId == owner }.mapNotNull {
            it.toItem(knownDeviceIds, members, tags, owner, shortcutAvailable(it.deviceId))
        }.sortedBy { it.label.lowercase() }.let { Category.Mine(it) }
        val shared = devices.asSequence().filterNot { it.ownerId == owner }.mapNotNull {
            it.toItem(knownDeviceIds, members, tags, owner, shortcutAvailable(it.deviceId))
        }.sortedBy { it.label.lowercase() }.groupBy { it.deviceOwnerId }.map {
            Category.Shared(it.value.first().deviceOwner, it.value)
        }.toList().toTypedArray()
        val favouritesAvailable = devices.any { it.isOwner }
        Pair(listOf(favourites, mine, *shared), favouritesAvailable)
    }

    override val state = categories.mapLatest {
        val items = it?.first
        val favouritesAvailable = it?.second == true
        if(!items.isNullOrEmpty()) {
            State.Loaded(items, selectedDeviceId, favouritesAvailable)
        }else{
            State.Error
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun close() {
        viewModelScope.launch {
            containerNavigation.navigateBack()
        }
    }

    override fun onFavouritesClicked() {
        viewModelScope.launch {
            navigation.navigate(TagDevicePickerFragmentDirections
                .actionTagDevicePickerFragmentToTagDevicePickerFavouritesFragment(knownDeviceIds))
        }
    }

    private fun DeviceInfo.toItem(
        knownDeviceIds: Array<String>,
        members: Map<String, String>,
        tagStates: List<TagState>,
        myId: String,
        shortcutAvailable: Boolean
    ): Item? {
        //Ignore all Tags that we don't know about, such as new refreshes, but include not allowed
        val isAllowed = isOwner || shareable
        if(!knownDeviceIds.contains(deviceId) && isAllowed) return null
        val deviceOwner = members[ownerId]
        val tagState = tagStates.firstOrNull { it.deviceId == deviceId }
        val isAgreementNeeded = !isOwner && isPossiblySharable &&
                (tagState as? TagState.Loaded)?.requiresAgreement() ?: false
        return Item(
            label = label,
            icon = icon,
            shortcutIcon = markerIcons.first,
            deviceId = deviceId,
            deviceOwner = deviceOwner,
            deviceOwnerId = ownerId.takeIf { deviceOwner != null },
            isDifferentOwner = ownerId != myId,
            isShortcutAvailable = shortcutAvailable,
            isAllowed = isAllowed,
            isAgreementNeeded = isAgreementNeeded
        )
    }

    private fun UserInfoResponse.toMember(): Member {
        return Member(uuid, fullName)
    }

    private fun DevicesResponse.Member.toMember(): Member {
        return Member(stOwnerId, name)
    }

    private data class Member(
        val uuid: String,
        val name: String
    )

}