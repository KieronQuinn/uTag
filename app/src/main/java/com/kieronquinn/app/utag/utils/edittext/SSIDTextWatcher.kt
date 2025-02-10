package com.kieronquinn.app.utag.utils.edittext

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

//Based on https://tkit.dev/2014/01/16/android-edittext-automatic-mac-address-formatting/
class SSIDTextWatcher(private val editText: EditText) : TextWatcher {

    private var mPreviousSsid: String? = null

    /* (non-Javadoc)
         * Does nothing.
         * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
         */
    override fun afterTextChanged(arg0: Editable?) {
    }

    /* (non-Javadoc)
         * Does nothing.
         * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
         */
    override fun beforeTextChanged(arg0: CharSequence?, arg1: Int, arg2: Int, arg3: Int) {
    }

    /* (non-Javadoc)
         * Formats the SSID and handles the cursor position.
         * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
         */
    @SuppressLint("DefaultLocale")
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val enteredSsid: String = editText.getText().toString()
        val cleanSsid = clearNonSsidCharacters(enteredSsid)
        val selectionStart: Int = editText.selectionStart
        val lengthDiff = cleanSsid.length - enteredSsid.length
        setSsidEdit(cleanSsid, selectionStart, lengthDiff)
    }

    /**
     * Strips all characters from a string that are not allowed
     * @param ssid       User input string.
     * @return          String containing SSID-allowed characters.
     */
    private fun clearNonSsidCharacters(ssid: String): String {
        return ssid.replace("[+/\"\t]".toRegex(), "")
    }

    /**
     * Removes TextChange listener, sets SSID EditText field value,
     * sets new cursor position and re-initiates the listener.
     * @param cleanSsid          Clean SSID.
     * @param selectionStart    SSID EditText field cursor position.
     * @param lengthDiff        Formatted/Entered SSID number of characters difference.
     */
    private fun setSsidEdit(
        cleanSsid: String,
        selectionStart: Int,
        lengthDiff: Int
    ) {
        editText.removeTextChangedListener(this)
        if (cleanSsid.length <= 32) {
            editText.setText(cleanSsid)
            editText.setSelection((selectionStart + lengthDiff).coerceAtLeast(0))
            mPreviousSsid = cleanSsid
        } else {
            editText.setText(mPreviousSsid)
            editText.setSelection(mPreviousSsid!!.length)
        }
        editText.addTextChangedListener(this)
    }
}