package org.gicentre.treemappa;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.gicentre.io.ShapefileWriter;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.io.DOMProcessor;
import org.w3c.dom.Node;

//  **************************************************************************
/** Class to read tree data and create treemaps and treemap output files. 
 *  @author Jo Wood, giCentre.
 *  @version 3.0.1, 4th April, 2011.
 */
//  **************************************************************************

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

public class TreeMappa 
{
	// ------------------ Class and object variables --------------------

	private DOMProcessor dom;						// XML tree.
	private DefaultTreeModel tree;					// For internal tree structure.
	private TreeMapNode root;
	private	TreeMapNode rootNode;
	private TreeFrame treeFrame;
	private ColourTable defCTable;

	private Rectangle2D m_r  = new Rectangle2D.Double();
	private List<TreeMapNode>nodesToLayout;			// Children to be laid out in given node.
	private Vector<OrderDistance> leafDistances;	// For R-squared calculation of order-distance relationship.

	private double[] borderWidths;					// Width of border surrounding node in treemap.

	private double aspectRatio,readability,distDisplacement,angDisplacement;
	private int numNodes, numAdjacentLeaves, numSpatialNodes;
	private double west,south,east,north;			// Geographic bounds of spatial layouts.

	private Layout[] layoutTypes;
	private Layout[] alignments;
	private int maxDepth = 0;
	private boolean isVerbose;
	private boolean needsRebuild;					// Indicates if some properties of the treemap have been changed
	// that will require a rebuild of the treemap to come into effect.

	private static final int CSV = 0;
	private static final int CSV_COMPACT = 1;
	private static final int CSV_SPATIAL = 2;

	private double targetAR = 1f;					// TODO: Replace this with an AR stored in node.

	private TreeMapProperties props;				// Treemap configuration properties.

	// ------------------------- Constructor ----------------------------

	/** Creates an object capable of creating a treemap from the hierarchical data identified in the given
	 *  properties. These properties can also specify the appearance and output format of the treemap.
	 *  @param props Properties defining the data and appearance associated with the treemap.
	 */
	public TreeMappa(TreeMapProperties props)
	{
		this.props = props;
		isVerbose = props.getIsVerbose();
		west  =  Float.MAX_VALUE;
		east  = -Float.MAX_VALUE;
		south =  Float.MAX_VALUE;
		north = -Float.MAX_VALUE;
		needsRebuild = true;
	}

	// ---------------------------------------- Methods ----------------------------------------

	/** Reads in the data from the file identified in the treemap configuration file provided to 
	 *  the constructor.
	 *  @return True if data read without problems.
	 */
	public boolean readData()
	{
		boolean textOnly = props.getTextOnly();
		String fileType = props.getFileType();
		String inFileName = props.getInFileName();
		boolean useLabels = props.getUseLabels();

		if (inFileName == null)
		{
			System.err.println("No file specified from which to read tree data.");
			return false;
		}

		// Read in the external tree data.
		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Reading data.");
		}

		if  (fileType.equalsIgnoreCase("treeml"))
		{
			if (readTreeML(inFileName,useLabels) == false)
			{
				System.err.println("Problem reading treeML file.");
				return false;
			}
		}
		else if (fileType.equalsIgnoreCase("csv"))
		{
			if (readCSV(inFileName,useLabels,CSV) == false)
			{
				System.err.println("Problem reading CSV file.");
				return false;
			}
		}
		else if (fileType.equalsIgnoreCase("csvcompact"))
		{
			if (readCSV(inFileName,useLabels,CSV_COMPACT) == false)
			{
				System.err.println("Problem reading compact CSV file.");
				return false;
			}
		}
		else if (fileType.equalsIgnoreCase("csvspatial"))
		{
			if (readCSV(inFileName,useLabels,CSV_SPATIAL) == false)
			{
				System.err.println("Problem reading spatial CSV file.");
				return false;
			}
		}
		else
		{
			System.err.println("Unknown file type: '"+fileType+"'");
			return false;
		}

		// No problems if we get this far.
		needsRebuild = true;
		return true;
	}
	
	/** Reads in the data from the file identified in the given PTreeMappa object. This version is used
	 *  when reading data from a Processing applet and avoids security exceptions by assuming data are
	 *  stored in the Processing applet location.
	 *  @param pTreeMappa Processing interface to treemappa that handles files.
	 *  @return True if data read without problems.
	 */
	public boolean readData(PTreeMappa pTreeMappa)
	{
		boolean textOnly = props.getTextOnly();
		String fileType = props.getFileType();
		boolean useLabels = props.getUseLabels();
		
		BufferedReader bReader = pTreeMappa.parent.createReader(props.getInFileName());

		if (bReader == null)
		{
			System.err.println("No file specified from which to read tree data.");
			return false;
		}

		// Read in the external tree data.
		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Reading data.");
		}

		if  (fileType.equalsIgnoreCase("treeml"))
		{
			if (readTreeML(pTreeMappa.createDOM(),props.getInFileName(),useLabels) == false)
			{
				System.err.println("Problem reading treeML file.");
				return false;
			}
			return false;
		}
		else if (fileType.equalsIgnoreCase("csv"))
		{
			if (readCSV(bReader,useLabels,CSV) == false)
			{
				System.err.println("Problem reading CSV file.");
				return false;
			}
		}
		else if (fileType.equalsIgnoreCase("csvcompact"))
		{
			if (readCSV(bReader,useLabels,CSV_COMPACT) == false)
			{
				System.err.println("Problem reading compact CSV file.");
				return false;
			}
		}
		else if (fileType.equalsIgnoreCase("csvspatial"))
		{
			if (readCSV(bReader,useLabels,CSV_SPATIAL) == false)
			{
				System.err.println("Problem reading spatial CSV file.");
				return false;
			}
		}
		else
		{
			System.err.println("Unknown file type: '"+fileType+"'");
			return false;
		}

		// No problems if we get this far.
		needsRebuild = true;
		return true;
	}


	/** Builds the treemap from the hierarchical data stored in this object. The size of the treemap
	 *  is determined by configuration properties supplied to the constructor. This method should
	 *  only be called after <code>readData()</code> has been called to store the hierarchical data to map.
	 *  @return True if the tree has been built without problems.
	 */
	public boolean buildTreeMap()
	{
		boolean textOnly  = props.getTextOnly();
		double rootWidth  = props.getWidth();
		double rootHeight = props.getHeight();
		storeLayoutTypes(props.getLayouts());
		storeLayoutAlignments(props.getAlignments());
		borderWidths = props.getBorders();

		if (root == null)
		{
			System.err.println("Error: Must read data before building treemap.");
			return false;
		}

		// Ensure size values have be propagated up the entire tree and that nodes are sorted correctly.
		root.resetAccumulation();	
		for (int i=0; i<=maxDepth; i++)
		{
			root.sortAtLevel(i);
		}

		// Build the treeMap.
		rootNode = (TreeMapNode)tree.getRoot();
		Rectangle2D rootSize = new Rectangle2D.Double(0,0,rootWidth,rootHeight);

		if (layoutTypes[0] == Layout.MORTON)
		{
			nodesToLayout = new MortonList<TreeMapNode>();
		}
		else
		{
			nodesToLayout = new Vector<TreeMapNode>();
		}
		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Computing areas.");
		}
		rootNode.setRectangle(new Rectangle2D.Double(rootSize.getX(),rootSize.getY(),rootSize.getWidth(),rootSize.getHeight()));
		m_r.setRect(rootSize.getX(),rootSize.getY(),rootSize.getWidth(),rootSize.getHeight()); 
		rootNode.setArea(rootNode.getRectangle().getWidth()*rootNode.getRectangle().getHeight()); 
		computeAreas(rootNode);    
		updateArea(root, m_r);

		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Laying out nodes.");      
		}
		layout(root, m_r);

		// Build adjacencies
		//System.err.println("Building adjacencies -----------------------");

		root.resetNeighbours();

		for (TreeMapNode node : root)
		{
			TreeSet<TreeMapNode>candidates = new TreeSet<TreeMapNode>();
			if (node.getNeighbours() == null)
			{
				node.resetNeighbours();
			}

			// Add all the children of this node to a pool of those to check for adjacency.
			candidates.addAll(node.getChildren());

			// Also add any children of nodes that are adjacent to this one to the pool.
			//candidates.addAll(node.getNeighbours());

			// TODO: This is currently an O(n-squared) operation. Can we improve on that?
			for (TreeMapNode candidate : candidates)
			{
				for (TreeMapNode neighbour: candidates)
				{
					if ((candidate.getNeighbours() == null) || (!candidate.getNeighbours().contains(neighbour)))
					{
						if (isAdjacent(candidate,neighbour))
						{
							candidate.addNeighbour(neighbour);
							neighbour.addNeighbour(candidate);
						}
					}
				}
			}
		}

		needsRebuild = false;
		return true;
	}

	private boolean isAdjacent(TreeMapNode n1, TreeMapNode n2)
	{
		if (n1.equals(n2))
		{
			// If both nodes are identical, don't regard them as neighbouring.
			return false;
		}

		// Test for separate rectangles. If not separate, we can regard them as being adjacent.  
		if ((n1.getRectangle().getMaxX() < n2.getRectangle().getMinX()) || 
				(n1.getRectangle().getMinX() > n2.getRectangle().getMaxX()) ||
				(n1.getRectangle().getMaxY() < n2.getRectangle().getMinY()) || 
				(n1.getRectangle().getMinY() > n2.getRectangle().getMaxY()) )
		{
			// Must be separate, so cannot be adjacent.
			return false;
		}

		// Assume if not separate, must be adjacent (could be intersecting if small error in locations).
		return true;
	}

	/** Saves the current treeMap as a file. This might be a shapefile or text file depending on options
	 *  specified by the configuration properties supplied to the constructor.
	 *  @return True if output written successfully.
	 */
	public boolean writeOutput()
	{
		if (rootNode == null)
		{
			System.err.println("Error: Must build treeMap before writing output.");
			return false;
		}

		boolean textOnly  = props.getTextOnly();
		String outFileName = props.getOutFileName();

		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Generating output.");
		}

		if (outFileName == null)
		{
			System.err.println("No output file specified when attempting to write output.");
			return false;
		}

		if (outFileName.toLowerCase().endsWith(".shp"))
		{
			if (ShapefileWriter.writeNodes(root, outFileName) == true)
			{
				if ((textOnly == false) && (isVerbose))
				{
					System.out.println("\tTreeMap written as shapefile to '"+outFileName+"'");
				}
			}
			else
			{
				System.err.println("\tProblem writing treeMap as shapefile to '"+outFileName+"'");
				return false;
			}
		}
		else
		{
			File outFile = new File(outFileName);
			try 
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
				writeTreeMapAsText(out);
				out.close();
				if ((textOnly == false) && (isVerbose))
				{
					System.out.println("\tTreeMap coordinates written as text file to '"+outFile.getAbsolutePath()+"'");
				}
			} 
			catch (IOException e) 
			{
				System.err.println("Problem creating text output file '"+outFile.getAbsolutePath()+"'");
				return false;
			}
		}

		// No problem writing output file if we get this far.
		return true;
	}

	/** Creates a window in which the treemap is displayed.
	 *  @return New window created or null if there was a problem creating treeMap window.
	 */
	public TreeFrame createWindow()
	{
		if (rootNode == null)
		{
			System.err.println("Error: Must build treeMap before displaying it in a window.");
			return null;
		}

		if (props.getTextOnly() == true)
		{
			System.err.println("Error: Cannot create treeMap window in textOnly mode.");
			return null;
		}

		if (isVerbose)
		{
			System.out.println("Creating window.");
		}
		treeFrame = new TreeFrame(this);
		return treeFrame;
	}

	/** Creates a panel in which the treemap is displayed. This method can be used if you wish 
	 *  to incorporate a treemap display inside its own GUI.
	 *  @return Panel in which the treemap is displayed or null if problem creating treemap panel.
	 */
	public TreeMapPanel createPanel()
	{
		if (rootNode == null)
		{
			System.err.println("Error: Must build treeMap before a panel can be created.");
			return null;
		}
		return new TreeMapPanel((int)props.getWidth(),(int)props.getHeight(),this);
	}

	/** Provides the root node of the treeMap. The root should contain links to all its children and
	 *  therefore the entire treemap.
	 *  @return Root of the treemap or null if the treemap has not yet been built.
	 */
	public TreeMapNode getRoot()
	{
		if (tree == null)
		{
			return null;
		}
		return (TreeMapNode)tree.getRoot();
	}

	/** Sets the new tree data to be represented by the treemap. Note that the new treemap will not be created
	 *  until a call to <code>buildTreeMap()</code> is made. 
	 * @param root Root of the tree to be represented as a treemap.
	 */
	public void setRoot(TreeMapNode root)
	{
		this.root = root;
		tree = new DefaultTreeModel(root);
		needsRebuild = true;
	}
	
	/** Sets the default colour table to use if not specified in configuration file.
	 *  @param cTable Default colour table.
	 */
	void setDefaultColourTable(ColourTable cTable)
	{
		this.defCTable = cTable;
	}
	
	/** Reports the default colour table to use if not specified in configuration file.
	 *  @return cTable Default colour table.
	 */
	ColourTable getDefaultColourTable()
	{
		return defCTable;
	}
	
	/** Saves the treeMap displayed in the treeMap window as an image file. The name of the file should be
	 *  specified in the configuration properties supplied to the constructor.
	 *  @return True if image written successfully.
	 */
	public boolean writeImage()
	{
		if (treeFrame == null)
		{
			System.err.println("Error: Must create treeMap window before writing treemap to an image file.");
			return false;
		}

		String imgFileName = props.getImageFileName();
		if (imgFileName == null)
		{
			System.err.println("Error: No image file name specified when attempting to write treeMap image.");
			return false;
		}

		if (isVerbose)
		{
			System.out.println("Writing image...");
		}
		return treeFrame.writeImage(imgFileName);
	}

	/** Display the summary statistics describing the treemap.
	 *  @return True if summary statistics were able to be calculated.
	 */
	public boolean showStatistics()
	{
		if ((rootNode == null) || (needsRebuild == true))
		{
			System.err.println("Error: Must build treeMap before calculating summary statistics.");
			return false;
		}

		boolean textOnly  = props.getTextOnly();
		double rootWidth  = props.getWidth();
		double rootHeight = props.getHeight();

		if ((textOnly == false) && (isVerbose))
		{
			System.out.println("Calculating statistics.");
		}
		numNodes = 0;
		numAdjacentLeaves = 0;
		numSpatialNodes = 0;
		aspectRatio = 0;
		readability = 0;
		distDisplacement = 0;
		angDisplacement = 0;
		leafDistances = new Vector<OrderDistance>();

		TreeMapNode startNode = rootNode;
		if (layoutsContainSpatial())
		{
			calcDisplacementStatistics(startNode);
		}
		distDisplacement /= (numSpatialNodes*Math.sqrt(rootWidth*rootHeight));
		angDisplacement = (angDisplacement*180/Math.PI)/numSpatialNodes;

		/*
        if (startNode.getChildCount() == 1)
        {
        	// Ignore single level 1 node as it will simply reflect the aspect ratio of the user-defined parent rectangle.
        	startNode = startNode.getChildren().get(0);
        }*/

		for (TreeMapNode child : startNode.getChildren())
		{
			calcStatistics(child);
		}        
		aspectRatio /= numNodes;


		DecimalFormat df = new DecimalFormat("#0.##");
		for (int i=0; i<=maxDepth; i++)
		{
			System.out.println("\tLevel "+i+":\t"+layoutTypes[i]+" with border of "+borderWidths[i]);
		}
		System.out.println("\tNumber of nodes:\t"+numNodes);
		System.out.println("\tMaximum tree depth:\t"+(maxDepth+1));
		System.out.println("\tMean aspect ratio:\t"+df.format(aspectRatio));
		if (numAdjacentLeaves > 0)
		{
			readability = 1 - readability/numAdjacentLeaves;
			System.out.println("\tReadability:     \t"+df.format(readability));
		}
		if (layoutsContainSpatial())
		{
			System.out.println("\tDistance displacement:\t"+df.format(distDisplacement));
			System.out.println("\tAngular displacement:\t"+df.format(angDisplacement));
		}
		else
		{
			if (leafDistances.size() > 2)
			{
				// Calculate R-squared for order-distance relationship.
				Collections.sort(leafDistances);

				// Pass one to establish mean of rank order and distances.
				double xBar=0, yBar=0;
				double xt,yt, sXX=0,sYY=0, sXY=0;
				double r;
				for (int i=0; i<leafDistances.size(); i++)
				{
					xBar += i;		// Rank order for orders.
					yBar += leafDistances.get(i).getDistance();	  	        		
				}

				xBar /= leafDistances.size();
				yBar /= leafDistances.size();

				// Pass two to establish correlation coefficients.
				for (int i=0; i<leafDistances.size(); i++)
				{
					xt = i-xBar;
					yt = leafDistances.get(i).getDistance()-yBar;
					sXX += xt*xt;
					sYY += yt*yt;
					sXY += xt*yt;
				}

				r = sXY/Math.sqrt(sXX*sYY);
				System.out.println("\tOrder-distance R-sq:\t"+df.format(r*r));
			}
		}
		return true;
	}


	/** Provides the configuration options used to create the treemap. Note that these properties will
	 *  reflect any programmatic changes that have been made that affect the treemap. For example, if
	 *  <code>setWidth()</code> or <code>setBorder()</code> have been called, the configuration options
	 *  will reflect these new properties.
	 *  @return Configuration options used to create the treemap.
	 */
	public TreeMapProperties getConfig()
	{
		return props;
	}

	/** Provides a <code>TreeModel</code> tree model view of this treemap object. Can be used for
	 *  Swing components that require a TreeModel.
	 *  @return TreeModel view of this tree, or null if the treemap has not yet been created.
	 */
	public TreeModel getTreeModel()
	{
		return tree;
	}

	/** Reports whether or not the treemap needs building. It will need rebuilding if (a) it has
	 *  not yet been created, or (b) some property that affects layout has been changed, such
	 *  as the layout algorithm or the border size. The treemap can be rebuilt with a call to
	 *  <code>buildTreeMap()</code>. Note that if you make changes to one or more of the 
	 *  <code>TreeMapNode</code>s dirctly (ie not via one of the methods in this class) this may
	 *  require a rebuilding of the tree before they come into effect even if this method returns false.
	 *  @return True if the treemap needs rebuilding.
	 */
	public boolean getNeedsRebuilding()
	{
		return needsRebuild;
	}

	// ------------------ Requests that change the tree layout but not the tree structure

	/** Sets the alignment settings for all levels within the treemap. Note that since this operation 
	 *  requires the recalculation of the treemap layout, no changes will be made until
	 *  <code>buildTreeMap()</code> is called.
	 *  @param alignment Alignment setting to use. Valid values are 'horizontal', 'vertical' and 'free'.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setAlignments(String alignment)
	{
		boolean success = props.setParameter(TreeMapProperties.ALIGN, alignment);
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the alignment setting for the given level of the treemap. Note that since this operation 
	 *  requires the recalculation of the treemap layout, no changes will be made until
	 *  <code>buildTreeMap()</code> is called.
	 *  @param level Level of the hierarchy at which the given border setting is to apply.
	 *  @param alignment Alignment setting to use. Valid values are 'horizontal', 'vertical' and 'free'.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setAlignment(int level, String alignment)
	{
		boolean success = props.setParameter(TreeMapProperties.ALIGN, alignment);
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the border size of the treemap. Note that since this operation requires the recalculation
	 *  of the treemap layout, no changes will be made until <code>buildTreeMap()</code> is called.
	 *  @param borderSize Border size used to separate treemap nodes.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setBorders(double borderSize)
	{
		boolean success = props.setParameter(TreeMapProperties.BORDER, String.valueOf(borderSize));
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the border size of the nodes at the given level in the treemap. Note that since this
	 *  operation requires the recalculation of the treemap layout, no changes will be made until 
	 *  <code>buildTreeMap()</code> is called.
	 *  @param level Level of the hierarchy at which the given border setting is to apply.
	 *  @param borderSize Border size used to separate treemap nodes.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setBorder(int level, float borderSize)
	{
		boolean success = props.setParameter(TreeMapProperties.BORDER+level, String.valueOf(borderSize));
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the a new default layout for the treemap. Note that since this operation requires the
	 *  recalculation of the treemap layout, no changes will be made until <code>buildTreeMap()</code> 
	 *  is called.
	 *  @param layout Name of new layout algorithm to use.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setLayouts(String layout)
	{
		boolean success = props.setParameter(TreeMapProperties.LAYOUT, layout);
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the new layout for the given level in the treemap. Note that since this operation 
	 *  requires the recalculation of the treemap layout, no changes will be made until 
	 *  <code>buildTreeMap()</code> is called.
	 *  @param level Level of the hierarchy at which the given layout setting is to apply.
	 *  @param layout Name of new layout algorithm to use.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setLayout(int level, String layout)
	{
		boolean success = props.setParameter(TreeMapProperties.LAYOUT+level, layout);
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the new width of the treemap. This may or may not correspond to real pixel coordinates
	 *  depending on whether a <code>TreeMapPanel</code> has been created to display this treemap.
	 *  Note that since this operation requires the recalculation of the treemap layout, no changes 
	 *  will be made until <code>buildTreeMap()</code> is called.
	 *  @param width New width of of the treemap.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setWidth(float width)
	{
		boolean success = props.setParameter(TreeMapProperties.WIDTH, String.valueOf(width));
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}

	/** Sets the new height of the treemap. This may or may not correspond to real pixel coordinates
	 *  depending on whether a <code>TreeMapPanel</code> has been created to display this treemap.
	 *  Note that since this operation requires the recalculation of the treemap layout, no changes 
	 *  will be made until <code>buildTreeMap()</code> is called.
	 *  @param height New height of of the treemap.
	 *  @return True if new property is updated successfully.
	 */
	public boolean setHeight(float height)
	{
		boolean success = props.setParameter(TreeMapProperties.HEIGHT, String.valueOf(height));
		if (success)
		{
			needsRebuild = true;
			return true;
		}
		return false;
	}


	// ------------------------------------ Private methods ------------------------------------

	/** Determines if there are any spatial layouts in the treemap at any level.
	 * This can be used to check if spatial displacement statistics need to be calculated.
	 * @return True if there is a spatial layout of some kind at one or more levels. 
	 */
	private boolean layoutsContainSpatial()
	{
		for (Layout layoutType : layoutTypes)
		{
			if ((layoutType == Layout.SPATIAL) || (layoutType == Layout.SPATIAL_AV) || (layoutType == Layout.PIVOT_SPACE))
			{
				return true;
			}
		}
		return false;
	}

	/** Parses a set of layout type strings and stores the layout types. Can account for null and undefined layout strings.
	 *  @param layouts Array holding the strings representing layout types at each level in the treemap hierarchy.
	 */
	private void storeLayoutTypes(String[] layouts)
	{
		if (layouts == null)
		{
			layoutTypes = new Layout[TreeMapApp.MAX_DEPTH];
			for (int i=0; i<layoutTypes.length; i++)
			{
				layoutTypes[i] = Layout.ORDERED_SQUARIFIED;
			}
			return;
		}

		layoutTypes = new Layout[layouts.length];
		for (int i=0; i<layouts.length; i++)
		{
			if (layouts[i].equalsIgnoreCase("sliceanddice"))
			{
				layoutTypes[i] = Layout.SLICE_AND_DICE;
			}
			else if (layouts[i].equalsIgnoreCase("squarified"))
			{
				layoutTypes[i] = Layout.SQUARIFIED;
			}
			else if (layouts[i].equalsIgnoreCase("spatial"))
			{
				layoutTypes[i] = Layout.SPATIAL;
			}
			else if (layouts[i].equalsIgnoreCase("spatialAv"))
			{
				layoutTypes[i] = Layout.SPATIAL_AV;
			}
			else if (layouts[i].equalsIgnoreCase("pivotsize"))
			{
				layoutTypes[i] = Layout.PIVOT_SIZE;
			}
			else if (layouts[i].equalsIgnoreCase("pivotmiddle"))
			{
				layoutTypes[i] = Layout.PIVOT_MIDDLE;
			}
			else if (layouts[i].equalsIgnoreCase("pivotsplit"))
			{
				layoutTypes[i] = Layout.PIVOT_SPLIT_SIZE;
			}
			else if (layouts[i].equalsIgnoreCase("pivotspace"))
			{
				layoutTypes[i] = Layout.PIVOT_SPACE;
			}
			else if (layouts[i].equalsIgnoreCase("strip"))
			{
				layoutTypes[i] = Layout.STRIP;
			}
			else if (layouts[i].equalsIgnoreCase("morton"))
			{
				layoutTypes[i] = Layout.MORTON;
			}
			else 
			{
				layoutTypes[i] = Layout.ORDERED_SQUARIFIED;
			}
		}
	}

	/** Parses a set of layout alignment strings and stores the layout alignments. Can account for null and undefined alignment strings.
	 * @param aligns Array holding the strings representing layout alignments at each level in the treemap hierarchy.
	 */
	private void storeLayoutAlignments(String[] aligns)
	{
		if (aligns == null)
		{
			alignments = new Layout[TreeMapApp.MAX_DEPTH];
			for (int i=0; i<alignments.length; i++)
			{
				alignments[i] = Layout.FREE;
			}
			return;
		}

		alignments = new Layout[aligns.length];
		for (int i=0; i<aligns.length; i++)
		{
			if (aligns[i].equalsIgnoreCase("horizontal"))
			{
				alignments[i] = Layout.HORIZONTAL;
			}
			else if (aligns[i].equalsIgnoreCase("vertical"))
			{
				alignments[i] = Layout.VERTICAL;
			}
			else 
			{
				alignments[i] = Layout.FREE;
			}
		}
	}

	/** Recursively accumulates the aspect ratios and distance correlations
	 * of all descendants of the given node in the tree.
	 * @param node Node whose children will be used to accumulate statistics. 
	 */
	private void calcStatistics(TreeMapNode node)
	{
		Rectangle2D rect=  node.getRectangle();

		// Ignore nodes too small to display.
		if ((rect == null) || (rect.getWidth() < 1) || (rect.getHeight() < 1))
		{
			return;
		}

		boolean isDummy = node.getSizeValue() < 0;

		if (!isDummy)
		{
			aspectRatio += Math.max(rect.getWidth()/rect.getHeight(), rect.getHeight()/rect.getWidth());
			numNodes++;
		}

		if (node.getChildCount() > 0)
		{
			List<TreeMapNode> children = node.getChildren();

			//double originX = node.getRectangle().getX();
			//double originY = node.getRectangle().getY();

			double originX = root.getRectangle().getX();
			double originY = root.getRectangle().getY();

			int nodeLevel = node.getLevel();

			for (TreeMapNode child : children)
			{
				// Establish size-distance relationship for layouts that can order by size
				if ((child.isLeaf()) && (layoutTypes[nodeLevel] != Layout.SPATIAL) && (layoutTypes[nodeLevel] != Layout.SPATIAL_AV) && (layoutTypes[nodeLevel] != Layout.PIVOT_SPACE))
				{
					if ((child.getSizeValue() > 0) && (child.getRectangle() != null))	// Non-dummy children only.
					{
						double childX = child.getRectangle().getX();
						double childY = child.getRectangle().getY();
						double distance = Math.sqrt((childX-originX)*(childX-originX) + (childY-originY)*(childY-originY));
						leafDistances.add(new OrderDistance(child.getOrder(),distance));
					}
				}
				// Recurse looking for leaves.
				calcStatistics(child);
			}

			// Look for enough leaves to calculate readability measure.
			double oldDirection = 0;
			TreeMapNode oldChild;    		
			int nextLeafNum = -1;
			TreeMapNode child1 = null, child2 = null;

			for (int i=0; i<children.size()-1; i++)
			{
				TreeMapNode child = children.get(i);
				if (child.isLeaf() && (child.getSizeValue() >0))
				{
					if (child1 == null)
					{
						child1 = child;
					}
					else if (child2 == null)
					{
						child2 = child;
						nextLeafNum = i+1;
					}
				}
				if (nextLeafNum != -1)
				{
					break;
				}
			}

			if (nextLeafNum == -1)
			{
				return;
			}

			// Find initial vector.
			if ((child1 == null) || (child2 == null))
			{
				return;
			}
			
			Rectangle2D r1 = child1.getRectangle();
			Rectangle2D r2 = child2.getRectangle();
			if ((r1 == null) || (r2 == null))
			{
				return;
			}
			double c1x = r1.getCenterX();
			double c1y= r1.getCenterY();
			double c2x = r2.getCenterX();
			double c2y= r2.getCenterY();

			oldDirection = Math.atan2(c2y-c1y, c2x-c1x);
			oldChild = child2;

			boolean foundNewLeaf = false;

			for (int i=nextLeafNum; i<children.size(); i++)
			{
				TreeMapNode child = children.get(i);
				if (!(child.isLeaf()) || (child.getSizeValue() < 0) || (child.getRectangle()==null))
				{
					continue;
				}

				c1x = oldChild.getRectangle().getCenterX();
				c1y= oldChild.getRectangle().getCenterY();
				c2x = child.getRectangle().getCenterX();
				c2y= child.getRectangle().getCenterY();
				numAdjacentLeaves++;
				foundNewLeaf = true;

				double direction = Math.atan2(c2y-c1y, c2x-c1x);
				if (Math.abs(direction-oldDirection) > 0.1)
				{
					readability++;
				}
				oldDirection = direction;
				oldChild = child;
			}

			if (foundNewLeaf)
			{
				numAdjacentLeaves+=2;		// Account for first two nodes used to find initial vector.
			}
		}
	}

	/** Recursively calculates the displacement statistics of the given spatial node.
	 * @param node Node whose children will be used to accumulate statistics. 
	 */
	private void calcDisplacementStatistics(TreeMapNode node)
	{
		Rectangle2D rect=  node.getRectangle();

		// Ignore leaves and nodes too small to display.
		if (node.isLeaf() || (rect == null))
		{
			return;
		}

		if (node.getChildCount() > 0)
		{
			List<TreeMapNode> children = node.getChildren();

			// Find geographic range of children.
			double minEasting = Float.MAX_VALUE,minNorthing = Float.MAX_VALUE;
			double maxEasting = -Float.MAX_VALUE,maxNorthing = -Float.MAX_VALUE;
			double eastingRange,northingRange;

			for (TreeMapNode child : children)
			{
				minEasting = Math.min(minEasting,child.getLocation().getX());
				minNorthing = Math.min(minNorthing,child.getLocation().getY());
				maxEasting = Math.max(maxEasting,child.getLocation().getX());
				maxNorthing = Math.max(maxNorthing,child.getLocation().getY());
			}
			eastingRange = maxEasting-minEasting;
			northingRange = maxNorthing-minNorthing;

			for (TreeMapNode child : children)
			{
				if ((child.getRectangle() != null) && (eastingRange > 0) && (northingRange > 0))
				{
					Point2D geoCentre = new Point2D.Double(node.getRectangle().getX() + node.getRectangle().getWidth()*(child.getLocation().getX()-minEasting)/eastingRange,
							node.getRectangle().getY()+node.getRectangle().getHeight() - (node.getRectangle().getHeight()*(child.getLocation().getY()-minNorthing)/northingRange));  				

					// TODO: Calculate angular displacement to all other siblings for the moment, but this is an n^2 search, so should
					// be capped at nearest m neighbours.
					int numSiblings = 0;
					double angTotal = 0;
					for (TreeMapNode sibling: children)
					{
						if ((sibling != child) && (sibling.getRectangle() != null))
						{
							Point2D siblingGeoCentre = new Point2D.Double(node.getRectangle().getX() + node.getRectangle().getWidth()*(sibling.getLocation().getX()-minEasting)/eastingRange,
									node.getRectangle().getY()+node.getRectangle().getHeight() - (node.getRectangle().getHeight()*(sibling.getLocation().getY()-minNorthing)/northingRange));


							// Create two normalised vectors and their dot product to find angular deviation between them.
							double geoVectX = siblingGeoCentre.getX()-geoCentre.getX();
							double geoVectY = siblingGeoCentre.getY()-geoCentre.getY();
							double geoLength = Math.sqrt(geoVectX*geoVectX + geoVectY*geoVectY);

							if (geoLength > 0)
							{
								geoVectX /= geoLength;
								geoVectY /= geoLength;

								double tmVectX = sibling.getRectangle().getCenterX()-child.getRectangle().getCenterX();
								double tmVectY = sibling.getRectangle().getCenterY()-child.getRectangle().getCenterY();
								double tmLength = Math.sqrt(tmVectX*tmVectX + tmVectY*tmVectY);
								tmVectX /= tmLength;
								tmVectY /= tmLength;

								double arg = geoVectX*tmVectX + geoVectY*tmVectY;
								// Account for rounding errors.
								if (arg > 1)
								{
									arg = 1;
								}
								else if (arg <-1)
								{
									arg = -1;
								}
								double angle = Math.acos(arg);
								angTotal += angle;
								numSiblings++;			
							}
						}

					}
					angTotal /= numSiblings;
					angDisplacement += angTotal;
					double dist = Math.sqrt((child.getRectangle().getCenterX()-geoCentre.getX())*(child.getRectangle().getCenterX()-geoCentre.getX()) +
							(child.getRectangle().getCenterY()-geoCentre.getY())*(child.getRectangle().getCenterY()-geoCentre.getY()));
					distDisplacement += dist;    				
					numSpatialNodes++;
				}
				calcDisplacementStatistics(child);
			}
		}

	}

	/** Writes out the coordinates of the treemap as a text file for external processing.
	 * @param out File to write to. 
	 */
	private void writeTreeMapAsText(BufferedWriter out) throws IOException
	{
		//DecimalFormat valueFormatter = new DecimalFormat("#0.##########");
		DecimalFormat cf = new DecimalFormat("#0.##");

		// Breadth-first output ignoring root node.    	
		LinkedList<TreeMapNode> queue = new LinkedList<TreeMapNode>();
		List<TreeMapNode> children = ((TreeMapNode)tree.getRoot()).getChildren();

		for (TreeMapNode child : children)
		{
			queue.add(child);
		} 

		while (!queue.isEmpty())
		{
			// Write out the node at the head of the queue.
			TreeMapNode node = queue.removeFirst();
			Rectangle2D rect = node.getRectangle();
			if (rect != null)
			{
				out.write("\""+node.getLabel()+"\",");
				out.write(cf.format(rect.getX())+","+cf.format(rect.getY())+","+cf.format(rect.getX()+rect.getWidth())+","+cf.format(rect.getY()+rect.getHeight()));

				out.newLine();

				for (TreeMapNode child : node.getChildren())
				{
					queue.add(child);
				} 
			}
			else
			{
				System.err.println("  Node "+node.getLabel()+" not written to file since it is too small to display.");
			}
		}    	
	}

	private void computeAreas(TreeMapNode parent)
	{
		for (TreeMapNode child : parent.getChildren())
		{
			child.setArea(parent.getArea()*child.getAccumSize()/parent.getAccumSize());
			//System.err.println("CA/CN for "+child.getLabel()+" is "+child.getAccum()+" / "+child.getNumLeaves()+" = "+(child.getAccum()/child.getNumLeaves()));
			//System.err.println("Area of "+child.getLabel()+" is "+child.getArea());
			computeAreas(child);
		}
	}
	
	/** Creates a tree from the given TreeML file.
	 *  @param fileName Name of file containing the treeML data.
	 *  @return True if file read without problems.
	 */
	private boolean readTreeML(String fileName, boolean useLabels)
	{
		return readTreeML(new DOMProcessor(fileName), new File(fileName).getAbsolutePath(),useLabels);
	}
	
	/** Creates a tree from the given Document Object Model.
	 *  @param treeDOM DOM representing tree hierarchy.
	 *  @param fullFileName Full path of file containing the treeML data (used for error reporting).
	 *  @return True if file read without problems.
	 */
	private boolean readTreeML(DOMProcessor treeDOM, String fullFileName, boolean useLabels)
	{
		this.dom = treeDOM;
		
		if (dom.isEmpty())
		{
			System.err.println("No XML content found in TreeML file "+fullFileName);
			return false;
		}

		Node[] trees = dom.getElements("tree");
		if (trees.length < 1)
		{
			System.err.println("No <tree> node found in TreeML file "+fullFileName);
			return false;
		}        
		Node[] children = dom.getNodeElements("branch", trees[0]);

		if (children.length < 1)
		{
			System.err.println("No <branch> nodes found in TreeML file "+fullFileName);
			return false;
		}

		if (children.length > 1)
		{
			System.err.println("More than one root node found in TreeML file ("+children.length+")");
			return false;
		}
		
		// We need to store the layout types before building tree as layout determines if nodes are sorted.
		storeLayoutTypes(props.getLayouts());

		buildTreeFromDOM(children,null);
		return true; 
	}

	
	/** Creates a tree from the given CSV file. The file can be 'csvCompact' in which case, node order is determined
	 * by the 'size' attribute and then colour attribute if size values are equal; 'csv' in which case a separate
	 * column indicates node order; or 'csvSpatial' where spatial locations are given to all levels of the hierarchy
	 * <br /><br /> 
	 * For 'csvCompact', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", size, colour, x, y, node, [nodeChild, nodeGrandchild, nodeGreatGrandchild...] </code><br />
	 * For 'csv', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", order, size, colour, x, y, node, [nodeChild, nodeGrandchild, nodeGreatGrandchild...] </code><br />
	 * <br /><br />
	 *  For 'csvSpatial', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", order, size, colour, leafX, leafY, node, nodex, nodey [nodeChild, nodeChildX,nodeChildY, nodeGrandchild, nodeGrandchildX, nodeGrandchildY, nodeGreatGrandchild...] </code><br />
	 * <br /><br />
	 * where <code>size</code> is a numeric value that will be used to map the size of each leaf, <code>order</code> is a 
	 * numeric value that is used to determine node order (sorted from lowest to highest), <code>colour</code> is a numeric
	 * value that relates to a colour lookup or a raw 24 bit integer colour, <code>x,y</code> and variants represent the location of
	 * the leaf (CSV, CSVCompact) or branch (CSVSpatial), and the list of nodes represents the names of the leaf's parents. 
	 * Must include at least one node (root) in the list.
	 * @param inFileName Name of file containing the CSV data.
	 * @param useLabels Node labels used to define hierarchy if true. Otherwise tree structure only defined by level0, level1, level2 etc.
	 * @param flavour Type of CSV format. Can be one of <code>CSV</code>, <code>CSV_COMPACT</code> or <code>CSV_SPATIAL</code>.
	 * @return True if file read without problems.
	 */
	private boolean readCSV(String inFileName, boolean useLabels, int flavour)
	{
		File inFile = new File(inFileName);
		
		try
		{
			if (inFile.canRead() == false)
			{
				System.err.println("Cannot find file "+inFile.getCanonicalPath());
				return false;
			}
			return readCSV(new BufferedReader(new FileReader(inFileName)),useLabels,flavour);
		}
		catch (IOException e)
		{
			System.err.println("Problem reading CSV file: "+e);
			return false;
		}
	}

	/** Creates a tree from the given CSV file. This version requires a buffered reader pointing to the CSV file and so
	 *  is compatible with Processing if the reader was created with <code>createReader()</code>. The file can be 
	 *  'csvCompact' in which case, node order is determined by the 'size' attribute and then colour attribute if size 
	 *  values are equal; 'csv' in which case a separate column indicates node order; or 'csvSpatial' where spatial 
	 *  locations are given to all levels of the hierarchy
	 * <br /><br /> 
	 * For 'csvCompact', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", size, colour, x, y, node, [nodeChild, nodeGrandchild, nodeGreatGrandchild...] </code><br />
	 * For 'csv', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", order, size, colour, x, y, node, [nodeChild, nodeGrandchild, nodeGreatGrandchild...] </code><br />
	 * <br /><br />
	 *  For 'csvSpatial', each row of the CSV file should be in the following format:<br />
	 * <code>"Leaf name", order, size, colour, leafX, leafY, node, nodex, nodey [nodeChild, nodeChildX,nodeChildY, nodeGrandchild, nodeGrandchildX, nodeGrandchildY, nodeGreatGrandchild...] </code><br />
	 * <br /><br />
	 * where <code>size</code> is a numeric value that will be used to map the size of each leaf, <code>order</code> is a 
	 * numeric value that is used to determine node order (sorted from lowest to highest), <code>colour</code> is a numeric
	 * value that relates to a colour lookup or a raw 24 bit integer colour, <code>x,y</code> and variants represent the location of
	 * the leaf (CSV, CSVCompact) or branch (CSVSpatial), and the list of nodes represents the names of the leaf's parents. 
	 * Must include at least one node (root) in the list.
	 * @param bReader Buffered reader pointing to the file containing the CSV data.
	 * @param useLabels Node labels used to define hierarchy if true. Otherwise tree structure only defined by level0, level1, level2 etc.
	 * @param flavour Type of CSV format. Can be one of <code>CSV</code>, <code>CSV_COMPACT</code> or <code>CSV_SPATIAL</code>.
	 * @return True if file read without problems.
	 */
	private boolean readCSV(BufferedReader bReader, boolean useLabels, int flavour)
	{
		try
		{
			String[] tokens;
			root = new TreeMapNode("root",0,null,null,null);
			tree = new DefaultTreeModel(root);

			int minTokens = 6, sizeTokenPosition = 1, colourTokenPosition=2,locationTokenPosition=3;
			int firstBranchIndex = minTokens-1;
			int itemsPerBranch = 1;

			if (flavour != CSV_COMPACT)
			{
				// Extra 'order' column in column 2.
				minTokens++;
				sizeTokenPosition++;
				colourTokenPosition++;
				locationTokenPosition++;
				firstBranchIndex = minTokens-1;
			}
			if (flavour == CSV_SPATIAL)
			{
				minTokens+=2;	// At least one extra pair of x,y coordinates.
				firstBranchIndex = minTokens-3;
				itemsPerBranch=3;
			}
			//int ln = 0;	        	        
			while (bReader.ready())
			{
				//if (++ln%500 == 0) System.err.println(ln);

				String inputLine = bReader.readLine();
				int leafIndex = 0;
				int lastBranchIndex;

				// Ignore blank lines or those starting with a #.
				if ((inputLine.trim().startsWith("#")) || (inputLine.trim().length()==0))
				{
					continue;
				}

				// Separate out the comma separated tokens.
				// TODO: Include commas within paired quotation marks.
				tokens = inputLine.split(",");

				if (tokens.length < minTokens)
				{
					System.err.println("Warning: Line contains fewer than the minimum "+minTokens+" values: "+inputLine);
					continue;
				}

				if (useLabels == false)
				{
					// If label is not defining leaf, then last item(s) in line defines the leaf node.
					if (flavour == CSV_SPATIAL)
					{
						leafIndex = tokens.length-3;
						lastBranchIndex = tokens.length-6;
					}
					else
					{
						leafIndex = tokens.length-1;
						lastBranchIndex = tokens.length-2;
					}
				}
				else
				{
					if (flavour == CSV_SPATIAL)
					{
						lastBranchIndex = tokens.length-3;
					}
					else
					{
						lastBranchIndex = tokens.length-1;
					}
				}

				// Look to see if we have an existing node with the same name.
				TreeMapNode parent = root;		    	    	
				for (int n=firstBranchIndex; n<=lastBranchIndex; n+=itemsPerBranch)
				{
					TreeMapNode matchedNode = null;
					for (TreeMapNode child : parent.getChildren())
					{
						if (child.getLabel().equalsIgnoreCase(tokens[n]))
						{
							matchedNode = child;
							break;
						}
					}

					if (matchedNode == null)
					{
						// Must be a new node to attach to parent.
						double orderValue = 0;

						if ((flavour != CSV_COMPACT) && (tokens[1].trim().length() >0))
						{
							orderValue = Double.parseDouble(tokens[1]);
							//System.err.println("1. Order is "+orderValue);
						}
						Point2D location = null;

						if ((flavour == CSV_SPATIAL) &&	(tokens[n+1].trim().length() >0) && (tokens[n+2].trim().length() >0))
						{
							try
							{
								location = new Point2D.Double(Double.parseDouble(tokens[n+1]), Double.parseDouble(tokens[n+2]));
								if (location.getX() < west)
								{
									west = location.getX();
								}
								if (location.getX() > east)
								{
									east = location.getX();
								}
								if (location.getY() < south)
								{
									south = location.getY();
								}
								if (location.getY() > north)
								{
									north = location.getY();
								}
							}
							catch (NumberFormatException e)
							{
								System.err.println("Cannot extract location coordinates from "+inputLine);
								location = null;
							}
						}	    

						TreeMapNode newNode = new TreeMapNode(tokens[n],orderValue,null,null,location);
						parent.add(newNode);		    			
						if (newNode.getLevel() > maxDepth)
						{
							maxDepth = newNode.getLevel();
						}
						parent = newNode;
					}
					else
					{
						parent = matchedNode;
					}
				}

				// Attach leaf to last matched or created node.
				Float sizeValue = null;
				double orderValue = 0;

				if ((flavour != CSV_COMPACT) && (tokens[1].trim().length() >0))
				{
					orderValue = Double.parseDouble(tokens[1]);
					//System.err.println("2. Order is "+orderValue);
				}

				if (tokens[sizeTokenPosition].trim().length() >0)
				{
					sizeValue = new Float(tokens[sizeTokenPosition]);
				}

				Float colourValue = null;
				if (tokens[colourTokenPosition].trim().length() >0)
				{
					colourValue = new Float(tokens[colourTokenPosition]);
				}		    	
				Point2D location = null;
				if ((tokens[locationTokenPosition].trim().length() >0) && (tokens[locationTokenPosition+1].trim().length() >0))
				{
					try
					{
						location = new Point2D.Double(Double.parseDouble(tokens[locationTokenPosition]), Double.parseDouble(tokens[locationTokenPosition+1]));
						if (location.getX() < west)
						{
							west = location.getX();
						}
						if (location.getX() > east)
						{
							east = location.getX();
						}
						if (location.getY() < south)
						{
							south = location.getY();
						}
						if (location.getY() > north)
						{
							north = location.getY();
						}
					}
					catch (NumberFormatException e)
					{
						System.err.println("Cannot extract location coordinates from "+inputLine);
						location = null;
					}
				}	    	
				TreeMapNode node = new TreeMapNode(tokens[leafIndex],orderValue,sizeValue,colourValue,location);
				node.setLabel(tokens[0]);
				parent.add(node);
			}
			bReader.close();	    	
		}
		catch (IOException e)
		{
			System.err.println("Problem reading CSV file: "+e);
			return false;
		}	
		return true;
	}

	/** Computes the treemap layout. Lays out all the children of the given parent node
	 * then recursively calls itself to lay out all descendants. 
	 * @param parent Parent node whose children will be laid out. 
	 * @param rectangle Rectangle into which nodes must be laid out.
	 */
	private void layout(TreeMapNode parent, Rectangle2D rectangle)
	{
		int level = parent.getLevel();
		Rectangle2D rect = rectangle;

		for (TreeMapNode child : parent.getChildren())
		{
			nodesToLayout.add(child);
		}

		//System.err.println("About to lay out "+nodesToLayout.size()+" nodes at level "+nodesToLayout.get(0).getLevel()+" First is "+nodesToLayout.get(0).getLabel());   	
		// Lay out siblings
		switch (layoutTypes[level])
		{
		case SLICE_AND_DICE:
			sliceAndDice(nodesToLayout, rect,alignments[level]);
			break;
		case SQUARIFIED:
			squarify(nodesToLayout, rect,alignments[level]);
			break;
		case MORTON:
			mortonise((MortonList<TreeMapNode>)nodesToLayout, rect);
			break;
		case SPATIAL:	
			orderedSquarify(nodesToLayout, rect,Layout.SPATIAL,alignments[level]);
			break;
		case SPATIAL_AV:	
			orderedSquarify(nodesToLayout, rect,Layout.SPATIAL_AV,alignments[level]);
			break;
		case STRIP:
			stripMap(nodesToLayout, rect,alignments[level]);
			break;
		case PIVOT_MIDDLE:
		case PIVOT_SIZE:
		case PIVOT_SPLIT_SIZE:
		case PIVOT_SPACE:
			pivot(nodesToLayout, rect);
			break;
		default:
			orderedSquarify(nodesToLayout, rect,Layout.ORDERED_SQUARIFIED,alignments[level]);
		}

		nodesToLayout.clear();

		// Recursively process descendants.
		for (TreeMapNode child : parent.getChildren())
		{          
			if (child.getChildCount() > 0 && getArea(child) > 0 ) 
			{
				updateArea(child,rect);
				Rectangle2D childRect = child.getRectangle();

				rect = new Rectangle2D.Double(childRect.getX(),childRect.getY(),childRect.getWidth(),childRect.getHeight());
				layout(child, rect);
			}
		}
	}

	/** Calculates modified areas due to reduction in size to accommodate border spacing.
	 * @param node Node whose area is to be updated.
	 * @param rect Rectangle defining bounds of node.
	 */
	private void updateArea(TreeMapNode node, Rectangle2D rect) 
	{
		Rectangle2D b = node.getRectangle();
		double nodeBorder = 0;

		if (node != root)
		{
			// Find parent's border width.
			nodeBorder = borderWidths[node.getLevel()-1];
		}


		// Reduce area to accommodate border. If area smaller than border area, reduce border size.
		double s = 0;

		for (TreeMapNode child : node.getChildren())
		{
			s += getArea(child);
		}     
		double dA = 2*nodeBorder*(b.getWidth()+b.getHeight()-2*nodeBorder);
		if (nodeBorder != 0)
		{
			//while (dA <=0)
				while ((nodeBorder > 0) && ((b.getWidth() <= 2*nodeBorder) || (b.getHeight() <= 2*nodeBorder)))
				{
					nodeBorder = Math.max(0,nodeBorder-1);
					dA = 2*nodeBorder*(b.getWidth()+b.getHeight()-2*nodeBorder);
				}
		}

		double A = getArea(node) - dA;

		// Compute renormalisation factor.
		double t = A/s;

		// Renormalise child areas.
		for (TreeMapNode child : node.getChildren())
		{
			child.setArea(child.getArea()*t);
		}

		// Set bounding rectangle.
		rect.setRect(b.getX()+nodeBorder, b.getY()+nodeBorder, 
				b.getWidth()-2*nodeBorder, b.getHeight()-2*nodeBorder);
		node.setRectangle(new Rectangle2D.Double(b.getX()+nodeBorder, b.getY()+nodeBorder, 
				b.getWidth()-2*nodeBorder, b.getHeight()-2*nodeBorder));
		return;
	}

	/** Lays out the given nodes in the given rectangle attempting to position the nodes
	 * either according to their true location or ordered by proximity to the top-left
	 * corner of the rectangle. In either case, nodes are kept as square as possible.
	 * If the given rectangle is too small to fit any nodes within it, no nodes are laid out.
	 * @param nodes Nodes to lay out.
	 * @param rectangle Rectangle in which to lay out nodes.
	 * @param layoutType Either ORDERED_SQUARIFY or SPATIAL.
	 * @param alignment Alignment constraint (HORIZONTAL, VERTICAL or FREE if no constraint).
	 */
	private void orderedSquarify(List<TreeMapNode>nodes, Rectangle2D rectangle, Layout layoutType, Layout alignment) 
	{
		Rectangle2D rect = rectangle;
		
		// Check rectangle is large enough to fit nodes within it.
		if ((rect.getWidth() <= 0) ||  (rect.getHeight() <= 0))
		{
			return;
		}

		LocationList locatedNodes = new LocationList();
		LocationList distances = new LocationList();
		double stripAR;

		// TODO: Stagger y as well as x locations.	

		boolean isHorizontal;
		double layoutSide;

		if ((alignment == Layout.HORIZONTAL) || ((alignment==Layout.FREE) && (rect.getWidth() <= rect.getHeight())))
		{
			isHorizontal = true;
			layoutSide = rect.getWidth();
			stripAR = targetAR;
		}
		else
		{
			isHorizontal = false;
			layoutSide = rect.getHeight();
			stripAR = 1/targetAR;
		}

		double xInc = Math.sqrt(targetAR*rect.getWidth()*rect.getHeight()/nodes.size());
		double yInc = Math.sqrt(rect.getWidth()*rect.getHeight()/(nodes.size()*targetAR));

		double globalXMin = rect.getX() + 0.5*xInc;
		double globalYMin = rect.getY() + 0.5*yInc; 
		double totalNodeArea = 0;
		int n=0;
		double x,y;

		// Set locations ordered from the top left.
		// This is necessary even if we will be doing a spatial arrangement, since nodes that share
		// a common spatial location should be ordered by distance from origin.
		for (int i=0; i<nodes.size(); i++)
		{   
			totalNodeArea += nodes.get(i).getArea();

			if (isHorizontal)
			{
				x = rect.getX() + ((i+0.5)*xInc)%rect.getWidth();
				y = globalYMin + (int)(((i+0.5)*xInc)/rect.getWidth())*yInc;
			}
			else
			{
				x = globalXMin + (int)((i+0.5)*yInc/rect.getHeight())*xInc;
				y = rect.getY() + ((i+0.5)*yInc)%rect.getHeight();
			}  
			Double dist = new Double(Math.sqrt((x-globalXMin)*(x-globalXMin)+(y-globalYMin)*(y-globalYMin)));
			distances.add(new LocatedObject(x,y,dist));		
		}  	
		distances.sortByDistance(new Point2D.Double(globalXMin,globalYMin));

		for (int i=0; i<nodes.size(); i++)
		{
			Point2D location = distances.get(i).getLocation();
			locatedNodes.add(new LocatedObject(location.getX(),location.getY(),nodes.get(i)));
		}
		distances.clear();  

		if ((layoutType == Layout.SPATIAL) || (layoutType == Layout.SPATIAL_AV))
		{
			double xMin = Float.MAX_VALUE;
			double xMax = -Float.MAX_VALUE;
			double yMin = Float.MAX_VALUE;
			double yMax = -Float.MAX_VALUE;

			// Use location to determine layout by scaling locations to fit inside parent rectangle.
			for (int i=0; i<locatedNodes.size(); i++)
			{    			
				Point2D location = ((TreeMapNode)(locatedNodes.get(i).getObject())).getLocation();
				if (location.getX() < xMin)
				{
					xMin = location.getX();
				}
				if (location.getX() > xMax)
				{
					xMax = location.getX();
				}
				if (location.getY() < yMin)
				{
					yMin = location.getY();
				}
				if (location.getY() > yMax)
				{
					yMax = location.getY();
				}
			}
			if ((float)xMax != (float)xMin  || (float)yMax != (float)yMin)		    		
			{
				for (int i=0; i<locatedNodes.size(); i++)
				{
					Point2D location = ((TreeMapNode)(locatedNodes.get(i).getObject())).getLocation();	

					double dx = 0.5;
					double dy = 0.5;

					// Range converted to float to stop rounding errors from implying there
					// is spatial variation when there is none.
					if ((float)xMax > (float)xMin)
					{
						dx = (location.getX()-xMin)/(xMax-xMin);
					}
					if ((float)yMax > (float)yMin)
					{
						dy = (location.getY()-yMin)/(yMax-yMin);
					}			
					locatedNodes.get(i).getLocation().setLocation(rect.getX() + 0.5*xInc + (rect.getWidth()-xInc)*dx,
							rect.getY()+ rect.getHeight() - (0.5*yInc+(rect.getHeight()-yInc)*dy));        
				}
			}
		}

		List<TreeMapNode> row = new Vector<TreeMapNode>();
		double oldAspectRatio = Double.MAX_VALUE, newAspectRatio;
		double xOffset = 0,yOffset=0;
		double xPos = rect.getX()+ 0.5*xInc,
		yPos = rect.getY() + 0.5*yInc;
		while (locatedNodes.size() > 0)
		{      
			if (isHorizontal)
			{        		
				xPos = rect.getX() + (n+0.5)*xInc + xOffset;
				yPos = rect.getY() + 0.5*yInc + yOffset;
			}
			else
			{        		
				xPos = rect.getX() + 0.5*xInc + xOffset;
				yPos = rect.getY() + (n+0.5)*yInc + yOffset;
			}     

			LocatedObject locatedNode = locatedNodes.getClosest(new Point2D.Double(xPos,yPos));

			// Make sure idealised grid points don't jump ahead of current row/column layout.
			if (isHorizontal)
			{
				xOffset = Math.min(0,locatedNode.getLocation().getX()-xPos);
			}
			else
			{
				yOffset = Math.min(0,locatedNode.getLocation().getY()-yPos);
			}
			TreeMapNode node = (TreeMapNode)locatedNode.getObject(); 
			double a = getArea(node);

			if (a <= 0) 
			{
				locatedNodes.remove(locatedNode);
				continue;
			}

			row.add(node);

			if (layoutType == Layout.SPATIAL_AV)
			{
				// TODO: Make sure spatial av also incorporates target AR.
				newAspectRatio = getAvAspectRatio(row, layoutSide);
			}
			else
			{
				newAspectRatio = getWorstAspectRatio(row, layoutSide,stripAR);
			}

			if (newAspectRatio <= oldAspectRatio) 
			{
				locatedNodes.remove(locatedNode);	
				oldAspectRatio = newAspectRatio;  
				n++;
			}             
			else 
			{
				row.remove(row.size()-1); 				// Remove the latest addition since it worsens the aspect ratio.
				rect = layoutLine(row, layoutSide, rect,alignment); // Lay out the current row.

				// Recompute smallest side and direction of remaining rectangle space.
				if ((alignment == Layout.HORIZONTAL) || ((alignment==Layout.FREE) && (rect.getWidth() <= rect.getHeight())))
				{
					isHorizontal = true;
					layoutSide = rect.getWidth();
					stripAR = targetAR;
				}
				else
				{
					isHorizontal = false;
					layoutSide = rect.getHeight();
					stripAR = 1/targetAR;
				}

				row.clear(); // clear the row
				xOffset = 0;
				yOffset = 0;             
				oldAspectRatio = Double.MAX_VALUE;  

				// Distribute the remaining nodes evenly within the remaining space.
				xInc = Math.sqrt(targetAR*rect.getWidth()*rect.getHeight()/locatedNodes.size());
				yInc = Math.sqrt(rect.getWidth()*rect.getHeight()/(locatedNodes.size()*targetAR));
				n=0;

				double localXMin = rect.getX() + 0.5*xInc;
				double localYMin = rect.getY() + 0.5*yInc;  
				            
				for (int i=0; i<locatedNodes.size(); i++)
				{
					if (isHorizontal)
					{
						x = rect.getX() + ((i+0.5)*xInc)%rect.getWidth();
						y = localYMin + (int)(((i+0.5)*xInc)/rect.getWidth())*yInc;
					}
					else
					{
						x = localXMin + (int)((i+0.5)*yInc/rect.getHeight())*xInc;
						y = rect.getY() + ((i+0.5)*yInc)%rect.getHeight();
					}
					
					Double dist = new Double(Math.sqrt((x-globalXMin)*(x-globalXMin)+(y-globalYMin)*(y-globalYMin)));
					distances.add(new LocatedObject(x,y,dist));
				}

				// Re-calculate distances and rank order based on the remaining rectangle and number of nodes.
				distances.sortByDistance(new Point2D.Double(globalXMin,globalYMin));

				for (int i=0; i<locatedNodes.size(); i++)
				{
					locatedNodes.get(i).getLocation().setLocation(distances.get(i).getLocation());	
				} 
				distances.clear();

				if ((layoutType == Layout.SPATIAL) || (layoutType == Layout.SPATIAL_AV))
				{
					double xMin = Float.MAX_VALUE;
					double xMax = -Float.MAX_VALUE;
					double yMin = Float.MAX_VALUE;
					double yMax = -Float.MAX_VALUE;

					// Use location to determine layout by scaling remaining locations to fit inside remaining rectangle.
					for (int i=0; i<locatedNodes.size(); i++)
					{
						Point2D location = ((TreeMapNode)(locatedNodes.get(i).getObject())).getLocation();
						if (location.getX() < xMin)
						{
							xMin = location.getX();
						}
						if (location.getX() > xMax)
						{
							xMax = location.getX();
						}
						if (location.getY() < yMin)
						{
							yMin = location.getY();
						}
						if (location.getY() > yMax)
						{
							yMax = location.getY();
						}
					}
					if ((float)xMax != (float)xMin  || (float)yMax != (float)yMin)
					{	
						for (int i=0; i<locatedNodes.size(); i++)
						{
							Point2D location = ((TreeMapNode)(locatedNodes.get(i).getObject())).getLocation();

							double dx = 0.5;
							double dy = 0.5;

							// Range converted to float to stop rounding errors from implying there
							// is spatial variation when there is none.
							if ((float)xMax > (float)xMin)
							{
								dx = (location.getX()-xMin)/(xMax-xMin);
							}
							if ((float)yMax > (float)yMin)
							{
								dy = (location.getY()-yMin)/(yMax-yMin);
							}

							locatedNodes.get(i).getLocation().setLocation(rect.getX() + 0.5*xInc + (rect.getWidth()-xInc)*dx,
									rect.getY()+ rect.getHeight() - (0.5*yInc+(rect.getHeight()-yInc)*dy));                   
						}
					}
				}
			}
		}
		if (row.size() > 0) 
		{
			rect = layoutLine(row, layoutSide, rect,alignment); // Lay out the current row.
			row.clear(); 										// Clear the row.         
		}
	}

	/** Lays out the given nodes in the given rectangle attempting to make the aspect 
	 * ratios of the nodes as square as possible. Nodes are ordered from left to right
	 * and from top to bottom, usually in alternating sequence. This version is 
	 * computationally efficient, produces good aspect ratios, but can lead to 
	 * non-intuitive ordering of nodes.
	 * @param nodes Nodes to lay out.
	 * @param rectangle Rectangle in which to lay out nodes.
	 * @param alignment Alignment constraint (HORIZONTAL, VERTICAL or FREE if no constraint).
	 */
	private void squarify(List<TreeMapNode>nodes, Rectangle2D rectangle, Layout alignment) 
	{
		double stripAR;
		List<TreeMapNode> row = new Vector<TreeMapNode>();
		double worst = Double.MAX_VALUE, nworst;
		double layoutSide;
		Rectangle2D rect = rectangle;

		if ((alignment == Layout.HORIZONTAL) || ((alignment==Layout.FREE) && (rect.getWidth() <= rect.getHeight())))
		{
			layoutSide = rect.getWidth();
			stripAR = targetAR;
		}
		else 
		{
			layoutSide = rect.getHeight();
			stripAR = 1/targetAR;
		}

		while (nodes.size() > 0)
		{        	 
			TreeMapNode item = nodes.get(0); 
			double a = getArea(item);

			if (a <= 0) 
			{
				nodes.remove(0);
				continue;
			}

			row.add(item);         
			nworst = getWorstAspectRatio(row, layoutSide,stripAR);

			if (nworst <= worst) 
			{
				nodes.remove(0);	
				worst = nworst;              
			} 
			else 
			{
				row.remove(row.size()-1); 							// Remove the latest addition since it worsens the aspect ratio.
				rect = layoutLine(row, layoutSide, rect,alignment); // Lay out the current row.

				// Recompute smallest side and direction of remaining rectangle space.
				if ((alignment == Layout.HORIZONTAL) || ((alignment==Layout.FREE) && (rect.getWidth() <= rect.getHeight())))
				{
					layoutSide = rect.getWidth();
					stripAR = targetAR;
				}
				else 
				{
					layoutSide = rect.getHeight();
					stripAR = 1/targetAR;
				}

				row.clear(); // clear the row
				worst = Double.MAX_VALUE;  
			}
		}

		if (row.size() > 0) 
		{
			rect = layoutLine(row, layoutSide, rect,alignment); // Lay out the current row.
			row.clear(); 										// Clear the row.
		}
	}

	/** Lays out the given nodes in the given rectangle attempting to make the aspect ratios
	 * of the nodes as square as possible but arranging the nodes in strips. Uses the strip
	 * algorithm presented by Bederson, Schneiderman and Wattenberg, 2002. Preserves 1d order
	 * (and therefore stability) while giving a better average aspect ratio than slice and dice.
	 * Note that if alignment is set to FREE, a horizontal strip map will be produced.
	 * @param nodes Nodes to lay out.
	 * @param rect Rectangle in which to lay out nodes.
	 * @param alignment Alignment constraint (HORIZONTAL, VERTICAL or FREE if no constraint).
	 */
	private void stripMap(List<TreeMapNode>nodes, Rectangle2D rect, Layout alignment) 
	{
		List<TreeMapNode> row = new Vector<TreeMapNode>();
		double avAspectRatio = Double.MAX_VALUE, newAvAspectRatio,layoutSide=0;

		while (nodes.size() > 0)
		{        	 
			TreeMapNode item = nodes.get(0); 
			double a = getArea(item);

			if (a <= 0) 
			{
				nodes.remove(0);
				continue;
			}

			row.add(item); 

			if (alignment == Layout.VERTICAL)
			{
				layoutSide = rect.getHeight();
			}
			else
			{
				layoutSide = rect.getWidth();
			}

			newAvAspectRatio = getAvAspectRatio(row, layoutSide);

			if (newAvAspectRatio <= avAspectRatio) 
			{
				nodes.remove(0);	
				avAspectRatio = newAvAspectRatio;              
			} 
			else 
			{
				row.remove(row.size()-1); 							// Remove the latest addition since it worsens the average aspect ratio.
				lookahead(nodes,row,avAspectRatio,layoutSide);		// Consider combining with next strip if better aspect ratio overall.

				layoutLine(row, layoutSide, rect,alignment);		// Lay out the current strip.

				row.clear(); // clear the row
				avAspectRatio = Double.MAX_VALUE;  
			}
		}
		if (row.size() > 0) 
		{
			layoutLine(row, layoutSide, rect,alignment); 	// Lay out the current row.
			row.clear(); 						   			// Clear the row.
		}
	}


	/** Recursively lays out the given nodes in the given rectangle with a reasonably stable and square layout.
	 * Uses the pivot algorithms presented by Bederson, Schneiderman and Wattenberg, 2002. 
	 * @param nodes Nodes to lay out.
	 * @param rect Rectangle in which to lay out nodes.
	 */
	private void pivot(List<TreeMapNode>nodes, Rectangle2D rect) 
	{
		// Look for base case first.
		if (nodes.size() <= 4)
		{
			// Nothing to lay out.
			if (nodes.size() == 0)
			{
				return;
			}
			int level = nodes.get(0).getLevel() - 1;

			// Use a squarified layout for 1-4 nodes.
			if (layoutTypes[level]==Layout.PIVOT_SPACE)
			{
				orderedSquarify(nodes, rect, Layout.SPATIAL,alignments[level]);
			}
			else
			{
				squarify(nodes, rect,alignments[level]);
			}
			return;
		}

		// As we have more than 4 nodes, find the pivot position.
		int pivotPosition = -1;
		int level = nodes.get(0).getLevel() - 1;
		if (layoutTypes[level] == Layout.PIVOT_SIZE)
		{
			double maxArea = -1;
			for (int i=0; i<nodes.size(); i++)
			{
				TreeMapNode node = nodes.get(i);
				if (node.getArea() > maxArea)
				{
					maxArea = node.getArea();
					pivotPosition = i;
				}
			}
		}
		else if (layoutTypes[level] == Layout.PIVOT_MIDDLE)
		{
			pivotPosition = nodes.size()/2;
		}
		else if (layoutTypes[level] == Layout.PIVOT_SPLIT_SIZE)
		{
			double targetArea = rect.getWidth()*rect.getHeight()/2;
			double area = 0;
			for (int i=0; i<nodes.size(); i++)
			{
				TreeMapNode node = nodes.get(i);
				if (Math.abs(area+node.getArea()-targetArea) < Math.abs(area-targetArea))
				{
					area += node.getArea();
					pivotPosition = i;
				}
				else
				{
					break;
				}
			}
		}
		else if (layoutTypes[level] == Layout.PIVOT_SPACE)
		{
			// Sort nodes in an E-W direction if enclosing rectangle horizontal, otherwise N-S direction
			if (((alignments[level]==Layout.FREE) && (rect.getWidth() > rect.getHeight())) || (alignments[level]==Layout.VERTICAL))
			{
				Collections.sort(nodes, TreeMapNode.getEWComparator());
			}
			else
			{
				Collections.sort(nodes, TreeMapNode.getNSComparator());
			}	

			pivotPosition = nodes.size()/2;	
		}
		else
		{
			System.err.println("Error: Unknown pivot algorithm requested.");
			return;
		}

		// Divide list into 3 sub lists, L1 < pivot; L2 containing pivot and enough to make 
		// pivot node as square as possible; L3 remaining nodes > L2.

		List<TreeMapNode>list1 = new Vector<TreeMapNode>();
		List<TreeMapNode>list2 = new Vector<TreeMapNode>();
		List<TreeMapNode>list3 = new Vector<TreeMapNode>();

		double area1=0, area2,r1Width,r1Height,r2Width,r2Height;

		for (int i=0; i<pivotPosition; i++)
		{
			TreeMapNode node = nodes.remove(0);
			list1.add(node);
			area1 += node.getArea();
		}

		TreeMapNode pivotNode = nodes.remove(0);
		list2.add(pivotNode);
		area2 = pivotNode.getArea();

		int numNodesLeft = nodes.size();
		for (int i=0; i<numNodesLeft; i++)
		{
			TreeMapNode node = nodes.remove(0);
			list3.add(node);
		}

		// Layout out L1 so all items fill the left (or top) of the containing rectangle.

		Rectangle2D rect2  = null;
		boolean isHorizontal;
		if (((alignments[level]==Layout.FREE) && (rect.getWidth() < rect.getHeight())) || (alignments[level]==Layout.HORIZONTAL))
		{
			r1Width = rect.getWidth();
			r1Height = area1/rect.getWidth();
			rect2 = new Rectangle2D.Double(rect.getX(),rect.getY()+r1Height,rect.getWidth(),area2/rect.getWidth());
			isHorizontal= false;
		}
		else
		{
			r1Height = rect.getHeight();
			r1Width = area1/rect.getHeight();
			rect2 = new Rectangle2D.Double(rect.getX()+r1Width,rect.getY(),area2/rect.getHeight(),rect.getHeight());
			isHorizontal=true;
		}
		Rectangle2D rect1 = new Rectangle2D.Double(rect.getX(),rect.getY(),r1Width, r1Height);
		pivot(list1,rect1);

		// See how many nodes need to be added to L2 to minimise the aspect ratio of the pivot node.
		r2Width = rect2.getWidth();
		r2Height = rect2.getHeight();
		double pivotAspectRatio = Math.max(rect2.getWidth()/rect2.getHeight(), rect2.getHeight()/rect2.getWidth());
		double minAspectRatio = pivotAspectRatio;

		// Try taking nodes from List3 and putting them in List 2 until we no longer improve the aspect ratio of the pivot.
		while(list3.size() > 0)
		{
			TreeMapNode newNode = list3.get(0);
			area2 += newNode.getArea();
			list2.add(newNode);
			double newRectWidth,newRectHeight;

			if (isHorizontal)
			{
				newRectWidth = area2/rect.getHeight();
				newRectHeight = rect.getHeight();
				double widthSq = newRectWidth*newRectWidth;
				pivotAspectRatio = Math.max(widthSq/pivotNode.getArea(), pivotNode.getArea()/widthSq);
			}
			else
			{
				newRectHeight = area2/rect.getWidth();
				newRectWidth = rect.getWidth();
				double heightSq = newRectHeight*newRectHeight;
				pivotAspectRatio = Math.max(heightSq/pivotNode.getArea(), pivotNode.getArea()/heightSq);
			}

			if (pivotAspectRatio < minAspectRatio)
			{
				// We have improved the pivot aspect ratio, so update the size of rect2 and complete 
				// transfer of node in list 3 to list 2.
				list3.remove(0);
				r2Width = newRectWidth;
				r2Height = newRectHeight;
				rect2.setRect(rect2.getX(), rect2.getY(), r2Width, r2Height);
				minAspectRatio = pivotAspectRatio;
			}
			else
			{
				// Discard the last addition.
				list2.remove(list2.size()-1);
				area2 -= newNode.getArea();
				break;
			}
		}

		// The three lists should now contain the optimum number of nodes to maximise pivot aspect ratio.
		pivot(list2,rect2);

		Rectangle2D rect3;

		if (isHorizontal)
		{
			double rect3x = rect.getX()+r1Width+r2Width;
			rect3 = new Rectangle2D.Double(rect3x,rect.getY(),rect.getWidth()-(r2Width+r1Width),rect.getHeight());
		}
		else
		{
			double rect3y = rect.getY()+r1Height+r2Height;
			rect3 = new Rectangle2D.Double(rect.getX(),rect3y,rect.getWidth(),rect.getHeight()-(r2Height+r1Height));
		}
		pivot(list3,rect3);
	}


	/** Considers whether combining the next row with the given row would produce a better overall
	 * aspect ratio. If it does, the contents of <code>row1</code> are updated with both rows, ready
	 * for layout by the <code>strip()</code> method.
	 * @param nodes List of remaining nodes to lay out.
	 * @param row1 Current row to lay out.
	 * @param row1AR Aspect ratio of the current row to lay out.
	 * @param width Width of strip in which to place nodes.
	 */
	private void lookahead(List<TreeMapNode>nodes, List<TreeMapNode>row1, double row1AR, double width) 
	{
		List<TreeMapNode> row2 = new Vector<TreeMapNode>();
		double row2AR = Double.MAX_VALUE, newRow2AR;

		if (nodes.size() == 0)
		{
			// No more nodes to process, so existing last row was optimal.
			return;
		}

		for (TreeMapNode item : nodes)
		{        	 
			//TreeMapNode item = nodes.get(0); 
			double a = getArea(item);

			if (a <= 0) 
			{
				//nodes.remove(0);
				continue;
			}

			row2.add(item);         
			newRow2AR = getAvAspectRatio(row2, width);

			if (newRow2AR <= row2AR) 
			{
				//nodes.remove(0);	
				row2AR = newRow2AR;              
			} 
			else 
			{
				row2.remove(row2.size()-1); 		// Remove the latest addition since it worsens the average aspect ratio.
				break;
			}
		}

		double row12AR = (row1AR*row1.size() + row2AR*row2.size())/(row1.size()+row2.size());

		List<TreeMapNode>combinedRows = new Vector<TreeMapNode>();
		combinedRows.addAll(row1);
		combinedRows.addAll(row2);
		double combinedAR = getAvAspectRatio(combinedRows, width);

		// Combining this and the next row would produce better overall aspect ratio
		if (combinedAR < row12AR)
		{
			row1.addAll(row2);
			nodes.removeAll(row2);
		}
	}


	/** Lays out the given nodes in the given rectangle using Schneiderman's slice and dice algorithm. 
	 * Nodes are ordered from left to right or from top to bottom depending on the aspect ratio of the
	 * enclosing rectangle.
	 * @param nodes Nodes to lay out.
	 * @param rect Rectangle in which to lay out nodes.
	 * @param alignment Alignment constraint (HORIZONTAL, VERTICAL or FREE if no constraint).
	 */
	private void sliceAndDice(List<TreeMapNode>nodes, Rectangle2D rect, Layout alignment) 
	{
		// Simply lay out all sibling nodes along the longer side of the containing rectangle.
		if (alignment == Layout.FREE)
		{
			layoutLine(nodes, Math.max(rect.getWidth(), rect.getHeight()), rect,alignment);
		}
		else if (alignment == Layout.HORIZONTAL)
		{
			layoutLine(nodes, rect.getWidth(), rect, alignment);
		}
		else if (alignment == Layout.VERTICAL)
		{
			layoutLine(nodes, rect.getHeight(), rect, alignment);
		}
	}

	/** Lays out the given nodes inside the given rectangle in Morton order. 
	 * @param nodes Nodes to lay out.
	 * @param rectangle Rectangle in which to lay out nodes.
	 */
	private void mortonise(MortonList<TreeMapNode>nodes, Rectangle2D rectangle) 
	{
		List<TreeMapNode> column = new Vector<TreeMapNode>();
		int nodesToProcess = nodes.size();
		Rectangle2D rect = rectangle;

		if (nodesToProcess == 0)
		{
			return;
		}

		int mortonPosition = 0;
		int newMortonPosition = 0;

		while (nodesToProcess > 0)
		{
			TreeMapNode item = nodes.get(mortonPosition);
			
			column.add(item);
			nodesToProcess--;

			int x = nodes.getX(mortonPosition);
			int y = nodes.getY(mortonPosition);
			newMortonPosition = nodes.getMorton(x, y+1);

			if (newMortonPosition >= nodes.size())
			{
				// We've come to the end of a column, so lay it out and advance to the next column.
				rect = layoutColumn(column, rect);
				column.clear(); 					
				newMortonPosition = nodes.getMorton(x+1, 0);            	
			}

			// Add the new item to the column and continue.
			mortonPosition = newMortonPosition;       
		} 
	}

	/** Lays out the given line of nodes in the given rectangle.
	 * @param nodes Nodes to lay out in a single row or column.
	 * @param sideLength Length of side of the rectangle along which nodes will be laid out.
	 * @param rect Rectangle in which to lay out nodes.
	 * @param alignment If Layout.FREE alignment is determined by sideLength, otherwise determined by Layout.VERTICAL or Layout.HORIZONTAL.
	 * @return Remaining rectangular space after nodes have been laid out.
	 */
	private Rectangle2D layoutLine(List<TreeMapNode> nodes, double sideLength, Rectangle2D rect, Layout alignment)
	{ 
		double s = 0; // sum of row areas

		for (TreeMapNode node : nodes)
		{
			s += getArea(node);
		}

		double x = rect.getX(), y = rect.getY(), d = 0;
		double h = sideLength==0 ? 0 : s/sideLength;

		boolean horiz = false;
		if (sideLength==rect.getWidth())
		{
			horiz = true;

			// Need this if containing rectangle is exactly square.
			if ((alignment == Layout.VERTICAL) && (sideLength == rect.getHeight()))
			{
				horiz = false;
			}
		}
		// Set node positions and dimensions.
		for (TreeMapNode node : nodes)
		{
			TreeMapNode p = node.getParent();
			node.setRectangle(new Rectangle2D.Double(p.getRectangle().getX(),p.getRectangle().getY(),0,0));

			Rectangle2D.Double newRect = new Rectangle2D.Double();
			if (horiz) 
			{
				newRect.x = x+d;
				newRect.y = y;
			} 
			else 
			{
				newRect.x = x;
				newRect.y = y+d;
			}
			newRect.width = node.getRectangle().getWidth();
			newRect.height = node.getRectangle().getHeight();
			node.getRectangle().setRect(newRect);
			double nw = getArea(node)/h;

			if (horiz) 
			{
				newRect.width = nw;
				newRect.height = h;
				d += nw;
			} 
			else 
			{
				newRect.width = h;
				newRect.height = nw;
				d += nw;
			} 
			node.getRectangle().setRect(newRect);
		}

		// update space available in rectangle r
		if (horiz)
		{
			rect.setRect(x,y+h,rect.getWidth(),rect.getHeight()-h);
		}
		else
		{
			rect.setRect(x+h,y,rect.getWidth()-h,rect.getHeight());
		} 
		return rect;
	}


	/** Lays out the given list of nodes as a column in the given rectangle.
	 * @param nodes Nodes to lay out in a single column.
	 * @param rect Rectangle in which to lay out nodes.
	 * @return Remaining rectangular space after nodes have been laid out.
	 */
	private Rectangle2D layoutColumn(List<TreeMapNode> nodes, Rectangle2D rect)
	{ 
		double s = 0; // sum of row areas

		for (TreeMapNode node : nodes)
		{
			s += getArea(node);
		}

		double x = rect.getX(), y = rect.getY(), d = 0;
		double h = rect.getHeight()==0 ? 0 : s/rect.getHeight();

		Rectangle2D.Double newRect = new Rectangle2D.Double();
		// set node positions and dimensions
		for (TreeMapNode node : nodes)
		{
			TreeMapNode p = node.getParent();
			node.setRectangle(new Rectangle2D.Double(p.getRectangle().getX(),p.getRectangle().getY(),0,0));

			newRect.x = x;
			newRect.y = y+d;


			newRect.width = node.getRectangle().getWidth();
			newRect.height = node.getRectangle().getHeight();
			node.getRectangle().setRect(newRect); 
			double nw = getArea(node)/h;

			newRect.width = h;
			newRect.height = nw;

			node.getRectangle().setRect(newRect);
			d += nw;  
		}

		// update space available in rectangle r
		rect.setRect(x+h,y,rect.getWidth()-h,rect.getHeight());
		return rect;
	}

	/* * Reports the highest aspect ratio (least square) of the given list of nodes if
	 * laid out along the given length. 
	 * @param nodes Nodes to consider.
	 * @param sideLength Length of side along which nodes are to be laid out.
	 * @return Highest aspect ratio produced by the given nodes if arranged along the given length.
	 * /
	private double getWorstAspectRatio(List<TreeMapNode> nodes, double sideLength) 
	{
		double aMax = -Double.MAX_VALUE, 
		aMin = Double.MAX_VALUE, 
		totalArea = 0.0;

		for (TreeMapNode node : nodes)
		{
			double area = getArea(node);
			aMin = Math.min(aMin, area);
			aMax = Math.max(aMax, area);
			totalArea += area;
		}
		double totalAreaSq = totalArea*totalArea; 
		double sideLengthSq = sideLength*sideLength;      
		return Math.max(sideLengthSq*aMax/totalAreaSq, totalAreaSq/(sideLengthSq*aMin));
	}
	*/

	/** Reports the highest aspect ratio (least square) of the given list of nodes if
	 *  laid out along the given length. 
	 *  @param nodes Nodes to consider.
	 *  @param sideLength Length of side along which nodes are to be laid out.
	 *  @param targetAspectRatio Target aspect ratio being aimed for.
	 *  @return Highest aspect ratio produced by the given nodes if arranged along the given length.
	 */
	private double getWorstAspectRatio(List<TreeMapNode> nodes, double sideLength, double targetAspectRatio) 
	{
		double aMax = -Double.MAX_VALUE, 
		aMin = Double.MAX_VALUE, 
		totalArea = 0.0;

		for (TreeMapNode node : nodes)
		{
			double area = getArea(node);
			aMin = Math.min(aMin, area);
			aMax = Math.max(aMax, area);
			totalArea += area;
		}
		double totalAreaSq = totalArea*totalArea; 
		double sideLengthSq = sideLength*sideLength;      
		return Math.max(sideLengthSq*aMax/(totalAreaSq*targetAspectRatio), (totalAreaSq*targetAspectRatio)/(sideLengthSq*aMin));
	}

	/** Reports the mean aspect ratio of the given list of nodes if laid out along the given length. 
	 * @param nodes Nodes to consider.
	 * @param sideLength Length of side along which nodes are to be laid out.
	 * @return Mean aspect ratio produced by the given nodes if arranged along the given length.
	 */
	private double getAvAspectRatio(List<TreeMapNode> nodes, double sideLength) 
	{
		double totalArea = 0.0;
		double sideLengthSq = sideLength*sideLength;

		for (TreeMapNode node : nodes)
		{
			totalArea += getArea(node);
		}

		double totalAreaSq = totalArea*totalArea;
		double totalAr = 0;

		for (TreeMapNode node : nodes)
		{
			double ar = Math.max(sideLengthSq*getArea(node)/totalAreaSq,
					totalAreaSq/(sideLengthSq*getArea(node)));
			totalAr += ar;        	 
		}          
		return totalAr/nodes.size();
	}

	/* * Reports the mean displacement of the nodes being laid out in the given rectangle. 
	 * @param nodes Nodes to consider.
	 * @param rect Rectangle in which nodes are to be laid out.
	 * @param totalNodeArea Area of all nodes that will have to be laid out.
	 * @param isHorizontal True if nodes to be laid out in a row, or false if down a column.
	 * @return Mean displacement of nodes if laid out in a row or column.
	 * /
	private double getAvDisplacement(List<LocatedObject> nodes, Rectangle2D rect, double totalNodeArea, boolean isHorizontal) 
	{
		double disp = 0;		// Displacement.

		// Find row/column width.
		double totalRowArea = 0;
		for (LocatedObject locatedNode : nodes)
		{
			totalRowArea += ((TreeMapNode)locatedNode.getObject()).getArea();
		}

		double rowWidth = totalRowArea/totalNodeArea;
		if (isHorizontal)
		{
			rowWidth *= rect.getHeight();
		}
		else
		{
			rowWidth *= rect.getWidth();
		}

		// Find individual displacements
		double rowArea = 0;
		for (LocatedObject locatedNode : nodes)
		{

			Point2D geoPoint = locatedNode.getLocation();
			TreeMapNode node = (TreeMapNode)locatedNode.getObject();
			rowArea += node.getArea();

			double treeX,treeY;
			if (isHorizontal)
			{
				treeX = rect.getX() + rect.getWidth()*(rowArea-node.getArea()/2)/totalRowArea;
				treeY = rect.getY() + rowWidth/2;
			}
			else
			{
				treeX = rect.getX() + rowWidth/2;
				treeY = rect.getY() + rect.getHeight()*(rowArea-node.getArea()/2)/totalRowArea;
			}

			disp += Math.sqrt((geoPoint.getX()-treeX)*(geoPoint.getX()-treeX) +
							  (geoPoint.getY()-treeY)*(geoPoint.getY()-treeY));
		}

		return disp/nodes.size();
	}
	*/

	/* * Reports the weighted mean aspect ratio of the given list of nodes if laid out along the
	 * given length. Aspect ratios are weighted according to the size of each node. 
	 * @param nodes Nodes to consider.
	 * @param sideLength Length of side along which nodes are to be laid out.
	 * @return Weighted mean aspect ratio produced by the given nodes if arranged along the given length.
	 * /
	private double getAvWeightedAspectRatio(List<TreeMapNode> nodes, double sideLength) 
	{
		double totalArea = 0.0;
		double sideLengthSq = sideLength*sideLength;

		for (TreeMapNode node : nodes)
		{
			totalArea += getArea(node);
		}

		double totalAreaSq = totalArea*totalArea;
		double totalAR = 0;
		double sumWeights = 0;
		for (TreeMapNode node : nodes)
		{
			double weight = node.getArea();
			sumWeights += weight;
			double ar = weight*Math.max(sideLengthSq*getArea(node)/totalAreaSq,
					totalAreaSq/(sideLengthSq*getArea(node)));
			totalAR += ar;        	 
		}          
		return totalAR/(sumWeights*nodes.size());
	}
	*/

	/** Builds a tree from the given collection of DOM nodes representing a set of branches.
	 *  Recursively searches the branches for any sub trees. Will order sibling nodes in
	 *  descending order of their value.
	 *  @param branches Set of branches representing a sub-tree. 
	 *  @param parent Parent to which sub-tree branches will connect.
	 */
	private void buildTreeFromDOM(Node[] branches, TreeMapNode parent)
	{
		// TODO: Add location extraction from DOM reader.
		for (Node branch : branches)
		{
			TreeMapNode thisNode = null;

			// See if we have an attribute attached to this branch.
			Node[] attribs = dom.getNodeElements("attribute", branch);
			String branchName = new String("n/a");

			for (Node attrib : attribs)
			{
				if (dom.getNodeAttribute("name", attrib) != null)
				{
					branchName = dom.getNodeAttribute("value", attrib);
				}
			}

			if ((tree == null) || (parent==null))
			{
				// This must be the root node.
				thisNode = new TreeMapNode(branchName,0,null,null);
				root = thisNode;
				tree = new DefaultTreeModel(root);
			}
			else
			{
				thisNode = new TreeMapNode(branchName,0,null,null);
				parent.add(thisNode);

				if (thisNode.getLevel() > maxDepth)
				{
					maxDepth = thisNode.getLevel();
				}
			}

			// See if we have any leaves attached to this branch.
			Node[] leaves = dom.getNodeElements("leaf", branch);

			for (Node leaf : leaves)
			{
				attribs = dom.getNodeElements("attribute", leaf);
				String leafName = new String("n/a");
				Float sizeValue = new Float(1);			// Default size value is 1.

				for (Node attrib : attribs)
				{
					String attName = dom.getNodeAttribute("name", attrib); 
					if (attName.equalsIgnoreCase("name"))
					{
						leafName = dom.getNodeAttribute("value", attrib);
					}
					else if (attName.equalsIgnoreCase("number"))
					{
						sizeValue = new Float(dom.getNodeAttribute("value", attrib));
					}
				}
				TreeMapNode newNode = new TreeMapNode(leafName,0,sizeValue,null); 
				thisNode.add(newNode);

				if (newNode.getLevel() > maxDepth)
				{
					maxDepth =newNode.getLevel();
				}

				// Add the leaf value to this branch.
				thisNode.accumulateSize(sizeValue.floatValue());
			}

			// Look for sub-trees.
			Node[] children = dom.getNodeElements("branch", branch);

			if (children.length > 0)
			{
				buildTreeFromDOM(children,thisNode);	
			}

			// Slice and dice, pivot and strip maps retain original order of nodes, other layouts sort them by size.
			// Note this is for DOM reading only since 'order' cannot be specified in TreeML.
			int level = thisNode.getLevel();
			if ((layoutTypes[level] != Layout.SLICE_AND_DICE)   && (layoutTypes[level] != Layout.STRIP) && 
					(layoutTypes[level] != Layout.PIVOT_MIDDLE)     && (layoutTypes[level] != Layout.PIVOT_SIZE) && 
					(layoutTypes[level] != Layout.PIVOT_SPLIT_SIZE) && (layoutTypes[level] != Layout.PIVOT_SPACE))
			{
				thisNode.sortChildren();
			}

			// Add this node's accumulated value to the parent accumulation
			if (parent != null)
			{
				parent.accumulateSize(thisNode.getAccumSize());
			}
		} 
	}

	/** Reports the area in pixels of a given node.
	 * @param node Node to consider.
	 * @return Number of pixels occupied by this node in the treemap.
	 */ 
	private double getArea(TreeMapNode node)
	{
		return node.getArea();
	}

	// ------------------------------------- Nested Classes -------------------------------------

	/** Class for storing a order-distance pair that can be sorted in by the 'order' value.
	 */
	private class OrderDistance implements Comparable<OrderDistance>
	{
		// --------------------------- Object variables ---------------------------

		private double order,distance;

		// ----------------------------- Constructor ------------------------------

		/** Stores the given order and distance values.
		 * @param order Order to store.
		 * @param distance Distance to store. 
		 */
		public OrderDistance(double order, double distance)
		{
			this.order = order;
			this.distance = distance;
		}

		// ------------------------------- Methods --------------------------------

		/** Performs a numerical comparison between the order value stored here and that in given <code>OrderDistance</code> object.
		 * @param other Another <code>OrderDistance</code> object with which to make the comparison. 
		 * @return -1 if this order is smaller than that in the given object, +1 if it is greater or 0 if identical.  
		 */
		public int compareTo(OrderDistance other)
		{
			if (order < other.getOrder())
			{
				return -1;
			}
			if (order > other.getOrder())
			{
				return 1;
			}
			return 0;
		}

		/** Reports the order value stored in this object.
		 * @return Stored order value.
		 */
		public double getOrder()
		{
			return order;
		}

		/** Reports the distance value stored in this object.
		 * @return Stored distance value.
		 */
		public double getDistance()
		{
			return distance;
		}

		/** Provides a textual description of this object.
		 *@return Textual description.  
		 */
		public String toString()
		{
			return new String("Order: "+order+" Distance: "+distance);
		}    	
	}
}
