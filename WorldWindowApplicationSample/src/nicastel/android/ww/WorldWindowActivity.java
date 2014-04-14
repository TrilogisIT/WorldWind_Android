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
package nicastel.android.ww;

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindowGLSurfaceView;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import nicastel.android.ww.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nicastel.android.ww.dialogs.AddWMSDialog;
import nicastel.android.ww.dialogs.TocDialog;
import nicastel.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;
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

    private static final String TAG = "NicastelWWExample";

    // This parameters are useful for WMS Addition and view.
    // Thanks to the Autonomous Province of Bolzano (Italy) for the Open WMS Server.
    // The Use of their WMS Services for commercial and/or support to companies is allowed.
    public final static String DEFAULT_WMS_URL = "http://sdi.provinz.bz.it/geoserver/wms";
    private final static double BOLZANO_LATITUDE = 46.4995d;
    private final static double BOLZANO_LONGITUDE = 11.3254d;
    private final static double BOLZANO_VIEW_HEADING = 60d;
    private final static double BOLZANO_VIEW_TILT = 60d;
    private final static double BOLZANO_VIEW_DISTANCE_KM = 13000d;

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

        // set the contentview
        this.setContentView(R.layout.main);
        // And initialize the WorldWindow Model and View
        this.wwd = (WorldWindowGLSurfaceView) this.findViewById(R.id.wwd);
        this.wwd.setModel((Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME));
        this.setupView();
        this.setupTextViews();
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

            case R.id.menu_show_wms:
                // TODO show webview with trilogis INFO! See walk&hike app
                break;
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
        BasicView view = (BasicView) this.wwd.getView();
        Globe globe = this.wwd.getModel().getGlobe();
        // set the initial position to "Bolzano", where you can see the WMS Layers
        view.setLookAtPosition(Position.fromDegrees(BOLZANO_LATITUDE, BOLZANO_LONGITUDE,
            globe.getElevation(Angle.fromDegrees(BOLZANO_LATITUDE), Angle.fromDegrees(BOLZANO_LONGITUDE))));
        view.setHeading(Angle.fromDegrees(BOLZANO_VIEW_HEADING));
        view.setTilt(Angle.fromDegrees(BOLZANO_VIEW_TILT));
        view.setRange(BOLZANO_VIEW_DISTANCE_KM);
    }

    protected void setupTextViews() {
        TextView latTextView = (TextView) findViewById(R.id.latvalue);
        this.wwd.setLatitudeText(latTextView);
        TextView lonTextView = (TextView) findViewById(R.id.lonvalue);
        this.wwd.setLongitudeText(lonTextView);
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
