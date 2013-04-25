package org.gicentre.treemappa.gui;

import javax.swing.*;           // For Swing components.
import javax.swing.border.*;    // For component borders.
import java.awt.*;              // For AWT classes.
import java.awt.event.*;        // For mouse listeners.
import java.util.*;             // For collections.
import java.util.List;

//  ***********************************************************************************************
/** Creates status bar for displaying messages and progress. 
  * @author Jo Wood
  * @version 3.2, 23rd February, 2011.
  */
//  ***********************************************************************************************

class JWStatusBar extends JPanel
{
	// ----------------- Object variables -------------------
	private static final long serialVersionUID = -1734032338071879276L;
	
    private JLabel message;    // Message to display in status bar.
    private JProgressBar progressBar;
    private Font font;
    private List<JWInterruptionListener> interruptionListeners;
    
    // ------------------ Constructors ----------------------
      
    /** Creates a status bar for displaying messages and progress.
      */
    public JWStatusBar()
    {
        this("Status:");
    }

    /** Creates a status bar for displaying messages and progress.
      * @param text Initial text to display in status bar.
      */
    public JWStatusBar(String text)
    {
        interruptionListeners = new Vector<JWInterruptionListener>();
        setLayout(new BorderLayout());   
        font = getFont().deriveFont(getFont().getSize()*.8f);
        
        // Text area.
        message = new JLabel(text,SwingConstants.LEFT);
        message.setBorder(BorderFactory.createCompoundBorder(createBorder(),BorderFactory.createEmptyBorder(1,4,1,1))); 
        message.setFont(font);
        message.setText(text);
        message.setToolTipText("Status");
        add(message,BorderLayout.CENTER);
        
        // Progress bar.
        progressBar = new JProgressBar();
        progressBar.setFont(font);
        progressBar.setStringPainted(true);
        progressBar.setBorder(createBorder());
        Dimension progSize = progressBar.getPreferredSize();
        progSize.width = 80;
        progressBar.setPreferredSize(progSize);
        progressBar.setToolTipText("Progress");
        progressBar.addMouseListener(new MouseClickMonitor());
        add(progressBar,BorderLayout.EAST);    
    }
        
    // --------------------- Methods -------------------------
        
    /** Displays the given message in the status bar.
      * @param text Message to display.
      */
    public void setMessage(String text)
    {
        message.setText(text);
    }
        
    /** Displays the minimum progress value.
      * @param minProgress Minimum progress value.
      */
    public void setMinProgress(int minProgress)
    {
        progressBar.setMinimum(minProgress);
    }
        
    /** Displays the maximum progress value.
      * @param maxProgress Maximum progress value.
      */
    public void setMaxProgress(int maxProgress)
    {
        progressBar.setMaximum(maxProgress);
    }
        
    /** Sets the current progress (should be between 0 and 100 unless 
      * minimum or maximum progress values have been changed. If set to
      * Integer.MAX_VALUE, bar is disabled.
      * @param progress progress value.
      */
    public void setProgress(int progress)
    {
        if (progress == Integer.MAX_VALUE)
        {
            progressBar.setValue(progressBar.getMinimum());
            progressBar.setStringPainted(false);
        }
        else
        {
            progressBar.setStringPainted(true);
            progressBar.setValue(progress);
        }
    }
    
    /** Shows or removes the progress bar.
      * @param visible Progress bar will be visible if true;
      */
    public void showProgress(boolean visible)
    {
        if (visible)
        {
        	add(progressBar,BorderLayout.EAST); 
        }
        else
        {
        	remove(progressBar);
        }
    }

    /** Adds the given interruption listener to this status bar. The
      * listener will be informed whenever the progress bar is clicked.
      * @param interruptionListener Object to be informed of an 
      *        interruption request.
      */
    public void addInterruptionListener(JWInterruptionListener interruptionListener)
    {
        interruptionListeners.add(interruptionListener);
    }
    
    /** Removes the given interruption listener from this status bar. 
      * @param interruptionListener Listener to be removed.
      * @return True if listener was present and has been successfully removed.
      */
    public boolean removeInterruptionListener(JWInterruptionListener interruptionListener)
    {
        return interruptionListeners.remove(interruptionListener);
    }
    
    // ------------------- Private Methods ----------------------
    
    /** Creates a border to place around message area and progress bar.
      * @return Border to place around components.
      */
    private static Border createBorder()
    { 
        return BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    }
    
    /** Reports the listeners that need to be informed of an interruption.
     *  @return Interruption listeners that need to be informed of an interruption.
     */
    List<JWInterruptionListener> getInterruptionListeners()
    {
    	return interruptionListeners;
    }
    // -------------------- Nested Classes ----------------------
     
    /** Handles mouse clicks on the progress bar and will send a message
      * to any interruption listeners that have been added to this status
      * bar. This is used to allow users to stop long processes by clicking
      * on the progress bar.
      */
    class MouseClickMonitor extends MouseAdapter
    {
        /** Handles a mouse click on the progress bar, by informing any 
          * interruption listeners.  
          * @param e Mouse event associated with the click.
          */
        public void mousePressed(MouseEvent e)
        {
            for (JWInterruptionListener listener : getInterruptionListeners())
            {
                try
                {
                    listener.interruptionRequested();
                }
                catch (ConcurrentModificationException modEx)
                {
                    // If listening object is being modifed by another thread, 
                    // don't pass on interruption request.
                    System.err.println("Process completed before interruption could be requested.");
                    break;
                }
            }
        }
    }
}