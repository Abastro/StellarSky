package stellarium.client.gui.clock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import stellarium.StellarSky;
import stellarium.api.IHourProvider;
import stellarium.api.StellarSkyAPI;
import stellarium.client.ClientSettings;
import stellarium.client.EnumKey;
import stellarium.client.gui.EnumGuiOverlayMode;
import stellarium.client.gui.IGuiOverlayElement;
import stellarium.client.gui.pos.EnumHorizontalPos;
import stellarium.client.gui.pos.EnumVerticalPos;
import stellarium.stellars.StellarManager;
import stellarium.world.StellarDimensionManager;

public class GuiOverlayClock implements IGuiOverlayElement<GuiOverlayClockSettings> {
	
	private static final int WIDTH = 160;
	private static final int HEIGHT = 36;
	private static final int ADDITIONAL_HEIGHT = 20;
	private static final int ANIMATION_DURATION = 10;
	
	private Minecraft mc;
	private int animationTick = 0;
	private EnumGuiOverlayMode currentMode = EnumGuiOverlayMode.OVERLAY;

	private GuiOverlayClockSettings settings;
	private GuiButton buttonFix;
	private GuiButton buttonToggle;
	
	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return currentMode.focused()? HEIGHT + ADDITIONAL_HEIGHT : HEIGHT;
	}
	
	@Override
	public void initialize(Minecraft mc, GuiOverlayClockSettings settings) {
		this.mc = mc;
		this.settings = settings;
		
		this.buttonFix = new GuiButton(0, 0, HEIGHT, 80, 20, settings.isFixed? "Unfix" : "Fix");
		this.buttonToggle = new GuiButton(1, 80, HEIGHT, 80, 20, settings.viewMode.getName());
	}

	@Override
	public float animationOffsetX(float partialTicks) {
		return 0.0f;
	}

	@Override
	public float animationOffsetY(float partialTicks) {
		return - this.getHeight() * this.animationTick / (float)ANIMATION_DURATION;
	}

	@Override
	public void switchMode(EnumGuiOverlayMode mode) {
		if(mode != this.currentMode)
		{
			if(!settings.isFixed && mode.focused())
				this.animationTick = ANIMATION_DURATION;
			else this.animationTick = 0;
		}
		this.currentMode = mode;
	}

	@Override
	public void updateOverlay() {
		if(this.animationTick > 0 && currentMode.focused())
			this.animationTick--;
		else if(this.animationTick < ANIMATION_DURATION && !currentMode.focused())
			this.animationTick++;
		
		if(settings.isFixed)
			this.animationTick = 0;
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int eventButton) {
		if(!currentMode.focused())
			return false;
		
		Minecraft mc = Minecraft.getMinecraft();
		if(buttonFix.mousePressed(mc, mouseX, mouseY))
		{
			settings.isFixed = !settings.isFixed;
			buttonFix.displayString = settings.isFixed? "Unfix" : "Fix";
			return true;
		}

		if(buttonToggle.mousePressed(mc, mouseX, mouseY)) {
			settings.viewMode = settings.viewMode.nextMode();
			buttonToggle.displayString = settings.viewMode.getName();
			return true;
		}
		
		return false;
	}

	@Override
	public boolean mouseMovedOrUp(int mouseX, int mouseY, int eventButton) {
		if(!currentMode.focused())
			return false;
		
		buttonFix.mouseReleased(mouseX, mouseY);
		buttonToggle.mouseReleased(mouseX, mouseY);

		return false;
	}

	@Override
	public boolean keyTyped(EnumKey key, char eventChar) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		if(currentMode.focused()) {
			Minecraft mc = Minecraft.getMinecraft();

			buttonFix.drawButton(mc, mouseX, mouseY);
			buttonToggle.drawButton(mc, mouseX, mouseY);
		}
		
		ClientSettings setting = StellarSky.proxy.getClientSettings();

		FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
		StellarManager manager = StellarManager.getManager(true);
		StellarDimensionManager dimManager = StellarDimensionManager.get(Minecraft.getMinecraft().theWorld);

		if(dimManager == null)
			return;

		double currentTick = Minecraft.getMinecraft().theWorld.getWorldTime();
		double time = manager.getSkyTime(currentTick) + 1000.0;
		double daylength = manager.getSettings().day;
		double yearlength = manager.getSettings().year;
		double date = time / daylength + dimManager.getSettings().longitude / 180.0;
		double year = date / yearlength;

		int yr = (int)Math.floor(year);
		int day = (int)Math.floor(date - Math.floor(yr * yearlength));
		int tick = (int)Math.floor((date - Math.floor(yr * yearlength) - day)*daylength);

		IHourProvider provider = StellarSkyAPI.getCurrentHourProvider();

		int hour = provider.getCurrentHour(daylength, tick);
		int minute = provider.getCurrentMinute(daylength, tick, hour);

		int totalhour = provider.getTotalHour(daylength);
		int totalminute = provider.getTotalMinute(daylength, totalhour);
		int restMinuteInDay = provider.getRestMinuteInDay(daylength, totalhour);

		int yOffset = 0;

		this.drawString(fontRenderer, "hud.text.year", 5, 10*(yOffset++)+5,
				String.format("%d", yr));
		this.drawString(fontRenderer, "hud.text.day", 5, 10*(yOffset++)+5,
				String.format("%7d", day),
				String.format("%.2f", yearlength));

		if(settings.viewMode.showTick())
			this.drawString(fontRenderer, "hud.text.tick", 5, 10*(yOffset++)+5,
					String.format("%-6d", tick),
					String.format("%.2f", daylength));
		else this.drawString(fontRenderer, "hud.text.time", 5, 10*(yOffset++)+5,
				String.format("%3d", hour), 
				String.format("%02d", minute),
				String.format("%3d", totalhour),
				String.format("%02d", restMinuteInDay),
				String.format("%02d", totalminute));
	}
	
	private void drawString(FontRenderer fontRenderer, String str, int x, int y, String... obj) {
		fontRenderer.drawString(I18n.format(str, (Object[])obj), x, y, 0xff888888);
	}
}
