package org.altbeacon.beacon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Settings
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Foreground service scanning without a notification of its own: the scanning service shares the notification the
 * app shows with the same id, or shows a plain one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BeaconServiceForegroundNotificationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var beaconManager: BeaconManager
    private val services = mutableListOf<ServiceController<BeaconService>>()

    @Before
    fun before() {
        ShadowLog.stream = System.err
        LogManager.setLogger(Loggers.verboseLogger())
        LogManager.setVerboseLoggingEnabled(true)
        BeaconManager.setsManifestCheckingDisabled(true)
        beaconManager = BeaconManager.getInstanceForApplication(context)
    }

    @After
    fun after() {
        services.forEach { it.destroy() }
        beaconManager.disableForegroundServiceScanning()
    }

    @Test
    fun enablesForegroundServiceScanningWithoutANotification() {
        beaconManager.enableForegroundServiceScanning(null, NOTIFICATION_ID)
        assertTrue(beaconManager.isForegroundServiceScanningEnabled)
        assertNull(beaconManager.foregroundServiceNotification)
        assertEquals(NOTIFICATION_ID, beaconManager.foregroundServiceNotificationId)
    }

    @Test
    fun disablesForegroundServiceScanningWithoutANotification() {
        beaconManager.enableForegroundServiceScanning(null, NOTIFICATION_ID)
        beaconManager.disableForegroundServiceScanning()
        assertFalse(beaconManager.isForegroundServiceScanningEnabled)
    }

    @Test
    fun foregroundServiceScanStrategyNeedsNoNotification() {
        Settings.ForegroundServiceScanStrategy(NOTIFICATION_ID).configure(beaconManager)
        assertTrue(beaconManager.isForegroundServiceScanningEnabled)
        assertNull(beaconManager.foregroundServiceNotification)
        assertEquals(NOTIFICATION_ID, beaconManager.foregroundServiceNotificationId)
    }

    @Test
    fun serviceSharesTheNotificationTheAppShowsWithTheSameId() {
        notificationManager.notify(NOTIFICATION_ID, notification("Location in use"))
        beaconManager.enableForegroundServiceScanning(null, NOTIFICATION_ID)
        val service = startService()
        assertEquals(NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertEquals("Location in use", title(shadowOf(service).lastForegroundNotification))
    }

    @Test
    fun serviceShowsAPlainNotificationWhenTheAppShowsNone() {
        notificationManager.notify(NOTIFICATION_ID + 1, notification("Another notification"))
        beaconManager.enableForegroundServiceScanning(null, NOTIFICATION_ID)
        val service = startService()
        val notification = shadowOf(service).lastForegroundNotification
        assertEquals(NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertEquals(CHANNEL_ID, NotificationCompat.getChannelId(notification))
        val channel = shadowOf(notificationManager).notificationChannels.map { it as NotificationChannel }.single { it.id == CHANNEL_ID }
        assertEquals("Beacon scanning", channel.name)
        assertEquals(context.packageManager.getApplicationLabel(context.applicationInfo).toString(), title(notification))
    }

    @Test
    @Config(sdk = [21])
    fun serviceShowsAPlainNotificationBeforeNotificationChannels() {
        beaconManager.enableForegroundServiceScanning(null, NOTIFICATION_ID)
        val service = startService()
        assertEquals(NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertEquals(context.packageManager.getApplicationLabel(context.applicationInfo).toString(),
            title(shadowOf(service).lastForegroundNotification))
    }

    @Test
    fun serviceShowsTheNotificationItIsGiven() {
        val given = notification("Scanning for beacons")
        beaconManager.enableForegroundServiceScanning(given, NOTIFICATION_ID)
        val service = startService()
        assertSame(given, shadowOf(service).lastForegroundNotification)
    }

    @Test
    fun serviceStaysInTheBackgroundWithoutForegroundServiceScanning() {
        val service = startService()
        assertNull(shadowOf(service).lastForegroundNotification)
    }

    private fun startService(): BeaconService {
        val controller = Robolectric.buildService(BeaconService::class.java).create()
        services.add(controller)
        return controller.get()
    }

    private fun notification(title: String): Notification =
        NotificationCompat.Builder(context, "app")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .build()

    private fun title(notification: Notification?): String? =
        notification?.let { NotificationCompat.getExtras(it)?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() }

    companion object {
        private const val NOTIFICATION_ID = 456
        private const val CHANNEL_ID = "org.altbeacon.beacon.service.BeaconService"
    }
}
