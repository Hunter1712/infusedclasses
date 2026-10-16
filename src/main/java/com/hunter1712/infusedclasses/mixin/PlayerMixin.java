package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.PlayerClassAccess;
import com.hunter1712.infusedclasses.PlayerClasses;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * One PlayerClasses slot per player, saved with player data so class survives relogs,
 * plus the vanilla-crit retirement: {@code canCriticalAttack} is the single 1.21.11
 * gate for the falling-swing x1.5, the crit sound and the crit particles, and this
 * returns false for every player. The mixin holds zero policy; the mod's crits live
 * in {@code CritRoll} and settle through the melee hook. Mobs are untouched.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerClassAccess {
	@Unique
	private final PlayerClasses infusedclasses$classes = new PlayerClasses();

	@Override
	public PlayerClasses classes() {
		return infusedclasses$classes;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void infusedclasses$read(ValueInput input, CallbackInfo ci) {
		infusedclasses$classes.readId(input.getString(PlayerClasses.NBT_KEY).orElse(null));
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void infusedclasses$write(ValueOutput output, CallbackInfo ci) {
		String id = infusedclasses$classes.writeId();
		if (id != null) {
			output.putString(PlayerClasses.NBT_KEY, id);
		}
	}

	@Inject(method = "canCriticalAttack", at = @At("HEAD"), cancellable = true)
	private void infusedclasses$retireVanillaCrits(Entity target, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
