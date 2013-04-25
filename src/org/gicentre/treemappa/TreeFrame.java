package org.gicentre.treemappa;	

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

import org.gicentre.treemappa.gui.JWFrame;

// ***************************************************************************************************
/** Class to display a tree graphically as a tree map and link node tree. 
 *  @author Jo Wood, giCentre.
 *  @version 3.2, 23rd March, 2011.
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

public class TreeFrame extends JWFrame
{
	// ----------------- Class and object variables ---------------------

	private static final long serialVersionUID = 6886576572305629570L;
	private TreeMapPanel treemapPanel;

	// ------------------------ Constructor -----------------------------

	/** Initialises the window for displaying a hierarchical set of data.
	 * If the <code>tree</code> object is provided, a conventional cascading tree representation
	 * will be included in the window along with the treemap. If not, only the treemap is displayed. 
	 * @param treeMappa Object representing the treemap.
	 */
	public TreeFrame(TreeMappa treeMappa)
	{
		super("TreeMappa "+TreeMapApp.VERSION_TEXT);	

		JSplitPane splitPane = null;
		boolean showTreeView = treeMappa.getConfig().getShowTreeView();

		if (showTreeView)
		{
			// Create a collapsable tree view that allows one selection at a time.
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			JTree jTree = new JTree(treeMappa.getTreeModel());
			jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    

			for (int i = 0; i < jTree.getRowCount(); i++) 
			{
				//jTree.expandRow(i);
				jTree.collapseRow(i);
			}

			// Create the scroll pane and add the tree view to it. 
			JScrollPane treeView = new JScrollPane(jTree);
			treeView.setMinimumSize(new Dimension(100, 50));
			treeView.setPreferredSize(treeView.getMinimumSize());
			splitPane.add(treeView);
		}

		JPanel mapPanel = new JPanel(new BorderLayout());                    
		treemapPanel = new TreeMapPanel(600,400,treeMappa);
		treemapPanel.updateImage();

		mapPanel.add(treemapPanel,BorderLayout.CENTER);

		if ((showTreeView) && (splitPane != null))
		{
			splitPane.add(mapPanel);
			getContentPane().add(splitPane);
		}
		else
		{
			getContentPane().add(mapPanel);
		}

		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		// Necessary for new Java 6 repaint manager behaviour
		treemapPanel.repaint();
	}


	/** Writes the tree map display to an image or SVG file with the given name.
	 * @param imgFileName Name of image file to write.
	 * @return True if file written successfully.
	 */
	public boolean writeImage(String imgFileName)
	{
		return treemapPanel.writeImage(imgFileName);
	}

	/** Provides the panel in which the treemap is displayed.
	 *  @return Panel in which the treemap is displayed.
	 */
	public TreeMapPanel getTreeMapPanel()
	{
		return treemapPanel;
	}

	/*
	public void changeBorders()
	{
		treemapPanel.setBorders(10);
		treemapPanel.updateLayout();
		validate();
	}
	 */
}