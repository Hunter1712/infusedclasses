package com.hunter1712.infusedclasses.mixin;

import com.hunter1712.infusedclasses.PlayerClasses;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One PlayerClasses slot per player, saved with player data so class survives relogs. */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerClasses.Owner {
	@Unique
	private final PlayerClasses infusedclasses$classes = new PlayerClasses();

	@Override
	public PlayerClasses classes() {
		return infusedclasses$classes;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void infusedclasses$read(ValueInput input, CallbackInfo ci) {
		infusedclasses$classes.readId(input.getString(PlayerClasses.NBT_KEY).orElse(null));
		infusedclasses$classes.readTree(input.getString(PlayerClasses.TREE_KEY).orElse(null));
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void infusedclasses$write(ValueOutput output, CallbackInfo ci) {
		String id = infusedclasses$classes.writeId();
		if (id != null) {
			output.putString(PlayerClasses.NBT_KEY, id);
		}
		String tree = infusedclasses$classes.writeTree();
		if (tree != null && !tree.isEmpty()) {
			output.putString(PlayerClasses.TREE_KEY, tree);
		}
	}
}

