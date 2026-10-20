package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.InfusedClasses;
import com.hunter1712.infusedclasses.PlayerStatusPayload;
import com.hunter1712.infusedclasses.StatusHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The stack on screen: the adapter that draws {@link StatusHud#rows} verbatim. The
 * decision returns the whole stack as rows of words; this lays them out and hides them.
 * Every row is one shadowed line of text on the left edge, the block vertically centred:
 * the anchor's word, one space, its number, then any riders glued on with the dotted
 * separator. Nothing is padded into columns — a lone space separates the word from its
 * count, as words are written.
 *
 * <p>A lit chip draws white; a ready lane draws dim with no number, so the missing
 * number is what marks it ready. No icon, no fill, no frame: only shadowed text.
 *
 * <p>Drawing stops when any screen is open, the HUD is hidden (F1), no snapshot exists,
 * or the snapshot's class id is empty (classless); each of those lives here, in the
 * adapter, because hiding is never the decision's. Countdowns interpolate from the
 * snapshot's receipt time ({@link StatusStore#elapsed}), and lit is the live flag the
 * decision already carried.
 */
public final class StatusHudElement implements HudElement {
	/** This element's own id in the fabric HUD registry. */
	static final Identifier ID = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "status");

	/** The pad between the stack and the left screen edge. */
	private static final int EDGE_PAD = 4;
	/** One row's line height: 9px glyphs plus a hair, and the gap between rows. 10 + 2 = 12 pitch. */
	private static final int ROW_HEIGHT = 10;
	private static final int ROW_GAP = 2;

	/** Lit chip is what counts now; dim recedes. Shadowed text over anything, no backdrop needed. */
	private static final int LIT_TEXT = 0xFFFFFFFF;
	private static final int DIM_TEXT = 0xFFA6A6A6;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null || minecraft.options.hideGui) {
			return;
		}
		PlayerStatusPayload status = StatusStore.status();
		if (status == null || status.classId() == null || status.classId().isEmpty()) {
			return;
		}
		draw(graphics, minecraft.font,
				StatusHud.rows(status, StatusStore.elapsed(InfusedClassesClient.now())));
	}

	/** The whole stack: rows top-down, the block centred on the screen's left edge. */
	private void draw(GuiGraphics graphics, Font font, List<StatusHud.Row> rows) {
		if (rows.isEmpty()) {
			return;
		}
		int blockHeight = rows.size() * ROW_HEIGHT + (rows.size() - 1) * ROW_GAP;
		int topY = Math.max(EDGE_PAD, (graphics.guiHeight() - blockHeight) / 2);
		for (int i = 0; i < rows.size(); i++) {
			drawRow(graphics, font, rows.get(i), topY + i * (ROW_HEIGHT + ROW_GAP));
		}
	}

	/** One row: the word, one space, its number, then riders; every chip draws as its own plain text. */
	private static void drawRow(GuiGraphics graphics, Font font, StatusHud.Row row, int y) {
		int x = EDGE_PAD;
		for (StatusHud.Chip chip : row.chips()) {
			String text = chip.rider()
					? " \u00b7 " + (chip.label().isEmpty() ? "" : chip.label() + " ") + chip.value()
					: chip.label() + (chip.value().isEmpty() ? "" : " " + chip.value());
			graphics.drawString(font, text, x, y, chip.lit() ? LIT_TEXT : DIM_TEXT);
			x += font.width(text);
		}
	}
}
