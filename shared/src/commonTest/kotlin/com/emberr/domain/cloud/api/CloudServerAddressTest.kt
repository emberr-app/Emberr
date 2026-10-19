package com.emberr.domain.cloud.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloudServerAddressTest {

    @Test
    fun trimsSpacesAndTrailingSlashes() {
        assertEquals("http://192.168.1.5:8080", normalizedCloudServerAddress("  http://192.168.1.5:8080// "))
    }

    @Test
    fun refusesAddressesWithoutWebSchemeOrHost() {
        assertNull(normalizedCloudServerAddress("localhost:8080"))
        assertNull(normalizedCloudServerAddress("ftp://localhost:8080"))
        assertNull(normalizedCloudServerAddress("http://"))
    }
}
