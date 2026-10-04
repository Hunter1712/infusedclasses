package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.SkillTrees;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Minimal BL2-style talent panel. No vanilla widgets: slots, respec, and
 * tooltips are all custom-drawn flat frames with state colors. Server stays
 * authoritative: slot clicks and respec are intents, state arrives via
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
	private static final int TIP_W = 190;

	private static final int GOLD = 0xFFE8C15A;
	private static final int GREY = 0xFF9A9A9A;
	private static final int JUGGERNAUT_ACCENT = 0xFFE74C3C;
	private static final int BERSERKER_ACCENT = 0xFFE67E22;
	private static final int LOCKED_ACCENT = 0xFF555555;
	private static final int UNLOCKED_BORDER = 0xFF4CAF50;
	private static final int AVAILABLE_BORDER = 0xFFCFCFCF;
	private static final int GATED_BORDER = 0xFF555555;
	private static final int CAPSTONE_BORDER = 0xFFFFD75E;
	private static final int RED = 0xFFFF5555;

	private SkillTrees.Progress progress;
	private final List<NodeSlot> slots = new ArrayList<>();
	private int jugLeft;
	private int berLeft;
	private int cardsTop;
	private int cardH;
	private int maxRow;
	private int respecX;
	private int respecY;
	private int respecW;
	private int respecH;

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
		int wantH = GRID_TOP + maxRow * PITCH_Y + 90;
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
			slots.add(new NodeSlot(node, x, y, size));
		}

		String label = "RESPEC · REFUND " + SkillTrees.spentPoints(progress.ranks);
		respecW = font.width(label) + 16;
		respecH = 16;
		respecX = jugLeft + JUG_W / 2 - respecW / 2;
		respecY = cardsTop + cardH - PAD - respecH - 14;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
		int mx = (int) click.x();
		int my = (int) click.y();
		if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (NodeSlot slot : slots) {
				if (slot.contains(mx, my)) {
					InfusedClassesClient.spendTalent(slot.node.id());
					return true;
				}
			}
			if (mx >= respecX && mx < respecX + respecW && my >= respecY && my < respecY + respecH
					&& SkillTrees.spentPoints(progress.ranks) > 0) {
				InfusedClassesClient.respecTalents();
				return true;
			}
		}
		return super.mouseClicked(click, doubleClick);
	}

	private Component tooltipTitle(SkillTrees.Node node) {
		int rank = SkillTrees.rankOf(progress.ranks, node.id());
		String ranks = node.maxRank() > 1 ? " " + rank + "/" + node.maxRank() : "";
		return Component.literal(node.name().toUpperCase(java.util.Locale.ROOT) + ranks);
	}

	private String tooltipInfo(SkillTrees.Node node) {
		int tierNeed = SkillTrees.TIER_REQ.get(node.tier());
		String cost = node.maxRank() > 1 ? "1 pt/rank" : "cost " + node.cost() + " pt";
		String tier = tierNeed > 0 ? " · needs " + tierNeed + " spent" : "";
		return node.blurb() + " — " + cost + tier;
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

	private int stateTextColor(SkillTrees.Node node) {
		if (SkillTrees.rankOf(progress.ranks, node.id()) > 0) {
			return UNLOCKED_BORDER;
		}
		if (SkillTrees.canUnlock(progress.ranks, progress.unspent, node.id())) {
			return 0xFFFFFFFF;
		}
		return RED;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
		drawCardFrame(graphics, jugLeft, cardsTop, JUG_W, cardH, JUGGERNAUT_ACCENT, false);
		drawCardFrame(graphics, berLeft, cardsTop, SIDE_W, cardH, BERSERKER_ACCENT, true);
		drawTierDividers(graphics);
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
			String label = tier == 4 ? "CAPSTONE · 12 PTS"
					: "TIER " + tier + (tier == 1 ? "" : " · " + SkillTrees.TIER_REQ.get(tier) + " PTS");
			graphics.drawCenteredString(font, label, cx, y - 4, GREY);
			lastTier = tier;
		}
	}

	private static boolean isRing(SkillTrees.Node node) {
		return node.bonus() == null && !node.parents().isEmpty() && node.tier() == 3;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(font, Component.literal("TALENTS").withStyle(s -> s.withBold(true)),
				width / 2, cardsTop - 20, GOLD);
		drawJugHeader(graphics);
		drawLockedText(graphics);
		NodeSlot hovered = null;
		for (NodeSlot slot : slots) {
			boolean hot = slot.contains(mouseX, mouseY);
			drawSlot(graphics, slot, hot);
			if (hot) {
				hovered = slot;
			}
		}
		drawRespec(graphics, mouseX, mouseY);
		drawNamePlates(graphics);
		if (hovered != null) {
			drawTooltip(graphics, hovered, mouseX, mouseY);
		}
	}

	private void drawJugHeader(GuiGraphics graphics) {
		int cx = jugLeft + JUG_W / 2;
		graphics.drawCenteredString(font, "JUGGERNAUT", cx, cardsTop + 10, 0xFFFFFFFF);
		graphics.drawCenteredString(font, "DEFENCE", cx, cardsTop + 21, GREY);
		int next = SkillTrees.xpForNextLevel(progress.level);
		String sub = "LV " + progress.level + " · " + progress.xp + "/" + next + " XP · "
				+ progress.unspent + " PTS";
		graphics.drawCenteredString(font, sub, cx, cardsTop + 32, GREY);
		int bx0 = cx - 70;
		int by0 = cardsTop + 44;
		int bx1 = bx0 + 140;
		int by1 = by0 + 20;
		graphics.fill(bx0, by0, bx1, by1, 0xE0141414);
		graphics.fill(bx0, by0, bx1, by0 + 1, UNLOCKED_BORDER);
		graphics.fill(bx0, by1 - 1, bx1, by1, UNLOCKED_BORDER);
		graphics.fill(bx0, by0, bx0 + 1, by1, UNLOCKED_BORDER);
		graphics.fill(bx1 - 1, by0, bx1, by1, UNLOCKED_BORDER);
		try {
			graphics.renderItem(new ItemStack(Items.GOAT_HORN), bx0 + 4, by0 + 2);
		} catch (NoSuchMethodError | UnsupportedOperationException fallback) {
			graphics.fill(bx0 + 4, by0 + 2, bx0 + 20, by0 + 18, UNLOCKED_BORDER);
		}
		graphics.drawString(font, "WAR CRY", bx0 + 24, by0 + 6, 0xFFFFFFFF, false);
		String badge = "1/1";
		int bw = font.width(badge) + 6;
		graphics.fill(bx1 - bw + 2, by1 - 5, bx1 + 2, by1 + 6, 0xF0101010);
		graphics.drawString(font, badge, bx1 - bw + 5, by1 - 3, UNLOCKED_BORDER, false);
	}

	private void drawLockedText(GuiGraphics graphics) {
		int cx = berLeft + SIDE_W / 2;
		graphics.drawCenteredString(font, "BERSERKER", cx, cardsTop + 10, 0xFF8A8A8A);
		graphics.drawCenteredString(font, "OFFENCE", cx, cardsTop + 21, GREY);
		graphics.drawCenteredString(font, "COMING SOON", cx, cardsTop + 40, GOLD);
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
		int thick = capstone ? 2 : 1;
		graphics.fill(x0, y0, x1, y0 + thick, border);
		graphics.fill(x0, y1 - thick, x1, y1, border);
		graphics.fill(x0, y0, x0 + thick, y1, border);
		graphics.fill(x1 - thick, y0, x1, y1, border);
		if (!available) {
			graphics.fill(x0 + thick, y0 + thick, x1 - thick, y1 - thick, 0x50000000);
		}
		try {
			graphics.renderItem(iconFor(slot.node), x0 + (slot.size - 16) / 2, y0 + (slot.size - 16) / 2);
		} catch (NoSuchMethodError | UnsupportedOperationException fallback) {
			graphics.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, stateColor(slot.node));
		}
		String badge = rank + "/" + slot.node.maxRank();
		int color = owned ? UNLOCKED_BORDER : available ? 0xFFFFFFFF : 0xFF8A8A8A;
		int bw = font.width(badge) + 6;
		int bh = 11;
		int bx = x1 - bw + 2;
		int by = y1 - bh + 2;
		graphics.fill(bx, by, bx + bw, by + bh, 0xF0101010);
		graphics.drawString(font, badge, bx + 3, by + 2, color, false);
	}

	private void drawNamePlates(GuiGraphics graphics) {
		graphics.drawCenteredString(font, "JUGGERNAUT", jugLeft + JUG_W / 2, cardsTop + cardH - 12,
				JUGGERNAUT_ACCENT);
		graphics.drawCenteredString(font, "BERSERKER", berLeft + SIDE_W / 2, cardsTop + cardH - 12,
				0xFF8A8A8A);
	}

	private void drawRespec(GuiGraphics graphics, int mouseX, int mouseY) {
		int spent = SkillTrees.spentPoints(progress.ranks);
		boolean hot = mouseX >= respecX && mouseX < respecX + respecW && mouseY >= respecY && mouseY < respecY + respecH;
		int color = spent > 0 ? hot ? 0xFFFFFFFF : GREY : 0xFF555555;
		String label = "RESPEC · REFUND " + spent;
		graphics.drawCenteredString(font, label, jugLeft + JUG_W / 2, respecY + 4, color);
		if (hot && spent > 0) {
			int w = font.width(label);
			int cx = jugLeft + JUG_W / 2;
			graphics.fill(cx - w / 2, respecY + 13, cx + w / 2, respecY + 14, color);
		}
	}

	private void drawTooltip(GuiGraphics graphics, NodeSlot slot, int mouseX, int mouseY) {
		SkillTrees.Check check = SkillTrees.check(progress.ranks, progress.unspent, slot.node.id());
		String state = SkillTrees.rankOf(progress.ranks, slot.node.id()) > 0 ? "Owned"
				: SkillTrees.describe(check, slot.node);
		List<FormattedCharSequence> body = font.split(Component.literal(tooltipInfo(slot.node)), TIP_W);
		int h = (body.size() + 2) * 10 + 12;
		int w = TIP_W + 16;
		int x = mouseX + 14;
		int y = mouseY - 10;
		if (x + w > width) {
			x = mouseX - w - 10;
		}
		if (y + h > height) {
			y = height - h - 4;
		}
		graphics.fill(x, y, x + w, y + h, 0xF0101010);
		graphics.fill(x, y, x + w, y + 1, 0xFF6A6A6A);
		graphics.fill(x, y + h - 1, x + w, y + h, 0xFF6A6A6A);
		graphics.fill(x, y, x + 1, y + h, 0xFF6A6A6A);
		graphics.fill(x + w - 1, y, x + w, y + h, 0xFF6A6A6A);
		graphics.drawString(font, tooltipTitle(slot.node), x + 8, y + 6, GOLD, false);
		int ly = y + 18;
		for (FormattedCharSequence line : body) {
			graphics.drawString(font, line, x + 8, ly, GREY, false);
			ly += 10;
		}
		graphics.drawString(font, state, x + 8, ly, stateTextColor(slot.node), false);
	}

	private void drawCardFrame(GuiGraphics graphics, int x, int y, int w, int h, int accent, boolean locked) {
		if (locked) {
			accent = LOCKED_ACCENT;
		}
		graphics.fill(x, y, x + w, y + h, 0xE0141414);
		graphics.fill(x, y, x + w, y + 2, accent);
		graphics.fill(x, y, x + 1, y + h, 0xFF2A2A2A);
		graphics.fill(x + w - 1, y, x + w, y + h, 0xFF2A2A2A);
		graphics.fill(x, y, x + w, y + 1, 0xFF2A2A2A);
		graphics.fill(x, y + h - 1, x + w, y + h, 0xFF2A2A2A);
		if (locked) {
			graphics.fill(x + 1, y + 2, x + w - 1, y + h - 1, 0x50000000);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
