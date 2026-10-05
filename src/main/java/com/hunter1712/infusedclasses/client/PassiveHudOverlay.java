package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ConditionalPassive;
import com.hunter1712.infusedclasses.PassiveHud;
import com.hunter1712.infusedclasses.mixin.client.ContainerScreenAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.Locale;

/**
 * Thin client overlay: draws verbatim whatever the row decision returns, in vanilla's
 * own effect look. No real effect is registered, so milk and clear never touch this;
 * it is pixels only. In the HUD each passive joins the vanilla beneficial strip (same
 * 24px slots, same 25px step, same background sprite). Over inventory-style screens it
 * appends vanilla-metric effect boxes (background sprite, 18px icon, name plus
 * `mm:ss`) below vanilla's list, continuing its 33px step. Like vanilla, it draws
 * nothing when the decision says hidden, when hideGui (F1) is set, when a screen that
 * hides effects is open, or when the list has no room. Classless players are never
 * sent entries, so "no class" and "no stacks" share the empty check.
 */
public final class PassiveHudOverlay {
	// Same sprites vanilla draws: HUD slots plus inventory list boxes.
	private static final Identifier SLOT_BACKGROUND =
			Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier BOX_BACKGROUND =
			Identifier.withDefaultNamespace("container/inventory/effect_background");

	private static final int SLOT = 25;
	private static final int ROW_STEP = 33;
	private static final int BOX_H = 32;
	private static final int MARGIN = 4;

	private PassiveHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}
		if (mc.screen != null && !mc.screen.showsActiveEffects()) {
			return;
		}
		long now = System.currentTimeMillis();
		List<ConditionalPassive> visible = PassiveHud.visibleInRegistryOrder(p -> ClientPassiveStacks.stacksFor(p, now));
		if (visible.isEmpty()) {
			return;
		}
		if (mc.screen instanceof AbstractContainerScreen<?> containers) {
			renderList(graphics, mc, containers, visible, now);
		} else if (mc.screen == null) {
			renderStrip(graphics, mc, visible);
		}
	}

	/** HUD: icon slots continuing the beneficial strip leftward. No text, like vanilla. */
	private static void renderStrip(GuiGraphics graphics, Minecraft mc, List<ConditionalPassive> visible) {
		int beneficial = 0;
		for (MobEffectInstance effect : mc.player.getActiveEffects()) {
			if (effect.showIcon() && effect.getEffect().value().isBeneficial()) {
				beneficial++;
			}
		}
		long now = System.currentTimeMillis();
		int slot = 0;
		for (ConditionalPassive passive : visible) {
			PassiveHud.Row row = PassiveHud.rowFor(passive,
					ClientPassiveStacks.stacksFor(passive, now),
					ClientPassiveStacks.millisUntilDecay(passive.name(), now));
			if (row.hidden()) {
				continue;
			}
			int x = graphics.guiWidth() - SLOT * (beneficial + slot + 1);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND, x, 1, 24, 24);
			Identifier sprite = spriteFor(row.iconId());
			if (sprite != null) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 3, 4, 18, 18);
			}
			slot++;
		}
	}

	/** The one icon the decision can name today; unknown ids leave the frame empty. */
	private static Identifier spriteFor(String iconId) {
		if (!PassiveHud.ICON.equals(iconId)) {
			return null;
		}
		try {
			return Gui.getMobEffectSprite(MobEffects.RESISTANCE);
		} catch (RuntimeException missing) {
			return null;
		}
	}

	/** Inventory: vanilla-metric boxes appended below vanilla's own list rows. */
	private static void renderList(GuiGraphics graphics, Minecraft mc, AbstractContainerScreen<?> screen,
			List<ConditionalPassive> visible, long now) {
		ContainerScreenAccess geometry = (ContainerScreenAccess) screen;
		int x = geometry.infusedclasses$leftPos() + geometry.infusedclasses$imageWidth() + 2;
		int room = graphics.guiWidth() - x;
		if (room < 32) {
			return;
		}
		int maxW = room >= 120 ? room - 7 : 32;
		int vanillaRows = mc.player.getActiveEffects().size();
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
			int y = geometry.infusedclasses$topPos() + step * (vanillaRows + row);
			drawBox(graphics, font, x, y, maxW, decided);
			row++;
		}
	}

	/** One vanilla inventory row: background box sized off the text, icon, name, `mm:ss`. */
	private static void drawBox(GuiGraphics graphics, Font font, int x, int y, int maxW, PassiveHud.Row row) {
		String title = row.title();
		String duration = duration(row.secondsLeft());
		int w = Math.min(maxW, Math.max(32 + font.width(title) + 7, 32 + font.width(duration) + 7));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_BACKGROUND, x, y, w, BOX_H);
		Identifier sprite = spriteFor(row.iconId());
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
