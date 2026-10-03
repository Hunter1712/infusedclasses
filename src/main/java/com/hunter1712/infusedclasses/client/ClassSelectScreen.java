package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassDefinition;
import com.hunter1712.infusedclasses.ClassRegistry;
import com.hunter1712.infusedclasses.Passive;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
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
	private static final int PASSIVE_COLOR = 0xFFBDBDBD;
	private static final int ABILITY_COLOR = 0xFF7ED6FF;
	private static final int FOOTER_COLOR = 0xFF777777;

	private List<ClassDefinition> classes = List.of();

	protected ClassSelectScreen() {
		super(Component.literal("Choose your class"));
	}

	@Override
	protected void init() {
		classes = List.copyOf(ClassRegistry.all());
		for (int i = 0; i < classes.size(); i++) {
			ClassDefinition def = classes.get(i);
			int[] card = cardRect(i, classes.size());
			int bx = card[0] + PAD;
			int by = card[1] + CARD_H - PAD - BUTTON_H;
			int bw = CARD_W - PAD * 2;
			Component label = def.locked() ? Component.literal("Locked") : Component.literal("Select");
			Component tip = def.locked()
					? Component.literal("Coming soon")
					: Component.literal(abilityBlurb(def));
			Button button = Button.builder(label, b -> InfusedClassesClient.pickClass(def.id()))
					.bounds(bx, by, bw, BUTTON_H)
					.tooltip(Tooltip.create(tip))
					.build();
			button.active = !def.locked();
			addRenderableWidget(button);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		for (int i = 0; i < classes.size(); i++) {
			int[] card = cardRect(i, classes.size());
			drawCardFrame(graphics, card, classes.get(i), isHovering(card, mouseX, mouseY));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int top = cardsTop();
		graphics.drawCenteredString(font, Component.literal("Choose your class").withStyle(s -> s.withBold(true)), width / 2, top - 44, TITLE_COLOR);
		graphics.drawCenteredString(font, "Your choice shapes passives and ability", width / 2, top - 28, SUBTITLE_COLOR);
		graphics.drawCenteredString(font, "\u00a7m                                           \u00a7r", width / 2, top - 17, SUBTITLE_COLOR);

		for (int i = 0; i < classes.size(); i++) {
			drawCardText(graphics, cardRect(i, classes.size()), classes.get(i));
		}
		graphics.drawCenteredString(font, "Click a class to choose it", width / 2, top + CARD_H + 14, FOOTER_COLOR);
	}

	private void drawCardFrame(GuiGraphics graphics, int[] card, ClassDefinition def, boolean hovered) {
		int x0 = card[0];
		int y0 = card[1];
		int x1 = x0 + CARD_W;
		int y1 = y0 + CARD_H;
		int accent = accentColor(def);
		if (def.locked()) {
			accent = 0xFF555555;
		}
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

	private void drawCardText(GuiGraphics graphics, int[] card, ClassDefinition def) {
		int cx = card[0] + CARD_W / 2;
		int y = card[1] + 10;
		graphics.drawCenteredString(font, def.displayName(), cx, y, def.locked() ? NAME_LOCKED_COLOR : NAME_COLOR);
		y += 11;
		graphics.drawCenteredString(font, tagline(def), cx, y, TAGLINE_COLOR);
		y += 12;
		graphics.fill(card[0] + PAD, y, card[0] + CARD_W - PAD, y + 1, 0xFF3A3A3A);
		y += 6;

		if (def.locked()) {
			graphics.drawCenteredString(font, "Passives: ???", cx, y, PASSIVE_COLOR);
			y += 11;
			graphics.drawCenteredString(font, "Ability: ???", cx, y, PASSIVE_COLOR);
			y += 15;
			graphics.drawCenteredString(font, "COMING SOON", cx, y, TITLE_COLOR);
			return;
		}

		for (String line : passiveLines(def)) {
			graphics.drawCenteredString(font, line, cx, y, PASSIVE_COLOR);
			y += 11;
		}
		y += 3;
		String[] ability = abilityLines(def);
		for (String line : ability) {
			graphics.drawCenteredString(font, line, cx, y, ABILITY_COLOR);
			y += 11;
		}
	}

	private int[] cardRect(int index, int count) {
		int rowW = count * CARD_W + (count - 1) * GAP;
		int x = width / 2 - rowW / 2 + index * (CARD_W + GAP);
		return new int[]{x, cardsTop()};
	}

	private int cardsTop() {
		return Math.max(64, height / 2 - CARD_H / 2 + 12);
	}

	private static boolean isHovering(int[] card, int mouseX, int mouseY) {
		return mouseX >= card[0] && mouseX < card[0] + CARD_W && mouseY >= card[1] && mouseY < card[1] + CARD_H;
	}

	static int accentColor(ClassDefinition def) {
		return def.accentColor();
	}

	static String tagline(ClassDefinition def) {
		return def.tagline();
	}

	static List<String> passiveLines(ClassDefinition def) {
		List<String> lines = new ArrayList<>();
		for (Passive p : def.passives()) {
			lines.add(formatPassive(p));
		}
		if (lines.isEmpty()) {
			lines.add("No passives yet");
		}
		return lines;
	}

	static String[] abilityLines(ClassDefinition def) {
		if (def.activeAbility() == null) {
			return new String[]{"Ability: —"};
		}
		return def.abilityLines().toArray(new String[0]);
	}

	static String abilityBlurb(ClassDefinition def) {
		return def.abilityBlurb();
	}

	static String formatPassive(Passive p) {
		return com.hunter1712.infusedclasses.ClassAttributes.display(p);
	}

	static String attributeLabel(Identifier attribute) {
		return com.hunter1712.infusedclasses.ClassAttributes.displayName(attribute);
	}

	static String prettify(String path) {
		return com.hunter1712.infusedclasses.ClassAttributes.prettify(path);
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
