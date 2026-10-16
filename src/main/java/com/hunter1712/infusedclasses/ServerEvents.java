package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Server event wiring for the entry: intent receivers, first-join picker, respawn copy,
 * logout drop, hit trigger, and the per-tick decay-plus-apply. Decisions stay in pure modules
 * and application in adapters; this only routes between them. The oath switch stays explicit
 * here per ADR-0009: it moved home, it did not shrink.
 */
final class ServerEvents {
	private ServerEvents() {
	}

	static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!PlayerClassAccess.of(player).hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload(""));
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
			// Attacker and victim melee settle below in field order; this only routes.
			if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
					&& attacker != entity) {
				settleAttackerHit(attacker, entity, baseDamageTaken, now);
			} else if (entity instanceof ServerPlayer victim
					&& source.getEntity() instanceof net.minecraft.world.entity.LivingEntity mobAttacker
					&& !(mobAttacker instanceof ServerPlayer)
					&& source.getDirectEntity() == mobAttacker) {
				// Victim: mob melee on a Legionary still pays the stance take mult
				// (player-vs-player take settles in the attacker branch above).
				// takeFor is 1.0 for non-Legionaries, and the settle is a no-op at 1.0.
				double take = MeleePlan.takeFor(PlayerClassAccess.of(victim).currentDefinition(),
						() -> InfusedClasses.runtime(victim), now);
				DamageSettle.settleTake(victim, baseDamageTaken, take,
						() -> mobAttacker.damageSources().mobAttack(mobAttacker));
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

	/**
	 * Intent receivers: what a reused bind means per class plus the picker switch path.
	 * Lives here with the hit sequence and the tick fan-out so all routing has one home;
	 * the initializer keeps payload registration and delegates.
	 */
	static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition oldDef = classes.currentDefinition();
					ClassDefinition def = classes.trySetClass(payload.classId());
					if (def == null) {
						AbilityApplier.announce(player, ChatLine.unknownOrLocked(), false);
						return;
					}
					InfusedClasses.onClassChanged(player, oldDef, def);
					context.responseSender().sendPacket(new ClassConfirmedPayload());
					AbilityApplier.announce(player, ChatLine.classPicked(def.displayName()), false);
				}));

		ServerPlayNetworking.registerGlobalReceiver(RushPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.currentDefinition();
					PlayerRuntime runtime = InfusedClasses.runtime(player);
					// Reused R bind: the wheel for Gamblers, the rotate intent for Legionaries,
					// Rush for everyone else, so binds survive the class.
					// Rotate announces without fanfare on purpose: it carries no cast sound.
					switch (BindMorph.rushFor(def)) {
						case ROTATE -> {
							RotateAbility.Result result = RotateAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == RotateAbility.Kind.FIRED, null, true);
						}
						case ROULETTE -> {
							RouletteAbility.Result result =
									RouletteAbility.fire(def, runtime, now, player.getRandom().nextFloat());
							RouletteApplier.apply(player, result);
						}
						case RUSH -> {
							RushAbility.Result result = RushAbility.fire(def, runtime, now);
							RushApplier.apply(player, result);
						}
					}
				}));

		ServerPlayNetworking.registerGlobalReceiver(RendPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.currentDefinition();
					PlayerRuntime runtime = InfusedClasses.runtime(player);
					// Reused G bind: All In for Gamblers, the oath ultimate for Legionaries,
					// Rend for everyone else.
					switch (BindMorph.oathFor(def)) {
						case AEQUILIBRIUM -> {
							AequilibriumAbility.Result result = AequilibriumAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == AequilibriumAbility.Kind.FIRED, AequilibriumAbility.CAST_SOUND, true);
						}
						case ALL_IN -> {
							AllInAbility.Result result = AllInAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == AllInAbility.Kind.FIRED, AllInAbility.CAST_SOUND, true);
						}
						case REND -> {
							RendAbility.Result result = RendAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == RendAbility.Kind.FIRED, RendAbility.CAST_SOUND, true);
							// The adapter spends only the priced passive; a fizzle spends nothing.
							// The empower damage lands on the next melee hit inside the window.
							if (result.consume()) {
								PassiveApplier.consumeSpent(player, runtime, def);
							}
						}
					}
				}));
	}

	/**
	 * One attacker melee hit, settled in field order: plan, settle (bump, dice movement,
	 * Rush credit, Impetus spend, Rend spend), dice cue, Rend bonus, stance settle. The
	 * sequence lives here so the hook cannot reorder or half-apply it; the plan decides
	 * and the appliers apply. A direct melee hit builds stacks for any class carrying an
	 * on-hit-dealt passive — full swings only, spam earns zero — and a full swing inside
	 * a live Rush window with its bonus still pending lands the engage bonus too.
	 * Classless players settle the universal crit and nothing else: the entry draws one
	 * roll per landed full-charge hit for every player, classless included, and sweeps
	 * draw per swept target. Self-damage excluded by the attacker check, projectiles and
	 * spells by the direct check.
	 */
	private static void settleAttackerHit(ServerPlayer attacker, net.minecraft.world.entity.Entity entity,
			float baseDamageTaken, long now) {
		ClassDefinition atkDef = PlayerClassAccess.of(attacker).currentDefinition();
		PlayerRuntime atkRuntime = InfusedClasses.runtime(attacker);
		float charge = attacker.getAttackStrengthScale(0.0F);
		// The dice are the mod's, not the class's: every landed full-charge hit draws
		// its own roll; half swings never roll and so never draw one.
		float roll = BonusCurve.isFullSwing(charge) ? attacker.getRandom().nextFloat() : 0.0F;
		ClassDefinition victimDef = entity instanceof ServerPlayer v
				? PlayerClassAccess.of(v).currentDefinition() : null;
		Supplier<PlayerRuntime> victimRt = entity instanceof ServerPlayer v2
				? () -> InfusedClasses.runtime(v2) : null;
		MeleePlan.HitPlan plan = MeleePlan.planHit(atkDef, atkRuntime, victimDef, victimRt, charge, now, roll);
		// The settle returns the stamped Rend bonus to land, so this lands what it spent.
		// Spam grants zero stacks; the pending bonus waits for a full swing.
		// Empty bonuses settle as no-ops in the adapter, so no pre-checks here.
		float rendHp = PassiveApplier.settleHit(atkRuntime, atkDef, plan, now);
		// The verdict's cue lands once per hit, classless included: a crit replays the
		// vanilla particles and sound, a betrayal gets its own crack.
		CritCue.apply(attacker, entity, plan.dice().verdict());
		// Rend empower: the stamped bonus lands on this hit and only this one,
		// applied through the adapter like every other outcome; the bonus is
		// what the plan already decided, never recomputed.
		DamageSettle.dealBonus(attacker, entity, rendHp);
		// Stance and dice mults: attacker deal times victim take times the dice mult,
		// settled at once. Gladius + Impetus reads x1.1 deal, Scutum + Testudo x0.85
		// take, a crit x1.25: see HitPlan.combined.
		DamageSettle.settleTake(entity, baseDamageTaken, plan.combined(),
				() -> attacker.damageSources().playerAttack(attacker));
	}
}
