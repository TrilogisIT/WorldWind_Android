/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.layers;

import android.graphics.Point;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The <code>RenderableLayer</code> class manages a collection of {@link gov.nasa.worldwind.render.Renderable} objects
 * for rendering, picking, and disposal.
 *
 * @author tag
 * @version $Id: RenderableLayer.java 778 2012-09-19 17:52:20Z dcollins $
 * @see gov.nasa.worldwind.render.Renderable
 */
public class RenderableLayer extends AbstractLayer
{
    protected Collection<Renderable> renderables = new ConcurrentLinkedQueue<Renderable>();

    /** Creates a new <code>RenderableLayer</code> with an empty internal collection. */
    public RenderableLayer()
    {
        this.setName(Logging.getMessage("layers.RenderableLayer.Name"));
    }

    /**
     * Returns the layer's opacity value, which is ignored by this layer because each of its renderables typiically has
     * its own opacity control.
     *
     * @return The layer opacity, a value between 0 and 1.
     */
    @Override
    public double getOpacity()
    {
        return super.getOpacity();
    }

    /**
     * Opacity is not applied to layers of this type because each renderable typically has its own opacity control.
     *
     * @param opacity the current opacity value, which is ignored by this layer.
     */
    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
    }

    /**
     * Returns the number of elements in this layer's internal collection.
     *
     * @return the size of this layer's internal collection, or 0 if the collection is empty.
     */
    public int getNumRenderables()
    {
        return this.renderables.size();
    }

    /**
     * Returns this layer's internal collection of Renderables as an Iterable.
     *
     * @return an Iterable over this layer's internal collection.
     */
    public Iterable<Renderable> getRenderables()
    {
        return this.renderables;
    }

    /**
     * Adds the specified <code>renderable</code> to this layer's internal collection.
     * <p/>
     * If the <code>renderable</code> implements {@link gov.nasa.worldwind.avlist.AVList}, the layer forwards its
     * property change events to the layer's property change listeners. Any property change listeners the layer attaches
     * to the <code>renderable</code> are removed in {@link #removeRenderable(gov.nasa.worldwind.render.Renderable)},
     * {@link #removeAllRenderables()}, or {@link #dispose()}.
     *
     * @param renderable Renderable to add.
     *
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     */
    public void addRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderables.add(renderable))
        {
            // Attach the layer as a property change listener of the renderable. This forwards property change events
            // from the renderable to the SceneController.
            if (renderable instanceof AVList)
                ((AVList) renderable).addPropertyChangeListener(this);
        }
    }

    /**
     * Adds the contents of the specified <code>renderables</code> to this layer's internal collection. This ignores any
     * null elements in the specified Iterable.
     * <p/>
     * If any of the <code>renderables</code> implement {@link gov.nasa.worldwind.avlist.AVList}, the layer forwards
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attaches to the <code>renderable</code> are removed in {@link #removeRenderable(gov.nasa.worldwind.render.Renderable)},
     * {@link #removeAllRenderables()}, or {@link #dispose()}.
     *
     * @param renderables Renderables to add.
     *
     * @throws IllegalArgumentException If <code>renderables</code> is null.
     */
    public void addAllRenderables(Iterable<? extends Renderable> renderables)
    {
        if (renderables == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        for (Renderable renderable : renderables)
        {
            // Internal list of renderables does not accept null values.
            if (renderable == null)
                continue;

            if (this.renderables.add(renderable))
            {
                // Attach the layer as a property change listener of the renderable. This forwards property change
                // events from the renderable to the SceneController.
                if (renderable instanceof AVList)
                    ((AVList) renderable).addPropertyChangeListener(this);
            }
        }
    }

    /**
     * Removes the specified <code>renderable</code> from this layer's internal collection, if it exists.
     * <p/>
     * If the <code>renderable</code> implements {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding the its
     * property change events to the layer's property change listeners. Any property change listeners the layer attached
     * to the <code>renderable</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or {@link
     * #addAllRenderables(Iterable)} are removed.
     *
     * @param renderable Renderable to remove.
     *
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     */
    public void removeRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.RenderableIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.renderables.remove(renderable))
        {
            // Remove the layer as a property change listener of the renderable. This prevents the renderable from
            // keeping a dangling reference to the layer.
            if (renderable instanceof AVList)
                ((AVList) renderable).removePropertyChangeListener(this);
        }
    }

    /**
     * Clears the contents of this layer's internal Renderable collection.
     * <p/>
     * If any of the <code>renderables</code> implement {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or
     * {@link #addAllRenderables(Iterable)} are removed.
     */
    public void removeAllRenderables()
    {
        // Remove the layer as property change listener of any renderables. This prevents the renderables from
        // keeping a dangling references to the layer.
        for (Renderable renderable : this.renderables)
        {
            if (renderable instanceof AVList)
                ((AVList) renderable).removePropertyChangeListener(this);
        }

        this.renderables.clear();
    }

    protected void doPick(DrawContext dc, Point pickPoint)
    {
        // TODO: Determine whether maintaining a pick list here has any purpose. Is everything deferred to ordered rendering?
        //try
        //{
        //    this.pickSupport.beginPicking(dc);
        //
        //    for (Renderable renderable : this.renderables)
        //    {
        //        Color color = dc.getUniquePickColor();
        //        dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        //
        //        try
        //        {
        //            renderable.render(dc);
        //        }
        //        catch (Exception e)
        //        {
        //            String msg = Logging.getMessage("generic.ExceptionPickingRenderable", renderable);
        //            Logging.error(msg, e);
        //            continue; // Don't abort; continue on to the next renderable.
        //        }
        //
        //        if (renderable instanceof Locatable)
        //        {
        //            this.pickSupport.addPickableObject(color.getRGB(), renderable,
        //                ((Locatable) renderable).getPosition(), false);
        //        }
        //        else
        //        {
        //            this.pickSupport.addPickableObject(color.getRGB(), renderable);
        //        }
        //    }
        //}
        //finally
        //{
        //    this.pickSupport.endPicking(dc);
        //    this.pickSupport.resolvePick(dc, pickPoint, this);
        //}
    }

    protected void doRender(DrawContext dc)
    {
        for (Renderable renderable : this.renderables)
        {
            try
            {
                renderable.render(dc);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionRenderingRenderable", renderable);
                Logging.error(msg, e);
                // Don't abort; continue on to the next renderable.
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation forwards the message to each Renderable that implements {@link
     * gov.nasa.worldwind.event.MessageListener}.
     *
     * @param message The message that was received.
     */
    @Override
    public void onMessage(Message message)
    {
        for (Renderable renderable : this.renderables)
        {
            try
            {
                if (renderable instanceof MessageListener)
                    ((MessageListener) renderable).onMessage(message);
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionSendingMessage", message, renderable);
                Logging.error(msg, e);
                // Don't abort; continue on to the next renderable.
            }
        }
    }

    /**
     * Disposes the contents of this layer's internal Renderable collection and removes all of its elements.
     * <p/>
     * If any of layer's internal Renderables implement {@link gov.nasa.worldwind.avlist.AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #addRenderable(gov.nasa.worldwind.render.Renderable)} or
     * {@link #addAllRenderables(Iterable)} are removed.
     */
    public void dispose()
    {
        for (Renderable renderable : this.renderables)
        {
            try
            {
                if (renderable instanceof Disposable)
                    ((Disposable) renderable).dispose();
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionDisposingObject", renderable);
                Logging.error(msg, e);
                // Don't abort; continue on to the next renderable.
            }
        }

        this.removeAllRenderables();
    }
}
