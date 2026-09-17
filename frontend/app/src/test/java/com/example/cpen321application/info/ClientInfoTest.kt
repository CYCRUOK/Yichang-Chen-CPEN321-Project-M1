package com.example.cpen321application.info

import java.net.InetAddress
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientInfoTest {

    private val requiredFormat = Regex("""^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$""")

    @Test
    fun `formats local time as hh_mm_ss GMT+hh_mm`() {
        val t = ZonedDateTime.of(2026, 9, 17, 9, 5, 3, 0, ZoneOffset.ofHours(8))
        assertEquals("09:05:03 GMT+08:00", ClientInfo.formatLocalTime(t))
    }

    @Test
    fun `negative and half-hour offsets`() {
        assertEquals("23:59:59 GMT-07:00", ClientInfo.formatLocalTime(ZonedDateTime.of(2026, 1, 1, 23, 59, 59, 0, ZoneOffset.ofHours(-7))))
        assertEquals("00:00:00 GMT+05:30", ClientInfo.formatLocalTime(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30))))
        assertEquals("12:00:00 GMT+00:00", ClientInfo.formatLocalTime(ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)))
    }

    @Test
    fun `matches the same format the backend uses`() {
        assertTrue(requiredFormat.matches(ClientInfo.formatLocalTime(ZonedDateTime.now())))
    }

    @Test
    fun `prefers a private IPv4 over loopback and IPv6`() {
        val picked = ClientInfo.pickDisplayAddress(
            listOf(
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("fe80::1"),
                InetAddress.getByName("2001:db8::10"),
                InetAddress.getByName("192.168.31.97"),
            ),
        )
        assertEquals("192.168.31.97", picked)
    }

    @Test
    fun `falls back to a global IPv6, then to unavailable`() {
        assertEquals(
            "2001:db8:0:0:0:0:0:10",
            ClientInfo.pickDisplayAddress(listOf(InetAddress.getByName("::1"), InetAddress.getByName("2001:db8::10"))),
        )
        assertEquals("unavailable", ClientInfo.pickDisplayAddress(listOf(InetAddress.getByName("127.0.0.1"))))
        assertEquals("unavailable", ClientInfo.pickDisplayAddress(emptyList()))
    }
}
