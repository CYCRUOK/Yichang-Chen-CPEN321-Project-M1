package com.example.cpen321application.info

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

/** Client-side counterparts of the server APIs: local IP and local time. */
object ClientInfo {

    /** `hh:mm:ss GMT+hh:mm`, 24-hour, same format the backend uses. */
    fun formatLocalTime(now: ZonedDateTime): String {
        val offset: ZoneOffset = now.offset
        val total = offset.totalSeconds
        val sign = if (total < 0) "-" else "+"
        val abs = kotlin.math.abs(total)
        return String.format(
            Locale.US,
            "%02d:%02d:%02d GMT%s%02d:%02d",
            now.hour, now.minute, now.second, sign, abs / 3600, (abs % 3600) / 60,
        )
    }

    /**
     * Picks the address to display: prefers a non-loopback IPv4 (Wi-Fi / mobile
     * private address), then a global IPv6, otherwise "unavailable".
     */
    fun pickDisplayAddress(addresses: List<InetAddress>): String {
        val usable = addresses.filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isAnyLocalAddress }
        val v4 = usable.firstOrNull { it is Inet4Address }
        val v6 = usable.firstOrNull { it is Inet6Address }
        val chosen = v4 ?: v6 ?: return "unavailable"
        // Strip the "%wlan0" scope id that Java appends to IPv6 addresses.
        return chosen.hostAddress?.substringBefore('%') ?: "unavailable"
    }

    /** All addresses of all "up" interfaces on this device. */
    fun localAddresses(): List<InetAddress> = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
    } catch (e: Exception) {
        emptyList()
    }

    fun localIp(): String = pickDisplayAddress(localAddresses())
}
