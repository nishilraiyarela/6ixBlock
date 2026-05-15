package com.sixblock.app.domain.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val birthday: String = "",
    val neighbourhood: String = "",
    val gender: String = "",
    val homeCityKey: String = "toronto",
    val radiusKm: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)
