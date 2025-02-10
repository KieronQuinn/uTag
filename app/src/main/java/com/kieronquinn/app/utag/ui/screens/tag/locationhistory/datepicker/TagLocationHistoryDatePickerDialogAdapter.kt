package com.kieronquinn.app.utag.ui.screens.tag.locationhistory.datepicker

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.ItemLocationHistoryDateBinding
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.datepicker.TagLocationHistoryDatePickerDialogAdapter.ViewHolder
import com.kieronquinn.app.utag.utils.extensions.getAttrColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TagLocationHistoryDatePickerDialogAdapter(
    context: Context,
    private val items: List<LocalDateTime>,
    private val selected: LocalDateTime,
    private val onItemClicked: (LocalDateTime) -> Unit
): RecyclerView.Adapter<ViewHolder>() {

    private val layoutInflater = context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val dayFormat = DateTimeFormatter.ofPattern("EE")

    private val dateFormat by lazy {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "MM/d")
        )
    }

    private val textColourPrimary = context.getAttrColor(android.R.attr.textColorPrimary)
    private val textColourSecondary = context.getAttrColor(android.R.attr.textColorSecondary)
    private val textColourSelected = ContextCompat.getColor(context, R.color.oui_accent_color)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLocationHistoryDateBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        val isSelected = item.toLocalDate() == selected.toLocalDate()
        itemLocationHistoryDateDate.text = dateFormat.format(item)
        itemLocationHistoryDateDate.setTextColor(
            if(isSelected) textColourSelected else textColourSecondary
        )
        itemLocationHistoryDateDay.text = dayFormat.format(item)
        itemLocationHistoryDateDay.setTextColor(
            if(isSelected) textColourSelected else textColourPrimary
        )
        root.setOnClickListener {
            onItemClicked(item)
        }
    }

    data class ViewHolder(val binding: ItemLocationHistoryDateBinding):
        RecyclerView.ViewHolder(binding.root)

}