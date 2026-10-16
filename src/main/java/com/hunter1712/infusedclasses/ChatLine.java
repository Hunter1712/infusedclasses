package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;

/**
 * A chat line a decision returns for the server entry to send verbatim: the text, and the chat
 * style it shows in. A null style is a decision too: the line goes out unstyled and inherits
 * whatever the client shows by default, which is what the cooldown lines have always done.
 * Shared by every ability decision and class-choice outcome so none reaches into another's nested type.
 */
public record ChatLine(String text, ChatFormatting style) {
	/** Cooldown line: unstyled by decision, one-decimal rounding. One shape for every ability. */
	public static ChatLine cooldown(String abilityName, long remainingMs) {
		return new ChatLine(
				abilityName + ": " + String.format(java.util.Locale.ROOT, "%.1fs", remainingMs / 1000.0), null);
	}

	/** Unknown or locked class rejection: red, action-bar. */
	public static ChatLine unknownOrLocked() {
		return new ChatLine(InfusedClasses.UNKNOWN_OR_LOCKED, ChatFormatting.RED);
	}

	/** Successful class pick: "Class: <display>" in gray, chat. */
	public static ChatLine classPicked(String displayName) {
		return new ChatLine("Class: " + displayName, ChatFormatting.GRAY);
	}
}
