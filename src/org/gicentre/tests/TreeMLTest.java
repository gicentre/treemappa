package org.gicentre.tests;

import org.gicentre.treemappa.PTreeMappa;
import org.gicentre.utils.move.ZoomPan;
import processing.core.PApplet;

//  ****************************************************************************************
/** Tests reading a TreeML format file and creating a deep, unbalanced treemap.
 *  @author Jo Wood, giCentre, City University London.
 *  @version 3.2.1, 3rd April, 2014.
 */ 
//  ****************************************************************************************

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

@SuppressWarnings("serial")
public class TreeMLTest extends PApplet
{
	// ------------------------------ Starter method ------------------------------- 

	/** Runs the sketch as an application.
	 *  @param args Command line arguments (ignored). 
	 */
	public static void main(String[] args)
	{   
		PApplet.main(new String[] {"org.gicentre.tests.TreeMLTest"});
	}

	// ----------------------------- Object variables ------------------------------

	private PTreeMappa pTreeMappa;    // Stores the treemap.
	private ZoomPan zoomer;			  // Allows zooming and panning of the treemap.

	// ---------------------------- Processing methods -----------------------------

	/** Initialises the sketch.
	 */
	public void setup()
	{   
		size(1000,700);
		zoomer = new ZoomPan(this);  

		// Display labels in a serif font
		textFont(createFont("serif",40));

		// Create an empty treemap.    
		pTreeMappa = new PTreeMappa(this);

		// Load the data and build the treemap.
		pTreeMappa.readData("ontology.xml","treeML"); 

		// Customise the appearance of the treemap 
		pTreeMappa.getTreeMapPanel().setShowBranchLabels(true);
		pTreeMappa.getTreeMapPanel().setLeafMaxTextSize(4);
		pTreeMappa.getTreeMapPanel().setAllowVerticalLabels(true);
		pTreeMappa.getTreeMapPanel().setBranchTextColours(color(0,50));

		pTreeMappa.getTreeMapPanel().setBorders(0);
		pTreeMappa.getTreeMapPanel().setBorderColour(color(255));

		pTreeMappa.getTreeMapPanel().setLayouts("sliceAndDice");

		// Layout needs updating because we have changed border size and the treemap layout algorithm.
		pTreeMappa.getTreeMapPanel().updateLayout();
	}

	/** Draws the sketch.
	 */
	public void draw()
	{   
		background(255);

		// Allow zooming and panning.
		zoomer.transform();

		// Get treemappa to draw itself.
		pTreeMappa.draw();
		
		noLoop();		// Only redraw when zooming/panning.
	}

	/** Updates the display whenever the mouse is dragged
	 */
	@Override
	public void mouseDragged()
	{
		loop();
	}
}
