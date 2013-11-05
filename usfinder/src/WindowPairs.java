/*=====================================================*/
/*                                                     */
/* Copyright (c) University of Warwick 2005            */
/*                                                     */
/*=====================================================*/

package usfinder;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;

import util.*;

/** Class to handle the windows. Makes a GUI with all the necessary values
 * propmpted
 */

public class WindowPairs {

    // Initial values
    private static final int[] YSTART  = {  1, 201, 401};
    private static final int[] XLEFT   = {100, 100, 100};
    private static final int[] XRIGHT  = {600, 600, 600};
    private static final int[] NX      = { 50,  50,  50};
    private static final int[] NY      = { 50,  50,  50};

    private JLabel[]           pairLabel   = new JLabel[3];
    private IntegerTextField[] ystartText  = new IntegerTextField[3];
    private IntegerTextField[] xleftText   = new IntegerTextField[3];
    private IntegerTextField[] xrightText  = new IntegerTextField[3];
    private IntegerTextField[] nxText      = new IntegerTextField[3];
    private IntegerTextField[] nyText      = new IntegerTextField[3];

    /** Main constructor 
     * @param gbLayout    layout to use
     * @param panel       panel to contain the window parameters
     * @param ypos        the yposition to add in the components
     * @param xbin        the X binning factor, needed to increment NX properly
     * @param ybin        the Y binning factor, needed to increment NY properly
     * @param backColour  background colour
     * @param errorColour colour when there is an error in a parameter.  
     * @param specialNY   colour when there is an error in a parameter.  
    */
    public WindowPairs (GridBagLayout gbLayout, JPanel panel, int ypos, int xbin, int ybin, Color backColour, Color errorColour, int[] specialNy) {
	for(int i=0; i<3; i++){
	    pairLabel[i]   = new JLabel("Pair " + (i+1));
	    pairLabel[i].setBackground(backColour);
	    ystartText[i]  = new IntegerTextField(YSTART[i],   1, 1072, 1,    "ystart, window pair " + (i+1), true, backColour, errorColour, 4);
	    xleftText[i]   = new IntegerTextField(XLEFT[i],    1, 1056, 1,    "xleft, window pair "  + (i+1), true, backColour, errorColour, 4);
	    xrightText[i]  = new IntegerTextField(XRIGHT[i],   1, 1056, 1,    "xright, window pair " + (i+1), true, backColour, errorColour, 4);
	    nxText[i]      = new IntegerTextField(NX[i],       1, 1056, xbin, "nx, window pair "     + (i+1), true, backColour, errorColour, 4);
	    nyText[i]      = new IntegerTextField(NY[i],       1, 1072, ybin, "ny, window pair "     + (i+1), true, backColour, errorColour, 4);

	    int xpos = 0;
	    _addComponent( gbLayout, panel, pairLabel[i],  xpos++, ypos,  GridBagConstraints.WEST);
	    _addComponent( gbLayout, panel, ystartText[i], xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, xleftText[i],  xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, xrightText[i], xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, nxText[i],     xpos++, ypos,  GridBagConstraints.CENTER);
	    _addComponent( gbLayout, panel, nyText[i],     xpos++, ypos,  GridBagConstraints.CENTER);
	    ypos++;
	}
	nyText[0].setSpecial(specialNy);
    }

    /** Hide from GUI */
    public void setVisible(boolean vis){
	for(int i=0; i<3; i++){
	    pairLabel[i].setVisible(vis);
	    ystartText[i].setVisible(vis);
	    xleftText[i].setVisible(vis);
	    xrightText[i].setVisible(vis);
	    nxText[i].setVisible(vis);
	    nyText[i].setVisible(vis);
	}
    }
    
	public void addActionListener(ActionListener action){
		for(int i=0; i<3; i++){
			ystartText[i].addActionListener(action);
			xleftText[i].addActionListener(action);
			xrightText[i].addActionListener(action);
			nxText[i].addActionListener(action);
			nyText[i].addActionListener(action);
		}
    }

    /** Disable paste operations in all fields */
    public void disablePaste(){
	for(int i=0; i<3; i++){
	    ystartText[i].setTransferHandler(null);
	    xleftText[i].setTransferHandler(null);
	    xrightText[i].setTransferHandler(null);
	    nxText[i].setTransferHandler(null);
	    nyText[i].setTransferHandler(null);
	}
    }

    /** Checks validity of windows given X and Y binning factors
     * @param xbin  X binning factor
     * @param ybin  Y binning factor
     * @param npair number of active pairs
     * @param loud  true to print error messages to System.out
     */
    public boolean isValid(int xbin, int ybin, int npair, boolean loud) {

	boolean ok = true;

	try{

	    for(int i=0; i<3; i++){
		nxText[i].setIncrement(xbin);
		nyText[i].setIncrement(ybin);
	    }

	    for(int i=0; i<npair; i++){

		int ystart  = ystartText[i].getValue();
		int xleft   = xleftText[i].getValue();
		int xright  = xrightText[i].getValue();
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

		if(ystart + ny > 1073){
		    nyText[i].setError();
		    throw new Exception("ny of window pair " + (i+1) + " is too large given the ystart value");
		}

		if(xleft + nx > 1057){
		    nxText[i].setError();
		    throw new Exception("nx of window pair " + (i+1) + " is too large given the xleft value");
		}

		if(xright + nx > 1057){
		    nxText[i].setError();
		    throw new Exception("nx of window pair " + (i+1) + " is too large given the xright value");
		}

		// Test for window overlap (x)
		// DA has setup drift mode so there must be a 2 pixel gap between windows. Increased to 3 for a margin of error by SL - 2013-03-12
		if(xleft+nx+3 > xright){
		    xleftText[i].setError();
		    throw new Exception("left hand window overlaps with right hand window");
		}
		// Test for window overlap (y)
		for(int j=0; j<i; j++){ 
		    
		    int ystart_p  = ystartText[j].getValue();
		    int xleft_p   = xleftText[j].getValue();
		    int xright_p  = xrightText[j].getValue();
		    int nx_p      = nxText[j].getValue();
		    int ny_p      = nyText[j].getValue();

		    if(ystart < ystart_p + ny_p && ystart + ny > ystart_p){
			ystartText[i].setError();
			throw new Exception("ystart of window pair " + (i+1) + " overlaps with window pair " + (j+1));
		    }
		}
	    }
	}
	catch(Exception e){
	    if(loud) System.out.println(e.toString());
	    ok = false;
	}
	return ok;
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

    public int getXleft(int nwin) throws Exception {
	return xleftText[nwin].getValue();
    }

    public String getXleftText(int nwin) {
	return xleftText[nwin].getText();
    }

    public void setXleftText(int nwin, String value) {
	xleftText[nwin].setText(value);
    }
    
    public int getXright(int nwin) throws Exception {
	return xrightText[nwin].getValue();
    }

    public String getXrightText(int nwin) {
	return xrightText[nwin].getText();
    }

    public void setXrightText(int nwin, String value) {
	xrightText[nwin].setText(value);
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

    public int getNy(int nwin) throws Exception {
	return nyText[nwin].getValue();
    }

    public String getNyText(int nwin) {
	return nyText[nwin].getText();
    }

    public void setNyText(int nwin, String value) {
	nyText[nwin].setText(value);
    }

    public void setNpair(int npair) {
	for(int i=0; i<npair; i++){
	    ystartText[i].setEnabled(true);
	    xleftText[i].setEnabled(true);
	    xrightText[i].setEnabled(true);
	    nxText[i].setEnabled(true);
	    nyText[i].setEnabled(true);
	}
	for(int i=npair; i<3; i++){
	    ystartText[i].setEnabled(false);
	    xleftText[i].setEnabled(false);
	    xrightText[i].setEnabled(false);
	    nxText[i].setEnabled(false);
	    nyText[i].setEnabled(false);
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


