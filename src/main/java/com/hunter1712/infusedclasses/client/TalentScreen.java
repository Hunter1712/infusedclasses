package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.SkillTrees;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Card-per-tree talent panel in the class-select idiom. Juggernaut holds the
 * spine as icon slots with state-coloured lines beneath; Berserker and
 * Warlord are dimmed locked cards. Server stays authoritative: icon clicks
 * and respec are intents, state arrives via TalentStatePayload.
 */
public class TalentScreen extends Screen {
	private static final int GAP = 12;
	private static final int PAD = 10;
	private static final int JUG_W = 210;
	private static final int SIDE_W = 120;
	private static final int SMALL_BG = 20;
	private static final int BIG_BG = 26;
	private static final int COL_DX = 32;
	private static final int SMALL_ROW_H = 22;
	private static final int BIG_ROW_H = 30;
	private static final int SPINE_TOP = 44;

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int JUGGERNAUT_ACCENT = 0xFFE74C3C;
	private static final int BERSERKER_ACCENT = 0xFFE67E22;
	private static final int WARLORD_ACCENT = 0xFF9B59B6;
	private static final int LOCKED_ACCENT = 0xFF555555;
	private static final int UNLOCKED_BORDER = 0xFF4CAF50;
	private static final int AVAILABLE_BORDER = 0xFFFFFFFF;
	private static final int GATED_BORDER = 0xFF555555;

	private SkillTrees.Progress progress;
	private final List<NodeSlot> slots = new ArrayList<>();
	private int jugLeft;
	private int cardsTop;
	private int cardH;

	private record NodeSlot(SkillTrees.Node node, int x, int y, int size, boolean big, ItemStack icon) {
		boolean contains(int mx, int my) {
			return mx >= x && mx < x + size && my >= y && my < y + size;
		}
	}

	public TalentScreen(String state) {
		super(Component.literal("Talents"));
		this.progress = SkillTrees.decodeProgress(state);
	}

	@Override
	protected void init() {
		slots.clear();
		// Probe the spine height first so the card and respec button fit content.
		int spineEnd = SPINE_TOP;
		for (SkillTrees.Node node : SkillTrees.NODES) {
			boolean big = node.bonus() == null;
			if (big) {
				spineEnd += 2;
			}
			if (big) {
				spineEnd += BIG_ROW_H;
			} else if (!isLeftColumn(node.id())) {
				spineEnd += SMALL_ROW_H;
			}
		}
		int wantH = spineEnd + 8 + 20 + PAD;
		cardH = Math.max(200, wantH);
		int rowW = JUG_W + SIDE_W * 2 + GAP * 2;
		jugLeft = width / 2 - rowW / 2;
		int centered = height / 2 - cardH / 2 + 8;
		cardsTop = Math.max(8, Math.min(centered, Math.max(8, height - cardH - 8)));

		int cx = jugLeft + JUG_W / 2;
		int y = cardsTop + SPINE_TOP;
		for (SkillTrees.Node node : SkillTrees.NODES) {
			boolean big = node.bonus() == null;
			int size = big ? BIG_BG : SMALL_BG;
			int x;
			if (big) {
				x = cx - size / 2;
				y += 2;
			} else if (isLeftColumn(node.id())) {
				x = cx - COL_DX - size / 2;
			} else {
				x = cx + COL_DX - size / 2;
			}
			NodeSlot slot = new NodeSlot(node, x, y, size, big, iconFor(node));
			slots.add(slot);
			Button iconButton = Button.builder(Component.literal(""), b -> InfusedClassesClient.spendTalent(node.id()))
					.bounds(x, y, size, size)
					.tooltip(Tooltip.create(tooltipFor(node)))
					.build();
			iconButton.active = progress.unlocked.contains(node.id())
					|| SkillTrees.canUnlock(progress.unlocked, progress.unspent, node.id());
			addRenderableWidget(iconButton);
			if (big) {
				y += BIG_ROW_H;
			} else if (!isLeftColumn(node.id())) {
				y += SMALL_ROW_H;
			}
		}

		int spent = SkillTrees.spentPoints(progress.unlocked);
		int by = cardsTop + cardH - PAD - 20;
		Button respec = Button.builder(Component.literal("Respec (refund " + spent + ")"),
				b -> InfusedClassesClient.respecTalents())
				.bounds(jugLeft + PAD, by, JUG_W - PAD * 2, 20)
				.tooltip(Tooltip.create(Component.literal("Refund all spent points")))
				.build();
		respec.active = spent > 0;
		addRenderableWidget(respec);
	}

	private static boolean isLeftColumn(String id) {
		return id.equals("juggernaut_vigor1") || id.equals("juggernaut_vigor2")
				|| id.equals("juggernaut_vigor3") || id.equals("juggernaut_hide1")
				|| id.equals("juggernaut_vigor4") || id.equals("juggernaut_ward3");
	}

	private static ItemStack iconFor(SkillTrees.Node node) {
		String id = node.id();
		if (id.equals(SkillTrees.THICK_SKIN_NODE)) {
			return new ItemStack(Items.TURTLE_HELMET);
		}
		if (id.equals(SkillTrees.WAR_CRY_NODE)) {
			return new ItemStack(Items.GOAT_HORN);
		}
		if (id.equals(SkillTrees.ENDURING_SKIN_NODE)) {
			return new ItemStack(Items.TOTEM_OF_UNDYING);
		}
		if (node.bonus() == null) {
			return new ItemStack(Items.SHIELD);
		}
		return switch (node.bonus().attribute().getPath()) {
			case "max_health" -> new ItemStack(Items.APPLE);
			case "armor" -> new ItemStack(Items.IRON_CHESTPLATE);
			case "armor_toughness" -> new ItemStack(Items.DIAMOND_CHESTPLATE);
			case "knockback_resistance" -> new ItemStack(Items.SHIELD);
			default -> new ItemStack(Items.BREAD);
		};
	}

	private int stateColor(SkillTrees.Node node) {
		if (progress.unlocked.contains(node.id())) {
			return UNLOCKED_BORDER;
		}
		if (SkillTrees.canUnlock(progress.unlocked, progress.unspent, node.id())) {
			return AVAILABLE_BORDER;
		}
		return GATED_BORDER;
	}

	private Component tooltipFor(SkillTrees.Node node) {
		String reason = SkillTrees.unlockRejectReason(progress.unlocked, progress.unspent, node.id());
		String state = progress.unlocked.contains(node.id()) ? "Unlocked ✓"
				: reason == null ? "Available" : reason;
		return Component.literal(node.name() + " — " + node.blurb()
				+ " — cost " + node.cost() + " pt" + (node.cost() == 1 ? "" : "s")
				+ (node.gate() > 0 ? ", needs " + node.gate() + " spent" : "")
				+ " — " + state);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		int berLeft = jugLeft + JUG_W + GAP;
		int warLeft = berLeft + SIDE_W + GAP;
		drawCardFrame(graphics, jugLeft, cardsTop, JUG_W, cardH, JUGGERNAUT_ACCENT,
				isHovering(jugLeft, cardsTop, JUG_W, cardH, mouseX, mouseY), false);
		drawCardFrame(graphics, berLeft, cardsTop, SIDE_W, cardH, BERSERKER_ACCENT, false, true);
		drawCardFrame(graphics, warLeft, cardsTop, SIDE_W, cardH, WARLORD_ACCENT, false, true);
		drawSpineLines(graphics);
	}

	private void drawSpineLines(GuiGraphics graphics) {
		int cx = jugLeft + JUG_W / 2;
		NodeSlot prevCenter = null;
		for (NodeSlot slot : slots) {
			int color = stateColor(slot.node);
			if (prevCenter != null) {
				int x0 = cx - 1;
				int y0 = prevCenter.y + prevCenter.size;
				int y1 = slot.y + (slot.big ? slot.size / 2 : 0);
				if (y1 > y0) {
					graphics.fill(x0, y0, x0 + 2, y1, color);
				}
			}
			if (!slot.big) {
				int tickY = slot.y + slot.size / 2;
				if (slot.x + slot.size < cx) {
					graphics.fill(slot.x + slot.size, tickY, cx + 1, tickY + 1, color);
				} else if (slot.x > cx) {
					graphics.fill(cx - 1, tickY, slot.x, tickY + 1, color);
				}
			}
			if (slot.big) {
				prevCenter = slot;
			} else if (prevCenter == null || slot.y >= prevCenter.y + prevCenter.size + SMALL_ROW_H) {
				prevCenter = slot;
			}
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(font, Component.literal("Talents").withStyle(s -> s.withBold(true)),
				width / 2, cardsTop - 20, TITLE_COLOR);
		drawJugHeader(graphics);
		drawLockedText(graphics);
		for (NodeSlot slot : slots) {
			drawSlot(graphics, slot, slot.contains(mouseX, mouseY));
		}
	}

	private void drawJugHeader(GuiGraphics graphics) {
		int cx = jugLeft + JUG_W / 2;
		graphics.drawCenteredString(font, "Juggernaut", cx, cardsTop + 10, 0xFFFFFFFF);
		int next = SkillTrees.xpForNextLevel(progress.level);
		String sub = "Level " + progress.level + " • " + progress.xp + "/" + next + " XP • "
				+ progress.unspent + " pts";
		graphics.drawCenteredString(font, sub, cx, cardsTop + 22, SUBTITLE_COLOR);
		graphics.fill(jugLeft + PAD, cardsTop + 34, jugLeft + JUG_W - PAD, cardsTop + 35, 0xFF3A3A3A);
	}

	private void drawLockedText(GuiGraphics graphics) {
		int rowW = JUG_W + SIDE_W * 2 + GAP * 2;
		int berLeft = jugLeft + JUG_W + GAP;
		int warLeft = berLeft + SIDE_W + GAP;
		for (int i = 0; i < 2; i++) {
			int left = i == 0 ? berLeft : warLeft;
			String name = i == 0 ? "Berserker" : "Warlord";
			int cx = left + SIDE_W / 2;
			graphics.drawCenteredString(font, name, cx, cardsTop + 10, 0xFF8A8A8A);
			graphics.drawCenteredString(font, "Passives: ???", cx, cardsTop + 26, SUBTITLE_COLOR);
			graphics.drawCenteredString(font, "Ability: ???", cx, cardsTop + 37, SUBTITLE_COLOR);
			graphics.drawCenteredString(font, "COMING SOON", cx, cardsTop + 56, TITLE_COLOR);
		}
	}

	private void drawSlot(GuiGraphics graphics, NodeSlot slot, boolean hovered) {
		int x0 = slot.x;
		int y0 = slot.y;
		int x1 = x0 + slot.size;
		int y1 = y0 + slot.size;
		boolean unlocked = progress.unlocked.contains(slot.node.id());
		boolean available = unlocked || SkillTrees.canUnlock(progress.unlocked, progress.unspent, slot.node.id());
		graphics.fill(x0, y0, x1, y1, 0xE0141414);
		int border = stateColor(slot.node);
		if (hovered && available) {
			border = 0xFF8A8A8A;
		}
		graphics.fill(x0, y0, x1, y0 + 1, border);
		graphics.fill(x0, y1 - 1, x1, y1, border);
		graphics.fill(x0, y0, x0 + 1, y1, border);
		graphics.fill(x1 - 1, y0, x1, y1, border);
		if (hovered && available) {
			graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0x14FFFFFF);
		}
		if (!available) {
			graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0x50000000);
		}
		try {
			graphics.renderItem(slot.icon, x0 + (slot.size - 16) / 2, y0 + (slot.size - 16) / 2);
		} catch (NoSuchMethodError | UnsupportedOperationException fallback) {
			graphics.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, stateColor(slot.node));
		}
		if (unlocked) {
			graphics.drawString(font, "✓", x1 - 8, y0 + 1, UNLOCKED_BORDER, false);
		}
	}

	private void drawCardFrame(GuiGraphics graphics, int x, int y, int w, int h, int accent, boolean hovered, boolean locked) {
		if (locked) {
			accent = LOCKED_ACCENT;
		}
		graphics.fill(x, y, x + w, y + h, 0xE0141414);
		graphics.fill(x, y, x + w, y + 3, accent);
		int border = hovered && !locked ? 0xFF8A8A8A : 0xFF3A3A3A;
		graphics.fill(x, y, x + 1, y + h, border);
		graphics.fill(x + w - 1, y, x + w, y + h, border);
		graphics.fill(x, y, x + w, y + 1, border);
		graphics.fill(x, y + h - 1, x + w, y + h, border);
		if (hovered && !locked) {
			graphics.fill(x + 1, y + 3, x + w - 1, y + h - 1, 0x14FFFFFF);
		}
		if (locked) {
			graphics.fill(x + 1, y + 3, x + w - 1, y + h - 1, 0x50000000);
		}
	}

	private static boolean isHovering(int x, int y, int w, int h, int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
