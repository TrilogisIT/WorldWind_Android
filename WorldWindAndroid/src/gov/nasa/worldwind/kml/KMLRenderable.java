/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.event.MessageListener;
import gov.nasa.worldwind.render.DrawContext;

/**
 * Interface for rendering KML elements.
 *
 * @author tag
 * @version $Id: KMLRenderable.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public interface KMLRenderable extends MessageListener
{
    /**
     * Render this element.
     *
     * @param tc the current KML traversal context.
     * @param dc the current draw context.
     *
     * @throws IllegalArgumentException if either the traversal context or the draw context is null.
     */
    void render(KMLTraversalContext tc, DrawContext dc);
}
