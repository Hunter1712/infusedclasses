package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import com.hunter1712.infusedclasses.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Picker screen: dimmed backdrop, one card per class with a select button. It builds the
 * widgets from the presentation data, tracks which card is hovered and lays the row out;
 * how a card is drawn is ClassCardRenderer's business.
 */
public class ClassSelectScreen extends Screen {
	private static final int GAP = 12;
	private static final int BUTTON_H = 20;

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
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
			int bx = card.x() + ClassCardRenderer.PAD;
			int by = card.y() + ClassCardRenderer.HEIGHT - ClassCardRenderer.PAD - BUTTON_H;
			int bw = ClassCardRenderer.WIDTH - ClassCardRenderer.PAD * 2;
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
			ClassCardRenderer.drawFrame(graphics, card.x(), card.y(), classes.get(i), isHovering(card, mouseX, mouseY));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int top = ClassCardRenderer.cardsTop(height);
		graphics.drawCenteredString(font, Component.literal("Choose your class").withStyle(s -> s.withBold(true)), width / 2, top - 44, TITLE_COLOR);
		graphics.drawCenteredString(font, ClassPresentation.subtitle(), width / 2, top - 28, SUBTITLE_COLOR);
		graphics.drawCenteredString(font, "\u00a7m                                           \u00a7r", width / 2, top - 17, SUBTITLE_COLOR);

		for (int i = 0; i < classes.size(); i++) {
			CardPos card = cardRect(i, classes.size());
			ClassDefinition def = classes.get(i);
			ClassPresentation.CardStyle style = ClassPresentation.cardStyleFor(def, isHovering(card, mouseX, mouseY));
			ClassCardRenderer.drawBody(graphics, font, card.x(), card.y(), def, style);
		}
		graphics.drawCenteredString(font, "Click a class to choose it", width / 2, top + ClassCardRenderer.HEIGHT + 14, FOOTER_COLOR);
	}

	private CardPos cardRect(int index, int count) {
		return new CardPos(ClassCardRenderer.cardX(index, count, width, GAP), ClassCardRenderer.cardsTop(height));
	}

	private static boolean isHovering(CardPos card, int mouseX, int mouseY) {
		return ClassCardRenderer.isHovering(card.x(), card.y(), mouseX, mouseY);
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
