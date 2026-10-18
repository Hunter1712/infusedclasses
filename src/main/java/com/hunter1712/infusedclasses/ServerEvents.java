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
			// One forced snapshot on join, classless included: the client starts from a
			// known state instead of guessing until the first change point.
			StatusSync.sendNow(player, System.currentTimeMillis());
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClassAccess.of(newPlayer).copyFrom(PlayerClassAccess.of(oldPlayer));
			// Only the choice crosses. The old player's transient state is dropped, and the new
			// player's starts empty, which is what a respawn has always done to it.
			// Load-bearing ordering lives in the runtime store: both players share one UUID.
			InfusedClasses.runtimes().respawn(newPlayer.getUUID());
			StatusSync.forget(newPlayer.getUUID());
			StatusSync.sendNow(newPlayer, System.currentTimeMillis());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			InfusedClasses.runtimes().remove(handler.getPlayer().getUUID());
			StatusSync.forget(handler.getPlayer().getUUID());
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			long now = System.currentTimeMillis();
			// Melee hook, three jobs: stacks, bonuses, stance mults. The plan decides,
			// adapters apply; this only routes. Settled hurts re-enter this hook
			// on the same thread, guarded so they grant nothing twice. `damageTaken`
			// is what the hit took after shield and modifier reductions; a hit that
			// took none earns nothing, so the branches settle from that value only.
			if (DamageSettle.isResolving()) {
				return;
			}
			// Attacker and victim melee settle below in field order; this only routes.
			if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
					&& attacker != entity) {
				settleAttackerHit(attacker, entity, damageTaken, now);
			} else if (entity instanceof ServerPlayer victim
					&& source.getEntity() instanceof net.minecraft.world.entity.LivingEntity mobAttacker
					&& !(mobAttacker instanceof ServerPlayer)
					&& source.getDirectEntity() == mobAttacker) {
				// Victim: mob melee on a Legionary still pays the stance take mult
				// (player-vs-player take settles in the attacker branch above).
				// takeFor is 1.0 for non-Legionaries, and the settle is a no-op at 1.0.
				double take = MeleePlan.takeFor(PlayerClassAccess.of(victim).currentDefinition(),
						() -> InfusedClasses.runtime(victim), now);
				DamageSettle.settleTake(victim, damageTaken, take,
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
				// One transient read for the tick: the applier reconciles from it and the
				// snapshot is rebuilt from it, so a tick reads state once per player.
				PlayerRuntime.State state = runtime.state(now);
				PassiveApplier.apply(player, def, runtime, state, now);
				// The one steady-state emission point: a tick that moved a lane, a window
				// or a stack tells the client; a quiet tick rebuilds and compares for free.
				StatusSync.maybeSend(player, def, runtime, state, now);
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
					ClassDefinition def = ClassSwitch.trySwitch(player, payload.classId());
					if (def == null) {
						return;
					}
					context.responseSender().sendPacket(new ClassConfirmedPayload());
				}));

		ServerPlayNetworking.registerGlobalReceiver(RushPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> press(context.player(), ServerEvents::pressSignature)));

		ServerPlayNetworking.registerGlobalReceiver(RendPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> press(context.player(), ServerEvents::pressUltimate)));
	}

	/** One slot press: the class read, the classless bail and the runtime lookup are the one path both binds share. */
	private static void press(ServerPlayer player, SlotPress slot) {
		long now = System.currentTimeMillis();
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		// A classless press can only reject, silently, so it costs no state
		// (ADR-0005): the runtime lookup below runs for a classed player only.
		if (def == null) {
			return;
		}
		slot.land(player, def, InfusedClasses.runtime(player), now);
	}

	/** What one slot's press lands once the class is known: the target's ability plus its adapter. */
	private interface SlotPress {
		void land(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now);
	}

	/**
	 * The signature slot: the reused bind's target fires and its landing runs, one line per
	 * arm. The arms stay explicit here per ADR-0009: this routes, the abilities decide.
	 */
	private static void pressSignature(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		// Reused signature bind: the wheel for Gamblers, the rotate intent for Legionaries,
		// Rush for everyone else, so binds survive the class.
		switch (BindMorph.signatureFor(def)) {
			// Rotate announces without fanfare on purpose: it carries no cast sound.
			case ROTATE -> {
				RotateAbility.Result result = RotateAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.outcome().announcement(),
						result.outcome().fired(), null, true);
			}
			case ROULETTE -> {
				RouletteAbility.Result result =
						RouletteAbility.fire(def, runtime, now, player.getRandom().nextFloat());
				AbilityApplier.announceAndCast(player, result.outcome().announcement(),
						result.outcome().fired(), RouletteAbility.CAST_SOUND, true);
			}
			case RUSH -> RushApplier.apply(player, RushAbility.fire(def, runtime, now));
		}
	}

	/**
	 * The ultimate slot: the reused bind's target fires and its landing runs, one line per
	 * arm, the arms explicit here for the same reason.
	 */
	private static void pressUltimate(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		// Reused ultimate bind: All In for Gamblers, the oath ultimate for Legionaries,
		// Rend for everyone else.
		switch (BindMorph.ultimateFor(def)) {
			case AEQUILIBRIUM -> {
				AbilityOutcome result = AequilibriumAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(),
						result.fired(), AequilibriumAbility.CAST_SOUND, true);
			}
			case ALL_IN -> {
				AbilityOutcome result = AllInAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(),
						result.fired(), AllInAbility.CAST_SOUND, true);
			}
			case REND -> RendApplier.apply(player, def, runtime, RendAbility.fire(def, runtime, now));
		}
	}

	/**
	 * One attacker melee hit: the plan resolves the whole hit in field order (deciding,
	 * then settling the bump, dice movement, Rush credit, Impetus spend and Rend spend
	 * on the runtime), and this entry lands exactly what the resolve returned — the
	 * verdict's cue, the Rend bonus, the stance/dice settle. The sequence lives behind
	 * the seam so the hook cannot reorder or half-apply it. A direct melee hit builds
	 * stacks for any class carrying an on-hit-dealt passive — full swings only, spam
	 * earns zero, and a hit that took no damage earns and spends nothing — and a full
	 * swing inside a live Rush window with its bonus still pending lands the engage
	 * bonus too. Classless players settle the universal crit and nothing else: the
	 * entry draws one roll per landed full-charge hit for every player, classless
	 * included, and sweeps draw per swept target. Self-damage excluded by the attacker
	 * check, projectiles and spells by the direct check.
	 */
	private static void settleAttackerHit(ServerPlayer attacker, net.minecraft.world.entity.Entity entity,
			float landedDamage, long now) {
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
		float roll = BonusCurve.isFullSwing(charge) ? attacker.getRandom().nextFloat() : 0.0F;
		ClassDefinition victimDef = entity instanceof ServerPlayer v
				? PlayerClassAccess.of(v).currentDefinition() : null;
		Supplier<PlayerRuntime> victimRt = entity instanceof ServerPlayer v2
				? () -> InfusedClasses.runtime(v2) : null;
		// The plan resolves the whole hit: it decides in field order and settles the
		// runtime movements in the same order, returning exactly what this entry lands.
		MeleePlan.Landed landed = MeleePlan.resolve(atkDef, atkRuntime, victimDef, victimRt, charge, landedDamage, now, roll);
		// Spam grants zero stacks; the pending bonus waits for a full swing.
		// Empty bonuses settle as no-ops in the adapter, so no pre-checks here.
		// The verdict's cue lands once per hit, classless included: a crit replays the
		// vanilla particles and sound, a betrayal gets its own crack.
		CritCue.apply(attacker, entity, landed.verdict());
		// Rend window: the stamped bonus lands on this hit and only this one,
		// applied through the adapter like every other outcome; the bonus is
		// what the resolve already settled, never recomputed.
		DamageSettle.dealBonus(attacker, entity, landed.rendHp());
		// Stance and dice mults: attacker deal times victim take times the dice mult,
		// settled at once from what the hit landed. Gladius + Impetus reads x1.1 deal,
		// Scutum + Testudo x0.85 take, a crit x1.25: see HitPlan.combined. A hit that
		// took no damage lands 0 x mult, which is nothing.
		DamageSettle.settleTake(entity, landedDamage, landed.combined(),
				() -> attacker.damageSources().playerAttack(attacker));
	}
}
