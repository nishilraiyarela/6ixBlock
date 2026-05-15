package com.sixblock.app.core.util

import com.sixblock.app.domain.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object TorontoDefaults {
    val center = GeoPoint(latitude = 43.6532, longitude = -79.3832)
    const val cityKey = "toronto"
    const val neighbourhood = "Toronto"
}

object GeoUtils {
    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val a = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        return 2 * earthRadiusKm * atan2(sqrt(a), sqrt(1 - a))
    }

    fun approximateDistanceLabel(distanceKm: Double?): String {
        if (distanceKm == null) return ""
        return when {
            distanceKm < 0.2 -> "Around this block"
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m away"
            else -> "${String.format("%.1f", distanceKm)} km away"
        }
    }

    fun coarseArea(point: GeoPoint): String {
        neighbourhoodFor(point)?.let { return it }

        val lat = floor(point.latitude * 100) / 100
        val lon = floor(point.longitude * 100) / 100
        return "Near ${String.format("%.2f", lat)}, ${String.format("%.2f", lon)}"
    }

    fun publicAreaLabel(storedArea: String?, point: GeoPoint): String {
        val area = storedArea.orEmpty().trim()
        return when {
            area.isBlank() -> coarseArea(point)
            area == TorontoDefaults.neighbourhood -> coarseArea(point)
            area.startsWith("Toronto area") -> coarseArea(point)
            area.startsWith("Near ") -> coarseArea(point)
            else -> area
        }
    }

    private fun neighbourhoodFor(point: GeoPoint): String? {
        val lat = point.latitude
        val lon = point.longitude
        return when {
            lat in 43.58..43.79 && lon in -79.64..-79.49 -> "Etobicoke"
            lat in 43.62..43.76 && lon in -79.49..-79.41 -> "York"
            lat in 43.62..43.71 && lon in -79.43..-79.34 -> "Downtown Toronto"
            lat in 43.67..43.73 && lon in -79.37..-79.29 -> "East York"
            lat in 43.68..43.86 && lon in -79.51..-79.30 -> "North York"
            lat in 43.67..43.86 && lon in -79.30..-79.12 -> "Scarborough"
            lat in 43.58..43.72 && lon in -79.42..-79.28 -> "Toronto East End"
            lat in 43.58..43.70 && lon in -79.52..-79.41 -> "Toronto West End"
            lat in 43.55..43.90 && lon in -79.65..-79.12 -> "Toronto"
            else -> null
        }
    }
}

object GeoHash {
    private const val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(point: GeoPoint, precision: Int = 7): String {
        var latRange = -90.0 to 90.0
        var lonRange = -180.0 to 180.0
        var isEven = true
        var bit = 0
        var ch = 0
        val hash = StringBuilder()

        while (hash.length < precision) {
            if (isEven) {
                val mid = (lonRange.first + lonRange.second) / 2
                if (point.longitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonRange = mid to lonRange.second
                } else {
                    lonRange = lonRange.first to mid
                }
            } else {
                val mid = (latRange.first + latRange.second) / 2
                if (point.latitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latRange = mid to latRange.second
                } else {
                    latRange = latRange.first to mid
                }
            }

            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                hash.append(base32[ch])
                bit = 0
                ch = 0
            }
        }

        return hash.toString()
    }
}
