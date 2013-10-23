/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.pick;

import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: PickedObjectList.java 830 2012-10-08 20:47:53Z tgaskins $
 */
public class PickedObjectList extends ArrayList<PickedObject> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8471685774055837886L;

	public PickedObjectList() {
	}

	public void set(PickedObjectList list) {
		super.clear();
		super.addAll(list);
	}

	public boolean hasNonTerrainObjects() {
		return this.size() > 1 || (this.size() == 1 && this.getTerrainObject() == null);
	}

	public Object getTopObject() {
		PickedObject po = this.getTopPickedObject();
		return po != null ? po.getObject() : null;
	}

	public PickedObject getTopPickedObject() {
		int size = this.size();

		if (1 < size) {
			for (PickedObject po : this) {
				if (po.isOnTop()) return po;
			}
		}

		if (0 < size) { // if we are here, then no objects were mark as 'top'
			return this.get(0);
		}

		return null;
	}

	public PickedObject getTerrainObject() {
		for (PickedObject po : this) {
			if (po.isTerrain()) return po;
		}

		return null;
	}

	public PickedObject getMostRecentPickedObject() {
		return this.size() > 0 ? this.get(this.size() - 1) : null;
	}
}
