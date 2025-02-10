package com.kieronquinn.app.utag.utils

/**
 *  Wrapper for [ByteArray] which allows specifying lengths of data which is being added, for
 *  better compression
 */
class LengthByteArray(size: Int) {

    private val bytes = ByteArray(size).apply {
        //Header
        this[0] = 0
    }

    /**
     *  Sets the [Byte] [value] at a given [index]
     */
    fun set(index: Int, value: Byte) {
        bytes[index / 8] = value
    }

    /**
     *  Sets the [Byte] [value] at a given [index], using a specified [length]
     */
    fun set(index: Int, value: Byte, length: Int) {
        for (i in 0 until length.coerceAtMost(8)) {
            val position = index + i
            bytes[position / 8] = if ((1 shl i and value.toInt()) == 0) {
                (bytes[position / 8].toInt() and (1 shl (position % 8)).inv()).toByte()
            } else {
                (bytes[position / 8].toInt() or (1 shl (position % 8))).toByte()
            }
        }
    }

    /**
     *  Sets the [value] at a given [index] to [ByteArray] which has been converted from a [Short]
     */
    fun setShort(index: Int, value: ByteArray) {
        set(index, value, 2)
    }

    /**
     *  Sets the [ByteArray] [value] at a given [index], using a specified [length]
     */
    fun set(index: Int, value: ByteArray, length: Int) {
        System.arraycopy(value, 0, bytes, index / 8, length)
    }

    fun getBytes(): ByteArray {
        return bytes
    }

}
