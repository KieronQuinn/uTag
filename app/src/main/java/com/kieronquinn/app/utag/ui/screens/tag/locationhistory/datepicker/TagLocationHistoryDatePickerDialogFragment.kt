package com.kieronquinn.app.utag.ui.screens.tag.locationhistory.datepicker

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.utag.databinding.FragmentTagLocationHistoryDatePickerBinding
import com.kieronquinn.app.utag.ui.base.BaseDialogFragment
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import me.moallemi.tools.daterange.localdate.rangeTo
import java.time.LocalDate
import java.time.LocalDateTime

class TagLocationHistoryDatePickerDialogFragment: BaseDialogFragment<FragmentTagLocationHistoryDatePickerBinding>(FragmentTagLocationHistoryDatePickerBinding::inflate) {

    companion object {
        private const val KEY_DATE_PICKER = "date_picker"
        private const val KEY_RESULT = "result"

        fun Fragment.setupDatePickerResultListener(callback: (result: LocalDateTime) -> Unit) {
            setFragmentResultListener(KEY_DATE_PICKER) { requestKey, bundle ->
                if(requestKey != KEY_DATE_PICKER) return@setFragmentResultListener
                val result = bundle.getSerializableCompat(KEY_RESULT, LocalDateTime::class.java)
                callback.invoke(result ?: return@setFragmentResultListener)
            }
        }

        private fun getAvailableDays(): List<LocalDateTime> {
            return ArrayList<LocalDateTime>().apply {
                for(day in LocalDate.now().minusDays(6) .. LocalDate.now()) {
                    add(day.atStartOfDay())
                }
            }
        }
    }

    private val args by navArgs<TagLocationHistoryDatePickerDialogFragmentArgs>()
    private val availableDays = getAvailableDays()

    private val adapter by lazy {
        TagLocationHistoryDatePickerDialogAdapter(
            requireContext(),
            availableDays,
            args.date,
            ::onItemClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            itemLocationHistoryDatePickerTitle.text = getTitle()
            itemLocationHistoryDatePickerRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            itemLocationHistoryDatePickerRecyclerView.adapter = adapter
        }
    }

    private fun getTitle(): String {
        return availableDays.map {
            it.year
        }.distinct().joinToString(" / ")
    }

    private fun onItemClicked(date: LocalDateTime) {
        setFragmentResult(KEY_DATE_PICKER, bundleOf(KEY_RESULT to date))
        dismiss()
    }

}