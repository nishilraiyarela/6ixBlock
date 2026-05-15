package com.sixblock.app.core.model

sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T, val fromCache: Boolean = false) : Resource<T>()
    data class Empty(val message: String) : Resource<Nothing>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}
