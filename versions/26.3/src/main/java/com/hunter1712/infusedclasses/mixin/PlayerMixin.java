package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.PlayerClassAccess;
import com.hunter1712.infusedclasses.PlayerClasses;
import com.hunter1712.infusedclasses.SwingChargeAccess;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * One PlayerClasses slot per player, saved with player data so class survives relogs,
 * plus the swing-charge read the damage hook cannot make for itself. On 26.3 the
 * vanilla jump crit asks the private {@code canCriticalAttack} inside {@code attack};
 * this zeroes its x1.5 multiplier and cancels both cue methods, so the mod's own crit
 * (rolled in the melee hook) is the only one. The mixin holds zero policy; decisions
 * live in {@code CritRoll} and the settle in {@code DamageSettle}. Mobs are untouched.
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

	/**
	 * The swing charge: vanilla zeroes the attack-strength ticker inside {@code onAttack},
	 * before the hit lands, so the ticker the hit is about to use is read at its head.
	 * The stab route (spear thrusts, mace smashes) damages inside {@code stabAttack}
	 * without a normal swing's own read, so the ticker is captured there too.
	 */
	@Inject(method = "onAttack", at = @At("HEAD"))
	private void infusedclasses$captureSwingCharge(CallbackInfo ci) {
		infusedclasses$swingCharge = this.getAttackStrengthScale(0.0F);
	}

	@Inject(method = "stabAttack", at = @At("HEAD"))
	private void infusedclasses$captureStabCharge(
			net.minecraft.world.entity.EquipmentSlot slot, Entity target, float damage,
			boolean flag1, boolean flag2, boolean flag3, CallbackInfoReturnable<Boolean> cir) {
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
