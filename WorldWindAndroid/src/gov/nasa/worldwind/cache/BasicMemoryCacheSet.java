/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.util.Logging;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dcollins
 * @version $Id: BasicMemoryCacheSet.java 834 2012-10-08 22:25:55Z dcollins $
 */
public class BasicMemoryCacheSet implements MemoryCacheSet
{
    protected ConcurrentHashMap<String, MemoryCache> caches = new ConcurrentHashMap<String, MemoryCache>();

    public BasicMemoryCacheSet()
    {
    }

    /** {@inheritDoc} */
    public synchronized MemoryCache get(String key)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.caches.get(key);
    }

    /** {@inheritDoc} */
    public synchronized MemoryCache put(String key, MemoryCache cache)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (cache == null)
        {
            String msg = Logging.getMessage("nullValue.CacheIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.caches.putIfAbsent(key, cache);
    }

    /** {@inheritDoc} */
    public synchronized boolean contains(String key)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.caches.containsKey(key);
    }

    /** {@inheritDoc} */
    public synchronized void clear()
    {
        for (MemoryCache cache : this.caches.values())
        {
            cache.clear();
        }

        this.caches.clear();
    }
}
