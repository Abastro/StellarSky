package stellarium.display;

import java.util.List;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import stellarapi.api.ICelestialCoordinate;
import stellarapi.api.ISkyEffect;
import stellarapi.api.StellarAPIReference;
import stellarapi.api.lib.config.SimpleHierarchicalConfig;
import stellarium.StellarSky;
import stellarium.client.ClientSettings;

@SideOnly(Side.CLIENT)
public class DisplayManager implements IDisplayInjectable {
	
	private List<Delegate> displayList = Lists.newArrayList();
	
	@Override
	public <Cfg extends PerDisplaySettings, Cache extends IDisplayCache<Cfg>> void injectDisplay(
			IDisplayElementType<Cfg, Cache> type, Cfg settings) {
		displayList.add(new Delegate(type, settings));
	}

	@Override
	public SimpleHierarchicalConfig getSubSettings(ClientSettings settings) {
		DisplayOverallSettings displaySettings = new DisplayOverallSettings();
		settings.putSubConfig("Display", displaySettings);
		return displaySettings;
	}

	public void render(Minecraft mc, Tessellator tessellator, float partialTicks, boolean postCelestials) {
		for(Delegate delegate : this.displayList) {
			delegate.renderer.render(new DisplayRenderInfo(mc, tessellator, partialTicks, postCelestials),
					delegate.cache);
		}
	}
	
	public void reloadClientSettings(ClientSettings clientSettings) {
		for(Delegate delegate : this.displayList) {
			delegate.cache.initialize(clientSettings, delegate.settings);
		}
	}

	public void updateDisplay(ClientSettings clientSettings) {
		ICelestialCoordinate coordinate = StellarAPIReference.getCoordinate(StellarSky.proxy.getDefWorld());
		ISkyEffect sky = StellarAPIReference.getSkyEffect(StellarSky.proxy.getDefWorld());

		DisplayCacheInfo info = new DisplayCacheInfo(coordinate, sky);
		for(Delegate delegate : this.displayList) {
			delegate.cache.updateCache(clientSettings, delegate.settings, info);
		}
	}
	
	private static class Delegate<Cfg extends PerDisplaySettings, Cache extends IDisplayCache<Cfg>> {
		public Delegate(IDisplayElementType<Cfg, Cache> type, Cfg settings) {
			this.type = type;
			this.settings = settings;
			this.cache = type.generateCache();
			this.renderer = type.getRenderer();
		}

		private IDisplayElementType<Cfg, Cache> type;
		private Cache cache;
		private Cfg settings;
		private IDisplayRenderer<Cache> renderer;
	}
}
