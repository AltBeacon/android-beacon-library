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
    fun `getLongScanForcingEnabledAttribute returns false by default`() {
        assertFalse(context.getLongScanForcingEnabledAttribute())
    }

    @Test
    fun `getJobPersistedEnabledAttribute returns true by default`() {
        assertTrue(context.getJobPersistedEnabledAttribute())
    }
}
