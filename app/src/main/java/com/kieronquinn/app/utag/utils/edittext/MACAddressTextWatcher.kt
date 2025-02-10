package com.kieronquinn.app.utag.utils.edittext

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

//Based on https://tkit.dev/2014/01/16/android-edittext-automatic-mac-address-formatting/
class MACAddressTextWatcher(private val editText: EditText) : TextWatcher {

    private var mPreviousMac: String? = null

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
         * Formats the MAC address and handles the cursor position.
         * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
         */
    @SuppressLint("DefaultLocale")
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val enteredMac: String = editText.getText().toString().uppercase()
        val cleanMac = clearNonMacCharacters(enteredMac)
        var formattedMac = formatMacAddress(cleanMac)

        val selectionStart: Int = editText.selectionStart
        formattedMac = handleColonDeletion(enteredMac, formattedMac, selectionStart)
        val lengthDiff = formattedMac.length - enteredMac.length
        setMacEdit(cleanMac, formattedMac, selectionStart, lengthDiff)
    }

    /**
     * Strips all characters from a string except A-F and 0-9.
     * @param mac       User input string.
     * @return          String containing MAC-allowed characters.
     */
    private fun clearNonMacCharacters(mac: String): String {
        return mac.replace("[^A-Fa-f0-9]".toRegex(), "")
    }

    /**
     * Adds a colon character to an unformatted MAC address after
     * every second character (strips full MAC trailing colon)
     * @param cleanMac      Unformatted MAC address.
     * @return              Properly formatted MAC address.
     */
    private fun formatMacAddress(cleanMac: String): String {
        var grouppedCharacters = 0
        var formattedMac = ""

        for (i in 0 until cleanMac.length) {
            formattedMac += cleanMac[i]
            ++grouppedCharacters

            if (grouppedCharacters == 2) {
                formattedMac += ":"
                grouppedCharacters = 0
            }
        }

        // Removes trailing colon for complete MAC address
        if (cleanMac.length == 12) formattedMac = formattedMac.substring(0, formattedMac.length - 1)

        return formattedMac
    }

    /**
     * Upon users colon deletion, deletes MAC character preceding deleted colon as well.
     * @param enteredMac            User input MAC.
     * @param formattedMac          Formatted MAC address.
     * @param selectionStart        MAC EditText field cursor position.
     * @return                      Formatted MAC address.
     */
    private fun handleColonDeletion(
        enteredMac: String,
        formattedMac: String,
        selectionStart: Int
    ): String {
        var formattedMac = formattedMac
        if (mPreviousMac != null && mPreviousMac!!.length > 1) {
            val previousColonCount = colonCount(mPreviousMac!!)
            val currentColonCount = colonCount(enteredMac)

            if (currentColonCount < previousColonCount) {
                formattedMac =
                    formattedMac.substring(0, selectionStart - 1) + formattedMac.substring(
                        selectionStart
                    )
                val cleanMac = clearNonMacCharacters(formattedMac)
                formattedMac = formatMacAddress(cleanMac)
            }
        }
        return formattedMac
    }

    /**
     * Gets MAC address current colon count.
     * @param formattedMac      Formatted MAC address.
     * @return                  Current number of colons in MAC address.
     */
    private fun colonCount(formattedMac: String): Int {
        return formattedMac.replace("[^:]".toRegex(), "").length
    }

    /**
     * Removes TextChange listener, sets MAC EditText field value,
     * sets new cursor position and re-initiates the listener.
     * @param cleanMac          Clean MAC address.
     * @param formattedMac      Formatted MAC address.
     * @param selectionStart    MAC EditText field cursor position.
     * @param lengthDiff        Formatted/Entered MAC number of characters difference.
     */
    private fun setMacEdit(
        cleanMac: String,
        formattedMac: String,
        selectionStart: Int,
        lengthDiff: Int
    ) {
        editText.removeTextChangedListener(this)
        if (cleanMac.length <= 12) {
            editText.setText(formattedMac)
            editText.setSelection((selectionStart + lengthDiff).coerceAtLeast(0))
            mPreviousMac = formattedMac
        } else {
            editText.setText(mPreviousMac)
            editText.setSelection(mPreviousMac!!.length)
        }
        editText.addTextChangedListener(this)
    }
}