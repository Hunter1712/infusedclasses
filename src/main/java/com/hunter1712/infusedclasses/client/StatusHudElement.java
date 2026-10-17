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

import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The strip on screen: the adapter that draws {@link StatusHud#rows} verbatim. The decision
 * returns the whole strip as rows; this lays it out and hides it. The strip is bare text in
 * a screen zone — the language the Hud Texts screenshots made visible: one line per row,
 * flush a breath from the screen's chosen edge, the stack vertically centered on the screen.
 * The signature row leads, top-down. No dot, no box, no icon, no texture, no fill — not one
 * rectangle exists between the rows decision and the screen, and words never enter: names
 * live in the fire lines and {@code code /class info}. The left zone (the default) left-aligns
 * every line on a shared flush edge; the right zone mirrors it, right-aligning every line on
 * the same flush edge — a line's facts read in either case: a ready lane draws its bare badge
 * dim, a cooling or live chip its bright badge and number, a rider its bare number beside its
 * lane.
 *
 * <p>Growing is always into the open screen — a left-zone line reaches rightward, a
 * right-zone line leftward — so no roster can clip and nothing is ever dropped to fit. The
 * config's {@code hud.scale} sizes the stack around the same flush edges; every coordinate is
 * scaled gui units beside vanilla geometry, so every gui scale and every {@code hud.scale}
 * lands the same frame.
 *
 * <p>Drawing stops when the config disables it, any screen is open, the HUD is hidden (F1),
 * no snapshot exists, or the snapshot's class id is empty (classless); each of those lives
 * here, in the adapter, because hiding is never the decision's. Countdowns interpolate from
 * the snapshot's receipt time ({@link StatusStore#elapsed}), and lit is the live flag the
 * decision already carried: ready lanes draw dim, live chips draw lit.
 */
public final class StatusHudElement implements HudElement {
	/** This element's own id in the fabric HUD registry. */
	static final Identifier ID = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "status");

	/** The zone pad between the strip and its screen edge: the breath Hud Texts uses. */
	private static final int EDGE_PAD = 2;
	/** The gap separating a row's chips: wide enough to read as one lane's facts. */
	private static final int CHIP_GAP = 4;
	private static final int LINE_H = 9;
	/** The gap between a zone stack's lines. */
	private static final int LINE_GAP = 2;

	/** Lit chip is what counts now; dim recedes. Shadowed text over anything, no backdrop needed. */
	private static final int LIT_TEXT = 0xFFFFFFFF;
	private static final int DIM_TEXT = 0xFFA6A6A6;

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
				config.corner(), config.scale());
	}

	/** One zone line's readable content: a token and whether it draws bright or dim. */
	private record Token(String text, boolean lit) {
		/** The token's width; trailing chip gaps ride the caller, so the record stays pure. */
		int width(Font font) {
			return font.width(text);
		}

		/** The token on its line: shadowed text at its entry edge, and where the next token starts. */
		int draw(GuiGraphics graphics, Font font, int x, int y) {
			graphics.drawString(font, text, x, y, lit ? LIT_TEXT : DIM_TEXT);
			return x + width(font);
		}
	}

	/** The whole strip: one line per row, stacked in the chosen zone's flush edge, screen-centered. */
	private void draw(GuiGraphics graphics, Font font, List<StatusHud.Row> rows, HudConfig.Corner corner, float scale) {
		boolean right = corner == HudConfig.Corner.RIGHT;
		int screen = graphics.guiHeight();
		int lineStep = LINE_H + LINE_GAP;
		int blockHeight = rows.size() * LINE_H + (rows.size() - 1) * LINE_GAP;
		int topY = Math.max(0, (screen - Math.round(blockHeight * scale)) / 2);
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(right ? graphics.guiWidth() - EDGE_PAD : EDGE_PAD, topY);
		pose.scale(scale, scale);
		for (int i = 0; i < rows.size(); i++) {
			int y = i * lineStep;
			List<Token> tokens = tokens(rows.get(i));
			int x = 0;
			if (right) {
				// the right zone shares one flush edge on the other side: every line right-aligns to it
				for (int j = 0; j < tokens.size(); j++) {
					x -= tokens.get(j).width(font) + (j < tokens.size() - 1 ? CHIP_GAP : 0);
				}
			}
			for (int j = 0; j < tokens.size(); j++) {
				x = tokens.get(j).draw(graphics, font, x, y) + (j < tokens.size() - 1 ? CHIP_GAP : 0);
			}
		}
		pose.popMatrix();
	}

	/** A row's tokens in reading order: every chip's facts as one bare token. */
	private static List<Token> tokens(StatusHud.Row row) {
		List<Token> tokens = new ArrayList<>();
		for (StatusHud.Chip chip : row.chips()) {
			String text = chipToken(chip);
			if (!text.isEmpty()) {
				tokens.add(new Token(text, chip.lit()));
			}
		}
		return tokens;
	}

	/** What one chip prints: its badge, its value, or badge plus value — a chip's token in the strip's language. */
	private static String chipToken(StatusHud.Chip chip) {
		boolean hasBadge = !chip.badge().isEmpty();
		if (!hasBadge) {
			return chip.value();
		}
		return chip.value().isEmpty() ? chip.badge() : chip.badge() + " " + chip.value();
	}
}
