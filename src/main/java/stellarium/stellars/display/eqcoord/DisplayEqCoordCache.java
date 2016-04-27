package stellarium.stellars.display.eqcoord;

import stellarapi.api.ICelestialCoordinate;
import stellarapi.api.ISkyEffect;
import stellarapi.api.lib.math.Matrix3;
import stellarapi.api.lib.math.SpCoord;
import stellarapi.api.lib.math.Vector3;
import stellarapi.api.optics.IOpticalFilter;
import stellarapi.api.optics.IViewScope;
import stellarium.client.ClientSettings;
import stellarium.stellars.display.DisplaySettings;
import stellarium.stellars.display.IDisplayRenderCache;

public class DisplayEqCoordCache implements IDisplayRenderCache<DisplayEqCoord> {
	
	//Zero-time axial tilt
	public static final double e=0.4090926;
	public static final Matrix3 EqtoEc = new Matrix3();
	
	static {
		EqtoEc.setAsRotation(1.0, 0.0, 0.0, -e);
	}
	
	private Vector3 Buf, baseColor, decColor, RAColor;
	protected Vector3[][] displayvec = null;
	protected Vector3[][] colorvec = null;
	protected int latn, longn;
	protected boolean enabled;
	protected float brightness;
	
	private int renderId;

	@Override
	public void initialize(ClientSettings settings, DisplaySettings specificSettings, DisplayEqCoord display) {
		this.latn = display.displayFrag;
		this.longn = 2*display.displayFrag;
		this.enabled = display.displayEnabled;
		if(this.enabled)
		{
			this.displayvec = new Vector3[longn][latn+1];
			this.colorvec = new Vector3[longn][latn+1];
		}
		this.brightness = (float) display.displayAlpha;
		this.baseColor = new Vector3(display.displayBaseColor);
		this.decColor = new Vector3(display.displayDecColor);
		this.RAColor = new Vector3(display.displayRAColor);
		decColor.sub(this.baseColor);
		RAColor.sub(this.baseColor);
	}

	@Override
	public void updateCache(ClientSettings settings, DisplaySettings specificSettings, DisplayEqCoord object,
			ICelestialCoordinate coordinate, ISkyEffect sky, IViewScope scope, IOpticalFilter filter) {
		if(!this.enabled)
			return;
		
		for(int longc=0; longc<longn; longc++){
			for(int latc=0; latc<=latn; latc++){
				Buf = new SpCoord(longc*360.0/longn, latc*180.0/latn - 90.0).getVec();
				EqtoEc.transform(Buf);
				coordinate.getProjectionToGround().transform(Buf);
				
				SpCoord coord = new SpCoord();
				coord.setWithVec(Buf);
				
				sky.applyAtmRefraction(coord);

				displayvec[longc][latc] = coord.getVec();
				displayvec[longc][latc].scale(50.0);
				
				colorvec[longc][latc] = new Vector3(this.baseColor);
				
				Buf = new Vector3(this.decColor);
				Buf.scale((double)latc/latn);
				colorvec[longc][latc].add(Buf);
				
				Buf.set(this.RAColor);
				Buf.scale((double)longc/longn);
				colorvec[longc][latc].add(Buf);
			}
		}
	}

	@Override
	public int getRenderId() {
		return this.renderId;
	}

	@Override
	public void setRenderId(int id) {
		this.renderId = id;
	}

}