/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileKey;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: GpuTextureTile.java 762 2012-09-07 00:22:58Z tgaskins $
 */
public class GpuTextureTile extends Tile implements SurfaceTile {
	protected Extent extent;
	protected volatile GpuTextureData textureData;
	protected GpuTextureTile fallbackTile;

	/**
	 * Returns the memory cache used to cache texture tiles, initializing the cache if it doesn't yet exist.
	 *
	 * @return the memory cache associated with texture tiles.
	 */
	public static MemoryCache getMemoryCache() {
		if (!WorldWind.getMemoryCacheSet().contains(GpuTextureTile.class.getName())) {
			long size = Configuration.getLongValue(AVKey.GPU_TEXTURE_TILE_CACHE_SIZE);
			MemoryCache cache = new BasicMemoryCache((long) (0.8 * size), size);
			cache.setName("Texture Tiles");
			WorldWind.getMemoryCacheSet().put(GpuTextureTile.class.getName(), cache);
		}

		return WorldWind.getMemoryCacheSet().get(GpuTextureTile.class.getName());
	}

	protected long updateTime = 0;

	public GpuTextureTile(Sector sector, Level level, int row, int column) {
		super(sector, level, row, column);
	}

	public GpuTextureTile(Sector sector, Level level, int row, int column, String cacheName) {
		super(sector, level, row, column, cacheName);
	}

	public Extent getExtent() {
		return this.extent;
	}

	public void setExtent(Extent extent) {
		this.extent = extent;
	}

	public GpuTextureData getTextureData() {
		return this.textureData;
	}

	public void setTextureData(GpuTextureData textureData) {
		this.textureData = textureData;
	}

	public GpuTexture getTexture(GpuResourceCache cache) {
		if (cache == null) {
			String msg = Logging.getMessage("nullValue.CacheIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return cache.getTexture(this.tileKey);
	}

	public void setTexture(GpuResourceCache cache, GpuTexture texture) {
		if (cache == null) {
			String msg = Logging.getMessage("nullValue.CacheIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Assign this tile its new texture, clear the texture data property, and update the tile's size in the memory
		// cache since its texture data is gone. We no longer need the texture data because the texture itself is in GPU
		// memory. On Android our process has ~24 MB of heap memory, but has 100+ MB of GPU memory. Eliminating the
		// texture data is critical to displaying large amounts of tiled imagery without running out of heap memory.

		cache.put(this.tileKey, texture);
		this.updateTime = System.currentTimeMillis();
		this.textureData = null;

		if (getMemoryCache().contains(this.tileKey)) getMemoryCache().put(this.tileKey, this);
	}

	public boolean isTextureInMemory(GpuResourceCache cache) {
		if (cache == null) {
			String msg = Logging.getMessage("nullValue.CacheIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return this.textureData != null || cache.contains(this.tileKey);
	}

	public boolean isTextureExpired() {
		return this.isTextureExpired(this.getLevel().getExpiryTime());
	}

	public boolean isTextureExpired(long expiryTime) {
		return this.updateTime > 0 && this.updateTime < expiryTime;
	}

	public Vec4 getCentroidPoint(Globe globe) {
		if (globe == null) {
			String msg = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return globe.computePointFromLocation(this.getSector().getCentroid());
	}

	public GpuTextureTile getFallbackTile() {
		return this.fallbackTile;
	}

	public void setFallbackTile(GpuTextureTile tile) {
		this.fallbackTile = tile;
	}

	@Override
	public long getSizeInBytes() {
		// This tile's size in bytes is computed as follows:
		// superclass: variable
		// extent: 4 bytes (1 32-bit reference)
		// textureData: 4 bytes + variable (1 32-bit reference + estimated memory size)
		// fallbackTileKey: 4 bytes (1 32-bit reference)
		// textureFactory: 4 bytes (1 32-bit reference)
		// memoryCache: 4 bytes (1 32-bit reference)
		// total: 20 bytes + superclass' size in bytes + texture data size

		long size = 20 + super.getSizeInBytes();

		if (this.textureData != null) size += this.textureData.getSizeInBytes();

		return size;
	}

	public boolean bind(DrawContext dc) {
		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		GpuTexture t = this.getOrCreateTexture(dc);

		if (t == null && this.fallbackTile != null) t = this.fallbackTile.getOrCreateTexture(dc);

		if (t != null) t.bind();

		return t != null;
	}

	public void applyInternalTransform(DrawContext dc, Matrix matrix) {
		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (matrix == null) {
			String msg = Logging.getMessage("nullValue.MatrixIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		GpuTexture t = this.getOrCreateTexture(dc);
		if (t != null) {
			t.applyInternalTransform(dc, matrix);
		} else if (this.fallbackTile != null) {
			t = this.fallbackTile.getOrCreateTexture(dc);
			if (t != null) {
				t.applyInternalTransform(dc, matrix);
				this.applyFallbackTransform(dc, matrix);
			}
		}
	}

	protected GpuTexture getOrCreateTexture(DrawContext dc) {
		if (this.textureData != null) {
			GpuTexture texture = this.createTexture(dc, this.textureData);
			if (texture != null) this.setTexture(dc.getGpuResourceCache(), texture);
			else {
				String msg = Logging.getMessage("GpuTextureTile.UnableToCreateTexture", this);
				Logging.warning(msg);
			}
		}

		return this.getTexture(dc.getGpuResourceCache());
	}

	protected GpuTexture createTexture(DrawContext dc, GpuTextureData textureData) {
		return GpuTexture.createTexture(textureData);
	}

	protected void applyFallbackTransform(DrawContext dc, Matrix matrix) {
		int deltaLevel = this.getLevelNumber() - this.fallbackTile.getLevelNumber();
		if (deltaLevel <= 0) return; // Fallback tile key must be from a level who's ordinal is less than this tile.

		int twoN = 2 << (deltaLevel - 1);
		double sxy = 1d / (double) twoN;
		double tx = sxy * (this.column % twoN);
		double ty = sxy * (this.row % twoN);

		// Apply a transform to the matrix that maps texture coordinates for this tile to texture coordinates for this
		// tile's fallbackTile. We have pre-computed the product of the translation and scaling matrices and stored the
		// result inline here to avoid unnecessary matrix allocations and multiplications. The matrix below is
		// equivalent to the following:
		//
		// Matrix trans = Matrix.fromTranslation(tx, ty, 0);
		// Matrix scale = Matrix.fromScale(sxy, sxy, 1);
		// matrix.multiplyAndSet(trans);
		// matrix.multiplyAndSet(scale);

		matrix.multiplyAndSet(sxy, 0, 0, tx, 0, sxy, 0, ty, 0, 0, 1, 0, 0, 0, 0, 1);
	}

	/**
	 * Creates a sub tile of this texture tile with the specified {@link gov.nasa.worldwind.geom.Sector}, {@link
	 * gov.nasa.worldwind.util.Level}, row, and column. This is called by {@link #createSubTiles(gov.nasa.worldwind.util.Level)},
	 * to construct a sub tile for each quadrant of this tile. Subclasses must override this method to return an
	 * instance of the derived version.
	 *
	 * @param sector the sub tile's sector.
	 * @param level  the sub tile's level.
	 * @param row    the sub tile's row.
	 * @param col    the sub tile's column.
	 *
	 * @return a sub tile of this texture tile.
	 */
	protected GpuTextureTile createSubTile(Sector sector, Level level, int row, int col)
	{
		return new GpuTextureTile(sector, level, row, col);
	}

	/**
	 * Returns a key for a sub tile of this texture tile with the specified {@link gov.nasa.worldwind.util.Level}, row,
	 * and column. This is called by {@link #createSubTiles(gov.nasa.worldwind.util.Level)}, to create a sub tile key
	 * for each quadrant of this tile.
	 *
	 * @param level the sub tile's level.
	 * @param row   the sub tile's row.
	 * @param col   the sub tile's column.
	 *
	 * @return a sub tile of this texture tile.
	 */
	protected TileKey createSubTileKey(Level level, int row, int col)
	{
		return new TileKey(level.getLevelNumber(), row, col, level.getCacheName());
	}

	protected GpuTextureTile getTileFromMemoryCache(TileKey tileKey)
	{
		return (GpuTextureTile) getMemoryCache().get(tileKey);
	}

	protected void updateMemoryCache()
	{
		if (this.getTileFromMemoryCache(this.getTileKey()) != null)
			getMemoryCache().put(this.getTileKey(), this);
	}

	/**
	 * Splits this texture tile into four tiles; one for each sub quadrant of this texture tile. This attempts to
	 * retrieve each sub tile from the texture tile cache. This calls {@link #createSubTile(gov.nasa.worldwind.geom.Sector,
	 * gov.nasa.worldwind.util.Level, int, int)} to create sub tiles not found in the cache.
	 *
	 * @param nextLevel the level for the sub tiles.
	 *
	 * @return a four-element array containing this texture tile's sub tiles.
	 *
	 * @throws IllegalArgumentException if the level is null.
	 */
	public GpuTextureTile[] createSubTiles(Level nextLevel)
	{
		if (nextLevel == null)
		{
			String msg = Logging.getMessage("nullValue.LevelIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Angle p0 = this.getSector().minLatitude;
		Angle p2 = this.getSector().maxLatitude;
		Angle p1 = Angle.midAngle(p0, p2);

		Angle t0 = this.getSector().minLongitude;
		Angle t2 = this.getSector().maxLongitude;
		Angle t1 = Angle.midAngle(t0, t2);

		int row = this.getRow();
		int col = this.getColumn();

		GpuTextureTile[] subTiles = new GpuTextureTile[4];

		TileKey key = this.createSubTileKey(nextLevel, 2 * row, 2 * col);
		GpuTextureTile subTile = this.getTileFromMemoryCache(key);
		if (subTile != null)
			subTiles[0] = subTile;
		else
			subTiles[0] = this.createSubTile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);

		key = this.createSubTileKey(nextLevel, 2 * row, 2 * col + 1);
		subTile = this.getTileFromMemoryCache(key);
		if (subTile != null)
			subTiles[1] = subTile;
		else
			subTiles[1] = this.createSubTile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);

		key = this.createSubTileKey(nextLevel, 2 * row + 1, 2 * col);
		subTile = this.getTileFromMemoryCache(key);
		if (subTile != null)
			subTiles[2] = subTile;
		else
			subTiles[2] = this.createSubTile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

		key = this.createSubTileKey(nextLevel, 2 * row + 1, 2 * col + 1);
		subTile = this.getTileFromMemoryCache(key);
		if (subTile != null)
			subTiles[3] = subTile;
		else
			subTiles[3] = this.createSubTile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);

		return subTiles;
	}
}
