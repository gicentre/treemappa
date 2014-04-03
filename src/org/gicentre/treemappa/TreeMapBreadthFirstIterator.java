package org.gicentre.treemappa;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

//****************************************************************************************************
/** Iterates over part of a tree map hierarchy using a breadth-first traversal.
 *  @author Jo Wood, giCentre.
 *  @version 3.2.1, 23rd February 2011.
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

public class TreeMapBreadthFirstIterator implements Iterator<TreeMapNode>
{
	// --------------------------- Object variables -------------------------------
	
	LinkedList<TreeMapNode>queue;
	
	// ----------------------------- Constructor ----------------------------------
	
	/** Initialises the iterator with the tree map node at the start of the traversal.
	 *  The first item to be returned by this iterator will be the first child of the
	 *  node provided to this constructor (assuming <code>hasNext</code> is true).
	 *  @param startNode Parent node from which to iterate over the tree.
	 */
	public TreeMapBreadthFirstIterator(TreeMapNode startNode)
	{
		queue = new LinkedList<TreeMapNode>();
		for (TreeMapNode child : startNode.getChildren())
		{
			queue.add(child);
		}
	}

	// ------------------------------- Methods ------------------------------------
	
	/** Reports whether or not there are any more elements in the tree that have yet
	 *  to be iterated over.
	 *  @return True if there are more elements to iterate over.
	 */
	public boolean hasNext()
	{
		if (queue.size() > 0)
		{
			return true;
		}
		return false;
	}

	/** Provides the next tree map node in the breadth-first traversal of the hierarchy
	 *  or throws a <code>NoSuchElementException</code> if there are no more nodes to iterate over.
	 *  @return Provides the next tree map node in the breadth-first traversal.
	 */
	public TreeMapNode next() 
	{
		if (!queue.isEmpty())
		{
			TreeMapNode node = queue.removeFirst();
			for (TreeMapNode child : node.getChildren())
			{
				queue.add(child);
			}
			return node;
		}
		throw new NoSuchElementException("No more nodes to iterate in the treemap hierarchy");
	}

	/** Would remove the last returned node from the hierarchy, but does nothing in this case.
	 */
	public void remove() 
	{
		// TODO Add node removal code here.
	}
}
