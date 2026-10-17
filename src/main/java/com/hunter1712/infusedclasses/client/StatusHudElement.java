package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassPackets;
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
 * The strip on screen: the adapter that draws {@link StatusHud#rows} verbatim. The decision
 * returns the whole strip as rows; this lays it out and hides it. One vertical strip of
 * uniform badge boxes in vanilla-slot style — sized once for the three-character badges,
 * height 20 — with the text label on the inner side, mirrored per corner, the strip growing
 * up from its bottom corner and the first row anchored on the corner, so a row appearing or
 * dropping never moves the rows under it. Letters and numbers only: no icons, no textures,
 * no animations, and no backdrop behind the chips — the drawn box is an outline only.
 *
 * <p>Drawing stops when the config disables it, any screen is open, the HUD is hidden (F1),
 * no snapshot exists, or the snapshot's class id is empty (classless); each of those lives
 * here, in the adapter, because hiding is never the decision's. Countdowns interpolate from
 * the snapshot's receipt time ({@link StatusStore#elapsed}), and a chip's dim/lit is the
 * live flag the decision already carried: ready lanes draw dim, live chips draw lit.
 */
public final class StatusHudElement implements HudElement {
	/** This element's own id in the fabric HUD registry. */
	static final Identifier ID = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "status");

	/** The uniform badge box: height 20 like a vanilla slot, width fit once for the badges. */
	private static final int BOX_H = 20;
	private static final int BADGE_PAD = 4;
	private static final int ROW_GAP = 2;
	private static final int CHIP_GAP = 4;
	/** The text label sits beside its box, this far off it. */
	private static final int TEXT_PAD = 2;
	private static final int MARGIN = 5;

	/** Shadowed text over anything: lit border and text are legible, dim border and text recede. */
	private static final int LIT_BORDER = 0xFF8B8B8B;
	private static final int LIT_TEXT = 0xFFFFFFFF;
	private static final int LIT_LABEL = 0xFFD4D4D4;
	private static final int DIM_BORDER = 0xFF565656;
	private static final int DIM_TEXT = 0xFF8C8C8C;

	/** The badge vocabulary the uniform box is sized for; slot and state names, never keys. */
	private static final String[] BADGES = {
			StatusHud.SIG, StatusHud.ULT, StatusHud.MOM, StatusHud.STR,
			StatusHud.GLA, StatusHud.SCU, StatusHud.IMP, StatusHud.TES, StatusHud.AEQ, StatusHud.ALL };

	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null || minecraft.options.hideGui) {
			return;
		}
		HudConfig config = InfusedClassesClient.hudConfig();
		if (!config.enabled()) {
			return;
		}
		PlayerStatusPayload status = StatusStore.status();
		if (status == null || status.classId() == null || status.classId().isEmpty()) {
			return;
		}
		draw(graphics, minecraft.font, StatusHud.rows(status, StatusStore.elapsed(System.currentTimeMillis())),
				config.corner());
	}

	/** The whole strip: one row of chips per decision row, stacked upward from the corner. */
	private void draw(GuiGraphics graphics, Font font, List<StatusHud.Row> rows, HudConfig.Corner corner) {
		int y = graphics.guiHeight() - MARGIN - BOX_H;
		int boxW = boxWidth(font);
		boolean left = corner == HudConfig.Corner.BOTTOM_LEFT;
		for (StatusHud.Row row : rows) {
			if (row.chips().isEmpty()) {
				continue;
			}
			drawRow(graphics, font, row, y, boxW, left);
			y -= BOX_H + ROW_GAP;
		}
	}

	/** One row of chips, drawn from the anchored corner inward: the lane or stance first, riders after. */
	private void drawRow(GuiGraphics graphics, Font font, StatusHud.Row row, int y, int boxW, boolean left) {
		int cursor = left ? MARGIN : graphics.guiWidth() - MARGIN;
		for (StatusHud.Chip chip : row.chips()) {
			cursor = drawChip(graphics, font, chip, cursor, y, boxW, left);
		}
	}

	/**
	 * One chip at {@code cursor} — its box's corner-side edge — and back where the next chip
	 * starts. A badge-bearing chip draws the outline box with its badge centered and its text on
	 * the inner side; a chip with an empty badge is a bare countdown text riding the row, no box.
	 */
	private int drawChip(GuiGraphics graphics, Font font, StatusHud.Chip chip, int cursor, int y, int boxW, boolean left) {
		int textY = y + (BOX_H - font.lineHeight) / 2;
		if (chip.badge().isEmpty()) {
			String text = chipText(chip);
			if (text.isEmpty()) {
				return cursor;
			}
			int x = left ? cursor + TEXT_PAD : cursor - TEXT_PAD - font.width(text);
			graphics.drawString(font, text, x, textY, textColor(chip));
			return left ? x + font.width(text) + CHIP_GAP : x - CHIP_GAP;
		}
		int boxX = left ? cursor : cursor - boxW;
		graphics.renderOutline(boxX, y, boxX + boxW, y + BOX_H, chip.lit() ? LIT_BORDER : DIM_BORDER);
		graphics.drawString(font, chip.badge(), boxX + (boxW - font.width(chip.badge())) / 2, textY, textColor(chip));
		String text = chipText(chip);
		if (!text.isEmpty()) {
			int x = left ? boxX + boxW + TEXT_PAD : boxX - TEXT_PAD - font.width(text);
			graphics.drawString(font, text, x, textY, labelColor(chip));
			return left ? x + font.width(text) + CHIP_GAP : x - CHIP_GAP;
		}
		return left ? boxX + boxW + CHIP_GAP : boxX - CHIP_GAP;
	}

	/** What prints beside the badge box: the label, its value, or both; bare riders print their value. */
	private static String chipText(StatusHud.Chip chip) {
		boolean hasLabel = !chip.label().isEmpty();
		boolean hasValue = !chip.value().isEmpty();
		if (!hasLabel) {
			return chip.value();
		}
		return hasValue ? chip.label() + " " + chip.value() : chip.label();
	}

	private static int textColor(StatusHud.Chip chip) {
		return chip.lit() ? LIT_TEXT : DIM_TEXT;
	}

	private static int labelColor(StatusHud.Chip chip) {
		return chip.lit() ? LIT_LABEL : DIM_TEXT;
	}

	/** The uniform box width, sized once for the widest badge; the height is the vanilla-slot 20. */
	private static int boxWidth(Font font) {
		int width = 0;
		for (String badge : BADGES) {
			width = Math.max(width, font.width(badge));
		}
		return width + BADGE_PAD * 2;
	}
}
