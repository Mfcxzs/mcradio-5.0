package com.mcradio;

import com.mcradio.McRadio.StationBlockEntity.Mode;
import com.mcradio.core.BroadcastEngine;
import com.mcradio.core.ChainTier;
import com.mcradio.core.Mixer;
import com.mcradio.core.SecureChannels;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Void Frequency v4: the eight-block broadcast chain.
 * Server -> Computer -> Amplifier -> Distribution Station (joined by invisible cables, shown only while a
 * Cable Wrench is held), Station - Translator - Antenna (face-adjacent), then Repeaters and Receivers/Megaphones.
 * The on-air logic itself lives in {@link BroadcastEngine}; this class wires it into Minecraft.
 */
public final class VoidChain {
    public static final int CABLE_RANGE = 24;
    public static final int AMP_COST_PER_TICK = 8;
    public static final int AMP_MIN_ROTATION = 16;

    private VoidChain() {
    }

    // ================================================================== registry

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, McRadio.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, McRadio.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, McRadio.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, McRadio.MOD_ID);

    static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(state -> state.getValue(McRadio.RadioDeviceBlock.ACTIVE) ? 6 : 0);
    }

    public static final RegistryObject<Block> SERVER_BLOCK = BLOCKS.register("music_server", () -> new MusicServerBlock(props()));
    public static final RegistryObject<Block> COMPUTER_BLOCK = BLOCKS.register("broadcast_computer", () -> new BroadcastComputerBlock(props()));
    public static final RegistryObject<Block> AMP_BLOCK = BLOCKS.register("audio_amplifier", () -> new AmplifierBlock(props()));
    public static final RegistryObject<Block> TRANSLATOR_BLOCK = BLOCKS.register("signal_translator", () -> new TranslatorBlock(props()));
    public static final RegistryObject<Block> ANTENNA1_BLOCK = BLOCKS.register("antenna_tier1", () -> new AntennaBlock(props(), 1));
    public static final RegistryObject<Block> ANTENNA2_BLOCK = BLOCKS.register("antenna_tier2", () -> new AntennaBlock(props(), 2));
    public static final RegistryObject<Block> ANTENNA3_BLOCK = BLOCKS.register("antenna_tier3", () -> new AntennaBlock(props(), 3));

    public static final RegistryObject<Item> SERVER_ITEM = ITEMS.register("music_server", () -> new BlockItem(SERVER_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register("broadcast_computer", () -> new BlockItem(COMPUTER_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> AMP_ITEM = ITEMS.register("audio_amplifier", () -> new BlockItem(AMP_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> TRANSLATOR_ITEM = ITEMS.register("signal_translator", () -> new BlockItem(TRANSLATOR_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANTENNA1_ITEM = ITEMS.register("antenna_tier1", () -> new BlockItem(ANTENNA1_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANTENNA2_ITEM = ITEMS.register("antenna_tier2", () -> new BlockItem(ANTENNA2_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANTENNA3_ITEM = ITEMS.register("antenna_tier3", () -> new BlockItem(ANTENNA3_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> WRENCH_ITEM = ITEMS.register("cable_wrench", () -> new CableWrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<MusicServerBE>> SERVER_BE = BLOCK_ENTITIES.register("music_server",
            () -> BlockEntityType.Builder.of(MusicServerBE::new, SERVER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChainComputerBE>> COMPUTER_BE = BLOCK_ENTITIES.register("broadcast_computer",
            () -> BlockEntityType.Builder.of(ChainComputerBE::new, COMPUTER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AmplifierBE>> AMP_BE = BLOCK_ENTITIES.register("audio_amplifier",
            () -> BlockEntityType.Builder.of(AmplifierBE::new, AMP_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<TranslatorBE>> TRANSLATOR_BE = BLOCK_ENTITIES.register("signal_translator",
            () -> BlockEntityType.Builder.of(TranslatorBE::new, TRANSLATOR_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AntennaBE>> ANTENNA_BE = BLOCK_ENTITIES.register("antenna",
            () -> BlockEntityType.Builder.of(AntennaBE::new, ANTENNA1_BLOCK.get(), ANTENNA2_BLOCK.get(), ANTENNA3_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<ChainMenu>> COMPUTER_MENU = MENUS.register("broadcast_computer",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ChainMenu(VoidChain.COMPUTER_MENU.get(), id, buf.readBlockPos())));
    public static final RegistryObject<MenuType<ChainMenu>> TRANSLATOR_MENU = MENUS.register("signal_translator",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ChainMenu(VoidChain.TRANSLATOR_MENU.get(), id, buf.readBlockPos())));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        init();
    }

    /** Forces class initialisation (and therefore channel registration). */
    public static void init() {
        // touching NET runs the static initialiser
        NET.toString();
    }

    /** Items shown in the creative tab, in chain order. */
    public static void addToTab(CreativeModeTab.Output output) {
        output.accept(SERVER_ITEM.get());
        output.accept(COMPUTER_ITEM.get());
        output.accept(AMP_ITEM.get());
        output.accept(TRANSLATOR_ITEM.get());
        output.accept(ANTENNA1_ITEM.get());
        output.accept(ANTENNA2_ITEM.get());
        output.accept(ANTENNA3_ITEM.get());
        output.accept(WRENCH_ITEM.get());
    }

    // ================================================================== network

    public static final SimpleChannel NET = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "chain"))
            .networkProtocolVersion(1)
            .simpleChannel()
            .play()
            .clientbound()
                .add(ComputerStatePacket.class, ComputerStatePacket.CODEC, ComputerStatePacket::handle)
                .add(LibraryPacket.class, LibraryPacket.CODEC, LibraryPacket::handle)
                .add(MixPacket.class, MixPacket.CODEC, MixPacket::handle)
                .add(CableListPacket.class, CableListPacket.CODEC, CableListPacket::handle)
            .serverbound()
                .add(ComputerActionPacket.class, ComputerActionPacket.CODEC, ComputerActionPacket::handle)
                .add(TranslatorSetPacket.class, TranslatorSetPacket.CODEC, TranslatorSetPacket::handle)
                .add(CableRequestPacket.class, CableRequestPacket.CODEC, CableRequestPacket::handle)
            .build();

    public static void sendTo(ServerPlayer player, Object packet) {
        NET.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object packet) {
        NET.send(packet, PacketDistributor.SERVER.noArg());
    }

    // ================================================================== cable links (invisible)

    /** Level-wide set of cable links. Stored server side; the client only ever sees links near a held wrench. */
    public static final class CableLinks extends SavedData {
        private static final String NAME = "mcradio_chain_links";
        private final Map<Long, Set<Long>> adjacency = new HashMap<>();

        public CableLinks() {
        }

        public static CableLinks load(CompoundTag tag, HolderLookup.Provider registries) {
            CableLinks links = new CableLinks();
            ListTag list = tag.getList("Links", Tag.TAG_LONG);
            for (int i = 0; i + 1 < list.size(); i += 2) {
                if (list.get(i) instanceof LongTag a && list.get(i + 1) instanceof LongTag b) {
                    links.add(a.getAsLong(), b.getAsLong());
                }
            }
            return links;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Set<Long>> entry : adjacency.entrySet()) {
                for (long other : entry.getValue()) {
                    if (entry.getKey() < other) {
                        list.add(LongTag.valueOf(entry.getKey()));
                        list.add(LongTag.valueOf(other));
                    }
                }
            }
            tag.put("Links", list);
            return tag;
        }

        public static CableLinks get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CableLinks::new, CableLinks::load, null), NAME);
        }

        private void add(long a, long b) {
            adjacency.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            adjacency.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }

        /** Links the two blocks, or removes the link if it exists. Returns true if a link now exists. */
        public boolean toggle(BlockPos a, BlockPos b) {
            long la = a.asLong();
            long lb = b.asLong();
            Set<Long> set = adjacency.get(la);
            boolean existed = set != null && set.contains(lb);
            if (existed) {
                adjacency.get(la).remove(lb);
                Set<Long> back = adjacency.get(lb);
                if (back != null) {
                    back.remove(la);
                }
            } else {
                add(la, lb);
            }
            setDirty();
            return !existed;
        }

        public void removeAll(BlockPos pos) {
            long key = pos.asLong();
            Set<Long> others = adjacency.remove(key);
            if (others != null) {
                for (long other : others) {
                    Set<Long> back = adjacency.get(other);
                    if (back != null) {
                        back.remove(key);
                    }
                }
                setDirty();
            }
        }

        public List<BlockPos> neighbours(BlockPos pos) {
            Set<Long> set = adjacency.get(pos.asLong());
            if (set == null || set.isEmpty()) {
                return List.of();
            }
            List<BlockPos> result = new ArrayList<>(set.size());
            for (long value : set) {
                result.add(BlockPos.of(value));
            }
            return result;
        }

        /** All links with at least one end within {@code range} of {@code center}, as packed pairs. */
        public List<long[]> near(BlockPos center, int range, int max) {
            double rangeSqr = (double) range * range;
            List<long[]> found = new ArrayList<>();
            for (Map.Entry<Long, Set<Long>> entry : adjacency.entrySet()) {
                BlockPos a = BlockPos.of(entry.getKey());
                for (long other : entry.getValue()) {
                    if (entry.getKey() < other && found.size() < max) {
                        BlockPos b = BlockPos.of(other);
                        if (a.distSqr(center) <= rangeSqr || b.distSqr(center) <= rangeSqr) {
                            found.add(new long[] {entry.getKey(), other});
                        }
                    }
                }
            }
            return found;
        }
    }

    // ================================================================== track library with sub-folders

    /**
     * Track library of the music servers. The root of {@code config/mc_radio/tracks} plus one level of
     * sub-folders; every Music Server is switched to one of them with a right click. Track ids are
     * {@code name.mp3} or {@code folder/name.mp3}.
     */
    public static final class Lib {
        private static final Map<String, Integer> SECONDS = new ConcurrentHashMap<>();
        private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();

        private Lib() {
        }

        private static boolean validFolder(String folder) {
            return folder.matches("[A-Za-z0-9 _\\-]{1,40}");
        }

        public static List<String> folders() {
            McRadio.TrackLibrary.ensureFolder();
            try (Stream<Path> files = Files.list(McRadio.TrackLibrary.folder())) {
                return files.filter(Files::isDirectory).map(path -> path.getFileName().toString())
                        .filter(Lib::validFolder).sorted(String.CASE_INSENSITIVE_ORDER).limit(32).toList();
            } catch (IOException e) {
                return List.of();
            }
        }

        public static List<String> list(String folder) {
            Path dir = McRadio.TrackLibrary.folder();
            if (!folder.isEmpty()) {
                if (!validFolder(folder)) {
                    return List.of();
                }
                dir = dir.resolve(folder);
            }
            if (!Files.isDirectory(dir)) {
                return List.of();
            }
            try (Stream<Path> files = Files.list(dir)) {
                return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                        .filter(McRadio.TrackLibrary::isMp3Name).filter(name -> name.length() <= McRadio.TrackLibrary.MAX_NAME_LENGTH)
                        .sorted(String.CASE_INSENSITIVE_ORDER).limit(200)
                        .map(name -> folder.isEmpty() ? name : folder + "/" + name).toList();
            } catch (IOException e) {
                return List.of();
            }
        }

        public static Optional<Path> resolve(String id) {
            if (id == null || id.isEmpty() || id.length() > 260) {
                return Optional.empty();
            }
            int slash = id.indexOf('/');
            String folder = slash < 0 ? "" : id.substring(0, slash);
            String name = slash < 0 ? id : id.substring(slash + 1);
            if (!folder.isEmpty() && !validFolder(folder)) {
                return Optional.empty();
            }
            if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.contains("..") || name.indexOf(':') >= 0
                    || !McRadio.TrackLibrary.isMp3Name(name)) {
                return Optional.empty();
            }
            Path root = McRadio.TrackLibrary.folder().toAbsolutePath().normalize();
            Path file = (folder.isEmpty() ? root : root.resolve(folder)).resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return Optional.empty();
            }
            return Optional.of(file);
        }

        /** Length in whole seconds, or -1 while it is still being measured (the measuring is started here). */
        public static int seconds(String id) {
            Integer known = SECONDS.get(id);
            if (known != null) {
                return known;
            }
            if (LOADING.add(id)) {
                Optional<Path> path = resolve(id);
                if (path.isEmpty()) {
                    LOADING.remove(id);
                    return -1;
                }
                McRadio.Mp3Source.loadAsync(path.get()).whenComplete((source, error) -> {
                    if (source != null) {
                        SECONDS.put(id, (int) Math.max(1, Math.round((source.end() - source.start()) / source.bytesPerSecond())));
                    }
                    LOADING.remove(id);
                });
            }
            return -1;
        }
    }

    // ================================================================== chain resolution

    /** Everything one station is connected to: adjacency (translator, antenna) and cables (amp, computer, servers). */
    public record Chain(McRadio.StationBlockEntity station, TranslatorBE translator, AntennaBE antenna,
                        AmplifierBE amp, ChainComputerBE computer, List<MusicServerBE> servers) {
        public boolean cabled() {
            return amp != null && computer != null && !servers.isEmpty();
        }

        public boolean powered() {
            return amp != null && amp.isPowered();
        }

        /** "ok" or a short key explaining what is missing (translated in the lang files as chain.problem.KEY). */
        public String problem() {
            if (station == null) return "no_station";
            if (translator == null) return "no_translator";
            if (antenna == null) return "no_antenna";
            if (amp == null) return "no_amp";
            if (computer == null) return "no_computer";
            if (servers.isEmpty()) return "no_server";
            if (!amp.isPowered()) return "no_power";
            return "ok";
        }
    }

    public static Chain fromStation(Level level, BlockPos pos) {
        McRadio.StationBlockEntity station = level.getBlockEntity(pos) instanceof McRadio.StationBlockEntity s ? s : null;
        TranslatorBE translator = null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof TranslatorBE t) {
                translator = t;
                break;
            }
        }
        AntennaBE antenna = null;
        if (translator != null) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(translator.getBlockPos().relative(direction)) instanceof AntennaBE a
                        && (antenna == null || a.tier() > antenna.tier())) {
                    antenna = a;
                }
            }
        }
        AmplifierBE amp = null;
        ChainComputerBE computer = null;
        List<MusicServerBE> servers = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            CableLinks links = CableLinks.get(serverLevel);
            for (BlockPos other : links.neighbours(pos)) {
                if (serverLevel.getBlockEntity(other) instanceof AmplifierBE a) {
                    amp = a;
                    break;
                }
            }
            if (amp != null) {
                for (BlockPos other : links.neighbours(amp.getBlockPos())) {
                    if (serverLevel.getBlockEntity(other) instanceof ChainComputerBE c) {
                        computer = c;
                        break;
                    }
                }
            }
            if (computer != null) {
                for (BlockPos other : links.neighbours(computer.getBlockPos())) {
                    if (serverLevel.getBlockEntity(other) instanceof MusicServerBE m) {
                        servers.add(m);
                    }
                }
            }
        }
        return new Chain(station, translator, antenna, amp, computer, servers);
    }

    /** The chain a computer sits in, found through its amplifier's station; servers are always listed. */
    public static Chain fromComputer(ServerLevel level, ChainComputerBE computer) {
        CableLinks links = CableLinks.get(level);
        AmplifierBE firstAmp = null;
        for (BlockPos other : links.neighbours(computer.getBlockPos())) {
            if (level.getBlockEntity(other) instanceof AmplifierBE amp) {
                if (firstAmp == null) {
                    firstAmp = amp;
                }
                for (BlockPos stationPos : links.neighbours(amp.getBlockPos())) {
                    if (level.getBlockEntity(stationPos) instanceof McRadio.StationBlockEntity) {
                        return fromStation(level, stationPos);
                    }
                }
            }
        }
        List<MusicServerBE> servers = new ArrayList<>();
        for (BlockPos other : links.neighbours(computer.getBlockPos())) {
            if (level.getBlockEntity(other) instanceof MusicServerBE m) {
                servers.add(m);
            }
        }
        return new Chain(null, null, null, firstAmp, computer, servers);
    }

    // ---- hooks used by McRadio.RadioNetworkManager / StationBlockEntity / RadioPhysics ----

    /** True if the station has a Translator next to it and therefore belongs to the v4 chain. */
    public static boolean isChainStation(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof TranslatorBE) {
                return true;
            }
        }
        return false;
    }

    /** Frequency a station emits on: the Translator's for chain stations, its own for legacy ones. */
    public static int chainFrequency(Level level, BlockPos pos, int legacy) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof TranslatorBE t) {
                return t.frequency();
            }
        }
        return legacy;
    }

    /** Antenna radius of a chain station (before weather), or -1 for a legacy station. */
    public static int chainRadius(Level level, BlockPos pos) {
        if (!isChainStation(level, pos)) {
            return -1;
        }
        Chain chain = fromStation(level, pos);
        return chain.antenna() == null ? 1 : ChainTier.byLevel(chain.antenna().tier()).radius;
    }

    /** May this station put a signal on the air right now? Handles the chain, power and the secure channels. */
    public static boolean mayTransmit(Level level, McRadio.StationBlockEntity station) {
        BlockPos pos = station.getBlockPos();
        if (!isChainStation(level, pos)) {
            return !SecureChannels.isSecure(station.getFrequency());
        }
        Chain chain = fromStation(level, pos);
        return "ok".equals(chain.problem())
                && SecureChannels.canUse(chain.translator().frequency(), chain.translator().password());
    }

    /** A chain station is streamed by its Computer, not by the legacy playlist code. */
    public static boolean isDriven(Level level, BlockPos pos) {
        return isChainStation(level, pos);
    }

    /** Thunderstorms cut radio ranges by 30%. */
    public static int thunder(Level level, int radius) {
        return level.isThundering() ? (int) Math.round(radius * ChainTier.THUNDER_FACTOR) : radius;
    }

    /** Thunderstorms add crackle: a signal never gets cleaner than 88%. */
    public static double weather(Level level, double quality) {
        return level.isThundering() && quality > 0.0 ? Math.min(quality, 0.88) : quality;
    }

    /** True if a receiver on this frequency without the right password is locked out. */
    public static boolean locked(int frequency, String password) {
        return !SecureChannels.canUse(frequency, password);
    }

    // ================================================================== menu

    public static final class ChainMenu extends AbstractContainerMenu {
        private final BlockPos pos;

        public ChainMenu(MenuType<?> type, int containerId, BlockPos pos) {
            super(type, containerId);
            this.pos = pos;
        }

        public BlockPos pos() {
            return pos;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0
                    && player.level().getBlockEntity(pos) != null;
        }
    }

    // ================================================================== blocks

    /** Common base: facing + active state from the v3 devices, and cable cleanup when the block is broken. */
    public abstract static class ChainBlock extends McRadio.RadioDeviceBlock {
        protected ChainBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                CableLinks.get(serverLevel).removeAll(pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    public static class MusicServerBlock extends ChainBlock {
        public static final MapCodec<MusicServerBlock> CODEC = simpleCodec(MusicServerBlock::new);

        public MusicServerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends MusicServerBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MusicServerBE(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MusicServerBE server) {
                server.cycle(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static class BroadcastComputerBlock extends ChainBlock {
        public static final MapCodec<BroadcastComputerBlock> CODEC = simpleCodec(BroadcastComputerBlock::new);

        public BroadcastComputerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BroadcastComputerBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ChainComputerBE(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof ChainComputerBE computer) {
                serverPlayer.openMenu(computer, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        /** Redstone on the computer: interrupt the music and run an emergency announcement. */
        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                       BlockPos neighborPos, boolean movedByPiston) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChainComputerBE computer) {
                computer.onRedstone(level.hasNeighborSignal(pos));
            }
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, VoidChain.COMPUTER_BE.get(),
                    (tickLevel, pos, tickState, entity) -> entity.serverTick((ServerLevel) tickLevel));
        }
    }

    public static class AmplifierBlock extends ChainBlock {
        public static final MapCodec<AmplifierBlock> CODEC = simpleCodec(AmplifierBlock::new);

        public AmplifierBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends AmplifierBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AmplifierBE(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof AmplifierBE amp) {
                player.displayClientMessage(Component.translatable("message.mcradio.amp_status",
                        amp.stored(), AmplifierBE.CAPACITY,
                        Component.translatable(amp.isPowered() ? "message.mcradio.amp_on" : "message.mcradio.amp_off")), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, VoidChain.AMP_BE.get(),
                    (tickLevel, pos, tickState, entity) -> entity.serverTick((ServerLevel) tickLevel));
        }
    }

    public static class TranslatorBlock extends ChainBlock {
        public static final MapCodec<TranslatorBlock> CODEC = simpleCodec(TranslatorBlock::new);

        public TranslatorBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends TranslatorBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TranslatorBE(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof TranslatorBE translator) {
                serverPlayer.openMenu(translator, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, VoidChain.TRANSLATOR_BE.get(),
                    (tickLevel, pos, tickState, entity) -> entity.serverTick((ServerLevel) tickLevel));
        }
    }

    public static class AntennaBlock extends ChainBlock {
        public static final MapCodec<AntennaBlock> CODEC = simpleCodec(properties -> new AntennaBlock(properties, 1));
        private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
        private final int tier;

        public AntennaBlock(Properties properties, int tier) {
            super(properties);
            this.tier = tier;
        }

        public int tier() {
            return tier;
        }

        @Override
        protected MapCodec<? extends AntennaBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AntennaBE(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                int radius = VoidChain.thunder(level, ChainTier.byLevel(tier).radius);
                player.displayClientMessage(Component.translatable("message.mcradio.antenna_status", tier, radius), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    // ================================================================== block entities

    public static final class MusicServerBE extends BlockEntity {
        private String folder = "";

        public MusicServerBE(BlockPos pos, BlockState state) {
            super(VoidChain.SERVER_BE.get(), pos, state);
        }

        public String folder() {
            return folder;
        }

        public void cycle(Player player) {
            List<String> options = new ArrayList<>();
            options.add("");
            options.addAll(Lib.folders());
            int index = options.indexOf(folder);
            folder = options.get((index + 1) % options.size());
            setChanged();
            player.displayClientMessage(Component.translatable("message.mcradio.server_library",
                    folder.isEmpty() ? "/" : folder, Lib.list(folder).size()), true);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putString("Folder", folder);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            folder = tag.getString("Folder");
        }
    }

    public static final class AntennaBE extends BlockEntity {
        public AntennaBE(BlockPos pos, BlockState state) {
            super(VoidChain.ANTENNA_BE.get(), pos, state);
        }

        public int tier() {
            return getBlockState().getBlock() instanceof AntennaBlock block ? block.tier() : 1;
        }
    }

    /** Audio processor: needs FE (or a spinning Create component next to it) to keep the chain alive. */
    public static final class AmplifierBE extends BlockEntity {
        public static final int CAPACITY = 20_000;

        private static final class Battery extends EnergyStorage {
            Battery() {
                super(CAPACITY, 2_000, 0);
            }

            boolean take(int amount) {
                if (energy < amount) {
                    return false;
                }
                energy -= amount;
                return true;
            }

            void set(int value) {
                energy = Math.max(0, Math.min(capacity, value));
            }
        }

        private final Battery battery = new Battery();
        private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> battery);
        private boolean powered;
        private boolean kinetic;

        public AmplifierBE(BlockPos pos, BlockState state) {
            super(VoidChain.AMP_BE.get(), pos, state);
        }

        public boolean isPowered() {
            return powered;
        }

        public int stored() {
            return battery.getEnergyStored();
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == ForgeCapabilities.ENERGY) {
                return energyCap.cast();
            }
            return super.getCapability(cap, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            energyCap.invalidate();
        }

        /**
         * Create support without a hard dependency: a rotating Create block entity touching the amplifier
         * (at least {@link VoidChain#AMP_MIN_ROTATION} RPM) powers it. Looked up by reflection every half second.
         */
        private boolean createRotation(ServerLevel level) {
            for (Direction direction : Direction.values()) {
                BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
                if (neighbour != null && neighbour.getClass().getName().startsWith("com.simibubi.create")) {
                    try {
                        Object speed = neighbour.getClass().getMethod("getSpeed").invoke(neighbour);
                        if (speed instanceof Float value && Math.abs(value) >= AMP_MIN_ROTATION) {
                            return true;
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        // not a kinetic block entity: ignore
                    }
                }
            }
            return false;
        }

        public void serverTick(ServerLevel level) {
            if (level.getGameTime() % 10 == 0) {
                kinetic = createRotation(level);
            }
            boolean now = kinetic || battery.take(AMP_COST_PER_TICK);
            if (now != powered) {
                powered = now;
                setChanged();
                McRadio.RadioDeviceBlock.setActive(level, worldPosition, now);
                McRadio.RadioNetworkManager.markDirty(level);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Energy", battery.getEnergyStored());
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            battery.set(tag.getInt("Energy"));
        }
    }

    /** Frequency modulator: binds the stream to a wave and holds the password for the secure channels. */
    public static final class TranslatorBE extends BlockEntity implements MenuProvider {
        private int frequency = McRadio.RadioPhysics.DEFAULT_FREQUENCY;
        private String password = "";
        private boolean chainOk;
        private int tier;
        private String problem = "no_station";
        private boolean lockedState;

        public TranslatorBE(BlockPos pos, BlockState state) {
            super(VoidChain.TRANSLATOR_BE.get(), pos, state);
        }

        public int frequency() {
            return frequency;
        }

        public String password() {
            return password;
        }

        public boolean chainOk() {
            return chainOk;
        }

        public int tier() {
            return tier;
        }

        public String problem() {
            return problem;
        }

        /** Server-computed and synced, because the password itself never reaches the client. */
        public boolean locked() {
            return lockedState;
        }

        public void configure(ServerLevel level, int newFrequency, String newPassword) {
            frequency = McRadio.RadioPhysics.clampFrequency(newFrequency);
            password = newPassword == null ? "" : newPassword.strip();
            if (password.length() > 24) {
                password = password.substring(0, 24);
            }
            lockedState = !SecureChannels.canUse(frequency, password);
            refresh(level);
            setChanged();
            sync();
            McRadio.RadioNetworkManager.markDirty(level);
        }

        private void refresh(ServerLevel level) {
            McRadio.StationBlockEntity station = null;
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction)) instanceof McRadio.StationBlockEntity s) {
                    station = s;
                    break;
                }
            }
            Chain chain = station == null ? null : fromStation(level, station.getBlockPos());
            String newProblem = chain == null ? "no_station" : chain.problem();
            boolean newOk = "ok".equals(newProblem);
            int newTier = chain == null || chain.antenna() == null ? 0 : chain.antenna().tier();
            if (newOk != chainOk || newTier != tier || !newProblem.equals(problem)) {
                chainOk = newOk;
                tier = newTier;
                problem = newProblem;
                sync();
            }
        }

        public void serverTick(ServerLevel level) {
            if (level.getGameTime() % 20 == 0) {
                refresh(level);
            }
        }

        private void sync() {
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Frequency", frequency);
            tag.putString("Pass", password);
            tag.putBoolean("ChainOk", chainOk);
            tag.putInt("Tier", tier);
            tag.putString("Problem", problem);
            tag.putBoolean("Locked", lockedState);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            frequency = tag.contains("Frequency") ? McRadio.RadioPhysics.clampFrequency(tag.getInt("Frequency"))
                    : McRadio.RadioPhysics.DEFAULT_FREQUENCY;
            password = tag.getString("Pass");
            chainOk = tag.getBoolean("ChainOk");
            tier = tag.getInt("Tier");
            problem = tag.contains("Problem") ? tag.getString("Problem") : "no_station";
            lockedState = tag.getBoolean("Locked");
        }

        /** The password never leaves the server. */
        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = saveWithoutMetadata(registries);
            tag.remove("Pass");
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.translator");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new ChainMenu(VoidChain.TRANSLATOR_MENU.get(), containerId, worldPosition);
        }
    }

    // ================================================================== the computer (on-air brain)

    public static final class ChainComputerBE extends BlockEntity implements MenuProvider {
        public static final String DEFAULT_ALARM = "ВНИМАНИЕ! Экстренное оповещение!";

        private static final class Feed {
            String id = "";
            int epoch = -1;
            McRadio.Mp3Source source;
            CompletableFuture<McRadio.Mp3Source> loading;
            int cursor;
            double budget;
            boolean positioned;

            void reset() {
                id = "";
                epoch = -1;
                source = null;
                if (loading != null) {
                    loading.cancel(false);
                    loading = null;
                }
                positioned = false;
            }
        }

        private final BroadcastEngine engine = new BroadcastEngine();
        private final Mixer mixer = new Mixer();
        private final Feed musicFeed = new Feed();
        private final Feed voiceFeed = new Feed();
        private Chain chain;
        private long chainStamp = Long.MIN_VALUE;
        private boolean airApplied;
        private int lastAnnEpoch = -1;
        private int lastMusicPct = -1;
        private int lastAnnPct = -1;
        private long lastMixSent;
        private long lastSend;
        private int libraryCounter;

        public ChainComputerBE(BlockPos pos, BlockState state) {
            super(VoidChain.COMPUTER_BE.get(), pos, state);
        }

        // ------------------------------------------------------------ lifecycle

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel && chain != null && chain.station() != null && airApplied) {
                chain.station().setChainAir(false);
                McRadio.RadioNetworkManager.sendStreamEnd(serverLevel, chain.station().key());
                McRadio.RadioNetworkManager.markDirty(serverLevel);
            }
            musicFeed.reset();
            voiceFeed.reset();
            super.setRemoved();
        }

        /** Redstone on the computer: interrupt the music with an emergency announcement from the queue. */
        public void onRedstone(boolean powered) {
            if (powered && engine.emergencyCount() == 0 && engine.announcementsView().isEmpty()) {
                engine.registerEmergency(new BroadcastEngine.Item("T:" + DEFAULT_ALARM, textSeconds(DEFAULT_ALARM), true));
            }
            engine.onRedstone(powered);
        }

        static int textSeconds(String text) {
            return Math.max(4, text.length() / 9 + 2);
        }

        // ------------------------------------------------------------ server tick

        public void serverTick(ServerLevel level) {
            long now = level.getGameTime();
            if (chain == null || now - chainStamp >= 10) {
                chain = fromComputer(level, this);
                chainStamp = now;
            }
            McRadio.StationBlockEntity station = chain.station();
            boolean live = station != null && chain.translator() != null && chain.cabled() && chain.powered();
            if (live) {
                engine.serverTick();
                drive(level, station, now);
            }
            boolean air = live && engine.snapshot().state() != BroadcastEngine.State.STOPPED;
            if (station != null && air != airApplied) {
                station.setChainAir(air);
                airApplied = air;
                McRadio.RadioNetworkManager.markDirty(level);
                if (!air) {
                    McRadio.RadioNetworkManager.sendStreamEnd(level, station.key());
                    musicFeed.reset();
                    voiceFeed.reset();
                }
            }
            McRadio.RadioDeviceBlock.setActive(level, worldPosition, air);
            if (engine.consumeDirty() || now % 10 == 0) {
                sendState(level);
            }
        }

        private void drive(ServerLevel level, McRadio.StationBlockEntity station, long now) {
            BroadcastEngine.Snapshot snapshot = engine.snapshot();
            boolean sent = feed(musicFeed, station, level, now, engine.musicItem(), engine.musicEpoch(),
                    engine.musicPositionTicks(), engine.musicPlaying(), McRadio.AudioCodec.MP3);

            BroadcastEngine.Item announcement = engine.announcementItem();
            BroadcastEngine.Item voice = announcement != null && announcement.id().startsWith("V:") ? announcement : null;
            sent |= feed(voiceFeed, station, level, now, voice, engine.annEpoch(), snapshot.positionTicks(),
                    voice != null && snapshot.state() == BroadcastEngine.State.PLAYING, McRadio.AudioCodec.MP3_ANNOUNCEMENT);

            if (engine.annEpoch() != lastAnnEpoch) {
                lastAnnEpoch = engine.annEpoch();
                if (announcement != null && announcement.id().startsWith("T:")) {
                    String text = announcement.id().substring(2);
                    String sender = snapshot.emergency() ? "ЭКСТРЕННЫЙ ЭФИР" : "ЭФИР";
                    int frequency = chain.translator().frequency();
                    for (ServerPlayer player : McRadio.RadioNetworkManager.listeners(level, station.key())) {
                        McRadio.RadioNetwork.sendToPlayer(player, new McRadio.RadioTextPacket(frequency, sender, text));
                    }
                }
            }

            if (sent) {
                lastSend = now;
            } else if (snapshot.state() != BroadcastEngine.State.STOPPED && now - lastSend >= 15) {
                lastSend = now;
                byte[] silence = new byte[800];
                Arrays.fill(silence, McRadio.UlawCodec.silence());
                station.broadcast(level, new McRadio.AudioChunkPacket(station.key(), McRadio.AudioCodec.ULAW_16K, silence));
            }

            int musicPct = Math.round(mixer.musicGain(snapshot.duck()) * 100f);
            int annPct = Math.round(mixer.announcementGain() * 100f);
            if (musicPct != lastMusicPct || annPct != lastAnnPct || now - lastMixSent >= 40) {
                lastMusicPct = musicPct;
                lastAnnPct = annPct;
                lastMixSent = now;
                MixPacket packet = new MixPacket(station.key(), musicPct, annPct);
                for (ServerPlayer player : McRadio.RadioNetworkManager.listeners(level, station.key())) {
                    sendTo(player, packet);
                }
            }
        }

        /** Streams one channel (music or voice announcement) from its current position. Returns true if bytes were sent. */
        private boolean feed(Feed f, McRadio.StationBlockEntity station, ServerLevel level, long now,
                             BroadcastEngine.Item item, int epoch, int positionTicks, boolean playing, int codec) {
            if (item == null) {
                f.reset();
                return false;
            }
            String libraryId = item.id().substring(2);
            if (!libraryId.equals(f.id)) {
                f.reset();
                f.id = libraryId;
                f.epoch = epoch;
                Optional<Path> file = Lib.resolve(libraryId);
                if (file.isEmpty()) {
                    return false;
                }
                f.loading = McRadio.Mp3Source.loadAsync(file.get());
            } else if (epoch != f.epoch) {
                f.epoch = epoch;
                f.positioned = false;
            }
            if (f.source == null) {
                if (f.loading == null || !f.loading.isDone()) {
                    return false;
                }
                try {
                    f.source = f.loading.join();
                } catch (RuntimeException e) {
                    McRadio.LOGGER.warn("Could not load track '{}' for the broadcast computer at {}", libraryId, worldPosition, e);
                    f.loading = null;
                    return false;
                }
                f.loading = null;
            }
            if (!f.positioned) {
                double bytesPerSecond = f.source.bytesPerSecond();
                f.cursor = Math.min(f.source.end() - 1, f.source.start() + (int) (positionTicks / 20.0 * bytesPerSecond));
                f.budget = bytesPerSecond;
                f.positioned = true;
            }
            if (!playing) {
                return false;
            }
            f.budget += f.source.bytesPerSecond() / 20.0;
            if (Math.floorMod(now + worldPosition.hashCode(), 4) != 0) {
                return false;
            }
            int wanted = (int) Math.min(f.budget, 200_000.0);
            wanted = Math.min(wanted, f.source.end() - f.cursor);
            if (wanted < 256) {
                return false;
            }
            f.budget -= wanted;
            byte[] chunk = new byte[wanted];
            System.arraycopy(f.source.data(), f.cursor, chunk, 0, wanted);
            f.cursor += wanted;
            station.broadcast(level, new McRadio.AudioChunkPacket(station.key(), codec, chunk));
            return true;
        }

        // ------------------------------------------------------------ GUI sync

        private List<ServerPlayer> viewers(ServerLevel level) {
            List<ServerPlayer> result = new ArrayList<>();
            for (ServerPlayer player : level.players()) {
                if (player.containerMenu instanceof ChainMenu menu && menu.pos().equals(worldPosition)) {
                    result.add(player);
                }
            }
            return result;
        }

        private void sendState(ServerLevel level) {
            List<ServerPlayer> viewers = viewers(level);
            if (viewers.isEmpty()) {
                return;
            }
            ComputerStatePacket packet = statePacket();
            for (ServerPlayer player : viewers) {
                sendTo(player, packet);
            }
            if (++libraryCounter >= 10) {
                libraryCounter = 0;
                sendLibrary(viewers);
            }
        }

        public void sendLibrary(List<ServerPlayer> targets) {
            if (chain == null && level instanceof ServerLevel serverLevel) {
                chain = fromComputer(serverLevel, this);
            }
            List<LibraryPacket.ServerInfo> servers = new ArrayList<>();
            if (chain != null) {
                for (MusicServerBE server : chain.servers()) {
                    List<LibraryPacket.TrackInfo> tracks = new ArrayList<>();
                    for (String id : Lib.list(server.folder())) {
                        tracks.add(new LibraryPacket.TrackInfo(id, Lib.seconds(id)));
                    }
                    servers.add(new LibraryPacket.ServerInfo(server.getBlockPos(), server.folder(), tracks));
                }
            }
            LibraryPacket packet = new LibraryPacket(worldPosition, servers);
            for (ServerPlayer player : targets) {
                sendTo(player, packet);
            }
        }

        private ComputerStatePacket statePacket() {
            BroadcastEngine.Snapshot s = engine.snapshot();
            List<String> queue = new ArrayList<>();
            for (BroadcastEngine.Item item : engine.queueView()) {
                if (queue.size() >= 40) {
                    break;
                }
                queue.add(item.id() + "\u0001" + item.totalSeconds());
            }
            String problem = chain == null ? "no_station" : chain.problem();
            int frequency = chain != null && chain.translator() != null ? chain.translator().frequency() : 0;
            int tier = chain != null && chain.antenna() != null ? chain.antenna().tier() : 0;
            String currentId = s.current() == null ? "" : s.current().id();
            int total = s.current() == null ? 0 : s.current().totalSeconds();
            return new ComputerStatePacket(worldPosition, s.state().ordinal(), currentId, total, s.positionTicks(),
                    Math.round(s.duck() * 100f), s.emergency(), queue,
                    engine.announcementsView().size(), engine.emergencyCount(),
                    mixer.music(), mixer.announcement(), mixer.master(), engine.looping(),
                    chain == null ? 0 : chain.servers().size(), chain != null && chain.powered(), frequency, tier, problem);
        }

        // ------------------------------------------------------------ actions from the GUI

        public void handle(ServerPlayer player, ComputerActionPacket packet) {
            switch (packet.action()) {
                case REQUEST_LIBRARY -> sendLibrary(List.of(player));
                case PLAY -> engine.play();
                case PAUSE -> engine.pause();
                case STOP -> engine.stop();
                case SEEK_TO -> engine.seekTo(packet.a());
                case SEEK_BY -> engine.seekBy(packet.a());
                case SKIP -> engine.skip();
                case REMOVE -> engine.removeFromQueue(packet.a());
                case CLEAR -> engine.clearQueue();
                case LOOP -> engine.setLoop(!engine.looping());
                case ADD_TRACK -> {
                    int seconds = Lib.seconds(packet.text());
                    if (seconds <= 0) {
                        player.displayClientMessage(Component.translatable("message.mcradio.duration_pending"), true);
                    } else {
                        engine.enqueueTrack(new BroadcastEngine.Item("M:" + packet.text(), seconds, false));
                    }
                }
                case ADD_TEXT -> {
                    String text = clean(packet.text());
                    if (!text.isEmpty()) {
                        engine.enqueueAnnouncement(new BroadcastEngine.Item("T:" + text, textSeconds(text), true));
                    }
                }
                case ADD_VOICE -> {
                    int seconds = Lib.seconds(packet.text());
                    if (seconds <= 0) {
                        player.displayClientMessage(Component.translatable("message.mcradio.duration_pending"), true);
                    } else {
                        engine.enqueueAnnouncement(new BroadcastEngine.Item("V:" + packet.text(), seconds, true));
                    }
                }
                case EMERGENCY_TEXT -> {
                    String text = clean(packet.text());
                    if (text.isEmpty()) {
                        text = DEFAULT_ALARM;
                    }
                    engine.registerEmergency(new BroadcastEngine.Item("T:" + text, textSeconds(text), true));
                    engine.triggerEmergency();
                }
                case EMERGENCY_VOICE -> {
                    int seconds = Lib.seconds(packet.text());
                    if (seconds <= 0) {
                        player.displayClientMessage(Component.translatable("message.mcradio.duration_pending"), true);
                    } else {
                        engine.registerEmergency(new BroadcastEngine.Item("V:" + packet.text(), seconds, true));
                        engine.triggerEmergency();
                    }
                }
                case SET_MIX -> {
                    mixer.setMusic(packet.a() & 0xFF);
                    mixer.setAnnouncement((packet.a() >> 8) & 0xFF);
                    mixer.setMaster((packet.a() >> 16) & 0xFF);
                    setChanged();
                    lastMixSent = 0;
                }
            }
            if (level instanceof ServerLevel serverLevel) {
                sendState(serverLevel);
            }
            setChanged();
        }

        private static String clean(String raw) {
            StringBuilder out = new StringBuilder();
            for (char c : raw.strip().toCharArray()) {
                if (c >= 32 && c != 127) {
                    out.append(c);
                }
            }
            return out.length() > 120 ? out.substring(0, 120) : out.toString();
        }

        // ------------------------------------------------------------ persistence

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putIntArray("Mix", mixer.toArray());
            tag.putBoolean("Loop", engine.looping());
            ListTag queue = new ListTag();
            for (BroadcastEngine.Item item : engine.queueView()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", item.id());
                entry.putInt("Sec", item.totalSeconds());
                queue.add(entry);
            }
            tag.put("Queue", queue);
            BroadcastEngine.Item music = engine.musicItem();
            if (music != null) {
                tag.putString("CurId", music.id());
                tag.putInt("CurSec", music.totalSeconds());
                tag.putInt("CurPos", engine.musicPositionTicks());
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            mixer.fromArray(tag.getIntArray("Mix"));
            List<BroadcastEngine.Item> queue = new ArrayList<>();
            for (Tag raw : tag.getList("Queue", Tag.TAG_COMPOUND)) {
                if (raw instanceof CompoundTag entry && entry.getString("Id").startsWith("M:")) {
                    queue.add(new BroadcastEngine.Item(entry.getString("Id"), entry.getInt("Sec"), false));
                }
            }
            BroadcastEngine.Item current = null;
            if (tag.contains("CurId") && tag.getString("CurId").startsWith("M:")) {
                current = new BroadcastEngine.Item(tag.getString("CurId"), tag.getInt("CurSec"), false);
            }
            engine.restore(current, tag.getInt("CurPos"), queue, tag.getBoolean("Loop"));
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.computer");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new ChainMenu(VoidChain.COMPUTER_MENU.get(), containerId, worldPosition);
        }
    }

    // ================================================================== cable wrench

    public static class CableWrenchItem extends Item {
        private static final Map<UUID, BlockPos> SELECTION = new HashMap<>();

        public CableWrenchItem(Properties properties) {
            super(properties);
        }

        /** Position of a block in the chain: 0 server, 1 computer, 2 amplifier, 3 station; -1 otherwise. */
        static int role(Block block) {
            if (block == SERVER_BLOCK.get()) return 0;
            if (block == COMPUTER_BLOCK.get()) return 1;
            if (block == AMP_BLOCK.get()) return 2;
            if (block == McRadio.ModRegistry.STATION_BLOCK.get()) return 3;
            return -1;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (level.isClientSide || player == null || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            BlockPos pos = context.getClickedPos();
            int role = role(level.getBlockState(pos).getBlock());
            if (role < 0) {
                SELECTION.remove(player.getUUID());
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.not_chain"), true);
                return InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown()) {
                CableLinks.get(serverLevel).removeAll(pos);
                SELECTION.remove(player.getUUID());
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.cleared"), true);
                return InteractionResult.CONSUME;
            }
            BlockPos first = SELECTION.get(player.getUUID());
            if (first == null) {
                SELECTION.put(player.getUUID(), pos.immutable());
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.selected",
                        level.getBlockState(pos).getBlock().getName()), true);
                return InteractionResult.CONSUME;
            }
            SELECTION.remove(player.getUUID());
            if (first.equals(pos)) {
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.cancelled"), true);
                return InteractionResult.CONSUME;
            }
            int firstRole = role(level.getBlockState(first).getBlock());
            if (firstRole < 0 || Math.abs(firstRole - role) != 1) {
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.bad_pair"), true);
                return InteractionResult.CONSUME;
            }
            if (first.distSqr(pos) > (double) CABLE_RANGE * CABLE_RANGE) {
                player.displayClientMessage(Component.translatable("message.mcradio.wrench.too_far", CABLE_RANGE), true);
                return InteractionResult.CONSUME;
            }
            boolean linked = CableLinks.get(serverLevel).toggle(first, pos);
            McRadio.RadioNetworkManager.markDirty(level);
            player.displayClientMessage(Component.translatable(linked ? "message.mcradio.wrench.linked"
                    : "message.mcradio.wrench.unlinked"), true);
            return InteractionResult.CONSUME;
        }
    }

    // ================================================================== packets

    public static record ComputerActionPacket(BlockPos pos, Action action, int a, String text) {
        public enum Action {
            REQUEST_LIBRARY, PLAY, PAUSE, STOP, SEEK_TO, SEEK_BY, SKIP, REMOVE, CLEAR, LOOP,
            ADD_TRACK, ADD_TEXT, ADD_VOICE, EMERGENCY_TEXT, EMERGENCY_VOICE, SET_MIX
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ComputerActionPacket> CODEC =
                StreamCodec.ofMember(ComputerActionPacket::write, ComputerActionPacket::new);

        public ComputerActionPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Action.class), buf.readVarInt(), buf.readUtf(256));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeEnum(action);
            buf.writeVarInt(a);
            buf.writeUtf(text, 256);
        }

        public static void handle(ComputerActionPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || !(sender.containerMenu instanceof ChainMenu menu) || !menu.pos().equals(message.pos())) {
                    return;
                }
                if (sender.serverLevel().getBlockEntity(message.pos()) instanceof ChainComputerBE computer) {
                    computer.handle(sender, message);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static record TranslatorSetPacket(BlockPos pos, int frequency, String password) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TranslatorSetPacket> CODEC =
                StreamCodec.ofMember(TranslatorSetPacket::write, TranslatorSetPacket::new);

        public TranslatorSetPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(64));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(frequency);
            buf.writeUtf(password, 64);
        }

        public static void handle(TranslatorSetPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || !(sender.containerMenu instanceof ChainMenu menu) || !menu.pos().equals(message.pos())) {
                    return;
                }
                if (sender.serverLevel().getBlockEntity(message.pos()) instanceof TranslatorBE translator) {
                    translator.configure(sender.serverLevel(), message.frequency(), message.password());
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static record CableRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, CableRequestPacket> CODEC =
                StreamCodec.unit(new CableRequestPacket());

        public static void handle(CableRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null) {
                    return;
                }
                boolean holding = sender.getMainHandItem().getItem() instanceof CableWrenchItem
                        || sender.getOffhandItem().getItem() instanceof CableWrenchItem;
                if (holding) {
                    sendTo(sender, new CableListPacket(CableLinks.get(sender.serverLevel())
                            .near(sender.blockPosition(), 64, 256)));
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static record CableListPacket(List<long[]> pairs) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CableListPacket> CODEC =
                StreamCodec.ofMember(CableListPacket::write, CableListPacket::new);

        public CableListPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<long[]> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), 256);
            List<long[]> pairs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                pairs.add(new long[] {buf.readLong(), buf.readLong()});
            }
            return pairs;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(pairs.size());
            for (long[] pair : pairs) {
                buf.writeLong(pair[0]);
                buf.writeLong(pair[1]);
            }
        }

        public static void handle(CableListPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientHooks.cables(message.pairs()));
            context.setPacketHandled(true);
        }
    }

    /** Gains of the two channels of a station stream, in percent: music (ducked) and announcement. */
    public static record MixPacket(long station, int music, int announcement) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MixPacket> CODEC =
                StreamCodec.ofMember(MixPacket::write, MixPacket::new);

        public MixPacket(FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readVarInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(station);
            buf.writeVarInt(music);
            buf.writeVarInt(announcement);
        }

        public static void handle(MixPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientHooks.mix(message.station(), message.music(), message.announcement()));
            context.setPacketHandled(true);
        }
    }

    public static record ComputerStatePacket(BlockPos pos, int state, String currentId, int currentTotal, int positionTicks,
                                             int duckPercent, boolean emergency, List<String> queue, int announcements,
                                             int emergencies, int music, int announcementVolume, int master, boolean loop,
                                             int servers, boolean powered, int frequency, int tier, String problem) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ComputerStatePacket> CODEC =
                StreamCodec.ofMember(ComputerStatePacket::write, ComputerStatePacket::new);

        public ComputerStatePacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(300), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readBoolean(), readQueue(buf), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                    buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(32));
        }

        private static List<String> readQueue(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), 40);
            List<String> queue = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                queue.add(buf.readUtf(300));
            }
            return queue;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(state);
            buf.writeUtf(currentId, 300);
            buf.writeVarInt(currentTotal);
            buf.writeVarInt(positionTicks);
            buf.writeVarInt(duckPercent);
            buf.writeBoolean(emergency);
            buf.writeVarInt(queue.size());
            for (String entry : queue) {
                buf.writeUtf(entry, 300);
            }
            buf.writeVarInt(announcements);
            buf.writeVarInt(emergencies);
            buf.writeVarInt(music);
            buf.writeVarInt(announcementVolume);
            buf.writeVarInt(master);
            buf.writeBoolean(loop);
            buf.writeVarInt(servers);
            buf.writeBoolean(powered);
            buf.writeVarInt(frequency);
            buf.writeVarInt(tier);
            buf.writeUtf(problem, 32);
        }

        public static void handle(ComputerStatePacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientHooks.computerState(message));
            context.setPacketHandled(true);
        }
    }

    public static record LibraryPacket(BlockPos pos, List<ServerInfo> servers) {
        public static record TrackInfo(String id, int seconds) {
        }

        public static record ServerInfo(BlockPos pos, String folder, List<TrackInfo> tracks) {
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, LibraryPacket> CODEC =
                StreamCodec.ofMember(LibraryPacket::write, LibraryPacket::new);

        public LibraryPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), readServers(buf));
        }

        private static List<ServerInfo> readServers(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), 16);
            List<ServerInfo> servers = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BlockPos serverPos = buf.readBlockPos();
                String folder = buf.readUtf(64);
                int trackCount = Math.min(buf.readVarInt(), 200);
                List<TrackInfo> tracks = new ArrayList<>(trackCount);
                for (int t = 0; t < trackCount; t++) {
                    tracks.add(new TrackInfo(buf.readUtf(260), buf.readVarInt() + 1));
                }
                servers.add(new ServerInfo(serverPos, folder, tracks));
            }
            return servers;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(servers.size());
            for (ServerInfo server : servers) {
                buf.writeBlockPos(server.pos());
                buf.writeUtf(server.folder(), 64);
                buf.writeVarInt(server.tracks().size());
                for (TrackInfo track : server.tracks()) {
                    buf.writeUtf(track.id(), 260);
                    buf.writeVarInt(track.seconds() + 1);
                }
            }
        }

        public static void handle(LibraryPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientHooks.library(message));
            context.setPacketHandled(true);
        }
    }

    // ================================================================== client side

    /** Entry points from packet handlers into client-only code. */
    public static final class ClientHooks {
        private static List<long[]> cables = List.of();

        private ClientHooks() {
        }

        static void cables(List<long[]> pairs) {
            cables = pairs;
        }

        static List<long[]> cableList() {
            return cables;
        }

        static void mix(long station, int music, int announcement) {
            McRadio.ClientAudioManager.get(station).setGains(music / 100.0F, announcement / 100.0F);
        }

        static void computerState(ComputerStatePacket packet) {
            if (Minecraft.getInstance().screen instanceof ComputerScreen screen && screen.getMenu().pos().equals(packet.pos())) {
                screen.apply(packet);
            }
        }

        static void library(LibraryPacket packet) {
            if (Minecraft.getInstance().screen instanceof ComputerScreen screen && screen.getMenu().pos().equals(packet.pos())) {
                screen.applyLibrary(packet);
            }
        }
    }

    public static final class ClientInit {
        private ClientInit() {
        }

        public static void register() {
            MenuScreens.register(COMPUTER_MENU.get(), ComputerScreen::new);
            MenuScreens.register(TRANSLATOR_MENU.get(), TranslatorScreen::new);
        }
    }

    /**
     * Neon cable overlay: the invisible cables are drawn only while a Cable Wrench is in a hand. The client asks
     * the server for the links around it once a second while that is the case.
     */
    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientCables {
        private static long lastRequest;

        private ClientCables() {
        }

        @SubscribeEvent
        public static void onRender(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return;
            }
            boolean holding = player.getMainHandItem().getItem() instanceof CableWrenchItem
                    || player.getOffhandItem().getItem() instanceof CableWrenchItem;
            if (!holding) {
                ClientHooks.cables(List.of());
                return;
            }
            long now = Util.getMillis();
            if (now - lastRequest > 1000L) {
                lastRequest = now;
                sendToServer(new CableRequestPacket());
            }
            List<long[]> links = ClientHooks.cableList();
            if (links.isEmpty()) {
                return;
            }
            org.joml.Matrix4f matrix = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            float pulse = 0.65F + 0.35F * (float) Math.sin(now / 250.0);
            org.joml.Vector3f normal = new org.joml.Vector3f();
            for (long[] pair : links) {
                BlockPos a = BlockPos.of(pair[0]);
                BlockPos b = BlockPos.of(pair[1]);
                float x1 = (float) (a.getX() + 0.5 - camera.x);
                float y1 = (float) (a.getY() + 0.5 - camera.y);
                float z1 = (float) (a.getZ() + 0.5 - camera.z);
                float x2 = (float) (b.getX() + 0.5 - camera.x);
                float y2 = (float) (b.getY() + 0.5 - camera.y);
                float z2 = (float) (b.getZ() + 0.5 - camera.z);
                float dx = x2 - x1;
                float dy = y2 - y1;
                float dz = z2 - z1;
                float length = Math.max(0.001F, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
                matrix.transformDirection(dx / length, dy / length, dz / length, normal);
                // a thick neon line: the core plus six lines offset by a hair, in cyan with a magenta halo
                float[][] offsets = {{0, 0, 0}, {0.03F, 0, 0}, {-0.03F, 0, 0}, {0, 0.03F, 0}, {0, -0.03F, 0}, {0, 0, 0.03F}, {0, 0, -0.03F}};
                for (int i = 0; i < offsets.length; i++) {
                    float[] o = offsets[i];
                    int red = i == 0 ? 0 : 255;
                    int green = i == 0 ? 240 : 46;
                    int blue = i == 0 ? 255 : 147;
                    int alpha = (int) ((i == 0 ? 255 : 150) * pulse);
                    lines.addVertex(matrix, x1 + o[0], y1 + o[1], z1 + o[2]).setColor(red, green, blue, alpha)
                            .setNormal(normal.x, normal.y, normal.z);
                    lines.addVertex(matrix, x2 + o[0], y2 + o[1], z2 + o[2]).setColor(red, green, blue, alpha)
                            .setNormal(normal.x, normal.y, normal.z);
                }
            }
            buffers.endBatch(RenderType.lines());
        }
    }

    // ---------------------------------------------------------------- dark UI toolkit

    static final class Ui {
        static final int BG = 0xFF121212;
        static final int PANEL = 0xFF1E1E1E;
        static final int EDGE = 0xFF2E2E2E;
        static final int CYAN = 0xFF00F0FF;
        static final int PINK = 0xFFFF2E93;
        static final int GREEN = 0xFF39FF88;
        static final int AMBER = 0xFFFFB000;
        static final int RED = 0xFFFF3B3B;
        static final int TEXT = 0xFFE0E0E0;
        static final int MUTED = 0xFF8A8A8A;

        private Ui() {
        }

        static void panel(GuiGraphics g, int x, int y, int w, int h, Component title, Font font) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x6000F0FF);
            g.fill(x, y, x + w, y + h, BG);
            g.fill(x + 3, y + 22, x + w - 3, y + h - 3, PANEL);
            g.fill(x, y, x + w, y + 20, BG);
            g.fill(x, y + 20, x + w, y + 21, CYAN);
            g.drawString(font, title, x + 8, y + 6, CYAN, false);
            for (int i = 0; i < 3; i++) {
                g.fill(x + w - 14 - i * 8, y + 7, x + w - 9 - i * 8, y + 12, i == 0 ? PINK : EDGE);
            }
        }

        static void inset(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, 0xFF000000);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
        }

        static void led(GuiGraphics g, int x, int y, boolean on, int color) {
            g.fill(x, y, x + 7, y + 7, 0xFF000000);
            g.fill(x + 1, y + 1, x + 6, y + 6, on ? color : 0xFF2A2A2A);
        }

        static String clip(Font font, String text, int width) {
            return font.plainSubstrByWidth(text, width);
        }

        /** Human readable name of an on-air item id. */
        static String label(String id) {
            if (id.startsWith("T:")) {
                return "[ТЕКСТ] " + id.substring(2);
            }
            String name = id.length() > 2 ? id.substring(2) : id;
            int slash = name.lastIndexOf('/');
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            if (name.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                name = name.substring(0, name.length() - 4);
            }
            return id.startsWith("V:") ? "[ГОЛОС] " + name : name;
        }
    }

    static final class NeonButton extends Button {
        private final int color;

        NeonButton(int x, int y, int width, int height, Component label, OnPress onPress, int color) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.color = color;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hot = active && isHoveredOrFocused();
            int frame = !active ? 0xFF2A2A2A : hot ? color : (color & 0x00FFFFFF) | 0x90000000;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, frame);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, hot ? 0xFF2A2A2A : Ui.BG);
            int text = !active ? 0xFF555555 : hot ? color : Ui.TEXT;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, text);
        }
    }

    static final class MixSlider extends AbstractSliderButton {
        private final Component prefix;
        private final IntConsumer onCommit;

        MixSlider(int x, int y, int width, int height, Component prefix, int percent, IntConsumer onCommit) {
            super(x, y, width, height, Component.empty(), percent / 100.0);
            this.prefix = prefix;
            this.onCommit = onCommit;
            updateMessage();
        }

        int percent() {
            return (int) Math.round(value * 100.0);
        }

        void set(int percent) {
            this.value = Math.max(0, Math.min(100, percent)) / 100.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty().append(prefix).append(" " + percent() + "%"));
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            onCommit.accept(percent());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, Ui.BG);
            int fill = (int) ((width - 2) * value);
            graphics.fill(getX() + 1, getY() + 1, getX() + 1 + fill, getY() + height - 1, 0xFF005F66);
            graphics.fill(getX() + 1 + Math.max(0, fill - 1), getY() + 1, getX() + 1 + fill + 1, getY() + height - 1, Ui.CYAN);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, Ui.TEXT);
        }
    }

    abstract static class ChainScreen extends Screen implements MenuAccess<ChainMenu> {
        protected final ChainMenu menu;
        protected final int panelWidth;
        protected final int panelHeight;
        protected int left;
        protected int top;

        protected ChainScreen(ChainMenu menu, Component title, int panelWidth, int panelHeight) {
            super(title);
            this.menu = menu;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
        }

        @Override
        public ChainMenu getMenu() {
            return menu;
        }

        @Override
        protected void init() {
            left = (width - panelWidth) / 2;
            top = (height - panelHeight) / 2;
            buildWidgets();
        }

        protected abstract void buildWidgets();

        protected abstract void drawForeground(GuiGraphics graphics, int mouseX, int mouseY);

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xC0000000);
            Ui.panel(graphics, left, top, panelWidth, panelHeight, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            drawForeground(graphics, mouseX, mouseY);
        }

        @Override
        public void onClose() {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            } else {
                super.onClose();
            }
        }

        protected NeonButton add(Component label, Button.OnPress press, int x, int y, int w, int h, int color) {
            return addRenderableWidget(new NeonButton(left + x, top + y, w, h, label, press, color));
        }
    }

    /** The radio station's operating system: servers, player, queue, announcements, emergency, mixer. */
    public static final class ComputerScreen extends ChainScreen {
        private static final int ROWS = 10;
        private static final int QUEUE_ROWS = 6;

        private final BroadcastEngine mirror = new BroadcastEngine();
        private ComputerStatePacket state;
        private List<LibraryPacket.ServerInfo> servers = List.of();
        private int selectedServer;
        private int scroll;
        private int selectedTrack = -1;
        private int refreshCounter;
        private EditBox announcementBox;
        private MixSlider musicSlider;
        private MixSlider announcementSlider;
        private MixSlider masterSlider;
        private boolean slidersSynced;

        public ComputerScreen(ChainMenu menu, Inventory inventory, Component title) {
            super(menu, title, 380, 226);
        }

        private void action(ComputerActionPacket.Action action, int a, String text) {
            sendToServer(new ComputerActionPacket(menu.pos(), action, a, text));
        }

        @Override
        protected void buildWidgets() {
            for (int i = 0; i < 3; i++) {
                final int index = i;
                add(Component.literal("S" + (i + 1)), b -> {
                    selectedServer = index;
                    scroll = 0;
                    selectedTrack = -1;
                }, 8 + i * 41, 36, 38, 12, Ui.CYAN);
            }
            add(Component.literal("\u25B2"), b -> scroll = Math.max(0, scroll - ROWS), 8, 154, 58, 12, Ui.CYAN);
            add(Component.literal("\u25BC"), b -> scroll += ROWS, 70, 154, 58, 12, Ui.CYAN);
            add(Component.translatable("gui.mcradio.v4.add_track"), b -> {
                String id = selectedTrackId();
                if (id != null) {
                    action(ComputerActionPacket.Action.ADD_TRACK, 0, id);
                }
            }, 8, 168, 120, 14, Ui.GREEN);
            add(Component.translatable("gui.mcradio.v4.add_voice"), b -> {
                String id = selectedTrackId();
                if (id != null) {
                    action(ComputerActionPacket.Action.ADD_VOICE, 0, id);
                }
            }, 8, 184, 120, 14, Ui.AMBER);

            add(Component.literal("-10"), b -> action(ComputerActionPacket.Action.SEEK_BY, -10, ""), 136, 70, 36, 14, Ui.CYAN);
            add(Component.translatable("gui.mcradio.v4.play"), b -> action(ComputerActionPacket.Action.PLAY, 0, ""), 176, 70, 36, 14, Ui.GREEN);
            add(Component.translatable("gui.mcradio.v4.pause"), b -> action(ComputerActionPacket.Action.PAUSE, 0, ""), 216, 70, 36, 14, Ui.AMBER);
            add(Component.translatable("gui.mcradio.v4.stop"), b -> action(ComputerActionPacket.Action.STOP, 0, ""), 256, 70, 36, 14, Ui.RED);
            add(Component.literal("+10"), b -> action(ComputerActionPacket.Action.SEEK_BY, 10, ""), 296, 70, 36, 14, Ui.CYAN);
            add(Component.translatable("gui.mcradio.v4.next"), b -> action(ComputerActionPacket.Action.SKIP, 0, ""), 336, 70, 36, 14, Ui.CYAN);

            add(Component.translatable("gui.mcradio.v4.loop"), b -> action(ComputerActionPacket.Action.LOOP, 0, ""), 292, 86, 40, 12, Ui.PINK);
            add(Component.translatable("gui.mcradio.v4.clear"), b -> action(ComputerActionPacket.Action.CLEAR, 0, ""), 334, 86, 38, 12, Ui.PINK);

            announcementBox = new EditBox(font, left + 136, top + 164, 236, 14, Component.translatable("gui.mcradio.v4.announcement"));
            announcementBox.setMaxLength(120);
            announcementBox.setHint(Component.translatable("gui.mcradio.v4.announcement_hint"));
            addRenderableWidget(announcementBox);
            add(Component.translatable("gui.mcradio.v4.queue_text"), b -> {
                action(ComputerActionPacket.Action.ADD_TEXT, 0, announcementBox.getValue());
                announcementBox.setValue("");
            }, 136, 182, 76, 14, Ui.AMBER);
            add(Component.translatable("gui.mcradio.v4.emergency"), b -> {
                action(ComputerActionPacket.Action.EMERGENCY_TEXT, 0, announcementBox.getValue());
                announcementBox.setValue("");
            }, 216, 182, 156, 14, Ui.RED);

            int music = state == null ? 100 : state.music();
            int ann = state == null ? 100 : state.announcementVolume();
            int master = state == null ? 100 : state.master();
            musicSlider = addRenderableWidget(new MixSlider(left + 8, top + 204, 120, 14,
                    Component.translatable("gui.mcradio.v4.music_volume"), music, v -> sendMix()));
            announcementSlider = addRenderableWidget(new MixSlider(left + 132, top + 204, 120, 14,
                    Component.translatable("gui.mcradio.v4.announcement_volume"), ann, v -> sendMix()));
            masterSlider = addRenderableWidget(new MixSlider(left + 256, top + 204, 120, 14,
                    Component.translatable("gui.mcradio.v4.master_volume"), master, v -> sendMix()));
            action(ComputerActionPacket.Action.REQUEST_LIBRARY, 0, "");
        }

        private void sendMix() {
            int packed = musicSlider.percent() | (announcementSlider.percent() << 8) | (masterSlider.percent() << 16);
            action(ComputerActionPacket.Action.SET_MIX, packed, "");
        }

        private List<LibraryPacket.TrackInfo> tracks() {
            if (selectedServer < servers.size()) {
                return servers.get(selectedServer).tracks();
            }
            return List.of();
        }

        private String selectedTrackId() {
            List<LibraryPacket.TrackInfo> tracks = tracks();
            return selectedTrack >= 0 && selectedTrack < tracks.size() ? tracks.get(selectedTrack).id() : null;
        }

        void apply(ComputerStatePacket packet) {
            state = packet;
            BroadcastEngine.Item current = packet.currentId().isEmpty() ? null
                    : new BroadcastEngine.Item(packet.currentId(), Math.max(1, packet.currentTotal()), !packet.currentId().startsWith("M:"));
            mirror.applySnapshot(new BroadcastEngine.Snapshot(BroadcastEngine.State.values()[
                    Math.max(0, Math.min(BroadcastEngine.State.values().length - 1, packet.state()))],
                    current, packet.positionTicks(), packet.queue().size(), packet.duckPercent() / 100.0F, packet.emergency()));
            if (!slidersSynced && musicSlider != null) {
                slidersSynced = true;
                musicSlider.set(packet.music());
                announcementSlider.set(packet.announcementVolume());
                masterSlider.set(packet.master());
            }
        }

        void applyLibrary(LibraryPacket packet) {
            servers = packet.servers();
            if (selectedServer >= servers.size()) {
                selectedServer = 0;
            }
            int max = Math.max(0, tracks().size() - ROWS);
            scroll = Math.min(scroll, max);
        }

        @Override
        public void tick() {
            super.tick();
            mirror.clientTick();
            if (++refreshCounter >= 100) {
                refreshCounter = 0;
                action(ComputerActionPacket.Action.REQUEST_LIBRARY, 0, "");
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (mouseX >= left + 8 && mouseX <= left + 128 && mouseY >= top + 52 && mouseY <= top + 152) {
                int max = Math.max(0, tracks().size() - ROWS);
                scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            int mx = (int) mouseX - left;
            int my = (int) mouseY - top;
            if (mx >= 8 && mx < 128 && my >= 52 && my < 152) {
                int row = (my - 52) / 10;
                int index = scroll + row;
                if (index < tracks().size()) {
                    selectedTrack = index;
                    return true;
                }
            }
            if (mx >= 136 && mx < 372 && my >= 48 && my < 58 && state != null && state.currentTotal() > 0) {
                int second = (int) ((mx - 136) / 236.0 * state.currentTotal());
                action(ComputerActionPacket.Action.SEEK_TO, second, "");
                return true;
            }
            if (mx >= 136 && mx < 372 && my >= 100 && my < 100 + QUEUE_ROWS * 10 && state != null) {
                int index = (my - 100) / 10;
                if (index < state.queue().size()) {
                    action(ComputerActionPacket.Action.REMOVE, index, "");
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void drawForeground(GuiGraphics g, int mouseX, int mouseY) {
            // ---- status line
            boolean ok = state != null && "ok".equals(state.problem());
            Ui.led(g, left + 8, top + 26, ok, Ui.GREEN);
            Component status;
            if (state == null) {
                status = Component.translatable("gui.mcradio.v4.connecting");
            } else if (ok) {
                status = Component.translatable("gui.mcradio.v4.status_ok", state.frequency(), state.tier(), state.servers());
            } else {
                status = Component.translatable("chain.problem." + state.problem());
            }
            g.drawString(font, status, left + 20, top + 26, ok ? Ui.GREEN : Ui.RED, false);

            // ---- servers + library
            for (int i = 0; i < 3; i++) {
                int x = left + 8 + i * 41;
                g.fill(x, top + 48, x + 38, top + 50, i == selectedServer && i < servers.size() ? Ui.CYAN : 0xFF000000);
            }
            Ui.inset(g, left + 8, top + 52, 120, 100);
            List<LibraryPacket.TrackInfo> tracks = tracks();
            for (int row = 0; row < ROWS; row++) {
                int index = scroll + row;
                if (index >= tracks.size()) {
                    break;
                }
                LibraryPacket.TrackInfo track = tracks.get(index);
                int y = top + 53 + row * 10;
                if (index == selectedTrack) {
                    g.fill(left + 9, y - 1, left + 127, y + 9, 0xFF004046);
                }
                String name = track.id();
                int slash = name.lastIndexOf('/');
                if (slash >= 0) {
                    name = name.substring(slash + 1);
                }
                String time = track.seconds() > 0 ? BroadcastEngine.fmt(track.seconds()) : "--:--";
                g.drawString(font, Ui.clip(font, name, 82), left + 11, y, index == selectedTrack ? Ui.CYAN : Ui.TEXT, false);
                g.drawString(font, time, left + 125 - font.width(time), y, Ui.MUTED, false);
            }
            if (servers.isEmpty()) {
                g.drawString(font, Component.translatable("gui.mcradio.v4.no_servers"), left + 12, top + 56, Ui.MUTED, false);
            }

            // ---- now playing
            BroadcastEngine.Snapshot snapshot = mirror.snapshot();
            String title = state == null || state.currentId().isEmpty() ? "-" : Ui.label(state.currentId());
            boolean emergency = state != null && state.emergency();
            g.drawString(font, Ui.clip(font, title, 232), left + 136, top + 36, emergency ? Ui.RED : Ui.CYAN, false);
            Ui.inset(g, left + 136, top + 48, 236, 10);
            int fill = (int) (232 * mirror.clientProgress());
            g.fill(left + 138, top + 50, left + 138 + fill, top + 56, emergency ? Ui.RED : Ui.CYAN);
            g.drawString(font, mirror.clientProgressLabel(), left + 136, top + 60, Ui.TEXT, false);
            if (state != null) {
                Component mode = Component.translatable("gui.mcradio.v4.state." + BroadcastEngine.State.values()[
                        Math.max(0, Math.min(2, state.state()))].name().toLowerCase(Locale.ROOT));
                g.drawString(font, mode, left + 372 - font.width(mode), top + 60, Ui.MUTED, false);
            }

            // ---- queue
            int queueSize = state == null ? 0 : state.queue().size();
            g.drawString(font, Component.translatable("gui.mcradio.v4.queue", queueSize,
                    state == null ? 0 : state.announcements()), left + 136, top + 88,
                    state != null && state.loop() ? Ui.PINK : Ui.MUTED, false);
            Ui.inset(g, left + 136, top + 100, 236, QUEUE_ROWS * 10 + 2);
            for (int row = 0; row < QUEUE_ROWS && state != null && row < state.queue().size(); row++) {
                String[] parts = state.queue().get(row).split("\u0001");
                int seconds = parts.length > 1 ? parseInt(parts[1]) : 0;
                g.drawString(font, (row + 1) + ". " + Ui.clip(font, Ui.label(parts[0]), 190), left + 139, top + 102 + row * 10, Ui.TEXT, false);
                String time = BroadcastEngine.fmt(seconds);
                g.drawString(font, time, left + 352 - font.width(time) + 8, top + 102 + row * 10, Ui.MUTED, false);
                g.drawString(font, "x", left + 366, top + 102 + row * 10, Ui.RED, false);
            }
            if (state != null && state.duckPercent() > 0) {
                g.drawString(font, Component.translatable("gui.mcradio.v4.ducking", state.duckPercent() * 70 / 100),
                        left + 136, top + 198, Ui.AMBER, false);
            }
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /** Frequency modulator GUI: frequency, master password, chain status. */
    public static final class TranslatorScreen extends ChainScreen {
        private EditBox frequencyBox;
        private EditBox passwordBox;

        public TranslatorScreen(ChainMenu menu, Inventory inventory, Component title) {
            super(menu, title, 260, 170);
        }

        private TranslatorBE be() {
            Level level = Minecraft.getInstance().level;
            return level != null && level.getBlockEntity(menu.pos()) instanceof TranslatorBE t ? t : null;
        }

        @Override
        protected void buildWidgets() {
            TranslatorBE translator = be();
            frequencyBox = new EditBox(font, left + 110, top + 34, 60, 16, Component.translatable("gui.mcradio.frequency"));
            frequencyBox.setMaxLength(4);
            frequencyBox.setFilter(value -> value.matches("\\d{0,4}"));
            frequencyBox.setValue(Integer.toString(translator == null ? McRadio.RadioPhysics.DEFAULT_FREQUENCY : translator.frequency()));
            addRenderableWidget(frequencyBox);

            passwordBox = new EditBox(font, left + 110, top + 58, 130, 16, Component.translatable("gui.mcradio.v4.password"));
            passwordBox.setMaxLength(24);
            passwordBox.setFormatter((text, offset) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(text.length()),
                    net.minecraft.network.chat.Style.EMPTY));
            passwordBox.setHint(Component.translatable("gui.mcradio.v4.password_hint"));
            addRenderableWidget(passwordBox);

            add(Component.translatable("gui.mcradio.apply"), b -> send(), 12, 84, 236, 18, Ui.CYAN);
        }

        private void send() {
            int frequency;
            try {
                frequency = McRadio.RadioPhysics.clampFrequency(Integer.parseInt(frequencyBox.getValue().trim()));
            } catch (NumberFormatException e) {
                frequency = McRadio.RadioPhysics.DEFAULT_FREQUENCY;
            }
            frequencyBox.setValue(Integer.toString(frequency));
            sendToServer(new TranslatorSetPacket(menu.pos(), frequency, passwordBox.getValue()));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                send();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        protected void drawForeground(GuiGraphics g, int mouseX, int mouseY) {
            g.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 38, Ui.TEXT, false);
            g.drawString(font, Component.translatable("gui.mcradio.v4.password"), left + 12, top + 62, Ui.TEXT, false);
            TranslatorBE translator = be();
            boolean ok = translator != null && translator.chainOk();
            Ui.led(g, left + 12, top + 112, ok, Ui.GREEN);
            Component status = translator == null ? Component.empty()
                    : ok ? Component.translatable("gui.mcradio.v4.translator_ok", translator.tier())
                    : Component.translatable("chain.problem." + translator.problem());
            g.drawString(font, status, left + 26, top + 112, ok ? Ui.GREEN : Ui.RED, false);

            boolean secure = SecureChannels.isSecure(translator == null ? 0 : translator.frequency());
            boolean locked = translator != null && translator.locked();
            Ui.led(g, left + 12, top + 128, secure && !locked, locked ? Ui.RED : Ui.CYAN);
            Component channel = !secure ? Component.translatable("gui.mcradio.v4.channel_open")
                    : locked ? Component.translatable("gui.mcradio.v4.channel_locked")
                    : Component.translatable("gui.mcradio.v4.channel_unlocked");
            g.drawString(font, channel, left + 26, top + 128, locked ? Ui.RED : secure ? Ui.CYAN : Ui.MUTED, false);
            g.drawString(font, Component.translatable("gui.mcradio.v4.secure_hint"), left + 12, top + 146, Ui.MUTED, false);
        }
    }
}
