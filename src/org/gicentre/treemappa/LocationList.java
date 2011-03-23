package org.gicentre.treemappa;

import java.awt.geom.Point2D;
import java.util.*;

//  ***************************************************************************************
/** Stores a list of objects each be associated with a location. Allows spatial queries of 
 *  the list (find closest item to (x,y)). 
 *  @author Jo Wood, giCentre.
 *  @version 3.0, 24th February, 2011.
 */ 
//  ***************************************************************************************

/* This file is part of the giCentre treeMappa library. treeMappa is free software: you can 
 * redistribute it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * treeMappa is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this
 * source code (see COPYING.LESSER included with this source code). If not, see 
 * http://www.gnu.org/licenses/.
 */

public class LocationList
{
	// -------------------- Object and class variables -------------------

	private static final long serialVersionUID = -6397433758641389862L;
	
	private Vector<LocatedObject> locations;
	
	// --------------------------- Constructor ---------------------------
	
	/** Creates the collection.
	  */
	public LocationList()
	{
		super();
		locations= new Vector<LocatedObject>();
	}

	// ----------------------------- Methods -----------------------------
	
	/** Adds the given object to the collection.
	  * @param locObj New located object to add to the collection. 
	  */
	public void add(LocatedObject locObj)
	{
		locations.add(locObj);
	}
	
	/** Removes the given object from the collection.
	  * @param locObj Object to remove from the collection. 
	  * @return True if object found and removed successfully.
	  */
	public boolean remove(LocatedObject locObj)
	{
		return locations.remove(locObj);
	}
	
	/** Removes the object at the given position in the collection.
	  * @param index Position in list at which to remove the object. 
	  */
	public void remove(int index)
	{
		locations.remove(index);
	}
	
	/** Clears the contents of the list. 
	  */
	public void clear()
	{
		locations.clear();
	}
	
	/** Gets the object at the given position in the collection.
	  * @param index Position in list at which to find the object. 
	  * @return Object at given index.
	  */
	public LocatedObject get(int index)
	{
		return locations.get(index);
	}
	
	/** Reports the number of items stored in the list.
	  * @return Number of located objects in list.
	  */
	public int size()
	{
		return locations.size();
	}
	
	/** Sorts the locations by the descending natural order of the objects that are attached to them.
	  * If two objects are identical, they are sorted by ascending distance from origin. 
	  */
	public void sortByObject()
	{
		for (LocatedObject locObj : locations)
		{
			locObj.setSortByObject(true);
		}
		Collections.sort(locations);
		Collections.reverse(locations);
	}
	
	/** Sorts the locations by distance from the given point. 
	  * @param p Point from which to measure distance. 
	  */
	public void sortByDistance(Point2D p)
	{
		// Translate so new origin is at p before sorting.
		for (LocatedObject locObj : locations)
		{
			locObj.translate(-p.getX(),-p.getY());
			locObj.setSortByObject(false);
		}
		Collections.sort(locations);
		
		// Translate back again.
		for (LocatedObject locObj : locations)
		{
			locObj.translate(p.getX(),p.getY());
		}		
	}
	
	/** Retrieves the object closest to the given location. 2D Euclidean distance used.
	  * @param location Location to query.
	  * @return Object closest to the given position or null if no located objects in collection.
	  */
	public LocatedObject getClosest(Point2D location)
	{
		if (locations.size() == 0)
		{
			return null;
		}
		double shortestDistance = Float.MAX_VALUE;
		LocatedObject closestObject = null;
		double px = location.getX();
		double py = location.getY();
		
		for (int i=0; i<locations.size(); i++)
		{
			LocatedObject locObj = locations.get(i); 
			Point2D loc = locObj.getLocation();
			double dist = (loc.getX()-px)*(loc.getX()-px) + (loc.getY()-py)*(loc.getY()-py);

			if (dist < shortestDistance)
			{
				shortestDistance = dist;
				closestObject = locObj;
			}
		}
		return closestObject;	
	}
	
	/** Reports a textual representation of the list.
	  * @return Textual representation of the list.
	  */
	public String toString()
	{
		StringBuffer output = new StringBuffer();
		
		for (LocatedObject locObj : locations)
		{
			output.append(locObj);
			output.append("\n");
		}
		return output.toString();
	}
}