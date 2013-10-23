/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.LayerList;

/**
 * @author dcollins
 * @version $Id: Model.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface Model extends WWObject
{
    Globe getGlobe();

    void setGlobe(Globe globe);

    LayerList getLayers();

    void setLayers(LayerList layers);
}
