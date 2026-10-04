package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import com.hunter1712.infusedclasses.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Polished vanilla+ picker: dimmed backdrop, one card per class with accent strip,
 * tagline, passive list and ability line. A small button at each card base selects.
 */
public class ClassSelectScreen extends Screen {
	private static final int CARD_W = 120;
	private static final int CARD_H = 156;
	private static final int GAP = 12;
	private static final int PAD = 10;
	private static final int BUTTON_H = 20;

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int NAME_LOCKED_COLOR = 0xFF8A8A8A;
	private static final int TAGLINE_COLOR = 0xFFB0A67E;
	private static final int PASSIVE_HEADER_COLOR = 0xFFFFFFFF; // white for headers
	private static final int PASSIVE_COLOR = 0xFFBDBDBD;         // light gray for items
	private static final int ABILITY_HEADER_COLOR = 0xFFFFFFFF;  // white for headers
	private static final int ABILITY_COLOR = 0xFF7ED6FF;         // blue for items
	private static final int FOOTER_COLOR = 0xFF777777;

	private List<ClassDefinition> classes = List.of();

	record CardPos(int x, int y) {
	}

	protected ClassSelectScreen() {
		super(Component.literal("Choose your class"));
	}

	@Override
	protected void init() {
		classes = List.copyOf(ClassRegistry.all());
		for (int i = 0; i < classes.size(); i++) {
			ClassDefinition def = classes.get(i);
			CardPos card = cardRect(i, classes.size());
			int bx = card.x() + PAD;
			int by = card.y() + CARD_H - PAD - BUTTON_H;
			int bw = CARD_W - PAD * 2;
			ClassPresentation.ButtonData btn = ClassPresentation.buttonFor(def);
			Component tip = Component.literal(ClassPresentation.tooltipFor(def));
			Button button = Button.builder(Component.literal(btn.label()), b -> InfusedClassesClient.pickClass(def.id()))
					.bounds(bx, by, bw, BUTTON_H)
					.tooltip(Tooltip.create(tip))
					.build();
			button.active = btn.active();
			addRenderableWidget(button);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		for (int i = 0; i < classes.size(); i++) {
			CardPos card = cardRect(i, classes.size());
			drawCardFrame(graphics, card, classes.get(i), isHovering(card, mouseX, mouseY));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int top = cardsTop();
		graphics.drawCenteredString(font, Component.literal("Choose your class").withStyle(s -> s.withBold(true)), width / 2, top - 44, TITLE_COLOR);
		graphics.drawCenteredString(font, ClassPresentation.subtitle(), width / 2, top - 28, SUBTITLE_COLOR);
		graphics.drawCenteredString(font, "\u00a7m                                           \u00a7r", width / 2, top - 17, SUBTITLE_COLOR);

		for (int i = 0; i < classes.size(); i++) {
			drawCardText(graphics, cardRect(i, classes.size()), classes.get(i));
		}
		graphics.drawCenteredString(font, "Click a class to choose it", width / 2, top + CARD_H + 14, FOOTER_COLOR);
	}

	private void drawCardFrame(GuiGraphics graphics, CardPos card, ClassDefinition def, boolean hovered) {
		int x0 = card.x();
		int y0 = card.y();
		int x1 = x0 + CARD_W;
		int y1 = y0 + CARD_H;
		int accent = ClassPresentation.accentFor(def);
		graphics.fill(x0, y0, x1, y1, 0xE0141414);
		graphics.fill(x0, y0, x1, y0 + 3, accent);
		int border = hovered && !def.locked() ? 0xFF8A8A8A : 0xFF3A3A3A;
		graphics.fill(x0, y0, x0 + 1, y1, border);
		graphics.fill(x1 - 1, y0, x1, y1, border);
		graphics.fill(x0, y0, x1, y0 + 1, border);
		graphics.fill(x0, y1 - 1, x1, y1, border);
		if (hovered && !def.locked()) {
			graphics.fill(x0 + 1, y0 + 3, x1 - 1, y1 - 1, 0x14FFFFFF);
		}
		if (def.locked()) {
			graphics.fill(x0 + 1, y0 + 3, x1 - 1, y1 - 1, 0x50000000);
		}
	}

	private void drawCardText(GuiGraphics graphics, CardPos card, ClassDefinition def) {
		int cx = card.x() + CARD_W / 2;
		int y = card.y() + 10;
		graphics.drawCenteredString(font, def.displayName(), cx, y, def.locked() ? NAME_LOCKED_COLOR : NAME_COLOR);
		y += 11;
		graphics.drawCenteredString(font, def.tagline(), cx, y, TAGLINE_COLOR);
		y += 12;
		graphics.fill(card.x() + PAD, y, card.x() + CARD_W - PAD, y + 1, 0xFF3A3A3A);
		y += 6;

		for (ClassPresentation.CardLine line : ClassPresentation.cardLinesFor(def)) {
			int color;
			boolean bold = false;
			if (line.text().equals("COMING SOON")) {
				color = TITLE_COLOR;
				bold = true;
			} else if (line.kind() == ClassPresentation.LineKind.PASSIVE_HEADER) {
				color = PASSIVE_HEADER_COLOR;
				bold = true;
			} else if (line.kind() == ClassPresentation.LineKind.PASSIVE) {
				color = PASSIVE_COLOR;
			} else if (line.kind() == ClassPresentation.LineKind.ABILITY_HEADER) {
				color = ABILITY_HEADER_COLOR;
				bold = true;
			} else {
				color = ABILITY_COLOR;
			}
			MutableComponent text = Component.literal(line.text());
			if (bold) {
				text = text.withStyle(s -> s.withBold(true));
			}
			graphics.drawCenteredString(font, text, cx, y, color);
			y += 11;
		}
	}

	private CardPos cardRect(int index, int count) {
		int rowW = count * CARD_W + (count - 1) * GAP;
		int x = width / 2 - rowW / 2 + index * (CARD_W + GAP);
		return new CardPos(x, cardsTop());
	}

	private int cardsTop() {
		return Math.max(64, height / 2 - CARD_H / 2 + 12);
	}

	private static boolean isHovering(CardPos card, int mouseX, int mouseY) {
		return mouseX >= card.x() && mouseX < card.x() + CARD_W && mouseY >= card.y() && mouseY < card.y() + CARD_H;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
