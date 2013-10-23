/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;

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
	protected MemoryCache memoryCache;

	/**
	 * @return the memoryCache
	 */
	public MemoryCache getMemoryCache() {
		return memoryCache;
	}

	protected long updateTime = 0;

	public GpuTextureTile(Sector sector, Level level, int row, int column, MemoryCache cache) {
		super(sector, level, row, column);

		if (cache == null) {
			String msg = Logging.getMessage("nullValue.CacheIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.memoryCache = cache;
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

		if (this.memoryCache.contains(this.tileKey)) this.memoryCache.put(this.tileKey, this);
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
		return GpuTexture.createTexture(dc, textureData);
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
}
