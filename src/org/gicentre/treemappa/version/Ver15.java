package org.gicentre.treemappa.version;

import processing.core.PApplet;

//***************************************************************************************
/** Provides a Processing 1.5 compatible implementation of the version-neutral interface.
 *  @author Jo Wood, giCentre.
 *  @version 3.2, 25th April, 2013.
 */
// **************************************************************************************

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

public class Ver15 implements VersionHandler
{
	// ---------------------------------- Object variables ----------------------------------
	
	private PApplet parent;
	
	// ------------------------------------ Constructor  ------------------------------------
	
	/** Creates a Processing 1.5 specific implementation of the abstract processing-neutral behavour.
	 *  @param parent Parent sketch which is to use the processing-neutral methods and classes.
	 */
	public Ver15(PApplet parent)
	{
		this.parent = parent;
		
		// Make a Processing 1.5 specific call to check this is the right implementation.
		@SuppressWarnings("unused")
		processing.xml.XMLElement e;
	}
	
	// -------------------------------------- Methods  --------------------------------------

	/** Draws a rounded rectangle with the given position parameters a-d with corners rounded to radius r.
	 *  @param a First rectangle parameter (depends on the current rectMode() setting.
	 *  @param b Second rectangle parameter (depends on the current rectMode() setting.
	 *  @param c Third rectangle parameter (depends on the current rectMode() setting.
	 *  @param d Fourth rectangle parameter (depends on the current rectMode() setting.
	 *  @param r Radius of curvature of the corners of the rectangle.
	 */
	public void rect(float a, float b, float c, float d, float r)
	{
		parent.rect(a, b, c, d,r,r);
	}

	/** Creates a processing version-neutral root XML element based on the given XML file.
	 *  @param sketch Sketch wishing to load the XML file.
	 *  @param fileName Name of XML file to load.
	 */
	public AbstractXML createXML(PApplet sketch, String fileName) 
	{
		return new XML15(sketch,fileName);
	}
}
