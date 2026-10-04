package com.hunter1712.infusedclasses;

/**
 * Reactive conditional passive: event-triggered stacks, a cap, and a short/long
 * description pair for the picker. Plain data, no game bootstrap needed, so tests assert on it directly.
 */
public record ConditionalPassive(String name, String trigger, String shortDesc, String fullDesc, int maxStacks) {
}
