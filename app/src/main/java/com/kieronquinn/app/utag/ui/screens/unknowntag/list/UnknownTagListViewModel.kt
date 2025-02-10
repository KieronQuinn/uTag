package com.kieronquinn.app.utag.ui.screens.unknowntag.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.UnknownTagNavigation
import com.kieronquinn.app.utag.repositories.ChaserRepository
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository.UnknownTag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class UnknownTagListViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onUnknownTagClicked(tag: UnknownTag)
    abstract fun onResetSafeTagsClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(val items: List<UnknownTag>, val tagImages: Map<String, String>): State()
        data object Empty: State()
    }

}

class UnknownTagListViewModelImpl(
    isStandalone: Boolean,
    private val nonOwnerTagRepository: NonOwnerTagRepository,
    chaserRepository: ChaserRepository,
    standaloneNavigation: UnknownTagNavigation,
    settingsNavigation: SettingsNavigation
): UnknownTagListViewModel() {

    private val navigation = if(isStandalone) {
        standaloneNavigation
    }else{
        settingsNavigation
    }

    private val tags = nonOwnerTagRepository.getUnknownTags().map {
        it.filterNot { tag -> tag.isSafe }
    }

    private val tagsWithImages = tags.mapLatest {
        val serviceData = it.map { tag -> tag.detections.first().serviceData.encodedServiceData }
            .toTypedArray()
        Pair(it, chaserRepository.getTagImages(*serviceData))
    }

    override val state = tagsWithImages.map {
        if(it.first.isNotEmpty()) {
            State.Loaded(it.first, it.second)
        }else{
            State.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onUnknownTagClicked(tag: UnknownTag) {
        viewModelScope.launch {
            navigation.navigate(UnknownTagListFragmentDirections
                .actionUnknownTagListFragmentToUnknownTagFragment(tag.privacyId))
        }
    }

    override fun onResetSafeTagsClicked() {
        nonOwnerTagRepository.resetUnknownSafeTags()
    }

    init {
        viewModelScope.launch {
            nonOwnerTagRepository.trimUnknownTags()
        }
    }

}