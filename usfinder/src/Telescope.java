package usfinder;

// Simple class to store inforamtion on telescopes.
//
// 'name'      is the case sensitive name to be used for the telescope
// 'zeroPoint' is an array of mags giving 1 counts/sec at airmass zero for ugriz
// plateScale  is the scale in arcsec/pixel
// flipped     is a flag to store if the north-east axis flipped or not?
// delta_pa    is the rotator position when the ultracam chip runs north-south
// delta_x     is the error in alignment (x-direction) between chip centre and telescope pointing (arcsecs)
// delta_y     is the error in alignment (y-direction) between chip centre and telescope pointing (arcsecs)
// slit_x      is the x position of the slit (in pixels)

public class Telescope {	

	public Telescope(String	name, double[] zeroPoint, double plateScale, boolean flipped,
                     double delta_pa, double delta_x, double delta_y, double slit_x) {
        this.name        = name;
        this.zeroPoint   = zeroPoint;
        this.plateScale  = plateScale;
        this.flipped     = flipped;
        this.delta_pa    = delta_pa;
        this.delta_x     = delta_x;
        this.delta_y     = delta_y;
        this.slit_x      = slit_x;
    }
	
    public void setPlateScale(double ps){
        this.plateScale = ps;
    }
    
    public String name;
    public double[] zeroPoint;
    public double plateScale;
    public boolean flipped; 
    public double delta_pa; 
    public double delta_x; 
    public double delta_y;
    public double slit_x;
    
};
