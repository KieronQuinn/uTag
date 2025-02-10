package com.samsung.android.fmm.maze

/**
 *  Despite the names, the "Font" calls are used to get the Chaser keystore info
 */
object FmmFontJNI {

    init {
        System.loadLibrary("fmm_ct")
    }

    /**
     *  Gets keystore password
     */
    @JvmStatic
    external fun getFont1(): String

    /**
     *  Gets keystore
     */
    @JvmStatic
    external fun getFont2(context: Any): String

    /**
     *  Gets key alias
     */
    @JvmStatic
    external fun getFont3(): String

    /**
     *  Gets key password
     */
    @JvmStatic
    external fun getFont4(): String

}