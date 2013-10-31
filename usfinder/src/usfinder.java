package usfinder;

import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.NumberFormatter;
import java.beans.*;
import java.net.URL;

import cds.tools.*;
import util.*;

import java.awt.geom.Ellipse2D;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.FontMetrics;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.util.prefs.Preferences;

public class usfinder extends JFrame implements VOApp,
    ActionListener{
        
    // Default Telescope data. See the class for a full description of the fields        
    private static final Telescope TELESCOPE_DATA = 
        new Telescope("NTT", new double[] {26.54, 28.35, 27.69, 27.55, 26.71}, 0.1055, false, 270.0, 5.8025, 4.431, 544.0);
        
    final Preferences _telPref = Preferences.userNodeForPackage(this.getClass());

    // The following are used to pass the telescope data around
    private Telescope _telescope = null, _old_telescope = null;
 
    // Colours
    public static final Color DEFAULT_COLOUR    = new Color(220, 220, 255);
    public static final Color SEPARATOR_BACK    = new Color(100, 100, 100);
    public static final Color SEPARATOR_FORE    = new Color(150, 150, 200);
    public static final Color LOG_COLOUR        = new Color(240, 230, 255);
    public static final Color ERROR_COLOUR      = new Color(255, 0,   0  );
    public static final Color WARNING_COLOUR    = new Color(255, 100, 0  );
    public static final Color GO_COLOUR         = new Color(0,   255, 0  );
    public static final Color STOP_COLOUR       = new Color(255, 0,   0  );
    
    // Font
    public static final Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 12);
    
    // Width for horizontal separator
    public static final int SEPARATOR_WIDTH = 5;
   
 
    private boolean _validStatus = true; 
    private int _filterIndex    = 1;
    private int _skyBrightIndex = 1;
 
    // Configuration file
    public String CONFIG_FILE = System.getProperty("CONFIG_FILE","usfinder.conf");
    
    // Exposure timer, active run timer, disk space display, checkRun
    // text fields for timing details
    // ActionListener that checks for run numbers
    private JTextField _frameRate        = new JTextField("", 7);
    private JTextField _cycleTime        = new JTextField("", 7);
    private JTextField _expTime        = new JTextField("", 7);
    private JTextField _dutyCycle        = new JTextField("", 7);
    private JTextField _exposureTime  = new JTextField("0", 7);

    // Configurable values
    public static String  TELESCOPE             = null;
    public static String   WINDOW_NAME          = new String("window");
    public static String[] TEMPLATE_LABEL       = {"1 or 2 windows"} ;
    public static String[] TEMPLATE_PAIR        = {"2"};
    public static String[] TEMPLATE_APP         = null;
    public static String[] TEMPLATE_ID          = null; 

    // Binning factors
    private int xbin    = 1;
    private int ybin    = 1;
    public static final int[] POSSIBLE_BIN_FACTORS = {1,2,4,8};
    private final IntegerTextField xbinText = new IntegerTextField(xbin, 1, 8, 1, "X bin factor", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    private final IntegerTextField ybinText = new IntegerTextField(ybin, 1, 8, 1, "Y bin factor", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    
    private static JComboBox templateChoice;
    public int numEnable;
    
    private String applicationTemplate    = new String("1 or 2 windows");
    private String oldApplicationTemplate = new String("1 or 2 windows");
    
    // Readout speeds
    private static final String[] SPEED_LABELS = {
	"Slow",
	"Medium",
	"Fast"
    };
    private JComboBox speedChoice = new JComboBox(SPEED_LABELS);
    private String readSpeed = "Slow";
    
    // Exposure delay measured in 1 millisecond intervals, so prompted
    private int expose = 500;
    private final IntegerTextField exposeText     = new IntegerTextField(0, 0, 10000000, 1, "exposure, milliseconds", true, DEFAULT_COLOUR, ERROR_COLOUR, 6);
    private int numExpose = -1;
    private final IntegerTextField numExposeText = new IntegerTextField(numExpose, -1, 100000, 1, "Number of exposures", true, DEFAULT_COLOUR, ERROR_COLOUR, 6);
    
    // Standard number of windows 
    private int numWin = 2;
    private IntegerTextField numWinText = new IntegerTextField(numWin, 1, 2, 1, "Number of windows", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);

    // High voltage gain
    private int hvGain = 0;
    private IntegerTextField hvGainText = new IntegerTextField(hvGain, 0, 9, 1, "Avalanche gain", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    private static final String[] CCD_OUTPUT_LABELS = {
	"Normal",
	"Avalanche"
    };
    private JComboBox ccdOutputChoice;
    private String ccdOutput = "Normal";
    
    // Fields for user information
    private static final JTextField _objectText     = new JTextField("", 15);
    private static final JTextField _filterText     = new JTextField("", 15);
    private static final JTextField _progidText     = new JTextField("", 15);
    private static final JTextField _piText         = new JTextField("", 15);
    private static final JTextField _observerText   = new JTextField("", 15);
    
    // Timing parameters from Vik (units of seconds, or seconds/pixel)
    public static final double VCLOCK = 9.12e-6; 
    public static final double HCLOCK = 0.48e-6;
    public static final double VIDEO_NORM_SLOW = 11.28e-6;
    public static final double VIDEO_NORM_MED  =  3.28e-6;
    public static final double VIDEO_NORM_FAST =  1.92e-6;
    public static final double VIDEO_AV_SLOW   = 11.76e-6;
    public static final double VIDEO_AV_MED    =  3.76e-6;
    public static final double VIDEO_AV_FAST   =  2.40e-6;
    public static final int    FFX             = 1072;
    public static final int    FFY             = 1072;
    public static final int    IFY             = 1072;
    public static final int    IFX             = 1072;
    public static final int    AVALANCHE_PIXELS= 1072;

    private JCheckBox clearEnabled = new JCheckBox();

    private static JButton syncWindows      = new JButton("Sync windows");

    private static JLabel windowsLabel = new JLabel("Windows");
    private static JLabel ystartLabel  = new JLabel("ystart");
    private static JLabel xstartLabel  = new JLabel("xstart");
    private static JLabel nxLabel      = new JLabel("nx");
    private static JLabel nyLabel      = new JLabel("ny");

    private static JLabel hvGainLabel  = new JLabel("Avalanche gain");

    private static SingleWindows _singleWindows;
    
    // Use this a fair bit, so just make one
    private static final GridBagLayout gbLayout = new GridBagLayout();
    
    // To switch between setup & observing panels
    JTabbedPane _actionPanel = null;
    JPanel _expertSetupPanel = null;
    JPanel _noddySetupPanel  = null;

    
    // Object for manipulating the Ultracam Field of View
    private static final FOVmanip FOV = new FOVmanip();
    
    // the surveys that this tool can query
    private JComboBox surveyChoice;
    public static String[] SURVEY_LABEL = {"DSS2-BLUE", "DSS2-RED", "DSS2-IR", "DSS1"};
    // string that stores the choice
    private String surveyString="DSS2-BLUE";
    
    private final JTextField objText = new JTextField("",10);
    private final JTextField coordText = new JTextField("",10);
    private static final JButton aladinGo = new JButton("Launch Aladin");
    private static final JButton addTarg = new JButton("Sel Targ");
    private static final JButton addComp = new JButton("Add Comp");
    
    int raHour=0, raMin=0, decDeg=0, decMin=0, paDeg=0;
    double raSec=0.0, decSec=0.0;
    IntegerTextField raHourVal = new IntegerTextField(raHour,  0,23,1,"RA hours",    true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    IntegerTextField raMinVal  = new IntegerTextField(raMin ,  0,59,1,"RA minutes",  true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    IntegerTextField decDegVal = new IntegerTextField(decDeg,-90,90,1,"Dec degrees", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    IntegerTextField decMinVal = new IntegerTextField(decMin,  0,59,1,"Dec minutes", true, DEFAULT_COLOUR, ERROR_COLOUR, 2);
    DoubleTextField  raSecVal  = new DoubleTextField(raSec,0.0,59.99,0.1,"RA seconds", true, DEFAULT_COLOUR, ERROR_COLOUR, 5); 
    DoubleTextField  decSecVal = new DoubleTextField(decSec,0.0,59.99,0.1,"Dec seconds", true, DEFAULT_COLOUR, ERROR_COLOUR, 5); 
    DoubleTextField  paDegVal = new DoubleTextField(paDeg,  0.0,359.9,0.3,"Position Angle",    true, DEFAULT_COLOUR, ERROR_COLOUR, 5);

    // handle action performed events
    public void actionPerformed(ActionEvent e){
    	FOVSync();
    }

    private  class aladinInstance implements Runnable {
    	public void run() {
    	    mw.startAladin();
    	}
    }

    private void telSync(){
        // now read in user set preferences if available
        boolean old_flipped = _telescope.flipped;
        _telescope.name       = _telPref.get("Name",     TELESCOPE_DATA.name);
        _telescope.plateScale = _telPref.getDouble("Plate Scale", TELESCOPE_DATA.plateScale);
        _telescope.flipped    = _telPref.getBoolean("Flip E-W", TELESCOPE_DATA.flipped);
        _telescope.delta_pa   = _telPref.getDouble("Delta PA", TELESCOPE_DATA.delta_pa);
        _telescope.delta_x    = _telPref.getDouble("Delta X",  TELESCOPE_DATA.delta_x);
        _telescope.delta_y    = _telPref.getDouble("Delta Y",  TELESCOPE_DATA.delta_y);
        _telescope.slit_x     = _telPref.getDouble("Slit X",   TELESCOPE_DATA.slit_x);
        boolean new_flipped = _telescope.flipped;
        if(old_flipped != new_flipped && aladin != null){
            aladin.execCommand("flipflop H");
        }
    }    
        
    private VOApp aladin = null;
    String aladinTarget=null;

    private static usfinder mw = null;

    public void FOVSync () {
	// update FOV in Aladin
    	FOV.configMainWin(_telescope);
	for(int i=0; i<numEnable; i++){
            FOV.addSingleWindow(_singleWindows, i, _telescope);
            FOV.configSingleWindow(_singleWindows, i, _telescope);
        }
        for(int i=numEnable; i<2; i++)
            FOV.delSingleWindow(i);				
	String raText=null, decText=null;
    	try {
            raText = raHourVal.getText() + ":" + raMinVal.getText() + ":" + String.valueOf(raSecVal.getValue());
            decText = decDegVal.getText() + ":" + decMinVal.getText() + ":" + decSecVal.getText();
            FOV.setCentre(raText, decText);
            FOV.setPA(String.valueOf(paDegVal.getValue()), _telescope);
            FOV.setSlit(_telescope);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }		
    	if (aladin != null){
            InputStream in;	
            in = FOV.getStream();		
            aladin.execCommand("rm 'USPEC_FoV'");
            aladin.putVOTable(mw,in,"USPEC_FoV");
    	}    	
    }
    
    /** This is the constructor which sets up the GUI, adds the panels etc
     * 
     *
     */    	
    public usfinder () {
	
    	try{
	    
	    // debugging panel to display aladin commands to usfinder
		/**
	    JFrame cmdFrame = new JFrame("Aladin Commands");
	    cmdFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    newPan = new JPanel(new BorderLayout());
	    displayArea = new JTextArea();
	    displayArea.setEditable(false);
	    JScrollPane scrollPane = new JScrollPane(displayArea);
	    scrollPane.setPreferredSize(new Dimension(375, 125));
	    newPan.add(scrollPane,BorderLayout.CENTER);
	    cmdFrame.setContentPane(newPan);
	    //Display the window.
	    cmdFrame.pack();
	    cmdFrame.setVisible(true);
	    **/

	    // Build GUI - set colours and fonts
	    UIManager.put("OptionPane.background",         DEFAULT_COLOUR);
	    UIManager.put("Panel.background",              DEFAULT_COLOUR);
	    UIManager.put("Button.background",             DEFAULT_COLOUR);
	    UIManager.put("CheckBoxMenuItem.background",   DEFAULT_COLOUR);
	    UIManager.put("SplitPane.background",          DEFAULT_COLOUR);
	    UIManager.put("Table.background",              DEFAULT_COLOUR);
	    UIManager.put("Menu.background",               DEFAULT_COLOUR);
	    UIManager.put("MenuItem.background",           DEFAULT_COLOUR);
	    UIManager.put("TextField.background",          DEFAULT_COLOUR);
	    UIManager.put("ComboBox.background",           DEFAULT_COLOUR);
	    UIManager.put("TabbedPane.background",         DEFAULT_COLOUR);
	    UIManager.put("TabbedPane.selected",           DEFAULT_COLOUR);
	    UIManager.put("MenuBar.background",            DEFAULT_COLOUR);
	    UIManager.put("window.background",             DEFAULT_COLOUR); 
	    UIManager.put("Slider.background",             DEFAULT_COLOUR);
	    UIManager.put("TextPane.background",           LOG_COLOUR);
	    UIManager.put("Tree.background",               LOG_COLOUR);
	    UIManager.put("RadioButtonMenuItem.background",DEFAULT_COLOUR);
	    UIManager.put("RadioButton.background",        DEFAULT_COLOUR);
	    UIManager.put("Table.font",                    DEFAULT_FONT);
	    UIManager.put("TabbedPane.font",               DEFAULT_FONT);
	    UIManager.put("OptionPane.font",               DEFAULT_FONT);
	    UIManager.put("Menu.font",                     DEFAULT_FONT);
	    UIManager.put("MenuItem.font",                 DEFAULT_FONT);
	    UIManager.put("Button.font",                   DEFAULT_FONT);
	    UIManager.put("ComboBox.font",                 DEFAULT_FONT);
	    UIManager.put("RadioButtonMenuItem.font",      DEFAULT_FONT);
	    UIManager.put("RadioButton.font",              DEFAULT_FONT);

	    loadConfig();
	
	    // This is a JFrame of sorts. Let's add titles etc
	    this.setTitle("UltraSpec finding chart and acquisition tool");
	    this.setSize(800,400);
	    	    
	    final Container container = this.getContentPane();
	    container.setBackground(DEFAULT_COLOUR);
	    container.setLayout(gbLayout);
	    
	    // Menu bar
	    final JMenuBar menubar = new JMenuBar();
	    this.setJMenuBar(menubar);	    

	    // File menu
	    menubar.add(createFileMenu());
	    menubar.add(createTelMenu());
	    
	    // Middle-left panel for displaying target and s-to-n information and timing
	    addComponent( container, createObjPanel(), 0, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	    
	    // Some horizontal space between the left- and right-hand panels
	    addComponent( container, Box.createHorizontalStrut(30), 1, 0,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    addComponent( container, Box.createHorizontalStrut(30), 1, 2,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    
	    // Top-right panel defines the window parameters
	    addComponent( container, createWindowPanel(), 2, 0,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    
	    // Update the colours while ensuring that paste operations remian disabled in numeric fields
	    updateGUI();
	    
	    this.pack();
	    this.setVisible(true);
	    
	    this.addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e){
	    		System.exit(0);
	    	}
	    }
	    );
	    
	    // Define timer to provide regular updating of timing information
	    // and to check whether windows are synchronised
	    // Task to perform
	    final ActionListener taskPerformer = new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
		    	speed();
			_singleWindows.setNwin(numEnable);
		    	if(_areSynchronised()){
		    		syncWindows.setEnabled(false);
		    		syncWindows.setBackground(DEFAULT_COLOUR);
		    	}else{
		    		syncWindows.setEnabled(true);
		    		syncWindows.setBackground(WARNING_COLOUR);
		    	}
		    }
	    };

	    // Checks once per 2 seconds
	    final Timer tinfoTimer = new Timer(1000, taskPerformer);	
	    tinfoTimer.start();
	
	    
	    final ActionListener aladinOn = new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
		    	if(aladin != null && aladinGo.isEnabled()){
		    		aladinGo.setEnabled(false);
		    	}
		    	if(aladin != null){
			    // aladin has been initialise
			    String status = aladin.execCommand("status");
			    if (status.length() < 1){
				// but is not currently useable
		    		addTarg.setEnabled(false); // disable target buttons
		    		addComp.setEnabled(false);
				if(!aladinGo.isEnabled()) aladinGo.setEnabled(true); // allow re-enabling 
			    }else{
				addTarg.setEnabled(true);
				addComp.setEnabled(true);
			    }
		    	}
		    }
	    };
	    // Checks once per 2 seconds
	    final Timer aladinTimer = new Timer(2000, aladinOn);	
	    aladinTimer.start();
	    
	}catch (final Exception e) {	    
	    e.printStackTrace();
	    System.out.println(e);
	    System.out.println("usfinder exiting.");
	    System.exit(0);
	}
    }
 
    /** Main program. Calls constructor and starts rtplot server */
    public static void main(final String[] args) {
    	mw = new usfinder();		
    }      
    
    	
    public void startAladin() {

    	// Instantiate Aladin
	System.out.println("Starting Aladin with target" + aladinTarget);
    	aladin = cds.aladin.Aladin.launch();    	
    	aladin.execCommand("sync");
    	String aladinSurvey=null;
    	if(surveyString.equals("DSS2-BLUE")) aladinSurvey = "Aladin(DSS2,J)";
    	if(surveyString.equals("DSS2-RED"))  aladinSurvey = "Aladin(DSS2,F)";
    	if(surveyString.equals("DSS2-IR"))  aladinSurvey = "Aladin(DSS2,I)";
    	if(surveyString.equals("DSS1"))      aladinSurvey = "Aladin(DSS1)";
        System.out.println("get "+ aladinSurvey+" " + aladinTarget);
    	aladin.execCommand("get "+aladinSurvey+" " + aladinTarget);

    	aladin.execCommand("sync");

    	// now we should check if it has loaded and if not we should exit.
    	String aladinStatus = aladin.execCommand("status");
    	final String[] statusarray = aladinStatus.split("\\n");
    	String Image=null;
    	boolean ImageLoaded=false;
    	for(int i=0; i < statusarray.length-3; i++){
    		//System.out.println(statusarray[i]);
    		if(statusarray[i].contains("PlaneID") && statusarray[i+2].contains("Image")){
    			ImageLoaded = true;
    			Image = statusarray[i].substring(8, statusarray[i].length());
    		}
    	}
    	
    	if( ! ImageLoaded) {
    		aladin.execCommand("quit");
    		aladin = null;
    		return;
    	}
    	
    	// load appropriate size field of view for this telescope
    	if(_telescope.name.equalsIgnoreCase("wht")){
    		aladin.execCommand("zoom 1x");
    	} else if (_telescope.name.equalsIgnoreCase("vlt")){
    		aladin.execCommand("zoom 2x");
    	} else if (_telescope.name.equalsIgnoreCase("ESO 3.6m")){
			aladin.execCommand("zoom 2x");
    	} else if (_telescope.name.equalsIgnoreCase("NTT")){
			aladin.execCommand("zoom 2x");
	}
    	if(_telescope.flipped) aladin.execCommand("flipflop H");
    		
    	/** We get the RA and DEC of our pointing from the Aladin status command. This is necessary to
    	 * correctly load the ultracam field of view
    	 */	
    	aladinStatus = aladin.execCommand("status");
    	int start = aladinStatus.indexOf("Centre");
    	start+=8;
    	int stop = start+11;
    	final String RA = aladinStatus.substring(start, stop);
    	start=stop+1;
    	stop = start+11;
    	final String DEC = aladinStatus.substring(start, stop);
    	String[] raSplit = RA.split(":");
    	raHourVal.setValue(Integer.parseInt(raSplit[0]));
    	raMinVal.setValue(Integer.parseInt(raSplit[1]));
    	raSecVal.setValue(Double.parseDouble(raSplit[2]));
    	String[] decSplit = DEC.split(":");
    	if(decSplit[0].startsWith("+")){
    		decDegVal.setValue(Integer.parseInt(decSplit[0].substring(1,3)));
    	} else {
    		decDegVal.setText(decSplit[0]);
    	}
    	decMinVal.setValue(Integer.parseInt(decSplit[1]));
    	decSecVal.setValue(Double.parseDouble(decSplit[2]));
    	
    	// load FOV
    	FOVSync();
    	
    	// run Sextractor to detect sources in Image and draw magnitude cirlces
    	// first run status to find the active image
    	aladin.execCommand("get Sextractor("+Image+",2,4,50000,1.2)");
    	aladin.execCommand("sync");
    	// rename the catalog to have an easy name
    	aladin.execCommand("set S-ex* PlaneID=SexCat");
    	aladin.execCommand("set SexCat Color=rgb(0,254,153)");
    	aladin.execCommand("filter SMag {draw circle(-$[phot.mag*])}");
    	aladin.execCommand("sync");
    	aladin.execCommand("filter SMag on"); 

    	// switch off annoying reticle
    	aladin.execCommand("reticle off");
    	
    	//aladin.addObserver(mw,1);
    }
	
    public boolean setTarget(){
    	aladin.execCommand("createplane");
    	// we have to check that this was succesfull, otherwise delete the spurious catalog
    	String messg = aladin.execCommand("status");
    	if (messg.contains("NbObj   0")){
    		aladin.execCommand("rm Select*");
    		return false;
    	}    	
    	aladin.execCommand("set Select* Color=red");
    	aladin.execCommand("set Select* PlaneID=Target");
    	aladin.execCommand("rm SMag");
    	aladin.execCommand("filter SMag {draw circle(-$[phot.mag*])}");
	aladin.execCommand("sync");
    	aladin.execCommand("filter SMag on"); 
    	return true;
    }
    
    public boolean addComparison(){
    	aladin.execCommand("createplane");
    	// we have to check that this was succesfull, otherwise delete the spurious catalog
    	String messg = aladin.execCommand("status");
    	if (messg.contains("NbObj   0")){
    		aladin.execCommand("rm Select*");
    		return false;
    	}    	
    	aladin.execCommand("set Select* PlaneID=tmp");
    	aladin.execCommand("select tmp Comp");
    	aladin.execCommand("createplane");
    	aladin.execCommand("rm Comp");
    	aladin.execCommand("rm tmp");
    	aladin.execCommand("set Select* PlaneID=Comp");
    	aladin.execCommand("set Comp* Color=blue");
    	aladin.execCommand("rm SMag");
    	aladin.execCommand("filter SMag {draw circle(-$[phot.mag*])}");
	aladin.execCommand("sync");
    	aladin.execCommand("filter SMag on"); 
    	return true;
    }

public void publishChart(){
	
	String tempdir = System.getProperty("java.io.tmpdir");
	if ( !(tempdir.endsWith("/") || tempdir.endsWith("\\")) )
	    tempdir = tempdir + System.getProperty("file.separator");
	
	if(aladin != null) aladin.execCommand("hide SexCat");
	if(aladin != null) aladin.execCommand("save "+tempdir+"tmp.bmp");
	if(aladin != null) aladin.execCommand("show SexCat");
	
	File file = new File(tempdir+"tmp.bmp");
	// load image
	BufferedImage img = null;
	try {
	    img = ImageIO.read(file);
	    
	    // Create a graphics context on the buffered image
	    Graphics2D g2d = img.createGraphics();
	    int width = img.getWidth(this);
	    
	    // Draw on the image
	    // Set up Font
	    int ypos = 20;
	    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    g2d.setPaint(Color.black);
	    Font font = new Font("Arial", Font.PLAIN, 16);
		g2d.setFont(font);
		FontMetrics fontMetrics = g2d.getFontMetrics();
	    int h = fontMetrics.getHeight();
	
		//Create an alpha composite of 50%  
		AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP);  
		g2d.setComposite(alpha);
	    
	    // Draw Object name, telescope pointing and window parameters
	    if(objText.getText().length() > 0) {
		String string = objText.getText();
		int w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos);
	    }else{
		// print coordinates
		String string = coordText.getText();
		int w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos);
	    }
	    // pointing parameters
	    String raText=null, decText=null, paText = null;
	    try {
		raText = raHourVal.getText() + ":" 
		    + raMinVal.getText() + ":" + String.valueOf(raSecVal.getValue());
		decText = decDegVal.getText() 
		    + ":" + decMinVal.getText() + ":" + decSecVal.getText();	    
		paText = String.valueOf(paDegVal.getValue());
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }		
	    if(raText.length() > 0){
		String string = "Tel RA: " + raText;
		int w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos+=h);
		
		string = "Tel Dec: " + decText;
		w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos+=h);
		
		string = "Tel PA: " + paText;
		w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos+=h);
	    }
	    ypos+=h/2;
	    if(numEnable >0){
		String string = "ystart  xstart   nx  ny";
		int w = fontMetrics.stringWidth(string);
		g2d.drawString(string,width-w-10,ypos+=h);
	    }
	    for(int i=0; i<numEnable; i++){
		try{
		    String string = ""+_singleWindows.getYstart(i)+ "   " +
			_singleWindows.getXstart(i)    + "  " + 
			_singleWindows.getNx(i)        + "  " +
			_singleWindows.getNy(i);
		    int w = fontMetrics.stringWidth(string);
		    g2d.drawString(string,width-w-10,ypos+=h);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    g2d.dispose();
	    
	    // allow use to output image
	    final JFileChooser fc = new JFileChooser();
	    FileFilterBMP ff = new FileFilterBMP();
	    fc.setFileFilter(ff);
	    int returnVal = fc.showSaveDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		File ofile = fc.getSelectedFile();
		ImageIO.write(img,"bmp",ofile);
	    }
	} catch (IOException e) {
	}
	// clean up temp file
	file.delete();
    }

    // You own implementation of VOApp methods for Aladin callbacks
    public void position(double ra, double dec){}
    public String putVOTable(final VOApp app, final InputStream in,final String label) { return null; }
    public String putVOTable(final InputStream in,final String label) { return null; }
    public InputStream getVOTable(final String dataID) { return null; }
    public String putFITS(final InputStream in,final String label) { return null; }
    public InputStream getFITS(final String dataID) { return null; }
    public void showVOTableObject(final String oid[]) {
    	System.out.print("I have to show:");
    	for( int i=0; i<oid.length; i++ ) System.out.print(" "+oid[i]);
    	System.out.println();
    }
    public void selectVOTableObject(final String oid[]) {
    	System.out.print("I have to select:");
    	for( int i=0; i<oid.length; i++ ) System.out.print(" "+oid[i]);
    	System.out.println();
    }
    public String execCommand(final String cmd) {
    	
	//displayArea.append(cmd + "\n");

    	// TO-DO: take cmd and parse it to get ra and dec numbers and roll value.
    	Pattern pattern= Pattern.compile("Target=(\\d*:\\d*:\\d*\\.\\d*) ([\\+-]\\d*:\\d*:\\d*\\.\\d*)");
    	Matcher matcher = pattern.matcher(cmd);
    	if(matcher.find()){
    			// get ra and dec
    	String DEC = matcher.group(2);
    	String RA = matcher.group(1);

    	String[] raSplit = RA.split(":");
    	raHourVal.setValue(Integer.parseInt(raSplit[0]));
    	raMinVal.setValue(Integer.parseInt(raSplit[1]));
    	raSecVal.setValue(Double.parseDouble(raSplit[2]));
    	String[] decSplit = DEC.split(":");
	//displayArea.append(decSplit[0] + "\n");
    	if(decSplit[0].startsWith("+")){
	    decDegVal.setValue(Integer.parseInt(decSplit[0].substring(1,3)));
    	} else {
	    //displayArea.append("negative dec " + decSplit[0] + "\n\n");
	    decDegVal.setText(decSplit[0]);
    	}
    	decMinVal.setValue(Integer.parseInt(decSplit[1]));
    	decSecVal.setValue(Double.parseDouble(decSplit[2]));
    	}
    	pattern=Pattern.compile("Roll=.*");
    	matcher = pattern.matcher(cmd);
    	if(matcher.find()){
    			// get roll
    		final String toSearch = matcher.group();
    		pattern=Pattern.compile("\\d+");
    		matcher=pattern.matcher(toSearch);
    		if(matcher.find()){
                double PAval = Double.parseDouble(matcher.group());
                PAval -= _telescope.delta_pa;
                if(PAval > 360.0) PAval-=360.0;
                if(PAval < 0.0) PAval += 360.0;
                paDegVal.setValue(PAval);
                
            }
    	}
    	return null; 
    	}
    public void addObserver(final VOObserver app,final int eventMasq) {}
     
    // Method for adding components to GridBagLayout for the window panel
    private static void addComponent (final Container cont, final Component comp, final int gridx, final int gridy, 
				      final int gridwidth, final int gridheight, final int fill, final int anchor){
	
	final GridBagConstraints gbc = new GridBagConstraints ();
	gbc.gridx      = gridx;
	gbc.gridy      = gridy;
	gbc.gridwidth  = gridwidth;
	gbc.gridheight = gridheight;
	gbc.fill       = fill;
	gbc.anchor     = anchor;
	gbLayout.setConstraints(comp, gbc);
	cont.add (comp);
    }
    
    /** Creates the panel which defines the object and telescope pointing parameters */
    public Component createObjPanel(){
    	int ypos=0; int xpos=0;
    	final JPanel _objPanel = new JPanel(gbLayout);
    	_objPanel.setBorder(new EmptyBorder(15,15,15,15));
	
	// Add some space before we get onto the pointing definitions
	addComponent( _objPanel, Box.createVerticalStrut(10), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// entry box for object name
    	final JLabel objLabel = new JLabel("Object Name  ");
    	addComponent(_objPanel,objLabel,0,ypos,1,1,
		     GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
	// define processes to run when object text is changed
    	objText.addActionListener(
				  new ActionListener(){
				      public void actionPerformed(final ActionEvent e){
					  if(objText.getText().length() > 0) {
					      aladinGo.setEnabled(true);
					  }else{
					      aladinGo.setEnabled(false);
					  }
				      }
				  }
				  );
    	addComponent(_objPanel,objText,1,ypos++,9,1,
		     GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	final JLabel coordLabel = new JLabel("or Coords ");
    	addComponent(_objPanel,coordLabel,0,ypos,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	coordText.addActionListener(
    			new ActionListener(){
    				public void actionPerformed(final ActionEvent e){
    				if(coordText.getText().length() > 0) {
    					aladinGo.setEnabled(true);
    				}else{
    					aladinGo.setEnabled(false);
    				}
    				}
    			}
    	);
    	addComponent(_objPanel,coordText,1,ypos++,9,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	final JLabel surveyLabel = new JLabel("Survey");
    	addComponent(_objPanel,surveyLabel,0,ypos,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	surveyChoice = new JComboBox(SURVEY_LABEL);
    	surveyChoice.setSelectedItem(surveyString);
    	surveyChoice.setMaximumRowCount(SURVEY_LABEL.length);
    	surveyChoice.addActionListener(
    			new ActionListener(){
    				public void actionPerformed(final ActionEvent e){
    					surveyString = (String) surveyChoice.getSelectedItem();
    					if(objText.getText().length() > 0 || 
    							coordText.getText().length() > 0) aladinGo.setEnabled(true);
    				}
    			}
    	);
    	
    	addComponent(_objPanel,surveyChoice,1,ypos++,9,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
   
    	
    	aladinGo.setEnabled(false);    		
    	aladinGo.addActionListener(
				   new ActionListener(){
				       public void actionPerformed(final ActionEvent e) {
					   if(coordText.getText().length() > 0){
					       aladinTarget = coordText.getText();
					       aladinGo.setEnabled(false);	

					       Thread t = new Thread(new aladinInstance()); 
					       t.start();
					   }else if(objText.getText().length() > 0){
					       aladinTarget = objText.getText();
					       aladinGo.setEnabled(false);	

					       Thread t = new Thread(new aladinInstance()); 
					       t.start();
					   }
				       }		
				   }
				   );		
    	addComponent( _objPanel, aladinGo, 1, ypos++, 9, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	// Add some space before we get onto the pointing definitions
    	addComponent( _objPanel, Box.createVerticalStrut(10), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
      	
    	final JPanel ra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    	final JPanel dec = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    	final JLabel raLabel = new JLabel("Tel. RA");
    	final JLabel decLabel = new JLabel("Tel. Dec");
    
    	raHourVal.addActionListener(this);
    	raMinVal.addActionListener(this);
    	raSecVal.addActionListener(this);
    	decDegVal.addActionListener(this);
    	decMinVal.addActionListener(this);
    	decSecVal.addActionListener(this);
      	paDegVal.addActionListener(this);
      	
    	ra.add(raHourVal);
    	ra.add(new JLabel(" : "));
    	ra.add(raMinVal);
    	ra.add(new JLabel(" : "));
    	ra.add(raSecVal);
    	dec.add(decDegVal);
    	dec.add(new JLabel(" : "));
    	dec.add(decMinVal);
    	dec.add(new JLabel(" : "));
    	dec.add(decSecVal);
    	addComponent(_objPanel,raLabel,0,ypos,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);    	
    	addComponent(_objPanel,ra,1,ypos++,10,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
       	addComponent(_objPanel,decLabel,0,ypos,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);    	
    	addComponent(_objPanel,dec,1,ypos++,10,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);

     	final JLabel paLabel = new JLabel("Tel. PA");
    	addComponent(_objPanel,paLabel,0,ypos,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);    	   
     	addComponent(_objPanel,paDegVal,1,ypos++,1,1,
    			GridBagConstraints.NONE, GridBagConstraints.WEST);
     	
	//      Add some space before we get onto the buttons to add targets
    	addComponent( _objPanel, Box.createVerticalStrut(10), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
      	  	
    	addTarg.setEnabled(false);    		
    	addTarg.addActionListener(
			new ActionListener(){
				public void actionPerformed(final ActionEvent e) {
						if (setTarget()) addTarg.setEnabled(false);
				}		
			}
    	);		
    	addComponent( _objPanel, addTarg, 1, ypos++, 9, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	
    	addComp.setEnabled(false);    		
    	addComp.addActionListener(
			new ActionListener(){
				public void actionPerformed(final ActionEvent e) {
						addComparison();
				}		
			}
    	);		
    	addComponent( _objPanel, addComp, 1, ypos++, 9, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	

	// Add some space before we get onto the timing info
    	addComponent( _objPanel, Box.createVerticalStrut(10), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// timing boxes
	addComponent( _objPanel, new JLabel("Frame rate (Hz)"), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	_frameRate.setEditable(false);
	addComponent( _objPanel, _frameRate, 1, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JLabel cycle = new JLabel("Cycle time (s)");
	cycle.setToolTipText("Time from start of one exposure to the start of the next");
	addComponent( _objPanel, cycle, 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	_cycleTime.setEditable(false);
	addComponent( _objPanel, _cycleTime, 1, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JLabel exp = new JLabel("Exposure time (s)");
	addComponent( _objPanel, exp, 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	_expTime.setEditable(false);
	addComponent( _objPanel, _expTime, 1, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JLabel duty = new JLabel("Duty cycle (%)");
	duty.setToolTipText("Percentage of time spent gathering photons");
	addComponent( _objPanel, duty, 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	_dutyCycle.setEditable(false);
	addComponent( _objPanel, _dutyCycle, 1, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

    	_objPanel.setBorder(new EmptyBorder(15,15,15,15));   	
    	return _objPanel;
    }
    
    /** Creates the panel which defines the window parameters */
    public Component createWindowPanel(){
	
	int ypos = 0;
	
	final JPanel _windowPanel     = new JPanel( gbLayout );
	_windowPanel.setBorder(new EmptyBorder(15,15,15,15));
	
		// Readout mode selection
	addComponent( _windowPanel,  new JLabel("CCD output"), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	ccdOutputChoice = new JComboBox(CCD_OUTPUT_LABELS);
	ccdOutputChoice.setSelectedItem(ccdOutput);
	ccdOutputChoice.addActionListener(
	    new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    ccdOutput = (String) ccdOutputChoice.getSelectedItem();
		    if(ccdOutputChoice.getSelectedItem().equals("Normal")){
			hvGainText.setEnabled(false);
			hvGainLabel.setEnabled(false);
		    }else{
			hvGainText.setEnabled(true);
			hvGainLabel.setEnabled(true);
		    }
		}
	    });
	addComponent( _windowPanel, ccdOutputChoice, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Avalanche gain setting
	addComponent( _windowPanel,  hvGainLabel, 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	addComponent( _windowPanel, hvGainText, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if(ccdOutputChoice.getSelectedItem().equals("Normal")){
	    hvGainText.setEnabled(false);
	    hvGainLabel.setEnabled(false);
	}else{
	    hvGainText.setEnabled(true);
	    hvGainLabel.setEnabled(true);
	}
	hvGainLabel.setToolTipText("High-voltage gain setting from no gain (0) to the highest (9)");

	// Readout speed selection
	addComponent( _windowPanel,  new JLabel("Readout speed"), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	speedChoice = new JComboBox(SPEED_LABELS);
	speedChoice.setSelectedItem(readSpeed);
	addComponent( _windowPanel, speedChoice, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// A little space
	addComponent( _windowPanel, Box.createVerticalStrut(5), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JLabel clearEnabledLabel = new JLabel("Clear enabled");
	clearEnabledLabel.setToolTipText("clear the CCD before each exposure or not");
	addComponent( _windowPanel,  clearEnabledLabel, 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	clearEnabled.setSelected(false);
	clearEnabled.setBackground(DEFAULT_COLOUR);
	addComponent( _windowPanel, clearEnabled, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// A little space
	addComponent( _windowPanel, Box.createVerticalStrut(5), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Exposure time
	addComponent( _windowPanel, new JLabel("Exposure delay (millisecs)     "), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JPanel exp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	exp.add(exposeText);
	addComponent( _windowPanel, exp, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// The binning factors
	xbinText.setAllowed(POSSIBLE_BIN_FACTORS);
	ybinText.setAllowed(POSSIBLE_BIN_FACTORS);
	addComponent( _windowPanel, new JLabel("Binning factors (X, Y)"), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	addComponent( _windowPanel, xbinText, 1, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	addComponent( _windowPanel, ybinText, 2, ypos++,  2, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Add some space before we get onto the window definitions
	addComponent( _windowPanel, Box.createVerticalStrut(10), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Ensure that binned windows match a standard phasing (designed so that there are no gaps
	// in the middle of the chip
	syncWindows.setEnabled(false);
	syncWindows.addActionListener(
			new ActionListener(){
				public void actionPerformed(final ActionEvent e) {
					if(_syncWindows()){
						syncWindows.setEnabled(false);						  
						syncWindows.setBackground(DEFAULT_COLOUR);
					}		
				}		
			}
	);		
	addComponent( _windowPanel, syncWindows, 1, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);

	// Number of windows for CCD 
	addComponent( _windowPanel, new JLabel("Number of windows"), 0, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	numWinText.addActionListener(
				      new ActionListener(){
					  public void actionPerformed(final ActionEvent e){
					      FOVSync();
					  }
				      }
				      );
	addComponent( _windowPanel, numWinText, 1, ypos++,  5, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Window definition lines
	setNumEnable();
	
	// First the labels for each column
	xstartLabel.setToolTipText("X value of first column of window");
	ystartLabel.setToolTipText("Y value of lowest row of window");
	nxLabel.setToolTipText("Number of unbinned pixels in X of window");
	nyLabel.setToolTipText("Number of unbinned pixels in Y of window");

	int xpos = 0;
	addComponent( _windowPanel, windowsLabel, xpos++, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);
	addComponent( _windowPanel, xstartLabel,  xpos++, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	addComponent( _windowPanel, ystartLabel,  xpos++, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	addComponent( _windowPanel, nxLabel,      xpos++, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	addComponent( _windowPanel, nyLabel,      xpos++, ypos,  1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	ypos++;
	
	// Then the row labels and fields for integer input
	_singleWindows = new SingleWindows(gbLayout, _windowPanel, ypos, xbin, ybin, DEFAULT_COLOUR, ERROR_COLOUR);
	_singleWindows.setNwin(numEnable);
	_singleWindows.addActionListener(
					 new ActionListener(){
					     public void actionPerformed(final ActionEvent e){
						 FOVSync();
					     }
					 }
					 );
	ypos += 2;

	// Add some space between window definitions and the user-defined stuff
	addComponent( _windowPanel, Box.createVerticalStrut(20), 0, ypos++,  1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Add a border
	_windowPanel.setBorder(new EmptyBorder(15,15,15,15));	
	return _windowPanel;
    }


/** Creates the "Telescope" menu entry **/
private JMenu createTelMenu() {
    final JMenu telMenu = new JMenu("Telescope");
    final JMenuItem _set = new JMenuItem("Set Telescope Parameters");
    _set.addActionListener(
			   new ActionListener(){
			       public void actionPerformed(ActionEvent e) {
                                   SwingUtilities.invokeLater(new Runnable() {
					   public void run() {
					       JFrame newframe = new JFrame("Telescope Parameters");
					       newframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                               
					       final JPanel newPanel = new JPanel(gbLayout);
					       newPanel.setBorder(new EmptyBorder(15,15,15,15));
                                               
					       final JTextField paField = new JTextField(15);
					       final JTextField dxField = new JTextField(15);
					       final JTextField dyField = new JTextField(15);
					       final JTextField sxField = new JTextField(15);
					       final JTextField psField = new JTextField(15);
                                               
					       final JRadioButton flipyes = new JRadioButton("Yes");
					       final JRadioButton flipno  = new JRadioButton("No");
					       if(_telPref.getBoolean("Flip E-W",TELESCOPE_DATA.flipped)){
						   flipyes.setSelected(true);
					       }else{
						   flipno.setSelected(true);
					       }
					       JLabel flipLabel = new JLabel("Flip E-W: ");
					       ButtonGroup group = new ButtonGroup();
					       group.add(flipyes);
					       group.add(flipno);
                                               
					       final JTextField nameField = new JTextField(_telPref.get("Name", _telescope.name),15);
					       JLabel nameLabel = new JLabel("Name: ");
                                               
					       ActionListener onEntry = new ActionListener () {
						       public void actionPerformed(ActionEvent e){
							   
							   if(e.getActionCommand().equals("close")){
							       newPanel.getTopLevelAncestor().setVisible(false);
							   }
							   if(e.getActionCommand().equals("name")){
							       _telPref.put("Name",nameField.getText());
							       telSync();
							   }
							   if(e.getActionCommand().equals("ps")){
							       _telPref.putDouble("Plate Scale",Double.parseDouble(psField.getText()));
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("flip")){
							       _telPref.putBoolean("Flip E-W",true);
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("noflip")){
							       _old_telescope = _telescope;
							       _telPref.putBoolean("Flip E-W",false);
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("pa")){
							       _telPref.putDouble("Delta PA",Double.parseDouble(paField.getText()));
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("dx")){
							       _telPref.putDouble("Delta X",Double.parseDouble(dxField.getText()));
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("dy")){
							       _telPref.putDouble("Delta Y",Double.parseDouble(dyField.getText()));
							       telSync();
							       FOVSync();                                                  
							   }
							   if(e.getActionCommand().equals("sx")){
							       _telPref.putDouble("Slit X",Double.parseDouble(sxField.getText()));
							       telSync();
							       FOVSync();                                                  
							   }
						       }
						   };        
					       
					       flipyes.setActionCommand("flip");
					       flipyes.addActionListener(onEntry);
					       flipno.setActionCommand("noflip");
					       flipno.addActionListener(onEntry);
                                               
					       psField.setText(round(_telPref.getDouble("Plate Scale", _telescope.plateScale),3));
					       psField.setToolTipText("Plate scale (in arcseconds)");
					       psField.setActionCommand("ps");
					       psField.addActionListener(onEntry);
					       JLabel psLabel = new JLabel("Plate Scale: ");
                                               
					       paField.setText(round( _telPref.getDouble("Delta PA", _telescope.delta_pa),2));
					       paField.setToolTipText("Rotator position in degrees when ultracam chip runs N-S");
					       paField.setActionCommand("pa");
					       paField.addActionListener(onEntry);
					       JLabel paLabel = new JLabel("Delta PA: ");
                                               
					       dxField.setText(round( _telPref.getDouble("Delta X", _telescope.delta_x),3 ));
					       dxField.setToolTipText("Error in alignment between chip centre and telescope pointing (arcsecs). Positive values imply that the telescope rotator centre is at x-values higher than 512.");
					       dxField.setActionCommand("dx");
					       dxField.addActionListener(onEntry);
					       JLabel dxLabel = new JLabel("Delta X: ");
					       
					       dyField.setText(round(_telPref.getDouble("Delta Y", _telescope.delta_y),3 ));
					       dyField.setToolTipText("Error in alignment between chip centre and telescope pointing (arcsecs). Positive values imply that the telescope rotator centre is at y-values lower than 512.");
					       dyField.setActionCommand("dy");
					       dyField.addActionListener(onEntry);
					       JLabel dyLabel = new JLabel("Delta Y: ");
					       
					       sxField.setText(round(_telPref.getDouble("Slit X", _telescope.slit_x),3));
					       sxField.setToolTipText("X Position of slit center (in pixels)");
					       sxField.setActionCommand("sx");
					       sxField.addActionListener(onEntry);
					       JLabel sxLabel = new JLabel("Slit X: ");
                                               
					       JButton closeButton = new JButton("Close");
					       closeButton.setActionCommand("close");
					       closeButton.addActionListener(onEntry);
                                               
					       int xpos=0; int ypos=0;
					       addComponent(newPanel, nameLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, nameField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, flipLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, flipyes, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, flipno, xpos, ypos++, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       xpos -= 2;
					       addComponent(newPanel, psLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, psField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, paLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, paField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, dxLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, dxField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, dyLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, dyField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, sxLabel, xpos++, ypos, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
					       addComponent(newPanel, sxField, xpos--, ypos++, 2, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
                                               
					       addComponent(newPanel, closeButton, xpos, ++ypos, 3, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER);
                                               
					       newframe.add(newPanel);
					       newframe.pack();
					       newframe.setVisible(true);
					   }
				       });
			       }
			   });
    telMenu.add(_set);
    
    return telMenu;
}

// Create the "File" menu entry
private JMenu createFileMenu() {
    
    final JMenu fileMenu = new JMenu("File");
    
    // Add actions to the "File" menu
    // Quit the program
    final JMenuItem _quit = new JMenuItem("Quit");
    _quit.addActionListener(
			    new ActionListener(){
				public void actionPerformed(final ActionEvent e){
				    System.exit(0);
				}
			    });
    final JMenuItem _publish = new JMenuItem("Publish...");
    _publish.addActionListener(new ActionListener(){
	    public void actionPerformed(final ActionEvent e){
		publishChart();
	    }
	});
    
    // Slide Control Button
    final JMenuItem _tweak = new JMenuItem("Tweak acq...");
    _tweak.addActionListener(
			     new ActionListener(){
				 public void actionPerformed(ActionEvent e) {
				     SwingUtilities.invokeLater(new Runnable() {
					     public void run() {
						 JFrame newframe = new JFrame("Tweak Acq");
						 newframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						 newframe.add(new Tweaker(_telescope));
						 newframe.pack();
						 newframe.setVisible(true);
					     }
					 });
				 }
			     });
    
    
    fileMenu.add(_publish);
    fileMenu.add(_tweak);
    fileMenu.add(_quit);
    
    return fileMenu;
}


    /** This routine implements's Vik's speed computations and reports
     *	the frame rate in Hertz, the cycle time (e.g. sampling time),
     * exposure time (time on source per cycle), the dead time and readout
     * time, all in seconds. Finally it also reports the duty cycle, and
     * in the case of drift mode, the number of windows in the storage area
     * along with the pipe shift in pixels.
     */
   public void speed() {

	try{

	    if(isValid(_validStatus)){
		

		// avalanche mode y/n?
		ccdOutput = (String) ccdOutputChoice.getSelectedItem();
		boolean lnormal = false;
		if(ccdOutput.equals("Avalanche")){
		    lnormal=false;
		}else if(ccdOutput.equals("Normal")){
		    lnormal=true;
		}else{
		    throw new Error("ccd mode = \"" + ccdOutput + "\" is unrecognised. Programming error");
		} 

		// Set the readout speed
		readSpeed = (String) speedChoice.getSelectedItem();

		double video;		    
		if(readSpeed.equals("Fast")){
		    if(lnormal){
			video = VIDEO_NORM_FAST;
		    }else{
			video = VIDEO_AV_FAST;
		    }
		}else if(readSpeed.equals("Medium")){
		    if(lnormal){
			video = VIDEO_NORM_MED;
		    }else{
			video = VIDEO_AV_MED;
		    }
		}else if(readSpeed.equals("Slow")){
		    if(lnormal){
			video = VIDEO_NORM_SLOW;
		    }else{
			video = VIDEO_AV_SLOW;
		    }
		}else{
		    throw new Error("readSpeed = \"" + readSpeed + "\" is unrecognised. Programming error");
		}

		// clear chip on/off?
		boolean lclear = false;
		if(clearEnabled.isSelected()){
		    lclear=true;
		}else {
		    lclear=false;
		} 

		// get exposure delay and binning factors
		expose = _getExpose();
		xbin         = xbinText.getValue();	
		ybin         = ybinText.getValue();	

		// window parameters
		boolean lwin2 = true;
		if(numEnable < 2) lwin2 = false;
		double x1start, x1size, y1start, y1size;
		double x2start=1, x2size=1, y2start=1, y2size=1;
		
		x1start = _singleWindows.getXstart(0);
		y1start = _singleWindows.getYstart(0);
		x1size  = _singleWindows.getNx(0);
		y1size  = _singleWindows.getNy(0);
		if(lwin2){
		    x2start = _singleWindows.getXstart(1);
		    y2start = _singleWindows.getYstart(1);
		    x2size  = _singleWindows.getNx(1);
		    y2size  = _singleWindows.getNy(1);
		}
		
		// convert timing parameters to seconds
		double expose_delay = expose*1.0e-3;


		// derek hardwires a delay of 1 ms to prevent system crashes
		expose_delay += 0.001;

		// clear chip by VCLOCK-ing the image and storage areas
		double clear_time;
		if(lclear){
		    clear_time = 2.0*FFY*VCLOCK;
		}else{
		    clear_time = 0.0;
		}


		/** move entire image into storage area
		    the -35 component is because Derek only shifts 1037 pixels
		    (composed of 1024 active rows, 5 dark reference rows, 2 transition rows
		    and 6 extra overscan rows for good measure) **/
		double frame_transfer = (FFY-35)*VCLOCK;


		// calculate the yshift, which places windows adjacent to the serial register
		double yshift_1 = (y1start-1.0)*VCLOCK;
		double yshift_2 = 0.0;
		if(lwin2)
		    yshift_2 = (y2start-y1start-y1size) *VCLOCK;
		
		
		// After placing the window adjacent to the serial register, Derek clears
		// the serial register to remove the charge dumped into it. Note that he
		// does FFX HCLOCKS in both normal and avalance readout mode.
		double line_clear1 = (y1size != IFY) ? FFX*HCLOCK : 0.0;
		double line_clear2 = lwin2 ? FFX*HCLOCK : 0.0;

		// calculate how long it takes to shift one row into the serial register
		// shift along serial register and then read out the data.
		int numhclocks = lnormal ? FFX : FFX + AVALANCHE_PIXELS;
		double line_read1 = (VCLOCK*ybin) + (numhclocks*HCLOCK) + (video*x1size/xbin);
		double line_read2 = lwin2 ? (VCLOCK*ybin) + (numhclocks*HCLOCK) + (video*x2size/xbin) : 0.0;
		
		// now multiply this by the number of rows to obtain total readout time
		double readout_1 = (y1size/ybin)*line_read1;
		double readout_2 = (y2size/ybin)*line_read2;

		// now get the total time to read out one exposure.
		double cycleTime, frameRate, expTime, deadTime, dutyCycle;
		cycleTime = expose_delay + clear_time + frame_transfer + yshift_1 + yshift_2 + 
		    line_clear1 + line_clear2 + line_read1 + line_read2 + readout_1 + readout_2;
		frameRate = 1.0/cycleTime;
		expTime = cycleTime - frame_transfer;
		deadTime = cycleTime - expTime;
		dutyCycle = 100.0*expTime/cycleTime;

		_frameRate.setText(round(frameRate,3));
		_cycleTime.setText(round(cycleTime,3));
		_dutyCycle.setText(round(dutyCycle,2));
		_expTime.setText(round(expTime,3));
	    }
	}
	catch(Exception e){
	    _frameRate.setText("UNDEFINED");
	    _cycleTime.setText("UNDEFINED");
	    _dutyCycle.setText("UNDEFINED");
	    _expTime.setText("UNDEFINED");
	}
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    // GUI update. It seems that updateComponentTree method re-enables the pastes on numeric fields where 
    // it was disabled. Thus we need to re-disable them all.
    public void updateGUI(){

	// Update colours
	SwingUtilities.updateComponentTreeUI(this);

	xbinText.setTransferHandler(null);
	ybinText.setTransferHandler(null);
	exposeText.setTransferHandler(null);
	numExposeText.setTransferHandler(null);
	hvGainText.setTransferHandler(null);
	numExposeText.setTransferHandler(null);
	numWinText.setTransferHandler(null);

	_singleWindows.disablePaste();
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    // Modifies window locations so that a full frame NxM binned window can
    // be used as a bias. Does so by ensuring no gap in the middle of the CCDs
    private boolean _syncWindows() {
	if(isValid(true)){
	    try {
		// 544 and 514 are based on the start pixel of 33,3 (+512) given by Derek
		for(int i=0; i<numEnable; i++){
		    _singleWindows.setXstartText(i, Integer.toString(_syncStart(_singleWindows.getXstart(i), xbin, 1,  1072, 544)) );
		    _singleWindows.setYstartText(i, Integer.toString(_syncStart(_singleWindows.getYstart(i), ybin, 1,  1072, 514)) );
		}
		for(int i=1; i<numEnable; i++){
		    _singleWindows.setXstartText(i, Integer.toString(_singleWindows.getXstart(0)));
		    _singleWindows.setNxText(i, Integer.toString(_singleWindows.getNx(0)));
		}
		return true;
	    }
	    catch(final Exception e){
		return false;
	    }
	}
	return true;
    }

    // Synchronises window so that the binned pixels end at ref and start at ref+1
    private int _syncStart(int start, int bin, int min, int max, int ref){
	int n = Math.round((float)((ref+1-start))/bin);
	start = ref + 1 - bin*n;
	if(start < min) start += bin;
	if(start > max) start -= bin;
	return start;
    }

    // Checks whether windows are synchronised
    private boolean _areSynchronised(){
	if(isValid(false)){
	    try{ 
		// Numbers here come from the 33,3 start pixel from Derek + 512
		for(int i=0; i<numEnable; i++){
		    if((545 - _singleWindows.getXstart(i)) % xbin != 0) return false;
		    if((515 - _singleWindows.getYstart(i)) % ybin != 0) return false;
		}
		
		for(int i=1; i<numEnable; i++){
		    if(_singleWindows.getXstart(i) != _singleWindows.getXstart(0)) return false;
		    if(_singleWindows.getNx(i)     != _singleWindows.getNx(0))     return false;
		}
		return true;
	    }
	    catch(Exception e){
		return false;
	    }
	}
	return true;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /** This class is for the display of the detailed timing information in 'speed' */
    class TableModel extends AbstractTableModel {
	
	private Object[][] data;

	public TableModel(final Object[][] data){
	    this.data = data;
	}
			
	public int getColumnCount() {
	    return data[0].length;
	}
		    
	public int getRowCount() {
	    return data.length;
	}

	public Object getValueAt(final int row, final int col) {
	    return data[row][col];
	}

    }

    // Converts a double to a string rounding to specified number of decimals
    public String round(final double f, final int ndp){
	final DecimalFormat form = new DecimalFormat();
	form.setMaximumFractionDigits(ndp);
	return form.format(f);
    }

    /** Returns the index of the current application. Should be done with a map
     * but this will have to do for now.
     */
    private int _whichTemplate(){
	int iapp = 0;
	for(iapp=0; iapp<TEMPLATE_LABEL.length; iapp++)
	    if(applicationTemplate.equals(TEMPLATE_LABEL[iapp])) break;
	if(iapp == TEMPLATE_LABEL.length){
	    System.out.println("Template = " + applicationTemplate + " not recognised.");
	    System.out.println("This is a programming or configuration file error and the program will terminate.");
	    System.exit(0);
	}
	return iapp;
    }


    //------------------------------------------------------------------------------------------------------------------------------------------
		
    /** Sets the number of window pairs in use */
    public void setNumEnable(){
	try{
	    numEnable = Math.min(numWin, Integer.parseInt(TEMPLATE_PAIR[_whichTemplate()]));
	}
	catch(final Exception e){
	    e.printStackTrace();
	    System.out.println("This is a programming or configuration file error and the program will terminate.");
	    System.exit(0);
	}
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    // Enables the labels for the windows/window pairs
    private void _setWinLabels(boolean enable){
	xstartLabel.setEnabled(enable);
	ystartLabel.setEnabled(enable);
	nxLabel.setEnabled(enable);
	nyLabel.setEnabled(enable);
    }    
    //------------------------------------------------------------------------------------------------------------------------------------------
	    
    /** Retrieves the values from the various fields and checks whether the currently 
     *  selected values represent a valid set of windows and sets. This should always
     *  be called by any routine that needs the most up-to-date values of the window parameters.
     */
    public boolean isValid(final boolean loud) {

	_validStatus = true;

	try{

	    xbin      = xbinText.getValue();	
	    ybin      = ybinText.getValue();	
	    expose    = _getExpose();
	    numExpose = numExposeText.getValue();
	    hvGain    = hvGainText.getValue();
	    numWin    = numWinText.getValue();
	    setNumEnable();
	    _validStatus = _singleWindows.isValid(xbin, ybin, numEnable, loud);
	    
	}
	catch(final Exception e){
	    _validStatus = false;
	}
	return _validStatus;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /** Get the exposure time from the two fields, one which gives the millsecond part, the other the 0.1 millsecond part
     * The application expect an integer number of 0.1 milliseconds
     */
    private int _getExpose() throws Exception {

	// Exposure specified in 0.1 milliseconds increments, but
	// this is too fine, so it is prompted for in terms of milliseconds.
	// This little program returns the value that must be sent to the servers

	expose  = exposeText.getValue();
	return expose;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------
    // Load the configuration file and set telescope information
    public void loadConfig() throws Exception {

	// Set the current telescope 
	// first set to hard coded defaults
	_telescope = TELESCOPE_DATA;
        
	// now read in user set preferences if available
	_telescope.name       = _telPref.get("Name",     TELESCOPE_DATA.name);
	_telescope.plateScale = _telPref.getDouble("Plate Scale", TELESCOPE_DATA.plateScale);
	_telescope.flipped    = _telPref.getBoolean("Flip E-W", TELESCOPE_DATA.flipped);
	_telescope.delta_pa   = _telPref.getDouble("Delta PA", TELESCOPE_DATA.delta_pa);
	_telescope.delta_x    = _telPref.getDouble("Delta X",  TELESCOPE_DATA.delta_x);
	_telescope.delta_y    = _telPref.getDouble("Delta Y",  TELESCOPE_DATA.delta_y);
	_telescope.slit_x     = _telPref.getDouble("Slit X",   TELESCOPE_DATA.slit_x);    
    }
    //------------------------------------------------------------------------------------------------------------------------------------------

    /** Splits up multiple arguments from configuration file */
    private String[] _loadSplitProperty(final Properties properties, final String key) throws Exception {
	final String propString = _loadProperty(properties, key);
	final StringTokenizer stringTokenizer = new StringTokenizer(propString, ";\n");
	final String[] multiString = new String[stringTokenizer.countTokens()];
	int i = 0;
	while(stringTokenizer.hasMoreTokens())
	    multiString[i++] = stringTokenizer.nextToken().trim();
	return multiString;
    }

    private String _loadProperty(final Properties properties, final String key) throws Exception {
	final String value = properties.getProperty(key);
	if(value == null)
	    throw new Exception("Could not find " + key + " in configration file " + CONFIG_FILE);
	return value;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /** Checks that a property has value YES or NO and returns true if yes. It throws an exception
     * if it neither yes nor no
     */
    private boolean _loadBooleanProperty(final Properties properties, final String key) throws Exception {
	final String value = properties.getProperty(key);
	if(value == null)
	    throw new Exception("Could not find " + key + " in configration file " + CONFIG_FILE);

	if(value.equalsIgnoreCase("YES") || value.equalsIgnoreCase("TRUE")){
	    return true;
	}else if(value.equalsIgnoreCase("NO") || value.equalsIgnoreCase("FALSE")){
	    return false;
	}else{
	    throw new Exception("Key " + key + " has value = " + value + " which does not match yes/no/true/false");
	}
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

}