package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.SkillTrees;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Card talent panel in the class-select idiom, rendered generically from node
 * grid coordinates. Juggernaut holds ranked slots with pips, tier dividers,
 * and explicit attachment edges; Berserker is a dimmed locked card. Server
 * stays authoritative: slot clicks and respec are intents, state arrives via
 * TalentStatePayload.
 */
public class TalentScreen extends Screen {
	private static final int GAP = 12;
	private static final int PAD = 10;
	private static final int JUG_W = 200;
	private static final int SIDE_W = 110;
	private static final int SMALL_BG = 20;
	private static final int BIG_BG = 26;
	private static final int PITCH_X = 56;
	private static final int PITCH_Y = 40;
	private static final int GRID_TOP = 96;

	private static final int TITLE_COLOR = 0xFFE8C15A;
	private static final int SUBTITLE_COLOR = 0xFF9A9A9A;
	private static final int JUGGERNAUT_ACCENT = 0xFFE74C3C;
	private static final int BERSERKER_ACCENT = 0xFFE67E22;
	private static final int LOCKED_ACCENT = 0xFF555555;
	private static final int UNLOCKED_BORDER = 0xFF4CAF50;
	private static final int AVAILABLE_BORDER = 0xFFFFFFFF;
	private static final int GATED_BORDER = 0xFF555555;
	private static final int CAPSTONE_BORDER = 0xFFFFD75E;

	private SkillTrees.Progress progress;
	private final List<NodeSlot> slots = new ArrayList<>();
	private int jugLeft;
	private int berLeft;
	private int cardsTop;
	private int cardH;
	private int maxRow;

	private record NodeSlot(SkillTrees.Node node, int x, int y, int size) {
		boolean contains(int mx, int my) {
			return mx >= x && mx < x + size && my >= y && my < y + size;
		}

		int cx() {
			return x + size / 2;
		}
	}

	public TalentScreen(String state) {
		super(Component.literal("Talents"));
		this.progress = SkillTrees.decodeProgress(state);
	}

	@Override
	protected void init() {
		slots.clear();
		maxRow = 0;
		for (SkillTrees.Node node : SkillTrees.NODES) {
			maxRow = Math.max(maxRow, node.row());
		}
		int wantH = GRID_TOP + maxRow * PITCH_Y + 76;
		cardH = Math.max(200, wantH);
		jugLeft = width / 2 - (JUG_W + SIDE_W + GAP) / 2;
		berLeft = jugLeft + JUG_W + GAP;
		int centered = height / 2 - cardH / 2 + 8;
		cardsTop = Math.max(8, Math.min(centered, Math.max(8, height - cardH - 8)));

		int cx = jugLeft + JUG_W / 2;
		for (SkillTrees.Node node : SkillTrees.NODES) {
			boolean big = node.bonus() == null && !isRing(node);
			int size = big ? BIG_BG : SMALL_BG;
			int x = cx + node.col() * PITCH_X - PITCH_X - size / 2;
			int y = cardsTop + GRID_TOP + node.row() * PITCH_Y - size / 2;
			NodeSlot slot = new NodeSlot(node, x, y, size);
			slots.add(slot);
			Button iconButton = Button.builder(Component.literal(""), b -> InfusedClassesClient.spendTalent(node.id()))
					.bounds(x, y, size, size)
					.tooltip(Tooltip.create(tooltipFor(node)))
					.build();
			iconButton.active = SkillTrees.rankOf(progress.ranks, node.id()) > 0
					|| SkillTrees.canUnlock(progress.ranks, progress.unspent, node.id());
			addRenderableWidget(iconButton);
		}

		int spent = SkillTrees.spentPoints(progress.ranks);
		int by = cardsTop + cardH - PAD - 20;
		Button respec = Button.builder(Component.literal("Respec (refund " + spent + ")"),
				b -> InfusedClassesClient.respecTalents())
				.bounds(jugLeft + PAD, by, JUG_W - PAD * 2, 20)
				.tooltip(Tooltip.create(Component.literal("Refund all spent ranks")))
				.build();
		respec.active = spent > 0;
		addRenderableWidget(respec);
	}

	private Component tooltipFor(SkillTrees.Node node) {
		int rank = SkillTrees.rankOf(progress.ranks, node.id());
		SkillTrees.Check check = SkillTrees.check(progress.ranks, progress.unspent, node.id());
		String rankRead = node.maxRank() > 1 ? "Rank " + rank + "/" + node.maxRank() + " — " : "";
		int tierNeed = SkillTrees.TIER_REQ.get(node.tier());
		return Component.literal(node.name() + " — " + node.blurb()
				+ " — " + rankRead + "cost " + node.cost() + " pt"
				+ (tierNeed > 0 ? ", needs " + tierNeed + " spent" : "")
				+ " — " + SkillTrees.describe(check, node));
	}

	private static ItemStack iconFor(SkillTrees.Node node) {
		if (node.icon() == null) {
			return new ItemStack(Items.BREAD);
		}
		return switch (node.icon().getPath()) {
			case "apple" -> new ItemStack(Items.APPLE);
			case "turtle_helmet" -> new ItemStack(Items.TURTLE_HELMET);
			case "totem_of_undying" -> new ItemStack(Items.TOTEM_OF_UNDYING);
			case "iron_chestplate" -> new ItemStack(Items.IRON_CHESTPLATE);
			case "goat_horn" -> new ItemStack(Items.GOAT_HORN);
			case "shield" -> new ItemStack(Items.SHIELD);
			case "netherite_chestplate" -> new ItemStack(Items.NETHERITE_CHESTPLATE);
			default -> new ItemStack(Items.BREAD);
		};
	}

	private int stateColor(SkillTrees.Node node) {
		if (SkillTrees.rankOf(progress.ranks, node.id()) > 0) {
			return UNLOCKED_BORDER;
		}
		if (SkillTrees.canUnlock(progress.ranks, progress.unspent, node.id())) {
			return AVAILABLE_BORDER;
		}
		return GATED_BORDER;
	}

	private NodeSlot slotOf(String id) {
		for (NodeSlot slot : slots) {
			if (slot.node.id().equals(id)) {
				return slot;
			}
		}
		return null;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		drawCardFrame(graphics, jugLeft, cardsTop, JUG_W, cardH, JUGGERNAUT_ACCENT,
				isHovering(jugLeft, cardsTop, JUG_W, cardH, mouseX, mouseY), false);
		drawCardFrame(graphics, berLeft, cardsTop, SIDE_W, cardH, BERSERKER_ACCENT, false, true);
		drawTierDividers(graphics);
		drawEdges(graphics);
	}

	private void drawTierDividers(GuiGraphics graphics) {
		int cx = jugLeft + JUG_W / 2;
		int lastTier = 0;
		for (int row = 0; row <= maxRow; row++) {
			int tier = 0;
			for (NodeSlot slot : slots) {
				if (slot.node.row() == row && !isRing(slot.node)) {
					tier = Math.max(tier, slot.node.tier());
				}
			}
			if (tier <= lastTier) {
				continue;
			}
			int y = cardsTop + GRID_TOP + row * PITCH_Y - PITCH_Y / 2;
			String label = tier == 4 ? "CAPSTONE — 12 PTS" : "TIER " + tier + (tier == 1 ? "" : " — " + SkillTrees.TIER_REQ.get(tier) + " PTS");
			graphics.drawCenteredString(font, label, cx, y - 4, SUBTITLE_COLOR);
			graphics.fill(jugLeft + PAD, y + 8, jugLeft + JUG_W - PAD, y + 9, 0xFF3A3A3A);
			lastTier = tier;
		}
	}

	private static boolean isRing(SkillTrees.Node node) {
		return node.bonus() == null && !node.parents().isEmpty() && node.tier() == 3;
	}

	private void drawEdges(GuiGraphics graphics) {
		drawEdge(graphics, null, SkillTrees.WAR_CRY_NODE);
		for (NodeSlot slot : slots) {
			for (String parent : slot.node.parents()) {
				drawEdge(graphics, parent, slot.node.id());
			}
		}
	}

	private void drawEdge(GuiGraphics graphics, String fromId, String toId) {
		NodeSlot to = slotOf(toId);
		if (to == null) {
			return;
		}
		int color = stateColor(to.node);
		int x2 = to.cx();
		int y2 = to.y - 2;
		int x1;
		int y1;
		if (fromId == null) {
			x1 = x2;
			y1 = cardsTop + 66;
		} else {
			NodeSlot from = slotOf(fromId);
			if (from == null) {
				return;
			}
			x1 = from.cx();
			y1 = from.y + from.size + 1;
		}
		if (y2 <= y1) {
			return;
		}
		if (x1 == x2) {
			graphics.fill(x1 - 1, y1, x1 + 1, y2, color);
			return;
		}
		int midY = y1 + (y2 - y1) / 2;
		graphics.fill(x1 - 1, y1, x1 + 1, midY + 1, color);
		graphics.fill(Math.min(x1, x2), midY, Math.max(x1, x2) + 1, midY + 1, color);
		graphics.fill(x2 - 1, midY, x2 + 1, y2, color);
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
		graphics.drawCenteredString(font, "Defence — tank", cx, cardsTop + 21, SUBTITLE_COLOR);
		int next = SkillTrees.xpForNextLevel(progress.level);
		String sub = "Level " + progress.level + " • " + progress.xp + "/" + next + " XP • "
				+ progress.unspent + " pts";
		graphics.drawCenteredString(font, sub, cx, cardsTop + 32, SUBTITLE_COLOR);
		graphics.fill(jugLeft + PAD, cardsTop + 44, jugLeft + JUG_W - PAD, cardsTop + 45, 0xFF3A3A3A);
		int bx0 = cx - 70;
		int by0 = cardsTop + 50;
		graphics.fill(bx0, by0, bx0 + 140, by0 + 16, 0xE0141414);
		graphics.fill(bx0, by0, bx0 + 140, by0 + 1, UNLOCKED_BORDER);
		graphics.fill(bx0, by0 + 15, bx0 + 140, by0 + 16, UNLOCKED_BORDER);
		graphics.fill(bx0, by0, bx0 + 1, by0 + 16, UNLOCKED_BORDER);
		graphics.fill(bx0 + 139, by0, bx0 + 140, by0 + 16, UNLOCKED_BORDER);
		graphics.drawCenteredString(font, "WAR CRY — owned ✓", cx, by0 + 4, 0xFFFFFFFF);
	}

	private void drawLockedText(GuiGraphics graphics) {
		int cx = berLeft + SIDE_W / 2;
		graphics.drawCenteredString(font, "Berserker", cx, cardsTop + 10, 0xFF8A8A8A);
		graphics.drawCenteredString(font, "Offence — damage", cx, cardsTop + 21, SUBTITLE_COLOR);
		graphics.drawCenteredString(font, "COMING SOON", cx, cardsTop + 40, TITLE_COLOR);
	}

	private void drawSlot(GuiGraphics graphics, NodeSlot slot, boolean hovered) {
		int x0 = slot.x;
		int y0 = slot.y;
		int x1 = x0 + slot.size;
		int y1 = y0 + slot.size;
		int rank = SkillTrees.rankOf(progress.ranks, slot.node.id());
		boolean owned = rank > 0;
		boolean available = owned || SkillTrees.canUnlock(progress.ranks, progress.unspent, slot.node.id());
		boolean capstone = slot.node.id().equals(SkillTrees.RALLYING_NODE);
		graphics.fill(x0, y0, x1, y1, 0xE0141414);
		int border = capstone && available ? CAPSTONE_BORDER : stateColor(slot.node);
		if (hovered && available) {
			border = 0xFF8A8A8A;
		}
		int thick = capstone ? 2 : 1;
		graphics.fill(x0, y0, x1, y0 + thick, border);
		graphics.fill(x0, y1 - thick, x1, y1, border);
		graphics.fill(x0, y0, x0 + thick, y1, border);
		graphics.fill(x1 - thick, y0, x1, y1, border);
		if (hovered && available) {
			graphics.fill(x0 + thick, y0 + thick, x1 - thick, y1 - thick, 0x14FFFFFF);
		}
		if (!available) {
			graphics.fill(x0 + thick, y0 + thick, x1 - thick, y1 - thick, 0x50000000);
		}
		try {
			graphics.renderItem(iconFor(slot.node), x0 + (slot.size - 16) / 2, y0 + (slot.size - 16) / 2);
		} catch (NoSuchMethodError | UnsupportedOperationException fallback) {
			graphics.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, stateColor(slot.node));
		}
		if (owned) {
			graphics.drawString(font, "✓", x1 - 8, y0 + 1, UNLOCKED_BORDER, false);
		}
		if (slot.node.maxRank() > 1) {
			String pips = rank + "/" + slot.node.maxRank();
			graphics.drawCenteredString(font, pips, slot.cx(), y1 + 2, owned || available ? 0xFFFFFFFF : SUBTITLE_COLOR);
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
