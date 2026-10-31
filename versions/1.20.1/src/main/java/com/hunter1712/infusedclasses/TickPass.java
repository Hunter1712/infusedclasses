package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * The tick pass: the one steady-state sequence the server tick runs per classed player.
 * One state read, the applier, the drained-Onslaught end, a re-read only when that end
 * actually closed the window, then the snapshot the emission guard compares. The wiring
 * only loops the players and sends; the order lives here, so the re-read cannot be
 * forgotten or run on a quiet tick.
 */
final class TickPass {
	private TickPass() {
	}

	static PlayerStatusPayload run(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		PlayerRuntime.State state = runtime.state(now);
		PassiveApplier.apply(player, def, runtime, state, now);
		// The Onslaught window lasts the bar as well as the clock: an empty bar ends it
		// here, after the bleed. If it ended, re-read before the snapshot: otherwise the
		// tick would draw a rider for a window that just closed.
		if (OnslaughtAbility.endIfDrained(def, runtime, state)) {
			state = runtime.state(now);
		}
		return PlayerStatusPayload.of(def, runtime, state, now);
	}
}
