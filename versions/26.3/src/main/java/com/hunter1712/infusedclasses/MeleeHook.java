package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/**
 * The melee hook: the one AFTER_DAMAGE body mapping a landed hit to the plan and the
 * settle, plus the AFTER_DEATH body for a killing blow (AFTER_DAMAGE does not fire for
 * one). The entry wires the events; this routes the hit — attacker or victim — and lands
 * exactly what {@link MeleePlan} returned. Decisions stay in the plan and the settle;
 * this holds the branch order and the guard read.
 */
final class MeleeHook {
	/** A kill always took positive damage; the plan only gates on that sign, not the amount. */
	private static final float LETHAL_DAMAGE = 1.0F;

	private MeleeHook() {
	}

	static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken,
			float damageTaken, boolean blocked) {
		long now = InfusedClasses.now();
		// Melee hook, three jobs: stacks, bonuses, stance mults. The plan decides,
		// adapters apply; this only routes. Settled hurts re-enter this hook
		// on the same thread, guarded so they grant nothing twice. `damageTaken`
		// is what the hit took after shield and flat reductions (the damage hook
		// reports it before armor); the settle turns it into what landed. A hit
		// that took none earns nothing, so the branches settle from that value only.
		if (DamageSettle.isResolving()) {
			return;
		}
		// Attacker and victim melee settle below in field order; this only routes.
		ServerPlayer attacker = playerMeleeAttacker(entity, source);
		if (attacker != null) {
			resolveAttackerHit(attacker, entity, damageTaken, now, false);
		} else if (entity instanceof ServerPlayer victim
				&& source.getEntity() instanceof LivingEntity mobAttacker
				&& !(mobAttacker instanceof ServerPlayer)
				&& source.getDirectEntity() == mobAttacker) {
			// Victim: mob melee on a Legionary still pays the stance take mult
			// (player-vs-player take settles in the attacker branch above).
			// takeFor is 1.0 for non-Legionaries, and the settle is a no-op at 1.0.
			ClassDefinition victimDef = PlayerClassAccess.of(victim).currentDefinition();
			double take = MeleePlan.takeFor(victimDef,
					() -> InfusedClasses.runtime(victim), now);
			DamageSettle.settleTake(victim, damageTaken, take,
					() -> mobAttacker.damageSources().mobAttack(mobAttacker));
			// The struck player's own bar feeds here too: a melee hit taken is the
			// same earning event as one dealt, and the same zero-damage gate applies.
			MeleePlan.resolveTaken(victimDef, () -> InfusedClasses.runtime(victim), damageTaken, now);
		}
	}

	/**
	 * A killing blow: AFTER_DAMAGE never fires for it, so this is the same attacker-side
	 * plan with a positive amount and no victim. The state moves and the crit cue land;
	 * the settle and the victim feed do not, because the target is already dead and
	 * ADR-0012 keeps the extra damage off a kill. The resolve guard covers a bonus settle
	 * that itself kills: the base hit already moved the same state.
	 */
	static void afterDeath(LivingEntity entity, DamageSource source) {
		if (DamageSettle.isResolving()) {
			return;
		}
		ServerPlayer attacker = playerMeleeAttacker(entity, source);
		if (attacker != null) {
			resolveAttackerHit(attacker, entity, LETHAL_DAMAGE, InfusedClasses.now(), true);
		}
	}

	/** The attacker shape both hooks share: a direct player melee hit on someone else. */
	private static ServerPlayer playerMeleeAttacker(LivingEntity entity, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer attacker
				&& source.getDirectEntity() == attacker
				&& attacker != entity) {
			return attacker;
		}
		return null;
	}

	/**
	 * One attacker melee hit: the plan resolves the whole hit in field order (deciding,
	 * then settling the bump, dice movement, Impetus spend and Onslaught price on the
	 * runtime), and this entry lands exactly what the resolve returned — the verdict's
	 * cue, the combined mult (chassis, stance, wheel, oath, window and dice), and the
	 * victim's own taken feed. The sequence lives behind the seam so the hook cannot
	 * reorder or half-apply it. A direct melee hit builds stacks for any class carrying
	 * an on-hit-dealt bar — full swings only, spam earns zero, and a hit that took no
	 * damage earns and spends nothing — and a hit inside a live Onslaught window pays the
	 * ultimate's mult instead of feeding, its price running on full swings. Classless
	 * players settle the universal crit and nothing else: the entry draws one roll per
	 * landed full-charge hit for every player, classless included, and sweeps draw per
	 * swept target. Self-damage excluded by the attacker check, projectiles and spells by
	 * the direct check. A lethal hit skips only the victim feed and the settle.
	 */
	private static void resolveAttackerHit(ServerPlayer attacker, Entity entity, float damageTaken, long now,
			boolean lethal) {
		ClassDefinition atkDef = PlayerClassAccess.of(attacker).currentDefinition();
		// Classless hits settle the universal dice and cost no transient state (ADR-0005):
		// the plan and the settle both read a null runtime as "no abilities to move".
		PlayerRuntime atkRuntime = atkDef == null ? null : InfusedClasses.runtime(attacker);
		// Captured at swing time, not read here: vanilla zeroes the attack-strength
		// ticker in onAttack() before the hit lands, so a read from this hook is always
		// zero and every full-swing gate below it was dead until the capture landed.
		float charge = SwingChargeAccess.of(attacker);
		// The dice are the mod's, not the class's: every landed full-charge hit draws
		// its own roll; half swings never roll and so never draw one.
		float roll = FullSwing.isFull(charge) ? attacker.getRandom().nextFloat() : 0.0F;
		ClassDefinition victimDef = !lethal && entity instanceof ServerPlayer v
				? PlayerClassAccess.of(v).currentDefinition() : null;
		Supplier<PlayerRuntime> victimRt = !lethal && entity instanceof ServerPlayer v2
				? () -> InfusedClasses.runtime(v2) : null;
		// The plan resolves the whole hit: it decides in field order and settles the
		// runtime movements in the same order, returning exactly what this entry lands.
		MeleePlan.Landed landed = MeleePlan.resolve(atkDef, atkRuntime, victimDef, victimRt, charge, damageTaken, now, roll);
		// Spam grants zero stacks; the ultimate's price waits for a full swing.
		// The verdict's cue lands once per hit, classless included: a crit replays the
		// vanilla cue, a betrayal gets its own crack.
		CritCue.apply(attacker, entity, landed.verdict());
		if (lethal) {
			return;
		}
		// The victim's own bar: a player struck by a player feeds ON_HIT_TAKEN here
		// (mob strikes feed it in the branch above), under the same landed gate.
		MeleePlan.resolveTaken(victimDef, victimRt, damageTaken, now);
		// Every attacker-side mult folds into combined: chassis, stance, wheel, oath,
		// window and dice, times the victim's take; the settle turns what the hook
		// reported into what landed and scales that once. Gladius + Impetus reads x1.1
		// deal, Scutum + Testudo x0.85 take, a crit x1.25: see HitPlan.combined. A hit
		// that took no damage lands 0 x mult, which is nothing.
		DamageSettle.settleTake(entity, damageTaken, landed.combined(),
				() -> attacker.damageSources().playerAttack(attacker));
	}
}
