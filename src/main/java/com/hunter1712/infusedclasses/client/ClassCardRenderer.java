package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

/**
 * Everything one class card looks like: frame palette, accent strip, locked veil, hover
 * highlight and the card body text. Each line is drawn with the colour and weight the
 * presentation module put on it, so nothing here has to know what a line says. The screen
 * decides where a card sits and whether it is hovered.
 */
public final class ClassCardRenderer {
	/** Card size and inner padding: the card's own metrics, shared with the screen's layout. */
	public static final int WIDTH = 120;
	public static final int HEIGHT = 156;
	public static final int PAD = 10;

	private static final int ACCENT_H = 3;
	private static final int BODY_COLOR = 0xE0141414;
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int HOVER_BORDER_COLOR = 0xFF8A8A8A;
	private static final int HOVER_WASH_COLOR = 0x14FFFFFF;
	private static final int LOCKED_VEIL_COLOR = 0x50000000;
	private static final int DIVIDER_COLOR = 0xFF3A3A3A;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int NAME_LOCKED_COLOR = 0xFF8A8A8A;
	private static final int TAGLINE_COLOR = 0xFFB0A67E;

	private ClassCardRenderer() {
	}

	/** Body, accent strip, border, hover highlight and locked veil. Drawn behind the widgets. */
	public static void drawFrame(GuiGraphics graphics, int x, int y, ClassDefinition def, boolean hovered) {
		int x1 = x + WIDTH;
		int y1 = y + HEIGHT;
		int accent = ClassPresentation.accentFor(def);
		graphics.fill(x, y, x1, y1, BODY_COLOR);
		graphics.fill(x, y, x1, y + ACCENT_H, accent);
		int border = hovered && !def.locked() ? HOVER_BORDER_COLOR : BORDER_COLOR;
		graphics.fill(x, y, x + 1, y1, border);
		graphics.fill(x1 - 1, y, x1, y1, border);
		graphics.fill(x, y, x1, y + 1, border);
		graphics.fill(x, y1 - 1, x1, y1, border);
		if (hovered && !def.locked()) {
			graphics.fill(x + 1, y + ACCENT_H, x1 - 1, y1 - 1, HOVER_WASH_COLOR);
		}
		if (def.locked()) {
			graphics.fill(x + 1, y + ACCENT_H, x1 - 1, y1 - 1, LOCKED_VEIL_COLOR);
		}
	}

	/** Name, tagline, divider and the card lines, each drawn with the style it carries. */
	public static void drawBody(GuiGraphics graphics, Font font, int x, int y, ClassDefinition def) {
		int cx = x + WIDTH / 2;
		int ty = y + 10;
		graphics.drawCenteredString(font, def.displayName(), cx, ty, def.locked() ? NAME_LOCKED_COLOR : NAME_COLOR);
		ty += 11;
		graphics.drawCenteredString(font, def.tagline(), cx, ty, TAGLINE_COLOR);
		ty += 12;
		graphics.fill(x + PAD, ty, x + WIDTH - PAD, ty + 1, DIVIDER_COLOR);
		ty += 6;

		for (ClassPresentation.CardLine line : ClassPresentation.cardLinesFor(def)) {
			MutableComponent text = Component.literal(line.text());
			if (line.bold()) {
				text = text.withStyle(s -> s.withBold(true));
			}
			graphics.drawCenteredString(font, text, cx, ty, line.color());
			ty += 11;
		}
	}
}
