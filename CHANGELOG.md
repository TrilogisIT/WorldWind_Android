Change Log
===============================================================================
Version 1.0.3 *(2014-04-28)*
----------------------------

 * LIBRARY: Added support to every device by using ETC1 texture compression instead of DDS.
 * LIBRARY: Added WMSLayerName parameter in the TiledImageLayer (for further usages).
 * LIBRARY: Added a function in BasicView that enables animation to a specific location.
 * LIBRARY: Added GPSMarker as a new renderer item, so it is possible to render the user position in 3D.
 * EXAMPLE: Updated manifest to support more devices (Phones too)
 * DEMOAPP: Updated APK to latest version (1.0.3) available in Google Play Store too.
 
Version 1.0.1 *(2013-11-22)*
----------------------------

 * LIBRARY: Fixed CompassLayer issue which lead the compass to rotate with a wrong heading.

 
Version 1.0.1 *(2013-10-25)*
----------------------------

 * DEMOAPP: Added APK with ready to try demo application
 * EXAMPLE: Demo Application renaming and installation limited to tablets since phones are not able to read DDS for the services. We are working to support phones with lower detailed tiles.


Version 1.0.0 *(2013-10-23)*
----------------------------
Initial release.

 * LIBRARY: Supported the addition of WMS Layers, Compass Layer, WorldMap Layer, SkyGradient Layer, ScaleBarLayer. Fixed some issues with XML parsing that lead to loose opening tags in placeholders.
 * EXAMPLE: Added an extra demo with an integration with Picasso library in CardThumbnail

