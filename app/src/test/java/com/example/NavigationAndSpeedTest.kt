package com.example

import com.example.data.model.RadarEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationAndSpeedTest {

    @Test
    fun `test radar speed limit extraction`() {
        val radar1 = RadarEntity(
            osmId = 12345L,
            latitude = 41.0,
            longitude = 29.0,
            maxSpeed = "82"
        )
        assertEquals(82, radar1.getParsedSpeedLimit())

        val radar2 = RadarEntity(
            osmId = 12346L,
            latitude = 41.1,
            longitude = 29.1,
            maxSpeed = "110 km/h"
        )
        assertEquals(110, radar2.getParsedSpeedLimit())

        val radar3 = RadarEntity(
            osmId = 12347L,
            latitude = 41.2,
            longitude = 29.2,
            maxSpeed = null
        )
        assertNull(radar3.getParsedSpeedLimit())
    }
}
