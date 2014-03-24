/*
 * Copyright (C) 2013 Trilogis S.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.trilogis.android.ww;

import android.opengl.GLSurfaceView;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import it.trilogis.android.ww.dialogs.AddWMSDialog;
import it.trilogis.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;
import it.trilogis.android.ww.dialogs.TocDialog;
import java.io.File;
import java.io.IOException;
import java.util.*;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * @author Nicola Dorigatti
 */
public class WorldWindowActivity extends Activity {
    static {
        System.setProperty("gov.nasa.worldwind.app.config.document", "config/wwandroiddemo.xml");
    }

    private static final String TAG = "TrilogisWWExample";

    // This parameters are useful for WMS Addition and view.
    // Thanks to the Autonomous Province of Bolzano (Italy) for the Open WMS Server.
    // The Use of their WMS Services for commercial and/or support to companies is allowed.
    public final static String DEFAULT_WMS_URL = "http://sdi.provinz.bz.it/geoserver/wms";
    private final static double BOLZANO_LATITUDE = 46.4995d;
    private final static double BOLZANO_LONGITUDE = 11.3254d;
    private final static double BOLZANO_VIEW_HEADING = 60d;
    private final static double BOLZANO_VIEW_TILT = 60d;
    private final static double BOLZANO_VIEW_DISTANCE_KM = 13000d;

	public static final int INITIAL_LATITUDE = 40;
	public static final int INITIAL_LONGITUDE = -120;

    protected WorldWindowGLSurfaceView wwd;

    // private CompassLayer cl;
    // private WorldMapLayer wml;
    // private SkyGradientLayer sgl;
    // private ScalebarLayer sbl;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting the location of the file store on Android as cache directory. Doing this, when user has no space left
        // on the device, if he asks to the system to free Cache of apps, all the MB/GB of WorldWindApplication will be cleared!
        File fileDir = getCacheDir();// getFilesDir();
		Logging.info("Application cache directory: " + fileDir);
		if (null != fileDir && fileDir.exists() && fileDir.canWrite()) {
            File output = new File(fileDir, ".nomedia");
            if (!output.exists()) {
                try {
                    output.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "IOException while creating .nomedia: " + e.getMessage());
                }
            }
        }
        System.setProperty("gov.nasa.worldwind.platform.user.store", fileDir.getAbsolutePath());

        WWIO.setContext(this);

        setContentView(R.layout.main);

        wwd = (WorldWindowGLSurfaceView) findViewById(R.id.wwd);
		wwd.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);

		wwd.setModel((Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME));
		wwd.setLatitudeText((TextView) findViewById(R.id.latvalue));
		wwd.setLongitudeText((TextView) findViewById(R.id.lonvalue));

		BasicView view = (BasicView) wwd.getView();
		Globe globe = wwd.getModel().getGlobe();

		view.setLookAtPosition(Position.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE,
				globe.getElevation(Angle.fromDegrees(INITIAL_LATITUDE), Angle.fromDegrees(INITIAL_LONGITUDE))));
//		view.setHeading(Angle.fromDegrees(BOLZANO_VIEW_HEADING));
		view.setTilt(Angle.fromDegrees(45));
		view.setRange(7000);

		RenderableLayer layer = new RenderableLayer();
		layer.setName("Renderable");
		CustomBox box = new CustomBox(Position.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE,
				globe.getElevation(Angle.fromDegrees(INITIAL_LATITUDE), Angle.fromDegrees(INITIAL_LONGITUDE))), 2000);
		layer.addRenderable(box);

		Path field = new Path();
		ArrayList<Position> positions = new ArrayList<Position>();
		positions.add(Position.fromDegrees(INITIAL_LATITUDE+1, INITIAL_LONGITUDE+1));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE+1, INITIAL_LONGITUDE-1));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE-1, INITIAL_LONGITUDE-1));
		positions.add(Position.fromDegrees(INITIAL_LATITUDE-1, INITIAL_LONGITUDE+1));
		field.setPositions(positions);
		field.setExtrude(true);
		field.setFollowTerrain(true);
		field.setDrawVerticals(true);
		field.setShowPositions(true);
		layer.addRenderable(field);

		insertBeforeCompass(wwd, layer);
    }

	public static void insertBeforeCompass(WorldWindow wwd, Layer layer)
	{
		// Insert the layer into the layer list just before the compass.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers)
		{
			if (l instanceof CompassLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

    @Override
    protected void onPause() {
        super.onPause();
        wwd.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wwd.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // case R.id.menu_toggle_compass:
            // if (null == cl) {
            // cl = searchSpecificLayer(CompassLayer.class);
            // }
            // if (null == cl) {
            // cl = new CompassLayer();
            // cl.setName("Compass");
            // if (this.wwd.getModel().getLayers().add(cl)) Log.d(TAG, "CompassLayer created from scratch and added!!");
            // } else {
            // if (this.wwd.getModel().getLayers().contains(cl)) {
            // cl.setEnabled(!cl.isEnabled());
            // // this.wwd.getModel().getLayers().remove(cl);
            // Log.d(TAG, "CompassLayer Removed!!");
            // } else {
            // this.wwd.getModel().getLayers().addIfAbsent(cl);
            // cl.setEnabled(true);
            // Log.d(TAG, "CompassLayer not created but added!!");
            // }
            // }
            // break;
            // case R.id.menu_toggle_worldmap:
            // if (null == wml) {
            // wml = searchSpecificLayer(WorldMapLayer.class);
            // }
            // if (null == wml) {
            // wml = new WorldMapLayer();
            // wml.setName("WorldMap");
            // if (this.wwd.getModel().getLayers().add(wml)) Log.d(TAG, "WorldMapLayer created from scratch and added!");
            // } else {
            // if (this.wwd.getModel().getLayers().contains(wml)) {
            // wml.setEnabled(!wml.isEnabled());
            // Log.d(TAG, "WorldMapLayer Removed!!");
            // } else {
            // this.wwd.getModel().getLayers().addIfAbsent(wml);
            // wml.setEnabled(true);
            // Log.d(TAG, "WorldMapLayer not created but added!!");
            // }
            // }
            // break;
            // case R.id.menu_toggle_sky:
            // if (null == sgl) {
            // sgl = searchSpecificLayer(SkyGradientLayer.class);
            // }
            // if (null == sgl) {
            // sgl = new SkyGradientLayer();
            // sgl.setName("Sky");
            // if (this.wwd.getModel().getLayers().add(sgl)) Log.d(TAG, "SkyGradientLayer created from scratch and added!");
            // } else {
            // if (this.wwd.getModel().getLayers().contains(sgl)) {
            // sgl.setEnabled(!sgl.isEnabled());
            // Log.d(TAG, "SkyGradientLayer Removed!!");
            // } else {
            // this.wwd.getModel().getLayers().addIfAbsent(sgl);
            // sgl.setEnabled(true);
            // Log.d(TAG, "SkyGradientLayer not created but added!!");
            // }
            // }
            // break;
            // case R.id.menu_toggle_scalebar:
            // if (null == sbl) {
            // sbl = searchSpecificLayer(ScalebarLayer.class);
            // }
            // if (null == sbl) {
            // sbl = new ScalebarLayer();
            // sbl.setName("Scale Bar");
            // if (this.wwd.getModel().getLayers().add(sbl)) Log.d(TAG, "ScalebarLayer created from scratch and added!");
            // } else {
            // if (this.wwd.getModel().getLayers().contains(sbl)) {
            // sbl.setEnabled(!sbl.isEnabled());
            // Log.d(TAG, "ScaleBarLayer Removed!!");
            // } else {
            // this.wwd.getModel().getLayers().addIfAbsent(sbl);
            // sbl.setEnabled(true);
            // Log.d(TAG, "ScaleBarLayer not created but added!!");
            // }
            // }
            // break;
            case R.id.menu_add_wms:
                AddWMSDialog wmsLayersDialog = new AddWMSDialog();
                wmsLayersDialog.setOnAddWMSLayersListener(new OnAddWMSLayersListener() {

                    public void onAddWMSLayers(List<Layer> layersToAdd) {
                        for (Layer lyr : layersToAdd) {
                            boolean added = WorldWindowActivity.this.wwd.getModel().getLayers().addIfAbsent(lyr);
                            Log.d(TAG, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!");
                        }
                    }
                });
                wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
                return true;
            case R.id.show_layers_toc:
                TocDialog tocDialog = new TocDialog();
                tocDialog.setWorldWindData(wwd);
                tocDialog.show(getFragmentManager(), "tocDialog");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T extends Layer> T searchSpecificLayer(Class<T> classToSearch) {
        for (Layer lyr : wwd.getModel().getLayers()) {
            if (classToSearch.isInstance(lyr))
                return (T) lyr;
        }
        return null;
    }
}
