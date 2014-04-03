package org.gicentre.treemappa.gui;

//********************************************************************************************
/** Interface for objects that need to be informed that a process has been interrupted
 *  by the user. This will usually be because of a click on the progress bar in a JWStatusBar. 
* @author Jo Wood, giCentre.
* @version 3.2.1, 23rd February, 2011.
*/
//********************************************************************************************

public interface JWInterruptionListener 
{
	/** Should respond to a request for an interruption to some process.
	 */
	public abstract void interruptionRequested();    
}