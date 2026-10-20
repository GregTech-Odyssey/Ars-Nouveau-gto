package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.common.block.LargeSourceJar;
import com.hollingsworth.arsnouveau.common.block.tile.LargeSourceJarTile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.LongSupplier;

public class LargeSourceJarItem extends BlockItem {
    private final LongSupplier unitCapacity;

    public LargeSourceJarItem(LargeSourceJar block, Properties properties, LongSupplier unitCapacity) {
        super(block, properties);
        this.unitCapacity = unitCapacity;
    }

    public static long storedSource(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getCompound("BlockEntityTag").getLong("source");
    }

    private static BlockPos unitOrigin(BlockPlaceContext ctx) {
        BlockPos clicked = ctx.getClickedPos();
        if (ctx.getClickedFace() == Direction.UP) {
            BlockPos below = clicked.below();
            BlockState belowState = ctx.getLevel().getBlockState(below);
            if (belowState.getBlock() instanceof LargeSourceJar && belowState.getValue(LargeSourceJar.PART_Y) == 2) {
                BlockPos core = LargeSourceJar.corePos(belowState, below);
                return new BlockPos(core.getX(), clicked.getY(), core.getZ());
            }
        }
        return clicked;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (!ctx.canPlace()) {
            return InteractionResult.FAIL;
        }
        Level level = ctx.getLevel();
        LargeSourceJar block = (LargeSourceJar) getBlock();
        BlockPos origin = unitOrigin(ctx);
        Player player = ctx.getPlayer();
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = block.partState(dx + 1, dy, dz + 1);
                    if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(ctx, pos, Direction.UP))
                            || !level.isUnobstructed(state, pos, collision)) {
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        if (!level.isClientSide) {
            BlockPos core = origin.above();
            ItemStack stack = ctx.getItemInHand();
            for (int dy = 0; dy < 3; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        level.setBlock(pos, block.withJoined(level, pos, block.partState(dx + 1, dy, dz + 1)), Block.UPDATE_ALL);
                    }
                }
            }
            if (level.getBlockEntity(core) instanceof LargeSourceJarTile tile) {
                tile.onUnitPlaced(storedSource(stack));
            }
            SoundType sound = block.defaultBlockState().getSoundType();
            level.playSound(null, core, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, core, GameEvent.Context.of(player, block.defaultBlockState()));
        }
        if (player == null || !player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("ars_nouveau.large_source_jar.capacity", unitCapacity.getAsLong()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ars_nouveau.large_source_jar.stacking").withStyle(ChatFormatting.GRAY));
        long stored = storedSource(stack);
        if (stored > 0) {
            tooltip.add(Component.translatable("ars_nouveau.large_source_jar.stored", stored).withStyle(ChatFormatting.GRAY));
        }
    }
}
