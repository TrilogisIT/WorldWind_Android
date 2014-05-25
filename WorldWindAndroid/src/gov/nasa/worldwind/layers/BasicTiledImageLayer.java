/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.event.BulkRetrievalListener;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuTextureData;
import gov.nasa.worldwind.render.GpuTextureTile;
import gov.nasa.worldwind.retrieve.AbstractRetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.BulkRetrievable;
import gov.nasa.worldwind.retrieve.BulkRetrievalThread;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.AbsentResourceList;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWXML;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author tag
 * @version $Id: BasicTiledImageLayer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class BasicTiledImageLayer extends TiledImageLayer implements BulkRetrievable {
	protected final Object fileLock = new Object();

	// Layer resource properties.
	protected ScheduledExecutorService resourceRetrievalService;
	protected AbsentResourceList absentResources;
	protected static final int RESOURCE_ID_OGC_CAPABILITIES = 1;
	protected static final int DEFAULT_MAX_RESOURCE_ATTEMPTS = 3;
	protected static final int DEFAULT_MIN_RESOURCE_CHECK_INTERVAL = (int) 6e5; // 10 minutes


	public BasicTiledImageLayer(LevelSet levelSet) {
		super(levelSet);
	}

	public BasicTiledImageLayer(AVList params) {
		// this(new LevelSet(params));
		super(params);

		// TODO String[] strings = (String[]) params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS);
		// if (strings != null && strings.length > 0) this.setAvailableImageFormats(strings);

		String s = params.getStringValue(AVKey.DISPLAY_NAME);
		if (s != null) this.setName(s);

		Double d = (Double) params.getValue(AVKey.OPACITY);
		if (d != null) this.setOpacity(d);

		d = (Double) params.getValue(AVKey.MAX_ACTIVE_ALTITUDE);
		if (d != null) this.setMaxActiveAltitude(d);

		d = (Double) params.getValue(AVKey.MIN_ACTIVE_ALTITUDE);
		if (d != null) this.setMinActiveAltitude(d);

		d = (Double) params.getValue(AVKey.MAP_SCALE);
		if (d != null) this.setValue(AVKey.MAP_SCALE, d);

		d = (Double) params.getValue(AVKey.DETAIL_HINT);
		if (d != null) this.setDetailHint(d);

		s = params.getStringValue(AVKey.TEXTURE_FORMAT);
		if (s != null) this.setTextureFormat(s);

		Boolean b = (Boolean) params.getValue(AVKey.FORCE_LEVEL_ZERO_LOADS);
		if (b != null) this.setForceLevelZeroLoads(b);
		b = (Boolean) params.getValue(AVKey.RETAIN_LEVEL_ZERO_TILES);
		if (b != null) this.setRetainLevelZeroTiles(b);

		b = (Boolean) params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED);
		if (b != null) this.setNetworkRetrievalEnabled(b);

		b = (Boolean) params.getValue(AVKey.USE_TRANSPARENT_TEXTURES);
		if (b != null) this.setUseTransparentTextures(b);

		Object o = params.getValue(AVKey.URL_CONNECT_TIMEOUT);
		if (o != null) this.setValue(AVKey.URL_CONNECT_TIMEOUT, o);

		o = params.getValue(AVKey.URL_READ_TIMEOUT);
		if (o != null) this.setValue(AVKey.URL_READ_TIMEOUT, o);

		o = params.getValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
		if (o != null) this.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, o);

		if (params.getValue(AVKey.TRANSPARENCY_COLORS) != null) this.setValue(AVKey.TRANSPARENCY_COLORS, params.getValue(AVKey.TRANSPARENCY_COLORS));

		this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());

		// If any resources should be retrieved for this Layer, start a task to retrieve those resources, and initialize
		// this Layer once those resources are retrieved.
		if (this.isRetrieveResources()) {
			this.startResourceRetrieval();
		}
	}

	public BasicTiledImageLayer(Document dom, AVList params) {
		this(dom.getDocumentElement(), params);
	}

	public BasicTiledImageLayer(Element domElement, AVList params) {
		this(getParamsFromDocument(domElement, params));
	}

	/** Overridden to cancel periodic non-tile resource retrieval tasks scheduled by this Layer. */
	@Override
	public void dispose() {
		super.dispose();

		// Stop any scheduled non-tile resource retrieval tasks. Resource retrievals are performed in a separate thread,
		// and are unnecessary once the Layer is disposed.
		this.stopResourceRetrieval();
	}

	protected static AVList getParamsFromDocument(Element domElement, AVList params) {
		if (domElement == null) {
			String message = Logging.getMessage("nullValue.DocumentIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		getTiledImageLayerConfigParams(domElement, params);
		setFallbacks(params);

		return params;
	}

	protected static void setFallbacks(AVList params) {
		if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null) {
			Angle delta = Angle.fromDegrees(36);
			params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
		}

		// BBB - Change Fallbacks
		if (params.getValue(AVKey.TILE_WIDTH) == null) params.setValue(AVKey.TILE_WIDTH, 128);

		if (params.getValue(AVKey.TILE_HEIGHT) == null) params.setValue(AVKey.TILE_HEIGHT, 128);

		if (params.getValue(AVKey.FORMAT_SUFFIX) == null) params.setValue(AVKey.FORMAT_SUFFIX, ".png");

		if (params.getValue(AVKey.NUM_LEVELS) == null) params.setValue(AVKey.NUM_LEVELS, 19); // approximately 0.1 meters per pixel

		if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null) params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
	}



	// *** Bulk download ***
	// *** Bulk download ***

	/**
	 * Start a new {@link BulkRetrievalThread} that downloads all imagery for a given sector and resolution to a
	 * specified {@link FileStore}, without downloading imagery tht is already in the file store.
	 * <p/>
	 * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create a downloader that has not been started,
	 * construct a {@link BasicTiledImageLayerBulkDownloader}.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 * 
	 * @param sector
	 *            the sector to download data for.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @param fileStore
	 *            the file store in which to place the downloaded imagery. If null the current World Wind file
	 *            cache is used.
	 * @param listener
	 *            an optional retrieval listener. May be null.
	 * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does
	 *         not intersect the layer bounding sector.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 * @see BasicTiledImageLayerBulkDownloader
	 */
	public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore, BulkRetrievalListener listener) {
		Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
		if (targetSector == null) return null;

		// TODO BasicTiledImageLayerBulkDownloader thread = new BasicTiledImageLayerBulkDownloader(this, targetSector, resolution, fileStore != null ? fileStore
		// : this.getDataFileStore(),
		// listener);
		TiledImageLayerBulkDownloader thread = new TiledImageLayerBulkDownloader(this, targetSector, resolution, fileStore != null ? fileStore : this.getDataFileStore(), listener);

		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	/**
	 * Start a new {@link BulkRetrievalThread} that downloads all imagery for a given sector and resolution to the
	 * current World Wind file cache, without downloading imagery that is already in the cache.
	 * <p/>
	 * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create a downloader that has not been started,
	 * construct a {@link TiledImageLayerBulkDownloader}.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 *
	 * @param sector
	 *            the sector to download imagery for.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @param listener
	 *            an optional retrieval listener. May be null.
	 * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does
	 *         not intersect the layer bounding sector.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 * @see TiledImageLayerBulkDownloader
	 */
	public BulkRetrievalThread makeLocal(Sector sector, double resolution, BulkRetrievalListener listener) {
		return makeLocal(sector, resolution, null, listener);
	}

	/**
	 * Get the estimated size in bytes of the imagery not in the World Wind file cache for the given sector and
	 * resolution.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 *
	 * @param sector
	 *            the sector to estimate.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @return the estimated size in bytes of the missing imagery.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 */
	public long getEstimatedMissingDataSize(Sector sector, double resolution) {
		return this.getEstimatedMissingDataSize(sector, resolution, null);
	}

	/**
	 * Get the estimated size in bytes of the imagery not in a specified file store for a specified sector and
	 * resolution.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 * 
	 * @param sector
	 *            the sector to estimate.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @param fileStore
	 *            the file store to examine. If null the current World Wind file cache is used.
	 * @return the estimated size in byte of the missing imagery.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 */
	public long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore) {
		Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
		if (targetSector == null) return 0;

		// TODO BasicTiledImageLayerBulkDownloader downloader = new BasicTiledImageLayerBulkDownloader(this, sector, resolution, fileStore != null ? fileStore :
		// this.getDataFileStore(),
		// null);
		TiledImageLayerBulkDownloader downloader = new TiledImageLayerBulkDownloader(this, sector, resolution, fileStore != null ? fileStore : this.getDataFileStore(), null);

		return downloader.getEstimatedMissingDataSize();
	}



	// *** Tile download ***
	// *** Tile download ***

	// **************************************************************//
	// ******************** Non-Tile Resource Retrieval ***********//
	// **************************************************************//

	/**
	 * Retrieves any non-tile resources associated with this Layer, either online or in the local filesystem, and
	 * initializes properties of this Layer using those resources. This returns a key indicating the retrieval state:
	 * {@link gov.nasa.worldwind.avlist.AVKey#RETRIEVAL_STATE_SUCCESSFUL} indicates the retrieval succeeded,
	 * {@link gov.nasa.worldwind.avlist.AVKey#RETRIEVAL_STATE_ERROR} indicates the retrieval failed with errors, and <code>null</code> indicates the retrieval
	 * state is unknown. This method may invoke blocking I/O operations, and
	 * therefore should not be executed from the rendering thread.
	 * 
	 * @return {@link gov.nasa.worldwind.avlist.AVKey#RETRIEVAL_STATE_SUCCESSFUL} if the retrieval succeeded,
	 *         {@link gov.nasa.worldwind.avlist.AVKey#RETRIEVAL_STATE_ERROR} if the retrieval failed with errors, and <code>null</code> if the retrieval state
	 *         is unknown.
	 */
	@SuppressWarnings("unused")
	protected String retrieveResources() {
		// This Layer has no construction parameters, so there is no description of what to retrieve. Return a key
		// indicating the resources have been successfully retrieved, though there is nothing to retrieve.
		AVList params = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
		if (params == null) {
			String message = Logging.getMessage("nullValue.ConstructionParametersIsNull");
			Logging.warning(message);
			return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
		}

		// This Layer has no OGC Capabilities URL in its construction parameters. Return a key indicating the resources
		// have been successfully retrieved, though there is nothing to retrieve.
		URL url = DataConfigurationUtils.getOGCGetCapabilitiesURL(params);
		if (url == null) {
			String message = Logging.getMessage("nullValue.CapabilitiesURLIsNull");
			Logging.warning(message);
			return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
		}

		// The OGC Capabilities resource is marked as absent. Return null indicating that the retrieval was not
		// successful, and we should try again later.
		if (this.absentResources.isResourceAbsent(RESOURCE_ID_OGC_CAPABILITIES)) return null;

		// Get the service's OGC Capabilities resource from the session cache, or initiate a retrieval to fetch it in
		// a separate thread. SessionCacheUtils.getOrRetrieveSessionCapabilities() returns null if it initiated a
		// retrieval, or if the OGC Capabilities URL is unavailable.
		//
		// Note that we use the URL's String representation as the cache key. We cannot use the URL itself, because
		// the cache invokes the methods Object.hashCode() and Object.equals() on the cache key. URL's implementations
		// of hashCode() and equals() perform blocking IO calls. World Wind does not perform blocking calls during
		// rendering, and this method is likely to be called from the rendering thread.
		WMSCapabilities caps = null;
		// if (this.isNetworkRetrievalEnabled()) caps = SessionCacheUtils.getOrRetrieveSessionCapabilities(url, WorldWind.getSessionCache(), url.toString(),
		// this.absentResources,
		// RESOURCE_ID_OGC_CAPABILITIES, null, null);
		// else caps = SessionCacheUtils.getSessionCapabilities(WorldWind.getSessionCache(), url.toString(), url.toString());

		// The OGC Capabilities resource retrieval is either currently running in another thread, or has failed. In
		// either case, return null indicating that that the retrieval was not successful, and we should try again
		// later.
		if (caps == null) return null;

		// We have sucessfully retrieved this Layer's OGC Capabilities resource. Intialize this Layer using the
		// Capabilities document, and return a key indicating the retrieval has succeeded.
		this.initFromOGCCapabilitiesResource(caps, params);

		return AVKey.RETRIEVAL_STATE_SUCCESSFUL;
	}

	/**
	 * Initializes this Layer's expiry time property from the specified WMS Capabilities document and parameter list
	 * describing the WMS layer names associated with this Layer. This method is thread safe; it synchronizes changes to
	 * this Layer by wrapping the appropriate method calls in {@link SwingUtilities#invokeLater(Runnable)}.
	 * 
	 * @param caps
	 *            the WMS Capabilities document retrieved from this Layer's WMS server.
	 * @param params
	 *            the parameter list describing the WMS layer names associated with this Layer.
	 * @throws IllegalArgumentException
	 *             if either the Capabilities or the parameter list is null.
	 */
	protected void initFromOGCCapabilitiesResource(WMSCapabilities caps, AVList params) {
		if (caps == null) {
			String message = Logging.getMessage("nullValue.CapabilitiesIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) {
			String message = Logging.getMessage("nullValue.ParametersIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		String[] names = DataConfigurationUtils.getOGCLayerNames(params);
		if (names == null || names.length == 0) return;

		final Long expiryTime = caps.getLayerLatestLastUpdateTime(caps, names);
		if (expiryTime == null) return;

		// Synchronize changes to this Layer with the Event Dispatch Thread.
		// TODO: BBB - rewrite swing utilities
		// SwingUtilities.invokeLater(new Runnable()
		// {public void run() {
		BasicTiledImageLayer.this.setExpiryTime(expiryTime);
		BasicTiledImageLayer.this.firePropertyChange(AVKey.LAYER, null, BasicTiledImageLayer.this);
		// } });
	}

	/**
	 * Returns a boolean value indicating if this Layer should retrieve any non-tile resources, either online or in the
	 * local filesystem, and initialize itself using those resources.
	 * 
	 * @return <code>true</code> if this Layer should retrieve any non-tile resources, and <code>false</code> otherwise.
	 */
	protected boolean isRetrieveResources() {
		AVList params = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
		if (params == null) return false;

		Boolean b = (Boolean) params.getValue(AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE);
		return b != null && b;
	}

	/**
	 * Starts retrieving non-tile resources associated with this Layer in a non-rendering thread. By default, this
	 * schedules a task immediately to retrieve those resources, and then every 10 seconds thereafter until the
	 * retrieval succeeds.
	 * <p/>
	 * If this method is invoked while any non-tile resource tasks are running or pending, this cancels any pending tasks (but allows any running tasks to
	 * finish).
	 */
	protected void startResourceRetrieval() {
		// Configure an AbsentResourceList with the specified number of max retrieval attempts, and the smallest
		// possible min attempt interval. We specify a small attempt interval because the resource retrieval service
		// itself schedules the attempts at our specified interval. We therefore want to bypass AbsentResourceLists's
		// internal timing scheme.
		this.absentResources = new AbsentResourceList(DEFAULT_MAX_RESOURCE_ATTEMPTS, 1);

		// Stop any pending resource retrieval tasks.
		if (this.resourceRetrievalService != null) this.resourceRetrievalService.shutdown();

		// Schedule a task to retrieve non-tile resources immediately, then at intervals thereafter.
		Runnable task = this.createResourceRetrievalTask();
		String taskName = Logging.getMessage("layers.TiledImageLayer.ResourceRetrieverThreadName", this.getName());
		this.resourceRetrievalService = DataConfigurationUtils.createResourceRetrievalService(taskName);
		this.resourceRetrievalService.scheduleAtFixedRate(task, 0, DEFAULT_MIN_RESOURCE_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}

	/** Cancels any pending non-tile resource retrieval tasks, and allows any running tasks to finish. */
	protected void stopResourceRetrieval() {
		if (this.resourceRetrievalService != null) {
			this.resourceRetrievalService.shutdownNow();
			this.resourceRetrievalService = null;
		}
	}

	// **************************************************************//
	// ********************** Retrieval ***************************//
	// **************************************************************//

	protected RequestTask createRequestTask(DrawContext dc, GpuTextureTile tile) {
		double priority = this.computeTilePriority(dc, tile);
		tile.setPriority(priority);
		return new RequestTask(tile, this, priority);
	}

	protected static class RequestTask implements Runnable, Comparable<RequestTask> {
		protected final BasicTiledImageLayer layer;
		protected final GpuTextureTile tile;
		protected double priority;

		protected RequestTask(GpuTextureTile tile, BasicTiledImageLayer layer, double priority) {
			this.layer = layer;
			this.tile = tile;
			this.priority = priority;
		}

		public void run() {
			if (Thread.currentThread().isInterrupted()) return; // the task was cancelled because it's a duplicate or for some other reason

			this.layer.loadTile(this.tile);
		}

		/**
		 * @param that
		 *            the task to compare
		 * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
		 * @throws IllegalArgumentException
		 *             if <code>that</code> is null
		 */
		public int compareTo(RequestTask that) {
			if (that == null) return -1;

			return this.priority < that.priority ? -1 : (this.priority > that.priority ? 1 : 0);
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof RequestTask)) return false;

			RequestTask that = (RequestTask) o;
			return this.tile.equals(that.tile);
		}

		public int hashCode() {
			return (tile != null ? tile.hashCode() : 0);
		}

		public String toString() {
			return this.tile.toString();
		}
	}


	protected void addTileToCache(GpuTextureTile tile) {
		GpuTextureTile.getMemoryCache().put(tile.getTileKey(), tile);
	}

	protected void requestTile(DrawContext dc, GpuTextureTile tile) {
		Runnable task = this.createRequestTask(dc, tile);
		if (task == null) {
			String msg = Logging.getMessage("nullValue.TaskIsNull");
			Logging.warning(msg);
			return;
		}

		this.requestQ.add(task);
	}

	/**
	 * Compute the priority of loading this tile, based on distance from the eye to the tile's center point. Tiles
	 * closer to the eye have higher priority than those far from the eye.
	 *
	 * @param dc
	 *            current draw context.
	 * @param tile
	 *            tile for which to compute the priority.
	 * @return tile priority. A lower number indicates higher priority.
	 */
	protected double computeTilePriority(DrawContext dc, GpuTextureTile tile) {
		// Tile priority is ordered from low (most priority) to high (least priority). Assign the tile priority based
		// on square distance form the eye point. Since we don't care about the actual distance this enables us to
		// avoid a square root computation. Tiles further from the eye point are loaded last.
		return dc.getView().getEyePoint().distanceToSquared3(tile.getExtent().getCenter());
	}

	protected void forceTextureLoad(GpuTextureTile tile) {
		this.loadTile(tile);
	}

	/**
	 * Load a tile. If the tile exists in the file cache, it will be loaded from the file cache. If not, it will be
	 * requested from the network.
	 *
	 * @param tile
	 *            tile to load.
	 */
	protected void loadTile(GpuTextureTile tile) {
		URL textureURL = this.getDataFileStore().findFile(tile.getPath(), false);
		if (textureURL != null) {
			this.loadTexture(tile, textureURL);
		} else {
			this.retrieveTexture(tile, this.createDownloadPostProcessor(tile));
		}
	}

	/**
	 * Load a tile from the file cache.
	 *
	 * @param tile
	 *            tile to load.
	 * @param textureURL
	 *            local URL to the cached resource.
	 */
	protected boolean loadTexture(GpuTextureTile tile, URL textureURL) {
		GpuTextureData textureData;

		synchronized (this.fileLock) {
			textureData = this.createTextureData(textureURL, this.getTextureFormat());
		}

		if (textureData != null) {
			tile.setTextureData(textureData);

			// The tile's size has changed, so update its size in the memory cache.
			if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
				addTileToCache(tile);

			// Mark the tile as not absent to ensure that it is used, and cause any World Windows containing this layer
			// to repaint themselves.
			this.levels.unmarkResourceAbsent(tile);
			this.firePropertyChange(AVKey.LAYER, null, this);
			return true;
		} else {
			// Assume that something is wrong with the file and delete it.
			this.getDataFileStore().removeFile(textureURL);
			String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
			Logging.info(message);
			return false;
		}
	}

	protected GpuTextureData createTextureData(URL textureURL, String textureFormat) {
		return GpuTextureData.createTextureData(textureURL, textureURL.toString(), textureFormat, isUseMipMaps());
	}

	/**
	 * Create a post processor for a tile retrieval task.
	 *
	 * @param tile
	 *            tile to create a post processor for.
	 * @return new post processor.
	 */
	protected DownloadPostProcessor createDownloadPostProcessor(GpuTextureTile tile) {
		return new DownloadPostProcessor(tile, this, this.getDataFileStore());
	}

	/**
	 * Retrieve a tile from the network. This method initiates an asynchronous retrieval task and then returns.
	 *
	 * @param tile
	 *            tile to download.
	 * @param postProcessor
	 *            post processor to handle the retrieval.
	 */
	protected void retrieveTexture(GpuTextureTile tile, DownloadPostProcessor postProcessor) {
		this.retrieveRemoteTexture(tile, postProcessor);
	}

	protected void retrieveRemoteTexture(GpuTextureTile tile, DownloadPostProcessor postProcessor) {
		if (!this.isNetworkRetrievalEnabled()) {
			this.getLevels().markResourceAbsent(tile);
			return;
		}

		if (!WorldWind.getRetrievalService().isAvailable()) return;

		URL url;
		try {
			url = tile.getResourceURL();
		} catch (MalformedURLException e) {
			Logging.error(Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
			return;
		}

		if (WorldWind.getNetworkStatus().isHostUnavailable(url)) {
			this.getLevels().markResourceAbsent(tile);
			return;
		}

		Retriever retriever = URLRetriever.createRetriever(url, postProcessor);
		if (retriever == null) {
			Logging.error(Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", url.toString()));
			return;
		}
		retriever.setValue(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers

		// Apply any overridden timeouts.
		Integer connectTimeout = AVListImpl.getIntegerValue(this, AVKey.URL_CONNECT_TIMEOUT);
		if (connectTimeout != null && connectTimeout > 0) retriever.setConnectTimeout(connectTimeout);

		Integer readTimeout = AVListImpl.getIntegerValue(this, AVKey.URL_READ_TIMEOUT);
		if (readTimeout != null && readTimeout > 0) retriever.setReadTimeout(readTimeout);

		Integer staleRequestLimit = AVListImpl.getIntegerValue(this, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
		if (staleRequestLimit != null && staleRequestLimit > 0) retriever.setStaleRequestLimit(staleRequestLimit);

		WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
	}

	protected static class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
		protected GpuTextureTile tile;
		protected BasicTiledImageLayer layer;
		protected FileStore fileStore;

		public DownloadPostProcessor(GpuTextureTile tile, BasicTiledImageLayer layer, FileStore fileStore) {
			super(layer);

			this.tile = tile;
			this.layer = layer;
			this.fileStore = fileStore;
		}

		@Override
		protected void markResourceAbsent() {
			this.layer.getLevels().markResourceAbsent(this.tile);
		}

		@Override
		protected Object getFileLock() {
			return this.layer.fileLock;
		}

		protected FileStore getFileStore()
		{
			return this.fileStore != null ? this.fileStore : this.layer.getDataFileStore();
		}

		@Override
		protected File doGetOutputFile() {
			return layer.getDataFileStore().newFile(this.tile.getPath());
		}

		@Override
		protected ByteBuffer handleSuccessfulRetrieval() {
			ByteBuffer buffer = super.handleSuccessfulRetrieval();

			if (buffer != null) {
				// We've successfully cached data. Check if there's a configuration file for this layer, create one
				// if there's not.
				//TODO write configurationFile
				this.layer.writeConfigurationFile(this.getFileStore());

				// Fire a property change to denote that the layer's backing data has changed.
				this.layer.firePropertyChange(AVKey.LAYER, null, this);
			}

			return buffer;
		}

		@Override
		protected ByteBuffer handleTextContent() throws IOException {
			this.markResourceAbsent();

			return super.handleTextContent();
		}
	}

	/**
	 * Returns a Runnable task which retrieves any non-tile resources associated with a specified Layer in it's run
	 * method. This task is used by the Layer to schedule periodic resource checks. If the task's run method throws an
	 * Exception, it will no longer be scheduled for execution. By default, this returns a reference to a new {@link ResourceRetrievalTask}.
	 * 
	 * @return Runnable who's run method retrieves non-tile resources.
	 */
	protected Runnable createResourceRetrievalTask() {
		return new ResourceRetrievalTask(this);
	}

	/** ResourceRetrievalTask retrieves any non-tile resources associated with this Layer in it's run method. */
	protected static class ResourceRetrievalTask implements Runnable {
		protected BasicTiledImageLayer layer;

		/**
		 * Constructs a new ResourceRetrievalTask, but otherwise does nothing.
		 * 
		 * @param layer
		 *            the BasicTiledImageLayer who's non-tile resources should be retrieved in the run method.
		 * @throws IllegalArgumentException
		 *             if the layer is null.
		 */
		public ResourceRetrievalTask(BasicTiledImageLayer layer) {
			if (layer == null) {
				String message = Logging.getMessage("nullValue.LayerIsNull");
				Logging.error(message);
				throw new IllegalArgumentException(message);
			}

			this.layer = layer;
		}

		/**
		 * Returns the layer who's non-tile resources are retrieved by this ResourceRetrievalTask
		 * 
		 * @return the layer who's non-tile resources are retireved.
		 */
		public BasicTiledImageLayer getLayer() {
			return this.layer;
		}

		/**
		 * Retrieves any non-tile resources associated with the specified Layer, and cancels any pending retrieval tasks
		 * if the retrieval succeeds, or if an exception is thrown during retrieval.
		 */
		public void run() {
			try {
				if (this.layer.isEnabled()) this.retrieveResources();
			} catch (Throwable t) {
				this.handleUncaughtException(t);
			}
		}

		/**
		 * Invokes {@link BasicTiledImageLayer#retrieveResources()}, and cancels any pending retrieval tasks if the call
		 * returns {@link gov.nasa.worldwind.avlist.AVKey#RETRIEVAL_STATE_SUCCESSFUL}.
		 */
		protected void retrieveResources() {
			String state = this.layer.retrieveResources();

			if (state != null && state.equals(AVKey.RETRIEVAL_STATE_SUCCESSFUL)) {
				this.layer.stopResourceRetrieval();
			}
		}

		/**
		 * Logs a message describing the uncaught exception thrown during a call to run, and cancels any pending
		 * retrieval tasks.
		 * 
		 * @param t
		 *            the uncaught exception.
		 */
		protected void handleUncaughtException(Throwable t) {
			String message = Logging.getMessage("layers.TiledImageLayer.ExceptionRetrievingResources", this.layer.getName());
			Logging.info(message, t);

			this.layer.stopResourceRetrieval();
		}
	}

	// **************************************************************//
	// ******************** Configuration *************************//
	// **************************************************************//

	protected void writeConfigurationFile(FileStore fileStore) {
		// TODO: configurable max attempts for creating a configuration file.

		try {
			AVList configParams = this.getConfigurationParams(null);
			this.writeConfigurationParams(fileStore, configParams);
		} catch (Exception e) {
			String message = Logging.getMessage("generic.ExceptionAttemptingToWriteConfigurationFile");
			Logging.error(message, e);
		}
	}

	protected void writeConfigurationParams(FileStore fileStore, AVList params) {
		// Determine what the configuration file name should be based on the configuration parameters. Assume an XML
		// configuration document type, and append the XML file suffix.
		String fileName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
		if (fileName == null) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new WWRuntimeException(message);
		}

		// Check if this component needs to write a configuration file. This happens outside of the synchronized block
		// to improve multithreaded performance for the common case: the configuration file already exists, this just
		// need to check that it's there and return. If the file exists but is expired, do not remove it - this
		// removes the file inside the synchronized block below.
		if (!this.needsConfigurationFile(fileStore, fileName, params, false)) return;

		synchronized (this.fileLock) {
			// Check again if the component needs to write a configuration file, potentially removing any existing file
			// which has expired. This additional check is necessary because the file could have been created by
			// another thread while we were waiting for the lock.
			if (!this.needsConfigurationFile(fileStore, fileName, params, true)) return;

			this.doWriteConfigurationParams(fileStore, fileName, params);
		}
	}

	protected void doWriteConfigurationParams(FileStore fileStore, String fileName, AVList params) {
		if(WorldWindowImpl.DEBUG)
			Logging.verbose(getName() + " writing layer configuration file");

		java.io.File file = fileStore.newFile(fileName);
		if (file == null) {
			String message = Logging.getMessage("generic.CannotCreateFile", fileName);
			Logging.error(message);
			throw new WWRuntimeException(message);
		}

		Document doc = this.createConfigurationDocument(params);
		WWXML.saveDocumentToFile(doc, file.getPath());

		String message = Logging.getMessage("generic.ConfigurationFileCreated", fileName);
		Logging.info(message);
	}

	protected boolean needsConfigurationFile(FileStore fileStore, String fileName, AVList params, boolean removeIfExpired) {
		long expiryTime = this.getExpiryTime();
		if (expiryTime <= 0) expiryTime = AVListImpl.getLongValue(params, AVKey.EXPIRY_TIME, 0L);

		return !DataConfigurationUtils.hasDataConfigFile(fileStore, fileName, removeIfExpired, expiryTime);
	}

	protected AVList getConfigurationParams(AVList params) {
		if (params == null) params = new AVListImpl();

		// Gather all the construction parameters if they are available.
		AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
		if (constructionParams != null) params.setValues(constructionParams);

		// Gather any missing LevelSet parameters from the LevelSet itself.
		DataConfigurationUtils.getLevelSetConfigParams(this.getLevels(), params);

		return params;
	}

	protected Document createConfigurationDocument(AVList params) {
		return createTiledImageLayerConfigDocument(params);
	}

}
