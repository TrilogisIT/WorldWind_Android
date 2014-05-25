/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import org.w3c.dom.Element;

/**
 * @author dcollins
 * @version $Id: BasicModel.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class BasicModel extends WWObjectImpl implements Model
{
    protected Globe globe;
    protected LayerList layers;
	protected boolean showTessellationBoundingVolumes;
	protected boolean showTessellationTileIds;
	protected boolean showWireframe;

    public BasicModel()
    {
        this.setGlobe(this.createGlobe());
        this.setLayers(this.createLayers());
    }

    public BasicModel(Globe globe, LayerList layers)
    {
        this.setGlobe(globe);
        this.setLayers(layers);
    }

    protected Globe createGlobe()
    {
        return (Globe) WorldWind.createConfigurationComponent(AVKey.GLOBE_CLASS_NAME);
    }

    protected LayerList createLayers()
    {
        Element el = Configuration.getElement("./LayerList");
        if (el != null)
        {
            Object o = BasicFactory.create(AVKey.LAYER_FACTORY, el);

            if (o instanceof LayerList)
                return (LayerList) o;

            else if (o instanceof Layer)
                return new LayerList(new Layer[] {(Layer) o});

            else if (o instanceof LayerList[])
            {
                LayerList[] lists = (LayerList[]) o;
                if (lists.length > 0)
                    return LayerList.collapseLists((LayerList[]) o);
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    public Globe getGlobe()
    {
        return this.globe;
    }

    /** {@inheritDoc} */
    public void setGlobe(Globe globe)
    {
        // don't raise an exception if globe == null. In that case, we are disassociating the model from any globe

        if (this.globe != null)
            this.globe.removePropertyChangeListener(this);
        if (globe != null)
            globe.addPropertyChangeListener(this);

        Globe old = this.globe;
        this.globe = globe;
        this.firePropertyChange(AVKey.GLOBE, old, this.globe);
    }

    /** {@inheritDoc} */
    public LayerList getLayers()
    {
        return this.layers;
    }

    /** {@inheritDoc} */
    public void setLayers(LayerList layers)
    {
        // don't raise an exception if layers == null. In that case, we are disassociating the model from any layer set

        if (this.layers != null)
            this.layers.removePropertyChangeListener(this);
        if (layers != null)
            layers.addPropertyChangeListener(this);

        LayerList old = this.layers;
        this.layers = layers;
        this.firePropertyChange(AVKey.LAYERS, old, this.layers);
    }

	/**
	 * Specifies whether to display as wireframe the exterior geometry of the tessellated globe surface.
	 *
	 * @param show true causes the geometry to be shown, false, the default, does not.
	 */
	public void setShowWireframe(boolean show)
	{
		this.showWireframe = show;
	}

	/**
	 * Indicates whether the globe surface's interior geometry is to be drawn.
	 *
	 * @return true if it is to be drawn, otherwise false.
	 */
	public boolean isShowWireframe()
	{
		return this.showWireframe;
	}

	/**
	 * Indicates whether the bounding volumes of the tessellated globe's surface geometry should be displayed.
	 *
	 * @return true if the bounding volumes are to be drawn, otherwise false.
	 */
	public boolean isShowTessellationBoundingVolumes()
	{
		return showTessellationBoundingVolumes;
	}

	/**
	 * Specifies whether the bounding volumes of the globes tessellated surface geometry is to be drawn.
	 *
	 * @param showTessellationBoundingVolumes
	 *         true if the bounding volumes should be drawn, false, the default, if not.
	 */
	public void setShowTessellationBoundingVolumes(boolean showTessellationBoundingVolumes)
	{
		this.showTessellationBoundingVolumes = showTessellationBoundingVolumes;
	}

	@Override
	public boolean isShowTessellationTileIds() {
		return this.showTessellationTileIds;
	}

	@Override
	public void setShowTessellationTileIds(boolean showTileIds) {
		this.showTessellationTileIds = showTileIds;
	}
}
