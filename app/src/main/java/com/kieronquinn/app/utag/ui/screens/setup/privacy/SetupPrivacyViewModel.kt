package com.kieronquinn.app.utag.ui.screens.setup.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupPrivacyViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onAgreeClicked()
    abstract fun onDisagreeClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val locationPolicyUrl: String,
            val privacyNoticeUrl: String
        ): State()
        data object Error: State()
    }

}

class SetupPrivacyViewModelImpl(
    private val navigation: SetupNavigation,
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository
): SetupPrivacyViewModel() {

    companion object {
        private const val LOCATION_POLICY_URL =
            "https://smartthingsfind.samsung.com/contents/legal/%s/stf_lbs.html"
    }

    private val hasAgreedToPolicies = settingsRepository.hasAgreedToPolicies

    private val consentDetails = flow {
        emit(authRepository.getConsentDetails())
    }

    override val state = consentDetails.mapLatest {
        if(it != null) {
            State.Loaded(
                LOCATION_POLICY_URL.format(it.region.lowercase()),
                it.uri
            )
        }else{
            State.Error
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onAgreeClicked() {
        viewModelScope.launch {
            hasAgreedToPolicies.set(true)
            navigation.navigate(SetupPrivacyFragmentDirections
                .actionSetupPrivacyFragmentToSetupModFragment())
        }
    }

    override fun onDisagreeClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}