package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.SkillTrees;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Juggernaut talent panel. Same picker idiom as the class screen: server is
 * authoritative, clicks are intents, state arrives via TalentStatePayload.
 */
public class TalentScreen extends Screen {
	private static final int ROW_W = 240;
	private static final int ROW_H = 20;
	private static final int GAP = 6;

	private SkillTrees.Progress progress;

	public TalentScreen(String state) {
		super(Component.literal("Juggernaut Talents"));
		this.progress = SkillTrees.decodeProgress(state);
	}

	@Override
	protected void init() {
		int y = Math.max(64, height / 2 - (SkillTrees.NODES.size() * (ROW_H + GAP)) / 2);
		for (SkillTrees.Node node : SkillTrees.NODES) {
			String label = label(node);
			String tip = node.blurb() + " — cost " + node.cost() + " pt"
					+ (node.cost() == 1 ? "" : "s")
					+ (node.gate() > 0 ? ", needs " + node.gate() + " spent" : "");
			Button button = Button.builder(Component.literal(label), b -> InfusedClassesClient.spendTalent(node.id()))
					.bounds(width / 2 - ROW_W / 2, y, ROW_W, ROW_H)
					.tooltip(Tooltip.create(Component.literal(tip)))
					.build();
			button.active = progress.unlocked.contains(node.id())
					|| SkillTrees.canUnlock(progress.unlocked, progress.unspent, node.id());
			addRenderableWidget(button);
			y += ROW_H + GAP;
		}
		for (String branch : SkillTrees.LOCKED_BRANCHES) {
			Button locked = Button.builder(Component.literal(branch + " (locked — coming soon)"), b -> {})
					.bounds(width / 2 - ROW_W / 2, y, ROW_W, ROW_H)
					.tooltip(Tooltip.create(Component.literal(branch + " branch is not playable in MVP")))
					.build();
			locked.active = false;
			addRenderableWidget(locked);
			y += ROW_H + GAP;
		}
	}

	private String label(SkillTrees.Node node) {
		if (progress.unlocked.contains(node.id())) {
			return node.name() + " ✓";
		}
		String reason = SkillTrees.unlockRejectReason(progress.unlocked, progress.unspent, node.id());
		if (reason == null) {
			return node.name() + " [" + node.cost() + " pt]";
		}
		return node.name() + " (" + reason + ")";
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int top = Math.max(64, height / 2 - (SkillTrees.NODES.size() * (ROW_H + GAP)) / 2);
		graphics.drawCenteredString(font, "Juggernaut Talents", width / 2, top - 44, 0xFFE8C15A);
		int next = SkillTrees.xpForNextLevel(progress.level);
		String sub = progress.level >= SkillTrees.MAX_LEVEL
				? "Level " + progress.level + " (max) • " + progress.unspent + " pts"
				: "Level " + progress.level + " • " + progress.xp + "/" + next + " XP • " + progress.unspent + " pts";
		graphics.drawCenteredString(font, sub, width / 2, top - 28, 0xFF9A9A9A);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
