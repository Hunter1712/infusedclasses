package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.client.CardWrap;
import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassPresentation;
import com.hunter1712.infusedclasses.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Focused picker screen: dimmed backdrop, one large class card per page with its full
 * kit printed on the card and no tooltips anywhere. Side arrows, clickable dots and
 * keyboard paging move between pages with wraparound. It builds the widgets from the
 * presentation data and the layout math; how a card is drawn is ClassCardRenderer's
 * business, and what a card says is the card text's. Picking still closes only on
 * server confirm, so a rejected pick stays open for retry.
 */
public class ClassSelectScreen extends Screen {
	private static final String TITLE = "Choose your class";

	private static final Component TITLE_BOLD = Component.literal(TITLE).withStyle(s -> s.withBold(true));

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int FOOTER_COLOR = 0xFF777777;
	/** Backdrop gradient, darker toward the bottom, with the edge vignette laid on top. */
	private static final int BACKDROP_TOP = 0xD00C0C12;
	private static final int BACKDROP_BOTTOM = 0xE80A0A10;
	private static final int VIGNETTE_ALPHA = 0x40;
	private static final int VIGNETTE_STEPS = 6;

	private List<ClassDefinition> classes = List.of();
	private final List<Button> dots = new ArrayList<>();
	private int page;
	private int scrollPx;
	// One resolved view: the background pass resolves the card, its model and its measured
	// layout together, and the body pass reuses that one value. Scroll clamps against the
	// same measured layout instead of a separate last-max field. The cache holds while the
	// page, the hover state and the screen size hold: wrapping the card text is the
	// expensive half of a frame.
	private View frame;
	private int framePage = -1;
	private boolean frameHovered;
	private int frameWidth;
	private int frameHeight;

	/** One frame's resolved card: its rectangle, its presentation and its wrapped layout. */
	private record View(CardLayout.CardRect card, ClassPresentation.CardModel model,
			CardWrap.Measured measured) {
		int maxScroll() {
			return measured.layout().maxScroll();
		}
	}

	public ClassSelectScreen(String currentClassId) {
		super(Component.literal(TITLE));
		ClassDefinition def = currentClassId == null || currentClassId.isEmpty()
				? null : ClassRegistry.byId(currentClassId);
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
		CardLayout.CardRect card = CardLayout.focusedCard(width, height);

		CardLayout.ButtonRect prev = CardLayout.prevRect(card);
		addRenderableWidget(Button.builder(Component.literal("<"), b -> goTo(CardLayout.prevPage(page, classes.size())))
				.bounds(prev.x(), prev.y(), prev.w(), prev.h())
				.build());
		CardLayout.ButtonRect next = CardLayout.nextRect(width, card);
		addRenderableWidget(Button.builder(Component.literal(">"), b -> goTo(CardLayout.nextPage(page, classes.size())))
				.bounds(next.x(), next.y(), next.w(), next.h())
				.build());

		int dotsTop = CardLayout.dotsY(card);
		for (int i = 0; i < classes.size(); i++) {
			int dot = i;
			Button dotButton = Button.builder(Component.literal(dot == page ? "\u25CF" : "\u25CB"),
					b -> goTo(dot))
					.bounds(CardLayout.dotX(i, classes.size(), width), dotsTop,
							CardLayout.DOT_W, CardLayout.DOT_H)
					.build();
			dots.add(dotButton);
			addRenderableWidget(dotButton);
		}

		CardLayout.ButtonRect select = CardLayout.selectRect(card);
		addRenderableWidget(Button.builder(Component.literal("Select"),
				b -> InfusedClassesClient.pickClass(classes.get(page).id()))
				.bounds(select.x(), select.y(), select.w(), select.h())
				.build());
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, BACKDROP_TOP, BACKDROP_BOTTOM);
		drawVignette(graphics);
		if (classes.isEmpty()) {
			return;
		}
		frame = resolve(mouseX, mouseY);
		ClassCardRenderer.drawFrame(graphics, frame.card(), frame.model());
	}

	/** Deepens the screen edges in a few steps: a subtle vignette over the picker backdrop. */
	private void drawVignette(GuiGraphics graphics) {
		int band = Math.max(24, Math.min(width, height) / 8);
		int strip = Math.max(1, band / VIGNETTE_STEPS);
		for (int i = 0; i < VIGNETTE_STEPS; i++) {
			int shade = (VIGNETTE_ALPHA * (VIGNETTE_STEPS - 1 - i) / VIGNETTE_STEPS) << 24;
			graphics.fill(i * strip, 0, (i + 1) * strip, height, shade);
			graphics.fill(width - (i + 1) * strip, 0, width - i * strip, height, shade);
			graphics.fill(0, i * strip, width, (i + 1) * strip, shade);
			graphics.fill(0, height - (i + 1) * strip, width, height - i * strip, shade);
		}
	}

	/** The one resolution: card rectangle, presentation model and measured layout. */
	private View resolve(int mouseX, int mouseY) {
		CardLayout.CardRect card = CardLayout.focusedCard(width, height);
		boolean hovered = CardLayout.isHovering(card, mouseX, mouseY);
		if (frame != null && framePage == page && frameHovered == hovered
				&& frameWidth == width && frameHeight == height) {
			return frame;
		}
		ClassPresentation.CardModel model = ClassPresentation.cardFor(classes.get(page), hovered);
		frame = new View(card, model, CardWrap.measure(card, model, font::width));
		framePage = page;
		frameHovered = hovered;
		frameWidth = width;
		frameHeight = height;
		return frame;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		if (classes.isEmpty()) {
			return;
		}
		View view = frame != null ? frame : resolve(mouseX, mouseY);
		CardLayout.CardRect card = view.card();
		graphics.drawCenteredString(font, TITLE_BOLD, width / 2, CardLayout.titleY(card), TITLE_COLOR);
		graphics.drawCenteredString(font, ClassPresentation.subtitle(), width / 2, CardLayout.subtitleY(card),
				SUBTITLE_COLOR);

		// The scroll clamps against this frame's own measured content, so a page that
		// shrank (or a reset) can never draw one frame past its end.
		scrollPx = CardLayout.clampScroll(scrollPx, view.maxScroll());
		ClassCardRenderer.drawBody(graphics, font, card, view.model(), view.measured(), scrollPx);
		CardLayout.ButtonRect bar = CardLayout.scrollBar(card, view.measured().layout(), scrollPx);
		if (bar != null) {
			graphics.fill(bar.x(), bar.y(), bar.x() + bar.w(), bar.y() + bar.h(), 0xFF8A8A8A);
		}
		for (int i = 0; i < dots.size(); i++) {
			dots.get(i).setMessage(Component.literal(i == page ? "\u25CF" : "\u25CB"));
		}
		graphics.drawCenteredString(font, "Use Select to choose it", width / 2, CardLayout.footerY(card),
				FOOTER_COLOR);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!classes.isEmpty()) {
			View view = resolve((int) mouseX, (int) mouseY);
			if (CardLayout.isHovering(view.card(), (int) mouseX, (int) mouseY)) {
				scrollPx = CardLayout.clampScroll(scrollPx - (int) (verticalAmount * 12), view.maxScroll());
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (classes.isEmpty()) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
			goTo(CardLayout.prevPage(page, classes.size()));
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
			goTo(CardLayout.nextPage(page, classes.size()));
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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
