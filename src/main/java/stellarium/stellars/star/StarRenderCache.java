package stellarium.stellars.star;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stellarapi.api.lib.config.IConfigHandler;
import stellarapi.api.lib.math.SpCoord;
import stellarapi.api.lib.math.Vector3;
import stellarapi.api.optics.Wavelength;
import stellarium.client.ClientSettings;
import stellarium.render.stellars.access.IStellarChecker;
import stellarium.render.stellars.layer.IObjRenderCache;
import stellarium.stellars.OpticsHelper;
import stellarium.stellars.render.ICelestialObjectRenderer;
import stellarium.stellars.util.StarColor;
import stellarium.util.math.StellarMath;
import stellarium.view.ViewerInfo;

public class StarRenderCache implements IObjRenderCache<BgStar, StarImage, IConfigHandler> {

	//private float magLimit;
	protected boolean shouldRender;
	protected SpCoord appPos = new SpCoord();
	protected float red, green, blue;
	protected Vector3 ref = new Vector3();

	@Override
	public void updateSettings(ClientSettings settings, IConfigHandler config, BgStar star) {
		
	}

	@Override
	public void updateCache(BgStar object, StarImage image, ViewerInfo info, IStellarChecker checker) {
		if(image == null) {
			ref.set(object.pos);
			info.coordinate.getProjectionToGround().transform(this.ref);
			appPos.setWithVec(this.ref);
			info.sky.applyAtmRefraction(this.appPos);
		} else {
			SpCoord appCoord = image.getCurrentHorizontalPos();
			this.appPos.x = appCoord.x;
			this.appPos.y = appCoord.y;
		}

		double airmass = info.sky.calculateAirmass(this.appPos);

		/*this.appMag = (float) (object.mag + airmass * info.getExtinctionRate(EnumRGBA.Alpha)
			+ StellarMath.LumToMagWithoutSize(multiplier));*/

		//this.shouldRender = true;
		/*if(this.appMag > this.magLimit) {
			this.shouldRender = false;
			return;
		}
		
		if(appCoord.y < 0.0 && info.hideObjectsUnderHorizon) {
			this.shouldRender = false;
			return;
		}*/

		StarColor starColor = StarColor.getColor(object.B_V);

		double alpha = OpticsHelper.getBrightnessFromMagnitude(
				OpticsHelper.turbulance() + object.mag + airmass * info.sky.getExtinctionRate(Wavelength.visible));
		this.red = (float) (alpha * starColor.r / 255.0 * StellarMath.MagToLumWithoutSize(airmass * info.sky.getExtinctionRate(Wavelength.red)));
		this.green = (float) (alpha * starColor.g / 255.0 * StellarMath.MagToLumWithoutSize(airmass * info.sky.getExtinctionRate(Wavelength.V)));
		this.blue = (float) (alpha * starColor.b / 255.0 * StellarMath.MagToLumWithoutSize(airmass * info.sky.getExtinctionRate(Wavelength.B)));

		checker.startDescription();
		checker.pos(this.appPos);
		checker.brightness(red, green, blue);
		this.shouldRender = checker.checkRendered();
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ICelestialObjectRenderer getRenderer() {
		return StarRenderer.INSTANCE;
	}

}
