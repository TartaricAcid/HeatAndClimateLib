package defeatedcrow.hac.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import defeatedcrow.hac.api.climate.ClimateAPI;
import defeatedcrow.hac.api.climate.DCHeatTier;
import defeatedcrow.hac.api.climate.IClimate;
import defeatedcrow.hac.config.CoreConfigDC;
import defeatedcrow.hac.core.ClimateCore;
import defeatedcrow.hac.core.client.DCTextures;

@SideOnly(Side.CLIENT)
public class RenderTempHUDEvent {

	public static final RenderTempHUDEvent INSTANCE = new RenderTempHUDEvent();

	private int tier = 0;
	private int count = 10;

	public static boolean enable = CoreConfigDC.showDamageIcon;

	@SubscribeEvent
	public void doRender(RenderGameOverlayEvent.Post event) {
		if (event.getType() != null && event.getType() == ElementType.HEALTH) {
			EntityPlayer player = ClimateCore.proxy.getPlayer();
			World world = ClimateCore.proxy.getClientWorld();
			GuiScreen gui = Minecraft.getMinecraft().currentScreen;
			if (player != null && world != null && gui == null) {
				if (count == 0) {
					count = 10;

					BlockPos pos = player.getPosition();
					if (pos != null && world.isAreaLoaded(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
						DCHeatTier temp = ClimateAPI.calculator.getAverageTemp(world, pos, 2, false);
						if (temp != null) {
							tier = temp.getTier();
						}
					}
				} else {
					count--;
				}

				if (enable) {
					int prev = 2 - CoreConfigDC.damageDifficulty;
					int damage = 0;
					if (tier < 0) {
						damage = (tier * 2) + prev;
					} else if (tier > 0) {
						damage = tier - prev;
					}

					int tX = 2;
					if (damage >= 2) {
						tX = 4;
					} else if (damage == 1) {
						tX = 3;
					} else if (damage == -1) {
						tX = 1;
					} else if (damage <= -2) {
						tX = 0;
					}
					tX *= 16;

					Minecraft.getMinecraft().getTextureManager().bindTexture(DCTextures.HUD.getRocation());
					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.enableBlend();

					int factX = -106;
					int factY = -4;
					int offsetX = CoreConfigDC.iconX;
					int offsetY = CoreConfigDC.iconY;
					int x = (event.getResolution().getScaledWidth() / 2) + factX + offsetX;
					int y = event.getResolution().getScaledHeight() - 39 + factY + offsetY;
					drawTexturedModalRect(x, y, tX, 0, 16, 16);

					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				}

			}

		}
	}

	private IClimate[][][] climates = new IClimate[3][3][3];
	private int count2 = 0;

	// @SubscribeEvent
	public void renderClimateViewer(DrawBlockHighlightEvent event) {
		EntityPlayer player = event.getPlayer();
		RayTraceResult target = event.getTarget();
		if (player != null && target != null) {
			World world = player.worldObj;
			BlockPos pos = target.getBlockPos();
			IClimateViewer viewer = null;

			ItemStack held = player.getHeldItemMainhand();
			if (held != null && held.getItem() instanceof IClimateViewer) {
				viewer = (IClimateViewer) held.getItem();
			} else {
				held = player.getHeldItemOffhand();
				if (held != null && held.getItem() instanceof IClimateViewer) {
					viewer = (IClimateViewer) held.getItem();
				}
			}

			if (viewer != null && player.isSneaking()) {
				// climate情報の更新
				if (count2 == 0) {
					count2 = 10;
					// DCLogger.debugLog("viewer");
					if (pos != null && world.isAreaLoaded(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
						BlockPos base = pos.add(-1, -1, -1);
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								for (int k = 0; k < 3; k++) {
									BlockPos p = base.add(i, j, k);
									IClimate clm = ClimateAPI.calculator.getClimate(world, p, new int[] {
											2,
											1,
											1 });
									climates[i][j][k] = clm;
								}
							}
						}
					}
				} else {
					count2--;
				}
				ViewerMode mode = viewer.getMode(held);
				if (mode != ViewerMode.NONE) {
					for (int i = 0; i < 3; i++) {
						for (int j = 0; j < 3; j++) {
							for (int k = 0; k < 3; k++) {
								IClimate clm = climates[i][j][k];
								BlockPos base = pos.add(-1, -1, -1);
								BlockPos p = base.add(i, j, k);
								if (clm != null) {
									// DCLogger.debugLog("view " + i + ", " + j + ", " + k);
									float r = 1.0F;
									float g = 1.0F;
									float b = 1.0F;
									if (mode == ViewerMode.TEMP) {
										r = clm.getHeat().getColor()[0] / 256.0F;
										g = clm.getHeat().getColor()[1] / 256.0F;
										b = clm.getHeat().getColor()[2] / 256.0F;
									} else if (mode == ViewerMode.HUM) {
										r = clm.getHumidity().getColor()[0] / 256.0F;
										g = clm.getHumidity().getColor()[1] / 256.0F;
										b = clm.getHumidity().getColor()[2] / 256.0F;
									} else if (mode == ViewerMode.WIND) {
										r = clm.getAirflow().getColor()[0] / 256.0F;
										g = clm.getAirflow().getColor()[1] / 256.0F;
										b = clm.getAirflow().getColor()[2] / 256.0F;
									}
									double mx = p.getX() + 0.25D;
									double my = p.getY() + 0.25D;
									double mz = p.getZ() + 0.25D;
									double xx = p.getX() + 0.75D;
									double xy = p.getY() + 0.75D;
									double xz = p.getZ() + 0.75D;
									AxisAlignedBB aabb = new AxisAlignedBB(mx, my, mz, xx, xy, xz);

									Minecraft.getMinecraft().getTextureManager()
											.bindTexture(DCTextures.GRAY.getRocation());
									GL11.glPushMatrix();
									GL11.glEnable(GL11.GL_BLEND);
									GL11.glDisable(GL11.GL_LIGHTING);
									GL11.glDepthMask(true);
									OpenGlHelper.glBlendFunc(770, 771, 1, 0);
									GL11.glColor4f(r, g, b, 0.85F);

									this.drawOutlinedBoundingBox(aabb);

									Minecraft.getMinecraft().renderEngine
											.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
									// GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
									OpenGlHelper.glBlendFunc(770, 1, 1, 0);
									GL11.glDisable(GL11.GL_BLEND);
									GL11.glEnable(GL11.GL_LIGHTING);
									GL11.glDepthMask(true);
									GL11.glPopMatrix();
								}
							}
						}
					}
				}
			}
		}

	}

	public void drawTexturedModalRect(int x, int y, int tX, int tY, int width, int height) {
		float f = 0.00390625F;
		float f1 = 0.00390625F;
		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer vertexbuffer = tessellator.getBuffer();
		vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		vertexbuffer.pos(x + 0, y + height, -90.0F).tex((tX + 0) * f, (tY + height) * f1).endVertex();
		vertexbuffer.pos(x + width, y + height, -90.0F).tex((tX + width) * f, (tY + height) * f1).endVertex();
		vertexbuffer.pos(x + width, y + 0, -90.0F).tex((tX + width) * f, (tY + 0) * f1).endVertex();
		vertexbuffer.pos(x + 0, y + 0, -90.0F).tex((tX + 0) * f, (tY + 0) * f1).endVertex();
		tessellator.draw();
	}

	private void drawOutlinedBoundingBox(AxisAlignedBB aabb) {
		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer vertexbuffer = tessellator.getBuffer();
		vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		vertexbuffer.pos(aabb.minX, aabb.maxY, aabb.minZ).tex(1, 0).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.maxY, aabb.minZ).tex(0, 0).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).tex(0, 1).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.maxY, aabb.maxZ).tex(1, 1).endVertex();

		vertexbuffer.pos(aabb.maxX, aabb.minY, aabb.minZ).tex(1, 0).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.minY, aabb.minZ).tex(0, 0).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.minY, aabb.maxZ).tex(0, 1).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.minY, aabb.maxZ).tex(1, 1).endVertex();

		vertexbuffer.pos(aabb.minX, aabb.minY, aabb.minZ).tex(1, 0).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.minY, aabb.minZ).tex(0, 0).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.maxY, aabb.minZ).tex(0, 1).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.maxY, aabb.minZ).tex(1, 1).endVertex();

		vertexbuffer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).tex(1, 0).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.maxY, aabb.maxZ).tex(0, 0).endVertex();
		vertexbuffer.pos(aabb.minX, aabb.maxY, aabb.maxZ).tex(0, 1).endVertex();
		vertexbuffer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).tex(1, 1).endVertex();
		tessellator.draw();
	}
}
