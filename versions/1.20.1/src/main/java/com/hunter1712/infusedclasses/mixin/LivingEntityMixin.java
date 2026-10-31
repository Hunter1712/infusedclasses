package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.MeleeHook;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1's Fabric API has no after-damage event, so the melee hook fires here instead:
 * at the return of {@code hurt}, on the server, for a hit that applied damage and left
 * the entity alive. A lethal hit is left to {@code ServerLivingEntityEvents.AFTER_DEATH},
 * matching the after-damage contract the other versions get from Fabric. The amount at
 * return is the shield-reduced, pre-armor value the hook expects.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "hurt", at = @At("RETURN"))
	private void infusedclasses$afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity living = (LivingEntity) (Object) this;
		if (!cir.getReturnValueZ() || living.level().isClientSide() || living.isDeadOrDying()) {
			return;
		}
		MeleeHook.afterDamage(living, source, amount, amount, false);
	}
}
