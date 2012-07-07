package org.gicentre.treemappa;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.gicentre.treemappa.gui.Drawable;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.io.DOMProcessor;
import org.w3c.dom.Node;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.xml.XMLElement;

//********************************************************************************
/** Wrapper class to allow Processing sketches to load, create and draw treemaps. 
 *  @author Jo Wood, giCentre.
 *  @version 3.1.1, 7th July, 2012.
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

public class PTreeMappa
{
	// ------------------------------------ Object variables ------------------------------------
	
	PApplet parent;							// Processing sketch that will use the treemap.
	private TreeMappa treeMappa;			// TreeMappa object doing the treemapping stuff.
	private TreeMapPanel tmPanel;			// Panel for displaying treemaps.
	private Drawable renderer;		    	// Alternative renderer for sketchy graphics and other styles.
	private float curveRadius;				// Curve radius for rounded rectangles.
	private int leafAlignX,leafAlignY;		// Text alignment of leaf labels.
	private int branchAlignX,branchAlignY;	// Text alignment of leaf labels.
	
	// -------------------------------------- Constructors --------------------------------------
	
	/** Creates an object capable of building and representing a treemap.
	 *  @param parent Sketch in which this treemap will be used.
	 */
	public PTreeMappa(PApplet parent)
	{
		this(parent,null);
	}
	
	/** Creates an object capable of building and representing a treemap with the given default image dimensions.
	 *  @param parent Sketch in which this treemap will be used.
	 *  @param w Initial width of the treemap in screen coordinates. If 0, the width of the parent sketch is used.
	 *  @param h Initial height of the treemap in screen coordinates. If 0, the height of the parent sketch is used.
	 */
	public PTreeMappa(PApplet parent, int w, int h)
	{
		this(parent,null,w,h);
	}
		
	/** Creates a treemap from the details supplied in the given configuration file.
	 *  @param parent Sketch in which this treemap will be used.
	 *  @param configFileName Name of file containing the treemap configuration.
	 */
	public PTreeMappa(PApplet parent, String configFileName)
	{
		this(parent,configFileName,0,0);
	}
		
	/** Creates a treemap of the given display dimensions from the details supplied in the given configuration file.
	 *  If the given width and height values are greater than 0, they will override any provided by the configuration file.
	 *  @param parent Sketch in which this treemap will be used.
	 *  @param configFileName Name of file containing the treemap configuration.
	 *  @param w Initial width of the treemap in screen coordinates. If 0, the width provided in the configuration file is used.
	 *  @param h Initial height of the treemap in screen coordinates. If 0, the height provided in the configuration file is used.
	 */
	public PTreeMappa(PApplet parent, String configFileName, int w, int h)
	{
		this.parent = parent;
		this.curveRadius = 0;
		
		leafAlignX = PConstants.CENTER;
		leafAlignY = PConstants.CENTER;
		branchAlignX = PConstants.CENTER;
		branchAlignY = PConstants.CENTER;
		
		TreeMapProperties props = new TreeMapProperties();
		treeMappa = new TreeMappa(props);
		
		// Default size is that of the sketch unless it is specified in a user config file.
		treeMappa.getConfig().setParameter(TreeMapProperties.WIDTH, Integer.toString(parent.width));
		treeMappa.getConfig().setParameter(TreeMapProperties.HEIGHT, Integer.toString(parent.height));
				
		InputStream configStream = parent.createInput(configFileName);
		if (configStream != null)
		{
			props.load(configStream);
		}
		
		// Set the treemap panel dimensions if not default.
		if (w > 0)
		{
			treeMappa.getConfig().setParameter(TreeMapProperties.WIDTH, Integer.toString(w));
		}
		if (h > 0)
		{
			treeMappa.getConfig().setParameter(TreeMapProperties.HEIGHT, Integer.toString(h));
		}
	}
	
	// ---------------------------------------- Methods ----------------------------------------
	
	/** Reads in CSV tree data from the Processing sketch's data folder. If you need to read data
	 *  from other locations, use TreeMappa's file reading methods instead.
	 *  @param dataFileName Name of CSV file in the data folder representing the tree data.
	 */
	public void readData(String dataFileName)
	{		
		readData(dataFileName,"csv");
	}
	
	/** Reads in tree data in the given format from the Processing sketch's data folder. If you 
	 *  need to read data from other locations or need to control format options, use TreeMappa's
	 *  file reading methods instead.
	 *  @param dataFileName Name of file in the data folder representing the tree data.
	 *  @param fileFormat Format of data file, can be 'csv', 'csvSpatial', 'csvCompact' or 'treeML;
	 */
	public void readData(String dataFileName, String fileFormat)
	{		
		// Set default file reading properties.
		treeMappa.getConfig().setParameter(TreeMapProperties.TEXT_ONLY, "false");
		treeMappa.getConfig().setParameter(TreeMapProperties.FILE_TYPE, fileFormat);
		treeMappa.getConfig().setParameter(TreeMapProperties.USE_LABELS, "true");
		//treeMappa.getConfig().setParameter(TreeMapProperties.IN_FILE,parent.dataPath(dataFileName));
		treeMappa.getConfig().setParameter(TreeMapProperties.IN_FILE,dataFileName);
				
		treeMappa.readData(this);
		treeMappa.buildTreeMap();
						
		// Use current fill setting if no colour table supplied.
		if (treeMappa.getConfig().getCTableFileName() == null)
		{
			ColourTable pColourTable = new ColourTable();
			pColourTable.addContinuousColourRule(0, (int)parent.red(parent.g.fillColor),
					                                (int)parent.green(parent.g.fillColor), 
					                                (int)parent.blue(parent.g.fillColor),
					                                (int)parent.alpha(parent.g.fillColor));
			treeMappa.setDefaultColourTable(pColourTable);
		}
		
		// Create a default panel the size of the parent sketch.
		tmPanel = treeMappa.createPanel();
	}
	
	/** Builds the treemap from the hierarchical data stored in this object. This method should be called in preference
	 *  to <ode>buildTreeMap()</code> in the <code>TreeMappa</code> class since it will update the treemap panel used
	 *  for drawing in Processing.
	 *  @return True if the tree has been built without problems.
	 */
	public boolean buildTreeMap()
	{
		if (treeMappa.buildTreeMap() == false)
		{
			return false;
		}
		
		tmPanel = treeMappa.createPanel();
		return true;
	}
		
	/** Draws and provides an image containing the current treemap. Note that this will update the treemap with
	 *  the current layout settings so should not be called repeatedly if no changes have been made since the
	 *  previous call. Instead, call it once and store the resulting image as a PImage variable.
	 *  @return Image representing the treemap.
	 */
	public PImage createImage()
	{
		tmPanel.updateLayout();
		tmPanel.updateImage();
		BufferedImage bImage = tmPanel.getImage();
		
		// Convert Buffered Image into a Processing PImage
		PImage pImage = new PImage(bImage.getWidth(),bImage.getHeight(),PConstants.ARGB);
		bImage.getRGB(0, 0, pImage.width, pImage.height, pImage.pixels, 0, pImage.width);
	    pImage.updatePixels();
	    return pImage;
	}
	
	/** Draws the treemap directly in the parent sketch at the given position and size.
	 *  @param x Coordinate of the left hand side of the treemap in pixel units.
	 *  @param y Coordinate of the top of the treemap in pixel units.
	 *  @param w Width of the treemap in pixel units.
	 *  @param h Height of the treemap in pixel units.
	 */
	public void draw(float x, float y, float w, float h)
	{
		parent.pushMatrix();

		parent.translate(x,y);
		parent.scale(w/tmPanel.getWidth(),h/tmPanel.getHeight());
		draw();
		parent.popMatrix();
	}
	
	/** Draws the treemap directly in the parent sketch. This is an alternative to calling <code>createImage()</code> 
	 *  and will use the currently selected font and stroke from the parent sketch. Treemap nodes are filled
	 *  according to the colour rules set for the treemap.
	 */
	public void draw()
	{
		parent.pushStyle();
		parent.textSize(40);
		float textPadding = parent.textWidth("i");
				
		for (NodePanel leaf : tmPanel.getLeaves())
		{
			if (leaf.isDummy())
			{	
				// Do not display dummy nodes.
				continue;
			}

			// Fill leaf background.
			Rectangle2D bounds = leaf.getBounds();
			
			if ((bounds.getWidth() <=1) || (bounds.getHeight() <=1))
			{	
				// Do not display sub-pixel nodes.
				continue;
			}
			
			parent.fill(leaf.getColour().getRGB());
				
			if (tmPanel.getShowLeafBorders())
			{
				parent.stroke(tmPanel.getLeafBorderColour().getRGB());	
				
				float borderWeight = tmPanel.getLeafBorderWeight();
				if (borderWeight < 0)
				{
					// Negative weights are given a thin line.
					borderWeight = 0.1f;
				}
				
				if (borderWeight > 0)
				{
					parent.strokeWeight(borderWeight);
				}
			}
			else
			{
				// For backward compatibility, non-border rendering is with a thin, pale border.
				parent.strokeWeight(0.1f);
				parent.stroke(tmPanel.getBorderColour().getRGB(),25);
			}
			
			if (renderer == null)
			{
				parent.rect((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight(),curveRadius,curveRadius);
			}
			else
			{
				renderer.rect((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight(),curveRadius,curveRadius);
			}

			// Draw leaf label.
			if (tmPanel.getShowLeafLabels() && bounds.getWidth() > 1)
			{
				String[] lines = leaf.getLabel().split("\\\\n");				
				parent.fill(tmPanel.getLeafTextColour().getRGB());
								
				float maxWidth=0;
				for (String line : lines)
				{
					maxWidth = Math.max(maxWidth, parent.textWidth(line));
				}

				float lineHeight = parent.textAscent() + parent.textDescent();
				float totalHeight = lines.length*lineHeight;

				// Work out the scalings required to fit text both vertically and horizontally.        
				double horizXScale = bounds.getWidth() / maxWidth;
				double horizYScale = bounds.getHeight() / totalHeight;
				double horizScale = horizXScale;
				double vertXScale = bounds.getWidth() / totalHeight;
				double vertYScale = bounds.getHeight() / maxWidth;
				double vertScale = vertXScale;

				horizScale = Math.min(horizXScale, horizYScale);
				vertScale  = Math.min(vertXScale, vertYScale);

				horizScale = (float)(horizScale*0.7 + (0.3*bounds.getWidth()*bounds.getHeight()) / tmPanel.getRootArea());
				vertScale = (float)(vertScale*0.7 + (0.3*bounds.getWidth()*bounds.getHeight()) / tmPanel.getRootArea());

				if (tmPanel.getMaxLeafText() > 0)
				{
					if (horizScale > tmPanel.getMaxLeafText()/40)
					{
						// No need to try vertical if text is already large enough.
						horizScale = tmPanel.getMaxLeafText()/40;
						vertScale = 0;
					}
					else if (vertScale > tmPanel.getMaxLeafText()/40)
					{
						vertScale = tmPanel.getMaxLeafText()/40;
					}
				}

				for (int i=0; i<lines.length; i++)
				{
					parent.pushMatrix();
					double x=0,y=0;

					// Only use vertical text if it increases text size by at least 20% and is allowed.
					if ((tmPanel.getAllowVerticalLabels()) && (vertScale > horizScale*1.2))
					{
						double cx,cy;						
						int xAlign = leafAlignX;
						int yAlign = leafAlignY;
						
						if (leafAlignX == PConstants.LEFT)
						{
							cx = bounds.getX()+ vertScale*((lines.length-i-1)*lineHeight) + textPadding;
							yAlign = PConstants.BOTTOM;
						}
						else if (leafAlignX == PConstants.RIGHT)
						{
							cx = bounds.getX()+bounds.getWidth() - vertScale*i*lineHeight - textPadding;
							yAlign = PConstants.TOP;
						}
						else
						{
							cx = bounds.getX() + (bounds.getWidth()/2) - vertScale*((i+1)*lineHeight -totalHeight/2 -parent.textDescent());
							yAlign = PConstants.BOTTOM;
						}
												
						if (leafAlignY == PConstants.TOP)
						{
							cy = bounds.getY()+textPadding;
							xAlign = PConstants.LEFT;
						}
						else if (leafAlignY == PConstants.BOTTOM)
						{
							cy = bounds.getY()+bounds.getHeight()-textPadding;
							xAlign = PConstants.RIGHT;
						}
						else
						{
							cy = bounds.getY() + (bounds.getHeight()-parent.textWidth(lines[i])*vertScale)/2;
							xAlign = PConstants.LEFT;
						}
						
						parent.textAlign(xAlign,yAlign);				
						parent.translate((float)cx,(float)cy);
						parent.rotate(PConstants.HALF_PI);
						parent.scale((float)vertScale,(float)vertScale);
					}
					else
					{
						// Use horizontal text.						
						if (leafAlignX == PConstants.LEFT)
						{
							x = bounds.getX() + textPadding;
						}
						else if (leafAlignX == PConstants.RIGHT)
						{
							x = bounds.getX() + bounds.getWidth() - textPadding;
						}
						else
						{
							x = bounds.getX() + bounds.getWidth()/2;
						}
						
						if (leafAlignY == PConstants.TOP)
						{
							y = bounds.getY() + horizScale*i*lineHeight+ textPadding;
						}
						else if (leafAlignY == PConstants.BOTTOM)
						{
							y = bounds.getY() + (bounds.getHeight() - horizScale*(lines.length-i-1)*lineHeight - textPadding);
						} 
						else
						{
							y = bounds.getY() + (bounds.getHeight()/2) + horizScale*((i+1)*lineHeight -totalHeight/2 -parent.textDescent());
						}
						
						parent.textAlign(leafAlignX,leafAlignY);
						parent.translate((float)x,(float)y);
						parent.scale((float)horizScale,(float)horizScale);
					}

					parent.text(lines[i],0,0);
					parent.popMatrix();
				}				
			}
		}

		for (NodePanel branch : tmPanel.getBranches())
		{
			if (branch.isDummy())
			{	
				// Do not display dummy nodes.
				continue;
			}

			Rectangle2D bounds = branch.getBounds();
			int level = branch.getLevel();
			
			// Draw branch label.
			if ((tmPanel.getShowBranchLabels()) && bounds.getWidth() > 1)
			{
				String[] lines = branch.getLabel().split("\\\\n");
				
				// Draw text in original fill colour.
				parent.fill(tmPanel.getBranchTextColours()[level-1].getRGB());
				
				float maxWidth=0;
				for (String line:lines)
				{
					maxWidth = Math.max(maxWidth, parent.textWidth(line));
				}				
				
				float lineHeight = parent.textAscent()+parent.textDescent();
				float totalHeight = lines.length*lineHeight;

				// Work out the scalings required to fit text both vertically and horizontally.        
				double horizXScale = bounds.getWidth() / maxWidth;
				double horizYScale = bounds.getHeight() / totalHeight;
				double horizScale = horizXScale;
				double vertXScale = bounds.getWidth() / totalHeight;
				double vertYScale = bounds.getHeight() / maxWidth;
				double vertScale = vertXScale;

				horizScale = Math.min(horizXScale, horizYScale);
				vertScale  = Math.min(vertXScale, vertYScale);

				horizScale = (float)(horizScale*0.7 + (0.3*bounds.getWidth()*bounds.getHeight()) / tmPanel.getRootArea());
				vertScale = (float)(vertScale*0.7 + (0.3*bounds.getWidth()*bounds.getHeight()) / tmPanel.getRootArea());

				if (tmPanel.getMaxBranchTexts()[level-1] > 0)
				{
					if (horizScale > tmPanel.getMaxBranchTexts()[level-1]/40)
					{
						// No need to try vertical if text is already large enough.
						horizScale = tmPanel.getMaxBranchTexts()[level-1]/40;
						vertScale = 0;
					}
					else if (vertScale > tmPanel.getMaxBranchTexts()[level-1]/40)
					{
						vertScale = tmPanel.getMaxBranchTexts()[level-1]/40;
					}
				}

				for (int i=0; i<lines.length; i++)
				{     	
					double x=0,y=0;
					parent.pushMatrix();
					
					// Only use vertical text if it increases text size by at least 20% and is allowed.
					if ((tmPanel.getAllowVerticalLabels()) && (vertScale > horizScale*1.2))
					{
						/* Rotate text about its centre (since this will produce a larger label)
						double cx =  bounds.getX() + (bounds.getWidth()/2) - vertScale*((i+1)*lineHeight -totalHeight/2 -parent.textDescent());
						double cy =  bounds.getY() + (bounds.getHeight()-(parent.textWidth(lines[i]))*vertScale)/2;

						parent.translate((float)cx,(float)cy);
						parent.rotate(PConstants.HALF_PI);
						parent.scale((float)vertScale,(float)vertScale);
						*/
						
						double cx,cy;						
						int xAlign = leafAlignX;
						int yAlign = leafAlignY;
						
						if (leafAlignX == PConstants.LEFT)
						{
							cx = bounds.getX()+ vertScale*((lines.length-i-1)*lineHeight) + textPadding;
							yAlign = PConstants.BOTTOM;
						}
						else if (leafAlignX == PConstants.RIGHT)
						{
							cx = bounds.getX()+bounds.getWidth() - vertScale*i*lineHeight - textPadding;
							yAlign = PConstants.TOP;
						}
						else
						{
							cx = bounds.getX() + (bounds.getWidth()/2) - vertScale*((i+1)*lineHeight -totalHeight/2 -parent.textDescent());
							yAlign = PConstants.BOTTOM;
						}
												
						if (leafAlignY == PConstants.TOP)
						{
							cy = bounds.getY()+textPadding;
							xAlign = PConstants.LEFT;
						}
						else if (leafAlignY == PConstants.BOTTOM)
						{
							cy = bounds.getY()+bounds.getHeight()-textPadding;
							xAlign = PConstants.RIGHT;
						}
						else
						{
							cy = bounds.getY() + bounds.getHeight()/2;
							xAlign = PConstants.LEFT;
						}
						
						parent.textAlign(xAlign,yAlign);				
						parent.translate((float)cx,(float)cy);
						parent.rotate(PConstants.HALF_PI);
						parent.scale((float)vertScale,(float)vertScale);
					}
					else
					{
						// Use horizontal text.						
						if (branchAlignX == PConstants.LEFT)
						{
							x = bounds.getX() + textPadding;
						}
						else if (branchAlignX == PConstants.RIGHT)
						{
							x = bounds.getX() + bounds.getWidth() - textPadding;
						}
						else
						{
							x = bounds.getX() + bounds.getWidth()/2;
						}
						
						if (branchAlignY == PConstants.TOP)
						{
							y = bounds.getY() + horizScale*i*lineHeight+ textPadding;
						}
						else if (branchAlignY == PConstants.BOTTOM)
						{
							y = bounds.getY() + (bounds.getHeight() - horizScale*(lines.length-i-1)*lineHeight - textPadding);
						} 
						else
						{
							y = bounds.getY() + bounds.getHeight()/2;
						}
						
						parent.textAlign(branchAlignX,branchAlignY);
						parent.translate((float)x,(float)y);
						parent.scale((float)horizScale,(float)horizScale);
						
					}
					parent.text(lines[i],0,0);
					parent.popMatrix();
				}
			}
			float opacity = 255*Math.max(0.1f,(tmPanel.getMaxDepth()-level)/(float)tmPanel.getMaxDepth());
			parent.stroke(tmPanel.getBorderColour().getRGB(),opacity);
			parent.noFill();
			
			float borderWeight = tmPanel.getBorderWeights()[level];
			if (borderWeight < 0)
			{
				borderWeight = 1;
			}
			
			if (borderWeight > 0)
			{
				parent.strokeWeight(borderWeight);	

				if (renderer == null)
				{
					parent.rect((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight(),curveRadius,curveRadius);
				}
				else
				{
					renderer.rect((float)bounds.getX(), (float)bounds.getY(), (float)bounds.getWidth(), (float)bounds.getHeight(),curveRadius,curveRadius);
				}
			}
		}

		// Draw displacement vectors if requested.
		parent.noFill();
		
		for (NodePanel branch : tmPanel.getBranches())
		{
			int level = branch.getLevel();
			if (tmPanel.getShowBranchDisplacements()[level-1])
			{
				Rectangle2D bounds = branch.getBounds();
				parent.stroke(tmPanel.getBranchTextColours()[level].getRGB());

				// Draw displacement vector
				if ((branch.getGeoBounds() != null) && (bounds.getWidth()>0) && (bounds.getHeight()>0))
				{
					drawCurve(bounds.getX()+bounds.getWidth()/2.0, bounds.getY()+bounds.getHeight()/2.0,
							  branch.getGeoBounds().getX(),branch.getGeoBounds().getY());
				}
			}
		}
		if (tmPanel.getShowLeafDisplacement())
		{
			for (NodePanel leaf : tmPanel.getLeaves())
			{
				Rectangle2D bounds = leaf.getBounds();
				
				// Draw displacement vector
				if ((leaf.getGeoBounds() != null) && (bounds.getWidth()>0) && (bounds.getHeight()>0))
				{
					parent.stroke(tmPanel.getLeafTextColour().getRGB());
					drawCurve(bounds.getX()+bounds.getWidth()/2.0, bounds.getY()+bounds.getHeight()/2.0,
							  leaf.getGeoBounds().getX(),leaf.getGeoBounds().getY());
				}
			}
		}
		
		parent.popStyle();
	}
		
	// ------------------------------------- Mutator methods ------------------------------------
	
	
	/** Sets a new colour table from a colour table file. Assumes the colour table file is stored 
	 *  in the data directory of the sketch.
	 *  @param cTableFileName Name of file containing the colour table to use to represent treemap nodes.
	 */
	public void setColourTable(String cTableFileName)
	{
		tmPanel.setColourTable(ColourTable.readFile(parent.dataPath(cTableFileName)));
	}
	
	/** Sets the renderer to be used for drawing treemaps. This need only be set if some non-default
	 *  rendering is required (such as the sketchy rendering produced by the Handy library).
	 *  @param renderer New renderer to use or null if default rendering is to be used.
	 */
	public void setRenderer(Drawable renderer)
	{
		this.renderer = renderer;
	}
	
	/** Sets the curvature radius of rounded rectangles. If 0, normal rectangles with sharp corners are
	 *  drawn in the treemap. Values greater than 0 increase the curviness of the rectangles.
	 *  @param curveRadius Radius of curvature of treemap rectangle corners in pixel units.
	 */
	public void setCurvature(float curveRadius)
	{
		this.curveRadius = Math.max(0,curveRadius);
	}
	
	/** Sets the text alignment for leaf labels. Used for positioning the text of a leaf label relative
	 *  to its enclosing rectangle. Will default to CENTER if alignment values not recognised.
	 *  @param alignX Horizontal alignment of text can be Processing constants LEFT, CENTER or RIGHT.
	 *  @param alignY Vertical alignment of text can be Processing constants TOP, CENTER, or BOTTOM.
	 */
	public void setLeafTextAlignment(int alignX, int alignY)
	{
		// Constrain to valid alignment values and default to CENTER alignment.
		if ((alignX == PConstants.LEFT) || (alignX == PConstants.RIGHT))
		{
			this.leafAlignX = alignX;
		}
		else
		{
			this.leafAlignX = PConstants.CENTER;
		}
		
		if ((alignY == PConstants.TOP) || (alignY == PConstants.BOTTOM))
		{
			this.leafAlignY = alignY;
		}
		else
		{
			this.leafAlignY = PConstants.CENTER;
		}
	}
	
	/** Sets the text alignment for branch labels. Used for positioning the text of a branch label 
	 *  relative to its enclosing rectangle.
	 *  @param alignX Horizontal alignment of text can be Processing constants LEFT, CENTER or RIGHT.
	 *  @param alignY Vertical alignment of text can be Processing constants TOP, CENTER, or BOTTOM.
	 */
	public void setBranchTextAlignment(int alignX, int alignY)
	{
		this.branchAlignX = alignX;
		this.branchAlignY = alignY;
	}

	// ------------------------------------ Accessor methods ------------------------------------
	
	/** Provides the TreeMappa object used to build and display treemaps. This object can be used
	 *  to customise the features of the treemap that affect layout.
	 *  @return TreeMappa object used to build and display treemaps.
	 */
	public TreeMappa getTreeMappa()
	{
		return treeMappa;
	}
	
	/** Provides a TreeMapPanel used to customise the appearance of the treemap. This object can be used
	 *  to customise the features of the treemap that affect appearance but not layout.
	 *  @return Treemap panel used to display treemaps.
	 */
	public TreeMapPanel getTreeMapPanel()
	{
		return tmPanel;
	}	
	
	// ------------------------------------ Private methods ------------------------------------
	
	/** Creates a DOM from the inFile stored in TreeMappa's inFile property. This method uses the applet-safe
	 *  Processing class XMLElement to read file.
	 */
	DOMProcessor createDOM()
	{
		XMLElement root = new XMLElement(parent,treeMappa.getConfig().getInFileName());
		DOMProcessor dom = new DOMProcessor();
		dom.addElement(root.getName());
		copyXMLContents(root,dom.getElements(root.getName())[0],dom);
		return dom;
	}
	
	/** Recursively copies the contents of the XML element to a dom. Will search for child elements and
	 *  copy these too.
	 * @param xmle XMLElement from which to copy.
	 * @param domNode Destination DOM node into which attributes and values copied.
	 * @param dom Processor that handles the DOM creation.
	 */
	private void copyXMLContents(XMLElement xmle, Node domNode, DOMProcessor dom)
	{
		// Copy node attributes
		String[] attributes = xmle.listAttributes();
		for (String attribute : attributes)
		{
			dom.addAttribute(attribute,xmle.getString(attribute), domNode);
		}
		
		// Copy node text if it exists.
		String text = xmle.getContent();
		if (text != null)
		{
			dom.addText(xmle.getContent(), domNode);
		}
		
		// Copy node's children
		for (XMLElement child : xmle.getChildren())
		{
			Node domChild = dom.addElement(child.getName(),domNode);
			copyXMLContents(child,domChild,dom);
		}
	}
		
	/** Draws an asymmetric curve from (x1,y1) to (x2,y2). Greater angular change is at the source 
	 *  of the arrow (x1,y1), in order to provide a visual indication of direction. See Fekete, J-D,
	 *  Wang, D., Dang, N., Aris, A. and Plaisant, C. 'Overlaying Graph Links on TreeMaps',
	 *  Information Visualisation Poster Compendium, pp.82-83
	 */
	private void drawCurve(double x1,double y1, double x2, double y2)
	{  
	  double x = (x1-x2)/4;
	  double y = (y1-y2)/4;

	  double cx = (x2 + x*Math.cos(TreeMapPanel.CURVE_ANGLE) - y*Math.sin(TreeMapPanel.CURVE_ANGLE));
	  double cy = (y2 + y*Math.cos(TreeMapPanel.CURVE_ANGLE) + x*Math.sin(TreeMapPanel.CURVE_ANGLE));
	  parent.bezier((float)x1,(float)y1,(float)cx,(float)cy,(float)x2,(float)y2,(float)x2,(float)y2);
	}
}
