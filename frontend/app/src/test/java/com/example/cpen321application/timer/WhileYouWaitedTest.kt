package com.example.cpen321application.timer

import com.example.cpen321application.ui.timer.formatCountdown
import com.example.cpen321application.ui.timer.formatElapsed
import com.example.cpen321application.ui.timer.formatKm
import com.example.cpen321application.ui.timer.formatLatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhileYouWaitedTest {

    private val vancouver = IssPosition(latitude = 49.28, longitude = -123.12, altitudeKm = 420.0, velocityKmh = 27_600.0)
    private val calgary = IssPosition(latitude = 51.05, longitude = -114.07, altitudeKm = 420.0, velocityKmh = 27_600.0)

    @Test
    fun `haversine of one degree of longitude on the equator is about 111 km`() {
        val km = WhileYouWaited.haversineKm(0.0, 0.0, 0.0, 1.0)
        assertEquals(111.19, km, 0.05)
    }

    @Test
    fun `haversine is zero for the same point and symmetric`() {
        assertEquals(0.0, WhileYouWaited.haversineKm(10.0, 20.0, 10.0, 20.0), 1e-9)
        val ab = WhileYouWaited.haversineKm(49.28, -123.12, 51.05, -114.07)
        val ba = WhileYouWaited.haversineKm(51.05, -114.07, 49.28, -123.12)
        assertEquals(ab, ba, 1e-9)
    }

    @Test
    fun `altitude scales the arc length`() {
        val surface = WhileYouWaited.haversineKm(0.0, 0.0, 0.0, 10.0)
        val orbit = WhileYouWaited.haversineKm(0.0, 0.0, 0.0, 10.0, altitudeKm = 420.0)
        assertEquals(surface * (6_371.0 + 420.0) / 6_371.0, orbit, 1e-6)
    }

    @Test
    fun `facts for one minute with two ISS fixes`() {
        val facts = WhileYouWaited.compute(60, vancouver, calgary)

        assertEquals(60, facts.elapsedSeconds)
        // Vancouver -> Calgary is ~675 km on the surface; a bit more at 420 km up.
        assertEquals(720.0, facts.issDistanceKm!!, 30.0)
        assertEquals(29.78 * 60, facts.earthOrbitKm, 1e-6)
        assertEquals(299_792.458 * 60, facts.lightKm, 1e-3)
        assertEquals(299_792.458 * 60 / 768_800.0, facts.moonRoundTrips, 1e-6)
        assertEquals(70, facts.heartbeats)
        assertEquals(17, facts.blinks)
        assertEquals(264, facts.babiesBorn)
        assertFalse(facts.sunlightArrived)
    }

    @Test
    fun `sunlight arrives after 8 min 19 s`() {
        assertFalse(WhileYouWaited.compute(498, null, null).sunlightArrived)
        assertTrue(WhileYouWaited.compute(499, null, null).sunlightArrived)
    }

    @Test
    fun `single ISS fix falls back to speed times time`() {
        val onlyEnd = WhileYouWaited.compute(120, null, calgary)
        assertEquals(27_600.0 / 3600 * 120, onlyEnd.issDistanceKm!!, 1e-6)

        val onlyStart = WhileYouWaited.compute(120, vancouver, null)
        assertEquals(27_600.0 / 3600 * 120, onlyStart.issDistanceKm!!, 1e-6)
    }

    @Test
    fun `no ISS fix leaves the distance null but other facts intact`() {
        val facts = WhileYouWaited.compute(10, null, null)
        assertNull(facts.issDistanceKm)
        assertEquals(12, facts.heartbeats)
    }

    @Test
    fun `countdown formatting rounds up and zero-pads`() {
        assertEquals("00:10", formatCountdown(10_000))
        assertEquals("00:10", formatCountdown(9_001))
        assertEquals("00:01", formatCountdown(1))
        assertEquals("01:05", formatCountdown(65_000))
        assertEquals("12:00", formatCountdown(720_000))
    }

    @Test
    fun `display helpers`() {
        assertEquals("49.3°N 123.1°W", formatLatLon(vancouver))
        assertEquals("12.5°S 45.0°E", formatLatLon(IssPosition(-12.5, 45.0, 0.0, 0.0)))
        assertEquals("1.8 million km", formatKm(1_787_000.0))
        assertEquals("1,787 km", formatKm(1_787.4))
        assertEquals("42.0 km", formatKm(42.0))
        assertEquals("45 s", formatElapsed(45))
        assertEquals("2 min 5 s", formatElapsed(125))
    }
}
