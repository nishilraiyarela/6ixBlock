package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.GeoPoint

interface LocationRepository {
    suspend fun getBestLocation(): Resource<GeoPoint>
}
