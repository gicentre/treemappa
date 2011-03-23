package org.gicentre.treemappa;

// ***********************************************************************
/** Represents the types of layout supported by the TreeMap application.
 *  @author Jo Wood, giCentre.
 *  @version 3.0, 23rd February, 2011.
 */ 
// ***********************************************************************

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

public enum Layout 
{ 
	/** Indicates slice and dice layout. */		SLICE_AND_DICE,
	/** Indicates squarified layout. */			SQUARIFIED,
	/** Indicates ordered squarified layout. */	ORDERED_SQUARIFIED,
	/** Indicates Morton ordered layout. */		MORTON, 
	/** Indicates spatial layout. */				SPATIAL, SPATIAL_AV,
	/** Indicates pivot by middle layout. */		PIVOT_MIDDLE,
	/** Indicates pivot by size layout. */			PIVOT_SIZE,
	/** Indicates pivot by split size layout. */	PIVOT_SPLIT_SIZE,
	/** Indicates pivot by space layout. */		PIVOT_SPACE,
	/** Indicates strip map layout. */				STRIP,
	/** Indicates layout should partition space in horizontal strips.*/ HORIZONTAL,
	/** Indicates layout should partition space in vertical strips.*/ VERTICAL,
	/** Indicates layout is not constrained in a horizontal or vertical direction.*/ FREE;

	public String toString()
	{
		switch (this)
		{
			case SLICE_AND_DICE:
				return "Slice and dice";
			case SQUARIFIED:
				return "Squarified";
			case ORDERED_SQUARIFIED:
				return "Ordered squarified";
			case MORTON:
				return "Morton";
			case SPATIAL:
				return "Spatial";
			case SPATIAL_AV:
				return "Spatial av";
			case PIVOT_MIDDLE:
				return "Pivot by middle";
			case PIVOT_SIZE:
				return "Pivot by size";
			case PIVOT_SPLIT_SIZE:
				return "Pivot by split size";
			case PIVOT_SPACE:
				return "Pivot by space";
			case STRIP:
				return "Strip";
			case HORIZONTAL:
				return "Layout in horizontal rows";
			case VERTICAL:
				return "Layout in vertical columns";
			case FREE:
				return "Unconstrained layout";
		}
		// We shouldn't ever get to this line.
		return super.toString();
	}
}
