package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.PlayerClassAccess;
import com.hunter1712.infusedclasses.PlayerClasses;
import com.hunter1712.infusedclasses.SwingChargeAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * One PlayerClasses slot per player, saved with player data so class survives relogs,
 * plus two Player reads the server entry cannot make for itself. The vanilla-crit
 * retirement: {@code canCriticalAttack} is the single 1.21.11 gate for the
 * falling-swing x1.5, the crit sound and the crit particles, and this returns false
 * for every player. The swing charge: vanilla zeroes the attack-strength ticker at
 * the top of {@code onAttack}, before the hit lands, so the damage hook would always
 * read zero; this records the value just above that reset. The mixin holds zero
 * policy; the mod's crits live in {@code CritRoll} and settle through the melee hook.
 * Mobs are untouched.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerClassAccess, SwingChargeAccess {
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

	@Unique
	private float infusedclasses$swingCharge;

	@Shadow
	public abstract float getAttackStrengthScale(float f);

	@Override
	public float swingCharge() {
		return infusedclasses$swingCharge;
	}

	@Inject(method = "onAttack", at = @At("HEAD"))
	private void infusedclasses$captureSwingCharge(CallbackInfo ci) {
		infusedclasses$swingCharge = this.getAttackStrengthScale(0.0F);
	}

	@Inject(method = "canCriticalAttack", at = @At("HEAD"), cancellable = true)
	private void infusedclasses$retireVanillaCrits(Entity target, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
