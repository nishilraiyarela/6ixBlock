package com.sixblock.app.core.util

import com.sixblock.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {
    @Test
    fun distanceKm_betweenTorontoLandmarks_isReasonable() {
        val cityHall = GeoPoint(43.6535, -79.3839)
        val highPark = GeoPoint(43.6465, -79.4637)

        val distance = GeoUtils.distanceKm(cityHall, highPark)

        assertTrue(distance > 5.0)
        assertTrue(distance < 8.0)
    }

    @Test
    fun geohash_isStableForSamePoint() {
        val point = GeoPoint(43.6532, -79.3832)

        assertEquals(GeoHash.encode(point), GeoHash.encode(point))
    }

    @Test
    fun coarseArea_identifiesScarboroughCoordinates() {
        val scarborough = GeoPoint(43.7764, -79.2318)

        assertEquals("Scarborough", GeoUtils.coarseArea(scarborough))
    }

    @Test
    fun publicAreaLabel_replacesOldGenericTorontoLabel() {
        val scarborough = GeoPoint(43.7764, -79.2318)

        assertEquals("Scarborough", GeoUtils.publicAreaLabel("Toronto area 43.77, -79.23", scarborough))
    }
}
