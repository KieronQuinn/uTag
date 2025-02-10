package com.kieronquinn.app.utag.ui.screens.settings.advanced.language

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.utils.extensions.getSelectedLanguage
import com.kieronquinn.app.utag.utils.extensions.getSupportedLocales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

abstract class SettingsLanguageViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun reload()
    abstract fun setLanguage(locale: Locale?)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val supportedLocales: List<Locale>,
            val selectedLocale: Locale?,
            val timestamp: Long = System.currentTimeMillis()
        ): State()
    }

}

class SettingsLanguageViewModelImpl(
    private val settingsRepository: SettingsRepository,
    context: Context
): SettingsLanguageViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val localeManager = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
    } else null

    override val state = reloadBus.map {
        val supported = context.getSupportedLocales().sortedBy { it.displayName.lowercase() }
        val selected = context.getSelectedLanguage(supported.map { it.toLanguageTag() })
        State.Loaded(supported, selected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun reload() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun setLanguage(locale: Locale?) {
        viewModelScope.launch {
            if(localeManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                localeManager.applicationLocales = locale?.let {
                    LocaleList(it)
                } ?: LocaleList.getEmptyLocaleList()
                reloadBus.emit(System.currentTimeMillis())
            }else{
                settingsRepository.locale.set(locale?.toLanguageTag() ?: "")
            }
        }
    }

}