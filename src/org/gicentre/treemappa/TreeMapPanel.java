package org.gicentre.treemappa;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.gicentre.utils.colour.ColourTable;


// ***************************************************************************************************
/** Class to provide a visual representation of the tree map.
 *  @author Jo Wood, giCentre.
 *  @version 3.0, 23rd March, 2011.
 */
// ***************************************************************************************************

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

public class TreeMapPanel extends JPanel
{
	// ---------------------- Class variables ----------------------

	private static final long serialVersionUID = -3241603372524830275L;

	/** Angle of curvature for displacement arrows. */
	static final double CURVE_ANGLE = -60*Math.PI/180;
	private static final double PI_OVER_TWO = Math.PI/2.0;

	private BufferedImage screenImg; 
	private Point clickPosition,       	// Initial mouse position at start of drag.
	oldPosition;         	// Mouse position since last dragged position.
	private int mode;                  	// Type of interaction mode.
	private TreeMappa treeMappa;		// TreeMappa object capable of recalculating treemap geometry.

	private Font leafFont;				// Label fonts.
	private Font[] branchFonts;
	private boolean isTransparent;		// Determines whether or not transparent colours are used in rendering.
	private int randColourLevel;		// The level at which the evolutionary colour mutation starts.
	private boolean allowVerticalLabels;
	private Color borderColour;
	private Color[] branchTextColours;
	private Color leafTextColour;
	private float mutation;
	private boolean showArrowHead;
	private boolean[] showBranchDisplacements;
	private boolean showBranchLabels;
	private boolean showLeafDisplacement;
	private boolean showLeafLabels;

	private Random rand;				// For random colour mutation.
	private Vector<Float>[] hues;		// For base colours in evolutionary colour scheme.
	private float hue;
	private Vector<NodePanel> leaves,branches;
	private ColourTable cTable;			// For file-based colour table.
	private double rootArea;			// Area of the root rectangle in the treemap.
	private int maxDepth;				// Maximum depth of the tree.

	//private boolean isZooming;

	private float maxLeafText;			// Largest text size for leaf labels (point size or 0 for no max)
	private float[] maxBranchTexts;		// Largest text size for non-leaf labels (point size or 0 for no max)

	private AffineTransform trans,    	// Georef to pixel transformation.
	iTrans;    	// Pixel to georef transformation.

	private Point2D.Float panOffset,localPanOffset;
	private float zoomFactor,localZoomFactor;
	// Pattern matching to make SVG output XML-safe.
	private Pattern cleanAmp,cleanLT,cleanGT,cleanQuote;

	private float leafVectorWidth;
	private float[] vectorWidths;

	private static final int PAN = 1;
	private static final int ZOOM = 2;

	// Shifted or right-button mouse click/drag.
	private static final int SECONDARY_MASK = InputEvent.SHIFT_MASK |
	InputEvent.BUTTON2_MASK |
	InputEvent.BUTTON3_MASK;

	// ---------------------- Constructor ----------------------

	/** Creates the panel in which to place the visual representation of the tree map.
	 *  @param width Default width of the tree map.
	 *  @param height Default height of the tree map. 
	 *  @param treeMappa Object capable of creating a treemap.
	 */
	public TreeMapPanel(int width, int height, TreeMappa treeMappa)
	{
		super();
		this.treeMappa = treeMappa;
		TreeMapProperties props = treeMappa.getConfig();

		maxBranchTexts = props.getBranchMaxTextSizes();
		maxLeafText = props.getLeafMaxTextSize();

		leafFont = new Font(props.getLeafTextFont(),Font.BOLD,40);
		buildBranchFonts(props.getBranchTextFonts());

		leafVectorWidth = props.getLeafVectorWidth();	
		isTransparent= props.getIsTransparent();
		randColourLevel = props.getRandColourLevel();
		randColourLevel = props.getRandColourLevel();
		allowVerticalLabels = props.getAllowVerticalLabels();
		borderColour = props.getBorderColour();
		branchTextColours = props.getBranchTextColours();
		leafTextColour = props.getLeafTextColour();
		mutation = props.getMutation();
		showArrowHead = props.getShowArrowHead();
		showBranchDisplacements = props.getShowBranchDisplacements();
		showBranchLabels = props.getLabelBranches();
		showLeafDisplacement = props.getShowLeafDisplacement();
		showLeafLabels = props.getLabelLeaves();
		vectorWidths = props.getBranchVectorWidths();

		String cTableFile = props.getCTableFileName();
		long seed = props.getSeed();

		if (seed == 0)
		{
			rand = new Random();
		}
		else
		{
			rand = new Random(seed);
		}

		hues = new Vector[randColourLevel+1];

		// Attempt to load colour table.
		if (cTableFile != null)
		{
			cTable = ColourTable.readFile(cTableFile);
		}
		else
		{
			// Check to see if default colour table was provided to treeMappa.
			cTable = treeMappa.getDefaultColourTable();
		}

		// If colour table could not be found, create one.
		if (cTable == null)
		{
			cTable = new ColourTable();
			cTable.addContinuousColourRule(0, 50, 50, 50);
			cTable.addContinuousColourRule(1, 255, 100, 100);
		}

		cleanAmp = Pattern.compile("&");
		cleanLT = Pattern.compile("<");
		cleanGT = Pattern.compile(">");
		cleanQuote = Pattern.compile("\"");

		setBackground(Color.white);
		setSize(width, height);
		setPreferredSize(new Dimension(600,400));

		Rectangle2D rootBounds = treeMappa.getRoot().getRectangle();
		rootArea = rootBounds.getWidth()*rootBounds.getHeight();
		maxDepth = treeMappa.getRoot().getMaxDepth();
		screenImg = new BufferedImage((int)rootBounds.getWidth(), (int)rootBounds.getHeight(), BufferedImage.TYPE_INT_ARGB);

		addMouseListener(new MouseClickMonitor());
		addMouseMotionListener(new MouseMoveMonitor());
		addMouseWheelListener(new MouseWheelMonitor());
		addComponentListener(new PanelSizeMonitor());

		//isZooming = false;
		panOffset = new Point2D.Float(0,0);
		localPanOffset = new Point2D.Float(0,0);
		zoomFactor = 1;
		localZoomFactor = 1;
		calcTransformation();

		leaves = new Vector<NodePanel>();
		branches = new Vector<NodePanel>();
		addRectangles(treeMappa.getRoot(),Color.getHSBColor(rand.nextFloat(), 0.6f, 0.6f));
	}

	// ------------------------ Methods ------------------------

	/** Draws the tree map nodes.
	 * @param g Graphics context in which to draw. 
	 */
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Point2D min = getGeoToPixel(new Point2D.Float(0,0));
		Point2D max = getGeoToPixel(new Point2D.Float(screenImg.getWidth(),screenImg.getHeight()));

		int x = (int)(min.getX());
		int y = (int)(max.getY());
		int width  = (int)(max.getX()-min.getX());
		int height = (int)(min.getY()-max.getY());

		g.drawImage(screenImg,x,y,width,height,this);        
	}

	/** Reports the treemap image assuming it can store alpha (transparency) values. This is exactly equivalent to
	 *  calling <code>getImage(true)</code>. Note that the returned image is not suitable for saving as a jpeg, bmp
	 *  or gif file. To do this, call <code>getImage(false)</code>.
	 *  @return Image holding the treemap at its natural resolution.
	 */
	public BufferedImage getImage()
	{
		return getImage(true);
	}

	/** Reports the treemap image. Setting <code>hasAlpha</code> to true is less memory intensive, but can only be
	 *  saved as a .png file. If this image is needed to be saved as a jpg, bmp or gif file, <code>hasAlpha</code>
	 *  should be set to false. 
	 *  @param hasAlpha Determines if image is to use transparency or not. 
	 *  @return Image holding the treemap at its natural resolution.
	 */
	public BufferedImage getImage(boolean hasAlpha)
	{
		if (hasAlpha == true)
		{
			return screenImg;
		}

		try
		{
			BufferedImage bufImage = new BufferedImage(screenImg.getWidth(), screenImg.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D big = bufImage.createGraphics();        
			big.setColor(Color.white);
			big.fillRect(0,0,screenImg.getWidth(), screenImg.getHeight());

			big.drawImage(screenImg,0,0,screenImg.getWidth(), screenImg.getHeight(),null);
			return bufImage;
		}
		catch (OutOfMemoryError e)
		{
			System.err.println("Warning: Not enough memory to save graphics image.");
			return null;
		}
	}

	/** Writes the tree map display to an image or SVG file with the given name.
	 * @param imgFileName Name of image file to write.
	 * @return True if file written successfully.
	 */
	public boolean writeImage(String imgFileName)
	{		
		int dotLocation = imgFileName.lastIndexOf('.');
		if ((dotLocation == 0) || (dotLocation >= imgFileName.length()-1))
		{
			System.err.println("Image file name '"+imgFileName+"' must include an extension (e.g. .png, .svg etc.)");
			return false;
		}

		String extension = imgFileName.substring(dotLocation+1);

		// Check for SVG output request
		if ((extension.equals("svg")) || (extension.equals("svgz")))
		{
			writeSVG(imgFileName,isTransparent);
			return true;
		}

		String writerNames[] = ImageIO.getWriterFormatNames();
		boolean validType = false;
		for (String name : writerNames)
		{
			if (extension.equals(name))
			{
				validType = true;
				break;
			}
		}

		if (validType == false)
		{
			System.err.print("Extension '"+extension+"' not supported. Should be one of ");

			for (String name : writerNames)
			{
				System.err.print(name+" ");
			}
			System.err.println("svg svgz");
			return false;
		}

		try 
		{
			File outputfile = new File(imgFileName);
			boolean hasAlpha = false;
			if (extension.equalsIgnoreCase("png"))
			{
				hasAlpha = true;
			}
			ImageIO.write(getImage(hasAlpha), extension, outputfile);
		}
		catch (IOException e)
		{
			System.err.println("Problem writing image file '"+imgFileName+"'");
			return false;
		}

		return true;
	}



	/** Updates the tree map display to be shown in this panel.
	 */
	public void updateImage()
	{
		Graphics2D g = (Graphics2D)screenImg.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, screenImg.getWidth(), screenImg.getHeight());
		BasicStroke leafStroke = new BasicStroke(leafVectorWidth); 

		BasicStroke[] branchStrokes = new BasicStroke[vectorWidths.length];
		for (int i=0; i<branchStrokes.length; i++)
		{
			branchStrokes[i] = new BasicStroke(vectorWidths[i]*10); 
		}

		g.setFont(leafFont);
		for (NodePanel leaf : leaves)
		{
			if (leaf.isDummy())
			{	
				// Do not display dummy nodes.
				continue;
			}

			// Fill leaf background.
			Rectangle bounds = leaf.getBounds();		
			g.setColor(leaf.getColour());
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

			// Draw leaf label.
			if (showLeafLabels && bounds.width > 10)
			{
				String[] lines = leaf.getLabel().split("\\\\n");				
				g.setColor(leafTextColour);
				FontRenderContext frc = g.getFontRenderContext();

				float maxWidth=0;
				for (String line:lines)
				{
					maxWidth = Math.max(maxWidth, (float)leafFont.getStringBounds(line, frc).getWidth());
				}

				LineMetrics lm = leafFont.getLineMetrics(lines[0], frc);
				float lineHeight = lm.getAscent() + lm.getDescent();
				float totalHeight = lines.length*lineHeight;

				// Work out the scalings required to fit text both vertically and horizontally.        
				float horizXScale = bounds.width / maxWidth;
				float horizYScale = bounds.height / totalHeight;
				float horizScale = horizXScale;
				float vertXScale = bounds.width / totalHeight;
				float vertYScale = bounds.height / maxWidth;
				float vertScale = vertXScale;

				horizScale = Math.min(horizXScale, horizYScale);
				vertScale  = Math.min(vertXScale, vertYScale);

				horizScale = (float)(horizScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);
				vertScale = (float)(vertScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);

				if (maxLeafText > 0)
				{
					if (horizScale > maxLeafText/40f)
					{
						// No need to try vertical if text is already large enough.
						horizScale = maxLeafText/40f;
						vertScale = 0;
					}
					else if (vertScale > maxLeafText/40f)
					{
						vertScale = maxLeafText/40f;
					}
				}

				for (int i=0; i<lines.length; i++)
				{
					AffineTransform at = g.getTransform();		// Store current transformation.
					double x=0,y=0;

					// Only use vertical text if it increases text size by at least 20% and is allowed.
					if (allowVerticalLabels && (vertScale > horizScale*1.2))
					{
						// Rotate text about its centre (since this will produce a larger label)
						double cx =  bounds.x + (bounds.width/2) - vertScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());
						double cy =  bounds.y + (bounds.height-(leafFont.getStringBounds(lines[i], frc).getWidth())*vertScale)/2.0;	        		

						/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
		        		at.translate(cx, cy);
		        		at.rotate(PI_OVER_TWO);
		        		at.scale(vertScale,vertScale);
						 */

						// Apply transformations directly to Graphics2D to avoid java deriveFont bug.
						g.translate(cx, cy);
						g.rotate(PI_OVER_TWO);
						g.scale(vertScale,vertScale);
					}
					else
					{
						// Use horizontal text.
						x = bounds.x + (bounds.width - horizScale*(leafFont.getStringBounds(lines[i], frc).getWidth()))/2.0;
						y = bounds.y + (bounds.height/2) + horizScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

						/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
			        	at.translate(x, y);
		        		at.scale(horizScale, horizScale);
						 */

						// Apply transformations directly to Graphics2D to avoid java deriveFont bug.
						g.translate(x, y);
						g.scale(horizScale, horizScale);
					}

					g.drawString(lines[i],0,0);
					g.setTransform(at);			// Restore original transformation.

					/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
		        	g.setFont(leafFont.deriveFont(at));
		        	g.drawString(lines[i],0,0);
					 */
				}
			}

			g.setColor(new Color(borderColour.getRed()/255f,borderColour.getGreen()/255f,borderColour.getBlue()/255f,0.1f));
			g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}

		for (NodePanel branch : branches)
		{
			if (branch.isDummy())
			{	
				// Do not display dummy nodes.
				continue;
			}

			Rectangle bounds = branch.getBounds();
			int level = branch.getLevel();
			g.setFont(branchFonts[level-1]);

			// Draw branch label.
			if (showBranchLabels && bounds.width > 10)
			{
				String[] lines = branch.getLabel().split("\\\\n");
				g.setColor(branchTextColours[level-1]);				
				FontRenderContext frc = g.getFontRenderContext();
				float maxWidth=0;
				for (String line:lines)
				{
					maxWidth = Math.max(maxWidth, (float)branchFonts[level-1].getStringBounds(line, frc).getWidth());
				}				

				LineMetrics lm = branchFonts[level-1].getLineMetrics(lines[0], frc);
				float lineHeight = lm.getAscent() + lm.getDescent();
				float totalHeight = lines.length*lineHeight;

				// Work out the scalings required to fit text both vertically and horizontally.        
				float horizXScale = bounds.width / maxWidth;
				float horizYScale = bounds.height / totalHeight;
				float horizScale = horizXScale;
				float vertXScale = bounds.width / totalHeight;
				float vertYScale = bounds.height / maxWidth;
				float vertScale = vertXScale;

				horizScale = Math.min(horizXScale, horizYScale);
				vertScale  = Math.min(vertXScale, vertYScale);

				horizScale = (float)(horizScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);
				vertScale = (float)(vertScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);

				if (maxBranchTexts[level-1] > 0)
				{
					if (horizScale > maxBranchTexts[level-1]/40f)
					{
						// No need to try vertical if text is already large enough.
						horizScale = maxBranchTexts[level-1]/40f;
						vertScale = 0;
					}
					else if (vertScale > maxBranchTexts[level-1]/40f)
					{
						vertScale = maxBranchTexts[level-1]/40f;
					}
				}

				for (int i=0; i<lines.length; i++)
				{     	
					AffineTransform at = g.getTransform();		// Store current transformation.
					double x=0,y=0;

					// Only use vertical text if it increases text size by at least 20% and is allowed.
					if (allowVerticalLabels && (vertScale > horizScale*1.2))
					{
						// Rotate text about its centre (since this will produce a larger label)
						double cx =  bounds.x + (bounds.width/2) - vertScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());
						double cy =  bounds.y + (bounds.height-(branchFonts[level-1].getStringBounds(lines[i], frc).getWidth())*vertScale)/2.0;

						/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
		        		at.translate(cx, cy);
		        		at.rotate(PI_OVER_TWO);
		        		at.scale(vertScale,vertScale);
						 */

						// Apply transformations directly to Graphics2D to avoid java deriveFont bug.
						g.translate(cx, cy);
						g.rotate(PI_OVER_TWO);
						g.scale(vertScale,vertScale);
					}
					else
					{
						// Use horizontal text.
						x = bounds.x + (bounds.width - horizScale*(branchFonts[level-1].getStringBounds(lines[i], frc).getWidth()))/2.0;
						y = bounds.y + (bounds.height/2) + horizScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

						/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
			        	at.translate(x, y);
		        		at.scale(horizScale, horizScale);
						 */

						// Apply transformations directly to Graphics2D to avoid java deriveFont bug.
						g.translate(x, y);
						g.scale(horizScale, horizScale);
					}

					g.drawString(lines[i],0,0);
					g.setTransform(at);			// Restore original transformation.

					/* This cannot be used due to Java MacOSX 1.5 bug Radar #4068592
		        	g.setFont(branchFont.deriveFont(at));
		        	g.drawString(lines[i],0,0);
					 */
				}
			}
			float opacity = Math.max(0.1f,(maxDepth-level)/(float)maxDepth);
			g.setColor(new Color(borderColour.getRed()/255f,borderColour.getGreen()/255f,borderColour.getBlue()/255f,opacity));
			g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}

		// Draw displacement vectors if requested.
		for (NodePanel branch : branches)
		{
			int level = branch.getLevel();
			if (showBranchDisplacements[level-1])
			{
				Rectangle bounds = branch.getBounds();

				// Draw displacement vector
				if ((branch.getGeoBounds() != null) && (bounds.width>0) && (bounds.height>0))
				{
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

					g.setColor(branchTextColours[level]);
					g.setStroke(branchStrokes[level-1]); 
					g.draw(getArrow(bounds.x+bounds.width/2f, bounds.y+bounds.height/2f,(float)branch.getGeoBounds().getX(),(float)branch.getGeoBounds().getY(),branchStrokes[level-1].getLineWidth(),showArrowHead));
				}
			}
		}
		if (showLeafDisplacement)
		{
			for (NodePanel leaf : leaves)
			{
				Rectangle bounds = leaf.getBounds();
				// Draw displacement vector
				if ((leaf.getGeoBounds() != null) && (bounds.width>0) && (bounds.height>0))
				{
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

					g.setColor(leafTextColour);
					g.setStroke(leafStroke); 
					g.draw(getArrow(bounds.x+bounds.width/2f, bounds.y+bounds.height/2f,(float)leaf.getGeoBounds().getX(),(float)leaf.getGeoBounds().getY(),leafStroke.getLineWidth(),showArrowHead));
				}
			}
		}
	}

	/** Updates the entire treemap layout. Should be called when some properties of the treemap that
	 *  affect layout have been changed, but not the underlying tree structure, for example, changing
	 *  the border width. Requests that simply change the rendering style, such as colours or text fonts
	 *  should call <code>updateImage()</code> instead. Requests that change the underlying tree 
	 *  structure, such as swapping data between hierarchy levels should call <code>updateData()</code>. 
	 */
	public void updateLayout()
	{        
		treeMappa.buildTreeMap();

		leaves = new Vector<NodePanel>();
		branches = new Vector<NodePanel>();
		addRectangles(treeMappa.getRoot(),Color.getHSBColor(rand.nextFloat(), 0.6f, 0.6f));
		updateImage();
	}

	/** Updates the entire treemap using new tree data. This method should only be called if the underlying
	 *  tree data have been changed in some way. For changes to layout or display using the current tree
	 *  data, call <code>updateLayout()</code> or <code>updateImage()</code> instead. 
	 */
	public void updateData()
	{
		TreeMapNode root = treeMappa.getRoot();
		//treeMappa.setRoot(root);		// Needed to ensure root and tree are always consistent???
		Rectangle2D rootBounds = root.getRectangle();
		rootArea = rootBounds.getWidth()*rootBounds.getHeight();
		maxDepth = root.getMaxDepth();
		updateLayout();
	}

	/** Display the summary statistics describing the treemap.
	 *  @return True if summary statistics were able to be calculated.
	 */
	public boolean showStatistics()
	{
		return treeMappa.showStatistics();
	}

	// ------------------ Package-wide accessors for use by other treemap classes.

	/** Reports the list of leaves in the treemap.
	 *  @return list of leaves in the treemap.
	 */
	List<NodePanel>getLeaves()
	{
		return leaves;
	}

	/** Reports the list of branches in the treemap.
	 *  @return list of branches in the treemap.
	 */
	List<NodePanel>getBranches()
	{
		return branches;
	}

	/** Reports the mutation factor used for evolutionary colour schemes.
	 *  @return Mutation factor.
	 */
	float getMutation()
	{
		return mutation;
	}

	/** Reports the random number generator used for evolutionary colour schemes.
	 *  @return Random number generator.
	 */
	Random getRand()
	{
		return rand;
	}

	/** Reports whether or not leaf labels are to be drawn.
	 *  @return True if leaf labels are to be drawn.
	 */
	boolean getShowLeafLabels()
	{
		return showLeafLabels;
	}

	/** Reports whether or not branch labels are to be drawn.
	 *  @return True if branch labels are to be drawn.
	 */
	boolean getShowBranchLabels()
	{
		return showBranchLabels;
	}

	/** Reports the area occupied by the root node of the tree.
	 *  @return Root area.
	 */
	double getRootArea()
	{
		return rootArea;
	}

	/** Reports the maximum text size for leaf labels.
	 *  @return Maximum text size for leaf labels.
	 */
	float getMaxLeafText()
	{
		return maxLeafText;
	}

	/** Reports the maximum text size for branch labels at each level.
	 *  @return Maximum text sizes for branch labels.
	 */
	float[] getMaxBranchTexts()
	{
		return maxBranchTexts;
	}

	/** Reports the maximum depth of the hierarchy.
	 *  @return maximum depth of the hierarchy.
	 */
	int getMaxDepth()
	{
		return maxDepth;
	}

	/** Reports whether or not vertical labels are permitted.
	 *  @return True if vertical labels are permitted.
	 */
	boolean getAllowVerticalLabels()
	{
		return allowVerticalLabels;
	}

	/** Reports the border colour around each leaf and branch.
	 *  @return border colour.
	 */
	Color getBorderColour()
	{
		return borderColour;
	}

	/** Reports the leaf text label colour.
	 *  @return Leaf text colour.
	 */
	Color getLeafTextColour()
	{
		return leafTextColour;
	}

	/** Reports the branch text label colours for each level of the hierarchy.
	 *  @return Branch text colours.
	 */
	Color[] getBranchTextColours()
	{
		return branchTextColours;
	}

	/** Reports whether or not branch displacements are shown at each level in the hierarchy
	 *  @return displacement visibility at each level of the hierarchy.
	 */
	boolean[] getShowBranchDisplacements()
	{
		return showBranchDisplacements;
	}

	/** Reports whether or not leaf displacement is shown.
	 *  @return True if leaf displacement is shown.
	 */
	boolean getShowLeafDisplacement()
	{
		return showLeafDisplacement;
	}


	// ------------------ Requests that change only the rendering style

	/** Sets a whether or not vertical text labels are allowed for tall thin rectangles. Note that
	 *  the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param allow If true, vertical labels will be permitted, if not, all labels will be horizontal.
	 *  @return True if change has been made successfully.
	 */
	public boolean setAllowVerticalLabels(boolean allow)
	{
		this.allowVerticalLabels = allow;
		treeMappa.getConfig().setParameter("allowVerticalLabels", allow?"true":"false");
		return true;
	}

	/** Sets the colour used to display treemap borders. Note that the treemap will not use this new 
	 *  setting until a call to <code>updateImage()</code> is made.
	 *  @param borderColour New colour in which to display treemap node borders.
	 *  @return True if change has been made successfully.
	 */
	public boolean setBorderColour(Color borderColour)
	{
		this.borderColour = borderColour;
		treeMappa.getConfig().setParameter("borderColour", ColourTable.getHexString(borderColour.getRGB()));
		return true;
	}

	/** Sets the colour used to display leaf text. Note that the treemap will not use this new 
	 *  setting until a call to <code>updateImage()</code> is made.
	 *  @param textColour New colour in which to display treemap leaf labels.
	 *  @return True if change has been made successfully.
	 */
	public boolean setLeafTextColour(Color textColour)
	{
		this.leafTextColour = textColour;
		String hexColour = ColourTable.getHexString(textColour.getRGB());
		if (leafTextColour.getAlpha() < 255)
		{
			hexColour = hexColour+paddedHex(textColour.getAlpha());
		}

		treeMappa.getConfig().setParameter("leafTextColour", hexColour);
		return true;
	}

	/** Sets the colour used to display text at all branch levels. Note that the treemap will 
	 *  not use this new  setting until a call to <code>updateImage()</code> is made.
	 *  @param textColour New colour in which to display treemap branch labels.
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchTextColours(Color textColour)
	{
		for (int i=0; i<branchTextColours.length; i++)
		{
			branchTextColours[i] = textColour;
		}
		String hexColour = ColourTable.getHexString(textColour.getRGB());
		if (textColour.getAlpha() < 255)
		{
			hexColour = hexColour+paddedHex(textColour.getAlpha());
		}

		treeMappa.getConfig().setParameter("textColour", hexColour);
		return true;
	}

	/** Sets the colour used to display branch text at the given level. Note that the treemap will 
	 *  not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level at which to make the change.
	 *  @param textColour New colour in which to display treemap branch labels at the given level.
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchTextColour(int level, Color textColour)
	{
		if ((level < 0) || (level >= branchTextColours.length))
		{
			System.err.println("Cannot set text colour at level "+level+". Must be between 0 and "+(branchTextColours.length-1));
			return false;
		}
		branchTextColours[level] = textColour;
		String hexColour = ColourTable.getHexString(textColour.getRGB());
		if (textColour.getAlpha() < 255)
		{
			hexColour = hexColour+paddedHex(textColour.getAlpha());
		}

		treeMappa.getConfig().setParameter("textColour"+level, hexColour);
		return true;
	}

	/** Sets the font used to display text at all branch levels. Note that the treemap will 
	 *  not use this new  setting until a call to <code>updateImage()</code> is made.
	 *  @param fontName Name of new font in which to display treemap branch labels.
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchFonts(String fontName)
	{
		String[] fontNames = new String[branchFonts.length];
		for (int i=0; i<fontNames.length; i++)
		{
			fontNames[i] = fontName;
		}
		buildBranchFonts(fontNames);
		treeMappa.getConfig().setParameter("textFont", fontName);
		return true;
	}

	/** Sets the font used to display branch text at the given level. Note that the treemap will 
	 *  not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level at which to make the change.
	 *  @param fontName Name of font in which to display treemap branch labels at the given level.
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchFont(int level, String fontName)
	{
		if ((level < 0) || (level >= branchFonts.length))
		{
			System.err.println("Cannot set font at level "+level+". Must be between 0 and "+(branchFonts.length-1));
			return false;
		}
		branchFonts[level] = new Font(fontName,Font.PLAIN,40);
		treeMappa.getConfig().setParameter("textFont"+level, fontName);
		return true;
	}

	/** Sets the font used to display leaf text. Note that the treemap will not use this new setting
	 *  until a call to <code>updateImage()</code> is made.
	 *  @param fontName New font in which to display treemap leaf labels.
	 *  @return True if change has been made successfully.
	 */
	public boolean setLeafFont(String fontName)
	{
		leafFont = new Font(fontName,Font.PLAIN,40);
		treeMappa.getConfig().setParameter("leafTextFont", fontName);
		return true;
	}

	/** Sets the width of the leaf displacement vectors. Note that the treemap will not use this new setting
	 *  until a call to <code>updateImage()</code> is made.
	 *  @param vectWidth Width of leaf displacement vectors in pixel units.
	 *  @return True if change has been made successfully.
	 */
	public boolean setLeafVectorWidth(float vectWidth)
	{
		leafVectorWidth = vectWidth;
		treeMappa.getConfig().setParameter("vectorWidth", Float.toString(vectWidth));
		return true;
	}

	/** Sets the maximum text size of the branch labels at the given branch level. Note that the 
	 *  treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level at which to make the change.
	 *  @param size Maximum vertical text size in pixel units. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchMaxTextSize(int level, float size)
	{
		if ((level < 0) || (level >= maxBranchTexts.length))
		{
			System.err.println("Cannot set branch maximum text size at level "+level+". Must be between 0 and "+(maxBranchTexts.length-1));
			return false;
		}
		maxBranchTexts[level] = size;
		treeMappa.getConfig().setParameter("maxBranchText"+level, Float.toString(size));
		return true;
	}

	/** Sets the maximum text size of the branch labels at all hierarchy levels. Note that the 
	 *  treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param size Maximum vertical text size in pixel units. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchMaxTextSizes(float size)
	{
		for (int i=0; i<maxBranchTexts.length; i++)
		{
			maxBranchTexts[i] = size;
		}
		treeMappa.getConfig().setParameter("maxBranchText", Float.toString(size));
		return true;
	}

	/** Sets the maximum text size of the leaf labels. Note that the treemap will not use this 
	 *  new setting until a call to <code>updateImage()</code> is made.
	 *  @param size Maximum vertical text size in pixel units. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setLeafMaxTextSize(float size)
	{
		maxLeafText = size;
		treeMappa.getConfig().setParameter("maxLeafText", Float.toString(size));
		return true;
	}

	/** Sets the mutation value that controls the degree of colour mutation when using evolutionary 
	 *  colour scheme. Note that the treemap will not use this new setting until a call to 
	 *  <code>updateImage()</code> is made.
	 *  @param mutation Mutation index between 0 (inherits parent colour exactly) and 1 (independent of parent colour). 
	 *  @return True if change has been made successfully.
	 */
	public boolean setMutation(float mutation)
	{
		if ((mutation < 0) || (mutation > 1))
		{
			System.err.println("Cannot set mutation value of "+mutation+". Must be between 0 and 1");
			return false;
		}
		this.mutation = mutation;
		treeMappa.getConfig().setParameter("mutation", Float.toString(mutation));
		return true;
	}

	/** Sets the level at which random colour mutation should occur when using an evolutionary colour scheme.
	 *  Note that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level beyond which colour mutation should occur. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setRandColourLevel(int level)
	{
		if ((level < 0) || (level > TreeMapApp.MAX_DEPTH))
		{
			System.err.println("Cannot set random colour level value of "+level+". Must be between 0 and "+TreeMapApp.MAX_DEPTH);
			return false;
		}
		this.randColourLevel = level;
		treeMappa.getConfig().setParameter("randColourLevel", Integer.toString(level));
		return true;
	}

	/** Sets the seed value used to generate random colour mutations in the evolutionary colour scheme.
	 *  This allows a repeatable, but random colour scheme to be used.
	 *  Note that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param seed New random seed. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setSeed(long seed)
	{
		rand = new Random(seed);
		treeMappa.getConfig().setParameter("seed", Long.toString(seed));
		return true;
	}

	/** Determines whether or not arrow heads are displayed when showing displacement vectors.
	 *  Note that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param showArrowHead Arrow heads shown if true.
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowArrowHead(boolean showArrowHead)
	{
		this.showArrowHead = showArrowHead;
		treeMappa.getConfig().setParameter("showArrowHead", showArrowHead?"true":"false");
		return true;
	}

	/** Determines whether or not branch displacement vectors are shown at the given branch level. Note 
	 *  that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level at which to make the change.
	 *  @param showDisplacement Displacement vectors will be shown if true. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowBranchDisplacement(int level, boolean showDisplacement)
	{
		if ((level < 0) || (level >= showBranchDisplacements.length))
		{
			System.err.println("Cannot show branch displacements at level "+level+". Must be between 0 and "+(showBranchDisplacements.length-1));
			return false;
		}
		showBranchDisplacements[level] = showDisplacement;
		treeMappa.getConfig().setParameter("showBranchDisplacement"+level, showDisplacement?"true":"false");
		return true;
	}

	/** Determines whether or not branch displacement vectors are shown. Note 
	 *  that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param showDisplacement Displacement vectors will be shown if true. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowBranchDisplacements(boolean showDisplacement)
	{
		for (int i=0; i<showBranchDisplacements.length; i++)
		{
			showBranchDisplacements[i] = showDisplacement;
		}
		treeMappa.getConfig().setParameter("showBranchDisplacement", showDisplacement?"true":"false");
		return true;
	}

	/** Determines whether or not leaf displacement vectors are shown. Note 
	 *  that the treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param showDisplacement Displacement vectors will be shown if true. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowLeafDisplacement(boolean showDisplacement)
	{
		this.showLeafDisplacement = showDisplacement;
		treeMappa.getConfig().setParameter("showLeafDisplacement", showDisplacement?"true":"false");
		return true;
	}

	/** Sets the width of the branch displacement vectors at the given branch level. Note that the 
	 *  treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param level Hierarchy level at which to make the change.
	 *  @param vectWidth Width of branch displacement vectors in pixel units. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchVectorWidth(int level, float vectWidth)
	{
		if ((level < 0) || (level >= vectorWidths.length))
		{
			System.err.println("Cannot set branch vector width at level "+level+". Must be between 0 and "+(vectorWidths.length-1));
			return false;
		}
		vectorWidths[level] = vectWidth;
		treeMappa.getConfig().setParameter("vectorWdith"+level, Float.toString(vectWidth));
		return true;
	}

	/** Sets the width of the branch displacement vectors at all branch levels. Note that the 
	 *  treemap will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param vectWidth Width of branch displacement vectors in pixel units. 
	 *  @return True if change has been made successfully.
	 */
	public boolean setBranchVectorWidths(float vectWidth)
	{
		for (int i=0; i<vectorWidths.length; i++)
		{
			vectorWidths[i] = vectWidth;
		}
		treeMappa.getConfig().setParameter("vectorWdith", Float.toString(vectWidth));
		return true;
	}

	/** Sets a new colour table to be used to identify treemap node colours. Unlike the other set methods
	 *  that alter the treemap appearance, this method does not update the TreeMapProperties object associated
	 *  with the treemap. This is because that property is the name of a colour table file, and the 
	 *  incoming colour table supplied to this method may not have a version saved to file. Note also that
	 *  the treemap will not use these new colours until a call to <code>updateImage()</code> is made.
	 *  @param cTable New colour table to use when identifying treemap colours.
	 *  @return True if change has been made successfully.
	 */
	public boolean setColourTable(ColourTable cTable)
	{
		this.cTable = cTable;
		return true;
	}

	/** Sets whether or not transparency is used to when drawing the treemap. This can be useful when
	 *  creating PDF images that cannot display transparent colours correctly. Note that the treemap 
	 *  will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param useTransparent Transparency is used if true, otherwise, opaque grey levels are used for text labels.
	 *  @return True if change has been made successfully.
	 */
	public boolean setUseTransparency(boolean useTransparent)
	{
		this.isTransparent = useTransparent;
		treeMappa.getConfig().setParameter("transparent", useTransparent?"true":"false");
		return true;
	}

	/** Determines whether or not branch labels are to be displayed. Note that the treemap 
	 *  will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param showLabels Branch labels will be displayed if true.
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowBranchLabels(boolean showLabels)
	{
		this.showBranchLabels = showLabels;
		treeMappa.getConfig().setParameter("labelBranches", showLabels?"true":"false");
		return true;
	}

	/** Determines whether or not leaf labels are to be displayed. Note that the treemap 
	 *  will not use this new setting until a call to <code>updateImage()</code> is made.
	 *  @param showLabels Leaf labels will be displayed if true.
	 *  @return True if change has been made successfully.
	 */
	public boolean setShowLeafLabels(boolean showLabels)
	{
		this.showLeafLabels = showLabels;
		treeMappa.getConfig().setParameter("labelLeaves", showLabels?"true":"false");
		return true;
	}

	/** Provides the colour table currently used to identify treemap node colours. 
	 *  @return Colour table used when identifying treemap colours.
	 */
	public ColourTable getColourTable()
	{
		return cTable;
	}

	// ------------------ Requests that change the tree layout but not the tree structure

	/** Sets the alignment settings for all levels within the treemap. Note that since this operation
	 *  requires the recalculation of the treemap layout, no changes will be made until <code>updateLayout()</code>
	 *  is called.
	 *  @param alignment Alignment setting to use. Valid values are 'horizontal', 'vertical' and 'free'.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setAlignments(String alignment)
	{
		return treeMappa.setAlignments(alignment);
	}

	/** Sets the alignment setting for the given level of the treemap. Note that since this operation
	 *  requires the recalculation of the treemap layout, no changes will be made until <code>updateLayout()</code>
	 *  is called.
	 *  @param level Level of the hierarchy at which the given border setting is to apply.
	 *  @param alignment Alignment setting to use. Valid values are 'horizontal', 'vertical' and 'free'.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setAlignment(int level, String alignment)
	{
		return treeMappa.setAlignment(level, alignment);
	}

	/** Sets the border size of the treemap. Note that since this operation requires the recalculation
	 *  of the treemap layout, no changes will be made until <code>updateLayout()</code> is called.
	 *  @param borderSize Border size in pixels used to separate treemap nodes.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setBorders(double borderSize)
	{
		return treeMappa.setBorders(borderSize);
	}

	/** Sets the border size of the nodes at the given level in the treemap. Note that since this operation
	 *  requires the recalculation of the treemap layout, no changes will be made until <code>updateLayout()</code>
	 *  is called.
	 *  @param level Level of the hierarchy at which the given border setting is to apply.
	 *  @param borderSize Border size in pixels used to separate treemap nodes.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setBorder(int level, float borderSize)
	{
		return treeMappa.setBorder(level,borderSize);
	}

	/** Sets the a new default layout for the treemap. Note that no visible changes to the layout will be made until 
	 *  <code>updateLayout()</code> is called.
	 *  @param layout Name of new layout algorithm to use.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setLayouts(String layout)
	{
		return treeMappa.setLayouts(layout);
	}

	/** Sets the a new layout for the given level in the treemap. Note that no visible changes to the layout will be made until 
	 *  <code>updateLayout()</code> is called.
	 *  @param level Level of the hierarchy at which the given layout setting is to apply.
	 *  @param layout Name of new layout algorithm to use.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setLayout(int level, String layout)
	{
		return treeMappa.setLayout(level, layout);
	}

	/** Reports the currently used treemap layouts at each level of the hierarchy.
	 *  @return Name of the layouts currently being used to produce the treemap. The first item is the layout
	 *          of the nodes in the root of the tree, the second the children of the those nodes etc.
	 */
	public String[] getLayouts()
	{
		return treeMappa.getConfig().getLayouts();
	}

	// ------------------ Requests that change the tree structure.

	/** Removes the tree data from a the given level of the hierarchy. If the nodes to cut
	 *  are leaves, they are removed entirely from the tree. If they are branches, then their
	 *  children are attached to the removed node's parent. Note that the treemap will not be
	 *  updated until a call to <code>updateLayout()<code> is made.
	 *  @param level Level of the hierarchy at which to apply the operation.
	 *  @return True if the operation has been performed successfully.
	 */
	public boolean oCut(int level)
	{
		if (level <= 0)
		{
			System.err.println("Cannot cut nodes from level "+level+" of the tree");
			return false;
		}
		int maxDepthFromRoot = treeMappa.getRoot().getMaxDepth();
		if (level > maxDepthFromRoot)
		{
			System.err.println("Cannot cut nodes from level "+level+" of the tree since it only contains "+maxDepthFromRoot+" levels.");
			return false;
		}
		oCut(level, treeMappa.getRoot());
		return true;
	}

	private void oCut(int level, TreeMapNode node)
	{
		// Do a depth first search up to and including the given level.
		if (node.getLevel() < level)
		{
			HashSet<TreeMapNode> children = new HashSet<TreeMapNode>();
			for (TreeMapNode child: node.getChildren())
			{
				children.add(child);	
			}
			for (TreeMapNode child: children)
			{
				oCut(level,child);
			}
		}

		// We have a node at the level to cut, so cut this one and attach its children to its parent.
		TreeMapNode parent = node.getParent();

		if (parent != null)
		{

			for (TreeMapNode child: node.getChildren())
			{
				parent.add(child);
			}
			//node.removeFromParent();
			parent.remove(node);
		}

	}


	// ---------------------------------- Accessor methods ----------------------------------

	/** Provides the set of ordered border width settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of border width settings.
	 */
	public double[] getBorders()
	{
		return treeMappa.getConfig().getBorders();
	}

	// ----------------------------- Private Methods -------------------------------

	/** Adds the graphical representations of the given node and all its descendants
	 * to the main graphics panel representing the tree map.
	 * @param node Node to add to the panel. 
	 */
	private void addRectangles(TreeMapNode node, Color parentColour)
	{
		if (node.isLeaf())
		{
			return;
		}

		// Find the transformation coefficients that would allow georeferenced children to sit in rectangle.
		double minX = Float.MAX_VALUE;
		double maxX = -Float.MAX_VALUE;
		double minY = Float.MAX_VALUE;
		double maxY = -Float.MAX_VALUE;
		double xRange = 0, yRange = 0;

		// Only process spatial nodes.
		if (node.getLocation() != null)
		{	
			for (TreeMapNode child : node.getChildren())
			{
				if (child.getLocation().getX() < minX)
				{
					minX = child.getLocation().getX();
				}
				if (child.getLocation().getX() > maxX)
				{
					maxX = child.getLocation().getX();
				}
				if (child.getLocation().getY() < minY)
				{
					minY = child.getLocation().getY();
				}
				if (child.getLocation().getY() > maxY)
				{
					maxY = child.getLocation().getY();
				}
			}
			xRange = maxX-minX;
			yRange = maxY-minY;
		}

		for (TreeMapNode child : node.getChildren())
		{
			if (child.getRectangle() == null)
			{
				// Child too small to display.
				continue;
			}

			int level = node.getLevel();

			if (level < randColourLevel)
			{
				// Ensure colour remains random up to the randcolourLevel
				parentColour = null;
			}


			Color childColour = null;
			if (child.getColourValue() != null)
			{
				childColour = getColour(child.getColourValue().floatValue());
			}
			else if (parentColour == null)
			{
				if ((hues[level] == null) || (hues[level].size() == 0))
				{
					int numSiblings = node.getChildCount();
					float h = rand.nextFloat();
					hues[level] = new Vector<Float>();
					for (int i=0; i<numSiblings; i++)
					{
						hues[level].add(h);
						h += 1f/numSiblings;
					}
				}
				hue = hues[level].remove(rand.nextInt(hues[level].size())).floatValue();
			}

			Point2D geoCentre = null;
			if ((xRange > 0) && (yRange > 0))
			{
				double easting = child.getLocation().getX();
				double northing = child.getLocation().getY();
				geoCentre = new Point2D.Double(node.getRectangle().getX() + node.getRectangle().getWidth()*(easting-minX)/xRange,
						node.getRectangle().getY()+node.getRectangle().getHeight() - (node.getRectangle().getHeight()*(northing-minY)/yRange));
			}

			NodePanel nPanel = new NodePanel(this,child.getLabel(),child.getRectangle(),geoCentre,child.isLeaf(),child.getSizeValue()<0,hue, childColour,parentColour,child.getLevel());
			if (child.isLeaf())
			{
				leaves.add(nPanel);
			}
			else
			{
				branches.add(nPanel);
				addRectangles(child,nPanel.getColour());
			}
		}
	}

	/** Finds the colour to be associated with the given colour value. 
	 *  @param colourValue attribute to be mapped with a colour.
	 *  @return Colour associated with the given attribute.
	 */
	private Color getColour(float colourValue)
	{
		return new Color(cTable.findColour(colourValue),true);
	}

	/** Creates a curved arrow between the two given points. Greater angular change is at the source 
	 * of the arrow (p1), in order to provide a visual indication of direction. See 
	 * Fekete, J.D, Wang, D., Dang, N., Aris, A. and Plaisant, C. 'Overlaying Graph Links on TreeMaps',
	 * Information Visualisation Poster Compendium, pp.82-83
	 * @param p1x x coordinate of the start point of the arrow.
	 * @param p1y y coordinate of the start point of the arrow.
	 * @param p2x x coordinate of the end point of the arrow.
	 * @param p2y y coordinate of the end point of the arrow.
	 * @param showArrowHead Arrow head drawn if true.
	 * @return Path representing the arrow.
	 */
	private GeneralPath getArrow(float p1x, float p1y, float p2x, float p2y, float lineWidth, boolean showArrowHead)
	{
		GeneralPath path = new GeneralPath();

		// Set the control point to 60 to the right of the vector, along a quarter its length
		float x = (p2x-p1x)/4f;
		float y = (p2y-p1y)/4f;
		float cx = (float)(p1x + x*Math.cos(CURVE_ANGLE) - y*Math.sin(CURVE_ANGLE));
		float cy = (float)(p1y + y*Math.cos(CURVE_ANGLE) + x*Math.sin(CURVE_ANGLE));

		float arrowSize = lineWidth*2;          // Size of the arrow segments

		float ex = p2x - cx;
		float ey = p2y - cy;
		float abs_e = (float)Math.sqrt(ex*ex + ey*ey);
		ex /= abs_e;
		ey /= abs_e;

		// Creating curved arrow.
		path.moveTo(p1x, p1y);
		path.quadTo(cx, cy, p2x, p2y);

		if (showArrowHead)
		{
			path.lineTo(p2x + (ey-ex)*arrowSize, p2y - (ex + ey)*arrowSize);
			path.moveTo(p2x, p2y);
			path.lineTo(p2x - (ey + ex)*arrowSize, p2y + (ex - ey)*arrowSize);
		}

		return path;
	}

	/** Creates an SVG statement to construct a curved arrow between the two given points. Greater angular change
	 * is at the source of the arrow (p1), in order to provide a visual indication of direction. See 
	 * Fekete, J.D, Wang, D., Dang, N., Aris, A. and Plaisant, C. 'Overlaying Graph Links on TreeMaps',
	 * Information Visualisation Poster Compendium, pp.82-83
	 * @param p1x x coordinate of the start point of the arrow.
	 * @param p1y y coordinate of the start point of the arrow.
	 * @param p2x x coordinate of the end point of the arrow.
	 * @param p2y y coordinate of the end point of the arrow.
	 * @param showArrowHead Arrow head drawn if true.
	 * @return SVG text representing the arrow.
	 */
	private String getSVGArrow(double p1x, double p1y, double p2x, double p2y, double lineWidth, boolean showArrowHead)
	{
		// Set the control point to 60 to the right of the vector, along a quarter its length
		double x = (p2x-p1x)/4.0;
		double y = (p2y-p1y)/4.0;
		double cx = (p1x + x*Math.cos(CURVE_ANGLE) - y*Math.sin(CURVE_ANGLE));
		double cy = (p1y + y*Math.cos(CURVE_ANGLE) + x*Math.sin(CURVE_ANGLE));

		double arrowSize = lineWidth*2;          // Size of the arrow segments

		double ex = p2x - cx;
		double ey = p2y - cy;
		float abs_e = (float)Math.sqrt(ex*ex + ey*ey);
		ex /= abs_e;
		ey /= abs_e;

		if (showArrowHead)
		{
			return new String("<path d=\"M "+p1x+" "+p1y+" Q "+cx+" "+cy+" "+p2x+" "+p2y+
					" L "+(p2x + (ey-ex)*arrowSize)+" "+(p2y - (ex+ey)*arrowSize)+
					" M "+p2x+" "+p2y+
					" L "+(p2x - (ey+ex)*arrowSize)+" "+(p2y + (ex-ey)*arrowSize)+"\" />");
		}
		// Path without arrow head.
		return new String("<path d=\"M "+p1x+" "+p1y+" Q "+cx+" "+cy+" "+p2x+" "+p2y+"\" />");

	}

	/** Writes out the treemap as an SVG file. If the file name extension
	 * is <code>.svgz</code>, then the SVG file will be compressed using GZIP compression.
	 * @param fileName Name of file to create.
	 * @param isTransparent SVG output uses transparency if true. No transparency (false) is useful for PDF conversion.
	 * @return True if written successfully.
	 */
	private boolean writeSVG(String fileName, boolean isTransparent)
	{
		try
		{    
			BufferedWriter outFile;
			double multiplier = 50.0;		// To remove Safari printing bug.
			String borderColourHex = new String("#"+Integer.toHexString((borderColour.getRGB() & 0xffffff) | 0x1000000).substring(1));

			// Decide whether to compress based on file extension.
			if (fileName.toLowerCase().trim().endsWith("svgz"))
			{
				outFile = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))));
			}
			else
			{
				outFile = new BufferedWriter(new FileWriter(fileName));
			}

			// Write out header information. 
			writeLine("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>", outFile);
			writeLine("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "+
					(screenImg.getWidth()*multiplier)+" "+(screenImg.getHeight()*multiplier)+"\">",
					outFile);
			writeLine("",outFile);

			// Write out styles. 
			writeLine("<!-- Format definitions -->", outFile);
			writeLine("<defs>", outFile);
			writeLine(" <style type=\"text/css\"><![CDATA[",outFile);
			if (isTransparent)
			{
				writeLine("   #leafDisp   { stroke:black; stroke-width:"+(leafVectorWidth*multiplier)+"; stroke-opacity:0.6; fill:none; }",outFile);
				writeLine("   #branchDisp { stroke:black; stroke-width:"+(10*vectorWidths[0]*multiplier)+"; stroke-opacity:0.6; fill:none; }",outFile);
				writeLine("   polyline    { stroke:"+borderColourHex+"; stroke-width:0.05%; stroke-opacity:0.3; fill:none; }",outFile);
				writeLine("   polygon     { stroke:black; stroke-width:0.03%; stroke-opacity:0.4; fill-opacity:0.9; }",outFile);
				writeLine("   text        { stroke:none; font-family:sans-serif; text-anchor:middle; }",outFile);
			}
			else
			{
				writeLine("   #leafDisp   { stroke:#cccccc; stroke-width:"+(leafVectorWidth*multiplier)+"; fill:none; }",outFile);
				writeLine("   #branchDisp { stroke:#cccccc; stroke-width:"+(10*vectorWidths[0]*multiplier)+"; fill:none; }",outFile);
				writeLine("   polyline    { stroke:"+borderColourHex+"; stroke-width:0.05%; fill:none; }",outFile);
				writeLine("   polygon     { stroke:none; }",outFile);
				writeLine("   text        { stroke:none; font-family:sans-serif; text-anchor:middle; }",outFile);
			}
			writeLine(" ]]></style>",outFile);
			writeLine("</defs>", outFile);
			writeLine("",outFile);

			// Write out object metadata.
			writeLine("<!-- Object metadata -->", outFile);
			writeLine("<title>Tree map</title>", outFile);
			writeLine("<desc>Produced by treeMapps "+TreeMapApp.VERSION_TEXT+"</desc>", outFile);
			writeLine("",outFile);

			// Write out georeference to pixel transform and title.
			writeLine("<!-- Geometry -->", outFile);
			writeLine("<g id=\"spatialObject\">",outFile);
			writeLine("",outFile);  

			for (NodePanel leaf : leaves)
			{
				// Fill leaf background.
				Rectangle bounds = leaf.getBounds();
				writeLine("<polygon style=\"fill:"+leaf.getHexColour()+";\" points=\""+(multiplier*bounds.x)+","+(multiplier*bounds.y)+" "+
						(multiplier*(bounds.x+bounds.width))+","+(multiplier*bounds.y)+" "+
						(multiplier*(bounds.x+bounds.width))+","+(multiplier*(bounds.y+bounds.height))+" "+
						(multiplier*bounds.x)+","+(multiplier*(bounds.y+bounds.height))+
						"\" />",outFile);
			}

			for (NodePanel branch : branches)
			{
				// Fill branch background.
				Rectangle bounds = branch.getBounds();				
				writeLine("<polyline points=\""+(multiplier*bounds.x)+","+(multiplier*bounds.y)+" "+
						(multiplier*(bounds.x+bounds.width))+","+(multiplier*bounds.y)+" "+
						(multiplier*(bounds.x+bounds.width))+","+(multiplier*(bounds.y+bounds.height))+" "+
						(multiplier*bounds.x)+","+(multiplier*(bounds.y+bounds.height))+" "+
						(multiplier*bounds.x)+","+(multiplier*bounds.y)+
						"\" />",outFile);
			}

			if (showLeafLabels)
			{
				String hexLeafTextColour = Integer.toHexString(leafTextColour.getRGB());
				hexLeafTextColour = new String("#"+hexLeafTextColour.substring(2, hexLeafTextColour.length()));
				float alpha = leafTextColour.getAlpha()/255f;

				for (NodePanel leaf : leaves)
				{
					// Draw leaf label.
					Rectangle bounds = leaf.getBounds();

					if (bounds.width*bounds.height > 0)
					{
						Graphics2D g = (Graphics2D)getGraphics();	// Needed for font metrics.
						String[] lines = cleanXML(leaf.getLabel()).split("\\\\n");

						FontRenderContext frc = g.getFontRenderContext();

						float maxWidth=0;
						for (String line:lines)
						{
							maxWidth = Math.max(maxWidth, (float)leafFont.getStringBounds(line, frc).getWidth());
						}	

						LineMetrics lm = leafFont.getLineMetrics(lines[0], frc);
						float lineHeight = lm.getAscent() + lm.getDescent();
						float totalHeight = lines.length*lineHeight;

						// Work out the scalings required to fit text both vertically and horizontally.        
						float horizXScale = bounds.width / maxWidth;
						float horizYScale = bounds.height / totalHeight;
						float horizScale = horizXScale;
						float vertXScale = bounds.width / totalHeight;
						float vertYScale = bounds.height / maxWidth;
						float vertScale = vertXScale;

						horizScale = Math.min(horizXScale, horizYScale);
						vertScale  = Math.min(vertXScale, vertYScale);

						/*
				        float xScale = bounds.width / maxWidth;
				        float yScale = bounds.height / totalHeight;
				        float scale = xScale;
				        if (xScale > yScale)
				        {
				        	scale = yScale;
				        }
						 */

						if (maxLeafText > 0)
						{
							if (horizScale > maxLeafText/40f)
							{
								// No need to try vertical if text is already large enough.
								horizScale = maxLeafText/40f;
								vertScale = 0;
							}
							else if (vertScale > maxLeafText/40f)
							{
								vertScale = maxLeafText/40f;
							}
						}

						float fontSize = 40*horizScale;

						if (fontSize >= 1)
						{
							for (int i=0; i<lines.length; i++)
							{

								double y = bounds.y + (bounds.height/2) + horizScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

								if (allowVerticalLabels && (vertScale > horizScale))
								{
									fontSize = 40*vertScale;
									y = bounds.y + (bounds.height/2) + vertScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

									// Rotate text about its centre (since this will produce a larger label)
									double cx =  multiplier*(bounds.x + (bounds.width/2.0));
									double cy =  multiplier*(bounds.y + (bounds.height)/2.0);
									writeLine("<g id=\"vertLabel\" transform=\"translate("+cx+","+cy+") rotate(90) translate(-"+cx+",-"+cy+")\">" , outFile);
								}

								if (isTransparent)
								{
									writeLine("<text fill=\""+hexLeafTextColour+"\" style=\"opacity:"+alpha+"\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" font-size=\""+(multiplier*fontSize)+"\" >"+lines[i]+"</text>",outFile);
								}
								else
								{
									writeLine("<text fill=\""+hexLeafTextColour+"\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" font-size=\""+(multiplier*fontSize)+"\" >"+lines[i]+"</text>",outFile);
								}


								if (allowVerticalLabels && (vertScale > horizScale))
								{
									writeLine("</g>", outFile);
								}
							}
						}
					}
				}
			}

			if (showBranchLabels)
			{
				for (NodePanel branch : branches)
				{
					// Draw branch label.
					Rectangle bounds = branch.getBounds();

					if (showBranchLabels && bounds.width > 0)
					{
						int level = branch.getLevel();	
						String hexBranchTextColour = Integer.toHexString(branchTextColours[level-1].getRGB());
						hexBranchTextColour = new String("#"+hexBranchTextColour.substring(2, hexBranchTextColour.length()));
						float alpha = branchTextColours[level].getAlpha()/255f;

						Graphics2D g = (Graphics2D)getGraphics();	// Needed for font metrics.
						String[] lines = cleanXML(branch.getLabel()).split("\\\\n");

						FontRenderContext frc = g.getFontRenderContext();

						float maxWidth=0;
						for (String line:lines)
						{
							maxWidth = Math.max(maxWidth, (float)branchFonts[level-1].getStringBounds(line, frc).getWidth());
						}	

						LineMetrics lm = branchFonts[level-1].getLineMetrics(lines[0], frc);
						float lineHeight = lm.getAscent() + lm.getDescent();
						float totalHeight = lines.length*lineHeight;

						// Work out the scalings required to fit text both vertically and horizontally.        
						float horizXScale = bounds.width / maxWidth;
						float horizYScale = bounds.height / totalHeight;
						float horizScale = horizXScale;
						float vertXScale = bounds.width / totalHeight;
						float vertYScale = bounds.height / maxWidth;
						float vertScale = vertXScale;

						horizScale = Math.min(horizXScale, horizYScale);
						vertScale  = Math.min(vertXScale, vertYScale);

						/*
				        float xScale = bounds.width / maxWidth;
				        float yScale = bounds.height / totalHeight;

				        float scale = xScale;
				        if (xScale > yScale)
				        {
				        	scale = yScale;
				        }
						 */

						// scale = (float)(scale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);

						horizScale = (float)(horizScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);
						vertScale = (float)(vertScale*0.7 + (0.3*bounds.width*bounds.height) / rootArea);

						if (maxBranchTexts[level-1] > 0)
						{
							if (horizScale > maxBranchTexts[level-1]/40f)
							{
								// No need to try vertical if text is already large enough.
								horizScale = maxBranchTexts[level-1]/40f;
								vertScale = 0;
							}
							else if (vertScale > maxBranchTexts[level-1]/40f)
							{
								vertScale = maxBranchTexts[level-1]/40f;
							}
							/*
				        	if (scale > maxBranchTexts[level-1]/40f)
				        	{
				        		scale = maxBranchTexts[level-1]/40f;
				        	}
							 */
						}	
						float fontSize = 40*horizScale;
						if (fontSize >=1)
						{
							for (int i=0; i<lines.length; i++)
							{
								double y = bounds.y + (bounds.height/2) + horizScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

								if (allowVerticalLabels && (vertScale > horizScale))
								{
									fontSize = 40*vertScale;
									y = bounds.y + (bounds.height/2) + vertScale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

									// Rotate text about its centre (since this will produce a larger label)
									double cx =  multiplier*(bounds.x + (bounds.width/2.0));
									double cy =  multiplier*(bounds.y + (bounds.height)/2.0);
									writeLine("<g id=\"vertLabel\" transform=\"translate("+cx+","+cy+") rotate(90) translate(-"+cx+",-"+cy+")\">" , outFile);
								}

								// double y = bounds.y + (bounds.height/2) + scale*((i+1)*lineHeight -totalHeight/2 -lm.getDescent());

								if (level==1)
								{
									if (isTransparent)
									{
										writeLine("<text style=\"opacity:"+alpha+"; font-weight:bold\" fill=\""+hexBranchTextColour+"\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" font-size=\""+(multiplier*(fontSize*1.1))+"\" >"+lines[i]+"</text>",outFile);
									}
									else
									{
										writeLine("<text style=\"font-weight:bold\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" fill=\""+hexBranchTextColour+"\" font-size=\""+(multiplier*(fontSize*1.1))+"\" >"+lines[i]+"</text>",outFile);
									}
								}
								else
								{
									if (isTransparent)
									{
										writeLine("<text style=\"opacity:"+alpha+"\" fill=\""+hexBranchTextColour+"\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" font-size=\""+(multiplier*fontSize)+"\" >"+lines[i]+"</text>",outFile);
									}
									else
									{
										writeLine("<text fill=\""+hexBranchTextColour+"\" x=\""+(multiplier*(bounds.x+bounds.width/2.0))+"\" y=\""+(multiplier*y)+"\" font-size=\""+(multiplier*fontSize)+"\" >"+lines[i]+"</text>",outFile);
									}
								}

								if (allowVerticalLabels && (vertScale > horizScale))
								{
									writeLine("</g>", outFile);
								}
							}
						}
					}
				}				
			}
			writeLine("</g>",outFile);

			// Draw displacement vectors if requested.

			writeLine("<g id=\"branchDisp\">",outFile);
			for (NodePanel branch : branches)
			{
				int level = branch.getLevel();
				if (showBranchDisplacements[level-1])
				{
					Rectangle bounds = branch.getBounds();
					// Draw displacement vector
					if (branch.getGeoBounds() != null)
					{
						writeLine(getSVGArrow((multiplier*(bounds.x+bounds.width/2.0)), multiplier*(bounds.y+bounds.height/2.0), 
								multiplier*branch.getGeoBounds().getX(), multiplier*branch.getGeoBounds().getY(), vectorWidths[level-1]*multiplier, showArrowHead),outFile);	
					}
				}
			}
			writeLine("</g>",outFile);

			if (showLeafDisplacement)
			{
				writeLine("<g id=\"leafDisp\">",outFile);

				for (NodePanel leaf : leaves)
				{
					Rectangle bounds = leaf.getBounds();
					// Draw displacement vector
					if (leaf.getGeoBounds() != null)
					{
						writeLine(getSVGArrow((multiplier*(bounds.x+bounds.width/2.0)), multiplier*(bounds.y+bounds.height/2.0), 
								multiplier*leaf.getGeoBounds().getX(), multiplier*leaf.getGeoBounds().getY(), leafVectorWidth*multiplier, showArrowHead),outFile);
					}
				}
				writeLine("</g>",outFile);
			}

			// Write out footer.
			writeLine("</svg>",outFile);
			outFile.close();
		} 
		catch (IOException e)
		{ 
			System.err.println("Error writing SVG file (" + e + ")");
			return false;
		}
		return true;
	}  


	/** Writes out a given line of text on its own line to the given
	 * buffered writer.
	 * @param text Text to write.
	 * @param outFile Buffered file writer to receive the text.
	 * @return true if written successfully.
	 */
	private boolean writeLine(String text, BufferedWriter outFile)
	{
		try
		{
			outFile.write(text,0,text.length());
			outFile.newLine();
		}
		catch (IOException e)
		{
			System.err.println("Cannot write text output: "+e);
			return false;
		}
		return true;
	}

	/** Cleans the given text so it is XML-safe.
	 * @param text Text to clean.
	 * @return XML-friendly version of the text.
	 */
	private String cleanXML(String text)
	{
		Matcher matcher;
		String cleanText;

		matcher = cleanAmp.matcher(text);
		cleanText = matcher.replaceAll("&amp;");

		matcher = cleanLT.matcher(cleanText);
		cleanText = matcher.replaceAll("&lt;");

		matcher = cleanGT.matcher(cleanText);
		cleanText = matcher.replaceAll("&gt;");

		matcher = cleanQuote.matcher(cleanText);
		cleanText = matcher.replaceAll("&quot;");

		return cleanText;
	}

	/** Calculates the transformations required to convert between
	 * pixel coordinates and georeferenced coordinates.
	 */
	private void calcTransformation()
	{
		int panelWidth  = getWidth();
		int panelHeight = getHeight();

		calcTransformation(panelWidth,panelHeight, 0,false);
	}

	/** Calculates the transformations required to convert between
	 * pixel coordinates and georeferenced coordinates.
	 * @param panelWidth Width of panel in which image is drawn.
	 * @param panelHeight Height of panel in which image is drawn.
	 * @param borderSize Size of border around image in panel.
	 * @param ignoreZoom If true, will ignore current zoom settings when calculating transformation.
	 */
	private void calcTransformation(int panelWidth, int panelHeight, int borderSize, boolean ignoreZoom)
	{
		trans = new AffineTransform();
		iTrans = new AffineTransform();

		// Image must have been created to continue
		if ((screenImg == null) || (screenImg.getWidth() == 0) || (screenImg.getHeight()==0))
		{
			return;
		}

		if (ignoreZoom)
		{
			double scaling = Math.min((float)panelWidth/screenImg.getWidth(), (float)panelHeight/screenImg.getHeight());
			float centreX = (float)(panelWidth - (screenImg.getWidth()*scaling))/2;
			float centreY = (float)(panelHeight - (screenImg.getHeight()*scaling))/-2;

			// Geographic to pixel transformation.
			trans.translate(borderSize+centreX, panelHeight+borderSize+centreY);
			trans.scale(scaling,-scaling);
			//trans.translate(-bounds.getXOrigin(),-bounds.getYOrigin());

			// Pixel to geographic transformation.
			//iTrans.translate(bounds.getXOrigin(),bounds.getYOrigin());
			iTrans.scale(1/scaling,-1/scaling);
			iTrans.translate(-centreX-borderSize,-centreY-panelHeight-borderSize);
		}
		else
		{
			double scaling = zoomFactor*Math.min((float)panelWidth/screenImg.getWidth(), (float)panelHeight/screenImg.getHeight());
			float centreX = (float)(panelWidth - (screenImg.getWidth()*scaling))/2;
			float centreY = (float)(panelHeight - (screenImg.getHeight()*scaling))/-2;

			// Geographic to pixel transformation.
			trans.translate(borderSize+centreX+panOffset.getX(), panelHeight+borderSize+centreY-panOffset.getY());
			trans.scale(scaling,-scaling);
			// trans.translate(-bounds.getXOrigin(),-bounds.getYOrigin());

			// Pixel to geographic transformation.
			//iTrans.translate(bounds.getXOrigin(),bounds.getYOrigin());
			iTrans.scale(1/scaling,-1/scaling);
			iTrans.translate(-centreX-panOffset.getX()-borderSize,panOffset.getY()-centreY-panelHeight-borderSize);
		}
	}

	/** Transforms the given point from georeferenced to pixel coordinates.
	 * @param geo Georeferenced coordinate pair to transform.
	 * @return Pixel coordinate pair representing the given georeferenced point.
	 */
	private Point2D getGeoToPixel(Point2D geo)
	{
		Point2D pxl = new Point2D.Float();
		trans.transform(geo,pxl);

		return pxl; 
	}

	/** Transforms the given point from pixel to georeferenced coordinates.
	 * @param pxl Pixel coordinate pair to transform.
	 * @return Georeferenced coordinate pair representing the given pixel coordinates.
	 */
	public Point2D getPixelToGeo(Point2D pxl)
	{
		Point2D geo = new Point2D.Float();
		iTrans.transform(pxl,geo);

		return geo;        
	}


	/** Attempts to build a list of font objects for displaying treemap labels.
	 *  @param fontNames List of font names.
	 */
	private void buildBranchFonts(String[] fontNames)
	{
		Font defFont = new Font(fontNames[0],Font.PLAIN,40);

		branchFonts = new Font[fontNames.length];
		for (int i=0; i<fontNames.length; i++)
		{  
			if (fontNames[i].equalsIgnoreCase(fontNames[0]))
			{
				branchFonts[i] = defFont;
			}
			else
			{
				branchFonts[i] = new Font(fontNames[i],Font.PLAIN,40);
			}
		}
	}

	/** Converts the given integer into a hex string, padded with a zero if only one digit.
	 *  @param i Integer to convert.
	 *  @return Hex version of integer.
	 */ 
	private static String paddedHex(int i) 
	{
		String s = Integer.toHexString (i);
		if (s.length () == 1) 
		{
			s = "0" + s;
		}
		return s;
	}

	// -------------------------------------------- Nested classes --------------------------------------------




	/** Handles mouse clicks on the panel.
	 */
	private class MouseClickMonitor extends MouseAdapter
	{
		/** Handles a mouse click in the panel. Allows panning and zooming of the tree map image.  
		 * @param e Mouse event associated with the click.
		 */
		public void mousePressed(MouseEvent e)
		{
			// Store initial position.
			clickPosition = e.getPoint();
			oldPosition   = e.getPoint();

			if ((e.getModifiers() & SECONDARY_MASK) > 0)    // RH mouse button pressed.
			{
				mode = PAN;
			}

			// While zooming, don't recalculate tree.
			//isZooming = true;
		}

		/** Handles release of mouse click in the panel by returning from pan to zoom mode
		 * if we were in pan mode.
		 * @param e Mouse event associated with the click.
		 */
		public void mouseReleased(MouseEvent e)
		{   
			mode = ZOOM;

			//isZooming = false;
			localPanOffset = new Point2D.Float(0,0);
			localZoomFactor = 1;
			repaint();       
		}
	}

	/** Handles mouse movement over the panel.
	 */
	private class MouseMoveMonitor extends MouseMotionAdapter
	{    
		/** Checks for mouse dragging and performs the relevant graphical feedback
		 * depending on the display mode (panning, zooming, etc.).
		 * @param mouseEvent Mouse dragging event.
		 */
		public void mouseDragged(MouseEvent mouseEvent)
		{
			if (mode == PAN)
			{               

				localPanOffset.setLocation(localPanOffset.getX()+mouseEvent.getX() - oldPosition.x,
						localPanOffset.getY()+oldPosition.y - mouseEvent.getY());
				panOffset.setLocation((panOffset.getX()+ mouseEvent.getX() - oldPosition.x),
						panOffset.getY()+ oldPosition.y - mouseEvent.getY());
				calcTransformation();
				repaint();  
			}
			else if (mode == ZOOM)
			{                           
				if (oldPosition.y - mouseEvent.getY() > 0)
				{    
					zoomFactor *= 1.1f;       	// Zoom in.
					localZoomFactor *= 1.1f;
				}
				else if (oldPosition.y - mouseEvent.getY() < 0)
				{    
					zoomFactor *= 0.9f;			// Zoom out.
					localZoomFactor *= 0.9f;
				}

				// Find georeferenced location of first mouse click.
				Point2D geoClick = getPixelToGeo(new Point2D.Float(clickPosition.x,clickPosition.y));

				// Do the zooming transformation.   
				calcTransformation();

				// Find new pixel location of original mouse click location.
				Point2D newClickPosition = getGeoToPixel(geoClick);

				// Translate by change in click position.
				panOffset.x += clickPosition.x - newClickPosition.getX();
				panOffset.y -= clickPosition.y - newClickPosition.getY();

				// Vector image translation is simpler - just translate to origin, scale and translate back.
				localPanOffset.x = clickPosition.x*(1-localZoomFactor);
				localPanOffset.y = clickPosition.y*(localZoomFactor-1);

				// Do the final transformation and display.
				calcTransformation();
				repaint();
			}
			oldPosition = mouseEvent.getPoint(); 
		}
	}

	/** Handles mouse wheel movement on the panel.
	 */
	private class MouseWheelMonitor implements MouseWheelListener
	{
		/** Responds to a mouse wheel change event by zooming in or out of the image.
		 * @param event Mouse wheel event.
		 */
		public void mouseWheelMoved(MouseWheelEvent event)
		{     
			//isZooming = true;

			if (event.getWheelRotation() < 0)
			{     
				zoomFactor *= 1.1f;       	// Zoom in.
				localZoomFactor *= 1.1f;
			}
			else
			{     
				zoomFactor *= 0.9f;			// Zoom out.
				localZoomFactor *= 0.9f;	
			}

			// Find georeferenced location of mouse position while wheel is moving.
			Point2D geoClick  = getPixelToGeo(new Point2D.Float(event.getX(),event.getY()));

			// Do the zooming transformation.   
			calcTransformation();

			// Find new pixel location of original mouse location.
			Point2D newClickPosition = getGeoToPixel(geoClick);

			// Translate by the change in screen position before and after zooming.
			panOffset.x += event.getX() - newClickPosition.getX();
			panOffset.y -= event.getY() - newClickPosition.getY();

			// Translate to origin, scale and translate back.
			localPanOffset.x = event.getX()*(1-localZoomFactor);
			localPanOffset.y = event.getY()*(localZoomFactor-1);

			// Do the final transformation and display.
			calcTransformation();

			// Return interaction to previous mode.      
			//isZooming = false;
			localPanOffset = new Point2D.Float(0,0);
			localZoomFactor = 1;
			repaint();
		}
	} 

	/** Handles changes in the panel's status.
	 */
	private class PanelSizeMonitor extends ComponentAdapter
	{
		/** Handles panel resizing events by updating the pixel georeference
		 * transformation to account for new panel dimensions.
		 * @param e Panel resizing event.
		 */
		public void componentResized(ComponentEvent e)
		{
			// Create resized offscreen image on which to draw vector maps.
			//        screenImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			calcTransformation();
		}
	}
}