package com.hunter1712.infusedclasses;

/**
 * Which attribute a modifier lands on, as plain data: the registry id a decision names
 * (e.g. {@code "minecraft:armor"}) rather than a live holder or a registry key. A decision
 * can name the attribute without a registry, and the writer resolves the key at its one
 * use site. An unknown or absent id resolves to nothing written, which is the adapter's
 * answer to make, not the decision's.
 */
public record AttributeRef(String id) {
}
