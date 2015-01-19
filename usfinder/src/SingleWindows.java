package usfinder;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodListener;

import util.*;

/** Class to handle individual windows appropriate to ULTRASPEC. 
 * Makes a GUI with all the necessary values prompted, has checks on 
 * the values.
 */

public class SingleWindows {

    // Number of windows to handle 
    private static final int NWIN = 4;

    // Initial values
    private static final int[] XSTART  = {200, 200, 200, 200};
    private static final int[] YSTART  = {101, 401, 601, 801};
    private static final int[] NX      = {500, 500, 500, 500};
    private static final int[] NY      = { 50,  50,  50,  50};

    private JLabel[]           winLabel    = new JLabel[NWIN];
    private IntegerTextField[] xstartText  = new IntegerTextField[NWIN];
    private IntegerTextField[] ystartText  = new IntegerTextField[NWIN];
    private IntegerTextField[] nxText      = new IntegerTextField[NWIN];
    private IntegerTextField[] nyText      = new IntegerTextField[NWIN];

    /** Main constructor 
     * @param gbLayout    layout to use
     * @param panel       panel to contain the window parameters
     * @param ypos        the yposition to add in the components
     * @param xbin        the X binning factor, needed to increment NX properly
     * @param ybin        the Y binning factor, needed to increment NY properly
     * @param backColour  background colour
     * @param errorColour colour when there is an error in a parameter.  
    */
    public SingleWindows (GridBagLayout gbLayout, JPanel panel, int ypos, int xbin, int ybin, Color backColour, Color errorColour) {
	for(int i=0; i<NWIN; i++){
	    winLabel[i]   = new JLabel("Window " + (i+1));
	    winLabel[i].setBackground(backColour);
	    xstartText[i]  = new IntegerTextField(XSTART[i],   1, 1056, 1,    "xstart, window "  + (i+1), true, backColour, errorColour, 4);
	    ystartText[i]  = new IntegerTextField(YSTART[i],   1, 1072, 1,    "ystart, window " + (i+1), true, backColour, errorColour, 4);
	    nxText[i]      = new IntegerTextField(NX[i],       1, 1056, xbin, "nx, window "     + (i+1), true, backColour, errorColour, 4);
	    nyText[i]      = new IntegerTextField(NY[i],       1, 1072, ybin, "ny, window "     + (i+1), true, backColour, errorColour, 4);

	    int xpos = 0;
	    _addComponent( gbLayout, panel, winLabel[i],   xpos++, ypos,  GridBagConstraints.WEST);
	    _addComponent( gbLayout, panel, xstartText[i], xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, ystartText[i], xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, nxText[i],     xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, nyText[i],     xpos++, ypos,  GridBagConstraints.CENTER);
	    ypos++;
	}
    }
	
    public void addInputMethodListener(InputMethodListener action){
	for(int i=0; i<NWIN; i++){
	    xstartText[i].addInputMethodListener(action);
	    ystartText[i].addInputMethodListener(action);
	    nxText[i].addInputMethodListener(action);
	    nyText[i].addInputMethodListener(action);
	}
    }

    public void addActionListener(ActionListener action){
	for(int i=0; i<NWIN; i++){
	    xstartText[i].addActionListener(action);
	    ystartText[i].addActionListener(action);
	    nxText[i].addActionListener(action);
	    nyText[i].addActionListener(action);
	}
    }

    /** Disable paste operations in all fields */
    public void disablePaste(){
	for(int i=0; i<NWIN; i++){
	    xstartText[i].setTransferHandler(null);
	    ystartText[i].setTransferHandler(null);
	    nxText[i].setTransferHandler(null);
	    nyText[i].setTransferHandler(null);
	}
    }

    /** Checks validity of windows given X and Y binning factors. Also require
     * an even number of binned pixels overall.
     * @param xbin  X binning factor
     * @param ybin  Y binning factor
     * @param nwin  number of active windows
     * @param loud  true to print error messages to System.out
     */
    public boolean isValid(int xbin, int ybin, int nwin, boolean loud) {

	boolean ok = true;

	try{

	    for(int i=0; i<NWIN; i++){
            nxText[i].setIncrement(xbin);
            nyText[i].setIncrement(ybin);
	    }

        for(int i=nwin; i<NWIN; i++){
            nxText[i].setNormal();
            nyText[i].setNormal();
            xstartText[i].setNormal();
            ystartText[i].setNormal();
        }
	    int ntotal = 0;

	    for(int i=0; i<nwin; i++){

            int xstart  = xstartText[i].getValue();
            int ystart  = ystartText[i].getValue();
            int nx      = nxText[i].getValue();
            int ny      = nyText[i].getValue();

            // If we get here, the values are at least within
            // range assuming that there are no windows and not accounting
            // for window size. Now we check further ...

		if(nx % xbin != 0){
		    nxText[i].setError();
		    throw new Exception("nx of window " + (i+1) + " is not a multiple of xbin = " + xbin);
		}

		if(ny % ybin != 0){
		    nyText[i].setError();
		    throw new Exception("ny of window " + (i+1) + " is not a multiple of ybin = " + ybin);
		}

		if(xstart + nx > 1073){
		    nxText[i].setError();
		    throw new Exception("nx of window " + (i+1) + " is too large given the xstart value");
		}

		if(ystart + ny > 1073){
		    nyText[i].setError();
		    throw new Exception("ny of window " + (i+1) + " is too large given the ystart value");
		}

		ntotal += (nx/xbin)*(ny/ybin);

		// Test for window overlap and order
		for(int j=0; j<i; j++){ 
		    
		    int xstart_p  = xstartText[j].getValue();
		    int ystart_p  = ystartText[j].getValue();
		    int nx_p      = nxText[j].getValue();
		    int ny_p      = nyText[j].getValue();

		    if(ystart < ystart_p + ny_p && ystart + ny > ystart_p){
			ystartText[i].setError();
			throw new Exception("y values of window " + (i+1) + " overlaps with window " + (j+1));
		    }

		    if(ystart < ystart_p){
			ystartText[i].setError();
			throw new Exception("window " + (i+1) + " is the lower than window " + (j+1));
		    }
		}

	    }
	    
	    if(ntotal % 2 == 1){
		nyText[0].setError();
		throw new Exception("total number of binned pixels is odd, but must be even");
	    }
	}
	catch(Exception e){
	    if(loud) System.out.println(e.toString());
	    ok = false;
	}
	return ok;
    }

    public int getXstart(int nwin) throws Exception {
	return xstartText[nwin].getValue();
    }

    public String getXstartText(int nwin) {
	return xstartText[nwin].getText();
    }

    public void setXstartText(int nwin, String value) {
	xstartText[nwin].setText(value);
    }
    
    public int getYstart(int nwin) throws Exception {
	return ystartText[nwin].getValue();
    }

    public String getYstartText(int nwin) {
	return ystartText[nwin].getText();
    }

    public void setYstartText(int nwin, String value) {
	ystartText[nwin].setText(value);
    }

    public int getNx(int nwin) throws Exception {
	return nxText[nwin].getValue();
    }

    public String getNxText(int nwin) {
	return nxText[nwin].getText();
    }

    public void setNxText(int nwin, String value) {
	nxText[nwin].setText(value);
    }

    public int getNy(int nwin) throws Exception{
	return nyText[nwin].getValue();
    }

    public String getNyText(int nwin) {
	return nyText[nwin].getText();
    }

    public void setNyText(int nwin, String value) {
	nyText[nwin].setText(value);
    }

    public void setNwin(int nwin) {
	for(int i=0; i<nwin; i++){
	    winLabel[i].setEnabled(true);
	    xstartText[i].setEnabled(true);
	    ystartText[i].setEnabled(true);
	    nxText[i].setEnabled(true);
	    nyText[i].setEnabled(true);
	}
	for(int i=nwin; i<NWIN; i++){
	    winLabel[i].setEnabled(false);
	    xstartText[i].setEnabled(false);
	    ystartText[i].setEnabled(false);
	    nxText[i].setEnabled(false);
	    nyText[i].setEnabled(false);
	}
    }	

    public void setVisible(boolean vis){
	for(int i=0; i<NWIN; i++){
	    winLabel[i].setVisible(vis);
	    xstartText[i].setVisible(vis);
	    ystartText[i].setVisible(vis);
	    nxText[i].setVisible(vis);
	    nyText[i].setVisible(vis);
	    xstartText[i].setVisible(vis);
	    ystartText[i].setVisible(vis);
	    nxText[i].setVisible(vis);
	    nyText[i].setVisible(vis);
	}
    }
    
    // Method for adding components to GridBagLayout
    private static void _addComponent (GridBagLayout gbl, Container cont, Component comp, int gridx, int gridy, int anchor){
	GridBagConstraints gbc = new GridBagConstraints ();
	gbc.gridx      = gridx;
	gbc.gridy      = gridy;
	gbc.gridwidth  = 1;
	gbc.gridheight = 1;
	gbc.fill       = GridBagConstraints.NONE;
	gbc.anchor     = anchor;
	gbl.setConstraints(comp, gbc);
	cont.add (comp);
    }

}


