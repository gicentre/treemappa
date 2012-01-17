package org.gicentre.tests;

import org.gicentre.handy.HandyRenderer;
import org.gicentre.treemappa.PTreeMappa;
import org.gicentre.treemappa.gui.DrawableFactory;
import org.gicentre.utils.move.ZoomPan;
import processing.core.PApplet;
import processing.core.PImage;

//  ****************************************************************************************
/** Tests the Processing wrapper for the treeMappa classes.
 *  @author Jo Wood, giCentre, City University London.
 *  @version 1.0, 17th January, 2012.
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
public class TreeMapProcessingTest extends PApplet
{
	// ------------------------------ Starter method ------------------------------- 

	/** Runs the sketch as an application.
	 *  @param args Command line arguments (ignored). 
	 */
	public static void main(String[] args)
	{   
		PApplet.main(new String[] {"org.gicentre.tests.TreeMapProcessingTest"});
	}

	// ----------------------------- Object variables ------------------------------

	private PTreeMappa pTreeMappa;    // Stores the treemap.
	private ZoomPan zoomer;			  // Allows zooming and panning of the treemap.
	
	// ---------------------------- Processing methods -----------------------------

	/** Initialises the sketch.
	 */
	public void setup()
	{   
		size(800,500);
		smooth();
		zoomer = new ZoomPan(this);

		// Create an empty treemap.    
		pTreeMappa = new PTreeMappa(this);
		
		// Create a sketchy renderer
		HandyRenderer handy = new HandyRenderer(this);
		handy.setFillGap(2);
		handy.setFillWeight(0.1f);

		// Load the data and build the treemap.
		pTreeMappa.readData("life.csv"); 
		
		// Customise the appearance of the treemap
		pTreeMappa.getTreeMapPanel().setLeafMaxTextSize(24);
		pTreeMappa.getTreeMapPanel().setBorders(20);
		pTreeMappa.getTreeMapPanel().setBorder(2, 8);
		pTreeMappa.getTreeMapPanel().setShowLeafBorders(true);
		pTreeMappa.getTreeMapPanel().setBorderWeights(2);
		pTreeMappa.getTreeMapPanel().setLeafBorderWeight(2);
		pTreeMappa.getTreeMapPanel().setLeafBorderColour(color(0,100));
		

		// Layout needs updating because we have changed border size and the
		// treemap layout algorithm.
		pTreeMappa.getTreeMapPanel().updateLayout();
		
		// Make treeMappa use the sketchy renderer and a handwritten font.
		textFont(createFont("DolceCaffe-Regular",40));
		pTreeMappa.setRenderer(DrawableFactory.createHandyRenderer(handy));
	}

	/** Draws the shapefile data in the sketch.
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

	/** Updates the display whenever the mouse is moved.
	 */
	@Override
	public void mouseMoved()
	{
		loop();
	}

	/** Updates the display whenever the mouse is dragged
	 */
	@Override
	public void mouseDragged()
	{
		loop();
	}
}
