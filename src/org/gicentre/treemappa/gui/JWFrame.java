package org.gicentre.treemappa.gui;

import javax.swing.*;          // For Swing components
import java.awt.*;             // For AWT components.
import java.awt.event.*;       // For event handling.
import java.util.*;            // For collections.
import java.util.List;
import java.net.*;             // For icon handling.

//  ***************************************************************************************************
/** Creates a containing window with status, progress, menu and  toolbar options. If this window 
 *  contains any buttons on its toolbar, it must contain equivalent menu items. Menu items need not
 *  have equivalent toolbar buttons.
 *  <br /><br />
 *  Menus are aligned with space for iconic representation. If icons are used they should be all the 
 *  same size (16x16 recommended). Alignment is forced by inserting blank icons of the same size where
 *  necessary. This icon should be located at images/blank.gif in the classpath of the VM that uses 
 *  this class.
 *  @author Jo Wood, giCentre.
 *  @version 3.2.1, 23rd March, 2011.
 */
//  ***************************************************************************************************

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

public class JWFrame extends JFrame implements Runnable
{   
	// --------------------- Class and Object Variables --------------------

	private static final long serialVersionUID = -4210396644438569174L;

	private JMenuBar    menuBar;
	private JWStatusBar statusBar;
	private JToolBar    toolBar;
	List<JMenuItem> menuItems;
	List<AbstractButton>buttons;
	private ImageIcon   blankIcon;

	ImageIcon trueCheckIcon;

	ImageIcon falseCheckIcon;

	ImageIcon trueCheckIconG;

	ImageIcon falseCheckIconG;
	ImageIcon   trueRadioIcon;

	ImageIcon falseRadioIcon;

	ImageIcon trueRadioIconG;

	ImageIcon falseRadioIconG;   

	@SuppressWarnings("hiding")
	private static final int NORMAL   = 1;
	private static final int CHECKBOX = 2;

	// ----------------------- Constructors -----------------------

	/** Creates a top-level window with a given title. Assumes that an icon file 
	 * called Icon16.gif is located in he 'images' subdirectory. 
	 * @param title Title of window.
	 */
	public JWFrame(String title)
	{
		this(title,"Icon16.gif",false,false,false);
	}

	/** Creates a top-level window with various decoration options.
	 * @param title Title of window.
	 * @param iconName Name of file containing icon. File should be a .gif or .jpeg in the 'images' subdirectory.
	 * @param showMenu Menu will be displayed if true;
	 * @param showTools Toolbar will be displayed if true;
	 * @param showStatus Statusbar will be displayed if true;
	 */
	public JWFrame(String title, String iconName, boolean showMenu, boolean showTools, boolean showStatus)
	{
		// Create closable window with a title and icon.
		super(title);

		menuItems = new ArrayList<JMenuItem>();
		buttons   = new ArrayList<AbstractButton>();

		// Look for window icon.
		URL iconURL = searchFile(iconName);
		if (iconURL != null)
		{
			setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));
		}

		// Look for spacer and customised check box icons.
		iconURL = searchFile("blank.gif");
		if (iconURL != null)
		{
			blankIcon = new ImageIcon(iconURL);
		}
		else
		{
			blankIcon = null;
		}

		iconURL = searchFile("cbTrue16.gif");
		if (iconURL != null)
		{
			trueCheckIcon = new ImageIcon(iconURL);
		}
		else
		{
			trueCheckIcon = null;
		}

		iconURL = searchFile("cbTrue16G.gif");
		if (iconURL != null)
		{
			trueCheckIconG = new ImageIcon(iconURL);
		}
		else
		{
			trueCheckIconG = null;
		}

		iconURL = searchFile("cbFalse16.gif");
		if (iconURL != null)
		{
			falseCheckIcon = new ImageIcon(iconURL);
		}
		else
		{
			falseCheckIcon = null;  
		}

		iconURL = searchFile("cbFalse16G.gif");
		if (iconURL != null)
		{
			falseCheckIconG = new ImageIcon(iconURL);
		}
		else
		{
			falseCheckIconG = null;  
		}

		iconURL = searchFile("rbTrue16.gif");
		if (iconURL != null)
		{
			trueRadioIcon = new ImageIcon(iconURL);
		}
		else
		{
			trueRadioIcon = null;
		}

		iconURL = searchFile("rbTrue16G.gif");
		if (iconURL != null)
		{
			trueRadioIconG = new ImageIcon(iconURL);
		}
		else
		{
			trueRadioIconG = null;
		}

		iconURL = searchFile("rbFalse16.gif");
		if (iconURL != null)
		{
			falseRadioIcon = new ImageIcon(iconURL);
		}
		else
		{
			falseRadioIcon = null;  
		}

		iconURL = searchFile("rbFalse16G.gif");
		if (iconURL != null)
		{
			falseRadioIconG = new ImageIcon(iconURL);
		}
		else
		{
			falseRadioIconG = null;  
		}

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WinMonitor());
		Container contentPane = getContentPane();
		if (contentPane instanceof JComponent) 
		{
			((JComponent)contentPane).setMinimumSize(new Dimension(100, 100));
		}

		contentPane.setLayout(new BorderLayout());

		// Add menu, toolbar, status and progress components to window if requested.
		menuBar   = new JMenuBar();
		toolBar   = new JToolBar();

		// Ensure toolbar has reasonable spacing regardless of look and feel.
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT,0,1);
		toolBar.setLayout(layout);

		int minButtonWidth = layout.minimumLayoutSize(toolBar).width;
		layout.setHgap(17-minButtonWidth<0 ? 0:17-minButtonWidth);

		toolBar.setFloatable(false);
		toolBar.putClientProperty("isRolloverIconEffect", Boolean.TRUE);
		toolBar.setRollover(true);

		toolBar.setMargin(new Insets(0,0,0,0));
		statusBar = new JWStatusBar();

		if (showMenu)
		{
			setJMenuBar(menuBar);
		}

		if (showTools)
		{
			contentPane.add(toolBar,BorderLayout.NORTH);
		}

		if (showStatus)
		{
			contentPane.add(statusBar,BorderLayout.SOUTH);
		}         
	}

	// ------------------------ Methods --------------------------

	/** Adds a given menu without mnemonic to menu bar.
	 * @param name Name on menu to add. 
	 * @return Reference to the menu just created. This is useful for adding menu items.
	 */
	public JMenu addMenu(String name)
	{
		return addMenu(name,KeyEvent.CHAR_UNDEFINED);

	}

	/** Adds a given menu with mnemonic to menu bar.
	 * @param name Name on menu to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @return Reference to the menu just created. This is useful for adding menu items.
	 */
	public JMenu addMenu(String name, int mnemonic)
	{
		return addMenu(menuBar,name, mnemonic);
	}

	/** Adds a given menu with mnemonic to a component such as a menu bar or sub menu.
	 * @param parent Component onto which a menu is added.
	 * @param name Name on menu to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @return Reference to the menu just created. This is useful for adding menu items.
	 */
	public JMenu addMenu(JComponent parent, String name, int mnemonic)
	{
		JMenu menu = new JMenu(name);

		// Ensure sub menus are aligned with others that have icons.
		if (parent instanceof JMenu)
		{
			menu.setIcon(new ImageIcon(searchFile("blank.gif")));
		}

		if (mnemonic != KeyEvent.CHAR_UNDEFINED)
		{
			menu.setMnemonic(mnemonic);
		}
		parent.add(menu);
		return menu;
	}

	/** Adds a separator to the given menu. This should be called in sequence between
	 * adding other menus/menu items to the menu.
	 * @param menu Menu onto which a separator.
	 */
	public void addMenuSeparator(JMenu menu)
	{
		menu.addSeparator();
	}

	/** Adds a separator to the tool bar. This should be called in sequence between
	 * adding other items to the toolbar.
	 */
	public void addButtonSeparator()
	{
		toolBar.addSeparator();
	}

	/** Adds a given menu item to a given menu.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addActionItem(JMenuItem menu, String name, int mnemonic, ActionListener listener)
	{
		return addActionItem(NORMAL,menu,name,null,mnemonic,KeyEvent.CHAR_UNDEFINED,listener);
	}

	/** Adds given menu and button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addActionItem(JMenuItem menu, String name, String icon, int mnemonic, ActionListener listener)
	{
		return addActionItem(NORMAL,menu,name,icon,mnemonic,KeyEvent.CHAR_UNDEFINED,listener);
	}

	/** Adds a given menu and button item.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param accelerator Keyboard accelerator to use. Uses the system-dependent mask (e.g. CTRL on windows, Command on MacOS).
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addActionItem(JMenuItem menu, String name, String icon, int mnemonic, int accelerator, ActionListener listener)
	{
		return addActionItem(NORMAL,menu,name,icon,mnemonic,accelerator,listener);
	}

	/** Adds a given menu and button item.
	 * @param type Type of menu (NORMAL, CHECKBOX).
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param accelerator Keyboard accelerator to use. Uses the system-dependent mask (e.g. CTRL on windows, Command on MacOS).
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addActionItem(int type, JMenuItem menu, String name, String icon, int mnemonic, int accelerator, ActionListener listener)
	{
		return addActionItem(type,menu,name,icon,mnemonic,accelerator,null,listener);
	}

	/** Adds a given checkbox menu item to a given menu.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, int mnemonic, ActionListener listener)
	{
		return addBinActionItem(menu,name,false,null,mnemonic,null,listener);
	}

	/** Adds a given checkbox menu item to a given menu.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param isSelected Item will be selected if true.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, boolean isSelected, int mnemonic, ActionListener listener)
	{
		return addBinActionItem(menu,name,isSelected,null,mnemonic,null,listener);
	}

	/** Adds a given checkbox menu item to a given menu.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param bg Button group for grouped toggle buttons/checkboxes.
	 * @param listener Action listener to respond to menu selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, int mnemonic, ButtonGroup bg, ActionListener listener)
	{
		return addBinActionItem(menu,name,false,null,mnemonic,bg,listener);
	}

	/** Adds given checkbox menu and toggle button items. Assumes added item is not selected.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, String icon, int mnemonic, ActionListener listener)
	{
		return addBinActionItem(menu,name,false,icon,mnemonic,null,listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param isSelected Item will be selected if true.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, boolean isSelected, String icon, int mnemonic, ActionListener listener)
	{
		return addBinActionItem(menu,name,isSelected,icon,mnemonic,null,listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param bg Button group for grouped toggle buttons/checkboxes.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, String icon, int mnemonic, ButtonGroup bg, ActionListener listener)
	{
		return addBinActionItem(menu,name,false,icon,mnemonic,bg,listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param isSelected Item will be selected if true.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param bg Button group for grouped toggle buttons/checkboxes.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, boolean isSelected, int mnemonic, ButtonGroup bg, ActionListener listener)
	{
		return addBinActionItem(menu,name,isSelected,null,mnemonic,bg,listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param isSelected Item will be selected if true.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param bg Button group for grouped toggle buttons/checkboxes.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, boolean isSelected, String icon, int mnemonic, ButtonGroup bg, ActionListener listener)
	{
		return addBinActionItem(menu, name, isSelected, icon, mnemonic, KeyEvent.CHAR_UNDEFINED, bg, listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param accelerator Keyboard accelerator to use. Uses the system-dependent mask (e.g. CTRL on windows, Command on MacOS).
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, int mnemonic, int accelerator, ActionListener listener)
	{
		return addBinActionItem(menu, name, false, null, mnemonic, accelerator, null, listener);
	}

	/** Adds given checkbox menu and toggle button items.
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param isSelected Item will be selected if true.
	 * @param icon Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param accelerator Keyboard accelerator to use. Uses the system-dependent mask (e.g. CTRL on windows, Command on MacOS).
	 * @param bg Button group for grouped toggle buttons/checkboxes.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addBinActionItem(JMenuItem menu, String name, boolean isSelected, String icon, int mnemonic, int accelerator, ButtonGroup bg, ActionListener listener)
	{
		MyCheckBoxMenuItem binItem = (MyCheckBoxMenuItem)addActionItem(CHECKBOX,menu,name, icon,mnemonic,accelerator,bg,listener);
		if (isSelected)
		{
			binItem.doClick();
		}

		return binItem;
	}

	/** Adds a given menu and button item.
	 * @param type Type of menu (NORMAL, CHECKBOX).
	 * @param menu Menu onto which this menu item is added.
	 * @param name Name on the new menu item to add.
	 * @param iconName Name of file containing icon or null if no icon.
	 * @param mnemonic Keyboard mnemonic to use.
	 * @param accelerator Keyboard accelerator to use. Uses the system-dependent mask (e.g. CTRL on windows, Command on MacOS).
	 * @param bg Button group for linking items together.
	 * @param listener Action listener to respond to menu or button selection.
	 * @return Reference to the menu item just created. This is useful for adding sub-menus.
	 */
	public JMenuItem addActionItem(int type, JMenuItem menu, String name, String iconName, int mnemonic, int accelerator, ButtonGroup bg, ActionListener listener)
	{
		ImageIcon iGhost=null, iRollover=null;
		String icon = iconName;

		// Store lower case version without ellipsis.
		String actionName = name.toLowerCase();
		if (actionName.endsWith("..."))
		{
			actionName = actionName.substring(0,actionName.length()-3);
		}

		// Create the action.
		MyAction action = null;
		if (icon == null)
		{
			action = new MyAction(name,null,listener);
		}
		else 
		{
			URL iconURL = searchFile(icon);
			if (iconURL != null)
			{
				action = new MyAction(name,new ImageIcon(iconURL),listener);
			}
			else
			{
				icon = null;
				action = new MyAction(name,null,listener);
			}
		}    
		// Look for rollovers and ghosted versions of the icon.
		// Assumes rollover icon name is same as original with R appended.
		// Assumes ghost icon name is same as original with G appended.
		if (icon != null)
		{
			URL url = searchFile(getBody(icon)+"G."+getExtension(icon));
			if (url != null)
			{
				iGhost = new ImageIcon(url);
			}

			url = searchFile(getBody(icon)+"R."+getExtension(icon));
			if (url != null)
			{
				iRollover = new ImageIcon(url);
			}
		}

		// Create the menu version.
		JMenuItem menuItem = null;

		if (type == NORMAL)
		{
			menuItem = new JMenuItem(name);
		}
		else
		{
			menuItem = new MyCheckBoxMenuItem(name,bg);
		}

		menuItem.setAction(action);
		menuItem.setActionCommand(actionName);

		// Align text with items that have icons and customise checkbox icons.
		if ((icon == null) || (type == CHECKBOX))
		{
			if ((type == NORMAL) && (blankIcon != null))
			{
				menuItem.setIcon(blankIcon);
			}
			else
			{
				if (bg == null)
				{
					if (falseCheckIcon != null)
					{
						menuItem.setIcon(falseCheckIcon);
					}

					if (falseCheckIconG != null)
					{
						menuItem.setDisabledIcon(falseCheckIconG);
					}
				}
				else
				{
					if (falseRadioIcon != null)
					{
						menuItem.setIcon(falseRadioIcon);
					}

					if (falseRadioIconG != null)
					{
						menuItem.setDisabledIcon(falseRadioIconG);
					}
				}
			}
		}
		else
		{
			if (iGhost != null)
			{
				menuItem.setDisabledIcon(iGhost);
			}
			if (iRollover != null)
			{
				menuItem.setRolloverIcon(iRollover);
				menuItem.setRolloverEnabled(true);
			}
		}

		if (mnemonic != KeyEvent.CHAR_UNDEFINED)
		{
			menuItem.setMnemonic(mnemonic);
		}

		if (accelerator != KeyEvent.CHAR_UNDEFINED)
		{
			menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		menu.add(menuItem);
		menuItems.add(menuItem);

		// Create the button version.

		AbstractButton button;
		if (type==CHECKBOX)
		{
			if (bg == null)
			{
				button = new JToggleButton(name);
			}
			else
			{
				button = new JRadioButton(name);
			}
		}
		else
		{
			if (bg == null)
			{
				button = new JButton(name);
			}
			else
			{
				button = new JRadioButton(name);
			}
		}

		button.setRolloverEnabled(true);
		button.setFocusPainted(false);

		if (icon != null)
		{
			button.setAction(action);
			button.setText("");
			button.setActionCommand(actionName);
			button.setToolTipText(name);
			button.setBorderPainted(false);
			button.setMargin(new Insets(0,0,0,0));

			if (iGhost != null)
			{
				button.setDisabledIcon(iGhost);
			}
			if (iRollover != null)
			{
				button.setRolloverIcon(iRollover);
				button.setRolloverEnabled(true);
			}

			toolBar.add(button);
		}
		buttons.add(button);

		// Group checkbox itmes together in group if requested.
		if ((type == CHECKBOX) && (bg != null))
		{
			MyCheckBoxMenuItem myCb = (MyCheckBoxMenuItem)menuItem;   
			bg.add(myCb);
			myCb.setGroup(bg);
		}

		return menuItem;
	}

	/** Returns the action item (button or menu item) associated with the given name.
	 * @param name Name of action item to search for. Should be lower case without ellipsis.
	 * @return The first action item that has the given name, or null of nothing found.
	 *         Should be all lower case and without ellipsis.
	 */
	public Action getAction(String name)
	{
		for (JMenuItem menuItem : menuItems)
		{
			if (menuItem.getActionCommand().equals(name)) 
			{
				return menuItem.getAction();
			}
		}

		System.err.println("Can't find action ["+name+"]");
		for (JMenuItem menuItem : menuItems)
		{ 
			System.err.println("["+menuItem.getActionCommand()+"]");
		}
		return null;
	}

	/** Displays the given message in the status bar.
	 * @param text Message to display.
	 */
	public void setMessage(String text)
	{
		statusBar.setMessage(text);
	}

	/** Displays the minimum progress value.
	 * @param minProgress Minimum progress value.
	 */
	public void setMinProgress(int minProgress)
	{
		statusBar.setMinProgress(minProgress);
	}

	/** Displays the maximum progress value.
	 * @param maxProgress Maximum progress value.
	 */
	public void setMaxProgress(int maxProgress)
	{
		statusBar.setMaxProgress(maxProgress);
	}

	/** Sets the current progress (should be between 0 and 100 unless 
	 * minimum or maximum progress values have been changed.
	 * @param progress progress value.
	 */
	public void setProgress(int progress)
	{
		statusBar.setProgress(progress);
	}

	/** Shows or removes the progress bar from the frame.
	 * @param visible Progress bar will be visible if true;
	 */
	public void showProgress(boolean visible)
	{
		statusBar.showProgress(visible); 
	}

	/** Adds the given interruption listener to this window. The
	 * listener will be informed whenever the progress bar is clicked.
	 * @param interruptionListener Object to be informed of an 
	 *        interruption request.
	 */
	public void addInterruptionListener(JWInterruptionListener interruptionListener)
	{
		statusBar.addInterruptionListener(interruptionListener);
	}

	/** Removes the given interruption listener from this window. 
	 * @param interruptionListener Listener to be removed.
	 * @return True if listener was present and has been successfully removed.
	 */
	public boolean removeInterruptionListener(JWInterruptionListener interruptionListener)
	{
		return statusBar.removeInterruptionListener(interruptionListener);
	}

	/** Displays the frame as part of a threaded process. This can
	 * be used when called with EventQueue.invokeLater() to ensure
	 * that the window is invoked in a thread-safe manner. 
	 */
	public void run() 
	{
		pack();
		setVisible(true);
		initWindow();
	}

	/** Performs any window initialisation that is required after the
	 * frame has been displayed. Not usually required, but this can be
	 * overridden for subclasses that need to perform some dynamic display
	 * changes or need to capture the graphics context of this frame. 
	 */
	protected void initWindow()
	{
		// Do nothing.
	}

	// -------------------- Private Methods ----------------------

	/** Asks the user if they really want to quit, then closes.
	 */
	protected void closeDown()
	{
		int response = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to quit?",
				"Exit",JOptionPane.YES_NO_OPTION);                                        
		if (response == JOptionPane.YES_OPTION)
		{
			System.exit(0);    // Exit program.
		}
	}

	/** Looks for a given file in the classpath of the VM that uses this class.
	 * Will search in the 'images' sub-directory too. Useful for loading icon
	 * images. This method allows images in JAR files to be loaded in the same
	 * way as unarchived files.
	 * @param fileName Name of the file to search for (should be relative path).
	 * @return Fully specified URL of the fileName to search for.
	 */
	private URL searchFile(String fileName)
	{
		// Search for file in class path.
		URL url = getClass().getClassLoader().getResource(fileName);

		// Check the images sub-directory.
		if (url == null)
		{
			url = getClass().getClassLoader().getResource("images/"+fileName);
		}

		return url;
	}


	/** Extracts the name of a given filename without extension.
	 * @param filename Full file name.
	 * @return File name without extension.
	 */
	private String getBody(String filename)
	{
		int i = filename.lastIndexOf('.');

		if ((i>0) && (i<filename.length()-1))
		{
			return filename.substring(0,i);
		}

		return filename;
	}

	/** Extracts the extension of a given filename.
	 * @param filename Full file name.
	 * @return File name extension.
	 */
	private String getExtension(String filename)
	{
		int i = filename.lastIndexOf('.');

		if ((i>0) && (i<filename.length()-1))
		{
			return filename.substring(i+1);
		}
		return "";
	}

	// -------------------- Nested Classes -----------------------

	/** Monitors window closing events and performs a 'clean exit' 
	 * when requested.
	 */
	private class WinMonitor extends WindowAdapter
	{  
		public WinMonitor() 
		{
			super();
		}

		/** Responds to attempt to close window via the GUI. Checks
		 * the user really wants to quite before closing down.
		 * @param event Window closing event.
		 */
		public void windowClosing(WindowEvent event)
		{
			closeDown();
		}
	}

	/** Represents an action event that can be attached to a button or menu.
	 * Allows event handling to be delegated to any ActionListener.
	 */
	private class MyAction extends AbstractAction
	{  
		private static final long serialVersionUID = 1643832400778944423L;
		
		private ActionListener listener;    // Class to respond to action events.

		/** Creates the action with given name, icon and action listener.
		 * @param name Name to appear on menu and to be used for identification.
		 * @param icon Icon for button representation, or null if no button.
		 * @param listener The class that will respond to action events.
		 */
		public MyAction(String name, ImageIcon icon, ActionListener listener)
		{
			super(name,icon);
			this.listener = listener;
		}

		/** Delegates a response to an action event to the listener given in the constructor.
		 * @param e Action event to respond to.
		 */
		public void actionPerformed(ActionEvent e) 
		{
			// Intercept button presses and checkbox menu items and make sure corresponding
			// component is in a consistent state.
			if (e.getSource() instanceof JMenuItem)
			{
				if (e.getSource() instanceof MyCheckBoxMenuItem)
				{
					//System.out.println("Checkbox pressed.");
					MyCheckBoxMenuItem cb = (MyCheckBoxMenuItem)e.getSource();
					cb.setSelected(!cb.isSelected());    

					JToggleButton tb = (JToggleButton)buttons.get(menuItems.indexOf(cb));
					tb.setSelected(cb.isSelected());
				}

				listener.actionPerformed(e);
			}
			else
			{
				JMenuItem menuItem = menuItems.get(buttons.indexOf(e.getSource()));
				menuItem.doClick();
			}
		} 
	}

	/** Customised checkbox that uses a user-defined 16x16 icon instead of the L&F's
	 *  checkbox.
	 */ 
	private class MyCheckBoxMenuItem extends JMenuItem
	{
		// -------------- Object Variables ----------------

		private static final long serialVersionUID = 1L;
		private boolean selected;
		private ButtonGroup bg;

		// ---------------- Constructor -------------------

		/** Creates a checkbox menu item with the given name and possible group.
		 * If the button group is not null, checkboxes will be 'radio button' style. 
		 * @param name Text to associated with menu item.
		 * @param bg Optional button group for radio buttons, or null if normal checkboxes.
		 */ 
		public MyCheckBoxMenuItem(String name, ButtonGroup bg)
		{
			super(name);
			selected = false;
			this.bg = bg;
		}

		// ----------------- Methods ----------------------

		/** Reports the state of the checkbox.
		 * @return State of the checkbox item.
		 */          
		public boolean isSelected()
		{
			return selected;
		}

		/** Sets the state of the checkbox item.
		 *  @param state New state of the checkbox item.
		 */ 
		public void setSelected(boolean state)
		{        
			this.selected = state;

			if ((falseCheckIcon != null) && (trueCheckIcon != null) &&
					(falseRadioIcon != null) && (trueRadioIcon != null))
			{
				if (state)
				{
					if (bg == null)
					{
						setIcon(trueCheckIcon);
						if (trueCheckIconG != null)
						{
							setDisabledIcon(trueCheckIconG);
						}
					}
					else
					{
						setIcon(trueRadioIcon);
						if (trueRadioIconG != null)
						{
							setDisabledIcon(trueRadioIconG);
						}
					}

					// If part of a button group, set all others to false.
					if (bg != null)
					{
						Enumeration<AbstractButton> e = bg.getElements();

						while (e.hasMoreElements())
						{
							AbstractButton b = e.nextElement();
							if (b != this)
							{
								b.setSelected(false);
							}
						}
					}
				}   
				else
				{
					// Don't allow groups to contain all unchecked items.
					if ((bg != null))
					{
						Enumeration<AbstractButton> e = bg.getElements();
						boolean allowed = false;

						while (e.hasMoreElements())
						{
							AbstractButton b = e.nextElement();
							if (b.isSelected())
							{
								allowed = true;
								break;
							}
						}

						if (!allowed)
						{
							this.selected = true;
							return;
						}  

						setIcon(falseRadioIcon);
						if (falseRadioIconG != null)
						{
							setDisabledIcon(falseRadioIconG);
						}
					}
					else
					{
						setIcon(falseCheckIcon);
						if (falseCheckIconG != null)
						{
							setDisabledIcon(falseCheckIconG);
						}
					}
				}
			}
		}

		/** Sets the button group to be used by the checkbox.
		 * @param bg Button group to use.
		 */
		public void setGroup(ButtonGroup bg)
		{
			this.bg = bg;
		}
	}
}