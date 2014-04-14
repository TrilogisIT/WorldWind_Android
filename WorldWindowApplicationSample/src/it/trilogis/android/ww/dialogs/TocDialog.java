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
package it.trilogis.android.ww.dialogs;

import gov.nasa.worldwind.WorldWindowGLSurfaceView;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import it.trilogis.android.ww.R;
import it.trilogis.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;
import it.trilogis.android.ww.view.DragListView;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * @author Nicola Dorigatti
 */
public class TocDialog extends DialogFragment {
    private static final String TAG = "TocDialog";
    private LayerArrayAdapter mListViewAdapter = null;
    private DragListView mListView;
    WorldWindowGLSurfaceView wwd = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.toc_view_dialog, null);
        mListView = (DragListView) view.findViewById(android.R.id.list);

        View removerow = view.findViewById(R.id.removeView);
        mListView.setTrashcan((ImageView) removerow);
        mListView.setDropListener(onDrop);
        mListView.setRemoveListener(onRemove);
        // Init WorldWind Data
        initWorldWindLayerAdapter();
        // Set view
        builder.setView(view);
        builder.setPositiveButton(getString(android.R.string.ok), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "Created layers that will be added to worldWind: ", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNeutralButton(getString(R.string.menu_action_text_wms), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Add dialog for wms
                openAddWMSDialog();
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /*
     * (non-Javadoc)
     * @see android.app.DialogFragment#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAddWMSDialog();
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mListViewAdapter != null) {
            mListViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Set the class parameter that will be used to fill adapter and perform operations.
     * 
     * @param wwd
     *            The WorldWind context to be used.
     */
    public void setWorldWindData(WorldWindowGLSurfaceView wwd) {
        if (null == wwd) {
            Log.e(TAG, "Setting null world wind data!!!");
            return;
        }
        this.wwd = wwd;
        // Initialize list view, adapters and so on...
        initWorldWindLayerAdapter();
    }

    /**
	 * 
	 */
    private void initWorldWindLayerAdapter() {
        if (null == wwd) {
            Log.e(TAG, "Trying to initialize layer adapter with not valid WorldWind context!!");
            return;
        }
        if (null == mListView) {
            Log.e(TAG, "Trying to initialize layer list view, but list view is null!!");
            return;
        }
        LayerList layers = wwd.getModel().getLayers();
        mListViewAdapter = new LayerArrayAdapter(getActivity(), layers);
        mListView.setAdapter(mListViewAdapter);
    }

    private DragListView.DropListener onDrop = new DragListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            Layer item = mListViewAdapter.getItem(from);
            mListViewAdapter.remove(item);
            mListViewAdapter.insert(item, to);
        }
    };

    private DragListView.RemoveListener onRemove = new DragListView.RemoveListener() {
        @Override
        public void remove(int which) {
            mListViewAdapter.remove(mListViewAdapter.getItem(which));
        }
    };

    private class LayerArrayAdapter extends ArrayAdapter<Layer> {

        private final List<Layer> list;

        public LayerArrayAdapter(Activity context, List<Layer> list) {
            super(context, R.layout.toc_list_view_item, list);
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Layer layer = list.get(position);
            View retval = convertView;
            if (retval == null) {
                LayoutInflater inflator = getActivity().getLayoutInflater();
                retval = inflator.inflate(R.layout.toc_list_view_item, null);
            }
            CheckBox checkbox = (CheckBox) retval.findViewById(R.id.toc_item_checkbox);
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    layer.setEnabled(buttonView.isChecked());
                }
            });
            checkbox.setChecked(layer.isEnabled());
            checkbox.setText(layer.getName());
            return retval;
        }
    }

    // ===================== ADD WMS ================================0
    private void openAddWMSDialog() {
        AddWMSDialog wmsLayersDialog = new AddWMSDialog();
        wmsLayersDialog.setOnAddWMSLayersListener(mListener);
        wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
    }

    private OnAddWMSLayersListener mListener = new OnAddWMSLayersListener() {

        @Override
        public void onAddWMSLayers(List<Layer> layersToAdd) {

            if (null == layersToAdd || layersToAdd.isEmpty() || null == wwd) {
                Log.w(TAG, "Null or empty layers/WorldWindContext to add!");
                return;
            }
            for (Layer lyr : layersToAdd) {
                boolean added = wwd.getModel().getLayers().addIfAbsent(lyr);
                Log.d(TAG, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!");

            }
            if (mListViewAdapter != null) {
                mListViewAdapter.notifyDataSetChanged();
            }

        }
    };
}
