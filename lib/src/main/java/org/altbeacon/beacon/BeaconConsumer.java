package org.altbeacon.beacon;
/**
 * An interface for an Android <code>Activity</code> or <code>Service</code>
 * that wants to interact with beacons.  The interface is used in conjunction
 * with <code>BeaconManager</code> and provides a callback when the <code>BeaconService</code>
 * is ready to use.  Until this callback is made, ranging and monitoring of beacons is not
 * possible.
 *
 * In the example below, an Activity implements the <code>BeaconConsumer</code> interface, binds
 * to the service, then when it gets the callback saying the service is ready, it starts ranging.
 *
 *  <pre><code>
 *  public class RangingActivity extends Activity implements BeaconConsumer {
 *      protected static final String TAG = "RangingActivity";
 *      private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
 *      {@literal @}Override
 *      protected void onCreate(Bundle savedInstanceState) {
 *          super.onCreate(savedInstanceState);
 *          setContentView(R.layout.activity_ranging);
 *          beaconManager.bind(this);
 *      }
 *
 *      {@literal @}Override
 *      protected void onDestroy() {
 *          super.onDestroy();
 *          beaconManager.unbind(this);
 *      }
 *
 *      {@literal @}Override
 *      public void onBeaconServiceConnect() {
 *          beaconManager.setRangeNotifier(new RangeNotifier() {
 *            {@literal @}Override
 *            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
 *                 if (beacons.size() > 0) {
 *                      Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
 *                 }
 *            }
 *          });
 *
 *          try {
 *              beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
 *          } catch (RemoteException e) {
 *              e.printStackTrace();
 *          }
 *      }
 *  }
 *  </code></pre>
 *
 * @see BeaconManager
 *
 * @author David G. Young
 * @deprecated Will be removed in 3.0.  See http://altbeacon.github.io/android-beacon-library/autobind.html
 */
@Deprecated
public interface BeaconConsumer extends InternalBeaconConsumer { }