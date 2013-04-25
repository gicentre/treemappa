package org.gicentre.treemappa.version;

//  ******************************************************************************
/** Interface describing a Processing version-neutral XML class. 
 *  @author Jo Wood, giCentre.
 *  @version 3.2, 25th April, 2013.
 */
//  ******************************************************************************

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

public interface AbstractXML 
{
	/** Should provide a list of all the XML element's attributes as an array.
	 *  @return List of the element's attributes.
	 */
	public abstract String[] listAttributes();
	
	/** Should provide the value of the given attribute of the XML element.
	 *  @param attribute Attribute for which a value is to be reported.
	 *  @return Value of the given attribute or null if attribute not found.
	 */
	public abstract String getString(String attribute);
	
	/** Should provide the content this XML element.
	 *  @return Content of this XML element or null if no content not found.
	 */
	public abstract String getContent();
	
	/** Should provide this XML element's full name.
	 *  @return Name of this XML element.
	 */
	public abstract String getName();
	
	/** Should provide all of this element's children.
	 * @return Array of this element's children.
	 */
	public AbstractXML[] getChildren();
}

