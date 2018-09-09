---
layout: android-beacon-library
---

The basic library architecture can be seen in the diagram below. The BeaconService and ScanJob are mutually exclusive -- the library uses either one or the other based on the operating system version or a configuration override. 

  <img src="images/block-diagram.svg"/>

