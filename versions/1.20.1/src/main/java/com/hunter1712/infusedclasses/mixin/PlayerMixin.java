package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.PlayerClassAccess;
import com.hunter1712.infusedclasses.PlayerClasses;
import com.hunter1712.infusedclasses.SwingChargeAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One PlayerClasses slot per player, saved with player data so class survives relogs,
 * plus the swing-charge read the damage hook cannot make for itself. On 1.20.1 the
 * vanilla jump crit is inline in {@code attack}: this zeroes its x1.5 multiplier and
 * cancels both cue methods, so the mod's own crit (rolled in the melee hook) is the
 * only one. The mixin holds zero policy; decisions live in {@code CritRoll} and the
 * settle in {@code DamageSettle}. Mobs are untouched.
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
	private void infusedclasses$read(CompoundTag input, CallbackInfo ci) {
		infusedclasses$classes.readId(input.contains(PlayerClasses.NBT_KEY)
				? input.getString(PlayerClasses.NBT_KEY) : null);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void infusedclasses$write(CompoundTag output, CallbackInfo ci) {
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

	/** Vanilla zeroes the attack-strength ticker inside {@code attack}, so the charge is read at its head. */
	@Inject(method = "attack", at = @At("HEAD"))
	private void infusedclasses$captureSwingCharge(Entity target, CallbackInfo ci) {
		infusedclasses$swingCharge = this.getAttackStrengthScale(0.0F);
	}

	/** The vanilla jump crit's damage bonus is retired: only the mod's own crit multiplies a hit. */
	@ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
	private float infusedclasses$retireVanillaCritBonus(float multiplier) {
		return 1.0F;
	}

	@Inject(method = "crit", at = @At("HEAD"), cancellable = true)
	private void infusedclasses$retireVanillaCritCue(Entity target, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "magicCrit", at = @At("HEAD"), cancellable = true)
	private void infusedclasses$retireVanillaMagicCritCue(Entity target, CallbackInfo ci) {
		ci.cancel();
	}
}
