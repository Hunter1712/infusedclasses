package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client stacks signal: one entry per passive, sent when a passive's stack
 * count changes (hit bump, decay tick that moved, class-switch and respawn clears).
 * The client stores the last entry per passive and ticks the countdown down locally.
 * An empty list means clear. Carries no attribute write: the passive effect plus its
 * applier stay the sole modifier path.
 */
public record PassiveStacksPayload(List<Entry> entries) implements CustomPacketPayload {
	public PassiveStacksPayload {
		entries = List.copyOf(entries);
	}

	/** Per-passive snapshot: what the row shows and the millis-until-decay it counts from. */
	public record Entry(String passiveName, int stacks, double perStack, int graceSeconds, long millisUntilDecay) {
	}

	public static final CustomPacketPayload.Type<PassiveStacksPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "passive_stacks"));

	static final StreamCodec<ByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Entry::passiveName,
			ByteBufCodecs.VAR_INT, Entry::stacks,
			ByteBufCodecs.DOUBLE, Entry::perStack,
			ByteBufCodecs.VAR_INT, Entry::graceSeconds,
			ByteBufCodecs.VAR_LONG, Entry::millisUntilDecay,
			Entry::new);

	public static final StreamCodec<ByteBuf, PassiveStacksPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(ByteBuf buf, PassiveStacksPayload value) {
			ByteBufCodecs.VAR_INT.encode(buf, value.entries.size());
			for (Entry entry : value.entries) {
				ENTRY_CODEC.encode(buf, entry);
			}
		}

		@Override
		public PassiveStacksPayload decode(ByteBuf buf) {
			int size = ByteBufCodecs.VAR_INT.decode(buf);
			List<Entry> out = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				out.add(ENTRY_CODEC.decode(buf));
			}
			return new PassiveStacksPayload(out);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Pure entry building: one entry per passive on the class, including zero-stack
	 * entries that clear the row. Classless yields empty and costs no packet.
	 */
	public static List<Entry> entriesFor(ClassDefinition def, PlayerRuntime runtime, long now) {
		if (def == null || runtime == null) {
			return List.of();
		}
		List<Entry> out = new ArrayList<>(def.passives().size());
		for (ConditionalPassive passive : def.passives()) {
			int stacks = runtime.stacks(passive);
			Long last = runtime.lastTrigger(passive);
			long until = 0;
			if (last != null && stacks > 0) {
				until = Math.max(0, last + passive.graceSeconds() * 1000L - now);
			}
			out.add(new Entry(passive.name(), stacks, passive.perStack(), passive.graceSeconds(), until));
		}
		return List.copyOf(out);
	}
}
