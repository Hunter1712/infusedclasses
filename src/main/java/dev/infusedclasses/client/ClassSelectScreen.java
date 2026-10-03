package dev.infusedclasses.client;

import dev.infusedclasses.ClassDefinition;
import dev.infusedclasses.ClassRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One button per class. Locked ones show but stay disabled. No custom art. */
public class ClassSelectScreen extends Screen {
	protected ClassSelectScreen() {
		super(Component.literal("Choose your class"));
	}

	@Override
	protected void init() {
		java.util.Collection<ClassDefinition> defs = ClassRegistry.all();
		int y = listTop(defs.size());
		for (ClassDefinition def : defs) {
			String label = def.displayName() + (def.locked() ? " (soon)" : "");
			addRenderableWidget(Button.builder(Component.literal(label), b -> InfusedClassesClient.pickClass(def.id()))
					.bounds(width / 2 - 100, y, 200, 20)
					.build())
					.active = !def.locked();
			y += 24;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(font, title, width / 2, listTop(ClassRegistry.all().size()) - 20, 0xFFFFFF);
	}

	private int listTop(int count) {
		return height / 2 - count * 14;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
