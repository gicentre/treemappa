package org.gicentre.treemappa;

import java.awt.Point;
import java.util.Vector;

//  **************************************************************************************************
/** Stores a one-dimensional ordered list of objects that can represent a two-dimensional arrangement
 *  using Morton ordering. The order of objects stored in this collection is assumed to be Morton. 
 *  Ordering can be <code>VERTICAL</code> (mirror 'N' shaped) or <code>HORIZONTAL</code> ('Z' shaped).
  * @author Jo Wood, giCentre.
  * @version 3.2, 24th February, 2011.
  * @param <E> Type of object stored in the Morton ordered collection. 
  */ 
//  **************************************************************************************************

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

public class MortonList<E> extends Vector<E>
{
	// -------------------- Object and class variables -------------------
	
	private static final long serialVersionUID = 1464385625231719807L;
	
							/** Indicates that Morton ordering is mirror 'N' shaped. */
	public static final int VERTICAL = 1;
							/** Indicates that Morton ordering is 'Z' shaped. */
	public static final int HORIZONTAL = 2;	
	
	private int direction;
	

	// --------------------------- Constructor ---------------------------
	
	/** Creates the collection assuming a vertical Morton layout.
	  */
	public MortonList()
	{
		this(VERTICAL);
		
	}
	
	/** Creates the collection with a Morton layout in the given direction.
	  * @param direction Direction of layout. Should be one of <code>VERTICAL</code>
	  * (mirrored 'N' shaped) or <code>HORIZONTAL</code> ('Z' shaped). 
	  */
	public MortonList(int direction)
	{
		super();
		this.direction = direction;
	}

	// ----------------------------- Methods -----------------------------
	
	/** Clears this collection and sets the given direction for Morton ordering.
	  * @param mDirection New direction for Morton ordering. Should be one of <code>VERTICAL</code>
	  * (mirrored 'N' shaped) or <code>HORIZONTAL</code> ('Z' shaped). 
	  */
	public void resetDirection(int mDirection)
	{
		clear();
		this.direction = mDirection;
	}
	
	/** Returns the x,y position of the first occurrence of the given object in this collection,
	  * or null if the collection does not contain the object.
	  * @param obj Object whose position will be reported.
	  * @return 2-dimensional position of the object.
	  */
	public Point positionOf(Object obj)
	{
		int index = indexOf(obj);
		if (index < 0)
		{
			return null;
		}
		
		return new Point(getX(index),getY(index));	
	}
	
	
	/** Returns the object at the given x,y position or null if no object at the given position.
	  * Note that it is not possible to distinguish between a null object at a given position
	  * and position that it out of bounds of the current collection.
	  * @param x x coordinate of the position from which to retrieve the object.
	  * @param y x coordinate of the position from which to retrieve the object. 
	  * @return Object at the given position or null if out of bounds or no object.
	  */
	public E get(int x, int y)
	{
		int morton = getMorton(x, y);
		if (morton >= size())
		{
//System.err.println("Position out of bounds: Morton number for "+x+","+y+" is "+morton+" but collection only has "+size()+" elements.");			
			return null;
		}
		
		return get(morton);
	}
	
	/** Returns the object that is to the 'right' (x+1) of the first instance of the given reference object
	  * or null if no object to the right or if the given reference object not found. 
	  * @param obj Reference object from which to find its neighbour.
	  * @return Object to the right of the given reference object.
	  */
	public E getNextX(Object obj)
	{
		Point position = positionOf(obj);
		
		if (position == null)
		{
//System.err.println("getNextX(): Object "+obj+" not found in collection.");			
			return null;
		}
		return get(position.x+1,position.y);
	}
	
	
	/** Returns the object that is 'below' (y+1) the first instance of the given reference object
	  * or null if no object below or if the given reference object not found. 
	  * @param obj Reference object from which to find its neighbour.
	  * @return Object below the given reference object.
	  */
	public E getNextY(Object obj)
	{
		Point position = positionOf(obj);
		
		if (position == null)
		{
//System.err.println("getNextY(): Object "+obj+" not found in collection.");			
			return null;
		}
		return get(position.x,position.y+1);
	}
			
	/** Reports the x coordinate of the position represented by the given Morton number.
	  * @param mortonNumber Number to process.
	  * @return x coordinate represented by the Morton number.
	  */
	public int getX(int mortonNumber)
	{
		int x=0;
		
		if (direction == HORIZONTAL)
		{
			for (int i=0; i<32; i+=2)
			{
				int mask = 1 << (i);
				x += (mortonNumber&mask)>>(i/2);
			}
		}
		else
		{
			for (int i=1; i<32; i+=2)
			{
				int mask = 1 << (i);
				x += (mortonNumber&mask)>>((i+1)/2);
			}
		}
		
		return x;
	}
	
	/** Reports the y coordinate of the position represented by the given Morton number.
	  * @param mortonNumber Number to process.
	  * @return y coordinate represented by the Morton number.
	  */
	public int getY(int mortonNumber)
	{
		int y=0;
		
		if (direction == HORIZONTAL)
		{
			for (int i=1; i<32; i+=2)
			{
				int mask = 1 << (i);
				y += (mortonNumber&mask)>>((i+1)/2);
			}
		}
		else
		{
			for (int i=0; i<32; i+=2)
			{
				int mask = 1 << (i);
				y += (mortonNumber&mask)>>(i/2);
			}
		}
		
		return y;
	}
	
	/** Reports the Morton number representing the given x,y coordinate pair.
	  * @param x x coordinate of position to calculate.
	  * @param y y coordinate of position to calculate.
	  * @return Morton number representing the given position. 
	  */
	public int getMorton(int x, int y)
	{
		int newX = x;
		int newY = y;
		int morton = 0;
		
		if (direction == HORIZONTAL)
		{
			// Swap x and y coordinates for horizontal ordering.
			newX = y;
			newY = x;
		}
		
		for (int i=0; i<16; i++)
		{
			int mask = 1 << (i);
			morton += (newX&mask)<<(i+1);
			morton += (newY&mask)<<i;
		}	
		return morton;
	}
}