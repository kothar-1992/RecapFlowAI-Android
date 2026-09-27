package com.recapflow.ai.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SideMenuPolicyTest {
    @Test
    fun httpsTelegramDestinationIsAccepted() {
        assertTrue(SideMenuPolicy.isHttpsDestination("https://t.me/kothar1992"))
    }

    @Test
    fun httpsFacebookDestinationIsAccepted() {
        assertTrue(SideMenuPolicy.isHttpsDestination("https://www.facebook.com/share/1DWHZwZKmH/"))
    }

    @Test
    fun schemeIsComparedCaseInsensitively() {
        assertTrue(SideMenuPolicy.isHttpsDestination("HTTPS://t.me/kothar1992"))
    }

    @Test
    fun surroundingWhitespaceIsTolerated() {
        assertTrue(SideMenuPolicy.isHttpsDestination("  https://t.me/kothar1992  "))
    }

    @Test
    fun plainHttpIsRejectedBeforeLeavingTheApp() {
        assertFalse(SideMenuPolicy.isHttpsDestination("http://t.me/kothar1992"))
    }

    @Test
    fun nonWebSchemesAreRejected() {
        assertFalse(SideMenuPolicy.isHttpsDestination("mailto:someone@example.com"))
        assertFalse(SideMenuPolicy.isHttpsDestination("javascript:alert(1)"))
    }

    @Test
    fun httpsWithoutHostIsRejected() {
        assertFalse(SideMenuPolicy.isHttpsDestination("https://"))
    }

    @Test
    fun blankAndMissingDestinationsAreRejected() {
        assertFalse(SideMenuPolicy.isHttpsDestination(""))
        assertFalse(SideMenuPolicy.isHttpsDestination("   "))
        assertFalse(SideMenuPolicy.isHttpsDestination(null))
    }

    @Test
    fun unparsableDestinationsAreRejectedInsteadOfThrowing() {
        assertFalse(SideMenuPolicy.isHttpsDestination("http s://not a uri"))
    }

    @Test
    fun contactableEmailRequiresANonBlankAddress() {
        assertTrue(SideMenuPolicy.isContactableEmail("kothihawailwin@gmail.com"))
        assertTrue(SideMenuPolicy.isContactableEmail("  kothihawailwin@gmail.com  "))
        assertFalse(SideMenuPolicy.isContactableEmail(""))
        assertFalse(SideMenuPolicy.isContactableEmail("   "))
        assertFalse(SideMenuPolicy.isContactableEmail(null))
    }

    @Test
    fun packageMetadataWinsOverBuildConfig() {
        val version = SideMenuPolicy.resolveVersion(
            packageVersionName = "1.0-phase6ux2",
            packageVersionCode = 7L,
            fallbackVersionName = "1.0-phase6f2.8.1",
            fallbackVersionCode = 1L,
        )

        assertEquals("1.0-phase6ux2", version.name)
        assertEquals("7", version.code)
    }

    @Test
    fun buildConfigIsUsedWhenPackageMetadataIsUnavailable() {
        val version = SideMenuPolicy.resolveVersion(
            packageVersionName = null,
            packageVersionCode = null,
            fallbackVersionName = "1.0-phase6ux2",
            fallbackVersionCode = 1L,
        )

        assertEquals("1.0-phase6ux2", version.name)
        assertEquals("1", version.code)
    }

    @Test
    fun blankPackageVersionNameFallsBackToBuildConfig() {
        val version = SideMenuPolicy.resolveVersion(
            packageVersionName = "   ",
            packageVersionCode = 4L,
            fallbackVersionName = "1.0-phase6ux2",
            fallbackVersionCode = 1L,
        )

        assertEquals("1.0-phase6ux2", version.name)
    }

    @Test
    fun versionCodeStaysAsciiInMyanmarLocale() {
        val version = SideMenuPolicy.resolveVersion(
            packageVersionName = "1.0-phase6ux2",
            packageVersionCode = 1234567L,
            fallbackVersionName = "1.0-phase6ux2",
            fallbackVersionCode = 1L,
        )

        assertTrue(
            version.code.all { it in '0'..'9' },
            "version code must stay ASCII digits, was ${version.code}",
        )
        assertFalse(
            version.code.any { it in '၀'..'၉' },
            "version code must never use Myanmar numerals",
        )
    }
}
