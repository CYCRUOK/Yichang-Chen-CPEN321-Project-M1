package com.example.cpen321application.timer

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** What happened in the world while the timer was running. All values are estimates. */
data class WaitFacts(
    val elapsedSeconds: Long,
    val issStart: IssPosition?,
    val issEnd: IssPosition?,
    /** Great-circle distance the ISS covered, or null when no position was available. */
    val issDistanceKm: Double?,
    val earthOrbitKm: Double,
    val lightKm: Double,
    val moonRoundTrips: Double,
    val heartbeats: Long,
    val blinks: Long,
    val babiesBorn: Long,
    /** True once the timer outlasted the ~8 min 20 s sunlight takes to reach Earth. */
    val sunlightArrived: Boolean,
)

object WhileYouWaited {
    const val EARTH_ORBIT_KM_PER_S = 29.78
    const val LIGHT_KM_PER_S = 299_792.458
    const val MOON_ROUND_TRIP_KM = 2 * 384_400.0
    const val HEARTBEATS_PER_MIN = 70.0
    const val BLINKS_PER_MIN = 17.0
    const val BIRTHS_PER_S = 4.4
    const val SUNLIGHT_TRAVEL_S = 499L
    const val EARTH_RADIUS_KM = 6_371.0

    fun compute(elapsedSeconds: Long, issStart: IssPosition?, issEnd: IssPosition?): WaitFacts {
        val issDistance = when {
            issStart != null && issEnd != null ->
                haversineKm(issStart.latitude, issStart.longitude, issEnd.latitude, issEnd.longitude, issEnd.altitudeKm)
            // Only one fix: fall back to speed x time.
            issStart != null -> issStart.velocityKmh / 3600.0 * elapsedSeconds
            issEnd != null -> issEnd.velocityKmh / 3600.0 * elapsedSeconds
            else -> null
        }
        val lightKm = LIGHT_KM_PER_S * elapsedSeconds
        return WaitFacts(
            elapsedSeconds = elapsedSeconds,
            issStart = issStart,
            issEnd = issEnd,
            issDistanceKm = issDistance,
            earthOrbitKm = EARTH_ORBIT_KM_PER_S * elapsedSeconds,
            lightKm = lightKm,
            moonRoundTrips = lightKm / MOON_ROUND_TRIP_KM,
            heartbeats = (HEARTBEATS_PER_MIN / 60.0 * elapsedSeconds).roundToLong(),
            blinks = (BLINKS_PER_MIN / 60.0 * elapsedSeconds).roundToLong(),
            babiesBorn = (BIRTHS_PER_S * elapsedSeconds).roundToLong(),
            sunlightArrived = elapsedSeconds >= SUNLIGHT_TRAVEL_S,
        )
    }

    /** Great-circle distance between two points at [altitudeKm] above the Earth's surface. */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double, altitudeKm: Double = 0.0): Double {
        val radius = EARTH_RADIUS_KM + altitudeKm
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * radius * asin(sqrt(a))
    }
}
