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

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.WorldWindowGLSurfaceView;
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
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting the location of the file store on Android as cache directory. Doing this, when user has no space left
        // on the device, if he asks to the system to free Cache of apps, all the MB/GB of WorldWindApplication will be cleared!
        File fileDir = getExternalCacheDir();// getFilesDir();
        Logging.info("Application cache directory: " + fileDir);
        if (null != fileDir && fileDir.exists() && fileDir.canWrite()) {
            // create .nomedia file, so pictures will not be visible in the gallery (otherwise, it's really awful to see all of the tiles as images!)
            File output = new File(fileDir, ".nomedia");
            if (output.exists()) {
                Log.d(TAG, "No need to create .nomedia file, it's already there! : " + output.getAbsolutePath());
            } else {
                // lets create the file
                boolean fileCreated = false;
                try {
                    fileCreated = output.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "IOException while creating .nomedia: " + e.getMessage());
                }
                if (!fileCreated) {
                    Log.e(TAG, ".nomedia file not created!");
                } else {
                    Log.d(TAG, ".nomedia file created!");
                }
            }
        }
        // Setup system property for the file store
        System.setProperty("gov.nasa.worldwind.platform.user.store", fileDir.getAbsolutePath());
        WWIO.setContext(this);
        // set the contentview
        this.setContentView(R.layout.main);
        // And initialize the WorldWindow Model and View
        this.wwd = (WorldWindowGLSurfaceView) this.findViewById(R.id.wwd);
        this.wwd.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        this.wwd.setModel((Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME));
//    	wwd.setLatitudeText((TextView) findViewById(R.id.latvalue));
//    	wwd.setLongitudeText((TextView) findViewById(R.id.lonvalue));
        this.setupView();
        RenderableLayer layer = new RenderableLayer();
		layer.setName("Renderable");
		CustomBox box = new CustomBox(Position.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE,
				wwd.getModel().getGlobe().getElevation(Angle.fromDegrees(INITIAL_LATITUDE), Angle.fromDegrees(INITIAL_LONGITUDE))), 2000);
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
        // Pause the OpenGL ES rendering thread.
        this.wwd.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume the OpenGL ES rendering thread.
        this.wwd.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Configure the application's options menu using the XML file res/menu/options.xml.
        this.getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

//            case R.id.menu_show_wms:
//                // TODO show webview with trilogis INFO! See walk&hike app
//                break;
            case R.id.menu_add_wms:
                openAddWMSDialog();
                break;
            case R.id.show_layers_toc:
                // Toast.makeText(getApplicationContext(), "Showing TOC!", Toast.LENGTH_LONG).show();
                showLayerManager();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T extends Layer> T searchSpecificLayer(Class<T> classToSearch) {
        if (null == this.wwd || null == this.wwd.getModel() || null == this.wwd.getModel().getLayers()) {
            Log.e(TAG, "No layers in model!");
            return null;
        }
        LayerList layers = this.wwd.getModel().getLayers();
        for (Layer lyr : layers) {
            if (classToSearch.isInstance(lyr)) {
                return (T) lyr;
            }
        }
        return null;
    }

    protected void setupView() {
    	BasicView view = (BasicView) wwd.getView();
		Globe globe = wwd.getModel().getGlobe();

		view.setLookAtPosition(Position.fromDegrees(INITIAL_LATITUDE, INITIAL_LONGITUDE,
				globe.getElevation(Angle.fromDegrees(INITIAL_LATITUDE), Angle.fromDegrees(INITIAL_LONGITUDE))));
//		view.setHeading(Angle.fromDegrees(BOLZANO_VIEW_HEADING));
		view.setTilt(Angle.fromDegrees(45));
		view.setRange(7000);
    }


    // ============== Add WMS ======================= //
    private void openAddWMSDialog() {
        AddWMSDialog wmsLayersDialog = new AddWMSDialog();
        wmsLayersDialog.setOnAddWMSLayersListener(mListener);
        wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
    }

    private OnAddWMSLayersListener mListener = new OnAddWMSLayersListener() {

        @Override
        public void onAddWMSLayers(List<Layer> layersToAdd) {
            if (null == layersToAdd || layersToAdd.isEmpty()) {
                Log.w(TAG, "Null or empty layers to add!");
                return;
            }
            for (Layer lyr : layersToAdd) {
                boolean added = WorldWindowActivity.this.wwd.getModel().getLayers().addIfAbsent(lyr);
                Log.d(TAG, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!");
            }
        }
    };

    // ============== Show Layer Manager ============ //
    private void showLayerManager() {
        TocDialog tocDialog = new TocDialog();
        tocDialog.setWorldWindData(this.wwd);
        tocDialog.show(getFragmentManager(), "tocDialog");
    }
}
