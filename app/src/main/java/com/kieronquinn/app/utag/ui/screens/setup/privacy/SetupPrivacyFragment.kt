package com.kieronquinn.app.utag.ui.screens.setup.privacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.updatePadding
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentSetupPrivacyBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.setup.privacy.SetupPrivacyViewModel.State
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupPrivacyFragment: BoundFragment<FragmentSetupPrivacyBinding>(FragmentSetupPrivacyBinding::inflate), BackAvailable, ProvidesTitle {

    private val viewModel by viewModel<SetupPrivacyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupInsets()
        setupButtons()
    }

    override fun getTitle() = ""

    private fun setupInsets() = with(binding) {
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        root.onApplyInsets { _, insets ->
            setupPrivacyButtonsContainer.updatePadding(
                bottom = insets.getInsets(SYSTEM_INSETS).bottom + padding
            )
        }
    }

    private fun setupButtons() = with(binding) {
        whenResumed {
            setupPrivacyAgree.onClicked {
                viewModel.onAgreeClicked()
            }
        }
        whenResumed {
            setupPrivacyDisagree.onClicked {
                viewModel.onDisagreeClicked()
            }
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> root.setDisplayedChildIfNeeded(1)
            is State.Loaded -> {
                root.setDisplayedChildIfNeeded(0)
                setupPrivacyContent.setupConsentText(state)
            }
            is State.Error -> showErrorDialog()
        }
    }

    private fun TextView.setupConsentText(state: State.Loaded) {
        text = Html.fromHtml(
            getString(
                R.string.privacy_notice_content,
                state.locationPolicyUrl,
                state.privacyNoticeUrl
            ),
            Html.FROM_HTML_MODE_COMPACT
        )
        movementMethod = BetterLinkMovementMethod.newInstance().apply {
            setOnLinkClickListener { _, url ->
                val intent = CustomTabsIntent.Builder().apply {
                    setShowTitle(false) //Policy pages don't have a title, show the URL
                    setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                    setBookmarksButtonEnabled(false)
                    setDownloadButtonEnabled(false)
                }.build().intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    data = Uri.parse(url)
                }
                startActivity(intent)
                true
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.privacy_notice_error_dialog_title)
            setMessage(R.string.privacy_notice_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.privacy_notice_error_dialog_close) { _, _ ->
                viewModel.onDisagreeClicked()
            }
        }.show()
    }

}