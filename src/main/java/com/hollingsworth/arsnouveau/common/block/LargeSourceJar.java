package com.hollingsworth.arsnouveau.common.block;

import com.hollingsworth.arsnouveau.common.block.tile.LargeSourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A 3x3x3 source jar placed as one unit; part_x/y/z locate each block inside its unit and the center block is the core.
 * Units placed directly above each other with aligned cores join into one column; joined marks a top or bottom layer touching such a unit.
 */
public class LargeSourceJar extends ModBlock implements EntityBlock {
    public static final IntegerProperty PART_X = IntegerProperty.create("part_x", 0, 2);
    public static final IntegerProperty PART_Y = IntegerProperty.create("part_y", 0, 2);
    public static final IntegerProperty PART_Z = IntegerProperty.create("part_z", 0, 2);
    public static final BooleanProperty JOINED = BooleanProperty.create("joined");

    public static final int BASE_PIXELS = 6;
    public static final int TOP_PIXELS = 9;
    private static final VoxelShape[] SHAPES = new VoxelShape[54];

    static {
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                for (int z = 0; z < 3; z++)
                    for (int j = 0; j < 2; j++)
                        SHAPES[shapeIndex(x, y, z, j == 1)] = partShape(x, y, z, j == 1);
    }

    private final BiFunction<BlockPos, BlockState, ? extends LargeSourceJarTile> tileFactory;

    public LargeSourceJar(Properties properties, BiFunction<BlockPos, BlockState, ? extends LargeSourceJarTile> tileFactory) {
        super(properties);
        this.tileFactory = tileFactory;
        registerDefaultState(stateDefinition.any().setValue(PART_X, 1).setValue(PART_Y, 1).setValue(PART_Z, 1).setValue(JOINED, false));
    }

    public LargeSourceJar() {
        this(defaultJarProperties(), LargeSourceJarTile::new);
    }

    public static Properties defaultJarProperties() {
        return Properties.of().sound(SoundType.GLASS).strength(2.0f, 6.0f).mapColor(MapColor.COLOR_PURPLE)
                .noOcclusion().pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false);
    }

    public static boolean isCore(BlockState state) {
        return state.getValue(PART_X) == 1 && state.getValue(PART_Y) == 1 && state.getValue(PART_Z) == 1;
    }

    public static BlockPos corePos(BlockState state, BlockPos pos) {
        return pos.offset(1 - state.getValue(PART_X), 1 - state.getValue(PART_Y), 1 - state.getValue(PART_Z));
    }

    private static int shapeIndex(int x, int y, int z, boolean joined) {
        return ((x * 3 + y) * 3 + z) * 2 + (joined ? 1 : 0);
    }

    private static VoxelShape partShape(int px, int py, int pz, boolean joined) {
        List<int[]> boxes = new ArrayList<>();
        if (py == 0 && !joined) {
            boxes.add(new int[]{6, 0, 6, 42, BASE_PIXELS, 42});
            boxes.add(new int[]{9, BASE_PIXELS, 9, 39, 16, 39});
            boxes.add(new int[]{12, BASE_PIXELS, 6, 36, 15, 42});
            boxes.add(new int[]{6, BASE_PIXELS, 12, 42, 15, 36});
        } else if (py == 2 && !joined) {
            boxes.add(new int[]{9, 0, 9, 39, 7, 39});
            boxes.add(new int[]{12, 7, 12, 36, 10, 36});
            boxes.add(new int[]{9, 10, 9, 39, 16, 39});
        } else {
            boxes.add(new int[]{9, 0, 9, 39, 16, 39});
            if (py == 0) {
                boxes.add(new int[]{8, 0, 11, 40, 3, 37});
                boxes.add(new int[]{11, 0, 8, 37, 3, 40});
            }
        }
        int pillarBottom = py == 0 && !joined ? BASE_PIXELS : 0;
        for (int px0 : new int[]{7, 37}) {
            for (int pz0 : new int[]{7, 37}) {
                boxes.add(new int[]{px0, pillarBottom, pz0, px0 + 4, 16, pz0 + 4});
            }
        }
        VoxelShape shape = Shapes.empty();
        int cx = px * 16, cz = pz * 16;
        for (int[] bx : boxes) {
            int x0 = Math.max(bx[0], cx) - cx, x1 = Math.min(bx[3], cx + 16) - cx;
            int z0 = Math.max(bx[2], cz) - cz, z1 = Math.min(bx[5], cz + 16) - cz;
            if (x0 < x1 && z0 < z1) {
                shape = Shapes.or(shape, Block.box(x0, bx[1], z0, x1, bx[4], z1));
            }
        }
        return shape.optimize();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[shapeIndex(state.getValue(PART_X), state.getValue(PART_Y), state.getValue(PART_Z), state.getValue(JOINED))];
    }

    public BlockState partState(int x, int y, int z) {
        return defaultBlockState().setValue(PART_X, x).setValue(PART_Y, y).setValue(PART_Z, z);
    }

    public BlockState withJoined(BlockGetter level, BlockPos pos, BlockState state) {
        int y = state.getValue(PART_Y);
        boolean joined = false;
        if (y == 2) {
            joined = alignedNeighbor(state, level.getBlockState(pos.above()), 0);
        } else if (y == 0) {
            joined = alignedNeighbor(state, level.getBlockState(pos.below()), 2);
        }
        return state.getValue(JOINED) == joined ? state : state.setValue(JOINED, joined);
    }

    private boolean alignedNeighbor(BlockState self, BlockState other, int expectedY) {
        return other.is(this) && other.getValue(PART_Y) == expectedY
                && other.getValue(PART_X).equals(self.getValue(PART_X)) && other.getValue(PART_Z).equals(self.getValue(PART_Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART_X, PART_Y, PART_Z, JOINED);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis() == Direction.Axis.Y ? withJoined(level, pos, state) : state;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !isCore(state)) {
            BlockPos core = corePos(state, pos);
            BlockState coreState = level.isLoaded(core) ? level.getBlockState(core) : null;
            if (coreState != null && coreState.is(this)) {
                if (!player.isCreative() && player.hasCorrectToolForDrops(coreState)) {
                    Block.dropResources(coreState, level, core, level.getBlockEntity(core), player, player.getMainHandItem());
                }
                level.setBlock(core, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this) && !level.isClientSide) {
            BlockPos core = corePos(state, pos);
            if (isCore(state)) {
                if (level.getBlockEntity(pos) instanceof LargeSourceJarTile tile) {
                    tile.dissolveColumn();
                }
                for (BlockPos other : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
                    if (!other.equals(pos) && level.isLoaded(other)) {
                        BlockState otherState = level.getBlockState(other);
                        if (otherState.is(this) && corePos(otherState, other).equals(core)) {
                            level.setBlock(other.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            } else if (level.isLoaded(core) && level.getBlockState(core).is(this)) {
                level.destroyBlock(core, true);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!isCore(state) || !(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LargeSourceJarTile tile)) {
            return Collections.emptyList();
        }
        long share = tile.currentShare();
        ItemStack stack = new ItemStack(this);
        if (share > 0) {
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.putLong("source", share);
            stack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
        }
        return Collections.singletonList(stack);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LargeSourceJarTile tile ? tile.getComparatorSignal() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tileFactory.apply(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !isCore(state)) {
            return null;
        }
        return (l, p, s, be) -> {
            if (be instanceof LargeSourceJarTile tile) {
                LargeSourceJarTile.serverTick(l, p, s, tile);
            }
        };
    }
}
