package com.kieronquinn.app.utag.ui.screens.tag.locationhistory.export

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagLocationHistoryExportBinding
import com.kieronquinn.app.utag.ui.base.BaseDialogFragment
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.export.TagLocationExportDialogViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagLocationExportDialogFragment: BaseDialogFragment<FragmentTagLocationHistoryExportBinding>(FragmentTagLocationHistoryExportBinding::inflate) {

    private val args by navArgs<TagLocationExportDialogFragmentArgs>()

    private val viewModel by viewModel<TagLocationExportDialogViewModel> {
        parametersOf(args.uri, args.locations.toList())
    }

    override val isDismissable = false

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
            is State.Exporting -> {
                //No-op
            }
            is State.Finished -> {
                val message = if(state.success) {
                    R.string.tag_location_history_export_success_toast
                }else{
                    R.string.tag_location_history_export_failed_toast
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
    }

}