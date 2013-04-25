package org.gicentre.treemappa.version;

import processing.core.PApplet;

//  ********************************************************************************
/** Interface describing Processing version neutral versions of methods that are 
 *  incompatible between Processing 1.x and 2.x. 
 *  @author Jo Wood, giCentre.
 *  @version 3.2, 25th April, 2013.
 */
//  ********************************************************************************

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

public interface VersionHandler 
{
	/** Draws a rounded rectangle with the given position parameters a-d with corners rounded to radius r.
	 *  @param a First rectangle parameter (depends on the current rectMode() setting.
	 *  @param b Second rectangle parameter (depends on the current rectMode() setting.
	 *  @param c Third rectangle parameter (depends on the current rectMode() setting.
	 *  @param d Fourth rectangle parameter (depends on the current rectMode() setting.
	 *  @param r Radius of curvature of the corners of the rectangle.
	 */
	public abstract void rect(float a, float b, float c, float d, float r);
	
	/** Should create a processing version-neutral root XML element based on the given XML file.
	 *  @param sketch Sketch wishing to load the XML file.
	 *  @param fileName Name of XML file to load.
	 */
	public abstract AbstractXML createXML(PApplet sketch, String fileName);	
}


