package com.kieronquinn.app.utag.ui.screens.tag.locationhistory

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.ItemLocationEncryptionWarningBinding
import com.kieronquinn.app.utag.databinding.ItemLocationHistoryBinding
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.LocationHistoryPoint
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryAdapter.ListItem.Type
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryAdapter.ViewHolder
import com.kieronquinn.app.utag.utils.extensions.formatDateTime
import com.kieronquinn.app.utag.utils.extensions.getAttrColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TagLocationHistoryAdapter(
    context: Context,
    var items: List<ListItem>,
    var selectedDay: LocalDate?,
    var debugModeEnabled: Boolean,
    private val onItemClicked: (LocationHistoryPoint) -> Unit,
    private val onItemLongClicked: (LocationHistoryPoint) -> Unit
): RecyclerView.Adapter<ViewHolder>() {

    private val layoutInflater = context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val timeFormat = DateFormat.getTimeFormat(context)
    private var selectedIndex = -1

    private val textColourSecondary = context.getAttrColor(android.R.attr.textColorSecondary)
    private val dayFormat = DateTimeFormatter.ofPattern("EE")

    private val textColourSelected = ContextCompat
        .getColor(context, R.color.location_history_selected_text)


    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Type.of(viewType)) {
            Type.HEADER -> {
                ViewHolder.Header(
                    ItemLocationEncryptionWarningBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )
            }
            Type.ITEM -> {
                ViewHolder.Item(
                    ItemLocationHistoryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    fun setSelectedItem(item: LocationHistoryPoint?): Int? {
        val current = selectedIndex
        val new = items.indexOfFirst { it is ListItem.Item && it.point == item }
        if(current == new) return null
        selectedIndex = new
        if(current != -1 && current <= items.size) {
            notifyItemChanged(current)
        }
        notifyItemChanged(new)
        return new
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder) {
            is ViewHolder.Header -> {
                //No-op
            }
            is ViewHolder.Item -> holder.binding.bind((item as ListItem.Item).point, position)
        }
    }

    private fun ItemLocationHistoryBinding.bind(item: LocationHistoryPoint, position: Int) {
        val context = root.context
        val isSelected = position == selectedIndex
        itemLocationHistoryTime.text = when {
            item.startTime != null && item.endTime != null -> {
                val start = if(item.startTime.toLocalDate() == selectedDay) {
                    timeFormat.formatDateTime(item.startTime)
                }else{
                    dayFormat.format(item.startTime)
                }
                val end = if(item.endTime.toLocalDate() == selectedDay) {
                    timeFormat.formatDateTime(item.endTime)
                }else{
                    dayFormat.format(item.endTime)
                }
                context.getString(R.string.tag_location_history_timestamp_multiple, start, end)
            }
            item.time != null -> {
                timeFormat.formatDateTime(item.time)
            }
            else -> ""
        }
        itemLocationHistoryTime.setTextColor(
            if(isSelected) textColourSelected else textColourSecondary
        )
        itemLocationHistoryLineUpper.isVisible =
            items.getOrNull(position - 1) is ListItem.Item
        itemLocationHistoryLineLower.isVisible = position < items.size - 1
        itemLocationHistoryMarker.isVisible = isSelected
        itemLocationHistoryFocusBubble.isEnabled = isSelected
        itemLocationHistoryAddress.text = item.address
            ?: context.getString(R.string.map_address_unknown)
        itemLocationHistoryAddress.isEnabled = isSelected
        root.setOnClickListener {
            onItemClicked(item)
        }
        if(debugModeEnabled) {
            root.setOnLongClickListener {
                onItemLongClicked(item)
                true
            }
        }else{
            root.setOnLongClickListener(null)
        }
    }

    sealed class ListItem(val type: Type) {
        data object Header: ListItem(Type.HEADER)
        data class Item(val point: LocationHistoryPoint): ListItem(Type.ITEM)

        enum class Type {
            HEADER, ITEM;

            companion object {
                fun of(ordinal: Int): Type {
                    return entries.first { it.ordinal == ordinal }
                }
            }
        }
    }

    sealed class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        data class Header(
            val binding: ItemLocationEncryptionWarningBinding
        ): ViewHolder(binding.root)
        data class Item(val binding: ItemLocationHistoryBinding): ViewHolder(binding.root)
    }

}