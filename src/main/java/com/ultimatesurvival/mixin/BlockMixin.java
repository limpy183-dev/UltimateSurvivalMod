package com.ultimatesurvival.mixin;

import com.ultimatesurvival.item.ToolFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tools with auto-smelt / magnet take over the drop step of a block break. */
@Mixin(Block.class)
public abstract class BlockMixin {
	@Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At("HEAD"), cancellable = true)
	private static void usm$toolDrops(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity be, @Nullable Entity breaker,
			ItemStack tool, CallbackInfo ci) {
		if (level instanceof ServerLevel sl && breaker instanceof ServerPlayer sp && ToolFx.handles(tool)) {
			List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, sl, pos, be, breaker, tool));
			ToolFx.deliver(sp, tool, drops, pos);
			state.spawnAfterBreak(sl, pos, tool, true);
			ci.cancel();
		}
	}
}
