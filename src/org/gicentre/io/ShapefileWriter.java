package org.gicentre.io;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import org.gicentre.treemappa.TreeMapNode;

//  ********************************************************************************************
/** Writes out a treemap as a collection of ESRI shapefiles. A shapefile consists of 3 separate
 *  files - <code><i>name</i>.shp</code> containing the geometry; <code><i>name</i>.shx</code> 
 *  containing the file offsets for the components that make up the geometry; and 
 *  <code><i>name</i>.dbf</code> containing the attributes.
  * @author Jo Wood, giCentre.
  * @version 3.0, 24th February, 2011.
  */
//  ***********************************************************************************************

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

public class ShapefileWriter 
{
    // --------------------------- Class variables -----------------------------
   
    private static int recordNumber;
    private static int numAreaVertices;
    private static int numAreaObjects;
    private static int numAreaParts;
        
   // ----------------------------- Constructor --------------------------------
	
	/** There should be no need to call the constructor explicitly since
	  * all methods are static. 
	  */
	public ShapefileWriter()
	{
		super();
	}
	
	// --------------------------- Static Methods ------------------------------

    /** Writes a shapefile based on the supplied treemap node (and all of its decendents).
      * The given fileName can be supplied with or without an extension, but this method will
      * write three files with the same base and extensions <code>.shp</code>, <code>.shx</code>
      * and <code>.dbf</code>. 
      * @param node Treemap node to write. 
      * @param fileName Name of core of the three files to create.
      * @return True if written successfully.
      */
    public static boolean writeNodes(TreeMapNode node, String fileName)
    {	
        // Generate names of the 3 output files.
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = new String(fileName);

        if (dotIndex >0)
        {
            baseName = fileName.substring(0,dotIndex);
        }
        
        // Determine the number of nodes.
        int numChildren = 0;
        
        // Perform a breadth-first search of all nodes.
        LinkedList<TreeMapNode> queue = new LinkedList<TreeMapNode>();
		for (TreeMapNode child : node.getChildren())
        {
            queue.add(child);
        } 
	    
		while (!queue.isEmpty())
		{
			// Write out the node at the head of the queue.
	        TreeMapNode child = queue.removeFirst();
	        numChildren++;
	        
	        for (TreeMapNode grandchild : child.getChildren())
        	{
        		queue.add(grandchild);
        	} 
		}
	                
        if (numChildren == 0)
        {
        	System.err.println("Cannot write shapefile for '"+node.getLabel()+"' as node has no children.");
        	return false;
        }
        
        numAreaVertices = 5*numChildren;	// Areas must be closed (duplicate first and last point) so each rectangle has 5 vertices.
        numAreaObjects = numChildren;
        numAreaParts  = numAreaObjects;
                
        if (numAreaVertices > 0)
        {
            if (writeDBF(node,baseName) == false)
            {
                return false;
            }
            
            if (writeShape(node,baseName) == false)
            {
                return false;
            } 
        }
        return true;   
    }
    
    // ------------------------------- Private Methods ----------------------------------
            
    /** Writes out the attributes of the given node's children as a DBF file (dBase III format).
      * @param node Node with children to write. 
      * @param baseName Prefix of the DBF file to write. Will be appended with <code>P.dbf</code>, <code>L.dbf</code>
      *                 or <code>A.dbf</code> depending on the geometry type.
      * @return True if attribute table written successfully. 
      */
    private static boolean writeDBF(TreeMapNode node, String baseName)
    {
        // NOTE: This should be called after the number of objects to be written has been determined.
        String fileName = new String(baseName+".dbf");
        int numRecords = numAreaObjects;
        LinkedList<TreeMapNode> queue;

        try
        {
            // Create the dbase header.
            DbaseFileHeader header = new DbaseFileHeader();
            
            int numCols = 6;
            
            // Find maximum label size.
            int maxLabelSize = 0;
            queue = new LinkedList<TreeMapNode>();
    		for (TreeMapNode child : node.getChildren())
            {
                queue.add(child);
            } 
    	    
    		while (!queue.isEmpty())
    		{
    			// Write out the node at the head of the queue.
    	        TreeMapNode child = queue.removeFirst();
            	int labelSize = child.getLabel().length();
            	if (labelSize > maxLabelSize)
            	{
            		maxLabelSize = labelSize;
            	}
            	
            	for (TreeMapNode grandchild : child.getChildren())
            	{
            		queue.add(grandchild);
            	} 
            }
            
            if (maxLabelSize > 254)
            {
                System.err.println("Maximum label length is 254 characters in length. Some labels may be truncated for shapefile output.");
                maxLabelSize = 254;
            }
           
            header.addColumn("TreemapID",'N',8,6); 
            header.addColumn("Label",'C',maxLabelSize,0); 
            header.addColumn("Size",'N',18,6);
            header.addColumn("Colour",'N',18,6);
            header.addColumn("Depth",'N',3,0);
            header.addColumn("Leaf",'C',1,0); 
            
            header.setNumRecords(numRecords);

            FileChannel channel = new FileOutputStream(fileName).getChannel();
            DbaseFileWriter writer = new DbaseFileWriter(header,channel);
            int id = 1;
            
            // Perform a breadth-first search of all nodes.
            queue = new LinkedList<TreeMapNode>();
    		for (TreeMapNode child : node.getChildren())
            {
                queue.add(child);
            } 
    	    
    		while (!queue.isEmpty())
    		{
    			// Write out the node at the head of the queue.
    	        TreeMapNode child = queue.removeFirst();
    	    
            	Object rowObjects[] = new Object[numCols];
            	rowObjects[0] = id++;
            	rowObjects[1] = child.getLabel();
            	rowObjects[2] = child.getAccumSize();
            	rowObjects[3] = child.getColourValue();
            	rowObjects[4] = child.getLevel();
            	if (child.isLeaf())
            	{
            		rowObjects[5] = new String("Y");
            	}
            	else
            	{
            		rowObjects[5] = new String("N");
            	}
            
            	writer.write(rowObjects);
            	
            	for (TreeMapNode grandchild : child.getChildren())
            	{
            		queue.add(grandchild);
            	} 
            }
            writer.close();
        }

        catch (IOException e)
        {
            System.err.println("Problem writing Shapefile DBF ("+fileName+").");
            return false;
        }
        
        return true;  
    }
        
    /** Writes out all the descendants of the given node as <code>.shp</code> and <code>.shx</code> files (the geometry and index of a shapefile).
      * @param node Node containing children to write. 
      * @param baseName Prefix of the <code>.shp</code> and <code>.shx</code> files to write.
      * @return True if geometry written successfully. 
      */
    private static boolean writeShape(TreeMapNode node, String baseName)
    {        
        String shpName = new String(baseName+".shp");
        String shxName = new String(baseName+".shx");
        
        if (node.getRectangle() == null)
        {
        	System.err.println("Cannot write node '"+node.getLabel()+"' to shapefile as it has no rectangle coordinates.");
        	return false;
        }

        Rectangle2D bounds = node.getRectangle();
        Rectangle2D geoBounds = node.calcGeoBounds();

        double scaleX = geoBounds.getWidth()/bounds.getWidth();
        double scaleY = geoBounds.getHeight()/bounds.getHeight(); 
                
        try
        {                  
            FileOutputStream shpStream      = new FileOutputStream(shpName);
            BufferedOutputStream shpBStream = new BufferedOutputStream(shpStream);
            
            FileOutputStream shxStream      = new FileOutputStream(shxName);
            BufferedOutputStream shxBStream = new BufferedOutputStream(shxStream);
            
            // Geometry type-specific information.           
            int shapeType = 5; 				// Area type.
            // Note that file size is specified in 16 bit words.
            int shpFileSize = 50 + numAreaObjects*26 + numAreaParts*2 + numAreaVertices*8;
            int shxFileSize = 50 + numAreaObjects*4;  
             
            // Write out headers.
            writeIntBigEndian(9994,shpBStream);         // Shapefile identifier.
            writeIntBigEndian(9994,shxBStream);
             
            for (int i=0; i<5; i++)                     // 5 blank bytes.
            {
                writeIntBigEndian(0,shpBStream);
                writeIntBigEndian(0,shxBStream);
            }
           
            writeIntBigEndian(shpFileSize,shpBStream);  // Length of file in 16-bit words.
            writeIntBigEndian(shxFileSize,shxBStream);
            writeIntLittleEndian(1000,shpBStream);      // Version number.
            writeIntLittleEndian(1000,shxBStream);
            writeIntLittleEndian(shapeType,shpBStream); // Type of shape (geometry).
            writeIntLittleEndian(shapeType,shxBStream);

            writeDoubleLittleEndian(geoBounds.getX(),shpBStream);
            writeDoubleLittleEndian(geoBounds.getX(),shxBStream);
            writeDoubleLittleEndian(geoBounds.getY(),shpBStream);
            writeDoubleLittleEndian(geoBounds.getY(),shxBStream);
            writeDoubleLittleEndian(geoBounds.getX()+geoBounds.getWidth(),shpBStream);
            writeDoubleLittleEndian(geoBounds.getX()+geoBounds.getWidth(),shxBStream);
            writeDoubleLittleEndian(geoBounds.getY()+geoBounds.getHeight(),shpBStream);
            writeDoubleLittleEndian(geoBounds.getY()+geoBounds.getHeight(),shxBStream);
            
            writeDoubleLittleEndian(0,shpBStream);  // Zmin.
            writeDoubleLittleEndian(0,shxBStream);
            writeDoubleLittleEndian(0,shpBStream);  // Zmax.
            writeDoubleLittleEndian(0,shxBStream);
            writeDoubleLittleEndian(0,shpBStream);  // Measured min.
            writeDoubleLittleEndian(0,shxBStream);
            writeDoubleLittleEndian(0,shpBStream);  // Measured max.
            writeDoubleLittleEndian(0,shxBStream);
                     
            // Add geometry records.
            recordNumber = 1;
            int recordOffset = 50;
                        
            // Perform a breadth-first search of all nodes.
            LinkedList<TreeMapNode> queue = new LinkedList<TreeMapNode>();
    		for (TreeMapNode child : node.getChildren())
            {
                queue.add(child);
            } 
    	    
    		while (!queue.isEmpty())
    		{
    			// Write out the node at the head of the queue.
    	        TreeMapNode child = queue.removeFirst();
    	        
            	Rectangle2D r = child.getRectangle();          	
            	Rectangle2D geoR = new Rectangle2D.Double(geoBounds.getX()+ geoBounds.getWidth()*(r.getX()-bounds.getX())/bounds.getWidth(),
            					        				  geoBounds.getY()+ geoBounds.getHeight()*(r.getY()-bounds.getY())/bounds.getHeight(),
            											  r.getWidth()*scaleX,r.getHeight()*scaleY);     	
	            int numCoords = 5;
	            int numParts  = 1;
	            double cy2 = 2*geoBounds.getY()+geoBounds.getHeight();
	            
	           // Store shapefile coordinate with origin at bottom-left rather than top-left
	            double x[] = new double[]{geoR.getX(),geoR.getX()+geoR.getWidth(),geoR.getX()+geoR.getWidth(),geoR.getX(),geoR.getX()};
	            double y[] = new double[]{cy2-geoR.getY(),cy2-geoR.getY(),cy2-(geoR.getY()+geoR.getHeight()),cy2-(geoR.getY()+geoR.getHeight()),cy2-geoR.getY()};           
	            	       
	            // Store array of subpath pointers.
	            int parts[] = new int[numParts];
	            parts[0] = 0;
	                   
	            int recordLength = 2 + 16 + 2 + 2 + numParts*2 + numCoords*8;
	           	                
	            // Record header.
	            writeIntBigEndian(recordNumber,shpBStream);
	            writeIntBigEndian(recordLength,shpBStream);
	                
	            // Add index record.
	            writeIntBigEndian(recordOffset,shxBStream);
	            writeIntBigEndian(recordLength,shxBStream); 
	            recordOffset += (recordLength+4);
	                
	            // Record contents.
	            writeIntLittleEndian(5,shpBStream); 
	 
	            writeDoubleLittleEndian(geoR.getX(),shpBStream);
	            writeDoubleLittleEndian(cy2-geoR.getY(),shpBStream);
	            writeDoubleLittleEndian(geoR.getX()+geoR.getWidth(),shpBStream);
	            writeDoubleLittleEndian(cy2-(geoR.getY()+geoR.getHeight()),shpBStream);
	                    
	            writeIntLittleEndian(numParts,shpBStream);
	            writeIntLittleEndian(numCoords,shpBStream);  
	                    
	            for (int part=0; part<numParts; part++)
	            {
	            	writeIntLittleEndian(parts[part],shpBStream);
	            }
	                    
	            for (int coord=0; coord<numCoords; coord++)
	            {
	            	writeDoubleLittleEndian(x[coord],shpBStream);
	            	writeDoubleLittleEndian(y[coord],shpBStream);
	            }
	        	                    
	            recordNumber++;
	            
	            for (TreeMapNode grandchild : child.getChildren())
	        	{
	        		queue.add(grandchild);
	        	} 
            }
      
            shpBStream.close();
            shpStream.close();
            shxBStream.close();
            shxStream.close();
        }
        catch (IOException e)
        {
            System.err.println("Problem writing shape file <"+shpName+"> or <"+shxName+">");
            return false;
        }
        
        return true;
    }
    
    
    // -------------------------- Private file writing methods -------------------------------
    
    /** Writes a 32 bit unsigned big-endian ('Motorola') word of data to the
      * given output stream.
      * @param value Value to write to output stream.
      * @param os Output stream to process.
      * @return True if written successfully.
      */
    private static boolean writeIntBigEndian(int value, OutputStream os) 
    { 
    	// 4 bytes 
        try
        {
        	byte[] intBytes = new byte[4];
        	intBytes[0] = (byte)((value >> 24)& 0xff);
        	intBytes[1] = (byte)((value >> 16)& 0xff);
        	intBytes[2] = (byte)((value >> 8) & 0xff);
        	intBytes[3] = (byte) (value       & 0xff);
        	//filePointer += 4;
        	os.write(intBytes);
        }
        catch (IOException e)
        {
           System.err.println("Cannot write 32 bit word to output stream: "+e);
        }

        return true;
    }
    
    /** Writes a 32 bit unsigned little-endian ('Intel') word of data to the
      * given output stream.
      * @param value Value to write to output stream.
      * @param os Output stream to process.
      * @return True if written successfully.
      */
    protected static boolean writeIntLittleEndian(int value, OutputStream os) 
    { 
    	// 4 bytes 
        try
        {
        	byte[] intBytes = new byte[4];
        	intBytes[0] = (byte) (value       & 0xff);
        	intBytes[1] = (byte)((value >> 8) & 0xff);
        	intBytes[2] = (byte)((value >> 16)& 0xff);
        	intBytes[3] = (byte)((value >> 24)& 0xff);        	
        	
            //filePointer += 4;
            os.write(intBytes);
        }
        catch (IOException e)
        {
            System.err.println("Cannot write 32 bit word to output stream: "+e);
        }

    	return true;
    }
    
    /** Writes a little-endian 8-byte double to the given output stream.
      * @param value Value to write to output stream.
      * @param os Output stream.
      * @return True if written successfully.
      */
    protected static boolean writeDoubleLittleEndian(double value, OutputStream os) 
    { 
        try
        {
            long doubleBits = Double.doubleToLongBits(value);
                
            byte[] doubleBytes = new byte[8];
        	doubleBytes[0] = (byte) (doubleBits       & 0xff);
        	doubleBytes[1] = (byte)((doubleBits >> 8) & 0xff);
        	doubleBytes[2] = (byte)((doubleBits >> 16)& 0xff);
        	doubleBytes[3] = (byte)((doubleBits >> 24)& 0xff);
        	doubleBytes[4] = (byte)((doubleBits >> 32)& 0xff); 
        	doubleBytes[5] = (byte)((doubleBits >> 40)& 0xff); 
        	doubleBytes[6] = (byte)((doubleBits >> 48)& 0xff); 
        	doubleBytes[7] = (byte)((doubleBits >> 56)& 0xff);         	
        	
            //filePointer += 8;
            os.write(doubleBytes);          
        }
        catch (IOException e)
        {
            System.err.println("Cannot write binary stream: "+e);
            return false;
        }
        return true;
    }
}
