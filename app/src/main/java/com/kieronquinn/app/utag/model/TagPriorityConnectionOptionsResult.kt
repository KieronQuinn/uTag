package com.kieronquinn.app.utag.model

sealed class TagPriorityConnectionOptionsResult {
    data object Success : TagPriorityConnectionOptionsResult()
    data class Failed(val error: String, val code: Int) : TagPriorityConnectionOptionsResult()
}