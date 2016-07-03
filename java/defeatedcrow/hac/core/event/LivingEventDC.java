package defeatedcrow.hac.core.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import defeatedcrow.hac.api.climate.ClimateAPI;
import defeatedcrow.hac.api.climate.DCHeatTier;
import defeatedcrow.hac.api.climate.IClimate;
import defeatedcrow.hac.api.damage.DamageAPI;
import defeatedcrow.hac.api.damage.DamageSourceClimate;
import defeatedcrow.hac.api.magic.CharmType;
import defeatedcrow.hac.api.magic.IJewelCharm;
import defeatedcrow.hac.api.recipe.IClimateSmelting;
import defeatedcrow.hac.api.recipe.RecipeAPI;
import defeatedcrow.hac.config.CoreConfigDC;
import defeatedcrow.hac.core.ClimateCore;
import defeatedcrow.hac.core.packet.HaCPacket;
import defeatedcrow.hac.core.packet.MessageCharmKey;
import defeatedcrow.hac.core.util.DCPotion;
import defeatedcrow.hac.core.util.DCTimeHelper;
import defeatedcrow.hac.core.util.DCUtil;

// 常時監視系
public class LivingEventDC {

	@SubscribeEvent
	public void onEvent(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase living = event.getEntityLiving();
		if (living instanceof EntityPlayer) {
			this.onPlayerUpdate(event);
			if (!living.worldObj.isRemote) {
				this.playerChunkUpdate(event);
			} else {
				this.onPlayerKeyUpdate(event);
			}
		}
		this.onLivingUpdate(event);
	}

	public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase living = event.getEntityLiving();

		/* Potion */
		ArrayList<PotionEffect> potions = new ArrayList<PotionEffect>();

		if (living != null) {

			if (!living.worldObj.isRemote) {
				boolean f = true;
				if (living instanceof EntityLiving && ((EntityLiving) living).hasCustomName()) {

				} else {
					if (living instanceof IMob) {
						f = false;
					} else if (living.getLowestRidingEntity() != null && living.getLowestRidingEntity() instanceof IMob) {
						f = false;
					} else if (living.getRidingEntity() != null && living.getRidingEntity() instanceof IMob) {
						f = false;
					}
				}

				if (f) {
					// PotionEffectのリスト
					Iterator iterator = living.getActivePotionEffects().iterator();

					while (iterator.hasNext()) {
						PotionEffect effect = (PotionEffect) iterator.next();

						Potion potion = effect.getPotion();

						if (potion != null && potion == DCPotion.jump) {
							living.fallDistance = 0.0F;
						}

						// 騎乗関係のMobにポーション効果を分け与える
						if (living.getRidingEntity() != null && living.getRidingEntity() instanceof EntityLivingBase) {
							EntityLivingBase riding = (EntityLivingBase) event.getEntity().getRidingEntity();
							if (potion != null) {
								riding.addPotionEffect(effect);
							}
						}

					}
				}

				/* climate damage */

				if (!living.worldObj.isRemote && DCTimeHelper.getCount(living.worldObj) == 0 && CoreConfigDC.climateDam) {
					int px = MathHelper.floor_double(living.posX);
					int py = MathHelper.floor_double(living.posY) + 1;
					int pz = MathHelper.floor_double(living.posZ);
					DCHeatTier heat = ClimateAPI.calculator.getAverageTemp(living.worldObj, new BlockPos(px, py, pz),
							2, false);

					float prev = 1.0F * (2 - CoreConfigDC.damageDifficulty); // 0F ~ 2.0F
					float dam = heat.getTier() * 1.0F; // hot 0F ~ 6.0F / cold 0F ~ 6.0F
					boolean isCold = heat.getTier() < 0;

					if (isCold) {
						dam *= -2.0F;
					}

					dam -= prev;

					// ピースフルではダメージがない
					if (living.worldObj.getDifficulty() == EnumDifficulty.PEACEFUL && !CoreConfigDC.peacefulDam) {
						dam = 0.0F;
					}

					/* damage判定 */
					// 防具の計算
					Iterable<ItemStack> items = living.getArmorInventoryList();
					if (items != null) {
						for (ItemStack item : items) {
							if (item != null && item.getItem() instanceof ItemArmor) {
								ArmorMaterial mat = ((ItemArmor) item.getItem()).getArmorMaterial();
								prev += DamageAPI.armorRegister.getPreventAmount(mat);
								if (!isCold
										&& EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, item) > 0) {
									prev += EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, item) * 1.0F;
								}
							}
						}
					}

					// mobごとの特性
					if (isCold) {
						if (living.isImmuneToFire()) {
							prev -= 2.0F;
						}
						if (living.isEntityUndead()) {
							prev += 1.0F;
						}

					} else {
						if (living.isPotionActive(DCPotion.fire_reg)) {
							prev += 2.0F;
						}
						if (living.isImmuneToFire()) {
							prev += 2.0F;
						} else if (living.isEntityUndead()) {
							prev -= 1.0F;
						}
					}

					// 村人補正
					if (living instanceof EntityVillager || living instanceof EntityGolem) {
						prev += 2.0F;
					} else if (living instanceof EntityAnimal) {
						prev += 1.0F;
					}

					// boss
					if (living instanceof EntityDragon || living instanceof EntityWither) {
						prev += 2.0F;
					}

					dam -= prev;

					// 2.0F未満の場合はとどめを刺さない
					if (dam < 2.0F && living.getHealth() < 1.0F) {
						dam = 0.0F;
					}

					if (dam >= 1.0F) {
						if (isCold) {
							living.attackEntityFrom(DamageSourceClimate.climateColdDamage, dam);
							// DCLogger.debugLog("cold dam:" + dam);
						} else {
							living.attackEntityFrom(DamageSourceClimate.climateHeatDamage, dam);
							// DCLogger.debugLog("heat dam:" + dam);
						}
					}
				}
			}
		}
	}

	public void onPlayerUpdate(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase entity = event.getEntityLiving();

		if ((entity instanceof EntityPlayer)) {
			EntityPlayer player = (EntityPlayer) event.getEntity();
			// 装備
			ItemStack[] equip = player.inventory.armorInventory;
			ItemStack[] inside = new ItemStack[9];
			for (int i = 0; i < 9; i++) {
				inside[i] = player.inventory.getStackInSlot(i + 9);
			}

			// charm
			if (!player.worldObj.isRemote) {
				Map<Integer, ItemStack> charms = DCUtil.getPlayerCharm(player, CharmType.CONSTANT);
				for (ItemStack item2 : charms.values()) {
					int m = item2.getMetadata();
					IJewelCharm jew = (IJewelCharm) item2.getItem();
					jew.constantEffect(player, item2);
				}
			}

		}
	}

	private boolean x_key = false;

	@SideOnly(Side.CLIENT)
	public void onPlayerKeyUpdate(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase entity = event.getEntityLiving();

		if ((entity instanceof EntityPlayer)) {
			EntityPlayer player = (EntityPlayer) event.getEntity();
			if (ClimateCore.proxy.isWarpKeyDown()) {
				if (!x_key) {
					x_key = true;
					player.playSound(Blocks.GLASS.getSoundType().getBreakSound(), 1.0F, 0.75F);
					HaCPacket.INSTANCE.sendToServer(new MessageCharmKey((byte) 1));
				}
			} else {
				x_key = false;
			}
		}
	}

	private int localCount = 0;

	// Block Update をプレイヤーに肩代わりさせる
	public void playerChunkUpdate(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase entity = event.getEntityLiving();

		if ((entity instanceof EntityPlayer)) {
			EntityPlayer player = (EntityPlayer) event.getEntity();
			World world = player.worldObj;
			int count = DCTimeHelper.getCount2(world);

			int tick = (count >> 4) & 15;

			if (tick != localCount) {
				localCount = tick;
				// 3回やる
				int i = 0;
				while (i < CoreConfigDC.updateFrequency) {
					int cx = player.chunkCoordX - 4 + world.rand.nextInt(9);
					int cz = player.chunkCoordZ - 4 + world.rand.nextInt(9);
					if (world.getChunkFromChunkCoords(cx, cz).isLoaded()) {
						int j = 0;
						while (j < CoreConfigDC.updateFrequency) {
							int x = (cx << 4) + world.rand.nextInt(16);
							int z = (cz << 4) + world.rand.nextInt(16);
							int y = world.provider.getActualHeight();

							BlockPos under = new BlockPos(x, 1, z);
							BlockPos upper = new BlockPos(x, y, z);
							Iterable<BlockPos> itr = under.getAllInBox(under, upper);
							for (BlockPos pos : itr) {
								if (world.rand.nextBoolean())
									continue;
								if (world.isAirBlock(pos)) {
									continue;
								}
								IBlockState state = world.getBlockState(pos);
								Block block = state.getBlock();
								int meta = block.getMetaFromState(state);
								IClimate clm = ClimateAPI.calculator.getClimate(world, pos, new int[] {
										2,
										1,
										1 });
								IClimateSmelting recipe = RecipeAPI.registerSmelting.getRecipe(clm, new ItemStack(
										block, 1, meta));
								if (recipe == null || !recipe.matchClimate(clm) || recipe.hasPlaceableOutput() != 1)
									continue;

								if (recipe.getOutput() != null && recipe.getOutput().getItem() instanceof ItemBlock) {
									Block retB = Block.getBlockFromItem(recipe.getOutput().getItem());
									int retM = recipe.getOutput().getMetadata();
									IBlockState ret = retB.getStateFromMeta(retM);
									world.setBlockState(pos, ret, 3);
									world.notifyBlockOfStateChange(pos, ret.getBlock());
								}
							}
							j++;
						}
					}
					i++;
				}

			}
		}
	}
}
