package com.kieronquinn.app.utag.utils.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun EditText.onChanged() = callbackFlow {
    val textWatcher = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            //No-op
        }

        override fun afterTextChanged(p0: Editable?) {
            //No-op
        }

        override fun onTextChanged(value: CharSequence?, p1: Int, p2: Int, p3: Int) {
            trySend(value)
        }
    }
    addTextChangedListener(textWatcher)
    awaitClose {
        removeTextChangedListener(textWatcher)
    }
}.debounce(TAP_DEBOUNCE)

fun EditText.setTextToEnd(content: String?) {
    setText(content)
    post {
        setSelection(text.length)
    }
}