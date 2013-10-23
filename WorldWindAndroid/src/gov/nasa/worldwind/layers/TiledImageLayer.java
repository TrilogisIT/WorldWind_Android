/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.event.BulkRetrievalListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuTextureData;
import gov.nasa.worldwind.render.GpuTextureTile;
import gov.nasa.worldwind.retrieve.AbstractRetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.BulkRetrievable;
import gov.nasa.worldwind.retrieve.BulkRetrievalThread;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileKey;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWXML;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import android.graphics.Point;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: TiledImageLayer.java 842 2012-10-09 23:46:47Z tgaskins $
 */
// TODO: apply layer opacity during rendering
public class TiledImageLayer extends AbstractLayer implements Tile.TileFactory, BulkRetrievable {
	protected LevelSet levels;
	protected double detailHintOrigin = 2.6; // the default detail hint origin
	protected double detailHint;
	protected boolean forceLevelZeroLoads = false;
	protected boolean levelZeroLoaded = false;
	protected boolean retainLevelZeroTiles = false;
	protected boolean useTransparentTextures = false;
	protected List<Tile> topLevelTiles = new ArrayList<Tile>();
	protected String tileCountName;

	// Stuff computed each frame
	protected List<GpuTextureTile> currentTiles = new ArrayList<GpuTextureTile>();
	protected GpuTextureTile currentAncestorTile;
	protected PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(200);
	protected final Object fileLock = new Object();

	public TiledImageLayer(AVList params) {
		if (params == null) {
			String message = Logging.getMessage("nullValue.ParamsIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.levels = new LevelSet(params);

		this.setValue(AVKey.SECTOR, this.levels.getSector());
		this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
		this.tileCountName = this.getName() + " Tiles";

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

		Boolean b;

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
	}

	public TiledImageLayer(Element domElement, AVList params) {
		this(getParamsFromDocument(domElement, params));
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

		if (params.getValue(AVKey.TILE_WIDTH) == null) params.setValue(AVKey.TILE_WIDTH, 512);

		if (params.getValue(AVKey.TILE_HEIGHT) == null) params.setValue(AVKey.TILE_HEIGHT, 512);

		if (params.getValue(AVKey.FORMAT_SUFFIX) == null) params.setValue(AVKey.FORMAT_SUFFIX, ".dds");

		if (params.getValue(AVKey.NUM_LEVELS) == null) params.setValue(AVKey.NUM_LEVELS, 19); // approximately 0.1 meters per pixel

		if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null) params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
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

	public boolean isForceLevelZeroLoads() {
		return this.forceLevelZeroLoads;
	}

	public void setForceLevelZeroLoads(boolean forceLevelZeroLoads) {
		this.forceLevelZeroLoads = forceLevelZeroLoads;
	}

	public boolean isRetainLevelZeroTiles() {
		return retainLevelZeroTiles;
	}

	public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles) {
		this.retainLevelZeroTiles = retainLevelZeroTiles;
	}

	/**
	 * Indicates the layer's detail hint, which is described in {@link #setDetailHint(double)}.
	 * 
	 * @return the detail hint
	 * @see #setDetailHint(double)
	 */
	public double getDetailHint() {
		return this.detailHint;
	}

	/**
	 * Modifies the default relationship of image resolution to screen resolution as the viewing altitude changes.
	 * Values greater than 0 cause imagery to appear at higher resolution at greater altitudes than normal, but at an
	 * increased performance cost. Values less than 0 decrease the default resolution at any given altitude. The default
	 * value is 0. Values typically range between -0.5 and 0.5.
	 * <p/>
	 * Note: The resolution-to-height relationship is defined by a scale factor that specifies the approximate size of discernible lengths in the image relative
	 * to eye distance. The scale is specified as a power of 10. A value of 3, for example, specifies that 1 meter on the surface should be distinguishable from
	 * an altitude of 10^3 meters (1000 meters). The default scale is 1/10^2.8, (1 over 10 raised to the power 2.8). The detail hint specifies deviations from
	 * that default. A detail hint of 0.2 specifies a scale of 1/1000, i.e., 1/10^(2.8 + .2) = 1/10^3. Scales much larger than 3 typically cause the applied
	 * resolution to be higher than discernible for the altitude. Such scales significantly decrease performance.
	 * 
	 * @param detailHint
	 *            the degree to modify the default relationship of image resolution to screen resolution with
	 *            changing view altitudes. Values greater than 1 increase the resolution. Values less than zero
	 *            decrease the resolution. The default value is 0.
	 */
	public void setDetailHint(double detailHint) {
		this.detailHint = detailHint;
	}

	protected LevelSet getLevels() {
		return levels;
	}

	protected PriorityBlockingQueue<Runnable> getRequestQ() {
		return requestQ;
	}

	public boolean isUseTransparentTextures() {
		return this.useTransparentTextures;
	}

	public void setUseTransparentTextures(boolean useTransparentTextures) {
		this.useTransparentTextures = useTransparentTextures;
	}

	/**
	 * Specifies the time of the layer's most recent dataset update, beyond which cached data is invalid. If greater
	 * than zero, the layer ignores and eliminates any in-memory or on-disk cached data older than the time specified,
	 * and requests new information from the data source. If zero, the default, the layer applies any expiry times
	 * associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired only
	 * when the expiry time is specified with this method and is greater than zero. This method also overwrites the
	 * expiry times of the layer's individual levels if the value specified to the method is greater than zero.
	 * 
	 * @param expiryTime
	 *            the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch. The
	 *            default expiry time is zero.
	 * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
	 */
	@Override
	public void setExpiryTime(long expiryTime) // Override this method to use intrinsic level-specific expiry times
	{
		super.setExpiryTime(expiryTime);

		if (expiryTime > 0) this.levels.setExpiryTime(expiryTime); // remove this in sub-class to use level-specific expiry times
	}

	protected void checkTextureExpiration(DrawContext dc, List<GpuTextureTile> tiles) {
		for (GpuTextureTile tile : tiles) {
			if (tile.isTextureExpired()) this.requestTile(dc, tile);
		}
	}

	protected void sendRequests() {
		Runnable task = this.requestQ.poll();
		while (task != null) {
			if (!WorldWind.getTaskService().isFull()) {
				WorldWind.getTaskService().runTask(task);
			}
			task = this.requestQ.poll();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to return <code>false</code> when this layer's LevelSet is entirely outside of the current visible sector. This provides an effective way to
	 * cull the entire layer before it performs any unnecessary work.
	 */
	@Override
	public boolean isLayerInView(DrawContext dc) {
		return dc.getVisibleSector() == null || dc.getVisibleSector().intersects(this.levels.getSector());
	}

	protected Vec4 computeReferencePoint(DrawContext dc) {
		if (dc.getViewportCenterPosition() != null) return dc.getGlobe().computePointFromPosition(dc.getViewportCenterPosition());
		Rect viewport = dc.getView().getViewport();

		int x = (int) viewport.width / 2;
		for (int y = (int) (0.5 * viewport.height); y >= 0; y--) {
			// point
			Point point = new Point(x, y);
			Position pos = new Position();
			boolean calculated = dc.getView().computePositionFromScreenPoint(dc.getGlobe(), point, pos);// computePositionFromScreenPoint(x, y);
			if (!calculated) continue;

			return dc.getGlobe().computePointFromPosition(pos.latitude, pos.longitude, 0d);
		}

		return null;
	}

	protected Vec4 getReferencePoint(DrawContext dc) {
		return this.computeReferencePoint(dc);
	}

	// ============== Rendering ======================= //
	// ============== Rendering ======================= //
	// ============== Rendering ======================= //

	@Override
	protected void doRender(DrawContext dc) {
		if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1) return;

		this.assembleTiles(dc);

		if (!this.currentTiles.isEmpty()) {
			// TODO: apply opacity and transparent texture support

			dc.getSurfaceTileRenderer().renderTiles(dc, this.currentTiles);

			// Check texture expiration. Memory-cached textures are checked for expiration only when an explicit,
			// non-zero expiry time has been set for the layer. If none has been set, the expiry times of the layer's
			// individual levels are used, but only for images in the local file cache, not textures in memory. This is
			// to avoid incurring the overhead of checking expiration of in-memory textures, a very rarely used feature.
			if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis()) this.checkTextureExpiration(dc, this.currentTiles);

			this.currentTiles.clear();
		}

		this.sendRequests();
		this.requestQ.clear();

		// TODO: clear fallback tiles
	}

	public GpuTextureTile createTile(Sector sector, Level level, int row, int column) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (level == null) {
			String msg = Logging.getMessage("nullValue.LevelIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (row < 0) {
			String msg = Logging.getMessage("generic.RowIndexOutOfRange", row);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (column < 0) {
			String msg = Logging.getMessage("generic.ColumnIndexOutOfRange", column);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return new GpuTextureTile(sector, level, row, column, this.getTextureTileCache());
	}

	// ============== Tile Assembly ======================= //
	// ============== Tile Assembly ======================= //
	// ============== Tile Assembly ======================= //

	protected void assembleTiles(DrawContext dc) {
		this.currentTiles.clear();

		if (this.topLevelTiles.isEmpty()) this.createTopLevelTiles();

		for (int i = 0; i < this.topLevelTiles.size(); i++) {
			Tile tile = this.topLevelTiles.get(i);

			this.updateTileExtent(dc, (GpuTextureTile) tile);
			this.currentAncestorTile = null;

			if (this.isTileVisible(dc, (GpuTextureTile) tile)) this.addTileOrDescendants(dc, (GpuTextureTile) tile);
		}
	}

	protected void createTopLevelTiles() {
		if (this.levels.getFirstLevel() == null) {
			Logging.warning(Logging.getMessage("generic.FirstLevelIsNull"));
			return;
		}

		this.topLevelTiles.clear();
		// TODO Tile.createTilesForLevel(this.levels.getFirstLevel(), this.levels.getSector(), this, this.topLevelTiles);
		Tile.createTilesForLevel(this.levels.getFirstLevel(), this.levels.getSector(), this, this.topLevelTiles, this.levels.getTileOrigin());

	}

	protected void addTileOrDescendants(DrawContext dc, GpuTextureTile tile) {
		this.updateTileExtent(dc, tile);

		if (this.meetsRenderCriteria(dc, tile)) {
			this.addTile(dc, tile);
			return;
		}

		// The incoming tile does not meet the rendering criteria, so it must be subdivided and those subdivisions
		// tested against the criteria.

		// All tiles that meet the selection criteria are drawn, but some of those tiles will not have textures
		// associated with them because their texture isn't loaded yet. In this case the tiles use the texture of the
		// closest ancestor that has a texture loaded. The ancestor is called the currentAncestorTile. A texture
		// transform is applied during rendering to align the sector's texture coordinates with the appropriate region
		// of the ancestor's texture.

		MemoryCache cache = this.getTextureTileCache();
		GpuTextureTile ancestorTile = null;

		try {
			if (tile.isTextureInMemory(dc.getGpuResourceCache()) || tile.getLevelNumber() == 0) {
				ancestorTile = this.currentAncestorTile;
				this.currentAncestorTile = tile;
			}

			Tile[] subTiles = tile.subdivide(this.levels.getLevel(tile.getLevelNumber() + 1), cache, this);
			for (Tile child : subTiles) {
				// Put all sub-tiles in the terrain tile cache to avoid repeatedly allocating them each frame. Sub
				// tiles are placed in the cache here, and updated when their terrain geometry changes.
				if (!cache.contains(child.getTileKey())) cache.put(child.getTileKey(), child);

				// Add descendant tiles that intersect the LevelSet's sector and are visible. If half or more of this
				// tile (in either latitude or longitude) extends beyond the LevelSet's sector, then two or three of its
				// children will be entirely outside the LevelSet's sector.
				if (this.levels.getSector().intersects(child.getSector()) && this.isTileVisible(dc, (GpuTextureTile) child)) {
					this.addTileOrDescendants(dc, (GpuTextureTile) child);
				}
			}
			tile.clearChildList();
		} finally {
			if (ancestorTile != null) this.currentAncestorTile = ancestorTile;
		}
	}

	protected void addTile(DrawContext dc, GpuTextureTile tile) {
		tile.setFallbackTile(null);

		// If this tile's level is empty, just ignore it. When the view moves closer to the tile it is subdivided and
		// an non-empty child level is eventually added.
		if (tile.getLevel().isEmpty()) return;

		// If the tile's texture is in memory, add it to the list of current tiles and return.
		if (tile.isTextureInMemory(dc.getGpuResourceCache())) {
			this.currentTiles.add(tile);
			return;
		}

		// The tile's texture is not in memory. Issue a request for the texture data if the tile is not already marked
		// as an absent resource. We ignore absent resources to avoid flooding the system with requests for resources
		// that are never resolved.
		if (!this.levels.isResourceAbsent(tile)) this.requestTile(dc, tile);

		if (this.currentAncestorTile != null) {
			// If the current ancestor tile's texture is in memory, then use it as this tile's fallback tile and add
			// this tile to the list of current tiles. Otherwise, we check if the ancestor tile is a level zero tile and
			// if so issue a request to load it into memory. This is critical to correctly handling the case when an
			// application is resumed with the view close to the globe. In that case, the level zero tiles are never
			// initially loaded and the tile that meets the render criteria may have no data. By issuing a request for
			// level zero ancestor tiles, we ensure that something displays when the application resumes.

			if (this.currentAncestorTile.isTextureInMemory(dc.getGpuResourceCache())) {
				tile.setFallbackTile(this.currentAncestorTile);
				this.currentTiles.add(tile);
			} else if (this.currentAncestorTile.getLevelNumber() == 0) {
				if (!this.levels.isResourceAbsent(this.currentAncestorTile)) this.requestTile(dc, this.currentAncestorTile);
			}
		}
	}

	protected boolean isTileVisible(DrawContext dc, GpuTextureTile tile) {
		// TODO: compute extent every frame or periodically update
		if (tile.getExtent() == null) tile.setExtent(this.computeTileExtent(dc, tile));

		Sector visibleSector = dc.getVisibleSector();
		Extent extent = tile.getExtent();

		return (visibleSector == null || visibleSector.intersects(tile.getSector())) && (extent == null || dc.getView().getFrustumInModelCoordinates().intersects(extent));
	}

	protected boolean meetsRenderCriteria(DrawContext dc, GpuTextureTile tile) {
		return this.levels.isFinalLevel(tile.getLevelNumber()) || !this.needToSubdivide(dc, tile);
	}

	protected boolean needToSubdivide(DrawContext dc, GpuTextureTile tile) {
		return tile.mustSubdivide(dc, this.detailHintOrigin + this.detailHint);
	}

	protected void updateTileExtent(DrawContext dc, GpuTextureTile tile) {
		// TODO: regenerate the tile extent and reference points whenever the underlying elevation model changes.
		// TODO: regenerate the tile extent and reference points whenever the vertical exaggeration changes.

		if (tile.getExtent() == null) {
			tile.setExtent(this.computeTileExtent(dc, tile));
		}

		// Update the tile's reference points.
		Vec4[] points = tile.getReferencePoints();
		if (points == null) {
			points = new Vec4[] { new Vec4(), new Vec4(), new Vec4(), new Vec4(), new Vec4() };
			tile.getSector().computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration(), points);
			tile.getSector().computeCentroidPoint(dc.getGlobe(), dc.getVerticalExaggeration(), points[4]);
			tile.setReferencePoints(points);
		}
	}

	protected Extent computeTileExtent(DrawContext dc, GpuTextureTile tile) {
		return Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), tile.getSector());
	}

	/**
	 * Returns the memory cache used to cache texture tiles, initializing the cache if it doesn't yet exist.
	 * 
	 * @return the memory cache associated with texture tiles.
	 */
	protected MemoryCache getTextureTileCache() {
		if (!WorldWind.getMemoryCacheSet().contains(GpuTextureTile.class.getName())) {
			long size = Configuration.getLongValue(AVKey.GPU_TEXTURE_TILE_CACHE_SIZE);
			MemoryCache cache = new BasicMemoryCache((long) (0.8 * size), size);
			cache.setName("Texture Tiles");
			WorldWind.getMemoryCacheSet().put(GpuTextureTile.class.getName(), cache);
		}

		return WorldWind.getMemoryCacheSet().get(GpuTextureTile.class.getName());
	}

	// **************************************************************//
	// ********************** Retrieval ***************************//
	// **************************************************************//

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
	 * Create a task to load a tile.
	 * 
	 * @param dc
	 *            current draw context.
	 * @param tile
	 *            tile to load.
	 * @return new task.
	 */
	protected Runnable createRequestTask(DrawContext dc, GpuTextureTile tile) {
		double priority = this.computeTilePriority(dc, tile);
		tile.setPriority(priority);
		return new RequestTask(tile, this, priority);
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
			this.loadTileFromCache(tile, textureURL);
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
	protected void loadTileFromCache(GpuTextureTile tile, URL textureURL) {
		GpuTextureData textureData;

		synchronized (this.fileLock) {
			textureData = this.createTextureData(textureURL);
		}

		if (textureData != null) {
			tile.setTextureData(textureData);

			// The tile's size has changed, so update its size in the memory cache.
			MemoryCache cache = this.getTextureTileCache();
			if (cache.contains(tile.getTileKey())) cache.put(tile.getTileKey(), tile);

			// Mark the tile as not absent to ensure that it is used, and cause any World Windows containing this layer
			// to repaint themselves.
			this.levels.unmarkResourceAbsent(tile);
			this.firePropertyChange(AVKey.LAYER, null, this);
		} else {
			// Assume that something is wrong with the file and delete it.
			this.getDataFileStore().removeFile(textureURL);
			String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
			Logging.info(message);
		}
	}

	protected GpuTextureData createTextureData(URL textureURL) {
		return GpuTextureData.createTextureData(textureURL);
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

	protected static class RequestTask implements Runnable, Comparable<RequestTask> {
		protected GpuTextureTile tile;
		protected TiledImageLayer layer;
		protected double priority;

		public RequestTask(GpuTextureTile tile, TiledImageLayer layer, double priority) {
			if (tile == null) {
				String msg = Logging.getMessage("nullValue.TileIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			if (layer == null) {
				String msg = Logging.getMessage("nullValue.LayerIsNull");
				Logging.error(msg);
				throw new IllegalArgumentException(msg);
			}

			this.tile = tile;
			this.layer = layer;
			this.priority = priority;
		}

		public void run() {
			if (Thread.currentThread().isInterrupted()) return; // This task was cancelled because it's a duplicate or for some other reason.

			this.layer.loadTile(this.tile);
		}

		public int compareTo(RequestTask that) {
			if (that == null) return -1;

			return this.priority < that.priority ? -1 : (this.priority > that.priority ? 1 : 0);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof RequestTask)) return false;

			RequestTask that = (RequestTask) o;
			return this.tile.equals(that.tile);
		}

		@Override
		public int hashCode() {
			return this.tile.hashCode();
		}

		@Override
		public String toString() {
			return this.tile.toString();
		}
	}

	protected static class DownloadPostProcessor extends AbstractRetrievalPostProcessor {
		protected GpuTextureTile tile;
		protected TiledImageLayer layer;
		protected FileStore fileStore;

		public DownloadPostProcessor(GpuTextureTile tile, TiledImageLayer layer, FileStore fileStore) {
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

		@Override
		protected File doGetOutputFile() {
			return layer.getDataFileStore().newFile(this.tile.getPath());
		}

		@Override
		protected ByteBuffer handleSuccessfulRetrieval() {
			ByteBuffer buffer = super.handleSuccessfulRetrieval();

			if (buffer != null) {
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

	// **************************************************************//
	// ******************** Configuration *************************//
	// **************************************************************//
	/**
	 * Creates a configuration document for a TiledImageLayer described by the
	 * specified params. The returned document may be used as a construction
	 * parameter to {@link gov.nasa.worldwind.layers.BasicTiledImageLayer}.
	 * 
	 * @param params
	 *            parameters describing the TiledImageLayer.
	 * @return a configuration document for the TiledImageLayer.
	 */
	public static Document createTiledImageLayerConfigDocument(AVList params) {
		Document doc = WWXML.createDocumentBuilder(true).newDocument();

		Element root = WWXML.setDocumentElement(doc, "Layer");
		WWXML.setIntegerAttribute(root, "version", 1);
		WWXML.setTextAttribute(root, "layerType", "TiledImageLayer");

		createTiledImageLayerConfigElements(params, root);

		return doc;
	}

	/**
	 * Appends TiledImageLayer configuration parameters as elements to the
	 * specified context. This appends elements for the following parameters:
	 * <table>
	 * <tr>
	 * <th>Parameter</th>
	 * <th>Element Path</th>
	 * <th>Type</th>
	 * </tr>
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
	 * <td>{@link AVKey#FORCE_LEVEL_ZERO_LOADS}</td>
	 * <td>ForceLevelZeroLoads</td>
	 * <td>Boolean</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#RETAIN_LEVEL_ZERO_TILES}</td>
	 * <td>RetainLevelZeroTiles</td>
	 * <td>Boolean</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#TEXTURE_FORMAT}</td>
	 * <td>TextureFormat</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#USE_MIP_MAPS}</td>
	 * <td>UseMipMaps</td>
	 * <td>Boolean</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#USE_TRANSPARENT_TEXTURES}</td>
	 * <td>UseTransparentTextures</td>
	 * <td>Boolean</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#URL_CONNECT_TIMEOUT}</td>
	 * <td>RetrievalTimeouts/ConnectTimeout/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#URL_READ_TIMEOUT}</td>
	 * <td>RetrievalTimeouts/ReadTimeout/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td>
	 * <td>RetrievalTimeouts/StaleRequestLimit/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * </table>
	 * This also writes common layer and LevelSet configuration parameters by
	 * invoking {@link gov.nasa.worldwind.layers.AbstractLayer#createLayerConfigElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)} and
	 * {@link DataConfigurationUtils#createLevelSetConfigElements(gov.nasa.worldwind.avlist.AVList, org.w3c.dom.Element)} .
	 * 
	 * @param params
	 *            the key-value pairs which define the TiledImageLayer
	 *            configuration parameters.
	 * @param context
	 *            the XML document root on which to append TiledImageLayer
	 *            configuration elements.
	 * @return a reference to context.
	 * @throws IllegalArgumentException
	 *             if either the parameters or the context are null.
	 */
	public static Element createTiledImageLayerConfigElements(AVList params, Element context) {
		if (params == null) {
			String message = Logging.getMessage("nullValue.ParametersIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (context == null) {
			String message = Logging.getMessage("nullValue.ContextIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		XPath xpath = WWXML.makeXPath();

		// Common layer properties.
		// AbstractLayer.createLayerConfigElements(params, context);
		AbstractLayer.getLayerConfigParams(context, params);

		// LevelSet properties.
		// DataConfigurationUtils.createLevelSetConfigElements(params, context);

		DataConfigurationUtils.getLevelSetConfigParams(context, params);

		// Service properties.
		// Try to get the SERVICE_NAME property, but default to "WWTileService".
		String s = AVListImpl.getStringValue(params, AVKey.SERVICE_NAME, "WWTileService");
		if (s != null && s.length() > 0) {
			// The service element may already exist, in which case we want to
			// append to it.
			Element el = WWXML.getElement(context, "Service", xpath);
			if (el == null) el = WWXML.appendElementPath(context, "Service");
			WWXML.setTextAttribute(el, "serviceName", s);
		}

		WWXML.checkAndAppendBooleanElement(params, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE, context, "RetrievePropertiesFromService");

		// Image format properties.
		WWXML.checkAndAppendTextElement(params, AVKey.IMAGE_FORMAT, context, "ImageFormat");
		WWXML.checkAndAppendTextElement(params, AVKey.TEXTURE_FORMAT, context, "TextureFormat");

		Object o = params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS);
		if (o != null && o instanceof String[]) {
			String[] strings = (String[]) o;
			if (strings.length > 0) {
				// The available image formats element may already exists, in
				// which case we want to append to it, rather
				// than create entirely separate paths.
				Element el = WWXML.getElement(context, "AvailableImageFormats", xpath);
				if (el == null) el = WWXML.appendElementPath(context, "AvailableImageFormats");
				WWXML.appendTextArray(el, "ImageFormat", strings);
			}
		}

		// Optional behavior properties.
		WWXML.checkAndAppendBooleanElement(params, AVKey.FORCE_LEVEL_ZERO_LOADS, context, "ForceLevelZeroLoads");
		WWXML.checkAndAppendBooleanElement(params, AVKey.RETAIN_LEVEL_ZERO_TILES, context, "RetainLevelZeroTiles");
		// WWXML.checkAndAppendBooleanElement(params, AVKey.USE_MIP_MAPS, context, "UseMipMaps");
		WWXML.checkAndAppendBooleanElement(params, AVKey.USE_TRANSPARENT_TEXTURES, context, "UseTransparentTextures");
		WWXML.checkAndAppendDoubleElement(params, AVKey.DETAIL_HINT, context, "DetailHint");

		// Retrieval properties.
		if (params.getValue(AVKey.URL_CONNECT_TIMEOUT) != null || params.getValue(AVKey.URL_READ_TIMEOUT) != null
				|| params.getValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT) != null) {
			Element el = WWXML.getElement(context, "RetrievalTimeouts", xpath);
			if (el == null) el = WWXML.appendElementPath(context, "RetrievalTimeouts");

			WWXML.checkAndAppendTimeElement(params, AVKey.URL_CONNECT_TIMEOUT, el, "ConnectTimeout/Time");
			WWXML.checkAndAppendTimeElement(params, AVKey.URL_READ_TIMEOUT, el, "ReadTimeout/Time");
			WWXML.checkAndAppendTimeElement(params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, el, "StaleRequestLimit/Time");
		}

		return context;
	}

	/**
	 * Parses TiledImageLayer configuration parameters from the specified DOM document. This writes output as key-value
	 * pairs to params. If a parameter from the XML document already exists in params, that parameter is ignored.
	 * Supported key and parameter names are:
	 * <table>
	 * <tr>
	 * <th>Parameter</th>
	 * <th>Element Path</th>
	 * <th>Type</th>
	 * </tr>
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
	 * <td>{@link AVKey#URL_CONNECT_TIMEOUT}</td>
	 * <td>RetrievalTimeouts/ConnectTimeout/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#URL_READ_TIMEOUT}</td>
	 * <td>RetrievalTimeouts/ReadTimeout/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * <tr>
	 * <td>{@link AVKey#RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT}</td>
	 * <td>RetrievalTimeouts/StaleRequestLimit/Time</td>
	 * <td>Integer milliseconds</td>
	 * </tr>
	 * </table>
	 * This also parses
	 * common layer and LevelSet configuration parameters by invoking
	 * {@link gov.nasa.worldwind.layers.AbstractLayer#getLayerConfigParams(org.w3c.dom.Element, gov.nasa.worldwind.avlist.AVList)} and
	 * {@link gov.nasa.worldwind.util.DataConfigurationUtils#getLevelSetConfigParams(org.w3c.dom.Element, gov.nasa.worldwind.avlist.AVList)}.
	 * 
	 * @param domElement
	 *            the XML document root to parse for TiledImageLayer configuration parameters.
	 * @param params
	 *            the output key-value pairs which receive the TiledImageLayer configuration parameters. A null
	 *            reference is permitted.
	 * @return a reference to params, or a new AVList if params is null.
	 * @throws IllegalArgumentException
	 *             if the document is null.
	 */
	public static AVList getTiledImageLayerConfigParams(Element domElement, AVList params) {
		if (domElement == null) {
			String message = Logging.getMessage("nullValue.DocumentIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		XPath xpath = WWXML.makeXPath();

		// Common layer properties.
		AbstractLayer.getLayerConfigParams(domElement, params);

		// LevelSet properties.
		DataConfigurationUtils.getLevelSetConfigParams(domElement, params);

		// Image format properties.
		WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);
		WWXML.checkAndSetUniqueStringsParam(domElement, params, AVKey.AVAILABLE_IMAGE_FORMATS, "AvailableImageFormats/ImageFormat", xpath);

		// Optional behavior properties.
		WWXML.checkAndSetDoubleParam(domElement, params, AVKey.DETAIL_HINT, "DetailHint", xpath);

		// Retrieval properties. Convert the Long time values to Integers.
		WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_CONNECT_TIMEOUT, "RetrievalTimeouts/ConnectTimeout/Time", xpath);
		WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.URL_READ_TIMEOUT, "RetrievalTimeouts/ReadTimeout/Time", xpath);
		WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, "RetrievalTimeouts/StaleRequestLimit/Time", xpath);

		return params;
	}

	// ============== Bulk Download ======================= //
	// ============== Bulk Download ======================= //
	// ============== Bulk Download ======================= //

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
	 * Start a new {@link BulkRetrievalThread} that downloads all imagery for a given sector and resolution to a
	 * specified {@link FileStore}, without downloading imagery that is already in the file store.
	 * <p/>
	 * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create a downloader that has not been started,
	 * construct a {@link TiledImageLayerBulkDownloader}.
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
	 * @see TiledImageLayerBulkDownloader
	 */
	public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore, BulkRetrievalListener listener) {
		Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
		if (targetSector == null) return null;

		TiledImageLayerBulkDownloader thread = new TiledImageLayerBulkDownloader(this, targetSector, resolution, fileStore != null ? fileStore : this.getDataFileStore(), listener);
		thread.setDaemon(true);
		thread.start();
		return thread;
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

		TiledImageLayerBulkDownloader downloader = new TiledImageLayerBulkDownloader(this, sector, resolution, fileStore != null ? fileStore : this.getDataFileStore(), null);

		return downloader.getEstimatedMissingDataSize();
	}

	protected boolean isTextureFileExpired(GpuTextureTile tile, java.net.URL textureURL, FileStore fileStore) {
		if (!WWIO.isFileOutOfDate(textureURL, tile.getLevel().getExpiryTime())) return false;

		// The file has expired. Delete it.
		fileStore.removeFile(textureURL);
		String message = Logging.getMessage("generic.DataFileExpired", textureURL);
		Logging.verbose(message);
		return true;
	}

	public int computeLevelForResolution(Sector sector, double resolution) {
		if (sector == null) {
			String message = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(message);
			throw new IllegalStateException(message);
		}

		// Find the first level exceeding the desired resolution
		double texelSize;
		Level targetLevel = this.levels.getLastLevel();
		for (int i = 0; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
			if (this.levels.isLevelEmpty(i)) continue;

			texelSize = this.levels.getLevel(i).getTexelSize();
			if (texelSize > resolution) continue;

			targetLevel = this.levels.getLevel(i);
			break;
		}

		// Choose the level closest to the resolution desired
		if (targetLevel.getLevelNumber() != 0 && !this.levels.isLevelEmpty(targetLevel.getLevelNumber() - 1)) {
			Level nextLowerLevel = this.levels.getLevel(targetLevel.getLevelNumber() - 1);
			double dless = Math.abs(nextLowerLevel.getTexelSize() - resolution);
			double dmore = Math.abs(targetLevel.getTexelSize() - resolution);
			if (dless < dmore) targetLevel = nextLowerLevel;
		}

		Logging.verbose(Logging.getMessage("layers.TiledImageLayer.LevelSelection", targetLevel.getLevelNumber(), Double.toString(targetLevel.getTexelSize())));
		return targetLevel.getLevelNumber();
	}

	public long countImagesInSector(Sector sector, int levelNumber) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Level targetLevel = this.levels.getLastLevel();
		if (levelNumber >= 0) {
			for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
				if (this.levels.isLevelEmpty(i)) continue;

				targetLevel = this.levels.getLevel(i);
				break;
			}
		}

		// Collect all the tiles intersecting the input sector.
		LatLon delta = targetLevel.getTileDelta();
		LatLon origin = this.levels.getTileOrigin();
		int nwRow = Tile.computeRow(delta.latitude, sector.maxLatitude, origin.latitude);
		int nwCol = Tile.computeColumn(delta.longitude, sector.minLongitude, origin.longitude);
		int seRow = Tile.computeRow(delta.latitude, sector.minLatitude, origin.latitude);
		int seCol = Tile.computeColumn(delta.longitude, sector.maxLongitude, origin.longitude);

		long numRows = nwRow - seRow + 1;
		long numCols = seCol - nwCol + 1;

		return numRows * numCols;
	}

	public GpuTextureTile[][] getTilesInSector(Sector sector, int levelNumber) {
		if (sector == null) {
			String msg = Logging.getMessage("nullValue.SectorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Level targetLevel = this.levels.getLastLevel();
		if (levelNumber >= 0) {
			for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
				if (this.levels.isLevelEmpty(i)) continue;

				targetLevel = this.levels.getLevel(i);
				break;
			}
		}

		// Collect all the tiles intersecting the input sector.
		LatLon delta = targetLevel.getTileDelta();
		LatLon origin = this.levels.getTileOrigin();
		int nwRow = Tile.computeRow(delta.latitude, sector.maxLatitude, origin.latitude);
		int nwCol = Tile.computeColumn(delta.longitude, sector.minLongitude, origin.longitude);
		int seRow = Tile.computeRow(delta.latitude, sector.minLatitude, origin.latitude);
		int seCol = Tile.computeColumn(delta.longitude, sector.maxLongitude, origin.longitude);

		int numRows = nwRow - seRow + 1;
		int numCols = seCol - nwCol + 1;
		GpuTextureTile[][] sectorTiles = new GpuTextureTile[numRows][numCols];

		for (int row = nwRow; row >= seRow; row--) {
			for (int col = nwCol; col <= seCol; col++) {
				TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
				Sector tileSector = this.levels.computeSectorForKey(key);
				sectorTiles[nwRow - row][col - nwCol] = this.createTile(tileSector, targetLevel, row, col);
			}
		}

		return sectorTiles;
	}
}
