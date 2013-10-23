/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWXML;
import java.beans.PropertyChangeEvent;
import javax.xml.xpath.XPath;
import org.w3c.dom.Element;
import android.graphics.Point;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: AbstractLayer.java 778 2012-09-19 17:52:20Z dcollins $
 */
// TODO ScreenCredit has not been implemented on Android
public abstract class AbstractLayer extends WWObjectImpl implements Layer {
	protected boolean enabled = true;
	protected boolean pickable = true;
	protected double opacity = 1d;
	protected double minActiveAltitude = -Double.MAX_VALUE;
	protected double maxActiveAltitude = Double.MAX_VALUE;
	protected boolean networkDownloadEnabled = true;
	protected long expiryTime = 0;
	protected FileStore dataFileStore = WorldWind.getDataFileStore();

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isPickEnabled() {
		return pickable;
	}

	public void setPickEnabled(boolean pickable) {
		this.pickable = pickable;
	}

	public void setEnabled(boolean enabled) {
		Boolean oldEnabled = this.enabled;
		this.enabled = enabled;
		this.propertyChange(new PropertyChangeEvent(this, "Enabled", oldEnabled, this.enabled));
	}

	public String getName() {
		Object n = this.getValue(AVKey.DISPLAY_NAME);

		return n != null ? n.toString() : this.toString();
	}

	public void setName(String name) {
		this.setValue(AVKey.DISPLAY_NAME, name);
	}

	@Override
	public String toString() {
		Object n = this.getValue(AVKey.DISPLAY_NAME);

		return n != null ? n.toString() : super.toString();
	}

	public double getOpacity() {
		return this.opacity;
	}

	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}

	public double getMinActiveAltitude() {
		return this.minActiveAltitude;
	}

	public void setMinActiveAltitude(double minActiveAltitude) {
		this.minActiveAltitude = minActiveAltitude;
	}

	public double getMaxActiveAltitude() {
		return this.maxActiveAltitude;
	}

	public void setMaxActiveAltitude(double maxActiveAltitude) {
		this.maxActiveAltitude = maxActiveAltitude;
	}

	public double getScale() {
		Object o = this.getValue(AVKey.MAP_SCALE);
		return o instanceof Double ? (Double) o : 1;
	}

	public boolean isNetworkRetrievalEnabled() {
		return networkDownloadEnabled;
	}

	public void setNetworkRetrievalEnabled(boolean networkDownloadEnabled) {
		this.networkDownloadEnabled = networkDownloadEnabled;
	}

	public FileStore getDataFileStore() {
		return this.dataFileStore;
	}

	public void setDataFileStore(FileStore fileStore) {
		if (fileStore == null) {
			String message = Logging.getMessage("nullValue.FileStoreIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		this.dataFileStore = fileStore;
	}

	public boolean isLayerInView(DrawContext dc) {
		if (dc == null) {
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		return true;
	}

	public boolean isLayerActive(DrawContext dc) {
		if (dc == null) {
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (dc.getView() == null) {
			String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		Position eyePos = dc.getView().getEyePosition(dc.getGlobe());
		if (eyePos == null) return false;

		double altitude = eyePos.elevation;
		return altitude >= this.minActiveAltitude && altitude <= this.maxActiveAltitude;
	}

	public void pick(DrawContext dc, Point point) {
		if (!this.enabled) return; // Don't check for arg errors if we're disabled

		if (dc == null) {
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (dc.getGlobe() == null) {
			String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (dc.getView() == null) {
			String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (!this.isLayerActive(dc)) return;

		if (!this.isLayerInView(dc)) return;

		this.doPick(dc, point);
	}

	/**
	 * @param dc
	 *            the current draw context
	 * @throws IllegalArgumentException
	 *             if <code>dc</code> is null, or <code>dc</code>'s <code>Globe</code> or <code>View</code> is null
	 */
	public void render(DrawContext dc) {
		if (!this.enabled) return; // Don't check for arg errors if we're disabled

		if (dc == null) {
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (dc.getGlobe() == null) {
			String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (dc.getView() == null) {
			String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		if (!this.isLayerActive(dc)) return;

		if (!this.isLayerInView(dc)) return;

		this.doRender(dc);
	}

	public void dispose() // override if disposal is a supported operation
	{
	}

	protected void doPick(DrawContext dc, Point point) {
		// any state that could change the color needs to be disabled, such as GL_TEXTURE, GL_LIGHTING or GL_FOG.
		// re-draw with unique colors
		// store the object info in the selectable objects table
		// read the color under the cursor
		// use the color code as a key to retrieve a selected object from the selectable objects table
		// create an instance of the PickedObject and add to the dc via the dc.addPickedObject() method
	}

	protected abstract void doRender(DrawContext dc);

	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}

	public long getExpiryTime() {
		return this.expiryTime;
	}

	// **************************************************************//
	// ******************** Configuration *************************//
	// **************************************************************//

	/**
	 * Parses layer configuration parameters from the specified DOM document. This writes output as key-value pairs to
	 * params. If a parameter from the XML document already exists in params, that parameter is ignored. Supported key
	 * and parameter names are:
	 * <table>
	 * <tr>
	 * <th>Parameter</th>
	 * <th>Element Path</th>
	 * <th>Type</th>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#DISPLAY_NAME}</td>
	 * <td>DisplayName</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#OPACITY}</td>
	 * <td>Opacity</td>
	 * <td>Double</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#MAX_ACTIVE_ALTITUDE}</td>
	 * <td>ActiveAltitudes/@max</td>
	 * <td>Double</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#MIN_ACTIVE_ALTITUDE}</td>
	 * <td>ActiveAltitudes/@min</td>
	 * <td>Double</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#NETWORK_RETRIEVAL_ENABLED}</td>
	 * <td>NetworkRetrievalEnabled</td>
	 * <td>Boolean</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#MAP_SCALE}</td>
	 * <td>MapScale</td>
	 * <td>Double</td>
	 * </tr>
	 * </table>
	 * 
	 * @param domElement
	 *            the XML document root to parse for layer configuration elements.
	 * @param params
	 *            the output key-value pairs which receive the layer configuration parameters. A null reference
	 *            is permitted.
	 * @return a reference to params, or a new AVList if params is null.
	 * @throws IllegalArgumentException
	 *             if the document is null.
	 */
	public static AVList getLayerConfigParams(Element domElement, AVList params) {
		if (domElement == null) {
			String message = Logging.getMessage("nullValue.DocumentIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		XPath xpath = WWXML.makeXPath();

		WWXML.checkAndSetStringParam(domElement, params, AVKey.DISPLAY_NAME, "DisplayName", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.OPACITY, "Opacity", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MAX_ACTIVE_ALTITUDE, "ActiveAltitudes/@max", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MIN_ACTIVE_ALTITUDE, "ActiveAltitudes/@min", xpath);
		WWXML.checkAndSetBooleanParam(domElement, params, AVKey.NETWORK_RETRIEVAL_ENABLED, "NetworkRetrievalEnabled", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MAP_SCALE, "MapScale", xpath);
		WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS, "MaxAbsentTileAttempts", xpath);
		WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL, "MinAbsentTileCheckInterval", xpath);

		return params;
	}
}
