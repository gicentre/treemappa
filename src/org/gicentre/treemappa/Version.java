package org.gicentre.treemappa;

//***************************************************************************************************
/** Describes the version number and date of the treemappa library.
 *  @author Jo Wood, giCentre.
 *  @version 3.1.1, 7th July 2012.
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

public class Version 
{
    private static final float  VERSION = 3.1f;
    private static final String VERSION_TEXT = "treeMappa V3.1.1, 7th July, 2012";

    /** Reports the current version of the treeMappa library.
      * @return Text describing the current version of this library.
      */
    public static String getText()
    {
        return VERSION_TEXT;
    }
    
    /** Reports the numeric version of the treeMappa library.
     *  @return Number representing the current version of this library.
     */
    public static float getVersion()
    {
        return VERSION;
    }
}