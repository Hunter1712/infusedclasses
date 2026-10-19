package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

/**
 * Bind morph decisions: what a reused slot bind means for a class. Every class reuses the two
 * slot binds so binds survive the class: the signature slot arrives as the rotate intent for
 * Legionaries, the roulette intent for Gamblers, Offload for everyone else; the ultimate slot
 * arrives as the oath ultimate for Legionaries, All In for Gamblers, Onslaught for everyone
 * else. Each target carries the ability it fires, so one walk of the class's actives answers
 * both what the slot fires and the row that draws it: a further class adds actives, not
 * branches, and the stack can never draw one ultimate while the key fires another.
 *
 * <p>The printed fallbacks (Offload, Onslaught) keep both keys working for a classless player
 * and for a class carrying neither slot's abilities; the drawn rows stay null there, because
 * there is no lane to draw. The intent receivers wire, this decides; decisions and applies
 * stay pure/adapter.
 */
public final class BindMorph {
	/** What the signature slot means once the class is known: the one ability it fires. */
	public enum SignatureTarget {
		OFFLOAD(ClassRegistry.OFFLOAD_ABILITY),
		ROTATE(ClassRegistry.ROTATE_ABILITY),
		ROULETTE(ClassRegistry.ROULETTE_ABILITY);

		private final Identifier ability;

		SignatureTarget(Identifier ability) {
			this.ability = ability;
		}

		/** The ability this target fires: what the class must carry, and the roster row that draws it. */
		public Identifier ability() {
			return ability;
		}
	}

	/** What the ultimate slot means once the class is known: the one ability it fires. */
	public enum UltimateTarget {
		ONSLAUGHT(ClassRegistry.ONSLAUGHT_ABILITY),
		AEQUILIBRIUM(ClassRegistry.AEQUILIBRIUM_ABILITY),
		ALL_IN(ClassRegistry.ALL_IN_ABILITY);

		private final Identifier ability;

		UltimateTarget(Identifier ability) {
			this.ability = ability;
		}

		/** The ability this target fires: what the class must carry, and the roster row that draws it. */
		public Identifier ability() {
			return ability;
		}
	}

	/**
	 * The class's shape: what each slot fires and the row that draws it. The drawn row is the
	 * fired target's own row, null when the class does not carry that ability — the two halves
	 * cannot disagree, and a class carrying neither slot ability draws no slot lines.
	 */
	public record Shape(
			SignatureTarget signature,
			AbilityRoster.Row signatureRow,
			UltimateTarget ultimate,
			AbilityRoster.Row ultimateRow) {
	}

	private BindMorph() {
	}

	/** What the signature slot means: the class's first active naming a signature ability, Offload for everyone else. */
	public static SignatureTarget signatureFor(ClassDefinition def) {
		if (def != null) {
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				SignatureTarget target = signatureTargetOf(active.id());
				if (target != null) {
					return target;
				}
			}
		}
		return SignatureTarget.OFFLOAD;
	}

	/** What the ultimate slot means: the class's first active naming an ultimate ability, Onslaught for everyone else. */
	public static UltimateTarget ultimateFor(ClassDefinition def) {
		if (def != null) {
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				UltimateTarget target = ultimateTargetOf(active.id());
				if (target != null) {
					return target;
				}
			}
		}
		return UltimateTarget.ONSLAUGHT;
	}

	/**
	 * The class's shape: both slot targets and the rows that draw them, from one walk of the
	 * class's actives. The HUD's slot lines and the keys' fires are this one value.
	 */
	public static Shape shape(ClassDefinition def) {
		SignatureTarget signature = signatureFor(def);
		UltimateTarget ultimate = ultimateFor(def);
		return new Shape(signature, row(def, signature.ability()), ultimate, row(def, ultimate.ability()));
	}

	/** The target an ability names as the signature, null when it names none. */
	private static SignatureTarget signatureTargetOf(Identifier ability) {
		for (SignatureTarget target : SignatureTarget.values()) {
			if (target.ability().equals(ability)) {
				return target;
			}
		}
		return null;
	}

	/** The target an ability names as the ultimate, null when it names none. */
	private static UltimateTarget ultimateTargetOf(Identifier ability) {
		for (UltimateTarget target : UltimateTarget.values()) {
			if (target.ability().equals(ability)) {
				return target;
			}
		}
		return null;
	}

	/** The row to draw for a slot: the target's own row, null when the class does not carry that ability. */
	private static AbilityRoster.Row row(ClassDefinition def, Identifier ability) {
		if (def == null || !def.hasAbility(ability)) {
			return null;
		}
		return AbilityRoster.byAbility(ability);
	}
}
