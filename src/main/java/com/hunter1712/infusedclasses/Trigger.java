package com.hunter1712.infusedclasses;

/**
 * A conditional passive's earning event, as a value: the id a definition and the runtime
 * agree on, plus the picker copy that describes it. The copy rides the trigger, so a new
 * trigger adds one value with its own words and cannot be described by prose it does not
 * carry; a definition cannot spell a trigger the mod never emits and still read as live,
 * because there is no string to misspell. Plain data, no game bootstrap needed.
 *
 * @param id the trigger's own name, what a definition and {@code onTrigger} match on
 * @param earn what the copy says earns a stack ("Land a hit")
 * @param idle what the copy says idleness costs ("no hit")
 */
public record Trigger(String id, String earn, String idle) {
	/** Fed by landing a full-swing melee hit: the Momentum earner, the one trigger the mod emits. */
	public static final Trigger ON_HIT_DEALT = new Trigger("on_hit_dealt", "Land a hit", "no hit");
}
