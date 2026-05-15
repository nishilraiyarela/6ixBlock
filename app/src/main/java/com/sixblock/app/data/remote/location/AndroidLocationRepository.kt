package com.sixblock.app.data.remote.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.repository.LocationRepository
import kotlinx.coroutines.tasks.await

class AndroidLocationRepository(context: Context) : LocationRepository {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    override suspend fun getBestLocation(): Resource<GeoPoint> {
        if (!hasLocationPermission()) return Resource.Success(TorontoDefaults.center)

        return runCatching {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(8_000L)
                .setMaxUpdateAgeMillis(0L)
                .build()
            val token = CancellationTokenSource()
            val fresh = client.getCurrentLocation(request, token.token).await()
            val best = fresh ?: bestSystemLastKnownLocation() ?: client.lastLocation.await()
            Resource.Success(
                best?.let { GeoPoint(it.latitude, it.longitude) } ?: TorontoDefaults.center
            )
        }.getOrElse {
            Resource.Success(TorontoDefaults.center)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun bestSystemLastKnownLocation(): Location? {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
