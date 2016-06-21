package defeatedcrow.hac.core.event;

import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import defeatedcrow.hac.api.magic.CharmType;
import defeatedcrow.hac.api.magic.IJewelCharm;
import defeatedcrow.hac.core.DCLogger;
import defeatedcrow.hac.core.util.DCUtil;

public class LivingHurtDC {

	@SubscribeEvent
	public void onHurt(LivingHurtEvent event) {
		EntityLivingBase living = event.getEntityLiving();
		DamageSource source = event.getSource();
		float newDam = event.getAmount();
		float prev = 0.0F;
		float add = 1.0F;
		if (living != null) {

			// DIFFENCE優先
			// チャームの防御判定から
			if (living instanceof EntityPlayer) {
				EntityPlayer player = (EntityPlayer) living;
				Map<Integer, ItemStack> charms = DCUtil.getPlayerCharm(player, CharmType.DEFFENCE);

				for (Entry<Integer, ItemStack> entry : charms.entrySet()) {
					DCLogger.debugLog("hurt charm");
					IJewelCharm charm = (IJewelCharm) entry.getValue().getItem();
					prev += charm.reduceDamage(source, entry.getValue());
					if (charm.onDiffence(source, player, newDam, entry.getValue())) {
						if (charm.consumeCharmItem(entry.getValue()) == null) {
							player.inventory.setInventorySlotContents(entry.getKey(), entry.getValue());
							player.inventory.markDirty();
						}
					}
				}
			}

			// ATTACK側のチャーム判定
			if (source instanceof EntityDamageSource && source.getEntity() != null
					&& source.getEntity() instanceof EntityPlayer) {
				EntityPlayer attacker = (EntityPlayer) source.getEntity();

				Map<Integer, ItemStack> charms2 = DCUtil.getPlayerCharm(attacker, CharmType.ATTACK);
				for (Entry<Integer, ItemStack> entry : charms2.entrySet()) {
					DCLogger.debugLog("attack charm");
					IJewelCharm charm = (IJewelCharm) entry.getValue().getItem();
					add *= charm.increaceDamage(living, entry.getValue());
					if (charm.onAttacking(attacker, living, newDam - prev, entry.getValue())) {
						if (charm.consumeCharmItem(entry.getValue()) == null) {
							attacker.inventory.setInventorySlotContents(entry.getKey(), entry.getValue());
							attacker.inventory.markDirty();
						}
					}
				}
			}

			// 最終的なダメージ
			newDam *= add;
			newDam -= prev;

			// 最終的に
			if (newDam < 0.5F) {
				event.setAmount(0F);
				event.setCanceled(true);
			} else {
				event.setAmount(newDam);
			}
			// DCLogger.debugLog("last climate dam:" + newDam);
		}
	}

}
