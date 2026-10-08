package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.List;

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
	public static final int BUTTON_H = 20;

	/** Button rect inside a card: the screen positions cards, never their internals. */
	public record ButtonRect(int x, int y, int w, int h) {
	}

	public static ButtonRect buttonRect(int cardX, int cardY) {
		return new ButtonRect(
				cardX + PAD,
				cardY + HEIGHT - PAD - BUTTON_H,
				WIDTH - PAD * 2,
				BUTTON_H);
	}

	private static final int ACCENT_H = 3;
	private static final int BODY_COLOR = 0xE0141414;
	private static final int DIVIDER_COLOR = 0xFF3A3A3A;
	private static final int TAGLINE_COLOR = 0xFFB0A67E;

	private ClassCardRenderer() {
	}

	/** Row math next to the metrics it uses, so the screen never repeats them. Pure ints. */
	public static int rowWidth(int count, int gap) {
		return count * WIDTH + (count - 1) * gap;
	}

	public static int cardX(int index, int count, int screenWidth, int gap) {
		return screenWidth / 2 - rowWidth(count, gap) / 2 + index * (WIDTH + gap);
	}

	public static int cardsTop(int screenHeight) {
		return Math.max(64, screenHeight / 2 - HEIGHT / 2 + 12);
	}

	public static boolean isHovering(int cardX, int cardY, int mouseX, int mouseY) {
		return mouseX >= cardX && mouseX < cardX + WIDTH
				&& mouseY >= cardY && mouseY < cardY + HEIGHT;
	}

	/** Body, accent strip, border, hover highlight and locked veil. Drawn behind the widgets. */
	public static void drawFrame(GuiGraphics graphics, int x, int y,
			ClassPresentation.CardStyle style, int accent) {
		int x1 = x + WIDTH;
		int y1 = y + HEIGHT;
		graphics.fill(x, y, x1, y1, BODY_COLOR);
		graphics.fill(x, y, x1, y + ACCENT_H, accent);
		graphics.fill(x, y, x + 1, y1, style.borderColor());
		graphics.fill(x1 - 1, y, x1, y1, style.borderColor());
		graphics.fill(x, y, x1, y + 1, style.borderColor());
		graphics.fill(x, y1 - 1, x1, y1, style.borderColor());
		if (style.washColor() != null) {
			graphics.fill(x + 1, y + ACCENT_H, x1 - 1, y1 - 1, style.washColor());
		}
		if (style.veilColor() != null) {
			graphics.fill(x + 1, y + ACCENT_H, x1 - 1, y1 - 1, style.veilColor());
		}
	}

	/** Name, tagline, divider and the card lines, each drawn with the style it carries. */
	public static void drawBody(GuiGraphics graphics, Font font, int x, int y, ClassDefinition def,
			ClassPresentation.CardStyle style) {
		drawBody(graphics, font, x, y, def, style, ClassPresentation.cardLinesFor(def));
	}

	/** Same body from one card model: the screen resolves the model once, never the parts. */
	public static void drawBody(GuiGraphics graphics, Font font, int x, int y, ClassDefinition def,
			ClassPresentation.CardStyle style, List<ClassPresentation.CardLine> lines) {
		int cx = x + WIDTH / 2;
		int ty = y + 10;
		graphics.drawCenteredString(font, def.displayName(), cx, ty, style.nameColor());
		ty += 11;
		graphics.drawCenteredString(font, def.tagline(), cx, ty, TAGLINE_COLOR);
		ty += 12;
		graphics.fill(x + PAD, ty, x + WIDTH - PAD, ty + 1, DIVIDER_COLOR);
		ty += 6;

		for (ClassPresentation.CardLine line : lines) {
			MutableComponent text = Component.literal(line.text());
			if (line.style().bold()) {
				text = text.withStyle(s -> s.withBold(true));
			}
			graphics.drawCenteredString(font, text, cx, ty, line.style().color());
			ty += 11;
		}
	}
}
