WorldWind_Android
=================

WorldWind Library and Example code for the Android framework.

This repository contains the updates to the **NASA WorldWind** library for the Android mobile OS.
The Library has updated with some bugfixing (XML parsing and other small issues), and with the support for new layers.

*  `CompassLayer` to display a Compass (that dinamically rotates and tilts) in the upper right corner of the view.
*  `ScalebarLayer` to display a Scalebar in the bottom part of the view.
*  `SkyGradientLayer` to display the Sky in the three dimensional environment. This layer has a nice visual impact when looking at the horizon.
*  `WMSTiledImageLayer` to display an arbitrary WMS layer. See the application example.
*  `WorldMapLayer` to display a WorldMap thumbnail in the upper left part of the view which shows where in the world the user is looking at.

**It requires API 15+**

## Examples

The project `WorldWindowApplicationSample` contains a full and working example of how to use the new layers.

ChangeLog
-------

* [Changelog:](https://github.com/TrilogisIT/WorldWind_Android/tree/master/CHANGELOG.md) A complete changelog


Credits
-------

Author: Nicola Dorigatti ([Trilogis Srl](http://www.trilogis.it))

* WorldWind is an opensource API relased by [NASA](http://www.nasa.gov/) under the [NASA Open Source Agreement (NOSA)](http://worldwind.arc.nasa.gov/worldwind-nosa-1.3.html)

* The WMS Layers used in the Application Sample are kindly made available from the [Autonomous Province of Bolzano (Italy)](http://www.provincia.bz.it/aprov/amministrazione/default.asp).

![Screen](http://www.trilogis.it/wp-content/uploads/2013/07/logo_ufficiale-e1375429066884.png)
![Screen](http://www.nasa.gov/sites/all/themes/custom/NASAOmegaHTML5/images/nasa-logo.png)
![Screen](http://www.provinz.bz.it/de/css/img/aprov_full.png)

Further information and screenshots can be found at [our website](http://www.trilogis.it/?portfolio=3ddroid&lang=en)

NASA and the NASA logo are registered trademarks of NASA, used with permission.
