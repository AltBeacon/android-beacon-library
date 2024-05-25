package org.altbeacon.beacon

class BeaconRegion(uniqueId: String, beaconParser: BeaconParser?, identifiers: List<Identifier>, bluetoothAddress: String?): Region(uniqueId, beaconParser, identifiers, bluetoothAddress, 3) {
    constructor(uniqueId: String, beaconParser: BeaconParser, id1: String?, id2: String?, id3: String?) : this(uniqueId, beaconParser, listOf(Identifier.parse(id1), Identifier.parse(id2), Identifier.parse(id3)), null) {
    }
    constructor(uniqueId: String, macAddress: String) : this(uniqueId, null, arrayListOf(), macAddress) {
    }
}
