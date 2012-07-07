package org.gicentre.treemappa;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.*;				// For collections.

import javax.swing.tree.*;

// ****************************************************************************************************
/** Represents a single node to be included in a tree map. Stores a label, a numeric value representing
 *  the size of the node, a numeric value representing the colour of the node and the sum of numeric
 *  size values below it in the tree.
 *  @author Jo Wood, giCentre.
 *  @version 3.1.1, 7th July, 2012
 */ 
// ****************************************************************************************************

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

public class TreeMapNode implements MutableTreeNode, Comparable<TreeMapNode>,Iterable<TreeMapNode>
{
	// ---------------------- Object variables --------------------------

	private String label;				// Node text label.
	private double order;				// Used to determine node order of siblings.
	private double sizeValue;			// The value to map to the node's size.
	private Float colourValue;		   	// Value to map to the node's colour.
	private double accumSize;			// Accumulated size value of this node and its children.
	private double avOrder;				// Accumulated average order value of this node and its children.
	private Point2D location;			// Location of node (if spatially referenced).
	private Rectangle2D.Double rect;	// Tree map rectangle coordinates.
	private int level;					// Node depth (root at 0)
	private int maxDepth;				// Maximum depth of this node's children (root is depth 0)
	private int numLeaves;				// Number of leaves including this one and all its children.
	// Used for recursive calculation of bounding rectangle.
	private double minGeoX,minGeoY, maxGeoX,maxGeoY;
	private boolean branchIsSpatial;	// Indicates if branch nodes have their own spatial location.
	private boolean useAccumSize;		// Indicates that the size of this node should be based on accumulated descendants

	private static boolean needsUpdate;
	private TreeMapNode parent;
	private Vector<TreeMapNode>children;

	private Vector<TreeMapNode>neighbours;

	private double area;

	private static final DecimalFormat valueFormatter = new DecimalFormat("#0.######");
	private static final DecimalFormat coordFormatter = new DecimalFormat("#0.#");

	// --------------------------- Constructors ----------------------------

	/** Creates a node with the given label. Assumes an order value of 1 and an undefined colour. 
	 *  Until this node is added to an existing tree, it is assumed to be a root leaf node. If the node
	 *  is a leaf it will have a unit size, otherwise it will have a size that is the accumulated size of all
	 *  its children.
	 *  @param label Text label of the node.
	 */
	public TreeMapNode(String label)
	{
		this(label,1, null,null,null);
	}
	
	/** Creates a node with the given label and size. Assumes an order value of 1 and an undefined colour. 
	 *  Until this node is added to an existing tree, it is assumed to be a root leaf node.
	 *  @param label Text label of the node.
	 *  @param sizeValue Numeric value to be associated with the size of the node. If this value is negative,
	 *                   this node is treated as a dummy node with the size of <code>abs(sizeValue)</code>.
	 */
	public TreeMapNode(String label, float sizeValue)
	{
		this(label,1, new Float(sizeValue),null,null);
	}
	
	/** Creates a node with the given label and size value. Assumes an order value of 1 and an undefined colour. 
	 *  Until this node is added to an existing tree, it is assumed to be a root leaf node.
	 *  @param label Text label of the node.
	 *  @param sizeValue Numeric value to be associated with the size of the node or null if it is to be found
	 *                   from the accumulated values of its descendants. If this is a leaf node (ie it has no
	 *                   descendants) and <code>sizeValue</code> is null, its size is assumed to be 1. If this
	 *                   value is negative, this node is treated as a dummy node with the size of <code>abs(sizeValue)</code>.
	 */
	public TreeMapNode(String label, Float sizeValue)
	{
		this(label,1, sizeValue,null,null);
	}
	
	/** Creates a node with the given label, order, size value and colour value. Until this node 
	 *  is added to an existing tree, it is assumed to be a root leaf node.
	 *  @param label Text label of the node.
	 *  @param order Value indicating relative order of node. This value is compared with the order of any
	 *               sibling nodes when sorting them. If order values are the same, nodes are sorted by size
	 *               then colour value.
	 *  @param sizeValue Numeric value to be associated with the size of the node or null if it is to be found
	 *                   from the accumulated values of its descendants. If this is a leaf node (ie it has no
	 *                   descendants) and <code>sizeValue</code> is null, its size is assumed to be 1. If this
	 *                   value is negative, this node is treated as a dummy node with the size of <code>abs(sizeValue)</code>.
	 *  @param colourValue Numeric value to be associated with the colour of the node or null if colour to be generated.
	 */
	public TreeMapNode(String label, double order, Float sizeValue, Float colourValue)
	{
		this(label,order, sizeValue,colourValue,null);
	}

	/** Creates a node with the given label, order, size value, colour value and location. Until this node 
	 *  is added to an existing tree, it is assumed to be a root leaf node.
	 *  @param label Text label of the node.
	 *  @param order Value indicating relative order of node. This value is compared with the order value of all
	 *               sibling nodes when sorting them. If order values are the same, nodes are sorted by size
	 *               then colour value.
	 *  @param sizeValue Numeric value to be associated with the size of the node or null if it is to be found
	 *                   from the accumulated values of its descendants. If this is a leaf node (ie it has no
	 *                   descendants) and <code>sizeValue</code> is null, its size is assumed to be 1. If this
	 *                   value is negative, this node is treated as a dummy node with the size of <code>abs(sizeValue)</code>.
	 *  @param colourValue Numeric value to be associated with the colour of the node or null if colour to be generated.
	 *  @param location Spatial location of node (can be null if not spatially referenced).
	 */
	public TreeMapNode(String label, double order, Float sizeValue, Float colourValue, Point2D location)
	{
		this.label = label;
		this.order = order;
		//root = new RootNode();
		setSizeValue(sizeValue);
		this.colourValue = colourValue;
		this.accumSize = Math.abs(this.sizeValue);
		this.avOrder = order;
		this.location = location;
		this.level = 0;
		this.maxDepth = 0;
		this.numLeaves = 1;
		rect = null;		
		this.parent = null;

		this.children = new Vector<TreeMapNode>();
		this.neighbours = null;       // Until adjacencies are calculated, neighbours should be null.

		if (location != null)
		{
			branchIsSpatial = true;
		}
		//System.err.println("Immediate order for "+label+" is "+order);
	}

	// ---------------------------- Methods --------------------------------

	/** Adds the given child node to this one. Note that once a child has been added, connected
	 *  <code>TreeMapNode</code>s may not return correct values for <code>getLevel()</code> or
	 *  <code>getMaxDepth()</code> until either <code>updateTree()</code> or <code>TreeMappa</code>'s
	 *  <code>buildTreeMap()</code> method has been called. 
	 *  @param child Child node to add.
	 */
	public void add(TreeMapNode child)
	{
		if (children.size() == 0)
		{
			// Transforming a leaf node into a branch node.
			numLeaves = 0;
		}
		children.add(child);
		numLeaves += child.numLeaves;
		child.setParent(this);

		// This will update the immediate child's level only.
		child.level = level+1;

		// Ensure that the child node is pointing to the same root as this one
		//child.root = root;
		//child.root.setTreeMapNode(root.getTreeMapNode());

		/*
		updateLevels(child);	
		maxDepth = Math.max(maxDepth,child.getMaxDepth());
		updateMaxDepth();
		 */
	}

	/** Performs a breadth-first search looking for the first node with the given label. Note that this 
	 *  search does not consider this node itself, but starts with its children. 
	 *  @param nodeLabel Text to search for.
	 *  @return Highest level node matching the given label, or null if no match found.
	 */
	public TreeMapNode findNode(String nodeLabel)
	{
		if (isLeaf())
		{
			return null;
		}

		LinkedList<TreeMapNode> queue = new LinkedList<TreeMapNode>();

		for (TreeMapNode child : children)
		{
			queue.add(child);
		} 

		while (!queue.isEmpty())
		{
			TreeMapNode node = queue.removeFirst();
			for (TreeMapNode child : node.getChildren())
			{
				queue.add(child);
			}

			if (node.getLabel().equals(nodeLabel))
			{
				return node;
			}
		}
		return null;
	}

	/** Sorts the immediate child nodes into descending order.
	 */
	public void sortChildren()
	{
		Collections.sort(children);
		Collections.reverse(children);
	}

	/** Sorts the immediate child nodes using the given comparator. This allows custom
	 *  sorting of nodes, for example by alphabetical order of label.
	 *  @param comparator Comparator to use to compare a pair of TreeMapNodes.
	 */
	public void sortChildren(Comparator<TreeMapNode>comparator)
	{
		Collections.sort(children,comparator);	
	}

	/** Sorts all of the descendants of this node using the given comparator. This allows custom
	 *  sorting of nodes, for example by alphabetical order of label.
	 *  @param comparator Comparator to use to compare a pair of TreeMapNodes.
	 */
	public void sortDescendants(Comparator<TreeMapNode>comparator)
	{
		sortDescendants(this,comparator);
	}

	/** Sorts all descendants into descending order.
	 */
	public void sortDescendants()
	{
		sortDescendants(this);
	}

	/** Sorts all nodes at the given level in the hierarchy into descending order.
	 * @param sortLevel Hierarchy level at which to apply sorting, where 0 is root level. 
	 */
	public void sortAtLevel(int sortLevel)
	{
		sortAtLevel(this,sortLevel);
	}

	/** Reports the path from the root node to this one (line of antecedents).
	 *  @return Text representing path to this node. 
	 */
	public String getPath()
	{
		StringBuffer path = new StringBuffer();
		if (parent != null)
		{
			path.append(label+" <- ");
			path.append(parent.getPath());	
		}
		else
		{
			path.append("*"+getLabel());
		}
		return path.toString();
	}

	/* * Reports the root node of the tree to which this node is attached. If this node
	 *  is not attached to anything, this method returns the node itself.
	 *  @return Root node of the tree in which this node is a part.
	 * / 
	public TreeMapNode getRoot()
	{
		return root.getTreeMapNode();
	}
	 */

	/** Provides a textual description of the node.
	 *  @return Text representation of the node. 
	 */
	public String toString()
	{
		if (rect == null)
		{
			if (location == null)
			{
				return new String(label+": "+valueFormatter.format(sizeValue)+" ("+valueFormatter.format(accumSize)+")");
			}
			return new String(label+": "+valueFormatter.format(sizeValue)+" ("+valueFormatter.format(accumSize)+") with location "+location.getX()+","+location.getY());
		}

		if (location == null)
		{
			return new String(label+": "+valueFormatter.format(sizeValue)+" ("+valueFormatter.format(accumSize)+") with rect ["+coordFormatter.format(rect.getX())+","+coordFormatter.format(rect.getY())+","+coordFormatter.format(rect.getX()+rect.getWidth())+","+coordFormatter.format(rect.getY()+rect.getHeight())+"]");
		}

		return new String(label+": "+valueFormatter.format(sizeValue)+" ("+valueFormatter.format(accumSize)+") with location "+location.getX()+","+location.getY()+" and rect ["+coordFormatter.format(rect.getX())+","+coordFormatter.format(rect.getY())+","+coordFormatter.format(rect.getX()+rect.getWidth())+","+coordFormatter.format(rect.getY()+rect.getHeight())+"]");
	}

	// ---------------- Accessor and mutator methods -------------------

	/** Reports the text label associated with the node.
	 *  @return Text label of the node.
	 */
	public String getLabel() 
	{
		return label;
	}

	/** Reports the value used to determine node order. The smaller the value, the higher up in the list of siblings
	 *  this node will appear. If two sibling nodes have the same order value, the average order of their descendants
	 *  is used, if they are the same, their areas and then their colour values are compared to determine order. 
	 *  @return Value used to determine node order of siblings.
	 */
	public double getOrder() 
	{
		return order;
	}

	/** Reports the numeric value associated with size of node.
	 * @return Numeric value associated with size of the node.
	 */
	public double getSizeValue() 
	{
		return sizeValue;
	}

	/** Reports the numeric value associated with colour of node. Can be null, if colour generation
	 * is automatic. 
	 * @return Numeric value associated with colour of the node.
	 */
	public Float getColourValue() 
	{
		return colourValue;
	}

	/** Reports the accumulated numeric values of this node and its children.
	 * @return Accumulated numeric values.
	 */
	public double getAccumSize() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}
		return accumSize;
	}

	/** Reports the area of this node. This should be approximately proportional to the
	 * accumulated numeric value for this node and its children, but takes into account
	 * any borders between nodes.
	 * @return Area of node.
	 */
	public double getArea() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}
		return area;
	}

	/** Reports the location of this node. This may be null if the node has no spatial reference.
	 * @return Spatial location of node.
	 */
	public Point2D getLocation() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}
		return location;
	}

	/** Reports the bounding rectangle of this node in georeferenced coordinates. This method searches 
	 * all this node's children in order to find the bounds, so should only be called when necessary.
	 * If the node does not have spatial referencing, the same value as <code>getRect()</code> is returned
	 * (ie the pixel coordinates of the node). 
	 * @return Bounds of this node.
	 */
	public Rectangle2D calcGeoBounds()
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}

		if (location == null)
		{
			return rect;
		}

		minGeoX = Float.MAX_VALUE;
		minGeoY = Float.MAX_VALUE;
		maxGeoX = -Float.MAX_VALUE;
		maxGeoY = -Float.MAX_VALUE;

		findGeoBounds(this);

		// If we have bounds without an area, use non-spatial rectangle.
		if ((maxGeoX <= minGeoX) || (maxGeoY <= minGeoY))
		{
			return rect;
		}

		return new Rectangle2D.Double(minGeoX, minGeoY,maxGeoX-minGeoX, maxGeoY-minGeoY);
	}

	/** Performs a recursive search of the given node and its descendants building up its MER.
	 * @param node Node to search.
	 */
	private void findGeoBounds(TreeMapNode node)
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}

		if (node.location == null)
		{
			return;
		}

		if (node.location.getX() < minGeoX)
		{
			minGeoX = node.location.getX();
		}
		if (node.location.getY() < minGeoY)
		{
			minGeoY = node.location.getY();
		}
		if (node.location.getX() > maxGeoX)
		{
			maxGeoX = node.location.getX();
		}
		if (node.location.getY() > maxGeoY)
		{
			maxGeoY = node.location.getY();
		}

		// Search children
		for (TreeMapNode child: node.children)
		{
			findGeoBounds(child);
		}
	}

	/** Reports the depth of this node. 0 is the root node, 1 is a child of the root, 2 is a grandchild etc.
	 *  @return Level of node.
	 */
	public int getLevel() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}

		return level;
	}

	/** Reports the maximum depth that can be found by traversing from this node downward. Maximum depth will always be
	 *  the level of this node if a leaf or at least this level+1 if this is a branch node.
	 *  @return Maximum depth of node and/or its descendants.
	 */
	public int getMaxDepth() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}

		return maxDepth;
	}

	/** Reports the number of leaves attached to this node and any of its descendants in the tree. If this node is a
	 *  leaf, will always return 1, if not it will return a value of at least 1.
	 *  @return Number of leaves from this point downward in the tree.
	 */
	public int getNumLeaves() 
	{
		// Ensure tree is in a consistent state.
		if (needsUpdate)
		{
			rebuild();
		}

		return numLeaves;
	}

	/** Sets the label associated with this node.
	 *  @param label New label.
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/** Sets a new size value for this node. Note that changing a node's size value will not effect the topology
	 *  of the tree, but will affect the future geomoetry associated with each node. To recalculate the geometry
	 *  a call to <code>TreeMappa</code>'s <code>buildTreeMap()</code> method will be necessary.
	 *  @param newSizeValue Numeric value to be associated with the size of the node or null if it is to be found
	 *                      from the accumulated values of its descendants. If this is a leaf node (ie it has no
	 *                      descendants) and <code>sizeValue</code> is null, its size is assumed to be 1. If this
	 *                      value is negative, this node is treated as a dummy node with the size of <code>abs(sizeValue)</code>.
	 */
	public void setSizeValue(Float newSizeValue)
	{
		if (newSizeValue == null)
		{
			this.sizeValue = 1;
			useAccumSize = true;
		}
		else
		{
			this.sizeValue = newSizeValue.floatValue();
			useAccumSize = false;
		}
	}

	/** Sets a new geographic location of this node. Note that changing a node's location will not effect the topology
	 *  of the tree, but will affect the future geomoetry associated with each node. To recalculate the geometry
	 *  a call to <code>TreeMappa</code>'s <code>buildTreeMap()</code> method will be necessary.
	 *  @param newLocation New location to be associated with the node or null if it is to be found from the mean centre
	 *                     of its descendants. If this is a leaf node (ie it has no descendants) and <code>location</code> 
	 *                     is null, it is assumed this node has no location.
	 */
	public void setLocation(Point2D newLocation)
	{
		this.location = newLocation;

		if (location != null)
		{
			branchIsSpatial = true;
		}
	}

	/** Reports the rectangle representing the tree-map coordinates of this node. Note that this geometry may
	 *  not be consistent with the current properties of the tree if it has not been rebuilt with 
	 *  <code>TreeMappa</code>'s <code>buildTreeMap()</code> method since a change to any of this node's 
	 *  properties that might affect geometry.
	 *  @return Rectangle representing the tree-map coordinates of this node.
	 */
	public Rectangle2D.Double getRectangle()
	{
		//System.err.println("Rect is "+rect+" in "+this);
		return rect;
	}		

	/** Reports the children of this node. 
	 *  @return This node's children. 
	 */ 
	public List <TreeMapNode> getChildren()
	{
		return children;
	}

	/** Reports the nodes that are immediate neighbours of this one. Neighbours are only stored for those
	 *  between nodes at the same level in the hierarchy as this node's level.
	 *  @return This node's adjacent neighbours or null if adjacency has not been calculated. This will
	 *          return an empty collection if adjacency has been calculated but this node has no siblings.
	 */ 
	public List <TreeMapNode> getNeighbours()
	{
		return neighbours;
	}

	// --------------------- Package-wide methods -----------------------

	/** Sets the area value for this node. This is used only by TreeMappa when building a treemap 
	 *  and should not be called on a node-by-node basis.
	 *  @param area New area value.
	 */
	void setArea(double area)
	{
		this.area = area;
	}	

	/** Sets the rectangle representing the tree-map coordinates of this node. This is used only by 
	 *  TreeMappa when building a treemap and should not be called on a node-by-node basis.
	 *  @param rect Rectangle representing the tree-map coordinates of this node.
	 */
	void setRectangle(Rectangle2D.Double rect)
	{
		this.rect = rect;
	}

	/** Resets the accumulation values for this and all its descendants based on the values 
	 *  of any leaves found below this one.  This is used only by <code>TreeMappa</code> when building
	 *  a treemap and should not be called on a node-by-node basis.
	 */
	void resetAccumulation()
	{
		resetAccumulation(this);
	}

	/** Adds the given value to the accumulated size values stored in this node. This is used only by
	 *  TreeMappa when building a treemap and should not be called on a node-by-node basis.
	 *  @param value Value to add to the accumulation. 
	 */
	void accumulateSize(double value)
	{
		accumSize += value;
	}

	/** Initialises the neighbour list for this node. This is used only by TreeMappa when building a 
	 *  treemap and should not be called on a node-by-node basis.
	 */
	void resetNeighbours()
	{
		this.neighbours = new Vector<TreeMapNode>();
	}

	/** Adds the given node to those stored as neighbours of this node. This is used only by TreeMappa
	 *  when building a treemap and should not be called on a node-by-node basis.
	 */
	void addNeighbour(TreeMapNode neighbour)
	{
		if (neighbours == null)
		{
			resetNeighbours();
		}
		neighbours.add(neighbour);
	}

	// --------------------- Implemented methods -----------------------

	/** Compares this node with another. Used for sorting nodes using the rules in the following order:
	 *  <ol>
	 *   <li>Compare the 'order' value of the two nodes. If they are equal then,</li>
	 *   <li>Compare the average order of the two nodes' descendants. If they are equal then,</li>
	 *   <li>Compare the 'size' values of the two nodes. If they are equal then,</li>
	 *   <li>Compare the accumulated size of the two nodes' descendants. If they are equal then,</li>
	 *   <li>Compare the colour values (but not the colours themselves) of the two nodes. If they are equal or either
	 *       undefined then,</li>
	 *   <li>Compare the labels of the two node alphabetically. If they are equal then,</li>
	 *   <li>Compare the hash codes of the two nodes</li>
	 *  </ul>
	 *  Nodes will therefore only be considered equal if they are identical object references.</br />
	 *  For alternative ordering of nodes (e.g. alphabetical ordering of node labels), provide a <code>Comparator</code>
	 *  object when calling <code>sortChildren()</code> or <code>sortDescendants()</code>.
	 *  @param node Node with which to compare this one.
	 *  @return Comparative order of this and the other object.
	 */
	public int compareTo(TreeMapNode node)
	{
		// Sort by 'order' value first.
		if (Double.compare(order,node.getOrder()) == 0)
		{
			// If order values identical, sort by average accumulated order next.
			if (Double.compare(avOrder,node.avOrder) == 0)
			{
				// If average accumulated order values identical, sort by size value next.
				if (Double.compare(sizeValue,node.sizeValue)==0)
				{
					// If size values are identical, sort by accumulated size value next.
					if (accumSize == node.getAccumSize())
					{
						// If colour values not defined, try sorting alphabetically by label
						if ((colourValue == null) || (node.getColourValue() == null))
						{
							int alphaSort = node.label.compareTo(label);
							if (alphaSort == 0)
							{
								// If labels are the same, do a hashcode sort.
								return Float.compare(node.hashCode(), hashCode());
							}
							return alphaSort;
						}

						// If size values are equal, sort by colour value.
						if (colourValue.equals(node.getColourValue()))
						{
							// If colour values are equal, try sorting alphabetically by label.
							int alphaSort = node.label.compareTo(label);
							if (alphaSort == 0)
							{
								// If labels are the same, do a hashcode sort.
								return Float.compare(node.hashCode(), hashCode());
							}
							return alphaSort;
						}
						if (colourValue.floatValue() < node.getColourValue().floatValue())
						{
							return -1;
						}
						return 1;
					}
					if (accumSize < node.getAccumSize())
					{
						return -1;
					}
					return 1;	
				}
				if (sizeValue < node.sizeValue)
				{
					return -1;
				}
				return 1;
			}	
			if (avOrder > node.avOrder)
			{
				return -1;
			}
			return 1;
		}
		// Note: Order sorted from largest to smallest so matches size and colour orders.
		if (order > node.getOrder())
		{
			return -1;
		}
		return 1;
	}

	/** Adds the given child node to this one.
	 * @param child Child node to add.
	 * @param index Position in list of children to add. 
	 */
	public void insert(MutableTreeNode child, int index)
	{
		if (child == null)
		{
			throw new IllegalArgumentException("Null 'node' argument.");
		}
		if (!(child instanceof TreeMapNode))
		{
			throw new IllegalArgumentException("Node to insert is not a TreeMapNode.");
		}

		if (isAncestor(child))
		{
			throw new IllegalArgumentException("Cannot insert ancestor node.");
		}

		children.add(index, (TreeMapNode)child);
		//System.err.println("insert(child,index) forcing rebuild: ");
		needsUpdate = true;
	}

	/** Removes the child node at the given index.
	 * @param index Position in list of children to remove. 
	 */
	public void remove(int index) 
	{
		TreeMapNode child = children.remove(index);
		child.setParent(null);
		needsUpdate = true;
		//System.err.println("remove(index) forcing rebuild: ");
	}

	/** Removes the given child node.
	 * @param child Child node to remove.
	 */
	public void remove(MutableTreeNode child) 
	{
		if (child == null)
		{
			throw new IllegalArgumentException("Null 'node' argument.");
		}
		if (child.getParent() != this)
		{
			throw new IllegalArgumentException("The given 'node' is not a child of this node.");
		}

		children.remove(child);
		child.setParent(null);
		needsUpdate = true;
		//System.err.println("remove(child) forcing rebuild: ");
	}

	/** Removes this node from its parent if it has one.
	 */
	public void removeFromParent() 
	{
		if (parent != null)
		{
			parent.remove(this);
			parent = null;
		}
	}

	/** Sets the new given parent of this node.
	 * @param parent New parent to link with this one.
	 */
	public void setParent(MutableTreeNode parent) 
	{
		if (parent == null)
		{
			if (this.parent == null)
			{
				// No change.
				return;
			}

			// We appear to be making this node a root node.
			this.level = 0;
			this.parent = null;
		}
		else
		{
			if (!(parent instanceof TreeMapNode))
			{
				throw new IllegalArgumentException("New parent is not a TreeMapNode: "+parent.getClass());
			}

			this.parent = (TreeMapNode)parent;
		}
		needsUpdate = true;
		//System.err.print(".");
	}

	/** Would set the new user object to be carried by this node, but does nothing
	 * in this case except report an error. 
	 * @param object Object to store (ignored).
	 */
	public void setUserObject(Object object)
	{
		System.err.println("Object "+object.getClass()+" cannot be stored in a TreeMapNode.");
	}

	/** Reports the children of this node.
	 * @return Children associated with this node.
	 */
	public Enumeration<TreeMapNode> children() 
	{
		return children.elements();
	}

	/** Reports whether this node allows children. Always returns true.
	 * @return Always true as this node allows children to be added. 
	 */
	public boolean getAllowsChildren() 
	{
		return true;
	}

	/** Reports the child at the given index.
	 * @param index Index representing position of the child.
	 */
	public TreeMapNode getChildAt(int index) 
	{
		return children.get(index);
	}

	/** Reports the number of children held by this node.
	 * @return Number of children of this node.
	 */
	public int getChildCount() 
	{
		return children.size();
	}

	/** Reports the index of the given node in this node's list of children.
	 * @param node Node to search for.
	 * @return Index of the first occurrence of the given node in this node's children or -1 if object not found.
	 */
	public int getIndex(TreeNode node) 
	{
		return children.indexOf(node);
	}

	/** Reports the parent of this node.
	 * @return This node's parent or null if no parent.
	 */
	public TreeMapNode getParent() 
	{
		return parent;
	}

	/** Reports whether not not this is a leaf node. A leaf is defined as a node without any children.
	 * @return True if this node does not have any children.
	 */
	public boolean isLeaf()
	{
		if (children.size() == 0)
		{
			return true;
		}
		return false;
	}

	/** Provides an iterator that will perform a breadth-first iteration over the hierarchy starting with 
	 *  this node and its children.
	 *  @return Iterator capable of performing a breadth-first search though the hierarchy.
	 */
	public Iterator<TreeMapNode> iterator() 
	{
		return new TreeMapBreadthFirstIterator(this);
	}

	/** Provides a spatial comparator that can be used for ordering nodes in an west-east direction.
	 * @return East-west comparator.
	 */
	public static Comparator<TreeMapNode> getEWComparator()
	{
		return new EWComparator();
	}

	/** Provides a spatial comparator that can be used for ordering nodes in an south-north direction.
	 * @return North-south comparator.
	 */
	public static Comparator<TreeMapNode> getNSComparator()
	{
		return new NSComparator();
	}

	/** Allows a left-right comparison of two spatially referenced treemap nodes.
	 */
	private static class EWComparator implements Comparator<TreeMapNode>
	{
		public EWComparator() 
		{
			super();
		}

		/** Compares two nodes. If the first is to the left (west) of the second, -1 is returned. If to 
		 * the right (east), 1 is returned, or 0 if both have the same easting.
		 * @param node1 First node to compare.
		 * @param node2 Second node to compare.
		 * @return Negative if the first node has smaller easting, 0 if the same, positive if greater easting.
		 */	
		public int compare(TreeMapNode node1, TreeMapNode node2) 
		{ 
			if (node1.getLocation().getX() < node2.getLocation().getX())
			{
				return -1;
			}

			if (node1.getLocation().getX() > node2.getLocation().getX())
			{
				return 1;
			}

			// If the two nodes have the same easting, order by northing.
			if (node1.getLocation().getY() > node2.getLocation().getY())
			{
				return -1;
			}

			if (node1.getLocation().getY() < node2.getLocation().getY())
			{
				return 1;
			}

			return 0;
		}
	}

	/** Allows a up-down comparison of two spatially referenced treemap nodes.
	 */
	private static class NSComparator implements Comparator<TreeMapNode>
	{
		public NSComparator() 
		{
			super();
		}

		/** Compares two nodes. If the first is to the below (south) of the second, -1 is returned. If above
		 * (north), 1 is returned, or 0 if both have the same northing.
		 * @param node1 First node to compare.
		 * @param node2 Second node to compare.
		 * @return Negative if the first node has smaller northing, 0 if the same, positive if greater northing.
		 */	
		public int compare(TreeMapNode node1, TreeMapNode node2) 
		{ 
			if (node1.getLocation().getY() > node2.getLocation().getY())
			{
				return -1;
			}

			if (node1.getLocation().getY() < node2.getLocation().getY())
			{
				return 1;
			}

			// If the two nodes share the same northing, order by east-west.
			if (node1.getLocation().getX() < node2.getLocation().getX())
			{
				return -1;
			}

			if (node1.getLocation().getX() > node2.getLocation().getX())
			{
				return 1;
			}

			return 0;
		}
	}

	// -------------------------------- Private Methods ---------------------------------


	/** Rebuilds the tree by recalculating tree levels, accumulated values and maximum depth.
	 */
	private void rebuild()
	{
		needsUpdate = false;
		//System.err.println("Doing a rebuild.");
		// Find root node.
		TreeMapNode root = this;

		TreeMapNode rootParent = root.getParent();
		while (rootParent != null)
		{
			root = rootParent;
			rootParent = root.getParent();
		}
		rebuild(root);
		resetAccumulation(root);
		needsUpdate = false;
	}

	/** Recursive version of the rebuild which finds tree levels on the way
	 *  down and the maximum depth levels on the way back up.
	 */
	private void rebuild(TreeMapNode node)
	{
		node.maxDepth = 0;
		for (TreeMapNode child: node.children)
		{
			child.level = node.level+1;
			rebuild(child);

			if (child.isLeaf())
			{
				child.maxDepth = child.level;
			}
			node.maxDepth = Math.max(node.maxDepth, child.maxDepth);
		}
	}

	/** Propagates the maximum depth value up to the root node.
	 * /
	private void updateMaxDepthx()
	{
		if (parent != null)
		{
			if (parent.maxDepth < maxDepth)
			{
				parent.maxDepth = maxDepth;
				parent.updateMaxDepthx();
			}
		}
	}
	*/

	/* * Updates the level of the given node and all of its descendants. The level of any given node will
	 *  be that of its parent plus one.
	 *  @param node Node to update, along with all of its descendants.
	 * /
	private void updateLevelsx(TreeMapNode node)
	{
		if (node.parent == null)
		{
			node.level = 0;
		}
		else
		{
			node.level = node.parent.level + 1;
			node.maxDepth = Math.max(node.maxDepth, node.level);
		}
		// Recurse through this node's children.
		for (TreeMapNode child : node.getChildren())
		{
			updateLevelsx(child);
		}
	}
	*/

	/** Resets the accumulation values for the given node and all its descendants based on 
	 *  the values of any leaves found below the given node. This version recursively calls
	 *  itself in order to complete the depth-first search.
	 */
	private void resetAccumulation(TreeMapNode node)
	{
		if (node.isLeaf())
		{
			node.accumSize = Math.abs(node.sizeValue);
			node.avOrder = node.order;
			return;
		}

		// Reset this node's accumulated size and average order and mean centre values.
		node.accumSize = 0;
		node.avOrder = 0;

		if (node.branchIsSpatial == false)
		{
			node.location = new Point2D.Double(0,0);
		}
		for (TreeMapNode child : node.getChildren())
		{
			resetAccumulation(child);
			if (node.useAccumSize)
			{
				node.accumulateSize(child.getAccumSize());
			}
			else
			{
				node.accumSize = Math.abs(node.sizeValue);
			}
			node.avOrder += child.avOrder;
			if (node.branchIsSpatial == false)
			{
				if ((node.location != null) && (child.getLocation() != null))
				{
					node.location.setLocation(node.location.getX()+child.getLocation().getX(),
							node.location.getY()+child.getLocation().getY());
				}
				else
				{
					node.location = null;
				}
			}
		}
		node.avOrder /= node.getChildCount();
		//node.order = node.accumOrder;

		if ((node.branchIsSpatial==false) && (node.location != null))
		{
			// Set the location of a parent node to be the mean centre of its children.
			node.location.setLocation(node.location.getX()/node.getChildCount(),
					node.location.getY()/node.getChildCount());
		}
	}

	/** Reports whether the given node is an ancestor of this one.  An ancestor node is 
	 *  this node, its parent or an ancestor of its parent. Used to prevent loops in trees.
	 *  @param node Node to consider (can be null).
	 *  @return True if the given node is an ancestor of this one.
	 */
	private boolean isAncestor(TreeNode node)
	{
		if (node == null)
		{
			return false;
		}

		TreeNode current = this;
		while ((current != null) && (current != node))
		{
			current = current.getParent();
		}

		return current == node;
	}

	/** Sorts all descendants of the given node into descending order.
	 *  This version recursively calls itself until all sub-nodes sorted. 
	 *  @param node Node whose descendants are to be sorted.
	 */
	private void sortDescendants(TreeMapNode node)
	{
		if (node.isLeaf())
		{
			return;
		}

		node.sortChildren();
		for (TreeMapNode child : node.getChildren())
		{
			sortDescendants(child);
		}
	}

	/** Sorts all of the descendants of the given node using the given comparator. This allows custom
	 *  sorting of nodes, for example by alphabetical order of label. This version recursively calls
	 *  itself until all sub-nodes sorted. 
	 *  @param node Node whose descendants are to be sorted.
	 *  @param comparator Comparator to use to compare a pair of TreeMapNodes.
	 */
	private void sortDescendants(TreeMapNode node, Comparator<TreeMapNode>comparator)
	{
		if (node.isLeaf())
		{
			return;
		}

		node.sortChildren(comparator);
		for (TreeMapNode child : node.getChildren())
		{
			sortDescendants(child,comparator);
		}
	}

	/** Sorts all of the nodes at the given level in the hierarchy into descending order.
	 *  @param sortLevel Level in hierarchy to sort, where 0 is the root level.
	 */
	private void sortAtLevel(TreeMapNode node, int sortLevel)
	{
		if ((node.isLeaf()) || (node.getLevel() > sortLevel))
		{
			return;
		}

		if (node.getLevel() == sortLevel)
		{
			node.sortChildren();
		}
		
		for (TreeMapNode child : node.getChildren())
		{
			sortAtLevel(child,sortLevel);
		}
	}
}