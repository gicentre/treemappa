package org.gicentre.io;

import java.io.*;
import java.text.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;

//  ************************************************************************************************
/** Used to write Dbase III files. This code is based on the class provided as part of the Geotools
 *  OpenSource mapping toolkit - <a href="http://www.geotools.org/">http://www.geotools.org/</a>
 *  under the GNU Lesser General Public License. The general use of this class is:
 *  <code><pre>
 *  DbaseFileHeader header = ...
 *  WritableFileChannel out = new FileOutputStream("thefile.dbf").getChannel();
 *  DbaseFileWriter w = new DbaseFileWriter(header,out);
 *  while ( moreRecords ) 
 *  {
 *    w.write( getMyRecord() );
 *  }
 *  w.close();
 *  </pre></code>
 *  You must supply the <code>moreRecords</code> and <code>getMyRecord()</code> logic.
 *  @author Ian Schneider with minor modifications by Jo Wood, giCentre.
 *  @version 3.0, 24th February, 2011.
 */
//  ************************************************************************************************


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

public class DbaseFileWriter 
{
    // ------------------------------- Object Variables -------------------------------
  
    private DbaseFileHeader header;
    private DbaseFileWriter.FieldFormatter formatter = new DbaseFileWriter.FieldFormatter();
    private WritableByteChannel channel;
    private ByteBuffer buffer;
    private final Number NULL_NUMBER = new Integer(0);
    private final String NULL_STRING = "";
    private final Date NULL_DATE = new Date();
  
    // --------------------------------- Constructors ---------------------------------
  
    /** Creates a DbaseFileWriter using the specified header and writing to
      * the given channel.
      * @param header The DbaseFileHeader to write.
      * @param out The channel to write to.
      * @throws IOException If errors occur while initializing.
      */
    public DbaseFileWriter(DbaseFileHeader header, WritableByteChannel out) throws IOException 
    {
        header.writeHeader(out);
        this.header = header;
        this.channel = out;
    
        init();
    }
 
    // -------------------------- Methods ----------------------------
  
    /** Write a single dBase record.
      * @param record The entries to write.
      * @throws IOException If IO error occurs or the entry doesn't comply with the header.
      */
    public void write(Object[] record) throws IOException
    {
        if (record.length != header.getNumFields()) 
        {
            throw new IOException("Wrong number of fields " + record.length + " expected " +  header.getNumFields());
        }
    
        buffer.position(0);
    
        // Put the 'not-deleted' marker
        buffer.put( (byte) ' ');
    
        for (int i = 0; i < header.getNumFields(); i++) 
        {
            String fieldString = fieldString(record[i], i);
            buffer.put(fieldString.getBytes());
        }
        write();
    }
    
    
    /** Release resources associated with this writer.
      * <b>Highly recommended</b>
      * @throws IOException If problem releasing all resources.
      */
    public void close() throws IOException 
    {
        // IANS - GEOT 193, bogus 0x00 written. According to dbf spec, optional
        // eof 0x1a marker is, well, optional. Since the original code wrote a
        // 0x00 (which is wrong anyway) lets just do away with this :)
        // - produced dbf works in OpenOffice and ArcExplorer java, so it must
        // be okay. 
        //    buffer.position(0);
        //    buffer.put((byte) 0).position(0).limit(1);
        //    write();
      
        channel.close();
        /*
        if (buffer instanceof MappedByteBuffer) 
        {
            NIOUtilities.clean(buffer);
        }
        */
        buffer = null;
        channel = null;
        formatter = null;
    }
  
    // --------------------------- Private Methods ----------------------------
  
    /** Initialises the writer.
      */
    private void init()
    {
        buffer = ByteBuffer.allocateDirect(header.getRecordLength());
    }
  
    /** Empties the output buffer to the output channel.
      * @throws IOException If problems occur while writing.
      */
    private void write() throws IOException 
    {
        buffer.position(0);
        int r = buffer.remaining();
        while ( (r -= channel.write(buffer)) > 0) 
        {
            // Do nothing
        }
    }
    
    /** Converts the given object into its fieldstring representation.
      * @param obj Object to convert.
      * @param col Database column number in which this object occurs. 
      * @return Text representation of the object.
      */
    private String fieldString(Object obj,final int col) 
    {
        String o;
        final int fieldLen = header.getFieldLength(col);
        switch (header.getFieldType(col)) 
        {
            case 'C':
            case 'c':
                o = formatter.getFieldString(fieldLen, obj == null ? NULL_STRING : obj.toString());
                break;
          
            case 'L':
            case 'l':
                o = (obj == null ? "F" : obj == Boolean.TRUE ? "T" : "F");
                break;
            
            case 'M':
            case 'G':
                o = formatter.getFieldString(fieldLen, obj == null ? NULL_STRING : obj.toString());
                break;
            
            case 'N':
            case 'n':
                if (header.getFieldDecimalCount(col) == 0) 
                {
                    o = formatter.getFieldString(fieldLen, 0, (Number) (obj == null ? NULL_NUMBER : obj));
                    break;
                }
                                
            //$FALL-THROUGH$
            case 'F':
            case 'f':
                o = formatter.getFieldString(fieldLen, header.getFieldDecimalCount(col),(Number) (obj == null ? NULL_NUMBER : obj));
                break;
                
            case 'D':
            case 'd':
                o = formatter.getFieldString((Date) (obj == null ? NULL_DATE : obj));
                break;
                
            default:
                throw new RuntimeException("Unknown type " + header.getFieldType(col));
        }
        
        return o;
    }
  
    // --------------------------- Nested classes -----------------------------
  
    /** Utility for formatting Dbase fields. 
     */
    public static class FieldFormatter 
    {
        // ------ Object variables.
        
        private StringBuffer buffer = new StringBuffer(255);
        private NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.US);
        private Calendar calendar = Calendar.getInstance(Locale.US);
        private String emtpyString;
        private static final int MAXCHARS = 255;
        
        // ------- Constructor.
        
        /** Creates the field formatter.
          */ 
        public FieldFormatter() 
        {
            // Avoid grouping on number format
            numFormat.setGroupingUsed(false);
          
            // build a 255 white spaces string
            StringBuffer sb = new StringBuffer(MAXCHARS);
            sb.setLength(MAXCHARS);
            for(int i = 0; i < MAXCHARS; i++) 
            {
                sb.setCharAt(i, ' ');
            }
          
            emtpyString = sb.toString();
        }
        
        // ------- Methods.
        
        /** Reports the field string set to the given size.
         *  @param size Size to set the field string to be.
         *  @param s String to set to the given size.
         *  @return New field string set to the given size. 
         */
        public String getFieldString(int size, String s) 
        {
            buffer.replace(0, size, emtpyString);
            buffer.setLength(size);
          
            if (s != null) 
            {
                buffer.replace(0, size, s);
                if (s.length() <= size) 
                {
                    for (int i = s.length(); i < size; i++) 
                    {
                        buffer.append(' ');
                    }
                }
            }
          
            buffer.setLength(size);
            return buffer.toString();
        }
        
        /** Reports the field string representing the given date.
         *  @param d Date to represent.
         *  @return Field string representation of the given date.
         */
        public String getFieldString(Date d) 
        {
          
            if (d != null) 
            {
                buffer.delete(0, buffer.length());
            
                calendar.setTime(d);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;  // returns 0 based month?
                int day = calendar.get(Calendar.DAY_OF_MONTH);
            
                if (year < 1000) 
                {
                    if (year >= 100) 
                    {
                        buffer.append("0");
                    }
                    else if (year >= 10) 
                    {
                        buffer.append("00");
                    }
                    else 
                    {
                        buffer.append("000");
                    }
                }
                
                buffer.append(year);
            
                if (month < 10) 
                {
                    buffer.append("0");
                }
                buffer.append(month);
            
                if (day < 10) 
                {
                    buffer.append("0");
                }
                buffer.append(day);
            }
            else 
            {
                buffer.setLength(8);
                buffer.replace(0, 8, emtpyString);
            }
          
            buffer.setLength(8);
            return buffer.toString();
        }
        
        /** Reports the field string representation of a number to a given number of decimal places.
         *  @param size Size to set the string representation of the number.
         *  @param decimalPlaces Number of decimal places to report the number.
         *  @param n The number to represent as a field string.
         *  @return Text representation of the number. 
         */
        public String getFieldString(int size, int decimalPlaces, Number n) 
        {
            buffer.delete(0, buffer.length());
          
            if (n != null) 
            {
                numFormat.setMaximumFractionDigits(decimalPlaces);
                numFormat.setMinimumFractionDigits(decimalPlaces);
                numFormat.format(n, buffer, new FieldPosition(NumberFormat.INTEGER_FIELD));
            }
          
            int diff = size - buffer.length();
            
            if (diff >= 0) 
            {
                while (diff-- > 0) 
                {
                    buffer.insert(0, ' ');
                }
            }
            else 
            {
                buffer.setLength(size);
            }
            
            return buffer.toString();
        }
    }
}
