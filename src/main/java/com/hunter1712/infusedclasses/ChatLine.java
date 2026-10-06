package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;

/**
 * A chat line a decision returns for the server entry to send verbatim: the text, and the chat
 * style it shows in. A null style is a decision too: the line goes out unstyled and inherits
 * whatever the client shows by default, which is what the cooldown lines have always done.
 * Shared by every ability decision so none reaches into another's nested type.
 */
public record ChatLine(String text, ChatFormatting style) {
	/** Cooldown line: unstyled by decision, whole-second rounding. One shape for every ability. */
	public static ChatLine cooldown(String abilityName, long remainingMs) {
		return new ChatLine(
				abilityName + ": " + String.format(java.util.Locale.ROOT, "%.0fs", remainingMs / 1000.0), null);
	}
}
