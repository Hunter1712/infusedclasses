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

	private static final String TITLE = "Choose your class";

	private static final Component TITLE_BOLD = Component.literal(TITLE).withStyle(s -> s.withBold(true));

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int FOOTER_COLOR = 0xFF777777;

	private List<ClassDefinition> classes = List.of();

	protected ClassSelectScreen() {
		super(Component.literal(TITLE));
	}

	@Override
	protected void init() {
		classes = List.copyOf(ClassRegistry.all());
		for (int i = 0; i < classes.size(); i++) {
			ClassDefinition def = classes.get(i);
			int cardX = cardX(i, classes.size());
			int cardY = ClassCardRenderer.cardsTop(height);
			ClassCardRenderer.ButtonRect bounds = ClassCardRenderer.buttonRect(cardX, cardY);
			ClassPresentation.CardModel model = ClassPresentation.cardFor(def, false);
			Component tip = Component.literal(model.tooltip());
			Button button = Button.builder(Component.literal(model.button().label()), b -> InfusedClassesClient.pickClass(def.id()))
					.bounds(bounds.x(), bounds.y(), bounds.w(), bounds.h())
					.tooltip(Tooltip.create(tip))
					.build();
			button.active = model.button().active();
			addRenderableWidget(button);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		for (int i = 0; i < classes.size(); i++) {
			int cardX = cardX(i, classes.size());
			int cardY = ClassCardRenderer.cardsTop(height);
			ClassDefinition def = classes.get(i);
			ClassPresentation.CardModel frame = cardModel(def, cardX, cardY, mouseX, mouseY);
			ClassCardRenderer.drawFrame(graphics, cardX, cardY, frame);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int top = ClassCardRenderer.cardsTop(height);
		graphics.drawCenteredString(font, TITLE_BOLD, width / 2, top - 44, TITLE_COLOR);
		graphics.drawCenteredString(font, ClassPresentation.subtitle(), width / 2, top - 28, SUBTITLE_COLOR);

		for (int i = 0; i < classes.size(); i++) {
			int cardX = cardX(i, classes.size());
			int cardY = ClassCardRenderer.cardsTop(height);
			ClassDefinition def = classes.get(i);
			ClassPresentation.CardModel model = cardModel(def, cardX, cardY, mouseX, mouseY);
			ClassCardRenderer.drawBody(graphics, font, cardX, cardY, model);
		}
		graphics.drawCenteredString(font, "Click a class to choose it", width / 2, top + ClassCardRenderer.HEIGHT + 14, FOOTER_COLOR);
	}

	private int cardX(int index, int count) {
		return ClassCardRenderer.cardX(index, count, width, GAP);
	}

	/**
	 * One card model per card per pass: hover test plus resolution in one place, so the
	 * frame and body passes cannot disagree on what hovered means.
	 */
	private static ClassPresentation.CardModel cardModel(ClassDefinition def, int cardX, int cardY,
			int mouseX, int mouseY) {
		return ClassPresentation.cardFor(def, ClassCardRenderer.isHovering(cardX, cardY, mouseX, mouseY));
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
