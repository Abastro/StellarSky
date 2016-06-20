package stellarium.stellars.milkyway;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Ordering;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stellarapi.api.celestials.EnumCelestialCollectionType;
import stellarapi.api.celestials.ICelestialObject;
import stellarapi.api.lib.config.IConfigHandler;
import stellarapi.api.lib.config.INBTConfig;
import stellarapi.api.lib.math.SpCoord;
import stellarium.stellars.layer.IStellarLayerType;
import stellarium.stellars.layer.StellarObjectContainer;
import stellarium.stellars.layer.query.ILayerTempManager;
import stellarium.stellars.render.ICelestialLayerRenderer;

public class LayerMilkyway implements IStellarLayerType<Milkyway, IConfigHandler, INBTConfig> {

	@Override
	public void initializeClient(IConfigHandler config, StellarObjectContainer container) throws IOException { }
	
	@Override
	public void initializeCommon(INBTConfig config, StellarObjectContainer container) throws IOException {
		Milkyway milkyway = new Milkyway();
		container.loadObject("Milkyway", milkyway);
		container.addRenderCache(milkyway, new MilkywayRenderCache());
		container.addImageType(milkyway, MilkywayImage.class);
	}
	
	@Override
	public void updateLayer(StellarObjectContainer<Milkyway, IConfigHandler> container, double year) { }

	@Override
	public String getName() {
		return "Milkyway";
	}

	@Override
	public int searchOrder() {
		return 3;
	}

	@Override
	public boolean isBackground() {
		return true;
	}

	@Override
	public EnumCelestialCollectionType getCollectionType() {
		return EnumCelestialCollectionType.DeepSkyObjects;
	}

	@Override
	public Comparator<ICelestialObject> getDistanceComparator(SpCoord pos) {
		return Ordering.allEqual().reverse();
	}

	@Override
	public Predicate<ICelestialObject> conditionInRange(SpCoord pos, double radius) {
		return Predicates.alwaysTrue();
	}

	@Override
	public Collection<Milkyway> getSuns(StellarObjectContainer container) {
		return null;
	}

	@Override
	public Collection<Milkyway> getMoons(StellarObjectContainer container) {
		return null;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ICelestialLayerRenderer getLayerRenderer() {
		return MilkywayLayerRenderer.INSTANCE;
	}

	@Override
	public ILayerTempManager<Milkyway> getTempLoadManager() {
		return null;
	}
}
