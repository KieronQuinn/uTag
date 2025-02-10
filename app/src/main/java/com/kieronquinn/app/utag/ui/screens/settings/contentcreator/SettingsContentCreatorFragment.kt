package com.kieronquinn.app.utag.ui.screens.settings.contentcreator

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.contentcreator.SettingsContentCreatorViewModel.State
import com.kieronquinn.app.utag.utils.extensions.appendBullet
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsContentCreatorFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsContentCreatorViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> setLoading(true)
            is State.Loaded -> handleContent(state)
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        switchBarPreference {
            title = getString(R.string.settings_content_creator_enabled)
            isChecked = state.enabled
            onChange<Boolean> {
                viewModel.onEnabledChanged(it)
            }
        }
        preferenceCategory("content_creator_info") {
            tipsCardPreference {
                title = getString(R.string.settings_content_creator_info_title)
                summary = getString(R.string.settings_content_creator_info_content)
            }
            tipsCardPreference {
                title = getString(R.string.settings_content_creator_does_title)
                summary = createInfo(
                    R.array.settings_content_creator_does_items,
                    R.string.settings_content_creator_does_footer
                )
            }
        }
    }

    private fun createInfo(
        @ArrayRes items: Int,
        @StringRes footer: Int
    ) = SpannableStringBuilder().apply {
        val footerItems = resources.getTextArray(items)
        footerItems.forEachIndexed { i, item ->
            appendBullet()
            append(item)
            if(i < footerItems.size - 1) {
                appendLine()
            }
        }
        appendLine()
        appendLine()
        append(getText(footer))
    }

}