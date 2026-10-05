package com.hunter1712.infusedclasses.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Inventory list geometry, so HUD rows can continue vanilla's effect list exactly. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
	@Accessor("leftPos")
	int infusedclasses$leftPos();

	@Accessor("topPos")
	int infusedclasses$topPos();

	@Accessor("imageWidth")
	int infusedclasses$imageWidth();
}
