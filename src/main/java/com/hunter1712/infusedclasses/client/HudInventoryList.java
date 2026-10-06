package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ConditionalPassive;
import com.hunter1712.infusedclasses.PassiveHud;
import com.hunter1712.infusedclasses.mixin.client.ContainerScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Inventory half of the HUD overlay: vanilla-metric effect boxes appended below vanilla's own
 * list rows. Draws verbatim whatever the row decision returns; hidden rows are skipped, like
 * the strip. Geometry comes from the screen access mixin, icons from the overlay's sprite map.
 */
final class HudInventoryList {
	private static final Identifier BOX_BACKGROUND =
			Identifier.withDefaultNamespace("container/inventory/effect_background");

	private static final int ROW_STEP = 33;
	private static final int BOX_H = 32;

	private HudInventoryList() {
	}

	/** Inventory: vanilla-metric boxes appended below vanilla's own list rows. */
	static void render(GuiGraphics graphics, Minecraft mc, AbstractContainerScreen<?> screen,
			List<ConditionalPassive> visible, PassiveHud.Row warCry, long now) {
		ContainerScreenAccess geometry = (ContainerScreenAccess) screen;
		int x = geometry.infusedclasses$leftPos() + geometry.infusedclasses$imageWidth() + 2;
		int room = graphics.guiWidth() - x;
		if (room < 32) {
			return;
		}
		int maxW = room >= 120 ? room - 7 : 32;
		// Shown effects only: iconless ones (War Cry's own Strength) are filtered from
		// vanilla's list by the client mixin, so they must not reserve a row here either.
		int vanillaRows = (int) mc.player.getActiveEffects().stream()
				.filter(effect -> effect.showIcon()).count();
		int step = vanillaRows > 5 ? 132 / (vanillaRows - 1) : ROW_STEP;
		Font font = mc.font;
		int row = 0;
		for (ConditionalPassive passive : visible) {
			PassiveHud.Row decided = PassiveHud.rowFor(passive,
					ClientPassiveStacks.stacksFor(passive, now),
					ClientPassiveStacks.millisUntilDecay(passive.name(), now));
			if (decided.hidden()) {
				continue;
			}
			drawBox(graphics, font, x, boxY(geometry, step, vanillaRows + row), maxW, decided);
			row++;
		}
		if (!warCry.hidden()) {
			drawBox(graphics, font, x, boxY(geometry, step, vanillaRows + row), maxW, warCry);
		}
	}

	/** One inventory row's top edge: vanilla rows first, ours appended below in the same step. */
	private static int boxY(ContainerScreenAccess geometry, int step, int index) {
		return geometry.infusedclasses$topPos() + step * index;
	}

	/** One vanilla inventory row: background box sized off the text, icon, name, `mm:ss`. */
	private static void drawBox(GuiGraphics graphics, Font font, int x, int y, int maxW, PassiveHud.Row row) {
		String title = row.title();
		String duration = duration(row.secondsLeft());
		int w = Math.min(maxW, Math.max(32 + font.width(title) + 7, 32 + font.width(duration) + 7));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_BACKGROUND, x, y, w, BOX_H);
		Identifier sprite = PassiveHudOverlay.spriteFor(row.iconId());
		if (sprite != null) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 7, y + 7, 18, 18);
		}
		if (w > 32 + 7) {
			graphics.drawString(font, title, x + 32, y + 7, 0xFFFFFFFF);
			graphics.drawString(font, duration, x + 32, y + 16, 0xFF808080);
		}
	}

	/** Vanilla duration read: `7` renders as `00:07`. */
	private static String duration(int secondsLeft) {
		return String.format(Locale.ROOT, "%02d:%02d", secondsLeft / 60, secondsLeft % 60);
	}
}
