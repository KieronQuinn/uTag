package com.kieronquinn.app.utag.ui.screens.tag.pinentry

import androidx.lifecycle.ViewModel

abstract class TagPinEntryDialogViewModel: ViewModel() {

    abstract var pin: String
    abstract var saveChecked: Boolean

}

class TagPinEntryDialogViewModelImpl: TagPinEntryDialogViewModel() {

    override var pin = ""
    override var saveChecked = false

}