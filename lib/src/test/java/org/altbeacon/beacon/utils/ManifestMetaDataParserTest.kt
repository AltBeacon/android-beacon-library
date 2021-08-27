package org.altbeacon.beacon.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class ManifestMetaDataParserTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `longScanForcingEnabled exists and return true`() {
        assertTrue(context.getManifestMetadataValueAsBoolean("longScanForcingEnabled"))
        assertTrue(context.getManifestMetadataValueAsBoolean("longScanForcingEnabled"))
    }

    @Test
    fun `jobPersistedEnabled exists and return false`() {
        assertFalse(context.getManifestMetadataValueAsBoolean("jobPersistedEnabled"))
    }

    @Test
    fun `doesNotExistFlag does not exist and return false`() {
        assertFalse(context.getManifestMetadataValueAsBoolean("doesNotExistFlag"))
    }
}
