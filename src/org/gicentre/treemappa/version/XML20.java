package org.gicentre.treemappa.version;

import processing.core.PApplet;
import processing.data.XML;

//***************************************************************************************
/** Provides a Processing 1.5 compatible implementation of the version-neutral interface.
 *  @author Jo Wood, giCentre.
 *  @version 3.2.1, 25th April, 2013.
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

public class XML20 implements AbstractXML
{
	// ---------------------------------- Object variables ----------------------------------
	
	private XML xml;
	
	// ------------------------------------ Constructor  ------------------------------------
	
	/** Creates a version-neutral abstracted XML object by loading the XML file with the given name.
	 * @param parent Sketch from which the XML file is to be loaded.
	 * @param fileName Name of file to load.
	 */
	public XML20(PApplet parent, String fileName)
	{
		this(parent.loadXML(fileName));
	}
	
	/** Creates a new abstracted XML element from the given Processing 2.0 XML object.
	 *  @param xml XML object to be wrapped by this version-neutral abstraction.
	 */
	public XML20(XML xml)
	{
		this.xml = xml;
	}
	
	// -------------------------------------- Methods  --------------------------------------

	/** Provides a list of all the XML element's attributes as an array.
	 *  @return List of the element's attributes.
	 */
	public String[] listAttributes() 
	{
		return xml.listAttributes();
	}

	/** Provides the value of the given attribute of the XML element.
	 *  @param attribute Attribute for which a value is to be reported.
	 *  @return Value of the given attribute or null if attribute not found.
	 */
	public String getString(String attribute) 
	{
		return xml.getString(attribute);
	}

	/** Provides the content this XML element.
	 *  @return Content of this XML element or null if no content not found.
	 */
	public String getContent() 
	{
		return xml.getContent();
	}

	/** Provides this XML element's full name.
	 *  @return Name of this XML element.
	 */
	public String getName() 
	{
		return xml.getName();
	}

	/** Provides all of this element's children.
	 *  @return Array of this element's children.
	 */
	public AbstractXML[] getChildren() 
	{
		XML[] children = xml.getChildren();
		AbstractXML[] aChildren = new AbstractXML[children.length]; 
		
		for (int i=0; i<children.length; i++)
		{
			aChildren[i] = new XML20(children[i]);
		}
		return aChildren;
	}
	
}