package com.example.cpen321application.ui.timer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Very coarse continent polygons (longitude, latitude) for the ISS mini map.
 * Hand-simplified; only meant to make the map recognisable at a few hundred px.
 */
object WorldOutline {
    private val continents: List<List<Pair<Double, Double>>> = listOf(
        // North America
        listOf(
            -168.0 to 66.0, -140.0 to 70.0, -95.0 to 72.0, -75.0 to 62.0, -55.0 to 50.0, -66.0 to 44.0,
            -75.0 to 35.0, -81.0 to 25.0, -90.0 to 29.0, -97.0 to 26.0, -105.0 to 20.0, -87.0 to 15.0,
            -78.0 to 8.0, -83.0 to 9.0, -93.0 to 16.0, -105.0 to 23.0, -115.0 to 31.0, -124.0 to 40.0,
            -125.0 to 49.0, -135.0 to 58.0, -152.0 to 60.0, -165.0 to 62.0,
        ),
        // Greenland
        listOf(-55.0 to 60.0, -40.0 to 65.0, -20.0 to 72.0, -25.0 to 80.0, -60.0 to 82.0, -72.0 to 78.0, -70.0 to 70.0),
        // South America
        listOf(
            -78.0 to 8.0, -60.0 to 10.0, -50.0 to 0.0, -35.0 to -5.0, -38.0 to -15.0, -48.0 to -25.0,
            -55.0 to -35.0, -63.0 to -40.0, -70.0 to -52.0, -68.0 to -55.0, -75.0 to -48.0, -72.0 to -30.0,
            -77.0 to -15.0, -81.0 to -5.0, -79.0 to 2.0,
        ),
        // Africa
        listOf(
            -17.0 to 15.0, -10.0 to 32.0, 10.0 to 37.0, 32.0 to 31.0, 43.0 to 12.0, 51.0 to 11.0, 40.0 to -5.0,
            35.0 to -20.0, 30.0 to -34.0, 18.0 to -34.0, 12.0 to -18.0, 9.0 to -2.0, 9.0 to 4.0, -5.0 to 5.0,
            -14.0 to 10.0,
        ),
        // Eurasia
        listOf(
            -10.0 to 44.0, -8.0 to 36.0, 0.0 to 37.0, 12.0 to 38.0, 20.0 to 40.0, 28.0 to 36.0, 36.0 to 36.0,
            50.0 to 26.0, 58.0 to 23.0, 66.0 to 25.0, 73.0 to 20.0, 78.0 to 8.0, 80.0 to 15.0, 88.0 to 22.0,
            98.0 to 10.0, 104.0 to 1.0, 110.0 to 10.0, 109.0 to 20.0, 122.0 to 30.0, 122.0 to 40.0, 130.0 to 42.0,
            141.0 to 50.0, 160.0 to 60.0, 180.0 to 66.0, 180.0 to 72.0, 150.0 to 76.0, 110.0 to 77.0, 70.0 to 73.0,
            50.0 to 68.0, 30.0 to 70.0, 20.0 to 70.0, 5.0 to 62.0, 5.0 to 58.0, -5.0 to 50.0,
        ),
        // Australia
        listOf(
            114.0 to -22.0, 122.0 to -17.0, 136.0 to -12.0, 142.0 to -11.0, 147.0 to -19.0, 153.0 to -27.0,
            150.0 to -37.0, 140.0 to -38.0, 131.0 to -32.0, 118.0 to -35.0, 114.0 to -30.0,
        ),
        // Antarctica (as a band)
        listOf(-180.0 to -70.0, -60.0 to -63.0, 0.0 to -70.0, 60.0 to -67.0, 120.0 to -66.0, 180.0 to -70.0, 180.0 to -90.0, -180.0 to -90.0),
    )

    /** Fills the continents on an equirectangular canvas the size of this DrawScope. */
    fun DrawScope.drawContinents(color: Color) {
        for (polygon in continents) {
            val path = Path()
            polygon.forEachIndexed { i, (lon, lat) ->
                val p = Offset(((lon + 180.0) / 360.0 * size.width).toFloat(), ((90.0 - lat) / 180.0 * size.height).toFloat())
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color)
        }
    }
}
