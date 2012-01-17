package org.gicentre.treemappa;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

// ********************************************************************************************************
/** Provides a persistent store of all treeMappa configuration options such as layout, text colours etc.
 *  This can be instantiated at run time, saved to and loaded from a file and passed to a TreeMappa object.
 *  @author Jo Wood, giCentre.
 *  @version 3.1, 17th January, 2012.
 */
// ********************************************************************************************************

public class TreeMapProperties
{	
	// ------------------------------ Class and object variables ------------------------------

	private Properties properties;			// Properties object used for storing all configuration
	private String configFileName;			// Used to indicate these properties are to be saved to file.

	static final String ALIGN 				= "align";
	static final String ALLOW_VERTICAL 		= "allowVerticalLabels";
	static final String BORDER				= "border";
	static final String BORDER_COLOUR		= "borderColour";
	static final String BORDER_WEIGHT		= "borderWeight";
	static final String COLOUR_TABLE		= "cTable";
	static final String FILE_TYPE			= "type";
	static final String HEIGHT				= "height";
	static final String IMAGE_FILE 			= "imageFile";
	static final String IN_FILE 			= "inFile";
	static final String LABEL_BRANCHES		= "labelBranches";
	static final String LABEL_LEAVES		= "labelLeaves";
	static final String LAYOUT	 			= "layout";
	static final String LEAF_BORDER_COLOUR	= "leafBorderColour";
	static final String LEAF_BORDER_WEIGHT	= "leafBorderWeight";
	static final String LEAF_TEXT_COLOUR	= "leafTextColour";
	static final String LEAF_TEXT_FONT		= "leafTextFont";
	static final String LEAF_VECTOR_WIDTH	= "leafVectorWidth";
	static final String MAX_BRANCH_TEXT		= "maxBranchText";
	static final String MAX_LEAF_TEXT		= "maxLeafText";
	static final String MUTATION 			= "mutation";	
	static final String OUT_FILE 			= "outFile";
	static final String RAND_COLOUR_LEVEL	= "randColourLevel";
	static final String SAVE_CONFIG			= "saveConfig";
	static final String SEED	 			= "seed";
	static final String SHOW_ARROW_HEAD		= "showArrowHead";
	static final String SHOW_BRANCH_DISP	= "showBranchDisplacement";
	static final String SHOW_LEAF_BORDER	= "showLeafBorder";
	static final String SHOW_LEAF_DISP		= "showLeafDisplacement";
	static final String SHOW_STATISTICS		= "statistics";
	static final String SHOW_TREE_VIEW		= "showTreeView";
	static final String TEXT_COLOUR			= "textColour";
	static final String TEXT_FONT			= "textFont";
	static final String TEXT_ONLY			= "textOnly";
	static final String IS_TRANSPARENT 		= "transparent";
	static final String USE_LABELS			= "useLabels";
	static final String VECTOR_WIDTH		= "vectorWidth";
	static final String VERBOSE				= "verbose";
	static final String WIDTH				= "width";


	// ------------------------------------- Constructor --------------------------------------

	/** Creates a default set of treeMappa properties.
	 */
	public TreeMapProperties()
	{
		properties = new Properties();
		setDefaults();
	}

	// --------------------------------------- Methods ----------------------------------------

	/** Sets the given property with the given value. All values are treated as strings even
	 *  if they represent numeric or boolean parameters. Values are case-insensitive with the
	 *  exception of file names and text labels.
	 *  @param key Parameter whose value is to be set.
	 *  @param value Value of parameter.
	 *  @return True if the parameter and its value are valid. 
	 */
	public boolean setParameter(String key, String value)
	{
		if ((key.equalsIgnoreCase(IN_FILE)) || (key.equalsIgnoreCase(OUT_FILE)) || (key.equalsIgnoreCase(IMAGE_FILE)) || (key.equalsIgnoreCase(COLOUR_TABLE)))
		{
			if ((value == null) || (value.trim().length()==0))
			{
				System.err.println("'"+key+"' must be a valid file name");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if (key.equalsIgnoreCase(LEAF_TEXT_FONT))
		{
			if ((value == null) || (value.trim().length()==0))
			{
				System.err.println("'"+key+"' must be a valid font name");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if (key.equalsIgnoreCase(FILE_TYPE))
		{
			if ((value.equalsIgnoreCase("csv")) || (value.equalsIgnoreCase("csvcompact")) || (value.equalsIgnoreCase("csvspatial"))	||
					(value.equalsIgnoreCase("treeml")))
			{
				properties.setProperty(key.toLowerCase(), value);
			}
			else
			{
				System.err.println("Invalid option for '"+key+"' ("+value+"). Valid options are 'csv', 'csvCompact' 'csvSpatial' or 'treeML'.");
				return false;
			}
		}
		else if (key.toLowerCase().startsWith(LAYOUT.toLowerCase()))
		{
			String levelKey = checkLevel(LAYOUT,key);
			if (levelKey != null)
			{
				if ((value.equalsIgnoreCase("sliceanddice")) || (value.equalsIgnoreCase("squarified")) || 
						(value.equalsIgnoreCase("orderedsquarified")) || (value.equalsIgnoreCase("strip")) ||
						(value.equalsIgnoreCase("morton")) || (value.equalsIgnoreCase("spatial")) || (value.equalsIgnoreCase("spatialAv")) ||  
						(value.equalsIgnoreCase("pivotsize")) || (value.equalsIgnoreCase("pivotmiddle")) || 
						(value.equalsIgnoreCase("pivotsplit")) || (value.equalsIgnoreCase("pivotspace")))
				{
					properties.setProperty(key.toLowerCase(), value);
				}
				else
				{
					System.err.println("Invalid option for '"+key+"' ("+value+"). Valid options are 'sliceAndDice', 'squarified', 'orderedSquarified', 'spatial', 'strip', 'pivotSize', 'pivotMiddle', 'pivotSplit', 'pivotSpace' or 'morton'.");
					return false;
				}
			}
		}
		else if (key.toLowerCase().startsWith(ALIGN.toLowerCase()))
		{
			String levelKey = checkLevel(ALIGN,key);
			if (levelKey != null)
			{
				if ((value.equalsIgnoreCase("horizontal")) || (value.equalsIgnoreCase("vertical")) || 
						(value.equalsIgnoreCase("free")))
				{
					properties.setProperty(key.toLowerCase(), value);
				}
				else
				{
					System.err.println("Invalid option for '"+key+"' ("+value+"). Valid options are 'horizontal', 'vertical', 'free'.");
					return false;
				}
			}
		}
		else if ((key.equalsIgnoreCase(USE_LABELS)) || (key.equalsIgnoreCase(IS_TRANSPARENT)) ||
			 	 (key.equalsIgnoreCase(ALLOW_VERTICAL)) || 
				 (key.equalsIgnoreCase(SHOW_LEAF_BORDER)) ||
				 (key.equalsIgnoreCase(LABEL_LEAVES)) || (key.equalsIgnoreCase(LABEL_BRANCHES)) ||
				 (key.equalsIgnoreCase(SHOW_LEAF_DISP)) || (key.equalsIgnoreCase(SHOW_ARROW_HEAD)) ||
				 (key.equalsIgnoreCase(SHOW_STATISTICS)) || (key.equalsIgnoreCase(SHOW_TREE_VIEW)) ||
				 (key.equalsIgnoreCase(TEXT_ONLY)) || (key.equalsIgnoreCase(VERBOSE)))
		{
			if ((value.equalsIgnoreCase("true")) || (value.equalsIgnoreCase("false")))
			{
				properties.setProperty(key.toLowerCase(), value);
			}
			else
			{
				System.err.println("Invalid option for '"+key+"' ("+value+"). Valid options are 'true' or 'false'.");
				return false;
			}
		}
		else if (key.equalsIgnoreCase(SEED))
		{
			try
			{
				Long.parseLong(value);
			}
			catch (NumberFormatException e)
			{
				System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if (key.equalsIgnoreCase(RAND_COLOUR_LEVEL))
		{
			try
			{
				int level = Integer.parseInt(value);
				if (level < 0)
				{
					System.err.println("'"+key+"' must be at least 0, but "+level+" was given.");
					return false;
				}
			}
			catch (NumberFormatException e)
			{
				System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if (key.equalsIgnoreCase(MUTATION))
		{
			try
			{
				float mutation = Float.parseFloat(value);
				if ((mutation < 0) || (mutation >1))
				{
					System.err.println("'"+key+"' must be between 0 and 1, but "+mutation+" was given.");
					return false;
				}
			}
			catch (NumberFormatException e)
			{
				System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if ((key.equalsIgnoreCase(WIDTH)) || (key.equalsIgnoreCase(HEIGHT)) || 
				(key.equalsIgnoreCase(MAX_LEAF_TEXT)) || (key.equalsIgnoreCase(LEAF_VECTOR_WIDTH)) ||
				(key.equalsIgnoreCase(LEAF_BORDER_WEIGHT)))
		{
			try
			{
				double dimension = Double.parseDouble(value);
				if ((dimension < 0) && (!key.equalsIgnoreCase(LEAF_BORDER_WEIGHT)))
				{
					System.err.println("'"+key+"' must be at least 0, but "+dimension+" was given.");
					return false;
				}
			}
			catch (NumberFormatException e)
			{
				System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if ((key.toLowerCase().startsWith(BORDER.toLowerCase())) && (!key.toLowerCase().startsWith(BORDER_COLOUR.toLowerCase()))
																	  && (!key.toLowerCase().startsWith(BORDER_WEIGHT.toLowerCase())))
		{
			String levelKey = checkLevel(BORDER,key);
			if (levelKey != null)
			{
				try
				{
					double dimension = Double.parseDouble(value);
					if (dimension < 0)
					{
						System.err.println("'"+key+"' must be at least 0, but "+dimension+" was given.");
						return false;
					}
				}
				catch (NumberFormatException e)
				{
					System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if (key.toLowerCase().startsWith(BORDER_WEIGHT.toLowerCase()))	 
		{
			String levelKey = checkLevel(BORDER_WEIGHT,key);
			if (levelKey != null)
			{
				try
				{
					double dimension = Double.parseDouble(value);
					if (dimension < 0)
					{
						System.err.println("'"+key+"' must be at least 0, but "+dimension+" was given.");
						return false;
					}
				}
				catch (NumberFormatException e)
				{
					System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if ((key.equalsIgnoreCase(BORDER_COLOUR)) || (key.equalsIgnoreCase(LEAF_TEXT_COLOUR)) || (key.equalsIgnoreCase(LEAF_BORDER_COLOUR)))
		{
			if (getHexColour(value) == null)
			{
				System.err.println("Cannot extract colour value '"+value+"' from '"+key+"'.");
				return false;
			}
			properties.setProperty(key.toLowerCase(), value);
		}
		else if (key.toLowerCase().startsWith(MAX_BRANCH_TEXT.toLowerCase()))
		{
			String levelKey = checkLevel(MAX_BRANCH_TEXT,key);
			if (levelKey != null)
			{
				try
				{
					double dimension = Double.parseDouble(value);
					if (dimension < 0)
					{
						System.err.println("'"+key+"' must be at least 0, but "+dimension+" was given.");
						return false;
					}
				}
				catch (NumberFormatException e)
				{
					System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if (key.toLowerCase().startsWith(VECTOR_WIDTH.toLowerCase()))
		{
			String levelKey = checkLevel(VECTOR_WIDTH,key);
			if (levelKey != null)
			{
				try
				{
					double dimension = Double.parseDouble(value);
					if (dimension < 0)
					{
						System.err.println("'"+key+"' must be at least 0, but "+dimension+" was given.");
						return false;
					}
				}
				catch (NumberFormatException e)
				{
					System.err.println("Cannot extract numeric value '"+value+"' from  ' "+key+"'.");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if (key.toLowerCase().startsWith(TEXT_COLOUR.toLowerCase()))
		{
			String levelKey = checkLevel(TEXT_COLOUR,key);
			if (levelKey != null)
			{
				if (getHexColour(value) == null)
				{
					System.err.println("Cannot extract colour value '"+value+"' from '"+key+"'.");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if (key.toLowerCase().startsWith(TEXT_FONT.toLowerCase()))
		{
			String levelKey = checkLevel(TEXT_FONT,key);
			if (levelKey != null)
			{
				if ((value == null) || (value.trim().length()==0))
				{
					System.err.println("'"+key+"' must be a valid font name");
					return false;
				}
				properties.setProperty(key.toLowerCase(), value);
			}
		}
		else if (key.toLowerCase().startsWith(SHOW_BRANCH_DISP.toLowerCase()))
		{
			String levelKey = checkLevel(SHOW_BRANCH_DISP,key);
			if (levelKey != null)
			{
				if ((value.equalsIgnoreCase("true")) || (value.equalsIgnoreCase("false")))
				{
					properties.setProperty(key.toLowerCase(), value);
				}
				else
				{
					System.err.println("Invalid option for '"+key+"' ("+value+"). Valid options are 'true' or 'false'.");
					return false;
				}
			}
		}
		else if (key.equalsIgnoreCase(SAVE_CONFIG))
		{
			// Flag to indicate properties are to be saved is non-persistent, so do not store it
			// in the props object.
			if ((value == null) || (value.trim().length()==0))
			{
				System.err.println("'"+key+"' must be a valid file name");
				return false;
			}
			configFileName = value.trim();	
		}
		else
		{
			System.err.println("Ignoring unknown treeMappa parameter '"+key+"' with value '"+value+"'");
			return false;
		}

		return true;
	}

	/** Saves these treeMappa properties to an XML file with the given name.
	 *  @param fileName Name of XML file to store the treemappa configuration options.
	 *  @return True if properties saved without problems.
	 */
	public boolean save(String fileName)
	{
		/*
		String tmInFile = properties.getProperty(IN_FILE.toLowerCase());

		if ((tmInFile == null) || (tmInFile.trim().length()==0))
		{
			System.err.println("TreeMappa configuration must define the name of the treeMappa input file before it can be saved.");
			return false;
		}
		 */	
		try
		{
			properties.storeToXML(new FileOutputStream(fileName),"TreeMappa "+TreeMapApp.VERSION_TEXT+", "+TreeMapApp.DATE+" configuration.");
		}
		catch (IOException e)
		{
			System.err.println("Problem saving treemappa configuration file: "+e.getMessage());
			System.err.println("Full path of configuration file given as '"+new File(fileName).getAbsolutePath()+"'");
			return false;
		}
		catch (ClassCastException e)
		{
			System.err.println("Treemappa configuration contains illegal property: "+e.getMessage());
			return false;
		}
		return true;
	}

	/** Loads a set of treemappa configuration options from an XML file with the given name.
	 *  The options all have defaults so the configuration file need only contain non-default
	 *  options.
	 *  @param fileName Name of XML file from which to retrieve the treemappa configuration.
	 *  @return True if configuration file loaded without problems.
	 */
	public boolean load(String fileName)
	{
		try
		{
			return load(new FileInputStream(fileName));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Cannot find configuration file at '"+new File(fileName).getAbsolutePath()+"'");
			return false;
		}
	}
	
	/** Loads a set of treemappa configuration options from an XML file from the given
	 *  input stream.
	 *  The options all have defaults so the configuration file need only contain non-default
	 *  options.
	 *  @param inStream Input stream containing the XML file from which to retrieve the treemappa configuration.
	 *  @return True if configuration file loaded without problems.
	 */
	public boolean load(InputStream inStream)
	{
		Properties loadedProps = new Properties();
		try
		{
			loadedProps.loadFromXML(inStream);
		}
		catch (InvalidPropertiesFormatException e)
		{
			System.err.println("Problem interpreting properties XML file: "+e.getMessage());
			return false;
		}
		catch (IOException e)
		{
			System.err.println("Problem loading treemappa configuration file: "+e.getMessage());
			return false;
		}

		// Validate all properties and store them.
		properties.clear();
		setDefaults();
		for (Object key : loadedProps.keySet())
		{
			String value = (String)loadedProps.get(key);
			setParameter(((String)key).toLowerCase(), value);
		}
		return true;
	}

	/** Reports whether or not text only output is required.
	 *  @return True if only text output is to be used.
	 */
	public boolean getTextOnly()
	{
		return Boolean.parseBoolean(properties.getProperty(TEXT_ONLY.toLowerCase()));
	}

	/** Reports whether or not arrow heads are to be drawn on displacement vectors.
	 *  @return True if arrow heads are to be drawn on displacement vectors.
	 */
	public boolean getShowArrowHead()
	{
		return Boolean.parseBoolean(properties.getProperty(SHOW_ARROW_HEAD.toLowerCase()));
	}

	/** Reports whether or not leaf displacement vectors are to be drawn.
	 *  @return True if leaf displacement vectors are to be drawn.
	 */
	public boolean getShowLeafDisplacement()
	{
		return Boolean.parseBoolean(properties.getProperty(SHOW_LEAF_DISP.toLowerCase()));
	}

	/** Reports whether or not verbose reporting of progress is required.
	 *  @return True if verbose progress reports are requested.
	 */
	public boolean getIsVerbose()
	{
		return Boolean.parseBoolean(properties.getProperty(VERBOSE.toLowerCase()));
	}

	/** Reports whether or not treemap statistics are to be reported.
	 *  @return True if treemap statistics are to be reported.
	 */
	public boolean getShowStatistics()
	{
		return Boolean.parseBoolean(properties.getProperty(SHOW_STATISTICS.toLowerCase()));
	}

	/** Reports whether or not a collapsable tree view is to be shown.
	 *  @return True if a collapsable tree view is to be shown.
	 */
	public boolean getShowTreeView()
	{
		return Boolean.parseBoolean(properties.getProperty(SHOW_TREE_VIEW.toLowerCase()));
	}

	/** Reports whether or not leaves are to be labelled.
	 *  @return True if leaves are to have labels.
	 */
	public boolean getLabelLeaves()
	{
		return Boolean.parseBoolean(properties.getProperty(LABEL_LEAVES.toLowerCase()));
	}

	/** Reports whether or not branches are to be labelled.
	 *  @return True if branches are to have labels.
	 */
	public boolean getLabelBranches()
	{
		return Boolean.parseBoolean(properties.getProperty(LABEL_BRANCHES.toLowerCase()));
	}

	/** Reports whether or not labels can have vertical text.
	 *  @return True if labels can have vertical text when inside tall thin rectangles.
	 */
	public boolean getAllowVerticalLabels()
	{
		return Boolean.parseBoolean(properties.getProperty(ALLOW_VERTICAL.toLowerCase()));
	}

	/** Reports whether or not transparency should be used when constructing the treemap graphic.
	 *  @return True transparency is used in rendering graphics.
	 */
	public boolean getIsTransparent()
	{
		return Boolean.parseBoolean(properties.getProperty(IS_TRANSPARENT.toLowerCase()));
	}

	/** Reports whether or not labels are used to define the treemap hierarchy.
	 *  @return True if labels are used to define the treemap hierarchy.
	 */
	public boolean getUseLabels()
	{
		return Boolean.parseBoolean(properties.getProperty(USE_LABELS.toLowerCase()));
	}

	/** Provides the level at which random colour mutation should occur when using an evolutionary colour scheme.
	 *  @return Hierarchy level at which random colour mutation can occur.
	 */
	public int getRandColourLevel()
	{
		return Integer.parseInt(properties.getProperty(RAND_COLOUR_LEVEL.toLowerCase()));
	}

	/** Provides the random seed when using an evolutionary colour scheme. If the seed is not zero, the same
	 *  random colours will be used on subsequent renderings of the same treemap data. The particular colours
	 *  will be dependent on that seed value. If zero, a different random sequence is used each time.
	 *  @return Hierarchy level at which random colour mutation can occur.
	 */
	public long getSeed()
	{
		return Long.parseLong(properties.getProperty(SEED.toLowerCase()));
	}

	/** Provides the colour used to display leaf labels.
	 *  @return Colour used to display leaf labels.
	 */
	public Color getLeafTextColour()
	{
		return getHexColour(properties.getProperty(LEAF_TEXT_COLOUR.toLowerCase()));
	}
	
	/** Provides the colour used to display leaf borders.
	 *  @return Colour used to display leaf borders.
	 */
	public Color getLeafBorderColour()
	{
		return getHexColour(properties.getProperty(LEAF_BORDER_COLOUR.toLowerCase()));
	}
	
	/** Provides the line thickness used to display leaf borders.
	 *  @return Line thickness in pixels of leaf borders.
	 */
	public float getLeafBorderWeight()
	{
		return Float.parseFloat(properties.getProperty(LEAF_BORDER_WEIGHT.toLowerCase()));
	}

	/** Provides the name of the font used to display leaf labels.
	 *  @return Font used to display leaf labels.
	 */
	public String getLeafTextFont()
	{
		return properties.getProperty(LEAF_TEXT_FONT.toLowerCase());
	}
	
	/** Reports whether or not leaf nodes are shown with borders.
	 *  @return True if leaf nodes are to be shown with borders.
	 */
	public boolean getShowLeafBorder()
	{
		return Boolean.parseBoolean(properties.getProperty(SHOW_LEAF_BORDER.toLowerCase()));
	}

	/** Provides the colour used to display borders
	 *  @return Colour used to display borders.
	 */
	public Color getBorderColour()
	{
		return getHexColour(properties.getProperty(BORDER_COLOUR.toLowerCase()));
	}

	/** Provides the width of the treemap in pixels.
	 *  @return Width of the treemap in pixels.
	 */
	public double getWidth()
	{
		return Double.parseDouble(properties.getProperty(WIDTH.toLowerCase()));
	}

	/** Provides the height of the treemap in pixels.
	 *  @return Height of the treemap in pixels.
	 */
	public double getHeight()
	{
		return Double.parseDouble(properties.getProperty(HEIGHT.toLowerCase()));
	}

	/** Provides the width of leaf displacement vectors in pixels.
	 *  @return Width of the leaf displacement vectors in pixels.
	 */
	public float getLeafVectorWidth()
	{
		return Float.parseFloat(properties.getProperty(LEAF_VECTOR_WIDTH.toLowerCase()));
	}

	/** Provides the maximum leaf text size pixels.
	 *  @return Maximum leaf text size in pixels.
	 */
	public float getLeafMaxTextSize()
	{
		return Float.parseFloat(properties.getProperty(MAX_LEAF_TEXT.toLowerCase()));
	}

	/** Provides the mutation index that controls the degree of colour mutation when using evolutionary colour scheme.
	 *  @return Mutation index between 0 (inherits parent colour exactly) and 1 (independent of parent colour).
	 */
	public float getMutation()
	{
		return Float.parseFloat(properties.getProperty(MUTATION.toLowerCase()));
	}


	/** Provides the file type used for defining the hierarchy.
	 *  @return Type of file format used to represent hierarchy.
	 */
	public String getFileType()
	{
		return properties.getProperty(FILE_TYPE.toLowerCase());
	}


	/** Provides the name of the file used for defining the hierarchy.
	 *  @return Name of file used to represent hierarchy.
	 */
	public String getInFileName()
	{
		return properties.getProperty(IN_FILE.toLowerCase());
	}

	/** Provides the name of the file used for treemap coordinate output.
	 *  @return Name of file used for coordinates output or null if not defined.
	 */
	public String getOutFileName()
	{
		return properties.getProperty(OUT_FILE.toLowerCase());
	}

	/** Provides the name of the file used for image output.
	 *  @return Name of file used for image output or null if not defined.
	 */
	public String getImageFileName()
	{
		return properties.getProperty(IMAGE_FILE.toLowerCase());
	}

	/** Provides the name of the file used for the treemap colour table.
	 *  @return Name of file used for the treemap colour table or null if not defined.
	 */
	public String getCTableFileName()
	{
		return properties.getProperty(COLOUR_TABLE.toLowerCase());
	}

	/** Provides the name of the file used to save this configuration or null if not to be saved.
	 *  Note that unlike all other properties, this option is never itself saved to disk.
	 *  @return Name of file used for saving configuration options or null if not defined.
	 */
	public String getConfigFileName()
	{
		return configFileName;
	}

	/** Provides a set of ordered layout settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of layout settings.
	 */
	public String[] getLayouts()
	{
		String[] layouts = new String[TreeMapApp.MAX_DEPTH];
		buildParamArray(LAYOUT, layouts);		
		return layouts;
	}

	/** Provides a set of ordered alignment settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of alignment settings.
	 */
	public String[] getAlignments()
	{
		String[] alignments = new String[TreeMapApp.MAX_DEPTH];
		buildParamArray(ALIGN, alignments);		
		return alignments;
	}

	/** Provides a set of ordered border width settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of border width settings.
	 */
	public double[] getBorders()
	{
		double[] borders = new double[TreeMapApp.MAX_DEPTH];
		buildParamArray(BORDER, borders);		
		return borders;
	}
	
	/** Provides a set of ordered border weight settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of border weight settings.
	 */
	public float[] getBorderWeights()
	{
		float[] borderWeights = new float[TreeMapApp.MAX_DEPTH];
		buildParamArray(BORDER_WEIGHT, borderWeights);		
		return borderWeights;
	}

	/** Provides a set of ordered maximum branch text size settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of maximum branch text size settings.
	 */
	public float[] getBranchMaxTextSizes()
	{
		float[] maxBranchTexts = new float[TreeMapApp.MAX_DEPTH];
		buildParamArray(MAX_BRANCH_TEXT, maxBranchTexts);		
		return maxBranchTexts;
	}

	/** Provides a set of ordered branch text colour settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of branch text colour settings.
	 */
	public Color[] getBranchTextColours()
	{
		Color[] branchTextColours = new Color[TreeMapApp.MAX_DEPTH];
		buildParamArray(TEXT_COLOUR, branchTextColours);		
		return branchTextColours;
	}

	/** Provides a set of ordered branch text font names for each level of the treemap hierarchy.
	 *  @return Ordered collection of branch text font names.
	 */
	public String[] getBranchTextFonts()
	{
		String[] branchTextFonts = new String[TreeMapApp.MAX_DEPTH];
		buildParamArray(TEXT_FONT, branchTextFonts);		
		return branchTextFonts;
	}

	/** Provides a set of ordered vector width settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of vector width settings.
	 */
	public float[] getBranchVectorWidths()
	{
		float[] vectorWidths = new float[TreeMapApp.MAX_DEPTH];
		buildParamArray(VECTOR_WIDTH, vectorWidths);		
		return vectorWidths;
	}

	/** Provides a set of branch displacement display settings for each level of the treemap hierarchy.
	 *  @return Ordered collection of branch displacement display settings.
	 */
	public boolean[] getShowBranchDisplacements()
	{
		boolean[] branchDisplacements = new boolean[TreeMapApp.MAX_DEPTH];
		buildParamArray(SHOW_BRANCH_DISP, branchDisplacements);		
		return branchDisplacements;
	}

	// ----------------------------------- Private methods ------------------------------------

	/** Sets the default treemap properties.
	 */
	private void setDefaults()
	{
		properties.setProperty(ALIGN.toLowerCase(),"free");
		properties.setProperty(ALLOW_VERTICAL.toLowerCase(),"false");
		properties.setProperty(BORDER.toLowerCase(),"1");
		properties.setProperty(BORDER_WEIGHT.toLowerCase(),"-1");
		properties.setProperty(BORDER_COLOUR.toLowerCase(),"#000000");
		properties.setProperty(HEIGHT.toLowerCase(),"400");
		properties.setProperty(LABEL_BRANCHES.toLowerCase(),"false");
		properties.setProperty(LABEL_LEAVES.toLowerCase(),"true");
		properties.setProperty(LAYOUT.toLowerCase(),"orderedSquarified");
		properties.setProperty(SHOW_LEAF_BORDER.toLowerCase(),"false");
		properties.setProperty(LEAF_BORDER_COLOUR.toLowerCase(),"#000000");
		properties.setProperty(LEAF_BORDER_WEIGHT.toLowerCase(),"-1");
		properties.setProperty(LEAF_TEXT_COLOUR.toLowerCase(),"#00000096");
		properties.setProperty(LEAF_TEXT_FONT.toLowerCase(),"SansSerif");
		properties.setProperty(LEAF_VECTOR_WIDTH.toLowerCase(),"0.3");
		properties.setProperty(MAX_BRANCH_TEXT.toLowerCase(),"0");		
		properties.setProperty(MAX_LEAF_TEXT.toLowerCase(),"8");
		properties.setProperty(MUTATION.toLowerCase(),"0.2");
		properties.setProperty(RAND_COLOUR_LEVEL.toLowerCase(),"1");
		properties.setProperty(SEED.toLowerCase(),"0");		
		properties.setProperty(SHOW_ARROW_HEAD.toLowerCase(),"false");
		properties.setProperty(SHOW_BRANCH_DISP.toLowerCase(),"false");
		properties.setProperty(SHOW_LEAF_DISP.toLowerCase(),"false");
		properties.setProperty(SHOW_STATISTICS.toLowerCase(),"false");
		properties.setProperty(SHOW_TREE_VIEW.toLowerCase(),"false");
		properties.setProperty(TEXT_FONT.toLowerCase(),"SanSerif");
		properties.setProperty(TEXT_ONLY.toLowerCase(),"false");
		properties.setProperty(TEXT_COLOUR.toLowerCase(),"#00000064");
		properties.setProperty(IS_TRANSPARENT.toLowerCase(),"true");
		properties.setProperty(FILE_TYPE.toLowerCase(),"csv");
		properties.setProperty(USE_LABELS.toLowerCase(),"true");
		properties.setProperty(VECTOR_WIDTH.toLowerCase(),"0.3");
		properties.setProperty(VERBOSE.toLowerCase(),"false");
		properties.setProperty(WIDTH.toLowerCase(),"400");
	}

	/** Checks to see that either a valid level is given with a multi-level parameter or no level is given.
	 *  Will display errors if invalid level provided. The returned string (if valid) will guarantee a 
	 *  simplified version of the level parameter (e.g. with no decimals, 00s etc.).
	 *  @param base Name of parameter
	 *  @param full Full parameter text including possible level modifier.
	 *  @return Validated level parameter or null if problem extracting level.
	 */
	private String checkLevel(String base, String full)
	{
		if (full.equalsIgnoreCase(base))
		{
			return full;
		}

		if (full.length() <= base.length())
		{
			System.err.println("Cannot extract "+base+" level from '"+full+"'");
			return null;
		}

		try
		{
			int level = Integer.parseInt(full.substring(base.length()));
			if ((level < 0) || (level > TreeMapApp.MAX_DEPTH))
			{
				System.err.println(base+" level '"+level+"' is invalid. Must be between 0 and "+TreeMapApp.MAX_DEPTH);
				return null;
			}
			return base+level;
		}
		catch (NumberFormatException e)
		{
			System.err.println("Unknown suffix attached to '"+full+"'. Should be a number indicating hierarchy level.");
			return null;
		}
	}

	/** Returns a Color object representing the given hex colour string.
	 *  @param hexColour Hex representation of colour in RGBA order. A is optional.
	 *  @return Color if the hexColour is a valid representation or null if it is not.
	 */
	private static Color getHexColour(String hexColour)
	{
		String alpha = "#ff";
		String cleanHexColour;

		if (hexColour.startsWith("#")) 
		{
			boolean hasAlpha = hexColour.length() ==9;  // We have an alpha channel
			if (hasAlpha) 
			{
				alpha = new String("#"+hexColour.substring(7, 9));
				cleanHexColour = hexColour.substring(0,7);
			}
			else
			{
				cleanHexColour = hexColour;
			}

			try 
			{
				int rgba = Integer.decode(cleanHexColour).intValue();	
				int a = Integer.decode(alpha).intValue();    	    	
				rgba = rgba | (a << 24);
				return new Color(rgba, hasAlpha);
			} 
			catch (NumberFormatException e) 
			{
				return null;
			}
		}
		return null;
	}

	/** Provides a set of ordered text settings for each level of the treemap hierarchy.
	 */
	private void buildParamArray(String key, String[] array)
	{
		String def = properties.getProperty(key.toLowerCase());

		for (int i=0; i<array.length; i++)
		{
			String value = properties.getProperty(key.toLowerCase()+i);
			if (value == null)
			{
				array[i] = def;
			}
			else
			{
				array[i] = value;
			}
		}
	}

	/** Provides a set of ordered double precision settings for each level of the treemap hierarchy.
	 */
	private void buildParamArray(String key, double[] array)
	{
		double def = Double.parseDouble(properties.getProperty(key.toLowerCase()));

		for (int i=0; i<array.length; i++)
		{
			String value = properties.getProperty(key.toLowerCase()+i);
			if (value == null)
			{
				array[i] = def;
			}
			else
			{
				array[i] = Double.parseDouble(value);
			}
		}
	}

	/** Provides a set of ordered floating point settings for each level of the treemap hierarchy.
	 */
	private void buildParamArray(String key, float[] array)
	{
		float def = Float.parseFloat(properties.getProperty(key.toLowerCase()));

		for (int i=0; i<array.length; i++)
		{
			String value = properties.getProperty(key.toLowerCase()+i);
			if (value == null)
			{
				array[i] = def;
			}
			else
			{
				array[i] = Float.parseFloat(value);
			}
		}
	}

	/** Provides a set of ordered boolean settings for each level of the treemap hierarchy.
	 */
	private void buildParamArray(String key, boolean[] array)
	{
		boolean def = Boolean.parseBoolean(properties.getProperty(key.toLowerCase()));

		for (int i=0; i<array.length; i++)
		{
			String value = properties.getProperty(key.toLowerCase()+i);
			if (value == null)
			{
				array[i] = def;
			}
			else
			{
				array[i] = Boolean.parseBoolean(value);
			}
		}
	}

	/** Provides a set of ordered colour settings for each level of the treemap hierarchy.
	 */
	private void buildParamArray (String key, Color[] array)
	{
		Color def = getHexColour(properties.getProperty(key.toLowerCase()));		
		for (int i=0; i<array.length; i++)
		{
			String value = properties.getProperty(key.toLowerCase()+i);
			if (value == null)
			{
				array[i] = def;
			}
			else
			{
				array[i] = getHexColour(value);
			}
		}
	}
}
