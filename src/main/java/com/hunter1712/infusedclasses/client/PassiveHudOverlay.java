package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ConditionalPassive;
import com.hunter1712.infusedclasses.PassiveHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

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
	// Same sprite vanilla draws for HUD slots.
	private static final Identifier SLOT_BACKGROUND =
			Identifier.withDefaultNamespace("hud/effect_background");

	private static final int SLOT = 25;

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
		// Backstop: no effect means no row even with ticks banked; the server zeroes the signal edge-triggered on effect loss.
		// Beacon Strength alone means no row too: only fire and proc signals feed the ticks.
		int warCryTicks = mc.player.hasEffect(MobEffects.STRENGTH) ? ClientWarCry.ticksLeft(now) : 0;
		PassiveHud.Row warCry = PassiveHud.warCryRow(warCryTicks);
		if (visible.isEmpty() && warCry.hidden()) {
			return;
		}
		if (mc.screen instanceof AbstractContainerScreen<?> containers) {
			HudInventoryList.render(graphics, mc, containers, visible, warCry, now);
		} else if (mc.screen == null) {
			renderStrip(graphics, mc, visible, warCry);
		}
	}

	/** HUD: icon slots continuing the beneficial strip leftward. No text, like vanilla. */
	private static void renderStrip(GuiGraphics graphics, Minecraft mc, List<ConditionalPassive> visible, PassiveHud.Row warCry) {
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
			drawSlot(graphics, x, row.iconId());
			slot++;
		}
		if (!warCry.hidden()) {
			int x = graphics.guiWidth() - SLOT * (beneficial + slot + 1);
			drawSlot(graphics, x, warCry.iconId());
		}
	}

	/** One HUD slot: background frame plus the decided icon, like vanilla. */
	private static void drawSlot(GuiGraphics graphics, int x, String iconId) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND, x, 1, 24, 24);
		Identifier sprite = spriteFor(iconId);
		if (sprite != null) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 3, 4, 18, 18);
		}
	}

	/** The icons the decisions can name; unknown ids leave the frame empty. */
	static Identifier spriteFor(String iconId) {
		try {
			if (PassiveHud.ICON.equals(iconId)) {
				return Gui.getMobEffectSprite(MobEffects.RESISTANCE);
			}
			if (PassiveHud.WAR_CRY_ICON.equals(iconId)) {
				return Gui.getMobEffectSprite(MobEffects.STRENGTH);
			}
		} catch (RuntimeException missing) {
			return null;
		}
		return null;
	}

	/** Inventory: vanilla-metric boxes live in the list layout; the strip keeps this file small. */
}
