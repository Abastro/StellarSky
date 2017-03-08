package stellarium.render.stellars.phased;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.util.ResourceLocation;
import stellarapi.api.lib.math.SpCoord;
import stellarapi.api.lib.math.Spmath;
import stellarapi.api.lib.math.Vector3;
import stellarium.render.shader.IShaderObject;
import stellarium.render.stellars.access.EnumStellarPass;
import stellarium.render.stellars.access.IStellarTessellator;
import stellarium.stellars.OpticsHelper;
import stellarium.view.ViewerInfo;

public class StellarTessellator implements IStellarTessellator {

	//Limits size per draw call
	private static final int maxBufferSize = 0x80000;
	
	
	private float radius;
	
	private float longitude, latitude, depth;
	private float uCoord, vCoord;
	private float red, green, blue;
	private float normalX, normalY, normalZ;
	
	private float renderedRed, renderedGreen, renderedBlue;
	
	
	private boolean renderingDominate, hasTexture;
	private IShaderObject shader;
	private ViewerInfo viewer;
	private int renderDominateList;
	
	private float rasterizedAngleRatio;
	
	private float weatherFactor;
	
	
	private int currentContextSize;
	private boolean hasNormal;
	
	private FloatBuffer buffer;
	private float[] rawBuffer;
	private int rawBufferCount;
	private short vertexCount;
	
	private static final double LOWER_LIMIT_FACTOR = 1.0e-3;
	
	StellarTessellator() {
		this.buffer = GLAllocation.createDirectFloatBuffer(maxBufferSize);
		this.rawBuffer = new float[maxBufferSize];
		// TODO Vertex Buffer
	}

	public void initialize(StellarRenderInformation info) {
		this.weatherFactor = 1.0f - (1.0f - 5.0e-5f) * info.world.getRainStrength(info.partialTicks);
		
		this.shader = info.getActiveShader();
		this.viewer = info.info;

		this.renderDominateList = info.getAtmCallList();
		
		this.rasterizedAngleRatio = (float) (viewer.multiplyingPower / Spmath.Radians(70.0f) * info.screenSize);
		
		this.preCalculateColorInfo();
	}
	
	public void initializePass(EnumStellarPass pass) {
		this.renderingDominate = pass.isDominate;
		this.hasTexture = pass.hasTexture;
		
		this.onPrePassRender();
	}
	
	@Override
	public void begin(boolean hasNormal) {
		if(this.hasTexture) {
			this.currentContextSize = hasNormal? 11 : 8;
		} else {
			this.currentContextSize = 6;
			this.radius = 0.0f;
		}
		
		this.rawBufferCount = 0;
		this.vertexCount = 0;
		
		this.recalcColorInfoObj(true);
	}

	@Override
	public void pos(SpCoord pos, float depth) {
		this.longitude = (float) Spmath.Radians(pos.x);
		this.latitude = (float) Spmath.Radians(pos.y);
		this.depth = depth;
	}

	@Override
	public void color(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	@Override
	public void texture(float u, float v) {
		if(this.hasTexture) {
			this.uCoord = u;
			this.vCoord = v;
		}
	}

	@Override
	public void normal(Vector3 normal) {
		if(this.currentContextSize == 11) {
			this.normalX = (float) normal.getX();
			this.normalY = (float) normal.getY();
			this.normalZ = (float) normal.getZ();
			this.hasNormal = true;
		}
	}

	@Override
	public void writeVertex() {
		this.processColor();
		
		this.rawBuffer[this.rawBufferCount] = this.longitude;
		this.rawBuffer[this.rawBufferCount + 1] = this.latitude;
		this.rawBuffer[this.rawBufferCount + 2] = this.depth;
		this.rawBuffer[this.rawBufferCount + 3] = this.renderedRed;
		this.rawBuffer[this.rawBufferCount + 4] = this.renderedGreen;
		this.rawBuffer[this.rawBufferCount + 5] = this.renderedBlue;

		if(this.hasTexture) {
			this.rawBuffer[this.rawBufferCount + 6] = this.uCoord;
			this.rawBuffer[this.rawBufferCount + 7] = this.vCoord;
		}
		if(this.hasNormal) {
			this.rawBuffer[this.rawBufferCount + 8] = this.normalX;
			this.rawBuffer[this.rawBufferCount + 9] = this.normalY;
			this.rawBuffer[this.rawBufferCount + 10] = this.normalZ;
		}

		this.rawBufferCount += this.currentContextSize;
		this.vertexCount ++;
		this.hasNormal = false;
	}
	
	private static final float DEFAULT_SIZE = Spmath.Radians(0.06f);
	
	private float surfMultRed, surfMultGreen, surfMultBlue;
	private float pointMultRed, pointMultGreen, pointMultBlue;
	private float radiusMultRed, radiusMultGreen, radiusMultBlue;
	private float currentMultRed, currentMultGreen, currentMultBlue;

	private void preCalculateColorInfo() {
		float multiplier = (float) (Spmath.sqr(DEFAULT_SIZE) * OpticsHelper.invSpriteScalef());

		this.surfMultRed = (float) (viewer.colorMultiplier.getX() / Spmath.sqr(viewer.multiplyingPower));
		this.surfMultGreen = (float) (viewer.colorMultiplier.getY() / Spmath.sqr(viewer.multiplyingPower));
		this.surfMultBlue = (float) (viewer.colorMultiplier.getZ() / Spmath.sqr(viewer.multiplyingPower));
		
		this.radiusMultRed = this.surfMultRed * multiplier;
		this.radiusMultGreen = this.surfMultGreen * multiplier;
		this.radiusMultBlue = this.surfMultBlue * multiplier;

		this.pointMultRed = (float) (this.radiusMultRed / Spmath.sqr(viewer.resolutionColor.getX()));
		this.pointMultGreen = (float) (this.radiusMultGreen / Spmath.sqr(viewer.resolutionColor.getY()));
		this.pointMultBlue = (float) (this.radiusMultBlue / Spmath.sqr(viewer.resolutionColor.getZ()));
	}
	
	private void onPrePassRender() {
		if(this.hasTexture || this.renderingDominate) {
			this.currentMultRed = this.surfMultRed;
			this.currentMultGreen = this.surfMultGreen;
			this.currentMultBlue = this.surfMultBlue;
		}
	}

	private void recalcColorInfoObj(boolean isCompletelyPoint) {
		if(!this.hasTexture && !this.renderingDominate) {
			if(isCompletelyPoint) {
				this.currentMultRed = this.pointMultRed;
				this.currentMultGreen = this.pointMultGreen;
				this.currentMultBlue = this.pointMultBlue;
			} else {
				this.currentMultRed = (float) (this.radiusMultRed / Spmath.sqr(viewer.resolutionColor.getX() + this.radius));
				this.currentMultGreen = (float) (this.radiusMultGreen / Spmath.sqr(viewer.resolutionColor.getY() + this.radius));
				this.currentMultBlue = (float) (this.radiusMultBlue / Spmath.sqr(viewer.resolutionColor.getZ() + this.radius));
			}
		}

		if(!this.renderingDominate) {
			this.currentMultRed *= this.weatherFactor;
			this.currentMultGreen *= this.weatherFactor;
			this.currentMultBlue *= this.weatherFactor;
		}
	}

	private void processColor() {
		this.renderedRed = this.red * this.currentMultRed;
		this.renderedGreen = this.green * this.currentMultGreen;
		this.renderedBlue = this.blue * this.currentMultRed;

		if(this.renderingDominate)
			return;

		float brightest = Math.max(this.renderedRed, Math.max(this.renderedGreen, this.blue));

		if(brightest < LOWER_LIMIT_FACTOR) {
			this.renderedRed = ((this.renderedRed * 2 * brightest + 1.0f - 2 * brightest) * brightest);
			this.renderedGreen = ((this.renderedGreen * 2 * brightest + 1.0f - 2 * brightest) * brightest);
			this.renderedBlue = ((this.renderedBlue * 2 * brightest + 1.0f - 2 * brightest) * brightest);
		}

		this.renderedRed = OpticsHelper.getCompressed(this.renderedRed);
		this.renderedGreen = OpticsHelper.getCompressed(this.renderedGreen);
		this.renderedBlue = OpticsHelper.getCompressed(this.renderedBlue);
	}

	@Override
	public void end() {
		if(this.hasTexture) {
			;
		} else if(!this.renderingDominate) {
			float size = 10.0f * (float)viewer.resolutionGeneral + 2.0f * this.radius;
			GL11.glPointSize(Math.max(this.rasterizedAngleRatio * size, 10.0f));
			if(this.radius > 0.0f)
				shader.getField("scaledRadius").setDouble(this.radius / size);
			
			shader.getField("invres2").setDouble4(
					OpticsHelper.invSpriteScale2()*this.toInvRes2(size, (float)viewer.resolutionColor.getX()),
					OpticsHelper.invSpriteScale2()*this.toInvRes2(size, (float)viewer.resolutionColor.getY()),
					OpticsHelper.invSpriteScale2()*this.toInvRes2(size, (float)viewer.resolutionColor.getZ()),
					OpticsHelper.invSpriteScale2()*this.toInvRes2(size, (float)viewer.resolutionGeneral));
		}
		
		if(!this.renderingDominate)
			this.tessellation();
		else {
			for(int index = 0; index < this.rawBufferCount; index += this.currentContextSize) {
				shader.getField("lightDir").setVector3(
						new SpCoord(Spmath.Degrees(this.rawBuffer[index]), Spmath.Degrees(this.rawBuffer[index+1])).getVec());
				shader.getField("lightColor").setDouble3(
						this.rawBuffer[index+3], this.rawBuffer[index+4], this.rawBuffer[index+5]);
				GL11.glCallList(this.renderDominateList);
			}
		}
	}
	
	private void tessellation() {
		if(this.rawBufferCount <= 0)
			return;
		
		buffer.clear();
		buffer.put(this.rawBuffer, 0, this.rawBufferCount);
		buffer.limit(this.rawBufferCount);
		
		buffer.position(0);
		GL11.glVertexPointer(3, this.currentContextSize << 2, this.buffer);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		
		buffer.position(3);
		GL11.glColorPointer(3, this.currentContextSize << 2, this.buffer);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

		if(this.hasTexture) {
			buffer.position(6);
			GL11.glTexCoordPointer(2, this.currentContextSize << 2, this.buffer);
			GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}
		
		if(this.currentContextSize == 11) {
			buffer.position(8);
			GL11.glNormalPointer(this.currentContextSize << 2, this.buffer);
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		}
		
		GL11.glDrawArrays(this.hasTexture? GL11.GL_QUADS : GL11.GL_POINTS,
				0, this.vertexCount);
		
		if(this.currentContextSize == 11) {
			GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		}
		
		if(this.hasTexture) {
			GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		}

		GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
	}
	
	private float toInvRes2(float size, float resolution) {
		float invres = size / resolution;
		return 0.5f * invres * invres;
	}

	
	@Override
	public void radius(float angularRadius) {
		if(!this.hasTexture) {
			this.radius = angularRadius;
			this.recalcColorInfoObj(false);
		}
	}

	@Override
	public void bindTexture(ResourceLocation location) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(location);
	}
}
