package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Server event wiring for the entry: first-join picker, respawn copy, logout drop, hit trigger,
 * and the per-tick decay-plus-apply. Decisions stay in pure modules and application in adapters;
 * this only routes between them.
 */
final class ServerEvents {
	private ServerEvents() {
	}

	static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!PlayerClassAccess.of(player).hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClassAccess.of(newPlayer).copyFrom(PlayerClassAccess.of(oldPlayer));
			// Only the choice crosses. The old player's transient state is dropped, and the new
			// player's starts empty, which is what a respawn has always done to it.
			// Load-bearing ordering lives in the runtime store: both players share one UUID.
			InfusedClasses.runtimes().respawn(newPlayer.getUUID());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				InfusedClasses.runtimes().remove(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			long now = System.currentTimeMillis();
			// Melee hook, three jobs: stacks, bonuses, stance mults. The plan decides,
			// adapters apply; this only routes. Settled hurts re-enter this hook
			// on the same thread, guarded so they grant nothing twice.
			if (DamageSettle.isResolving()) {
				return;
			}
			// Attacker: a direct melee hit builds stacks for any class carrying an
			// on-hit-dealt passive — full swings only, spam earns zero — and a full swing,
			// inside a live Rush window with its bonus still pending, lands the engage
			// bonus too. Rend and Impetus bonuses land through the shared charge
			// curve, and Legionary deal/take mults settle as one extra hurt (or a
			// heal-back when the stance discounts). Self-damage excluded by the
			// attacker check, projectiles and spells by the direct check.
			if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
					&& attacker != entity) {
				ClassDefinition atkDef = PlayerClassAccess.of(attacker).currentDefinition();
				if (atkDef != null) {
					PlayerRuntime atkRuntime = InfusedClasses.runtime(attacker);
					float charge = attacker.getAttackStrengthScale(0.0F);
					// One plan, settled in field order: bump, Rush, Rend, Impetus, stance settle.
					// The entry routes; the plan decides and the appliers apply.
					ClassDefinition victimDef = entity instanceof ServerPlayer v
							? PlayerClassAccess.of(v).currentDefinition() : null;
					Supplier<PlayerRuntime> victimRt = entity instanceof ServerPlayer v2
							? () -> InfusedClasses.runtime(v2) : null;
					MeleePlan.HitPlan plan = MeleePlan.planHit(atkDef, atkRuntime, victimDef, victimRt, charge, now);
					if (plan.bump()) {
						atkRuntime.onTrigger(atkDef, ConditionalPassive.ON_HIT_DEALT, now, charge);
					}
					// One-shot flags settle where the plan peeked them, at the same now:
					// the Rush credit grants and the Rend snapshot spends exactly when the
					// plan saw them live, so empty hits never touch a window. Spam grants
					// zero stacks; the pending bonus waits for a full swing.
					PassiveApplier.settleHitOneShots(atkRuntime, atkDef, plan, now);
					// Rend empower: the stamped bonus lands on this hit and only this one,
					// applied through the adapter like every other outcome; the bonus is
					// what the plan already decided, never recomputed.
					if (plan.rendHp() > 0) {
						DamageSettle.dealBonus(attacker, entity, plan.rendHp());
					}
					// Impetus: every melee hit inside the window earns the curve bonus.
					if (plan.impetusBonus() > 0) {
						DamageSettle.dealBonus(attacker, entity, plan.impetusBonus());
					}
					// Stance mults: attacker deal times victim take, settled at once.
					// Scutum + Testudo read x0.56, Gladius + Impetus x1.5: see HitPlan.combined.
					DamageSettle.settleMult(attacker, entity, baseDamageTaken, plan.combined());
				}
			} else if (entity instanceof ServerPlayer victim
					&& source.getEntity() instanceof net.minecraft.world.entity.LivingEntity mobAttacker
					&& !(mobAttacker instanceof ServerPlayer)
					&& source.getDirectEntity() == mobAttacker) {
				// Victim: mob melee on a Legionary still pays the stance take mult
				// (player-vs-player take settles in the attacker branch above).
				// takeFor is 1.0 for non-Legionaries, and the settle is a no-op at 1.0.
				double take = MeleePlan.takeFor(PlayerClassAccess.of(victim).currentDefinition(),
						() -> InfusedClasses.runtime(victim), now);
				if (take != 1.0) {
					DamageSettle.settleMobTake(victim, mobAttacker, baseDamageTaken, take);
				}
			}
		});

		// Server tick: decay conditional passive stacks, then put their effect on the player.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClassAccess.of(player);
				ClassDefinition def = classes.currentDefinition();
				if (def == null) {
					continue;
				}
				PlayerRuntime runtime = InfusedClasses.runtime(player);
				PassiveApplier.apply(player, def, runtime, now);
			}
		});
	}
}
