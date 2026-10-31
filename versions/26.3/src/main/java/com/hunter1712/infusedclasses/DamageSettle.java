package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.mixin.LivingEntityAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/**
 * Damage settle adapter: the single guarded {@code hurt} for the whole mod plus the
 * stance-mult settle every melee branch funnels through. The mults are post-mitigation:
 * the base each one scales is what the hit landed on the victim after armor, resistance
 * and enchantments (see {@link HitMitigation}), and the bonus then bypasses those —
 * the settle type's tag files put it in the three bypass tags — because that price was
 * already paid once. Bonus hurts re-enter the damage hook on the same thread, so one
 * re-entrancy flag guards them all: the entry returns early while it is set, and nested
 * hurts set it around the inner call. Server logic is single-threaded, so a plain flag
 * is enough.
 */
public final class DamageSettle {
	/**
	 * The bonus hurt's damage type: its datapack entry ships with the mod, and its tag
	 * files put it in {@code bypasses_armor}, {@code bypasses_effects} and
	 * {@code bypasses_shield}, because a mult priced off what landed must not pay
	 * mitigation a second time.
	 */
	private static final ResourceKey<DamageType> SETTLE_KEY = ResourceKey.create(Registries.DAMAGE_TYPE,
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "settle"));

	private static boolean resolving;

	private DamageSettle() {
	}

	/** Whether a settled hurt is on the stack: the hook grants nothing twice while true. */
	public static boolean isResolving() {
		return resolving;
	}

	/**
	 * Settles a stance mult on a just-hit entity: over 1.0 lands extra hurt through
	 * the guarded path (dead targets deal nothing); under 1.0 heals back
	 * the discount, so the net hit reads as landed times mult. Exactly 1.0 touches
	 * nothing. {@code damageTaken} arrives as the hit's post-shield amount — the damage
	 * hook reports it before armor — and this adapter converts it to what landed before
	 * scaling, so a blocked hit (nothing taken) settles nothing. The single home for
	 * every melee mult settle: the entry maps, this applies. The source arrives as a
	 * supplier so it is only resolved by this adapter, never by the entry, whichever
	 * side dealt the hit.
	 */
	public static void settleTake(Entity target, float damageTaken, double mult,
			Supplier<DamageSource> source) {
		if (target == null || source == null) {
			return;
		}
		settleWithSource(target, damageTaken, mult, source.get());
	}

	private static void settleWithSource(Entity target, float damageTaken, double mult, DamageSource source) {
		if (target == null || !target.isAlive() || source == null) {
			return;
		}
		float landed = target instanceof LivingEntity living
				? HitMitigation.landedOn(living, source, damageTaken)
				: damageTaken;
		float extra = settleMath(landed, mult);
		if (extra == 0) {
			return;
		}
		if (extra > 0) {
			hurtGuarded(target, source, extra);
		} else if (target instanceof LivingEntity living) {
			living.heal(-extra);
		}
	}

	/**
	 * Pure settle math: the extra hurt over 1.0, the healed-back discount under it, nothing at
	 * exactly 1.0, computed from the landed amount. Decided here so the curve is pinned without
	 * a player; adapters apply it.
	 */
	static float settleMath(float landedDamage, double mult) {
		if (mult == 1.0) {
			return 0;
		}
		return (float) (landedDamage * (mult - 1.0));
	}

	/**
	 * Single guarded hurt: extra damage re-enters the damage hook on the same thread.
	 * Vanilla remembers the biggest hurt of the window in {@code lastHurt} and drops any
	 * same-tick follow-up that does not beat it, so a bonus smaller than the base — a x1.25
	 * crit on a wood sword is a quarter of it — used to land nothing at all. Clearing the
	 * memory for the inner call routes the bonus through the difference branch instead:
	 * absorption, death protection and kill credit still apply, with no second sound,
	 * knockback or hurt flash since the feedback belongs to the base hit. The bonus rides
	 * the settle source, whose tags keep armor, effects and shields from pricing it twice,
	 * and the memory is put back by what the bonus actually took off — a rejected or fully
	 * eaten bonus adds nothing — so it reads as a single hit of base plus bonus.
	 */
	private static boolean hurtGuarded(Entity target, DamageSource source, float amount) {
		if (resolving) {
			return false;
		}
		resolving = true;
		try {
			// Server-only by construction: every settle funnels out of the server's
			// damage hook, and the guarded hurt must reach hurtServer, not its
			// client-predicting wrapper.
			ServerLevel level = (ServerLevel) target.level();
			DamageSource settled = settleSource(level, source);
			if (target instanceof LivingEntity living && amount > 0) {
				LivingEntityAccessor access = (LivingEntityAccessor) living;
				float lastHurt = access.infused$lastHurt();
				access.infused$lastHurt(0.0F);
				float before = living.getHealth();
				try {
					living.hurtServer(level, settled, amount);
				} finally {
					float landed = Math.max(0.0F, before - living.getHealth());
					access.infused$lastHurt(lastHurt + landed);
				}
			} else {
				target.hurtServer(level, settled, amount);
			}
		} finally {
			resolving = false;
		}
		return true;
	}

	/**
	 * The bonus hurt's source: the hit's own direct and causing entities, so kill credit,
	 * death messages and stats stay theirs, under the settle type's bypass tags.
	 */
	private static DamageSource settleSource(ServerLevel level, DamageSource from) {
		Holder<DamageType> type = level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SETTLE_KEY);
		return new DamageSource(type, from.getDirectEntity(), from.getEntity());
	}
}
