package org.gicentre.treemappa;

import java.io.File;

//  **************************************************************************
/** Class to read treeMappa command line parameters and start the application. 
  * @author Jo Wood, giCentre.
  * @version 3.2.1, 6th April, 2014.
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

public class TreeMapApp
{
	// ----------------------- Starter Method ---------------------------
													/** Description of command line syntax. */
	//public static final String USAGE = new String("Usage: TreeMapApp [layout[n] <squarified|orderedSquarified|spatial|sliceAndDice|strip|pivotSize|pivotMiddle|pivotSplit|pivotSpace|morton>] [align[n] <horizontal|vertical|free>] inFile <file_name> [type <treeML|csv|csvCompact|csvSpatial>] [useLabels <true|false>] [outFile <file_name>] [cTable <file_name>] [seed <long_integer>] [imageFile <file_name>] [transparent <true|false>] [border[n] <num_pixels>] [width <num_pixels>] [height <num_pixels>] [showTreeView <true|false>] [allowVerticalLabels <true|false>] [labelLeaves <true|false>] [showLeafDisplacement <true|false>] [maxLeafText <font_pt_size>] [labelBranches <true|false>] [showBranchDisplacement[n] <true|false>] [showArrowHead <true|false>] [vectorWidth[n] <num_pixels>] [leafVectorWidth <num_pixels>] [maxBranchText[n] <font_pt_size>] [textColour[n] <#hexColour>] [leafTextColour <#hexColour>] [statistics <true|false>] [textOnly <true|false>] [verbose <true|false>] [loadConfig <file_name>] [saveConfig <file_name>] [version]");
	
								/** Maximum hierarchy depth supported by TreeMappa. */
	static final int MAX_DEPTH = 20;
	
                               /** Copyright statement. */
    static final String COPYRIGHT = "(c) Jo Wood, giCentre, 2008-14";
    
	/** Starts the tree map application.
      * @param args Command line arguments. See USAGE for details.
      */
    public static void main(String args[])
    {   	
    	StringBuffer clOptions = new StringBuffer();    	
    	TreeMapProperties props = new TreeMapProperties();
 
    	// Extract the individual options
    	for (int i=0; i<args.length; i+=2)
       	{
       	    // Extract command and argument pair.
    		String command = args[i].trim();
       	    
    		// Process commands that don't require any arguments
       	    if ((command.equalsIgnoreCase(TreeMapProperties.VERSION)) || (command.equalsIgnoreCase("-"+TreeMapProperties.VERSION)))
       	    {
   	    		System.out.println("treeMappa "+Version.getText());
   	    		System.out.println(COPYRIGHT);
   	    		return;
       	    }
       	    if ((command.equalsIgnoreCase(TreeMapProperties.HELP)) || (command.equalsIgnoreCase("-"+TreeMapProperties.HELP)))
    	    {
       	    	props.displayOptions(System.out);
	    		return;
    	    }
       	 
       	    // Process commands with arguments.
       	    if (i >= args.length-1)
       	    {
       	    	System.err.println("Cannot process '"+clOptions+"', no argument provided for '"+command+"'");
       	    	return;
       	    }
       	    	
       	    String argument = args[i+1].trim();
    	    
       	    // Strip any enclosing quotes from each of the arguments.
       	    if (argument.startsWith("\""))
       	    {
      			while (!(argument.endsWith("\"")) && (i < args.length))
       	    	{
      				i++;
      				argument = argument+args[i].trim();
       	    	}
       	    }
       	    
       	    // Check to see if we are to load properties from a file
       	    if (command.equalsIgnoreCase(TreeMapProperties.LOAD_CONFIG))
       	    {
       	    	if (argument.length()==0)
       	    	{
       	    		System.err.println("'"+command+"' must be followed by a valid file name");
       	    		props.displayOptionsShort(System.err);
    				return;
       	    	}
       	    	if (props.load(argument) == false)
       	    	{
       	    		return;
       	    	}
       	    }
       	           	    
       	    // Store each of the command line options.
       	    else if (props.setParameter(command, argument) == false)
       	    {
       	    	props.displayOptionsShort(System.err);
       	    	return;
       	    }
       	} 	
    	
    	// Check for absence of any command line arguments.
    	if (args.length == 0)
    	{
    		props.displayOptionsShort(System.err);
    		return;
    	}
    	
	
    	// Check to see if the configuration should be saved to file.
    	if (props.getConfigFileName() != null)
    	{
    		if (props.save(props.getConfigFileName()))
    		{
    			System.out.println("Saved configuration options to "+new File(props.getConfigFileName()).getAbsolutePath());
    		}
    	}
        
    	// Create the tree map and write any output files requested.
    	TreeMappa treeMappa = new TreeMappa(props);
    		
 		if (treeMappa.readData() == false)
    	{
    		System.exit(-1);
    	}
    	   	 		
    	treeMappa.buildTreeMap();
    	
    	if (props.getOutFileName() != null)
    	{
    		treeMappa.writeOutput();
    	}
    	
    	TreeFrame frame = treeMappa.createWindow();
    	TreeMapPanel tmPanel = frame.getTreeMapPanel();
    	        	
    	if (props.getShowStatistics())
    	{
    		treeMappa.showStatistics();
    	}
    
    	if (props.getImageFileName() != null)
    	{
    		tmPanel.writeImage(props.getImageFileName());
    	}
    }
}