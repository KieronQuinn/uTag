package com.kieronquinn.app.utag.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BoundActivity<V: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BaseActivity() {

    private var _binding: V? = null

    protected val binding: V
        get() = _binding ?: throw NullPointerException("Unable to access binding before onCreate or after onDestroy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflate(layoutInflater, window.decorView as ViewGroup, false)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}