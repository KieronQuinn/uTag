package com.kieronquinn.app.utag.utils.preferences

import android.view.View
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.SeslSwitchPreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.UTagActionCardPreference
import androidx.preference.UTagBottomPaddingPreferenceCategory
import androidx.preference.UTagCheckBoxPreference
import androidx.preference.UTagDropDownPreference
import androidx.preference.UTagEditTextPreference
import androidx.preference.UTagGlideCheckBoxPreference
import androidx.preference.UTagGlidePreference
import androidx.preference.UTagLayoutPreference
import androidx.preference.UTagListPreference
import androidx.preference.UTagPreference
import androidx.preference.UTagPreferenceCategory
import androidx.preference.UTagRadioButtonPreference
import androidx.preference.UTagSeekBarPreference
import androidx.preference.UTagSpacerPreferenceCategory
import androidx.preference.UTagSwitchPreference
import androidx.preference.UTagSwitchPreferenceScreen
import androidx.preference.UTagTipsCardPreference
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.extensions.getAttrColor

fun PreferenceGroup.preferenceCategory(
    key: String,
    block: PreferenceCategory.() -> Unit
) {
    //PreferenceCategory is special and needs to be added BEFORE having content added to it
    val category = UTagPreferenceCategory(context).apply {
        this.key = key
    }
    addPreferenceCompat(category)
    block(category)
}

fun PreferenceGroup.preference(block: Preference.() -> Unit) {
    addPreferenceCompat(UTagPreference(context).apply(block))
}

fun PreferenceGroup.glidePreference(block: UTagGlidePreference.() -> Unit) {
    addPreferenceCompat(UTagGlidePreference(context).apply {
        isIconSpaceReserved = true
    }.apply(block))
}

fun PreferenceGroup.glideCheckBoxPreference(block: UTagGlideCheckBoxPreference.() -> Unit) {
    addPreferenceCompat(UTagGlideCheckBoxPreference(context).apply {
        isIconSpaceReserved = true
    }.apply(block))
}

fun PreferenceGroup.switchBarPreference(block: UTagSwitchPreference.() -> Unit) {
    addPreferenceCompat(UTagSwitchPreference(context).apply(block))
}

fun PreferenceGroup.switchPreference(block: UTagSwitchPreference.() -> Unit) {
    addPreferenceCompat(UTagSwitchPreference(context).apply(block))
}

fun PreferenceGroup.switchPreferenceScreen(block: SeslSwitchPreferenceScreen.() -> Unit) {
    addPreferenceCompat(UTagSwitchPreferenceScreen(context).apply(block))
}

fun PreferenceGroup.checkboxPreference(block: CheckBoxPreference.() -> Unit) {
    addPreferenceCompat(UTagCheckBoxPreference(context).apply(block))
}

fun PreferenceGroup.radioButtonPreference(block: UTagRadioButtonPreference.() -> Unit) {
    addPreferenceCompat(UTagRadioButtonPreference(context).apply(block))
}

fun PreferenceGroup.seekbarPreference(block: UTagSeekBarPreference.() -> Unit) {
    addPreferenceCompat(UTagSeekBarPreference(context).apply(block))
}

fun PreferenceGroup.editTextPreference(block: EditTextPreference.() -> Unit) {
    addPreferenceCompat(UTagEditTextPreference(context).apply(block))
}

fun PreferenceGroup.dropDownPreference(block: DropDownPreference.() -> Unit) {
    addPreferenceCompat(UTagDropDownPreference(context).apply(block))
}

fun PreferenceGroup.listPreference(block: ListPreference.() -> Unit) {
    addPreferenceCompat(UTagListPreference(context).apply(block))
}

fun PreferenceGroup.tipsCardPreference(block: UTagTipsCardPreference.() -> Unit) {
    addPreferenceCompat(UTagTipsCardPreference(context).apply(block))
}

fun PreferenceGroup.actionCardPreference(block: UTagActionCardPreference.() -> Unit) {
    addPreferenceCompat(UTagActionCardPreference(context).apply(block))
}

fun PreferenceGroup.layoutPreference(layout: View, key: String, block: UTagLayoutPreference.() -> Unit = {}) {
    addPreferenceCompat(UTagLayoutPreference(context, layout, key).apply(block))
}

fun PreferenceGroup.bottomPaddingPreferenceCategory(
    rootView: View,
    additionalPadding: Int = 0,
    block: UTagBottomPaddingPreferenceCategory.() -> Unit = {}
) {
    addPreferenceCompat(
        UTagBottomPaddingPreferenceCategory(context, rootView, additionalPadding)
        .apply(block))
}

fun PreferenceGroup.spacerPreferenceCategory(
    height: Int,
    key: String,
    block: UTagSpacerPreferenceCategory.() -> Unit = {}
) {
    addPreferenceCompat(UTagSpacerPreferenceCategory(context, height, key).apply(block))
}

fun <T> Preference.onChange(block: (T) -> Unit) {
    setOnPreferenceChangeListener { _, newValue ->
        block(newValue as T)
        false
    }
}

fun Preference.onClick(block: () -> Unit) {
    setOnPreferenceClickListener { _ ->
        block()
        true
    }
}

fun Preference.setSummaryAccented(enabled: Boolean) {
    seslSetSummaryColor(if(enabled) {
        ContextCompat.getColor(context, R.color.oui_accent_color)
    }else{
        context.getAttrColor(android.R.attr.textColorSecondary)
    })
}

fun EditTextPreference.setInputType(type: Int) {
    setOnBindEditTextListener {
        it.inputType = type
    }
}

private fun PreferenceGroup.addPreferenceCompat(preference: Preference) {
    //If a key is not set, apply the title as the key as it's usually static
    if(preference.key == null) {
        preference.key = when(preference) {
            is UTagBottomPaddingPreferenceCategory,
            is UTagLayoutPreference,
            is UTagSpacerPreferenceCategory -> {
                null //Handled with custom ID
            }
            else -> preference.title?.toString()
                ?: throw RuntimeException("Preference (${preference::class.java.name}) does not have a title!")
        }
    }
    //preference.order = preferenceCount
    addPreference(preference)
}