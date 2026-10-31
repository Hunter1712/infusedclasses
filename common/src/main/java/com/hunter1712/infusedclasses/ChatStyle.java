package com.hunter1712.infusedclasses;

/**
 * The chat style a decision names, as plain data: a decision cannot reach for the
 * client's own formatting enum, and a null style on a {@link ChatLine} is a decision
 * too (the line goes out unstyled and inherits the client's default). The adapter that
 * builds the {@code Component} maps these to the client's colours at its single use
 * site, so a new style is one value here plus one row there.
 */
public enum ChatStyle {
	RED,
	GRAY,
	YELLOW,
	GOLD
}
