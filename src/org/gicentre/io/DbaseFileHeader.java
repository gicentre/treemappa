package org.gicentre.io;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.nio.*;
import java.nio.channels.*;

//  ****************************************************************************************************
/** Represents the header of a Dbase III file. This code is based on the class provided as part
 *  of the Geotools OpenSource mapping toolkit - <a href="http://www.geotools.org">www.geotools.org/</a>
 *  under the GNU Lesser General Public License. That in turn is based on the original code in
 *  the GISToolkit project - <a href="http://gistoolkit.sourceforge.net">gistoolkit.sourceforge.net</a>.
 *  @author Geotools/GISToolkit modified by Jo Wood, giCentre.
 *  @version 3.2.0, 24th February, 2011.
 */
//  ****************************************************************************************************

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

public class DbaseFileHeader 
{
    // ----------------- Object and Class Variables ------------------
      
    private static final int FILE_DESCRIPTOR_SIZE=32;   // Constant for the size of a record.
    private static final byte MAGIC=0x03;               // Type of the file, must be 03h.
    private static final int MINIMUM_HEADER = 33;
    private Date date = new Date();                     // Date the file was last updated.
      
    private int recordCnt = 0;      
    private int fieldCnt = 0;
      
    // Set this to a default length of 1, which is enough for one "space"
    // character which signifies an empty record.
    private int recordLength = 1;
      
    // Set this to a flagged value so if no fields are added before the write,
    // we know to adjust the headerLength to MINIMUM_HEADER.
    private int headerLength = -1;
      
    private int largestFieldSize = 0;
    
    private DbaseField[] fields = new DbaseField[0];  // Collection of header records.
    
    private Logger logger = null;
      
   
    // ------------------------ Constructors -------------------------
    
    /** Creates a class for representing a DBase III header.
      */
    public DbaseFileHeader()
    {
        this(null);
    }
    
    /** Creates a class for representing a DBase III header with logged warning
      * and error messages.
      * @param logger Logger to monitor warning and error messages.  
      */
    public DbaseFileHeader(Logger logger)
    {
        super();
        this.logger = logger;
    }
    
    // -------------------------- Methods ----------------------------
    
   /** Determines the most appropriate Java Class for representing the data in the
     * field.
     * <pre>
     * All packages are <code>java.lang</code> unless otherwise specified.
     * C (Character) -&gt; String
     * N (Numeric)   -&gt; Integer or Double (depends on field's decimal count)
     * F (Floating)  -&gt; Double
     * L (Logical)   -&gt; Boolean
     * D (Date)      -&gt; java.util.Date
     * Unknown       -&gt; String
     * </pre>
     * @param i The index of the field, from 0 to <code>getNumFields() - 1</code> .
     * @return A Class which closely represents the dbase field type.
     */
    @SuppressWarnings("rawtypes")
	public Class getFieldClass(int i) 
    {
        Class typeClass = null;
    
        switch (fields[i].fieldType) 
        {
            case 'C': 
                typeClass = String.class;
                break;
        
            case 'N': 
                if (fields[i].decimalCount == 0) 
                {
                    typeClass = Integer.class;
                }
                else
                {
                    typeClass = Double.class;
                }
                break;
        
            case 'F': 
                typeClass = Double.class;
                break;
        
            case 'L':
                typeClass = Boolean.class;
                break;
        
            case 'D':
                typeClass = Date.class;
                break;
        
            default:
                typeClass = String.class;
                break;
        }
    
        return typeClass;
    }
  
   /** Adds a column to this DbaseFileHeader. The type is one of (C N L or D) character, 
     * number, logical(true/false), or date. The Field length is the total length in bytes
     * reserved for this column. Note that this length applies to the representation in text
     * form, not binary. Therefore numbers with large numbers of digits require proportionately
     * more bytes than their floating point or double precision equivalent. The decimal count
     * only applies to numbers(N), and floating point values (F), and refers to the number of
     * characters to reserve after the decimal point.
     * <pre>
     * Field Type MaxLength
     * ---------- ---------
     * C          254
     * D          8
     * F          20
     * N          18
     * </pre>
     * @param inFieldName The name of the new field, must be less than 10 characters or it
     * gets truncated.
     * @param inFieldType A character representing the dBase field, (see above). Case insensitive.
     * @param inFieldLength The length of the field, in bytes at 1 byte per digit (see above).
     * @param inDecimalCount For numeric fields, the number of decimal places to track.
     */
    public void addColumn(String inFieldName, char inFieldType, int inFieldLength, int inDecimalCount)
    {
        if (inFieldLength <=0) 
        {
            System.err.println("Field length <= 0");
            return;
        }
        if (fields == null) 
        {
            fields = new DbaseField[0];
        }
        
        int tempLength = 1;  // Length is used for the offset and there is a * for deleted as the first byte.
        DbaseField[] tempFieldDescriptors = new DbaseField[fields.length+1];
        
        for (int i=0; i<fields.length; i++)
        {
            fields[i].fieldDataAddress = tempLength;
            tempLength = tempLength + fields[i].fieldLength;
            tempFieldDescriptors[i] = fields[i];
        }
        
        tempFieldDescriptors[fields.length] = new DbaseField();
        tempFieldDescriptors[fields.length].fieldLength = inFieldLength;
        tempFieldDescriptors[fields.length].decimalCount = inDecimalCount;
        tempFieldDescriptors[fields.length].fieldDataAddress = tempLength;
    
        // Set the field name.
        String tempFieldName = inFieldName;
        if (tempFieldName == null) 
        {
            tempFieldName = "NoName";
        }
    
        // Fix for GEOT-42, ArcExplorer will not handle field names > 10 chars
        if (tempFieldName.length() > 10) 
        {
            tempFieldName = tempFieldName.substring(0,10);
            warn("FieldName "+inFieldName+" is longer than 10 characters, truncating to ["+tempFieldName+"] when creating Dbase III header.");
        }
        
        // ADDED BY JWO: Repace any spaces with underscores.
        if (tempFieldName.lastIndexOf(" ") >=0)
        {
            tempFieldName = tempFieldName.replace(' ','_');
            warn("FieldName "+inFieldName+" contains spaces. Replacing with ["+tempFieldName+"] when creating Dbase III header.");
        }
  
        tempFieldDescriptors[fields.length].fieldName = tempFieldName;
    
        // The field type
        if ((inFieldType == 'C') || (inFieldType == 'c')) 
        {
            tempFieldDescriptors[fields.length].fieldType = 'C';
            if (inFieldLength > 254) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+" which is longer than 254, not consistent with dbase III");
            }
        }
        else if ((inFieldType == 'S') || (inFieldType == 's'))
        {
            tempFieldDescriptors[fields.length].fieldType = 'C';
            warn("Field type for "+inFieldName+" set to S; should be C (character).");
            if (inFieldLength >254) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+" which is longer than 254, not consistent with dbase III");
            } 
            tempFieldDescriptors[fields.length].fieldLength = 8;
        }
        else if ((inFieldType == 'D') || (inFieldType == 'd'))
        {
            tempFieldDescriptors[fields.length].fieldType = 'D';
            if (inFieldLength != 8) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+". Setting to 8 digits YYYYMMDD");
            }
            tempFieldDescriptors[fields.length].fieldLength = 8;
        }
        else if ((inFieldType == 'F') || (inFieldType == 'f'))
        {
            tempFieldDescriptors[fields.length].fieldType = 'F';
            if (inFieldLength > 20) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+". Preserving length, but should be set to maximum of 20 not valid for dbase IV, and UP specification, not present in dbaseIII.");
            }
        }
        else if ((inFieldType == 'N') || (inFieldType == 'n'))
        {
            tempFieldDescriptors[fields.length].fieldType = 'N';
            if (inFieldLength > 18) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+". Preserving length, but should be set to Max of 18 for dbase III specification.");
            }
            
            if (inDecimalCount < 0)
            {
                warn("Field decimal position for "+inFieldName+" set to "+inDecimalCount+". Setting to 0 (no decimal data will be saved).");
                tempFieldDescriptors[fields.length].decimalCount = 0;
            }
            
            if (inDecimalCount>inFieldLength-1)
            {
                warn("Field decimal position for "+inFieldName+" set to "+inDecimalCount+". Setting to "+(inFieldLength-1)+" (no non decimal data will be saved).");
                tempFieldDescriptors[fields.length].decimalCount = inFieldLength-1;
            }
        }
        else if ((inFieldType == 'L') || (inFieldType == 'l'))
        {
            tempFieldDescriptors[fields.length].fieldType = 'L';
            if (inFieldLength != 1) 
            {
                warn("Field length for "+inFieldName+" set to "+inFieldLength+". Setting to length of 1 for logical fields.");
            }
            tempFieldDescriptors[fields.length].fieldLength = 1;
        }
        else 
        {
            System.err.println("Undefined field type "+inFieldType + " for column "+inFieldName);
            return;
        }
        
        // The length of a record
        tempLength = tempLength + tempFieldDescriptors[fields.length].fieldLength;
    
        // Set the new fields.
        fields = tempFieldDescriptors;
        fieldCnt = fields.length;
        headerLength = MINIMUM_HEADER+32*fields.length;
        recordLength=tempLength;
    }
  
    /** Removes a column from this DbaseFileHeader.
      * @param inFieldName The name of the field, will ignore case and trim.
      * @return index of the removed column, -1 if not found.
      */
    public int removeColumn(String inFieldName) 
    {
        int retCol = -1;
        int tempLength = 1;
        DbaseField[] tempFieldDescriptors = new DbaseField[fields.length - 1];
        
        for (int i=0, j=0; i< fields.length; i++) 
        {
            if (!inFieldName.equalsIgnoreCase(fields[i].fieldName.trim())) 
            {
                // If this is the last field and we still haven't found the named field.
                if (i == j && i == fields.length - 1) 
                {
                    System.err.println("Could not find a field named '" +inFieldName + "' for removal");
                    return retCol;
                }
        
                tempFieldDescriptors[j] = fields[i];
                tempFieldDescriptors[j].fieldDataAddress = tempLength;
                tempLength += tempFieldDescriptors[j].fieldLength;
                
                // Only increment j on non-matching fields
                j++;
            }
            else 
            {
                retCol = i;
            }
        }
    
        // Set the new fields.
        fields = tempFieldDescriptors;
        headerLength = 33+32*fields.length;
        recordLength = tempLength;
    
        return retCol;
    }
 
    /** Reports the field length in bytes.
      * @param inIndex The field index.
      * @return The length in bytes.
      */  
    public int getFieldLength(int inIndex)
    {
        return fields[inIndex].fieldLength;
    }
  
    /** Reports the location of the decimal point within the field.
      * @param inIndex The field index.
      * @return The decimal count.
      */  
    public int getFieldDecimalCount(int inIndex)
    {
        return fields[inIndex].decimalCount;
    }
  
    /** Reports the name of the field at the given index.
      * @param inIndex The field index.
      * @return The name of the field.
      */  
    public String getFieldName(int inIndex)
    {
        return fields[inIndex].fieldName;
    }
  
    /** Reports the type of field at the given index.
      * @param inIndex The field index.
      * @return The dbase character representing this field.
      */  
    public char getFieldType(int inIndex)
    {
        return fields[inIndex].fieldType;
    }
  
    /** Reports the date this file was last updated.
      * @return The Date last modified.
      */
    public Date getLastUpdateDate()
    {
        return date;
    }
  
    /** Reports the number of fields in the records.
      * @return The number of fields in this table.
      */
    public int getNumFields()
    {
        return fields.length;
    }
  
    /** Reports the number of records in the file.
      * @return The number of records in this table.
      */
    public int getNumRecords()
    {
        return recordCnt;
    }
  
    /** Reports the length of the records in bytes.
      * @return The number of bytes per record.
      */
    public int getRecordLength()
    {
        return recordLength;
    }
  
    /** Reports the length of the header.
      * @return The length of the header in bytes.
      */
    public int getHeaderLength() 
    {
        return headerLength;
    }
  
    /** Reads the header data from the DBF file.
      * @param channel A readable byte channel. If you have an InputStream you need to use, you can
      * call <code>java.nio.Channels.getChannel(InputStream in)</code>.
      * @throws IOException If errors occur while reading.
      */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void readHeader(ReadableByteChannel channel) throws IOException
    {
        // Read in chunks of 1K
        ByteBuffer in = ByteBuffer.allocateDirect(1024);
    
        // ByteBuffers come preset to BIG_ENDIAN.
        in.order(ByteOrder.LITTLE_ENDIAN);
    
        // Only want to read first 10 bytes...
        in.limit(10);
    
        read(in,channel);
        in.position(0);
    
        // Type of file.
        byte magic = in.get();
        if (magic != MAGIC) 
        {
            throw new IOException("Unsupported DBF file Type "+Integer.toHexString(magic));
        }
    
        // Parse the update date information.
        int tempUpdateYear = in.get();
        int tempUpdateMonth = in.get();
        int tempUpdateDay = in.get();
    
        // ouch Y2K uncompliant
        if (tempUpdateYear > 90) 
        {
            tempUpdateYear = tempUpdateYear + 1900;
        }
        else 
        {
            tempUpdateYear = tempUpdateYear + 2000;
        }
        
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, tempUpdateYear);
        c.set(Calendar.MONTH, tempUpdateMonth-1);
        c.set(Calendar.DATE, tempUpdateDay);
        date = c.getTime();
    
        // Read the number of records.
        recordCnt = in.getInt();
    
        // Read the length of a record (unsigned little-endian shorts).
        // mask out the byte and or it with shifted 2nd byte
        headerLength = (in.get() & 0xff) | ((in.get() & 0xff) << 8);
    
        // If the header is bigger than our 1K, reallocate
        if (headerLength > in.capacity()) 
        {
            in = ByteBuffer.allocateDirect(headerLength - 10);
        }
        
        in.limit(headerLength - 10);
        in.position(0);
        read(in,channel);
        in.position(0);
    
        // Read the length of a record (unsigned little-endian shorts).
        recordLength = (in.get() & 0xff) | ((in.get() & 0xff) << 8);
    
        // Skip the reserved bytes in the header.
        in.position(in.position() + 20);
    
        // Calculate the number of fields in the header.
        fieldCnt = (headerLength - FILE_DESCRIPTOR_SIZE -1)/FILE_DESCRIPTOR_SIZE;
    
        // Read all of the header records.
        List lfields = new ArrayList();
        for (int i=0; i < fieldCnt; i++)
        {
            DbaseField field = new DbaseField();
      
            // Read the field name
            byte[] buffer = new byte[11];
            in.get(buffer);
            String name = new String(buffer);
            int nullPoint = name.indexOf(0);
            if(nullPoint != -1)
            {
                name = name.substring(0,nullPoint);
            }
            field.fieldName = name.trim();
      
            // Read the field type.
            field.fieldType = (char) in.get();
      
            // Read the field data address, offset from the start of the record.
            field.fieldDataAddress = in.getInt();
      
            // Read the field length in bytes.
            int length = in.get();
            if (length < 0) 
            {
                length = length + 256;
            }
            field.fieldLength = length;
      
            if (length > largestFieldSize) 
            {
                largestFieldSize = length;
            }
      
            // Read the field decimal count in bytes.
            field.decimalCount = in.get();
      
            // rreservedvededved bytes.
            //in.skipBytes(14);
            in.position(in.position() + 14);
    
            // Some broken shapefiles have 0-length attributes. The reference implementation
            // (ArcExplorer 2.0, built with MapObjects) just ignores them.
            if(field.fieldLength > 0) 
            {
                lfields.add(field);  
            }
        }
    
        // Last byte is a marker for the end of the field definitions.
        //in.skipBytes(1);
        in.position(in.position() + 1);
    
        fields = new DbaseField[lfields.size()];
        fields = (DbaseField[]) lfields.toArray(fields);
    }
  
    /** Reports the largest field size of this table.
      * @return The largest field size in bytes.
      */  
    public int getLargestFieldSize() 
    {
        return largestFieldSize;
    }
  
    /** Sets the number of records in the file.
      * @param inNumRecords The number of records.
      */
    public void setNumRecords(int inNumRecords)
    {
        recordCnt = inNumRecords;
    }
  
    /** Writes the header data to the DBF file.
      * @param out A channel to write to. If you have an OutputStream you can obtain the correct
      * channel by using java.nio.Channels.newChannel(OutputStream out).
      * @throws IOException If errors occur.
      */
    public void writeHeader(WritableByteChannel out) throws IOException 
    {
        // Take care of the annoying case where no records have been added
        if (headerLength == -1) 
        {
            headerLength = MINIMUM_HEADER;
        }
    
        ByteBuffer buffer = ByteBuffer.allocateDirect(headerLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    
        // Write out the output file type.
        buffer.put(MAGIC);
    
        // Write out the date.
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        buffer.put( (byte) (c.get(Calendar.YEAR) % 100));
        buffer.put( (byte) (c.get(Calendar.MONTH)+1));
        buffer.put( (byte) (c.get(Calendar.DAY_OF_MONTH)));
    
        // Write out the number of records in the datafile.
        buffer.putInt(recordCnt);
    
        // Write out the length of the header structure.
        buffer.putShort((short)headerLength);
    
        // Write out the length of a record
        buffer.putShort((short)recordLength);
    
        //    Write the reserved bytes in the header.
        //    for (int i=0; i<20; i++) out.writeByteLE(0);
        buffer.position(buffer.position() + 20);
    
        // Write out all of the header records.
        int tempOffset = 0;
        for (int i=0; i<fields.length; i++)
        {
            // Write out the field name.
            for (int j=0; j<11; j++)
            {
                if (fields[i].fieldName.length() > j)
                {
                    buffer.put((byte) fields[i].fieldName.charAt(j));
                }
                else
                {
                    buffer.put((byte)0);
                }
            }
      
            // Write out the field type.
            buffer.put((byte)fields[i].fieldType);
            
            // Write out the field data address, offset from the start of the record.
            buffer.putInt(tempOffset);
            tempOffset += fields[i].fieldLength;
      
            // Write out the length of the field.
            buffer.put((byte)fields[i].fieldLength);
      
            // Write out the decimal count.
            buffer.put((byte)fields[i].decimalCount);
      
            // Write out the reserved bytes.
            // for (in j=0; jj<14; j++) out.writeByteLE(0);
            buffer.position(buffer.position() + 14);
        }
    
        // Write out the end of the field definitions marker.
        buffer.put((byte)0x0D);

        buffer.position(0);
    
        int r = buffer.remaining();
        while ( (r-= out.write(buffer)) > 0) 
        {
            // do nothing
        }
    }
  
     /** Provides a simple representation of this header.
       * @return A String representing the state of the header.
       */  
    public String toString() 
    {
        StringBuffer fs = new StringBuffer();
        for (int i = 0, ii = fields.length; i < ii; i++) 
        {
            DbaseField f = fields[i];
            fs.append(f.fieldName + " " + f.fieldType + " " + f.fieldLength + " " + f.decimalCount + " " + f.fieldDataAddress + "\n");
        }
    
        return "DB3 Header\n"+"Date : " + date + "\n"+"Records : " + recordCnt + "\n"+"Fields : "+fieldCnt+"\n" +fs;   
    }
  
    // ---------------------- Private Methods ------------------------
   
    /** Reads the contents of the given buffer via the given channel.
      * @param buffer Buffer to read.
      * @param channel Channel via which to do the reading. 
      * @throws IOException If end of file has been reached when attempting to read.
      */
    private void read(ByteBuffer buffer,ReadableByteChannel channel) throws IOException 
    {
        while (buffer.remaining() > 0) 
        {
            if (channel.read(buffer) == -1) 
            {
                throw new EOFException("Premature end of file");
            }
        }
    }
    
    /** Logs a non-fatal warning message.
      * @param message Warning message to log.
      */
    private void warn(String message)
    {
        if  (logger == null)
            System.err.println("Warning: "+message);
        else
            logger.warning(message);
    }
  
    // --------------------------- Nested classes ------------------------------
  
    /** Class for holding the information assicated with a record.
      */
    private class DbaseField
    {          
		String fieldName;        // Field Name.
        char fieldType;          // Field Type (C N L D or M).
        int fieldDataAddress;    // Field Data Address offset from the start of the record.
        int fieldLength;         // Length of the data in bytes.
        int decimalCount;        // Field decimal count in Binary, indicating where the decimal is.
        
        public DbaseField() 
        {
			// Do nothing.
		}
    }
}