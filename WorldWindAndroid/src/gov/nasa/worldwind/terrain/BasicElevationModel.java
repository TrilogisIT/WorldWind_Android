/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.event.BulkRetrievalListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.retrieve.AbstractRetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.BulkRetrievable;
import gov.nasa.worldwind.retrieve.BulkRetrievalThread;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.RetrieverFactory;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileKey;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.xml.xpath.XPath;
import org.w3c.dom.Element;

// Implementation notes, not for API doc:
//
// Implements an elevation model based on a quad tree of elevation tiles. Meant to be subclassed by very specific
// classes, e.g. Earth/SRTM. A Descriptor passed in at construction gives the configuration parameters. Eventually
// Descriptor will be replaced by an XML configuration document.
//
// A "tile" corresponds to one tile of the data set, which has a corresponding unique row/column address in the data
// set. An inner class implements Tile. An inner class also implements TileKey, which is used to address the
// corresponding Tile in the memory cache.

// Clients of this class get elevations from it by first getting an Elevations object for a specific Sector, then
// querying that object for the elevation at individual lat/lon positions. The Elevations object captures information
// that is used to compute elevations. See in-line comments for a description.
//
// When an elevation tile is needed but is not in memory, a task is threaded off to find it. If it's in the file cache
// then it's loaded by the task into the memory cache. If it's not in the file cache then a retrieval is initiated by
// the task. The disk is never accessed during a call to getElevations(sector, resolution) because that method is
// likely being called when a frame is being rendered. The details of all this are in-line below.

/**
 * @author dcollins
 * @version $Id: BasicElevationModel.java 835 2012-10-09 16:06:28Z tgaskins $
 */
public class BasicElevationModel extends AbstractElevationModel implements BulkRetrievable {
	protected final LevelSet levels;
	protected final double minElevation;
	protected final double maxElevation;
	protected String elevationDataType = AVKey.INT16;
	protected String elevationDataByteOrder = AVKey.LITTLE_ENDIAN;
	protected double detailHint = 0.0;
	protected final Object fileLock = new Object();
	protected MemoryCache memoryCache;
	protected int extremesLevel = -1;
	protected short[] extremes = null;
	protected MemoryCache extremesLookupCache;

	public BasicElevationModel(AVList params) {
		if (params == null) {
			String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		String s = params.getStringValue(AVKey.BYTE_ORDER);
		if (s != null) this.setByteOrder(s);

		Double d = (Double) params.getValue(AVKey.DETAIL_HINT);
		if (d != null) this.setDetailHint(d);

		s = params.getStringValue(AVKey.DISPLAY_NAME);
		if (s != null) this.setName(s);

		d = (Double) params.getValue(AVKey.ELEVATION_MIN);
		this.minElevation = d != null ? d : 0;

		d = (Double) params.getValue(AVKey.ELEVATION_MAX);
		this.maxElevation = d != null ? d : 0;

		Long lo = (Long) params.getValue(AVKey.EXPIRY_TIME);
		if (lo != null) params.setValue(AVKey.EXPIRY_TIME, lo);

		d = (Double) params.getValue(AVKey.MISSING_DATA_SIGNAL);
		if (d != null) this.setMissingDataSignal(d);

		d = (Double) params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
		if (d != null) this.setMissingDataReplacement(d);

		Boolean b = (Boolean) params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED);
		if (b != null) this.setNetworkRetrievalEnabled(b);

		s = params.getStringValue(AVKey.DATA_TYPE);
		if (s != null) this.setElevationDataType(s);

		s = params.getStringValue(AVKey.ELEVATION_EXTREMES_FILE);
		if (s != null) this.loadExtremeElevations(s);

		// Set some fallback values if not already set.
		setFallbacks(params);

		this.levels = new LevelSet(params);
		this.memoryCache = this.createMemoryCache(ElevationTile.class.getName());

		this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
		super.propertyChange(propertyChangeEvent);
	}

	public BasicElevationModel(Element domElement, AVList params) {
		this(getBasicElevationModelConfigParams(domElement, params));
	}

	@Override
	public void dispose() {
		WorldWind.removePropertyChangeListener(WorldWind.SHUTDOWN_EVENT, this);
	}

	@Override
	public Object setValue(String key, Object value) {
		// Offer it to the level set
		if (this.getLevels() != null) this.getLevels().setValue(key, value);

		return super.setValue(key, value);
	}

	@Override
	public Object getValue(String key) {
		Object value = super.getValue(key);

		return value != null ? value : this.getLevels().getValue(key); // see if the level set has it
	}

	protected static void setFallbacks(AVList params) {
		if (params.getValue(AVKey.TILE_WIDTH) == null) params.setValue(AVKey.TILE_WIDTH, 150);

		if (params.getValue(AVKey.TILE_HEIGHT) == null) params.setValue(AVKey.TILE_HEIGHT, 150);

		if (params.getValue(AVKey.FORMAT_SUFFIX) == null) params.setValue(AVKey.FORMAT_SUFFIX, ".bil");

		if (params.getValue(AVKey.NUM_LEVELS) == null) params.setValue(AVKey.NUM_LEVELS, 2);

		if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null) params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
	}

	protected MemoryCache getMemoryCache() {
		return memoryCache;
	}

	protected MemoryCache createMemoryCache(String cacheName) {
		if (WorldWind.getMemoryCacheSet().contains(cacheName)) {
			return WorldWind.getMemoryCache(cacheName);
		} else {
			long size = Configuration.getLongValue(AVKey.ELEVATION_TILE_CACHE_SIZE, 5000000L);
			MemoryCache mc = new BasicMemoryCache((long) (0.85 * size), size);
			mc.setName("Elevation Tiles");
			WorldWind.getMemoryCacheSet().put(cacheName, mc);
			return mc;
		}
	}

	public LevelSet getLevels() {
		return this.levels;
	}

	protected int getExtremesLevel() {
		return extremesLevel;
	}

	protected short[] getExtremes() {
		return extremes;
	}

	/**
	 * Specifies the time of the elevation models's most recent dataset update, beyond which cached data is invalid. If
	 * greater than zero, the model ignores and eliminates any in-memory or on-disk cached data older than the time
	 * specified, and requests new information from the data source. If zero, the default, the model applies any expiry
	 * times associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired
	 * only when the expiry time is specified with this method and is greater than zero. This method also overwrites the
	 * expiry times of the model's individual levels if the value specified to the method is greater than zero.
	 * 
	 * @param expiryTime
	 *            the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch. The
	 *            default expiry time is zero.
	 * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
	 */
	public void setExpiryTime(long expiryTime) // Override this method to use intrinsic level-specific expiry times
	{
		super.setExpiryTime(expiryTime);

		if (expiryTime > 0) this.levels.setExpiryTime(expiryTime); // remove this in sub-class to use level-specific expiry times
	}

	public double getMaxElevation() {
		return this.maxElevation;
	}

	public double getMinElevation() {
		return this.minElevation;
	}

	public double getBestResolution(Sector sector) {
		if (sector == null) return this.levels.getLastLevel().getTexelSize();

		Level level = this.levels.getLastLevel(sector);
		return level != null ? level.getTexelSize() : Double.MAX_VALUE;
	}

	public double getDetailHint(Sector sector) {
		return this.detailHint;
	}

	public void setDetailHint(double hint) {
		this.detailHint = hint;
	}

	public void setElevationDataType(String dataType) {
		if (dataType == null) {
			String message = Logging.getMessage("nullValue.DataTypeIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.elevationDataType = dataType;
	}

	public void setByteOrder(String byteOrder) {
		if (byteOrder == null) {
			String message = Logging.getMessage("nullValue.ByteOrderIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.elevationDataByteOrder = byteOrder;
	}

	public int intersects(Sector sector) {
		if (this.levels.getSector().contains(sector)) return 0;

		return this.levels.getSector().intersects(sector) ? 1 : -1;
	}

	public boolean contains(Angle latitude, Angle longitude) {
		if (latitude == null || longitude == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return this.levels.getSector().contains(latitude, longitude);
	}

	// **************************************************************//
	// ******************** Elevation Tile Management *************//
	// **************************************************************//

	// Create the tile corresponding to a specified key.

	protected ElevationTile createTile(TileKey key) {
		Level level = this.levels.getLevel(key.getLevelNumber());

		// Compute the tile's SW lat/lon based on its row/col in the level's data set.
		Angle dLat = level.getTileDelta().latitude;
		Angle dLon = level.getTileDelta().longitude;
		Angle latOrigin = this.levels.getTileOrigin().latitude;
		Angle lonOrigin = this.levels.getTileOrigin().longitude;

		Angle minLatitude = ElevationTile.computeRowLatitude(key.getRow(), dLat, latOrigin);
		Angle minLongitude = ElevationTile.computeColumnLongitude(key.getColumn(), dLon, lonOrigin);

		Sector tileSector = new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));

		return new ElevationTile(tileSector, level, key.getRow(), key.getColumn());
	}

	// Thread off a task to determine whether the file is local or remote and then retrieve it either from the file
	// cache or a remote server.

	protected void requestTile(TileKey key) {
		if (WorldWind.getTaskService().isFull()) return;

		if (this.getLevels().isResourceAbsent(key)) return;

		RequestTask request = new RequestTask(key, this);
		WorldWind.getTaskService().runTask(request);
	}

	protected static class RequestTask implements Runnable {
		protected final BasicElevationModel elevationModel;
		protected final TileKey tileKey;

		protected RequestTask(TileKey tileKey, BasicElevationModel elevationModel) {
			this.elevationModel = elevationModel;
			this.tileKey = tileKey;
		}

		public final void run() {
			if (Thread.currentThread().isInterrupted()) return; // the task was cancelled because it's a duplicate or for some other reason

			try {
				// check to ensure load is still needed
				if (this.elevationModel.areElevationsInMemory(this.tileKey)) return;

				ElevationTile tile = this.elevationModel.createTile(this.tileKey);
				final URL url = this.elevationModel.getDataFileStore().findFile(tile.getPath(), false);
				if (url != null && !this.elevationModel.isFileExpired(tile, url, this.elevationModel.getDataFileStore())) {
					if (this.elevationModel.loadElevations(tile, url)) {
						this.elevationModel.levels.unmarkResourceAbsent(tile);
						this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, tile);
						return;
					} else {
						// Assume that something is wrong with the file and delete it.
						this.elevationModel.getDataFileStore().removeFile(url);
						this.elevationModel.levels.markResourceAbsent(tile);
						String message = Logging.getMessage("generic.DeletedCorruptDataFile", url);
						Logging.info(message);
					}
				}

				this.elevationModel.downloadElevations(tile);
			} catch (IOException e) {
				String msg = Logging.getMessage("ElevationModel.ExceptionRequestingElevations", this.tileKey.toString());
				Logging.verbose(msg, e);
			}
		}

		public final boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final RequestTask that = (RequestTask) o;

			// noinspection RedundantIfStatement
			if (this.tileKey != null ? !this.tileKey.equals(that.tileKey) : that.tileKey != null) return false;

			return true;
		}

		public final int hashCode() {
			return (this.tileKey != null ? this.tileKey.hashCode() : 0);
		}

		public final String toString() {
			return this.tileKey.toString();
		}
	}

	protected boolean isFileExpired(Tile tile, java.net.URL fileURL, FileStore fileStore) {
		if (!WWIO.isFileOutOfDate(fileURL, tile.getLevel().getExpiryTime())) return false;

		// The file has expired. Delete it.
		fileStore.removeFile(fileURL);
		String message = Logging.getMessage("generic.DataFileExpired", fileURL);
		Logging.verbose(message);
		return true;
	}

	// Reads a tile's elevations from the file cache and adds the tile to the memory cache.

	protected boolean loadElevations(ElevationTile tile, java.net.URL url) throws IOException {
		short[] elevations = this.readElevations(url);
		if (elevations == null || elevations.length == 0) return false;

		tile.setElevations(elevations);
		this.addTileToCache(tile, elevations);

		return true;
	}

	protected void addTileToCache(ElevationTile tile, short[] elevations) {
		this.getMemoryCache().put(tile.getTileKey(), tile, elevations.length * 2);
	}

	protected boolean areElevationsInMemory(TileKey key) {
		// An elevation tile is considered to be in memory if it:
		// * Exists in the memory cache.
		// * Has non-null elevation data.
		// * Has not expired.
		ElevationTile tile = this.getTileFromMemory(key);
		return (tile != null && tile.getElevations() != null && !tile.isElevationsExpired());
	}

	protected ElevationTile getTileFromMemory(TileKey tileKey) {
		return (ElevationTile) this.getMemoryCache().get(tileKey);
	}

	// Read elevations from the file cache. Don't be confused by the use of a URL here: it's used so that files can
	// be read using System.getResource(URL), which will draw the data from a jar file in the classpath.

	protected short[] readElevations(URL url) throws IOException {
		try {
			ByteBuffer byteBuffer;
			synchronized (this.fileLock) {
				byteBuffer = WWIO.readURLContentToBuffer(url);
			}

			// This byte order assignment assumes the WW .BIL format. It will be generalized when the new server
			// setup is integrated.
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
			short[] elevations = new short[shortBuffer.limit()];
			shortBuffer.get(elevations);
			return elevations;
		} catch (java.io.IOException e) {
			Logging.error("ElevationModel.ExceptionReadingElevationFile", url.toString());
			throw e;
		}
	}

	// *** Bulk download ***
	// *** Bulk download ***
	// *** Bulk download ***

	/**
	 * Start a new {@link gov.nasa.worldwind.retrieve.BulkRetrievalThread} that downloads all elevations for a given
	 * sector and resolution to the current World Wind file cache, without downloading imagery already in the cache.
	 * <p/>
	 * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create a downloader that has not been started,
	 * construct a {@link BasicElevationModelBulkDownloader}.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 * 
	 * @param sector
	 *            the sector to download data for.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @param listener
	 *            an optional retrieval listener. May be null.
	 * @return the {@link gov.nasa.worldwind.retrieve.BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does not
	 *         intersect the elevation model bounding sector.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 * @see BasicElevationModelBulkDownloader
	 */
	public BulkRetrievalThread makeLocal(Sector sector, double resolution, BulkRetrievalListener listener) {
		return this.makeLocal(sector, resolution, null, listener);
	}

	/**
	 * Start a new {@link BulkRetrievalThread} that downloads all elevations for a given sector and resolution to a
	 * specified file store, without downloading imagery already in the file store.
	 * <p/>
	 * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create a downloader that has not been started,
	 * construct a {@link BasicElevationModelBulkDownloader}.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 * 
	 * @param sector
	 *            the sector to download data for.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @param fileStore
	 *            the file store in which to place the downloaded elevations. If null the current World Wind file
	 *            cache is used.
	 * @param listener
	 *            an optional retrieval listener. May be null.
	 * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector does
	 *         not intersect the elevation model bounding sector.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 * @see BasicElevationModelBulkDownloader
	 */
	public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore, BulkRetrievalListener listener) {
		Sector targetSector = sector != null ? getLevels().getSector().copy().intersection(sector) : null;
		if (targetSector == null) return null;

		// Args checked in downloader constructor
		BasicElevationModelBulkDownloader thread = new BasicElevationModelBulkDownloader(this, targetSector, resolution, fileStore != null ? fileStore : this.getDataFileStore(),
				listener);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	/**
	 * Get the estimated size in bytes of the elevations not in the World Wind file cache for the given sector and
	 * resolution.
	 * <p/>
	 * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in meters divided by the globe radius.
	 * 
	 * @param sector
	 *            the sector to estimate.
	 * @param resolution
	 *            the target resolution, provided in radians of latitude per texel.
	 * @return the estimated size in bytes of the missing elevations.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 */
	public long getEstimatedMissingDataSize(Sector sector, double resolution) {
		return this.getEstimatedMissingDataSize(sector, resolution, null);
	}

	/**
	 * Get the estimated size in bytes of the elevations not in a specified file store for the given sector and
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
	 * @return the estimated size in bytes of the missing elevations.
	 * @throws IllegalArgumentException
	 *             if the sector is null or the resolution is less than zero.
	 */
	public long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore) {
		Sector targetSector = sector != null ? getLevels().getSector().copy().intersection(sector) : null;
		if (targetSector == null) return 0;

		// Args checked by downloader constructor
		// Need a downloader to compute the missing data size.
		BasicElevationModelBulkDownloader downloader = new BasicElevationModelBulkDownloader(this, targetSector, resolution, fileStore != null ? fileStore
				: this.getDataFileStore(), null);

		return downloader.getEstimatedMissingDataSize();
	}

	// *** Tile download ***
	// *** Tile download ***
	// *** Tile download ***

	protected void downloadElevations(final Tile tile) {
		retrieveElevations(tile, new DownloadPostProcessor(tile, this));
	}

	protected void downloadElevations(final Tile tile, DownloadPostProcessor postProcessor) {
		retrieveElevations(tile, postProcessor);
	}

	protected void retrieveElevations(final Tile tile, DownloadPostProcessor postProcessor) {
		if (this.getValue(AVKey.RETRIEVER_FACTORY_LOCAL) != null) this.retrieveLocalElevations(tile, postProcessor);
		else
		// Assume it's remote, which handles the legacy cases.
		this.retrieveRemoteElevations(tile, postProcessor);
	}

	protected void retrieveLocalElevations(Tile tile, DownloadPostProcessor postProcessor) {
		if (!WorldWind.getLocalRetrievalService().isAvailable()) return;

		RetrieverFactory retrieverFactory = (RetrieverFactory) this.getValue(AVKey.RETRIEVER_FACTORY_LOCAL);
		if (retrieverFactory == null) return;

		AVListImpl avList = new AVListImpl();
		avList.setValue(AVKey.SECTOR, tile.getSector());
		avList.setValue(AVKey.WIDTH, tile.getWidth());
		avList.setValue(AVKey.HEIGHT, tile.getHeight());
		avList.setValue(AVKey.FILE_NAME, tile.getPath());

		Retriever retriever = retrieverFactory.createRetriever(avList, postProcessor);

		WorldWind.getLocalRetrievalService().runRetriever(retriever, tile.getPriority());
	}

	protected void retrieveRemoteElevations(final Tile tile, DownloadPostProcessor postProcessor) {
		if (!this.isNetworkRetrievalEnabled()) {
			this.getLevels().markResourceAbsent(tile);
			return;
		}

		if (!WorldWind.getRetrievalService().isAvailable()) return;

		java.net.URL url = null;
		try {
			url = tile.getResourceURL();
			if (WorldWind.getNetworkStatus().isHostUnavailable(url)) {
				this.getLevels().markResourceAbsent(tile);
				return;
			}
		} catch (java.net.MalformedURLException e) {
			Logging.error(Logging.getMessage("TiledElevationModel.ExceptionCreatingElevationsUrl", url), e);
			return;
		}

		if (postProcessor == null) postProcessor = new DownloadPostProcessor(tile, this);
		URLRetriever retriever = new HTTPRetriever(url, postProcessor);
		retriever.setValue(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy elevation models
		if (WorldWind.getRetrievalService().contains(retriever)) return;

		WorldWind.getRetrievalService().runRetriever(retriever, 0d);
	}

	protected static class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
		protected final Tile tile;
		protected final BasicElevationModel elevationModel;
		protected final FileStore fileStore;

		public DownloadPostProcessor(Tile tile, BasicElevationModel em) {
			this(tile, em, null);
		}

		public DownloadPostProcessor(Tile tile, BasicElevationModel em, FileStore fileStore) {
			// noinspection RedundantCast
			super((AVList) em);

			this.tile = tile;
			this.elevationModel = em;
			this.fileStore = fileStore;
		}

		protected FileStore getFileStore() {
			return this.fileStore != null ? this.fileStore : this.elevationModel.getDataFileStore();
		}

		@Override
		protected boolean overwriteExistingFile() {
			return true;
		}

		@Override
		protected void markResourceAbsent() {
			this.elevationModel.getLevels().markResourceAbsent(this.tile);
		}

		@Override
		protected Object getFileLock() {
			return this.elevationModel.fileLock;
		}

		@Override
		protected File doGetOutputFile() {
			return this.getFileStore().newFile(this.tile.getPath());
		}

		@Override
		protected ByteBuffer handleSuccessfulRetrieval() {
			ByteBuffer buffer = super.handleSuccessfulRetrieval();

			if (buffer != null) {
				// Fire a property change to denote that the model's backing data has changed.
				this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
			}

			return buffer;
		}

		@Override
		protected ByteBuffer handleTextContent() throws IOException {
			this.markResourceAbsent();

			return super.handleTextContent();
		}
	}

	/** Internal class to hold collections of elevation tiles that provide elevations for a specific sector. */
	protected static class Elevations {
		protected final BasicElevationModel elevationModel;
		protected java.util.Set<ElevationTile> tiles;
		protected double extremes[] = null;
		protected final double achievedResolution;

		protected Elevations(BasicElevationModel elevationModel, double achievedResolution) {
			this.elevationModel = elevationModel;
			this.achievedResolution = achievedResolution;
		}

		/**
		 * Look up the elevation at a specified location.
		 * 
		 * @param latitude
		 *            the location's latitude.
		 * @param longitude
		 *            the location's longitude.
		 * @return the elevation at the specified location, or {@code Double.MAX_VALUE} if an elevation could not be
		 *         determined.
		 */
		protected double getElevation(Angle latitude, Angle longitude) {
			if (latitude == null || longitude == null) {
				String msg = Logging.getMessage("nullValue.AngleIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (this.tiles == null) return Double.MAX_VALUE;

			try {
				for (ElevationTile tile : this.tiles) {
					if (tile.getSector().containsDegrees(latitude.degrees, longitude.degrees)) {
						return this.elevationModel.lookupElevation(latitude, longitude, tile);
					}
				}

				// Location is not within this group of tiles, so is outside the coverage of this elevation model.
				return Double.MAX_VALUE;
			} catch (Exception e) {
				// Throwing an exception within what's likely to be the caller's geometry creation loop
				// would be hard to recover from, and a reasonable response to the exception can be done here.
				Logging.error(Logging.getMessage("BasicElevationModel.ExceptionComputingElevation", latitude, longitude), e);

				return Double.MAX_VALUE;
			}
		}

		protected double[] getExtremes(Angle latitude, Angle longitude) {
			if (latitude == null || longitude == null) {
				String msg = Logging.getMessage("nullValue.AngleIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (this.extremes != null) return this.extremes;

			if (this.tiles == null || tiles.size() == 0) return this.elevationModel.getExtremeElevations(latitude, longitude);

			return this.getExtremes();
		}

		/**
		 * Get the extreme values (min/max) of this collection of elevations.
		 * 
		 * @return the extreme elevation values.
		 */
		protected double[] getExtremes() {
			if (this.extremes != null) return this.extremes;

			if (this.tiles == null || tiles.size() == 0) return this.extremes = new double[] { this.elevationModel.getMinElevation(), this.elevationModel.getMaxElevation() };

			this.extremes = WWUtil.defaultMinMix();

			for (ElevationTile tile : this.tiles) {
				short[] elevations = tile.getElevations();

				int len = elevations.length;
				if (len == 0) return null;

				for (int i = 0; i < len; i++) {
					this.elevationModel.determineExtremes(elevations[i], this.extremes);
				}
			}

			return new double[] { this.extremes[0], this.extremes[1] }; // return a defensive copy
		}

		protected double[] getExtremes(Sector sector) {
			if (this.extremes != null) return this.extremes;

			Iterator<ElevationTile> iter = this.tiles.iterator();
			if (!iter.hasNext()) return this.extremes = new double[] { this.elevationModel.getMinElevation(), this.elevationModel.getMaxElevation() };

			this.extremes = WWUtil.defaultMinMix();

			for (ElevationTile tile : this.tiles) {
				tile.getExtremes(sector, this.elevationModel, this.extremes);
			}

			return this.extremes;
		}
	}

	protected void determineExtremes(double value, double extremes[]) {
		if (value == this.missingDataFlag) value = this.missingDataValue;

		if (value < extremes[0]) extremes[0] = value;

		if (value > extremes[1]) extremes[1] = value;
	}

	public double getUnmappedElevation(Angle latitude, Angle longitude) {
		if (latitude == null || longitude == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (!this.contains(latitude, longitude)) return this.getMissingDataSignal();

		Level lastLevel = this.levels.getLastLevel(latitude, longitude);
		final TileKey tileKey = new TileKey(latitude, longitude, this.levels, lastLevel.getLevelNumber());
		ElevationTile tile = this.getTileFromMemory(tileKey);

		if (tile == null) {
			int fallbackRow = tileKey.getRow();
			int fallbackCol = tileKey.getColumn();
			for (int fallbackLevelNum = tileKey.getLevelNumber() - 1; fallbackLevelNum >= 0; fallbackLevelNum--) {
				fallbackRow /= 2;
				fallbackCol /= 2;

				if (this.levels.getLevel(fallbackLevelNum).isEmpty()) // everything lower res is empty
				return this.getExtremeElevations(latitude, longitude)[0];

				TileKey fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol, this.levels.getLevel(fallbackLevelNum).getCacheName());
				tile = this.getTileFromMemory(fallbackKey);
				if (tile != null) break;
			}
		}

		if (tile == null && !this.levels.getFirstLevel().isEmpty()) {
			// Request the level-zero tile since it's not in memory
			Level firstLevel = this.levels.getFirstLevel();
			final TileKey zeroKey = new TileKey(latitude, longitude, this.levels, firstLevel.getLevelNumber());
			this.requestTile(zeroKey);

			// Return the best we know about the location's elevation
			return this.getExtremeElevations(latitude, longitude)[0];
		}

		// Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
		// time has been set for the elevation model. If none has been set, the expiry times of the model's individual
		// levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
		// the overhead of checking expiration of in-memory tiles, a very rarely used feature.
		if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis()) {
			// Normally getUnmappedElevations() does not request elevation tiles, except for first level tiles. However
			// if the tile is already in memory but has expired, we must issue a request to replace the tile data. This
			// will not fetch new tiles into the cache, but rather will force a refresh of the expired tile's resources
			// in the file cache and the memory cache.
			if (tile != null) this.checkElevationExpiration(tile);
		}

		// The containing tile is non-null, so look up the elevation and return.
		return this.lookupElevation(latitude, longitude, tile);
	}

	public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer) {
		return this.getElevations(sector, latlons, targetResolution, buffer, true);
	}

	public double getUnmappedElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer) {
		return this.getElevations(sector, latlons, targetResolution, buffer, false);
	}

	protected double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer, boolean mapMissingData) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (latlons == null) {
			String msg = Logging.getMessage("nullValue.LatLonListIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (buffer == null) {
			String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (buffer.length < latlons.size()) {
			String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Level targetLevel = this.getTargetLevel(sector, targetResolution);
		if (targetLevel == null) return Double.MAX_VALUE;

		Elevations elevations = this.getElevations(sector, this.levels, targetLevel.getLevelNumber());
		if (elevations == null) return Double.MAX_VALUE;

		if (this.intersects(sector) == -1) return Double.MAX_VALUE;

		for (int i = 0; i < latlons.size(); i++) {
			LatLon ll = latlons.get(i);
			if (ll == null) continue;

			double value = elevations.getElevation(ll.latitude, ll.longitude);

			if (this.isTransparentValue(value)) continue;

			// If an elevation at the given location is available, write that elevation to the destination buffer.
			// If an elevation is not available but the location is within the elevation model's coverage, write the
			// elevation models extreme elevation at the location. Do nothing if the location is not within the
			// elevation model's coverage.
			if (value != Double.MAX_VALUE && value != this.getMissingDataSignal()) buffer[i] = value;
			else if (this.contains(ll.latitude, ll.longitude)) {
				if (value == Double.MAX_VALUE) buffer[i] = this.getExtremeElevations(sector)[0];
				else if (mapMissingData && value == this.getMissingDataSignal()) buffer[i] = this.getMissingDataReplacement();
			}
		}

		return elevations.achievedResolution;
	}

	protected Level getTargetLevel(Sector sector, double targetSize) {
		Level lastLevel = this.levels.getLastLevel(sector); // finest resolution available
		if (lastLevel == null) return null;

		if (lastLevel.getTexelSize() >= targetSize) return lastLevel; // can't do any better than this

		for (Level level : this.levels.getLevels()) {
			if (level.getTexelSize() <= targetSize) return !level.isEmpty() ? level : null;

			if (level == lastLevel) break;
		}

		return lastLevel;
	}

	protected double lookupElevation(Angle latitude, Angle longitude, final ElevationTile tile) {
		short[] elevations = tile.getElevations();
		Sector sector = tile.getSector();
		final int tileHeight = tile.getHeight();
		final int tileWidth = tile.getWidth();
		final double sectorDeltaLat = sector.getDeltaLat().radians;
		final double sectorDeltaLon = sector.getDeltaLon().radians;
		final double dLat = sector.maxLatitude.radians - latitude.radians;
		final double dLon = longitude.radians - sector.minLongitude.radians;
		final double sLat = dLat / sectorDeltaLat;
		final double sLon = dLon / sectorDeltaLon;

		int j = (int) ((tileHeight - 1) * sLat);
		int i = (int) ((tileWidth - 1) * sLon);
		int k = j * tileWidth + i;

		double eLeft = elevations[k];
		double eRight = i < (tileWidth - 1) ? elevations[k + 1] : eLeft;

		if (this.getMissingDataSignal() == eLeft || this.getMissingDataSignal() == eRight) return this.getMissingDataSignal();

		double dw = sectorDeltaLon / (tileWidth - 1);
		double dh = sectorDeltaLat / (tileHeight - 1);
		double ssLon = (dLon - i * dw) / dw;
		double ssLat = (dLat - j * dh) / dh;

		double eTop = eLeft + ssLon * (eRight - eLeft);

		if (j < tileHeight - 1 && i < tileWidth - 1) {
			eLeft = elevations[k + tileWidth];
			eRight = elevations[k + tileWidth + 1];

			if (this.getMissingDataSignal() == eLeft || this.getMissingDataSignal() == eRight) return this.getMissingDataSignal();
		}

		double eBot = eLeft + ssLon * (eRight - eLeft);
		return eTop + ssLat * (eBot - eTop);
	}

	public double[] getExtremeElevations(Angle latitude, Angle longitude) {
		if (latitude == null || longitude == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this.extremesLevel < 0 || this.extremes == null) return new double[] { this.getMinElevation(), this.getMaxElevation() };

		try {
			LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
			LatLon origin = this.levels.getTileOrigin();
			final int row = ElevationTile.computeRow(delta.latitude, latitude, origin.latitude);
			final int col = ElevationTile.computeColumn(delta.longitude, longitude, origin.longitude);

			final int nCols = ElevationTile.computeColumn(delta.longitude, Angle.fromDegrees(180), Angle.fromDegrees(-180)) + 1;

			int index = 2 * (row * nCols + col);
			double min = this.extremes[index];
			double max = this.extremes[index + 1];

			if (min == this.getMissingDataSignal()) min = this.getMissingDataReplacement();
			if (max == this.getMissingDataSignal()) max = this.getMissingDataReplacement();

			return new double[] { min, max };
		} catch (Exception e) {
			String message = Logging.getMessage("BasicElevationModel.ExceptionDeterminingExtremes", new LatLon(latitude, longitude));
			Logging.warning(message, e);

			return new double[] { this.getMinElevation(), this.getMaxElevation() };
		}
	}

	public double[] getExtremeElevations(Sector sector) {
		if (sector == null) {
			String message = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		try {
			double[] extremes = (double[]) this.getExtremesLookupCache().get(sector);
			if (extremes != null) return new double[] { extremes[0], extremes[1] }; // return defensive copy

			if (this.extremesLevel < 0 || this.extremes == null) return new double[] { this.getMinElevation(), this.getMaxElevation() };

			// Compute the extremes from the extreme-elevations file, but don't cache them. Only extremes computed from
			// fully resolved elevation tiles are cached. This ensures that the extreme values accurately reflect the
			// extremes of the sector, which is critical for bounding volume creation and thereby performance.
			extremes = this.computeExtremeElevations(sector);
			if (extremes != null) this.getExtremesLookupCache().put(sector, extremes, 16);

			// Return a defensive copy of the array to prevent the caller from modifying the cache contents.
			return extremes != null ? new double[] { extremes[0], extremes[1] } : null;
		} catch (Exception e) {
			String message = Logging.getMessage("BasicElevationModel.ExceptionDeterminingExtremes", sector);
			Logging.warning(message, e);

			return new double[] { this.getMinElevation(), this.getMaxElevation() };
		}
	}

	public void loadExtremeElevations(String extremesFileName) {
		if (extremesFileName == null) {
			String message = Logging.getMessage("nullValue.ExtremeElevationsFileName");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		InputStream is = null;
		try {
			is = this.getClass().getResourceAsStream("/" + extremesFileName);
			if (is == null) {
				// Look directly in the file system
				File file = new File(extremesFileName);
				if (file.exists()) is = new FileInputStream(file);
				else Logging.warning("BasicElevationModel.UnavailableExtremesFile", extremesFileName);
			}

			if (is == null) return;

			// The level the extremes were taken from is encoded as the last element in the file name
			String[] tokens = extremesFileName.substring(0, extremesFileName.lastIndexOf(".")).split("_");
			this.extremesLevel = Integer.parseInt(tokens[tokens.length - 1]);
			if (this.extremesLevel < 0) {
				this.extremes = null;
				Logging.warning(Logging.getMessage("BasicElevationModel.UnavailableExtremesLevel", extremesFileName));
				return;
			}

			ByteBuffer buffer = WWIO.readStreamToBuffer(is, true);

			// ShortBuffer accesses on Android are slow; convert the buffer to an array and use the array to access the
			// individual elevations.
			// Extremes are always saved in JVM byte order
			ShortBuffer shortBuffer = buffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
			this.extremes = new short[shortBuffer.remaining()];
			shortBuffer.get(this.extremes);
		} catch (FileNotFoundException e) {
			Logging.warning(Logging.getMessage("BasicElevationModel.ExceptionReadingExtremeElevations", extremesFileName), e);
			this.extremes = null;
			this.extremesLevel = -1;
			this.extremesLookupCache = null;
		} catch (IOException e) {
			Logging.warning(Logging.getMessage("BasicElevationModel.ExceptionReadingExtremeElevations", extremesFileName), e);
			this.extremes = null;
			this.extremesLevel = -1;
			this.extremesLookupCache = null;
		} finally {
			WWIO.closeStream(is, extremesFileName);

			// Clear the extreme elevations lookup cache.
			if (this.extremesLookupCache != null) this.extremesLookupCache.clear();
		}
	}

	protected double[] computeExtremeElevations(Sector sector) {
		LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
		LatLon origin = this.levels.getTileOrigin();
		final int nwRow = ElevationTile.computeRow(delta.latitude, sector.maxLatitude, origin.latitude);
		final int nwCol = ElevationTile.computeColumn(delta.longitude, sector.minLongitude, origin.longitude);
		final int seRow = ElevationTile.computeRow(delta.latitude, sector.minLatitude, origin.latitude);
		final int seCol = ElevationTile.computeColumn(delta.longitude, sector.maxLongitude, origin.longitude);

		final int nCols = ElevationTile.computeColumn(delta.longitude, Angle.fromDegrees(180), Angle.fromDegrees(-180)) + 1;

		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		double missingDataSignal = this.getMissingDataSignal();
		double missingDataReplacement = this.getMissingDataReplacement();

		for (int col = nwCol; col <= seCol; col++) {
			for (int row = seRow; row <= nwRow; row++) {
				int index = 2 * (row * nCols + col);
				double a = this.extremes[index];
				double b = this.extremes[index + 1];

				if (a == missingDataSignal) a = missingDataReplacement;
				if (b == missingDataSignal) b = missingDataReplacement;

				if (a > max) max = a;
				if (a < min) min = a;
				if (b > max) max = b;
				if (b < min) min = b;
			}
		}

		// Set to model's limits if for some reason a limit wasn't determined
		if (min == Double.MAX_VALUE) min = this.getMinElevation();
		if (max == Double.MAX_VALUE) max = this.getMaxElevation();

		return new double[] { min, max };
	}

	/**
	 * Returns the memory cache used to cache extreme elevation computations, initializing the cache if it doesn't yet
	 * exist. This is an instance level cache: each instance of BasicElevationModel has its own instance of an extreme
	 * elevations lookup cache.
	 * 
	 * @return the memory cache associated with the extreme elevations computations.
	 */
	protected synchronized MemoryCache getExtremesLookupCache() {
		// Note that the extremes lookup cache does not belong to the WorldWind memory cache set, therefore it will not
		// be automatically cleared and disposed when World Wind is shutdown. However, since the extremes lookup cache
		// is a local reference to this elevation model, it will be reclaimed by the JVM garbage collector when this
		// elevation model is reclaimed by the GC.

		if (this.extremesLookupCache == null) {
			// Default cache size holds 1250 min/max pairs. This size was experimentally determined to hold enough
			// value lookups to prevent cache thrashing.
			long size = Configuration.getLongValue(AVKey.ELEVATION_EXTREMES_LOOKUP_CACHE_SIZE, 20000L);
			this.extremesLookupCache = new BasicMemoryCache((long) (0.85 * size), size);
		}

		return this.extremesLookupCache;
	}

	protected static class ElevationTile extends gov.nasa.worldwind.util.Tile implements Cacheable {
		protected short[] elevations; // the elevations themselves
		protected long updateTime = 0;

		protected ElevationTile(Sector sector, Level level, int row, int col) {
			super(sector, level, row, col);
		}

		public short[] getElevations() {
			return this.elevations;
		}

		public void setElevations(short[] elevations) {
			this.elevations = elevations;
			this.updateTime = System.currentTimeMillis();
		}

		public boolean isElevationsExpired() {
			return this.isElevationsExpired(this.getLevel().getExpiryTime());
		}

		public boolean isElevationsExpired(long expiryTime) {
			return this.updateTime > 0 && this.updateTime < expiryTime;
		}

		public int computeElevationIndex(LatLon location) {
			Sector sector = this.getSector();

			final int tileHeight = this.getHeight();
			final int tileWidth = this.getWidth();

			final double sectorDeltaLat = sector.getDeltaLatRadians();
			final double sectorDeltaLon = sector.getDeltaLonRadians();

			final double dLat = sector.maxLatitude.radians - location.latitude.radians;
			final double dLon = location.longitude.radians - sector.minLongitude.radians;

			final double sLat = dLat / sectorDeltaLat;
			final double sLon = dLon / sectorDeltaLon;

			int j = (int) ((tileHeight - 1) * sLat);
			int i = (int) ((tileWidth - 1) * sLon);

			return j * tileWidth + i;
		}

		public double[] getExtremes(Sector sector, BasicElevationModel em, double[] extremes) {
			Sector intersection = this.getSector().copy().intersection(sector);
			if (intersection == null) return extremes;

			LatLon[] corners = intersection.getCorners();
			int[] indices = new int[4];
			for (int i = 0; i < 4; i++) {
				int k = this.computeElevationIndex(corners[i]);
				indices[i] = k < 0 ? 0 : k > this.elevations.length - 1 ? this.elevations.length - 1 : k;
			}

			int sw = indices[0];
			int se = indices[1];
			int nw = indices[3];

			int nCols = se - sw + 1;

			if (extremes == null) extremes = WWUtil.defaultMinMix();

			while (nw <= sw) {
				for (int i = 0; i < nCols; i++) {
					int k = nw + i;
					em.determineExtremes(this.elevations[k], extremes);
				}

				nw += this.getWidth();
			}

			return extremes;
		}
	}

	protected Elevations getElevations(Sector requestedSector, LevelSet levelSet, int targetLevelNumber) {
		// Compute the intersection of the requested sector with the LevelSet's sector.
		// The intersection will be used to determine which Tiles in the LevelSet are in the requested sector.
		Sector sector = requestedSector.copy();
		sector.intersection(levelSet.getSector());

		Level targetLevel = levelSet.getLevel(targetLevelNumber);
		LatLon delta = targetLevel.getTileDelta();
		LatLon origin = levelSet.getTileOrigin();
		final int nwRow = Tile.computeRow(delta.latitude, sector.maxLatitude, origin.latitude);
		final int nwCol = Tile.computeColumn(delta.longitude, sector.minLongitude, origin.longitude);
		final int seRow = Tile.computeRow(delta.latitude, sector.minLatitude, origin.latitude);
		final int seCol = Tile.computeColumn(delta.longitude, sector.maxLongitude, origin.longitude);

		java.util.TreeSet<ElevationTile> tiles = new java.util.TreeSet<ElevationTile>(new Comparator<ElevationTile>() {
			public int compare(ElevationTile t1, ElevationTile t2) {
				if (t2.getLevelNumber() == t1.getLevelNumber() && t2.getRow() == t1.getRow() && t2.getColumn() == t1.getColumn()) return 0;

				// Higher-res levels compare lower than lower-res
				return t1.getLevelNumber() > t2.getLevelNumber() ? -1 : 1;
			}
		});
		ArrayList<TileKey> requested = new ArrayList<TileKey>();

		boolean missingTargetTiles = false;
		boolean missingLevelZeroTiles = false;
		for (int row = seRow; row <= nwRow; row++) {
			for (int col = nwCol; col <= seCol; col++) {
				TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
				ElevationTile tile = this.getTileFromMemory(key);
				if (tile != null) {
					tiles.add(tile);
					continue;
				}

				missingTargetTiles = true;
				this.requestTile(key);

				// Determine the fallback to use. Simultaneously determine a fallback to request that is
				// the next resolution higher than the fallback chosen, if any. This will progressively
				// refine the display until the desired resolution tile arrives.
				TileKey fallbackToRequest = null;
				TileKey fallbackKey;
				int fallbackRow = row;
				int fallbackCol = col;
				for (int fallbackLevelNum = key.getLevelNumber() - 1; fallbackLevelNum >= 0; fallbackLevelNum--) {
					fallbackRow /= 2;
					fallbackCol /= 2;
					fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol, this.levels.getLevel(fallbackLevelNum).getCacheName());

					tile = this.getTileFromMemory(fallbackKey);
					if (tile != null) {
						if (!tiles.contains(tile)) {
							tiles.add(tile);
						}
						break;
					} else {
						if (fallbackLevelNum == 0) missingLevelZeroTiles = true;
						fallbackToRequest = fallbackKey; // keep track of lowest level to request
					}
				}

				if (fallbackToRequest != null) {
					if (!requested.contains(fallbackToRequest)) {
						this.requestTile(fallbackToRequest);
						requested.add(fallbackToRequest); // keep track to avoid overhead of duplicte requests
					}
				}
			}
		}

		Elevations elevations;

		if (missingLevelZeroTiles || tiles.isEmpty()) {
			// Double.MAX_VALUE is a signal for no in-memory tile for a given region of the sector.
			elevations = new Elevations(this, Double.MAX_VALUE);
			elevations.tiles = tiles;
		} else if (missingTargetTiles) {
			// Use the level of the the lowest resolution found to denote the resolution of this elevation set.
			// The list of tiles is sorted first by level, so use the level of the list's last entry.
			elevations = new Elevations(this, tiles.last().getLevel().getTexelSize());
			elevations.tiles = tiles;
		} else {
			elevations = new Elevations(this, tiles.last().getLevel().getTexelSize());

			// Compute the elevation extremes now that the sector is fully resolved
			if (tiles.size() > 0) {
				elevations.tiles = tiles;
				double[] extremes = elevations.getExtremes(requestedSector);
				if (extremes != null) this.getExtremesLookupCache().put(requestedSector, extremes, 16);
			}
		}

		// Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
		// time has been set for the elevation model. If none has been set, the expiry times of the model's individual
		// levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
		// the overhead of checking expiration of in-memory tiles, a very rarely used feature.
		if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis()) this.checkElevationExpiration(tiles);

		return elevations;
	}

	/** {@inheritDoc} */
	public double getElevations(Sector sector, int numLat, int numLon, double targetResolution, double[] buffer) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (numLat <= 0) {
			String msg = Logging.getMessage("generic.HeightIsInvalid", numLat);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (numLon <= 0) {
			String msg = Logging.getMessage("generic.WidthIsInvalid", numLon);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (targetResolution <= 0) {
			String msg = Logging.getMessage("generic.ResolutionIsInvalid", targetResolution);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (buffer == null) {
			String msg = Logging.getMessage("nullValue.BufferIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (buffer.length < numLat * numLon) {
			String msg = Logging.getMessage("generic.BufferInvalidLength", buffer.length);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this.intersects(sector) == -1) return Double.MAX_VALUE;

		Level targetLevel = this.getTargetLevel(sector, targetResolution);
		if (targetLevel == null) return Double.MAX_VALUE;

		Elevations tileSet = this.getElevations(sector, this.getLevels(), targetLevel.getLevelNumber());
		if (tileSet == null) return Double.MAX_VALUE;

		double minLat = sector.minLatitude.radians;
		double maxLat = sector.maxLatitude.radians;
		double minLon = sector.minLongitude.radians;
		double maxLon = sector.maxLongitude.radians;
		double deltaLat = sector.getDeltaLatRadians() / (numLat > 1 ? numLat - 1 : 1);
		double deltaLon = sector.getDeltaLonRadians() / (numLon > 1 ? numLon - 1 : 1);

		double lat = minLat;
		double lon = minLon;
		int index = 0;

		Angle latitude = new Angle();
		Angle longitude = new Angle();

		for (int j = 0; j < numLat; j++) {
			// Explicitly set the first and last row to minLat and maxLat, respectively, rather than using the
			// accumulated lat value. We do this to ensure that the Cartesian points of adjacent sectors are a
			// perfect match.
			if (j == 0) lat = minLat;
			else if (j == numLat - 1) lat = maxLat;
			else lat += deltaLat;

			for (int i = 0; i < numLon; i++) {
				// Explicitly set the first and last column to minLon and maxLon, respectively, rather than using the
				// accumulated lon value. We do this to ensure that the Cartesian points of adjacent sectors are a
				// perfect match.
				if (i == 0) lon = minLon;
				else if (i == numLon - 1) lon = maxLon;
				else lon += deltaLon;

				latitude.setRadians(lat);
				longitude.setRadians(lon);

				double value = tileSet.getElevation(latitude, longitude);
				if (value != Double.MAX_VALUE) buffer[index++] = value;
			}
		}

		return tileSet.achievedResolution;
	}

	protected void checkElevationExpiration(ElevationTile tile) {
		if (tile.isElevationsExpired()) this.requestTile(tile.getTileKey());
	}

	protected void checkElevationExpiration(Iterable<? extends ElevationTile> tiles) {
		for (ElevationTile tile : tiles) {
			if (tile.isElevationsExpired()) this.requestTile(tile.getTileKey());
		}
	}

	public ByteBuffer generateExtremeElevations(int levelNumber) {
		return null;
	}

	// **************************************************************//
	// ******************** Configuration *************************//
	// **************************************************************//

	/**
	 * Parses BasicElevationModel parameters from a specified DOM document. This writes output as key-value pairs to
	 * params. If a parameter from the XML document already exists in params, that parameter is ignored. Supported key
	 * and parameter names are:
	 * <table>
	 * <th>
	 * <td>Parameter</td>
	 * <td>Element Path</td>
	 * <td>Type</td></th>
	 * <tr>
	 * <td>{@link AVKey#SERVICE_NAME}</td>
	 * <td>Service/@serviceName</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#IMAGE_FORMAT}</td>
	 * <td>ImageFormat</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#AVAILABLE_IMAGE_FORMATS}</td>
	 * <td>AvailableImageFormats/ImageFormat</td>
	 * <td>String array</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#DATA_TYPE}</td>
	 * <td>DataType/@type</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#BYTE_ORDER}</td>
	 * <td>DataType/@byteOrder</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#ELEVATION_EXTREMES_FILE}</td>
	 * <td>ExtremeElevations/FileName</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#ELEVATION_MAX}</td>
	 * <td>ExtremeElevations/@max</td>
	 * <td>Double</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#ELEVATION_MIN}</td>
	 * <td>ExtremeElevations/@min</td>
	 * <td>Double</td>
	 * </tr>
	 * </table>
	 * This also parses common
	 * elevation model and LevelSet configuration parameters by invoking
	 * {@link gov.nasa.worldwind.terrain.AbstractElevationModel#getElevationModelConfigParams(org.w3c.dom.Element, gov.nasa.worldwind.avlist.AVList)} and
	 * {@link gov.nasa.worldwind.util.DataConfigurationUtils#getLevelSetConfigParams(org.w3c.dom.Element, gov.nasa.worldwind.avlist.AVList)}.
	 * 
	 * @param domElement
	 *            the XML document root to parse for BasicElevationModel configuration parameters.
	 * @param params
	 *            the output key-value pairs which recieve the BasicElevationModel configuration parameters. A
	 *            null reference is permitted.
	 * @return a reference to params, or a new AVList if params is null.
	 * @throws IllegalArgumentException
	 *             if the document is null.
	 */
	public static AVList getBasicElevationModelConfigParams(Element domElement, AVList params) {
		if (domElement == null) {
			String message = Logging.getMessage("nullValue.DocumentIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		XPath xpath = WWXML.makeXPath();

		// Common elevation model properties.
		AbstractElevationModel.getElevationModelConfigParams(domElement, params);

		// LevelSet properties.
		DataConfigurationUtils.getLevelSetConfigParams(domElement, params);

		// Service properties.
		WWXML.checkAndSetStringParam(domElement, params, AVKey.SERVICE_NAME, "Service/@serviceName", xpath);

		// Image format properties.
		WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);
		WWXML.checkAndSetUniqueStringsParam(domElement, params, AVKey.AVAILABLE_IMAGE_FORMATS, "AvailableImageFormats/ImageFormat", xpath);

		// Data type properties.
		if (params.getValue(AVKey.DATA_TYPE) == null) {
			String s = WWXML.getText(domElement, "DataType/@type", xpath);
			if (s != null && s.length() > 0) {
				s = WWXML.parseDataType(s);
				if (s != null && s.length() > 0) params.setValue(AVKey.DATA_TYPE, s);
			}
		}

		if (params.getValue(AVKey.BYTE_ORDER) == null) {
			String s = WWXML.getText(domElement, "DataType/@byteOrder", xpath);
			if (s != null && s.length() > 0) {
				s = WWXML.parseByteOrder(s);
				if (s != null && s.length() > 0) params.setValue(AVKey.BYTE_ORDER, s);
			}
		}

		// Elevation data properties.
		WWXML.checkAndSetStringParam(domElement, params, AVKey.ELEVATION_EXTREMES_FILE, "ExtremeElevations/FileName", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MAX, "ExtremeElevations/@max", xpath);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.ELEVATION_MIN, "ExtremeElevations/@min", xpath);

		return params;
	}
}
