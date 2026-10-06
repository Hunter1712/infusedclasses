package com.hunter1712.infusedclasses.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

/**
 * Iconless effects stay iconless in inventories too. Vanilla's HUD strip skips
 * {@code showIcon=false} effects, but its inventory list renders every active effect.
 * War Cry's Strength carries the damage while the War Cry row carries the display, so the
 * vanilla Strength box would duplicate the row. Filtered here, the effect keeps working —
 * milk still clears it, a beacon's Strength still shows and still stacks under it — while
 * only the War Cry row shows for our own uptime.
 */
@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
	@ModifyExpressionValue(method = "render",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
	private Collection<MobEffectInstance> infusedclasses$hideIconless(Collection<MobEffectInstance> original) {
		return original.stream().filter(MobEffectInstance::showIcon).toList();
	}
}
