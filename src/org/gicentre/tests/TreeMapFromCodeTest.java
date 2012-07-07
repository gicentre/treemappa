package org.gicentre.tests;

import org.gicentre.treemappa.PTreeMappa;
import org.gicentre.treemappa.TreeMapNode;
import org.gicentre.treemappa.TreeMappa;
import org.gicentre.utils.move.ZoomPan;
import processing.core.PApplet;

//  ****************************************************************************************
/** Creates a treemap programmatically. Tests the ability to pass data into a treemap without
 *  reading files as well as custom appearance in Processing.
 *  @author Jo Wood, giCentre, City University London.
 *  @version 1.0, 7th July, 2012.
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
public class TreeMapFromCodeTest extends PApplet
{
	// ------------------------------ Starter method ------------------------------- 

	/** Runs the sketch as an application.
	 *  @param args Command line arguments (ignored). 
	 */
	public static void main(String[] args)
	{   
		PApplet.main(new String[] {"org.gicentre.tests.TreeMapFromCodeTest"});
	}

	// ----------------------------- Object variables ------------------------------

	private PTreeMappa pTreeMappa;    // Stores the treemap.
	private ZoomPan zoomer;			  // Allows zooming and panning of the treemap.
	
	
	// ---------------------------- Processing methods -----------------------------
	
	/** Initialises the sketch.
	 */
	public void setup()
	{   
		size(1200,800);
		smooth();
		zoomer = new ZoomPan(this);

		// Create the treemap.    
		pTreeMappa = new PTreeMappa(this);
		buildTreeMap();		
		
		textFont(createFont("GeosansLight",8));
		
		// Customise the appearance of the treemap
		pTreeMappa.getTreeMapPanel().setLeafMaxTextSize(24);
		pTreeMappa.getTreeMapPanel().setBorders(6);					// Default border spacing 6 pixels
		pTreeMappa.getTreeMapPanel().setShowBranchLabels(true);
		pTreeMappa.getTreeMapPanel().setAllowVerticalLabels(true);
		pTreeMappa.getTreeMapPanel().setShowLeafBorders(true);
		pTreeMappa.getTreeMapPanel().setBorderWeights(2);			// Default border line thickness is 2 pixels
		pTreeMappa.getTreeMapPanel().setLeafBorderWeight(2);		// Border round leaf nodes is 2 pixels.
		pTreeMappa.getTreeMapPanel().setLeafBorderColour(color(2,0));
		pTreeMappa.getTreeMapPanel().setLayouts("orderedSquarified");
		
		// These are the Processing-specific customisation options.
		pTreeMappa.setCurvature(9);									// Rounded rectangles.
		pTreeMappa.setLeafTextAlignment(LEFT, BOTTOM);				// Place labels in the bottom-left corner of each leaf.
		pTreeMappa.setBranchTextAlignment(CENTER, CENTER);
	
		// Layout needs updating because we have changed border size and the treemap layout algorithm.
		pTreeMappa.getTreeMapPanel().updateLayout();
	}

	/** Draws the sketch.
	 */
	public void draw()
	{   
		background(255);
		zoomer.transform();

		// Get treeMappa to draw itself.
		pTreeMappa.draw();
		
		// Don't redraw unless instructed to do so.
		noLoop();
	}

	/** Updates the display whenever the mouse is dragged
	 */
	@Override
	public void mouseDragged()
	{
		loop();
	}
	
	// ------------------------------ Private methods ------------------------------
	
	private void buildTreeMap()
	{
		TreeMappa tm = pTreeMappa.getTreeMappa();
		
		// Create Root node
		TreeMapNode root = new TreeMapNode("root");
		tm.setRoot(root);
		
		root.add(new TreeMapNode("First Child\\nwith two lines"));
		root.add(new TreeMapNode("Second Child with a longer\\ntext label"));
		
		
		TreeMapNode child3 = new TreeMapNode("Third Child");
		root.add(child3);
		child3.add(new TreeMapNode("Grandchild A",8));
		child3.add(new TreeMapNode("Grandchild B",5));
		
		// Update treemap with new data
		pTreeMappa.buildTreeMap();		
	}
}
