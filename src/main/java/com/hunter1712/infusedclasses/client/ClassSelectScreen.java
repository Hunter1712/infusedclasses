package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import com.hunter1712.infusedclasses.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Focused picker screen: dimmed backdrop, one large class card per page with its full
 * kit printed on the card and no tooltips anywhere. Side arrows, clickable dots and
 * keyboard paging move between pages with wraparound. It builds the widgets from the
 * presentation data and the renderer math; how a card is drawn is ClassCardRenderer's
 * business, and what a card says is the card text's. Picking still closes only on
 * server confirm, so a rejected pick stays open for retry.
 */
public class ClassSelectScreen extends Screen {
	private static final String TITLE = "Choose your class";

	private static final Component TITLE_BOLD = Component.literal(TITLE).withStyle(s -> s.withBold(true));

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int FOOTER_COLOR = 0xFF777777;

	private List<ClassDefinition> classes = List.of();
	private final List<Button> dots = new ArrayList<>();
	private int page;
	private int scrollPx;
	private int lastMaxScroll;

	public ClassSelectScreen(String currentClassId) {
		super(Component.literal(TITLE));
		Identifier current = currentClassId == null || currentClassId.isEmpty()
				? null : Identifier.tryParse(currentClassId);
		ClassDefinition def = current == null ? null : ClassRegistry.byId(current);
		page = def == null ? 0 : Math.max(0, List.copyOf(ClassRegistry.all()).indexOf(def));
	}

	@Override
	protected void init() {
		classes = List.copyOf(ClassRegistry.all());
		dots.clear();
		if (classes.isEmpty()) {
			return;
		}
		page = Math.min(page, classes.size() - 1);
		ClassCardRenderer.CardRect card = ClassCardRenderer.focusedCard(width, height);

		ClassCardRenderer.ButtonRect prev = ClassCardRenderer.prevRect(width, card);
		addRenderableWidget(Button.builder(Component.literal("<"), b -> goTo(ClassCardRenderer.prevPage(page, classes.size())))
				.bounds(prev.x(), prev.y(), prev.w(), prev.h())
				.build());
		ClassCardRenderer.ButtonRect next = ClassCardRenderer.nextRect(width, card);
		addRenderableWidget(Button.builder(Component.literal(">"), b -> goTo(ClassCardRenderer.nextPage(page, classes.size())))
				.bounds(next.x(), next.y(), next.w(), next.h())
				.build());

		int dotsTop = ClassCardRenderer.dotsY(card);
		for (int i = 0; i < classes.size(); i++) {
			int dot = i;
			Button dotButton = Button.builder(Component.literal(dot == page ? "\u25CF" : "\u25CB"),
					b -> goTo(dot))
					.bounds(ClassCardRenderer.dotX(i, classes.size(), width), dotsTop,
							ClassCardRenderer.DOT_W, ClassCardRenderer.DOT_H)
					.build();
			dots.add(dotButton);
			addRenderableWidget(dotButton);
		}

		ClassCardRenderer.ButtonRect select = ClassCardRenderer.selectRect(card);
		ClassPresentation.ButtonData button = ClassPresentation.buttonFor(classes.get(page));
		addRenderableWidget(Button.builder(Component.literal(button.label()),
				b -> InfusedClassesClient.pickClass(classes.get(page).id()))
				.bounds(select.x(), select.y(), select.w(), select.h())
				.build());
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		if (classes.isEmpty()) {
			return;
		}
		ClassCardRenderer.CardRect card = ClassCardRenderer.focusedCard(width, height);
		ClassDefinition def = classes.get(page);
		ClassCardRenderer.drawFrame(graphics, card,
				ClassPresentation.cardFor(def, ClassCardRenderer.isHovering(card, mouseX, mouseY)));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		if (classes.isEmpty()) {
			return;
		}
		ClassCardRenderer.CardRect card = ClassCardRenderer.focusedCard(width, height);
		graphics.drawCenteredString(font, TITLE_BOLD, width / 2, ClassCardRenderer.titleY(card), TITLE_COLOR);
		graphics.drawCenteredString(font, ClassPresentation.subtitle(), width / 2, ClassCardRenderer.subtitleY(card),
				SUBTITLE_COLOR);

		ClassDefinition def = classes.get(page);
		boolean hovered = ClassCardRenderer.isHovering(card, mouseX, mouseY);
		scrollPx = ClassCardRenderer.clampScroll(scrollPx, lastMaxScroll);
		ClassCardRenderer.BodyLayout layout =
				ClassCardRenderer.drawBody(graphics, font, card, ClassPresentation.cardFor(def, hovered), scrollPx);
		lastMaxScroll = layout.maxScroll();
		ClassCardRenderer.ButtonRect bar = ClassCardRenderer.scrollBar(card, layout, scrollPx);
		if (bar != null) {
			graphics.fill(bar.x(), bar.y(), bar.x() + bar.w(), bar.y() + bar.h(), 0xFF8A8A8A);
		}
		for (int i = 0; i < dots.size(); i++) {
			dots.get(i).setMessage(Component.literal(i == page ? "\u25CF" : "\u25CB"));
		}
		graphics.drawCenteredString(font, "Click a class to choose it", width / 2, ClassCardRenderer.footerY(card),
				FOOTER_COLOR);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!classes.isEmpty()) {
			ClassCardRenderer.CardRect card = ClassCardRenderer.focusedCard(width, height);
			if (ClassCardRenderer.isHovering(card, (int) mouseX, (int) mouseY)) {
				scrollPx = ClassCardRenderer.clampScroll(scrollPx - (int) (verticalAmount * 12), lastMaxScroll);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (classes.isEmpty()) {
			return super.keyPressed(event);
		}
		if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_A) {
			goTo(ClassCardRenderer.prevPage(page, classes.size()));
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_D) {
			goTo(ClassCardRenderer.nextPage(page, classes.size()));
			return true;
		}
		return super.keyPressed(event);
	}

	/** Page jump that resets the scroll: a new page always opens at the top. */
	private void goTo(int next) {
		page = next;
		scrollPx = 0;
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
