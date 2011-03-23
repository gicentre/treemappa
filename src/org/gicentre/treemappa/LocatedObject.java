package org.gicentre.treemappa;

import java.awt.geom.Point2D;

//  *********************************************************************
/** Stores a 2D location which can be attached to any object.
  * @author Jo Wood. giCentre.
  * @version 3.0, 24th February, 2011.
  */ 
//  *********************************************************************

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

public class LocatedObject implements Comparable<LocatedObject>
{
	// ------------------------- Class and object variables -------------------------
	
	private double distance;
	private Point2D point;
	private Comparable obj;
	private boolean sortByObject;
	
	// --------------------------------- Constructor --------------------------------
	
	/** Creates a new location with the given coordinates attached to the given object.
	  * @param x X-coordinate of location.
	  * @param y Y-coordinate of location. 
	  * @param obj Object with which to attach location.
	  */
	public LocatedObject(double x, double y, Comparable obj)
	{
		point = new Point2D.Double(x,y);
		this.obj = obj;
		distance = Math.sqrt(x*x + y*y);
		sortByObject = false;
	}
	
	// ----------------------------------- Methods ----------------------------------
	
	/** Sets any sorting operation to sort either by object (true) or location (false).
	  * @param isObject Object ordered by object type if true, otherwise by distance from origin.
	  */
	public void setSortByObject(boolean isObject)
	{
		sortByObject = isObject;
	}
	
	/** Translates the object by the given coordinates.
	  * @param tx X-coordinate of the translation. 
	  * @param ty Y-coordinate of the translation. 
	  */
	public void translate(double tx, double ty)
	{
		point.setLocation(point.getX()+tx,point.getY()+ty);
		distance = Math.sqrt(point.getX()*point.getX() + point.getY()*point.getY());
	}
	
	/** Reports the coordinates of this object.
	  * @return Location of this object. 
	  */
	public Point2D getLocation()
	{
		return point;
	}
	
	/** Reports the object that has been given a location. This can be null if no object assigned.
	  * @return Object that has been given a location.
	  */
	public Comparable getObject()
	{
		return obj;
	}

    /** Compares this location with the given location. If this location is closer to the origin, it
      * will return +1, if matching the other location's distance, 0, otherwise -1. 
      * @param otherLocation Location with which to make comparison.
      * @return Positive if this object closer to origin, negative if the other is closer, or 0 if equal distance.
      */	
	public int compareTo(LocatedObject otherLocation) 
	{
		int comparison=0;
		if (sortByObject)
		{
			comparison = obj.compareTo(otherLocation.getObject());
		}
		if (comparison != 0)
		{
			return comparison;
		}
		if (distance < otherLocation.distance)
		{
			return -1;
		}
		if (distance > otherLocation.distance)
		{
			return 1;
		}
		return 0;
	}
	
	/** Reports a textual representation of the located object.
	  * @return Textual representation of the located object.
	  */
	public String toString()
	{
		return new String("["+point.getX()+","+point.getY()+"] "+obj.toString());
	}
}