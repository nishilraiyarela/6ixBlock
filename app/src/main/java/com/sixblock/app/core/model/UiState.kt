package com.sixblock.app.core.model

data class UiState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null,
    val emptyMessage: String? = null,
    val fromCache: Boolean = false
)
