package com.hollingsworth.arsnouveau.common.compat.ae2;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarSpec;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartModels;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import appeng.items.materials.MaterialItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import appeng.items.storage.StorageTier;
import appeng.parts.automation.StackWorldBehaviors;

/**
 * 应用能源 2 的可选联动，只在装了 ae2 时加载：ME 魔源罐、魔源转换器、魔源元件与便携元件、魔源与法术 P2P、魔源键类型与总线策略。
 * 原 Ars Énergistique（arseng，LGPLv3）并入于此，旧命名空间的注册名通过别名与缺失映射转到 ars_nouveau。
 */
public final class ArsAE2 {
    public static final ResourceKey<Registry<AEKeyType>> KEY_TYPES = ResourceKey.createRegistryKey(new ResourceLocation("ae2", "keytypes"));
    @Deprecated(since = "4.12.12", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.6.1")
    private static final String LEGACY = "arseng";
    private static final List<StorageTier> TIERS = List.of(StorageTier.SIZE_1K, StorageTier.SIZE_4K, StorageTier.SIZE_16K, StorageTier.SIZE_64K, StorageTier.SIZE_256K);

    public static MESourceJarBlock ME_SOURCE_JAR;
    public static SourceAcceptorBlock SOURCE_ACCEPTOR;
    public static BlockEntityType<MESourceJarBlockEntity> ME_SOURCE_JAR_ENTITY;
    public static BlockEntityType<SourceAcceptorBlockEntity> SOURCE_ACCEPTOR_ENTITY;
    public static Item SOURCE_CELL_HOUSING;
    public static Item CREATIVE_SOURCE_CELL;
    public static PartItem<SourceP2PTunnelPart> SOURCE_P2P_TUNNEL;
    public static PartItem<SpellP2PTunnelPart> SPELL_P2P_TUNNEL;
    public static PartItem<SourceAcceptorPart> SOURCE_ACCEPTOR_PART;
    private static final List<SourceCellItem> CELLS = new ArrayList<>();
    private static final List<PortableSourceCellItem> PORTABLES = new ArrayList<>();
    private static final List<Item> TAB_ITEMS = new ArrayList<>();
    @Deprecated(since = "4.12.12", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.6.1")
    private static final Map<String, Item> LEGACY_ITEMS = new HashMap<>();

    private ArsAE2() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ArsNouveau.MODID, path);
    }

    public static List<ItemStack> selfDrop(Block block, LootParams.Builder builder) {
        Float radius = builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && builder.getLevel().getRandom().nextFloat() > 1.0F / radius) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new ItemStack(block));
    }

    public static List<SourceCellItem> getCells() {
        return Collections.unmodifiableList(CELLS);
    }

    public static List<PortableSourceCellItem> getPortables() {
        return Collections.unmodifiableList(PORTABLES);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ArsAE2::onRegister);
        modBus.addListener(ArsAE2::setup);
        modBus.addListener(ArsAE2::fillCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(ArsAE2::remapLegacy);
        MinecraftForge.EVENT_BUS.addListener(SpellP2PTunnelPart::onSpellHit);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ArsAE2::attachCapabilities);
        ArsBlockEntityIndexes.registerSourceCandidateClass(type -> IPartHost.class.isAssignableFrom(type) || SourceAcceptorBlockEntity.class.isAssignableFrom(type));
        ArsBlockEntityIndexes.registerSourceProbe(ArsAE2::hasGenericInventory);
        ScribesTile.registerItemSource(ScribesNetworkSource::takeFromNetwork);
    }

    @Deprecated(since = "4.12.12", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.6.1")
    private static void alias(RegisterEvent event, String path) {
        if (event.getForgeRegistry() instanceof ForgeRegistry<?> registry) {
            registry.addAlias(new ResourceLocation(LEGACY, path), id(path));
        }
    }

    private static <T> void register(RegisterEvent event, ResourceKey<Registry<T>> registry, String path, T value) {
        event.register(registry, id(path), () -> value);
        alias(event, path);
    }

    private static <T extends Item> T registerItem(RegisterEvent event, String path, T item, boolean inTab) {
        register(event, Registries.ITEM, path, item);
        LEGACY_ITEMS.put(path, item);
        if (inTab) TAB_ITEMS.add(item);
        return item;
    }

    private static void onRegister(RegisterEvent event) {
        var key = event.getRegistryKey();
        if (key.equals(Registries.BLOCK)) {
            ME_SOURCE_JAR = new MESourceJarBlock();
            register(event, Registries.BLOCK, "me_source_jar", ME_SOURCE_JAR);
            SOURCE_ACCEPTOR = new SourceAcceptorBlock();
            register(event, Registries.BLOCK, "source_acceptor", SOURCE_ACCEPTOR);
        } else if (key.equals(Registries.ITEM)) {
            SOURCE_CELL_HOUSING = registerItem(event, "source_cell_housing", new MaterialItem(new Item.Properties()), true);
            for (StorageTier tier : TIERS) {
                CELLS.add(registerItem(event, "source_storage_cell_" + tier.namePrefix(),
                        new SourceCellItem(new Item.Properties().stacksTo(1), tier, SOURCE_CELL_HOUSING), true));
            }
            for (StorageTier tier : TIERS) {
                PORTABLES.add(registerItem(event, "portable_source_cell_" + tier.namePrefix(),
                        new PortableSourceCellItem(new Item.Properties().stacksTo(1), tier), true));
            }
            CREATIVE_SOURCE_CELL = registerItem(event, "creative_source_cell", new CreativeSourceCellItem(new Item.Properties()), true);
            PartModels.registerModels(PartModelsHelper.createModels(SourceP2PTunnelPart.class));
            SOURCE_P2P_TUNNEL = registerItem(event, "source_p2p_tunnel", new PartItem<>(new Item.Properties(), SourceP2PTunnelPart.class, SourceP2PTunnelPart::new), true);
            PartModels.registerModels(PartModelsHelper.createModels(SpellP2PTunnelPart.class));
            SPELL_P2P_TUNNEL = registerItem(event, "spell_p2p_tunnel", new PartItem<>(new Item.Properties(), SpellP2PTunnelPart.class, SpellP2PTunnelPart::new), true);
            PartModels.registerModels(PartModelsHelper.createModels(SourceAcceptorPart.class));
            SOURCE_ACCEPTOR_PART = registerItem(event, "cable_source_acceptor", new PartItem<>(new Item.Properties(), SourceAcceptorPart.class, SourceAcceptorPart::new), true);
            registerItem(event, "me_source_jar", new BlockItem(ME_SOURCE_JAR, new Item.Properties()), true);
            registerItem(event, "source_acceptor", new BlockItem(SOURCE_ACCEPTOR, new Item.Properties()), true);
        } else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
            ME_SOURCE_JAR_ENTITY = BlockEntityType.Builder.of(MESourceJarBlockEntity::new, ME_SOURCE_JAR).build(null);
            register(event, Registries.BLOCK_ENTITY_TYPE, "me_source_jar", ME_SOURCE_JAR_ENTITY);
            SOURCE_ACCEPTOR_ENTITY = BlockEntityType.Builder.of(SourceAcceptorBlockEntity::new, SOURCE_ACCEPTOR).build(null);
            register(event, Registries.BLOCK_ENTITY_TYPE, "source_acceptor", SOURCE_ACCEPTOR_ENTITY);
            SOURCE_ACCEPTOR.setBlockEntity(SourceAcceptorBlockEntity.class, SOURCE_ACCEPTOR_ENTITY, null, null);
        } else if (key.equals(KEY_TYPES)) {
            event.register(KEY_TYPES, SourceKeyType.ID, () -> SourceKeyType.TYPE);
            if (event.getForgeRegistry() instanceof ForgeRegistry<?> registry) {
                registry.addAlias(SourceKeyType.LEGACY_ID, SourceKeyType.ID);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            StorageCells.addCellHandler(SourceCellHandler.INSTANCE);
            StorageCells.addCellHandler(CreativeSourceCellHandler.INSTANCE);
            for (SourceCellItem cell : CELLS) {
                Upgrades.add(AEItems.VOID_CARD, cell, 1, GuiText.StorageCells.getTranslationKey());
            }
            for (PortableSourceCellItem cell : PORTABLES) {
                Upgrades.add(AEItems.ENERGY_CARD, cell, 2, GuiText.PortableCells.getTranslationKey());
                Upgrades.add(AEItems.VOID_CARD, cell, 1, GuiText.PortableCells.getTranslationKey());
            }
            StackWorldBehaviors.registerImportStrategy(SourceKeyType.TYPE, SourceStorageImportStrategy::new);
            StackWorldBehaviors.registerExportStrategy(SourceKeyType.TYPE, SourceStorageExportStrategy::new);
            StackWorldBehaviors.registerExternalStorageStrategy(SourceKeyType.TYPE, SourceExternalStorageStrategy::new);
            ContainerItemStrategy.register(SourceKeyType.TYPE, SourceKey.class, SourceContainerItemStrategy.INSTANCE);
            GenericSlotCapacities.register(SourceKeyType.TYPE, SourceJarSpec.JAR.capacity());
            P2PTunnelAttunement.registerAttunementTag(SOURCE_P2P_TUNNEL);
            P2PTunnelAttunement.registerAttunementTag(SPELL_P2P_TUNNEL);
        });
    }

    private static void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeTabRegistry.BLOCKS.getKey().equals(event.getTabKey())) return;
        for (Item item : TAB_ITEMS) {
            if (item instanceof AEBaseBlockItem blockItem && blockItem.getBlock() instanceof AEBaseBlock block) {
                block.addToMainCreativeTab(event);
            } else if (item instanceof AEBaseItem baseItem) {
                baseItem.addToMainCreativeTab(event);
            } else {
                event.accept(item);
            }
        }
    }

    @Deprecated(since = "4.12.12", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2027.6.1")
    private static void remapLegacy(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(Registries.ITEM, LEGACY)) {
            var item = LEGACY_ITEMS.get(mapping.getKey().getPath());
            if (item != null) mapping.remap(item);
        }
        for (var mapping : event.getMappings(Registries.BLOCK, LEGACY)) {
            switch (mapping.getKey().getPath()) {
                case "me_source_jar" -> mapping.remap(ME_SOURCE_JAR);
                case "source_acceptor" -> mapping.remap(SOURCE_ACCEPTOR);
                default -> {}
            }
        }
        for (var mapping : event.getMappings(Registries.BLOCK_ENTITY_TYPE, LEGACY)) {
            switch (mapping.getKey().getPath()) {
                case "me_source_jar" -> mapping.remap(ME_SOURCE_JAR_ENTITY);
                case "source_acceptor" -> mapping.remap(SOURCE_ACCEPTOR_ENTITY);
                default -> {}
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean hasGenericInventory(BlockEntity be) {
        if (be.getCapability(Capabilities.GENERIC_INTERNAL_INV, null).isPresent()) return true;
        for (Direction side : Direction.values()) {
            if (be.getCapability(Capabilities.GENERIC_INTERNAL_INV, side).isPresent()) return true;
        }
        return false;
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        var be = event.getObject();
        event.addCapability(id("generic_inv_wrapper"), new ICapabilityProvider() {
            @SuppressWarnings("UnstableApiUsage")
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CapabilityRegistry.SOURCE_TILE) {
                    return be.getCapability(Capabilities.GENERIC_INTERNAL_INV, side).lazyMap(GenericStackSourceStorage::new).cast();
                }
                return LazyOptional.empty();
            }
        });
    }
}
