package com.kieronquinn.app.utag.ui.screens.setup.account

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.setup.account.SetupAccountViewModel.State
import com.kieronquinn.app.utag.utils.extensions.appendBullet
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupAccountFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SetupAccountViewModel>()

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
            is State.SignIn -> setContent()
            is State.Error -> setError()
            is State.OpenBrowser -> {
                setLoading(true)
                viewModel.onOpenBrowser(state.url)
            }
        }
    }

    private fun setContent() {
        setPreferences {
            actionCardPreference {
                title = getString(R.string.account_title)
                summary = getString(R.string.account_content)
                addButton(getString(R.string.account_sign_in)) {
                    viewModel.onSignInClicked()
                }
            }
            preferenceCategory("info") {
                tipsCardPreference {
                    title = getString(R.string.account_warning_title)
                    summary = createInfo(
                        R.string.account_warning_content_header,
                        R.array.account_warning_content_items,
                        R.string.account_warning_content_footer
                    )
                }
            }
        }
    }

    private fun setError() {
        setPreferences {
            actionCardPreference {
                title = getString(R.string.account_error_title)
                summary = getString(R.string.account_error_content)
                addButton(getString(R.string.account_error_try_again)) {
                    viewModel.onSignInClicked()
                }
            }
        }
    }

    private fun createInfo(
        @StringRes header: Int,
        @ArrayRes items: Int,
        @StringRes footer: Int
    ) = SpannableStringBuilder().apply {
        appendLine(getText(header))
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