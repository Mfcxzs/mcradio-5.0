package com.mcradio;

import com.mcradio.McRadio.StationBlockEntity.Mode;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraftforge.common.extensions.IForgeMenuType;
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
 * Mod entry point. Runs on both physical sides and therefore must never touch client-only classes.
 */
@Mod(McRadio.MOD_ID)
public final class McRadio {
    public static final String MOD_ID = "mcradio";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public McRadio() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // config/mc_radio/tracks/ is created on every side, so the folder always exists after the first launch.
        TrackLibrary.ensureFolder();

        ModRegistry.register(modBus);
        VoidChain.register(modBus);
        RadioNetwork.init();
        LOGGER.info("MC Radio initialised; track folder: {}", TrackLibrary.folder());
    }

    // ==========================================================================
    // Everything below is part of the mod; nested so the whole mod is one source file.
    // ==========================================================================

    /** Wire identifiers for the audio payload carried by {@code AudioChunkPacket}. */
    public static final class AudioCodec {
        /** Raw MPEG audio bytes, decoded on the client by JLayer. */
        public static final int MP3 = 0;
        /** 16 kHz, mono, G.711 mu-law (one byte per sample): used for live microphone audio. */
        public static final int ULAW_16K = 1;
        /** MPEG audio of a voice announcement; decoded on a second channel so music can be ducked under it. */
        public static final int MP3_ANNOUNCEMENT = 2;

        public static final int VOICE_SAMPLE_RATE = 16000;

        private AudioCodec() {
        }
    }


    /** G.711 mu-law companding used for the live microphone stream (8 bit per sample on the wire). */
    public static final class UlawCodec {
        private static final int BIAS = 0x84;
        private static final int CLIP = 32635;
        private static final short[] DECODE_TABLE = new short[256];

        static {
            for (int i = 0; i < 256; i++) {
                int value = ~i & 0xFF;
                int sign = value & 0x80;
                int exponent = (value >> 4) & 0x07;
                int mantissa = value & 0x0F;
                int sample = ((mantissa << 3) + BIAS) << exponent;
                sample -= BIAS;
                DECODE_TABLE[i] = (short) (sign != 0 ? -sample : sample);
            }
        }

        private UlawCodec() {
        }

        public static short decode(byte encoded) {
            return DECODE_TABLE[encoded & 0xFF];
        }

        public static byte encode(short pcm) {
            int sample = pcm;
            int sign = (sample >> 8) & 0x80;
            if (sign != 0) {
                sample = -sample;
            }
            if (sample > CLIP) {
                sample = CLIP;
            }
            sample += BIAS;
            int exponent = 7;
            for (int mask = 0x4000; (sample & mask) == 0 && exponent > 0; mask >>= 1) {
                exponent--;
            }
            int mantissa = (sample >> (exponent + 3)) & 0x0F;
            return (byte) ~(sign | (exponent << 4) | mantissa);
        }

        /** The encoded value for digital silence. */
        public static byte silence() {
            return (byte) 0xFF;
        }
    }


    /** Access to the {@code config/mc_radio/tracks/} folder. All lookups are sandboxed to that folder. */
    public static final class TrackLibrary {
        public static final int MAX_TRACKS = 512;
        public static final int MAX_NAME_LENGTH = 200;
        public static final long MAX_FILE_BYTES = 96L * 1024L * 1024L;

        private TrackLibrary() {
        }

        public static Path folder() {
            return FMLPaths.CONFIGDIR.get().resolve("mc_radio").resolve("tracks");
        }

        public static void ensureFolder() {
            try {
                Files.createDirectories(folder());
            } catch (IOException e) {
                McRadio.LOGGER.error("Could not create track folder {}", folder(), e);
            }
        }

        public static boolean isMp3Name(String name) {
            return name.toLowerCase(Locale.ROOT).endsWith(".mp3");
        }

        /** Sorted file names of all playable tracks. */
        public static List<String> list() {
            ensureFolder();
            try (Stream<Path> files = Files.list(folder())) {
                return files.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(TrackLibrary::isMp3Name)
                        .filter(name -> name.length() <= MAX_NAME_LENGTH)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .limit(MAX_TRACKS)
                        .toList();
            } catch (IOException e) {
                McRadio.LOGGER.warn("Could not list track folder {}", folder(), e);
                return List.of();
            }
        }

        /** Resolves a track name sent by a client to a real file, rejecting anything outside the folder. */
        public static Optional<Path> resolve(String name) {
            if (name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH || !isMp3Name(name)) {
                return Optional.empty();
            }
            if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0 || name.contains("..")) {
                return Optional.empty();
            }
            Path root = folder().toAbsolutePath().normalize();
            Path file = root.resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return Optional.empty();
            }
            return Optional.of(file);
        }
    }


    /**
     * A fully loaded MP3 file on the server. The server never decodes audio for playback; it only scans the
     * frame headers once to learn the average byte rate, so that raw MP3 bytes can be streamed in real time.
     */
    public static final class Mp3Source {
        private static final ExecutorService IO = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "mcradio-mp3-loader");
            thread.setDaemon(true);
            return thread;
        });

        private final byte[] data;
        private final int start;
        private final int end;
        private final double bytesPerSecond;

        private Mp3Source(byte[] data, int start, int end, double bytesPerSecond) {
            this.data = data;
            this.start = start;
            this.end = end;
            this.bytesPerSecond = bytesPerSecond;
        }

        public static CompletableFuture<Mp3Source> loadAsync(Path path) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return load(path);
                } catch (IOException | JavaLayerException e) {
                    throw new CompletionException(e);
                }
            }, IO);
        }

        static Mp3Source load(Path path) throws IOException, JavaLayerException {
            long size = Files.size(path);
            if (size > TrackLibrary.MAX_FILE_BYTES) {
                throw new IOException("Track is too large (" + size + " bytes): " + path.getFileName());
            }
            byte[] data = Files.readAllBytes(path);
            int start = id3v2Length(data);
            int end = data.length;
            if (end - 128 > start && data[end - 128] == 'T' && data[end - 127] == 'A' && data[end - 126] == 'G') {
                end -= 128;
            }
            if (end <= start) {
                throw new IOException("Track contains no audio data: " + path.getFileName());
            }

            double milliseconds = 0.0;
            int frames = 0;
            Bitstream bitstream = new Bitstream(new ByteArrayInputStream(data, start, end - start));
            try {
                while (true) {
                    Header header = bitstream.readFrame();
                    if (header == null) {
                        break;
                    }
                    milliseconds += header.ms_per_frame();
                    frames++;
                    bitstream.closeFrame();
                }
            } catch (JavaLayerException e) {
                if (frames == 0) {
                    throw e;
                }
            } finally {
                try {
                    bitstream.close();
                } catch (JavaLayerException ignored) {
                    // nothing to release
                }
            }
            if (frames == 0 || milliseconds <= 0.0) {
                throw new IOException("No MPEG audio frames found in " + path.getFileName());
            }
            double bytesPerSecond = (end - start) / (milliseconds / 1000.0);
            bytesPerSecond = Math.max(4000.0, Math.min(80000.0, bytesPerSecond));
            return new Mp3Source(data, start, end, bytesPerSecond);
        }

        private static int id3v2Length(byte[] data) {
            if (data.length > 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
                int size = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14) | ((data[8] & 0x7F) << 7) | (data[9] & 0x7F);
                boolean footer = (data[5] & 0x10) != 0;
                return Math.min(10 + size + (footer ? 10 : 0), data.length);
            }
            return 0;
        }

        public byte[] data() {
            return data;
        }

        /** Offset of the first audio byte (after any ID3v2 tag). */
        public int start() {
            return start;
        }

        /** Offset one past the last audio byte (before any ID3v1 tag). */
        public int end() {
            return end;
        }

        public double bytesPerSecond() {
            return bytesPerSecond;
        }
    }


    /** Range model of the radio system. All distances are in blocks. */
    public static final class RadioPhysics {
        public static final int MIN_FREQUENCY = 1;
        public static final int MAX_FREQUENCY = 9999;
        public static final int DEFAULT_FREQUENCY = 101;

        public static final int STATION_BASE_RADIUS = 48;
        public static final int REPEATER_BASE_RADIUS = 96;
        public static final int RADIUS_PER_TRANSMITTER = 32;
        public static final int MAX_TRANSMITTERS = 8;

        /** Players within this distance of a receiver that has a signal are sent the audio stream. */
        public static final int LISTEN_RANGE = 64;

        /** Direct range between two walkie-talkies. Further than that, a repeater has to relay the transmission. */
        public static final int WALKIE_RANGE = 300;
        /** Radius of an active Signal Jammer. */
        public static final int JAMMER_RADIUS = 64;
        /** A Radio Scanner overhears relayed traffic of repeaters closer than this. */
        public static final int SCANNER_RANGE = 32;
        /** A computer can be linked to a distributor at most this far away. */
        public static final int COMPUTER_LINK_RANGE = 64;

        /** Above this height the radio signal is not attenuated by depth. */
        public static final int SURFACE_Y = 60;
        /** At or below this height (deepslate) there is complete radio silence: only white noise. */
        public static final int SILENCE_Y = 0;
        /** Longest cable network that is followed when looking for linked relays. */
        public static final int MAX_CABLES = 512;
        private static final int MAX_SHIELD_SAMPLES = 192;

        private RadioPhysics() {
        }

        /**
         * How much of a surface signal survives at height {@code y}: 1 at {@link #SURFACE_Y} and above, 0 at
         * {@link #SILENCE_Y} and below, a smooth S-curve in between.
         */
        public static double depthFactor(int y) {
            if (y >= SURFACE_Y) {
                return 1.0;
            }
            if (y <= SILENCE_Y) {
                return 0.0;
            }
            double t = (double) (y - SILENCE_Y) / (SURFACE_Y - SILENCE_Y);
            return t * t * (3.0 - 2.0 * t);
        }

        /** Blocks that shield radio waves: obsidian and iron blocks. */
        public static boolean isShield(BlockState state) {
            return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
                    || state.is(Blocks.IRON_BLOCK) || state.is(Blocks.RAW_IRON_BLOCK);
        }

        /** True if a shielding block lies on the straight line between the two positions. */
        public static boolean shielded(Level level, BlockPos from, BlockPos to) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double dz = to.getZ() - from.getZ();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 3.0) {
                return false;
            }
            int steps = (int) Math.min(MAX_SHIELD_SAMPLES, Math.ceil(length));
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int i = 1; i < steps; i++) {
                double t = (double) i / steps;
                cursor.set(Mth.floor(from.getX() + 0.5 + dx * t), Mth.floor(from.getY() + 0.5 + dy * t),
                        Mth.floor(from.getZ() + 0.5 + dz * t));
                if (level.isLoaded(cursor) && isShield(level.getBlockState(cursor))) {
                    return true;
                }
            }
            return false;
        }

        /** True if a shielding block is directly above {@code pos} (blocks signals that come from the sky). */
        public static boolean roofed(Level level, BlockPos pos) {
            int top = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + 48);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = pos.getY() + 1; y <= top; y++) {
                cursor.set(pos.getX(), y, pos.getZ());
                if (level.isLoaded(cursor) && isShield(level.getBlockState(cursor))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Signal quality in 0..1 of an emitter at {@code emitter} (reach {@code radius} blocks) as received at
         * {@code at}, or a negative number if {@code at} is out of reach. Quality falls off near the edge of the
         * radius, with depth below {@link #SURFACE_Y} (unless the emitter is a cable-fed relay) and is zero behind
         * obsidian or iron blocks.
         */
        public static double quality(Level level, BlockPos emitter, double radius, boolean wired, BlockPos at) {
            double distance = Math.sqrt(emitter.distSqr(at));
            if (distance > radius) {
                return -1.0;
            }
            double quality = Mth.clamp((1.0 - distance / radius) * 2.5, 0.0, 1.0);
            if (!wired) {
                quality *= depthFactor(at.getY());
            }
            if (quality > 0.0 && shielded(level, emitter, at)) {
                quality = 0.0;
            }
            return quality;
        }

        public static int clampFrequency(int frequency) {
            return Math.max(MIN_FREQUENCY, Math.min(MAX_FREQUENCY, frequency));
        }

        public static int clampVolume(int percent) {
            return Math.max(0, Math.min(100, percent));
        }

        /**
         * Number of Signal Transmitters that are connected to {@code origin} through a chain of face-adjacent
         * transmitter blocks (antenna array). Capped at {@link #MAX_TRANSMITTERS}.
         */
        public static int transmitterCount(Level level, BlockPos origin) {
            Set<BlockPos> visited = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            visited.add(origin);
            queue.add(origin);
            int count = 0;
            while (!queue.isEmpty() && count < MAX_TRANSMITTERS) {
                BlockPos current = queue.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos neighbour = current.relative(direction);
                    if (!visited.add(neighbour)) {
                        continue;
                    }
                    if (level.isLoaded(neighbour) && level.getBlockState(neighbour).getBlock() instanceof TransmitterBlock) {
                        count++;
                        queue.add(neighbour);
                        if (count >= MAX_TRANSMITTERS) {
                            break;
                        }
                    }
                }
            }
            return count;
        }

        /** Antenna reach: tiers 50/100/200 for chain stations; -30% during thunderstorms. */
        public static int stationRadius(Level level, BlockPos pos) {
            int chain = VoidChain.chainRadius(level, pos);
            int radius = chain > 0 ? chain : STATION_BASE_RADIUS + RADIUS_PER_TRANSMITTER * transmitterCount(level, pos);
            return VoidChain.thunder(level, radius);
        }

        /** A repeater extends the signal by 50 to 200 blocks (more booster transmitters, more reach). */
        public static int repeaterRadius(Level level, BlockPos pos) {
            int radius = Math.max(50, Math.min(200, 50 + 25 * transmitterCount(level, pos)));
            return VoidChain.thunder(level, radius);
        }
    }


    /**
     * The eerie number stations (Номерные радиостанции). Each one occupies a fixed frequency that every
     * receiver can tune to, is global (no emitter block needed), and endlessly sends a Morse-coded cipher. The
     * cipher and the location it leads to are derived from the world seed. A Radio Decoder turns the cipher
     * into the coordinates of an abandoned bunker or a crashed satellite.
     */
    public static final class NumberStations {
        public enum SiteKind { BUNKER, SATELLITE }

        private static final int[] FREQUENCIES = {313, 666, 1947, 2077};
        private static final long KEY_BASE = Long.MIN_VALUE;

        private NumberStations() {
        }

        public static int[] frequencies() {
            return FREQUENCIES.clone();
        }

        public static boolean isNumberFrequency(int frequency) {
            for (int f : FREQUENCIES) {
                if (f == frequency) {
                    return true;
                }
            }
            return false;
        }

        /** Stream key of a number station; lies outside the range of packed block positions. */
        public static long key(int frequency) {
            return KEY_BASE + frequency;
        }

        public static boolean isKey(long key) {
            return key < KEY_BASE + 100_000L && key >= KEY_BASE;
        }

        public static int frequencyOf(long key) {
            return (int) (key - KEY_BASE);
        }

        public static SiteKind kind(int frequency) {
            return frequency == 666 || frequency == 2077 ? SiteKind.SATELLITE : SiteKind.BUNKER;
        }

        private static Random random(ServerLevel level, int frequency) {
            long seed = level.getServer().overworld().getSeed();
            return new Random(seed * 0x9E3779B97F4A7C15L + frequency * 0xC2B2AE3D27D4EB4FL);
        }

        /** The cipher the station transmits: ten digits, written as two groups of five. */
        public static String code(ServerLevel level, int frequency) {
            Random random = random(level, frequency);
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i == 5) {
                    code.append(' ');
                }
                code.append(random.nextInt(10));
            }
            return code.toString();
        }

        /** Overworld X/Z of the site this station points to (Y is chosen when the site is built). */
        public static BlockPos site(ServerLevel level, int frequency) {
            Random random = random(level, frequency + 7919);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 1200.0 + random.nextDouble() * 2300.0;
            return new BlockPos((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
        }
    }


    /**
     * Builds the places number stations lead to. The site is generated the first time a player decodes the
     * matching cipher (the chunk is loaded on demand) and never again, so loot cannot be farmed.
     */
    public static final class NumberSites {
        private static final String DATA_NAME = "mcradio_number_sites";

        private NumberSites() {
        }

        /** Remembers which frequencies already had their site built. */
        public static final class SiteData extends SavedData {
            private final Set<Long> built = new HashSet<>();

            public static SiteData load(CompoundTag tag, HolderLookup.Provider registries) {
                SiteData data = new SiteData();
                for (Tag entry : tag.getList("Built", Tag.TAG_LONG)) {
                    if (entry instanceof LongTag value) {
                        data.built.add(value.getAsLong());
                    }
                }
                return data;
            }

            @Override
            public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
                ListTag list = new ListTag();
                for (long frequency : built) {
                    list.add(LongTag.valueOf(frequency));
                }
                tag.put("Built", list);
                return tag;
            }
        }

        /** Reveals (and on first use builds) the site of {@code frequency} for the player and notes it on the decoder. */
        public static void reveal(ServerPlayer player, ItemStack decoder, int frequency) {
            ServerLevel overworld = player.server.overworld();
            BlockPos site = NumberStations.site(overworld, frequency);
            NumberStations.SiteKind kind = NumberStations.kind(frequency);

            SiteData data = overworld.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(SiteData::new, SiteData::load, null), DATA_NAME);
            if (data.built.add((long) frequency)) {
                overworld.getChunk(site.getX() >> 4, site.getZ() >> 4);
                if (kind == NumberStations.SiteKind.BUNKER) {
                    buildBunker(overworld, site, frequency);
                } else {
                    buildSatellite(overworld, site);
                }
                data.setDirty();
            }
            DecoderItem.addSite(decoder, frequency, site.getX(), site.getZ(), kind);
            player.displayClientMessage(Component.translatable("message.mcradio.decoded."
                    + kind.name().toLowerCase(), site.getX(), site.getZ()), false);
        }

        private static void set(ServerLevel level, BlockPos pos, BlockState state) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        set(level, new BlockPos(x, y, z), state);
                    }
                }
            }
        }

        /** A sealed deepslate bunker with a ladder shaft, a lamp, a tuned receiver and a loot chest. */
        private static void buildBunker(ServerLevel level, BlockPos site, int frequency) {
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, site.getX(), site.getZ());
            int floor = Math.max(level.getMinBuildHeight() + 6, surface - 14);
            int x = site.getX();
            int z = site.getZ();
            BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            fill(level, x - 4, floor - 1, z - 4, x + 4, floor + 4, z + 4, wall);
            fill(level, x - 3, floor, z - 3, x + 3, floor + 3, z + 3, air);
            fill(level, x - 3, floor - 1, z - 3, x + 3, floor - 1, z + 3, Blocks.POLISHED_BLACKSTONE.defaultBlockState());

            // shaft: air column with a wall behind it, ladder on the wall, hatch on top
            int shaftZ = z + 4;
            for (int y = floor + 4; y <= surface; y++) {
                set(level, new BlockPos(x, y, shaftZ), air);
                set(level, new BlockPos(x, y, shaftZ + 1), wall);
                set(level, new BlockPos(x, y, shaftZ), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
            }
            set(level, new BlockPos(x, floor + 4, shaftZ), air);
            set(level, new BlockPos(x, floor + 4, z + 3), air);
            set(level, new BlockPos(x, floor + 3, z + 4), air);
            set(level, new BlockPos(x, surface, shaftZ), Blocks.SPRUCE_TRAPDOOR.defaultBlockState());

            set(level, new BlockPos(x - 3, floor, z - 3), Blocks.SOUL_LANTERN.defaultBlockState());
            set(level, new BlockPos(x + 3, floor, z - 3), Blocks.SOUL_LANTERN.defaultBlockState());

            BlockPos table = new BlockPos(x - 2, floor, z - 2);
            set(level, table, Blocks.IRON_BLOCK.defaultBlockState());
            BlockPos radio = table.above();
            set(level, radio, ModRegistry.RECEIVER_BLOCK.get().defaultBlockState());
            BlockEntity receiver = level.getBlockEntity(radio);
            if (receiver instanceof ReceiverBlockEntity radioEntity) {
                radioEntity.configure(level, frequency, 60);
            }

            BlockPos chest = new BlockPos(x + 2, floor, z - 2);
            set(level, chest, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity container) {
                container.setItem(0, new ItemStack(Items.IRON_INGOT, 18));
                container.setItem(2, new ItemStack(Items.REDSTONE, 24));
                container.setItem(4, new ItemStack(Items.COPPER_INGOT, 16));
                container.setItem(6, new ItemStack(ModRegistry.CIRCUIT_BOARD_ITEM.get(), 4));
                container.setItem(8, new ItemStack(ModRegistry.FERRITE_COIL_ITEM.get(), 3));
                container.setItem(10, new ItemStack(ModRegistry.ANTENNA_ROD_ITEM.get(), 6));
                container.setItem(13, new ItemStack(ModRegistry.WALKIE_ITEM.get()));
                container.setItem(15, new ItemStack(Items.IRON_BLOCK, 3));
            }
        }

        /** A crater with the wreck of a satellite: iron plating, a lightning-rod antenna and a loot chest. */
        private static void buildSatellite(ServerLevel level, BlockPos site) {
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, site.getX(), site.getZ());
            int x = site.getX();
            int z = site.getZ();
            int bottom = surface - 3;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 5.0) {
                        continue;
                    }
                    int depth = (int) Math.round(3.0 * (1.0 - distance / 5.0));
                    for (int dy = 0; dy < 6; dy++) {
                        int y = surface - depth + dy;
                        set(level, new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState());
                    }
                    set(level, new BlockPos(x + dx, surface - depth - 1, z + dz),
                            distance < 3.0 ? Blocks.COAL_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
                }
            }
            BlockPos[] plating = {new BlockPos(x - 2, bottom + 1, z), new BlockPos(x - 2, bottom + 2, z),
                    new BlockPos(x + 2, bottom + 1, z + 1), new BlockPos(x, bottom + 1, z - 3)};
            for (BlockPos pos : plating) {
                set(level, pos, Blocks.IRON_BLOCK.defaultBlockState());
            }
            set(level, new BlockPos(x + 1, bottom + 1, z - 1), Blocks.LIGHTNING_ROD.defaultBlockState());
            set(level, new BlockPos(x + 1, bottom + 2, z - 1), Blocks.LIGHTNING_ROD.defaultBlockState());

            BlockPos chest = new BlockPos(x, bottom + 1, z);
            set(level, chest, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity container) {
                container.setItem(0, new ItemStack(ModRegistry.SATELLITE_CORE_ITEM.get()));
                container.setItem(2, new ItemStack(ModRegistry.CIRCUIT_BOARD_ITEM.get(), 6));
                container.setItem(4, new ItemStack(ModRegistry.FERRITE_COIL_ITEM.get(), 4));
                container.setItem(6, new ItemStack(Items.GOLD_INGOT, 6));
                container.setItem(8, new ItemStack(Items.IRON_BLOCK, 2));
                container.setItem(10, new ItemStack(ModRegistry.SCANNER_ITEM.get()));
            }
        }
    }


    /** Text-chat sabotage of the Signal Jammer: letters are replaced by jumping obfuscated symbols (§k). */
    public static final class ChatJam {
        private static final Random RANDOM = new Random();

        private ChatJam() {
        }

        /**
         * Returns {@code text} with each character obfuscated with probability {@code jam}
         * (the jam level in 0..1); consecutive scrambled characters share one formatting code.
         */
        public static String scramble(String text, double jam) {
            if (jam <= 0.02) {
                return text;
            }
            double chance = Math.min(1.0, 0.25 + jam * 0.75);
            StringBuilder out = new StringBuilder();
            boolean scrambled = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean hit = !Character.isWhitespace(c) && RANDOM.nextDouble() < chance;
                if (hit != scrambled) {
                    out.append(hit ? "\u00a7k" : "\u00a7r");
                    scrambled = hit;
                }
                out.append(c);
            }
            if (scrambled) {
                out.append("\u00a7r");
            }
            return out.toString();
        }
    }


    /** The playback devices that share {@link ReceiverBlockEntity}: radio, industrial horn, wall speaker. */
    public static enum ReceiverVariant {
        /** Tabletop radio: clean sound, 32 blocks. */
        RADIO("radio_stream", false),
        /** Industrial horn loudspeaker: loud and distorted, 64 blocks. */
        HORN("radio_stream_horn", true),
        /** Compact wall speaker: clean sound, 16 blocks. */
        SPEAKER("radio_stream_speaker", false);

        private final String soundName;
        private final boolean distorted;

        ReceiverVariant(String soundName, boolean distorted) {
            this.soundName = soundName;
            this.distorted = distorted;
        }

        /** Name of the sound event in sounds.json (it defines the audible range). */
        public String soundName() {
            return soundName;
        }

        public boolean distorted() {
            return distorted;
        }
    }


    /** Serverbound: the open station GUI asks for the list of MP3 files in {@code config/mc_radio/tracks/}. */
    public static record TrackListRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackListRequestPacket> CODEC =
                StreamCodec.unit(new TrackListRequestPacket());

        public static void handle(TrackListRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender != null && sender.containerMenu instanceof RadioDeviceMenu) {
                    RadioNetwork.sendToPlayer(sender, new TrackListPacket(TrackLibrary.list()));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: file names available on the server. */
    public static record TrackListPacket(List<String> tracks) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackListPacket> CODEC =
                StreamCodec.ofMember(TrackListPacket::write, TrackListPacket::new);

        public TrackListPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<String> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), TrackLibrary.MAX_TRACKS);
            List<String> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(buf.readUtf(TrackLibrary.MAX_NAME_LENGTH));
            }
            return List.copyOf(result);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(tracks.size());
            for (String track : tracks) {
                buf.writeUtf(track, TrackLibrary.MAX_NAME_LENGTH);
            }
        }

        public static void handle(TrackListPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.trackList(message.tracks()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Serverbound: every GUI action of the configurable devices. The server only honours it while the sender
     * has the matching device menu open (which itself enforces distance and block validity). Distributor
     * actions sent from a computer GUI are applied to the distributor the computer is linked to.
     */
    public static record ConfigPacket(BlockPos pos, Action action, int a, int b, String text) {
        public static final int MAX_TEXT = 128;

        public enum Action {
            /** a = frequency, b = mode ordinal */
            STATION_APPLY,
            /** text = track file name */
            STATION_BROADCAST,
            STATION_STOP,
            /** text = message to display to players near tuned receivers */
            STATION_TEXT,
            /** a = frequency, b = volume percent */
            RECEIVER_SET,
            /** a = input frequency, b = output frequency */
            REPEATER_SET,
            /** text = password of a receiver for the secure channels */
            RECEIVER_PASSWORD,
            /** text = packed position (BlockPos#asLong) of the distributor to connect to */
            COMPUTER_LINK,
            COMPUTER_UNLINK,
            /** a = radius, b = switched on (1) or off (0) */
            JAMMER_SET
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPacket> CODEC =
                StreamCodec.ofMember(ConfigPacket::write, ConfigPacket::new);

        public ConfigPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Action.class), buf.readVarInt(), buf.readVarInt(), buf.readUtf(MAX_TEXT));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeEnum(action);
            buf.writeVarInt(a);
            buf.writeVarInt(b);
            buf.writeUtf(text, MAX_TEXT);
        }

        public static void handle(ConfigPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> process(sender, message));
            context.setPacketHandled(true);
        }

        private static void process(ServerPlayer sender, ConfigPacket message) {
            if (sender == null) {
                return;
            }
            if (!(sender.containerMenu instanceof RadioDeviceMenu menu) || !menu.pos().equals(message.pos())) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(message.pos());

            if (blockEntity instanceof ComputerBlockEntity computer
                    && (message.action() == Action.COMPUTER_LINK || message.action() == Action.COMPUTER_UNLINK)) {
                boolean changed;
                if (message.action() == Action.COMPUTER_LINK) {
                    changed = link(level, computer, message.text());
                } else {
                    computer.unlink(level);
                    changed = true;
                }
                if (changed) {
                    // Re-open the menu so the client receives the new link state.
                    sender.openMenu(computer, buf -> computer.menuData().write(buf));
                }
                return;
            }

            StationBlockEntity station = StationBlockEntity.find(level, message.pos());
            switch (message.action()) {
                case STATION_APPLY -> {
                    if (station != null) {
                        station.applySettings(level, RadioPhysics.clampFrequency(message.a()),
                                StationBlockEntity.Mode.of(message.b()));
                    }
                }
                case STATION_BROADCAST -> {
                    if (station != null && !station.startTrack(level, message.text())) {
                        sender.displayClientMessage(Component.translatable("message.mcradio.track_missing"), true);
                    }
                }
                case STATION_STOP -> {
                    if (station != null) {
                        station.stopBroadcast(level);
                    }
                }
                case STATION_TEXT -> {
                    if (station != null && !station.broadcastText(level, sender, message.text())) {
                        sender.displayClientMessage(Component.translatable("message.mcradio.text_not_live"), true);
                    }
                }
                case RECEIVER_SET -> {
                    if (blockEntity instanceof ReceiverBlockEntity receiver) {
                        receiver.configure(level, RadioPhysics.clampFrequency(message.a()),
                                RadioPhysics.clampVolume(message.b()));
                    }
                }
                case REPEATER_SET -> {
                    if (blockEntity instanceof RepeaterBlockEntity repeater) {
                        repeater.configure(level, RadioPhysics.clampFrequency(message.a()),
                                RadioPhysics.clampFrequency(message.b()));
                    }
                }
                case RECEIVER_PASSWORD -> {
                    if (blockEntity instanceof ReceiverBlockEntity receiver) {
                        receiver.setPassword(level, message.text());
                    }
                }
                case JAMMER_SET -> {
                    if (blockEntity instanceof JammerBlockEntity jammer) {
                        jammer.configure(level, message.a(), message.b() == 1);
                    }
                }
                case COMPUTER_LINK, COMPUTER_UNLINK -> {
                    // only meaningful for computers, handled above
                }
            }
        }

        private static boolean link(ServerLevel level, ComputerBlockEntity computer, String packedPosition) {
            try {
                return computer.link(level, BlockPos.of(Long.parseLong(packedPosition.trim())));
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }


    /** Serverbound: ~100 ms of microphone audio (16 kHz mono mu-law) for a station that is in live mode. */
    public static record LiveAudioPacket(BlockPos pos, byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, LiveAudioPacket> CODEC =
                StreamCodec.ofMember(LiveAudioPacket::write, LiveAudioPacket::new);

        public LiveAudioPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeByteArray(data);
        }

        public static void handle(LiveAudioPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || message.data().length == 0) {
                    return;
                }
                if (!(sender.containerMenu instanceof RadioDeviceMenu menu) || !menu.pos().equals(message.pos())) {
                    return;
                }
                ServerLevel level = sender.serverLevel();
                StationBlockEntity station = StationBlockEntity.find(level, message.pos());
                if (station != null) {
                    station.relayLive(level, message.data());
                }
            });
            context.setPacketHandled(true);
        }
    }


    /**
     * Clientbound: a slice of a station's audio stream. {@code station} is the packed position of the
     * originating Signal Broadcast Station and identifies the stream on the client.
     */
    public static record AudioChunkPacket(long station, int codec, byte[] data) {
        public static final int MAX_BYTES = 262144;

        public static final StreamCodec<RegistryFriendlyByteBuf, AudioChunkPacket> CODEC =
                StreamCodec.ofMember(AudioChunkPacket::write, AudioChunkPacket::new);

        public AudioChunkPacket(FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(station);
            buf.writeVarInt(codec);
            buf.writeByteArray(data);
        }

        public static void handle(AudioChunkPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.audioChunk(message.station(), message.codec(), message.data()));
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: the station stopped (or changed mode); drop the stream and its decoder. */
    public static record StreamEndPacket(long station) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StreamEndPacket> CODEC =
                StreamCodec.ofMember(StreamEndPacket::write, StreamEndPacket::new);

        public StreamEndPacket(FriendlyByteBuf buf) {
            this(buf.readLong());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(station);
        }

        public static void handle(StreamEndPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.streamEnd(message.station()));
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: a typed message from a station in live mode, shown in chat to players near tuned receivers. */
    public static record RadioTextPacket(int frequency, String sender, String text) {
        public static final StreamCodec<RegistryFriendlyByteBuf, RadioTextPacket> CODEC =
                StreamCodec.ofMember(RadioTextPacket::write, RadioTextPacket::new);

        public RadioTextPacket(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(64), buf.readUtf(ConfigPacket.MAX_TEXT));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(frequency);
            buf.writeUtf(sender, 64);
            buf.writeUtf(text, ConfigPacket.MAX_TEXT);
        }

        public static void handle(RadioTextPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.radioText(message.frequency(), message.sender(), message.text()));
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: set the frequency of the walkie-talkie or pocket player held in the given hand. */
    public static record WalkieConfigPacket(InteractionHand hand, int frequency) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WalkieConfigPacket> CODEC =
                StreamCodec.ofMember(WalkieConfigPacket::write, WalkieConfigPacket::new);

        public WalkieConfigPacket(FriendlyByteBuf buf) {
            this(buf.readEnum(InteractionHand.class), buf.readVarInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeEnum(hand);
            buf.writeVarInt(frequency);
        }

        public static void handle(WalkieConfigPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null) {
                    return;
                }
                ItemStack stack = sender.getItemInHand(message.hand());
                if (stack.getItem() instanceof WalkieTalkieItem || stack.getItem() instanceof PocketPlayerItem) {
                    WalkieTalkieItem.setFrequency(stack, RadioPhysics.clampFrequency(message.frequency()));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: ~100 ms of push-to-talk audio (16 kHz mono mu-law) from a player holding a walkie-talkie. */
    public static record WalkieVoicePacket(byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, WalkieVoicePacket> CODEC =
                StreamCodec.ofMember(WalkieVoicePacket::write, WalkieVoicePacket::new);

        public WalkieVoicePacket(FriendlyByteBuf buf) {
            this(buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeByteArray(data);
        }

        public static void handle(WalkieVoicePacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || message.data().length == 0) {
                    return;
                }
                int frequency = WalkieTalkieItem.heldFrequency(sender);
                if (frequency <= 0 || !RadioNetworkManager.allowVoice(sender, message.data().length)) {
                    return;
                }
                RadioNetworkManager.routeVoice(sender, frequency, WalkieTalkieItem.heldRange(sender), message.data());
            });
            context.setPacketHandled(true);
        }
    }


    /**
     * Clientbound: walkie-talkie voice from {@code speaker}. {@code mode} is 0 = direct, 1 = relayed through a
     * repeater, 2 = intercepted with a Radio Scanner. {@code quality} (0-100) is how clear the
     * signal is: lower values are mixed with static, 0 is pure white noise.
     */
    public static record VoiceChunkPacket(UUID speaker, String name, int frequency, int mode, int quality, byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, VoiceChunkPacket> CODEC =
                StreamCodec.ofMember(VoiceChunkPacket::write, VoiceChunkPacket::new);

        public VoiceChunkPacket(FriendlyByteBuf buf) {
            this(buf.readUUID(), buf.readUtf(48), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(speaker);
            buf.writeUtf(name, 48);
            buf.writeVarInt(frequency);
            buf.writeVarInt(mode);
            buf.writeVarInt(quality);
            buf.writeByteArray(data);
        }

        public static void handle(VoiceChunkPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.voice(message.speaker(), message.name(),
                    message.frequency(), message.mode(), message.quality(), message.data()));
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: the tablet asks for all radio devices of the player's dimension. */
    public static record MapRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, MapRequestPacket> CODEC =
                StreamCodec.unit(new MapRequestPacket());

        public static void handle(MapRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender != null && TabletItem.isHolding(sender) && RadioNetworkManager.allowMapRequest(sender)) {
                    RadioNetwork.sendToPlayer(sender, new MapDataPacket(
                            RadioNetworkManager.mapNodes(sender.serverLevel(), sender.blockPosition())));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: radio devices for the tablet map. */
    public static record MapDataPacket(List<Node> nodes) {
        public static final int KIND_DISTRIBUTOR = 0;
        public static final int KIND_REPEATER = 1;
        public static final int KIND_RECEIVER = 2;
        public static final int KIND_JAMMER = 3;
        public static final int MAX_NODES = 512;

        /**
         * One device. Distributor/receiver: a = frequency. Repeater: a = input, b = output frequency.
         * {@code radius} is the coverage radius in blocks (0 for receivers); {@code active} means on air /
         * relaying / receiving / powered.
         */
        public record Node(int kind, BlockPos pos, int a, int b, int radius, boolean active) {
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, MapDataPacket> CODEC =
                StreamCodec.ofMember(MapDataPacket::write, MapDataPacket::new);

        public MapDataPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<Node> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), MAX_NODES);
            List<Node> nodes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int kind = buf.readVarInt();
                BlockPos pos = buf.readBlockPos();
                int a = buf.readVarInt();
                int b = buf.readVarInt();
                int radius = buf.readVarInt();
                boolean active = buf.readBoolean();
                nodes.add(new Node(kind, pos, a, b, radius, active));
            }
            return List.copyOf(nodes);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(nodes.size());
            for (Node node : nodes) {
                buf.writeVarInt(node.kind());
                buf.writeBlockPos(node.pos());
                buf.writeVarInt(node.a());
                buf.writeVarInt(node.b());
                buf.writeVarInt(node.radius());
                buf.writeBoolean(node.active());
            }
        }

        public static void handle(MapDataPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.mapData(message.nodes()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Clientbound, once per second to a player holding a Pocket Player: which stream it picks up
     * ({@code source} 0 = nothing), how clear it is (0-100) and, for number stations, the Morse cipher.
     */
    public static record PocketStatePacket(long source, int quality, String cipher) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PocketStatePacket> CODEC =
                StreamCodec.ofMember(PocketStatePacket::write, PocketStatePacket::new);

        public PocketStatePacket(FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readUtf(32));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(source);
            buf.writeVarInt(quality);
            buf.writeUtf(cipher, 32);
        }

        public static void handle(PocketStatePacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.pocketState(message.source(), message.quality(), message.cipher()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Serverbound: the Radio Decoder GUI. Either the player types the cipher they heard on the given frequency,
     * or asks the decoder to capture it from a receiver (within 8 blocks) that is tuned to a number station.
     */
    public static record DecoderPacket(InteractionHand hand, int frequency, String cipher, boolean capture) {
        public static final StreamCodec<RegistryFriendlyByteBuf, DecoderPacket> CODEC =
                StreamCodec.ofMember(DecoderPacket::write, DecoderPacket::new);

        public DecoderPacket(FriendlyByteBuf buf) {
            this(buf.readEnum(InteractionHand.class), buf.readVarInt(), buf.readUtf(32), buf.readBoolean());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeEnum(hand);
            buf.writeVarInt(frequency);
            buf.writeUtf(cipher, 32);
            buf.writeBoolean(capture);
        }

        public static void handle(DecoderPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> process(sender, message));
            context.setPacketHandled(true);
        }

        private static String digits(String text) {
            StringBuilder out = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private static void process(ServerPlayer sender, DecoderPacket message) {
            if (sender == null) {
                return;
            }
            ItemStack decoder = sender.getItemInHand(message.hand());
            if (!(decoder.getItem() instanceof DecoderItem)) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            int frequency = RadioPhysics.clampFrequency(message.frequency());
            String cipher = digits(message.cipher());

            if (message.capture()) {
                ReceiverBlockEntity receiver = nearestNumberReceiver(level, sender.blockPosition());
                if (receiver == null) {
                    sender.displayClientMessage(Component.translatable("message.mcradio.decoder.no_receiver"), true);
                    return;
                }
                frequency = receiver.getFrequency();
                cipher = digits(NumberStations.code(level, frequency));
            }
            if (!NumberStations.isNumberFrequency(frequency)) {
                sender.displayClientMessage(Component.translatable("message.mcradio.decoder.no_station"), true);
                return;
            }
            if (!digits(NumberStations.code(level, frequency)).equals(cipher)) {
                sender.displayClientMessage(Component.translatable("message.mcradio.decoder.wrong_code"), true);
                return;
            }
            NumberSites.reveal(sender, decoder, frequency);
        }

        private static ReceiverBlockEntity nearestNumberReceiver(ServerLevel level, BlockPos around) {
            ReceiverBlockEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(around.offset(-8, -4, -8), around.offset(8, 4, 8))) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver
                        && receiver.isReceiving() && NumberStations.isNumberFrequency(receiver.getFrequency())) {
                    double distance = pos.distSqr(around);
                    if (distance < bestDistance) {
                        best = receiver;
                        bestDistance = distance;
                    }
                }
            }
            return best;
        }
    }


    /** Serverbound: a computer GUI asks for the distributors it could connect to. */
    public static record StationListRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, StationListRequestPacket> CODEC =
                StreamCodec.unit(new StationListRequestPacket());

        public static void handle(StationListRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || !(sender.containerMenu instanceof RadioDeviceMenu menu)) {
                    return;
                }
                ServerLevel level = sender.serverLevel();
                if (level.getBlockEntity(menu.pos()) instanceof ComputerBlockEntity) {
                    RadioNetwork.sendToPlayer(sender, new StationListPacket(RadioNetworkManager.stationsNear(level, menu.pos())));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: distributors a computer may link to. */
    public static record StationListPacket(List<Entry> entries) {
        public static final int MAX_ENTRIES = 64;

        public record Entry(BlockPos pos, int frequency, boolean onAir) {
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, StationListPacket> CODEC =
                StreamCodec.ofMember(StationListPacket::write, StationListPacket::new);

        public StationListPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<Entry> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BlockPos pos = buf.readBlockPos();
                int frequency = buf.readVarInt();
                boolean onAir = buf.readBoolean();
                entries.add(new Entry(pos, frequency, onAir));
            }
            return List.copyOf(entries);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (Entry entry : entries) {
                buf.writeBlockPos(entry.pos());
                buf.writeVarInt(entry.frequency());
                buf.writeBoolean(entry.onAir());
            }
        }

        public static void handle(StationListPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.stationList(message.entries()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Single registration point for every packet of the mod. Direction is declared explicitly
     * (clientbound / serverbound), and the handlers of clientbound packets only reach client code through
     * {@code ClientPacketHandlers}, so a dedicated server never loads client classes.
     */
    public static final class RadioNetwork {
        public static final int PROTOCOL_VERSION = 1;

        public static final SimpleChannel CHANNEL = ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "main"))
                .networkProtocolVersion(PROTOCOL_VERSION)
                .simpleChannel()
                .play()
                .clientbound()
                    .add(TrackListPacket.class, TrackListPacket.CODEC, TrackListPacket::handle)
                    .add(AudioChunkPacket.class, AudioChunkPacket.CODEC, AudioChunkPacket::handle)
                    .add(StreamEndPacket.class, StreamEndPacket.CODEC, StreamEndPacket::handle)
                    .add(RadioTextPacket.class, RadioTextPacket.CODEC, RadioTextPacket::handle)
                    .add(VoiceChunkPacket.class, VoiceChunkPacket.CODEC, VoiceChunkPacket::handle)
                    .add(MapDataPacket.class, MapDataPacket.CODEC, MapDataPacket::handle)
                    .add(StationListPacket.class, StationListPacket.CODEC, StationListPacket::handle)
                    .add(PocketStatePacket.class, PocketStatePacket.CODEC, PocketStatePacket::handle)
                .serverbound()
                    .add(TrackListRequestPacket.class, TrackListRequestPacket.CODEC, TrackListRequestPacket::handle)
                    .add(ConfigPacket.class, ConfigPacket.CODEC, ConfigPacket::handle)
                    .add(LiveAudioPacket.class, LiveAudioPacket.CODEC, LiveAudioPacket::handle)
                    .add(WalkieConfigPacket.class, WalkieConfigPacket.CODEC, WalkieConfigPacket::handle)
                    .add(WalkieVoicePacket.class, WalkieVoicePacket.CODEC, WalkieVoicePacket::handle)
                    .add(MapRequestPacket.class, MapRequestPacket.CODEC, MapRequestPacket::handle)
                    .add(DecoderPacket.class, DecoderPacket.CODEC, DecoderPacket::handle)
                    .add(StationListRequestPacket.class, StationListRequestPacket.CODEC, StationListRequestPacket::handle)
                .build();

        private RadioNetwork() {
        }

        /** Forces class initialisation (and therefore channel registration) during mod construction. */
        public static void init() {
            // intentionally empty: touching the class runs the static initialiser above
        }

        public static void sendToPlayer(ServerPlayer player, Object packet) {
            CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
        }

        public static void sendToServer(Object packet) {
            CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
        }
    }


    /**
     * Walkie-talkie (Рация). Right-click to set the frequency; hold the push-to-talk key (default V) while it is
     * in a hand to speak. Talks directly to other walkies within {@link RadioPhysics#WALKIE_RANGE} blocks; for
     * longer distances the speaker has to be in reach of a repeater.
     */
    public static class WalkieTalkieItem extends Item {
        private static final String FREQUENCY_KEY = "Frequency";

        public WalkieTalkieItem(Properties properties) {
            super(properties);
        }

        /** Direct reach of this device in blocks. */
        public int range() {
            return RadioPhysics.WALKIE_RANGE;
        }

        /** Direct reach of the walkie held by the player (main hand, else off hand); 0 if none is held. */
        public static int heldRange(Player player) {
            if (player.getMainHandItem().getItem() instanceof WalkieTalkieItem walkie) {
                return walkie.range();
            }
            if (player.getOffhandItem().getItem() instanceof WalkieTalkieItem walkie) {
                return walkie.range();
            }
            return 0;
        }

        public static int getFrequency(ItemStack stack) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return tag.contains(FREQUENCY_KEY) ? RadioPhysics.clampFrequency(tag.getInt(FREQUENCY_KEY))
                    : RadioPhysics.DEFAULT_FREQUENCY;
        }

        public static void setFrequency(ItemStack stack, int frequency) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(FREQUENCY_KEY, frequency));
        }

        /** Frequency of the walkie in the player's main hand, else off hand; 0 if none is held. */
        public static int heldFrequency(Player player) {
            ItemStack main = player.getMainHandItem();
            if (main.getItem() instanceof WalkieTalkieItem) {
                return getFrequency(main);
            }
            ItemStack off = player.getOffhandItem();
            if (off.getItem() instanceof WalkieTalkieItem) {
                return getFrequency(off);
            }
            return 0;
        }

        /** Frequency the player listens on: a held walkie first, otherwise any walkie in the inventory; 0 if none. */
        public static int tunedFrequency(Player player) {
            int held = heldFrequency(player);
            if (held > 0) {
                return held;
            }
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof WalkieTalkieItem) {
                    return getFrequency(stack);
                }
            }
            return 0;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openTuner(hand, getFrequency(stack), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
            super.inventoryTick(stack, level, entity, slotId, isSelected);
            if (level.isClientSide) {
                ClientHooks.walkieTick(stack, entity);
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.walkie_talkie.frequency", getFrequency(stack)));
            tooltip.add(Component.translatable("item.mcradio.walkie_talkie.range", range()));
        }
    }


    /**
     * Field Radio (Радио-рюкзак): a heavy backpack radio station. It works like a walkie-talkie (same push-to-talk
     * key and frequency dialog) but reaches {@link #RANGE} blocks directly; carrying it slows the player down.
     */
    public static class FieldRadioItem extends WalkieTalkieItem {
        public static final int RANGE = 800;

        public FieldRadioItem(Properties properties) {
            super(properties);
        }

        @Override
        public int range() {
            return RANGE;
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
            super.inventoryTick(stack, level, entity, slotId, isSelected);
            if (!level.isClientSide && entity instanceof ServerPlayer player && player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.field_radio.heavy"));
        }
    }


    /**
     * Pocket Player (Карманный плеер): while held it plays the tuned frequency straight into the player's ears
     * (stations, repeaters and number stations), and its needle shows the signal quality. Right-click to tune.
     */
    public static class PocketPlayerItem extends Item {
        public PocketPlayerItem(Properties properties) {
            super(properties);
        }

        /** Frequency of the pocket player in the main hand, else off hand; 0 if none is held. */
        public static int heldFrequency(Player player) {
            if (player.getMainHandItem().getItem() instanceof PocketPlayerItem) {
                return WalkieTalkieItem.getFrequency(player.getMainHandItem());
            }
            if (player.getOffhandItem().getItem() instanceof PocketPlayerItem) {
                return WalkieTalkieItem.getFrequency(player.getOffhandItem());
            }
            return 0;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openTuner(hand, WalkieTalkieItem.getFrequency(stack), false);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.walkie_talkie.frequency", WalkieTalkieItem.getFrequency(stack)));
            tooltip.add(Component.translatable("item.mcradio.pocket_player.tooltip"));
        }
    }


    /**
     * Radio Decoder (Дешифратор): turns the Morse cipher of a number station into the coordinates of an
     * abandoned bunker or a crashed satellite. Decoded sites are remembered on the item.
     */
    public static class DecoderItem extends Item {
        private static final String SITES_KEY = "Sites";

        public DecoderItem(Properties properties) {
            super(properties);
        }

        /** Remembered sites as "frequency|x|z|kind". */
        public static List<String> sites(ItemStack stack) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            List<String> sites = new ArrayList<>();
            for (Tag entry : tag.getList(SITES_KEY, Tag.TAG_STRING)) {
                sites.add(entry.getAsString());
            }
            return sites;
        }

        public static void addSite(ItemStack stack, int frequency, int x, int z, NumberStations.SiteKind kind) {
            String entry = frequency + "|" + x + "|" + z + "|" + kind.name();
            if (sites(stack).contains(entry)) {
                return;
            }
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                ListTag list = tag.getList(SITES_KEY, Tag.TAG_STRING);
                list.add(StringTag.valueOf(entry));
                tag.put(SITES_KEY, list);
            });
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openDecoder(hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.radio_decoder.tooltip"));
            tooltip.add(Component.translatable("item.mcradio.radio_decoder.known", sites(stack).size()));
        }
    }


    /**
     * Radio Scanner (Сканер): while held it overhears every walkie-talkie conversation that is relayed by a
     * repeater within {@link RadioPhysics#SCANNER_RANGE} blocks, whatever frequency it uses.
     */
    public static class ScannerItem extends Item {
        public ScannerItem(Properties properties) {
            super(properties);
        }

        public static boolean isHolding(Player player) {
            return player.getMainHandItem().getItem() instanceof ScannerItem
                    || player.getOffhandItem().getItem() instanceof ScannerItem;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.radio_scanner.tooltip", RadioPhysics.SCANNER_RANGE));
        }
    }


    /** Radio Tablet (Планшет): opens a map with all repeaters, distributors, receivers, jammers and their coverage. */
    public static class TabletItem extends Item {
        public TabletItem(Properties properties) {
            super(properties);
        }

        public static boolean isHolding(Player player) {
            return player.getMainHandItem().getItem() instanceof TabletItem
                    || player.getOffhandItem().getItem() instanceof TabletItem;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openTablet();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }


    /**
     * Slot-less menu shared by the station, repeater and receiver. It exists so the server can verify that
     * the player really has the device open (and is in reach) before accepting configuration packets, and so
     * the initial device state travels to the client in the open-menu packet.
     */
    public static class RadioDeviceMenu extends AbstractContainerMenu {
        private final DeviceData data;

        public RadioDeviceMenu(MenuType<?> type, int containerId, DeviceData data) {
            super(type, containerId);
            this.data = data;
        }

        public DeviceData data() {
            return data;
        }

        public BlockPos pos() {
            return data.pos();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            BlockPos pos = data.pos();
            return player.level().getBlockEntity(pos) instanceof RadioDevice
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }

        /**
         * Snapshot of a device at the moment its GUI opens.
         * Station: a = frequency, b = mode ordinal, c = radius, flag = broadcasting, text = track.
         * Receiver: a = frequency, b = volume percent, flag = has signal.
         * Repeater: a = input frequency, b = output frequency, c = radius, flag = relaying.
         */
        public record DeviceData(BlockPos pos, int a, int b, int c, boolean flag, String text) {
            public void write(FriendlyByteBuf buf) {
                buf.writeBlockPos(pos);
                buf.writeVarInt(a);
                buf.writeVarInt(b);
                buf.writeVarInt(c);
                buf.writeBoolean(flag);
                buf.writeUtf(text, 256);
            }

            public static DeviceData read(FriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                int a = buf.readVarInt();
                int b = buf.readVarInt();
                int c = buf.readVarInt();
                boolean flag = buf.readBoolean();
                String text = buf.readUtf(256);
                return new DeviceData(pos, a, b, c, flag, text);
            }
        }
    }


    /** Implemented by every block entity that opens a configuration GUI. */
    public static interface RadioDevice extends MenuProvider {
        RadioDeviceMenu.DeviceData menuData();
    }


    /**
     * Server-side radio graph. Block entities register themselves while loaded; once per game tick (driven by
     * whichever device ticks first) the manager
     * <ol>
     *   <li>every second, recomputes which distributors are on air, which repeaters catch a signal (over the air
     *       or through cables) and re-emit it, how clear the signal is at every receiver and pocket player
     *       (depth, shielding, distance, jammers), and tells receivers and nearby players about it;</li>
     *   <li>every tick, lets every on-air distributor push its audio.</li>
     * </ol>
     * It also routes walkie-talkie voice: a transmission is an emitter around the speaker, repeaters whose input
     * frequency matches re-emit it on their output frequency, and every tuned walkie inside any emitter's radius
     * hears it with the signal quality at its position.
     * Everything here runs on the server thread only.
     */
    public static final class RadioNetworkManager {
        public static final int RECOMPUTE_INTERVAL = 20;

        public static final int MODE_DIRECT = 0;
        public static final int MODE_RELAYED = 1;
        public static final int MODE_INTERCEPTED = 2;

        /** A repeater only catches a signal that is at least this clear. */
        private static final double CATCH_QUALITY = 0.12;
        private static final int MAX_MAP_NODES = 400;
        private static final int MAP_RANGE = 4096;
        private static final int VOICE_BYTES_PER_SECOND = 32_000;
        private static final int MAP_REQUEST_INTERVAL_TICKS = 10;

        public enum Kind { STATION, REPEATER, RECEIVER, JAMMER }

        /**
         * A source of signal. {@code wired} emitters are cable-fed relays (no depth attenuation); {@code mobile}
         * emitters are people with radios (their own depth attenuates them too).
         */
        private record Emitter(BlockPos pos, int frequency, double radius, long station, boolean wired, boolean mobile) {
        }

        private record Pick(Emitter emitter, double quality) {
        }

        private record JamField(BlockPos pos, int radius) {
        }

        private record Signal(boolean present, long source, double quality, String cipher) {
        }

        private static final class VoiceBudget {
            long window = -100;
            int bytes;
        }

        private static final class LevelNetwork {
            final Set<BlockPos> stations = new LinkedHashSet<>();
            final Set<BlockPos> repeaters = new LinkedHashSet<>();
            final Set<BlockPos> receivers = new LinkedHashSet<>();
            final Set<BlockPos> jammers = new LinkedHashSet<>();
            final Map<BlockPos, Long> cableGroups = new HashMap<>();
            List<JamField> jamFields = new ArrayList<>();
            Set<UUID> pocketHolders = new HashSet<>();
            Map<Long, Set<UUID>> listeners = new HashMap<>();
            long lastTick = Long.MIN_VALUE;
            boolean dirty = true;

            Set<BlockPos> set(Kind kind) {
                return switch (kind) {
                    case STATION -> stations;
                    case REPEATER -> repeaters;
                    case RECEIVER -> receivers;
                    case JAMMER -> jammers;
                };
            }
        }

        private static final Map<ResourceKey<Level>, LevelNetwork> LEVELS = new HashMap<>();
        private static final Map<UUID, VoiceBudget> VOICE_BUDGETS = new HashMap<>();
        private static final Map<UUID, Long> MAP_REQUESTS = new HashMap<>();

        private RadioNetworkManager() {
        }

        private static LevelNetwork network(ServerLevel level) {
            return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelNetwork());
        }

        public static void register(ServerLevel level, BlockPos pos, Kind kind) {
            LevelNetwork network = network(level);
            network.set(kind).add(pos.immutable());
            network.dirty = true;
        }

        public static void unregister(ServerLevel level, BlockPos pos, Kind kind) {
            LevelNetwork network = network(level);
            network.set(kind).remove(pos);
            network.dirty = true;
        }

        /** Requests a graph recompute on the next tick (frequency changed, transmitter placed, ...). */
        public static void markDirty(Level level) {
            if (level instanceof ServerLevel serverLevel) {
                network(serverLevel).dirty = true;
            }
        }

        public static void clear() {
            LEVELS.clear();
            VOICE_BUDGETS.clear();
            MAP_REQUESTS.clear();
        }

        /** Advances the radio network of a level by one tick; safe to call from any number of devices per tick. */
        public static void tick(ServerLevel level) {
            LevelNetwork network = network(level);
            long now = level.getGameTime();
            if (network.lastTick == now) {
                return;
            }
            network.lastTick = now;

            if (network.dirty || now % RECOMPUTE_INTERVAL == 0) {
                network.dirty = false;
                recompute(level, network);
            }
            for (BlockPos pos : new ArrayList<>(network.stations)) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    station.streamTick(level, now);
                }
            }
        }

        /** Players who currently should receive the stream of the given station (packed station position). */
        public static List<ServerPlayer> listeners(ServerLevel level, long stationKey) {
            Set<UUID> ids = network(level).listeners.get(stationKey);
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            List<ServerPlayer> players = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null && player.serverLevel() == level) {
                    players.add(player);
                }
            }
            return players;
        }

        /** Tells every current listener of a station that its stream ended, and forgets them. */
        public static void sendStreamEnd(ServerLevel level, long stationKey) {
            List<ServerPlayer> players = listeners(level, stationKey);
            if (!players.isEmpty()) {
                StreamEndPacket packet = new StreamEndPacket(stationKey);
                for (ServerPlayer player : players) {
                    RadioNetwork.sendToPlayer(player, packet);
                }
            }
        }

        // ------------------------------------------------------------------ jamming

        /**
         * How strongly jammers disturb {@code at}: 0 outside every active jammer's field, rising linearly to 1 at
         * the centre of the strongest field. Uses the field list computed once per second.
         */
        public static double jamLevel(ServerLevel level, BlockPos at) {
            double strongest = 0.0;
            for (JamField field : network(level).jamFields) {
                double distance = Math.sqrt(field.pos().distSqr(at));
                if (distance < field.radius()) {
                    strongest = Math.max(strongest, 1.0 - distance / field.radius());
                }
            }
            return strongest;
        }

        // ------------------------------------------------------------------ walkie-talkie voice

        /** Per-player voice rate limit (about 2x the nominal microphone rate). */
        public static boolean allowVoice(ServerPlayer player, int length) {
            long now = player.serverLevel().getGameTime();
            VoiceBudget budget = VOICE_BUDGETS.computeIfAbsent(player.getUUID(), id -> new VoiceBudget());
            if (now - budget.window >= 20 || now < budget.window) {
                budget.window = now;
                budget.bytes = 0;
            }
            if (budget.bytes + length > VOICE_BYTES_PER_SECOND) {
                return false;
            }
            budget.bytes += length;
            return true;
        }

        /**
         * Delivers one voice chunk of {@code sender} to every player who can hear it, each with the signal
         * quality at their position: distance falloff, depth (silence below Y=0), shielding blocks, jammers.
         * Direct delivery needs a walkie on the same frequency within {@code range}; further away the
         * transmission must be caught by a repeater (over the air or via cable) and the listener must lie inside
         * the radius of a repeater output on the listener's frequency. Players holding a Radio Scanner near a
         * repeater that took part in the relay overhear it regardless of their frequency.
         */
        public static void routeVoice(ServerPlayer sender, int frequency, int range, byte[] data) {
            ServerLevel level = sender.serverLevel();
            BlockPos origin = sender.blockPosition();
            LevelNetwork network = network(level);

            List<Emitter> emitters = new ArrayList<>();
            emitters.add(new Emitter(origin, frequency, Math.max(range, RadioPhysics.WALKIE_RANGE), 0L, false, true));
            Map<BlockPos, Emitter> relays = propagate(level, network, emitters);

            double scannerRangeSqr = (double) RadioPhysics.SCANNER_RANGE * RadioPhysics.SCANNER_RANGE;
            double senderJam = jamLevel(level, origin);
            String name = sender.getGameProfile().getName();

            for (ServerPlayer listener : level.players()) {
                if (listener == sender) {
                    continue;
                }
                BlockPos at = listener.blockPosition();
                double quality = -1.0;
                int mode = -1;
                int tuned = WalkieTalkieItem.tunedFrequency(listener);
                if (tuned > 0) {
                    Pick pick = pick(level, emitters, tuned, at);
                    if (pick != null) {
                        quality = pick.quality();
                        mode = pick.emitter().pos().equals(origin) ? MODE_DIRECT : MODE_RELAYED;
                    }
                }
                if (mode < 0 && !relays.isEmpty() && ScannerItem.isHolding(listener)) {
                    for (BlockPos relay : relays.keySet()) {
                        if (relay.distSqr(at) <= scannerRangeSqr) {
                            mode = MODE_INTERCEPTED;
                            quality = 0.85;
                            break;
                        }
                    }
                }
                if (mode >= 0) {
                    quality *= (1.0 - senderJam) * (1.0 - jamLevel(level, at));
                    RadioNetwork.sendToPlayer(listener, new VoiceChunkPacket(sender.getUUID(), name, frequency, mode,
                            (int) Math.round(Math.max(0.0, Math.min(1.0, quality)) * 100.0), data));
                }
            }
        }

        // ------------------------------------------------------------------ queries for GUIs

        public static boolean allowMapRequest(ServerPlayer player) {
            long now = player.serverLevel().getGameTime();
            Long last = MAP_REQUESTS.get(player.getUUID());
            if (last != null && now >= last && now - last < MAP_REQUEST_INTERVAL_TICKS) {
                return false;
            }
            MAP_REQUESTS.put(player.getUUID(), now);
            return true;
        }

        /** All loaded radio devices of the level for the tablet map. */
        public static List<MapDataPacket.Node> mapNodes(ServerLevel level, BlockPos around) {
            LevelNetwork network = network(level);
            double rangeSqr = (double) MAP_RANGE * MAP_RANGE;
            List<MapDataPacket.Node> nodes = new ArrayList<>();

            for (BlockPos pos : network.stations) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_DISTRIBUTOR, pos,
                            VoidChain.chainFrequency(level, pos, station.getFrequency()), 0,
                            RadioPhysics.stationRadius(level, pos), station.onAir()));
                }
            }
            for (BlockPos pos : network.repeaters) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_REPEATER, pos, repeater.getInputFrequency(),
                            repeater.getOutputFrequency(), RadioPhysics.repeaterRadius(level, pos), repeater.isActive()));
                }
            }
            for (BlockPos pos : network.receivers) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_RECEIVER, pos, receiver.getFrequency(), 0, 0,
                            receiver.isReceiving()));
                }
            }
            for (BlockPos pos : network.jammers) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof JammerBlockEntity jammer) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_JAMMER, pos, 0, 0, jammer.getRadius(), jammer.isActive()));
                }
            }
            return nodes;
        }

        /** Distributors a computer at {@code computer} may link to, nearest first. */
        public static List<StationListPacket.Entry> stationsNear(ServerLevel level, BlockPos computer) {
            double rangeSqr = (double) RadioPhysics.COMPUTER_LINK_RANGE * RadioPhysics.COMPUTER_LINK_RANGE;
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos pos : network(level).stations) {
                if (pos.distSqr(computer) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof StationBlockEntity) {
                    found.add(pos);
                }
            }
            found.sort(Comparator.comparingDouble(pos -> pos.distSqr(computer)));
            List<StationListPacket.Entry> entries = new ArrayList<>();
            for (BlockPos pos : found) {
                if (entries.size() >= StationListPacket.MAX_ENTRIES) {
                    break;
                }
                StationBlockEntity station = (StationBlockEntity) level.getBlockEntity(pos);
                entries.add(new StationListPacket.Entry(pos.immutable(), station.getFrequency(), station.onAir()));
            }
            return entries;
        }

        // ------------------------------------------------------------------ graph recompute

        private static void recompute(ServerLevel level, LevelNetwork network) {
            computeCableGroups(level, network);

            List<JamField> jamFields = new ArrayList<>();
            for (BlockPos pos : network.jammers) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof JammerBlockEntity jammer && jammer.isActive()) {
                    jamFields.add(new JamField(pos.immutable(), jammer.getRadius()));
                }
            }
            network.jamFields = jamFields;

            List<Emitter> emitters = new ArrayList<>();
            for (BlockPos pos : network.stations) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    station.refreshActiveState(level);
                    if (station.onAir() && VoidChain.mayTransmit(level, station)) {
                        emitters.add(new Emitter(pos, VoidChain.chainFrequency(level, pos, station.getFrequency()),
                                RadioPhysics.stationRadius(level, pos), pos.asLong(), false, false));
                    }
                }
            }

            Map<BlockPos, Emitter> activated = propagate(level, network, emitters);
            for (BlockPos pos : network.repeaters) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater) {
                    repeater.setActive(level, activated.containsKey(pos));
                }
            }

            Map<Long, Set<UUID>> listeners = new HashMap<>();
            List<ServerPlayer> players = level.players();
            double listenRangeSqr = (double) RadioPhysics.LISTEN_RANGE * RadioPhysics.LISTEN_RANGE;

            for (BlockPos pos : network.receivers) {
                if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver)) {
                    continue;
                }
                Signal signal = signalAt(level, emitters, receiver.getFrequency(), pos);
                double quality = VoidChain.weather(level, signal.quality() * (1.0 - jamLevel(level, pos)));
                boolean encrypted = VoidChain.locked(receiver.getFrequency(), receiver.getPassword());
                if (encrypted) {
                    quality = 0.0; // secure channel without the master password: white noise only
                }
                receiver.applySignal(level, signal.present(), signal.source(), (int) Math.round(quality * 100.0),
                        signal.cipher(), encrypted);
                if (signal.present() && !encrypted && !NumberStations.isKey(signal.source())) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    for (ServerPlayer player : players) {
                        if (player.distanceToSqr(center) <= listenRangeSqr) {
                            listeners.computeIfAbsent(signal.source(), key -> new HashSet<>()).add(player.getUUID());
                        }
                    }
                }
            }

            // Pocket players: virtual receivers carried by their holders.
            Set<UUID> holders = new HashSet<>();
            for (ServerPlayer player : players) {
                int frequency = PocketPlayerItem.heldFrequency(player);
                if (frequency <= 0) {
                    continue;
                }
                Signal signal = signalAt(level, emitters, frequency, player.blockPosition());
                double quality = VoidChain.weather(level, signal.quality() * (1.0 - jamLevel(level, player.blockPosition())));
                boolean pocketLocked = com.mcradio.core.SecureChannels.isSecure(frequency); // pocket players have no password entry
                if (pocketLocked) {
                    quality = 0.0;
                }
                holders.add(player.getUUID());
                RadioNetwork.sendToPlayer(player, new PocketStatePacket(signal.present() ? signal.source() : 0L,
                        (int) Math.round(quality * 100.0), signal.cipher()));
                if (signal.present() && !pocketLocked && !NumberStations.isKey(signal.source())) {
                    listeners.computeIfAbsent(signal.source(), key -> new HashSet<>()).add(player.getUUID());
                }
            }
            for (UUID previous : network.pocketHolders) {
                if (!holders.contains(previous)) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(previous);
                    if (player != null) {
                        RadioNetwork.sendToPlayer(player, new PocketStatePacket(0L, 0, ""));
                    }
                }
            }
            network.pocketHolders = holders;
            network.listeners = listeners;
        }

        /** What a receiver tuned to {@code frequency} picks up at {@code at}: a number station or the best emitter. */
        private static Signal signalAt(ServerLevel level, List<Emitter> emitters, int frequency, BlockPos at) {
            if (NumberStations.isNumberFrequency(frequency)) {
                double quality = RadioPhysics.depthFactor(at.getY());
                if (RadioPhysics.roofed(level, at)) {
                    quality = 0.0;
                }
                return new Signal(true, NumberStations.key(frequency), quality, NumberStations.code(level, frequency));
            }
            Pick pick = pick(level, emitters, frequency, at);
            if (pick == null) {
                return new Signal(false, 0L, 0.0, "");
            }
            return new Signal(true, pick.emitter().station(), pick.quality(), "");
        }

        /**
         * Lets repeaters catch signals and re-emit them, until nothing changes. A repeater is activated when it
         * catches an emitter on its input frequency over the air (clear enough, inside that emitter's radius) or,
         * failing that, through a cable network shared with an already active repeater whose output frequency equals
         * its input frequency. Each repeater activates at most once, so loops terminate. New relays are appended to
         * {@code emitters}; the returned map holds the relay emitter of every active repeater.
         */
        private static Map<BlockPos, Emitter> propagate(ServerLevel level, LevelNetwork network, List<Emitter> emitters) {
            Map<BlockPos, Emitter> activated = new HashMap<>();
            boolean changed;
            do {
                changed = false;
                for (BlockPos pos : network.repeaters) {
                    if (activated.containsKey(pos) || !level.isLoaded(pos)
                            || !(level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater)) {
                        continue;
                    }
                    double radius = RadioPhysics.repeaterRadius(level, pos);
                    Emitter relay = null;
                    Pick source = pick(level, emitters, repeater.getInputFrequency(), pos);
                    if (source != null && source.quality() >= CATCH_QUALITY) {
                        relay = new Emitter(pos.immutable(), repeater.getOutputFrequency(), radius,
                                source.emitter().station(), false, false);
                    } else {
                        Long group = network.cableGroups.get(pos);
                        if (group != null) {
                            for (Map.Entry<BlockPos, Emitter> other : activated.entrySet()) {
                                if (group.equals(network.cableGroups.get(other.getKey()))
                                        && other.getValue().frequency() == repeater.getInputFrequency()) {
                                    relay = new Emitter(pos.immutable(), repeater.getOutputFrequency(), radius,
                                            other.getValue().station(), true, false);
                                    break;
                                }
                            }
                        }
                    }
                    if (relay != null) {
                        emitters.add(relay);
                        activated.put(pos.immutable(), relay);
                        changed = true;
                    }
                }
            } while (changed);
            return activated;
        }

        /** Groups repeaters that touch the same cable network (group id = smallest packed cable position). */
        private static void computeCableGroups(ServerLevel level, LevelNetwork network) {
            network.cableGroups.clear();
            for (BlockPos repeater : network.repeaters) {
                if (!level.isLoaded(repeater)) {
                    continue;
                }
                Set<BlockPos> visited = new HashSet<>();
                Deque<BlockPos> queue = new ArrayDeque<>();
                for (Direction direction : Direction.values()) {
                    BlockPos neighbour = repeater.relative(direction);
                    if (isCable(level, neighbour) && visited.add(neighbour)) {
                        queue.add(neighbour);
                    }
                }
                long group = Long.MAX_VALUE;
                while (!queue.isEmpty() && visited.size() <= RadioPhysics.MAX_CABLES) {
                    BlockPos cable = queue.poll();
                    group = Math.min(group, cable.asLong());
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbour = cable.relative(direction);
                        if (!visited.contains(neighbour) && isCable(level, neighbour)) {
                            visited.add(neighbour);
                            queue.add(neighbour);
                        }
                    }
                }
                if (group != Long.MAX_VALUE) {
                    network.cableGroups.put(repeater.immutable(), group);
                }
            }
        }

        private static boolean isCable(ServerLevel level, BlockPos pos) {
            return level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof CableBlock;
        }

        private static double emitterQuality(ServerLevel level, Emitter emitter, BlockPos at) {
            double quality = RadioPhysics.quality(level, emitter.pos(), emitter.radius(), emitter.wired(), at);
            if (quality > 0.0 && emitter.mobile()) {
                quality *= RadioPhysics.depthFactor(emitter.pos().getY());
            }
            return quality;
        }

        /** The emitter on {@code frequency} that reaches {@code at} with the best quality, or null if none reaches it. */
        private static Pick pick(ServerLevel level, List<Emitter> emitters, int frequency, BlockPos at) {
            Pick best = null;
            for (Emitter emitter : emitters) {
                if (emitter.frequency() != frequency) {
                    continue;
                }
                double quality = emitterQuality(level, emitter, at);
                if (quality < 0.0) {
                    continue;
                }
                if (best == null || quality > best.quality()) {
                    best = new Pick(emitter, quality);
                }
            }
            return best;
        }
    }


    /**
     * Signal Broadcast Station. In PLAYLIST mode it streams the raw bytes of one MP3 from
     * {@code config/mc_radio/tracks/} (looping) in real time; in LIVE mode it is an always-on carrier that
     * relays microphone audio sent by the player who has the GUI open.
     */
    public static final class StationBlockEntity extends BlockEntity implements RadioDevice {
        public enum Mode {
            PLAYLIST, LIVE;

            public static Mode of(int ordinal) {
                return ordinal == 1 ? LIVE : PLAYLIST;
            }
        }

        private static final double PREBUFFER_SECONDS = 1.0;
        private static final int STREAM_INTERVAL_TICKS = 4;
        private static final int MAX_CHUNK_BYTES = 200_000;
        private static final int MIN_CHUNK_BYTES = 256;
        private static final int LIVE_BYTES_PER_SECOND = 32_000;
        private static final int LIVE_KEEPALIVE_BYTES = 800;

        private int frequency = RadioPhysics.DEFAULT_FREQUENCY;
        private Mode mode = Mode.PLAYLIST;
        private String track = "";
        private boolean broadcasting;
        /** Set by the broadcast computer of a v4 chain while it has something on air. */
        private boolean chainAir;

        // Server-only transient streaming state.
        private CompletableFuture<Mp3Source> loading;
        private Mp3Source source;
        private int cursor;
        private double budget;
        private int liveBytesThisSecond;

        public StationBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.STATION_BE.get(), pos, state);
        }

        /**
         * The distributor controlled from the device at {@code pos}: the distributor itself, or the one a
         * computer at that position is linked to. Null if there is none (yet).
         */
        public static StationBlockEntity find(ServerLevel level, BlockPos pos) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity station) {
                return station;
            }
            if (blockEntity instanceof ComputerBlockEntity computer) {
                return computer.resolve(level);
            }
            return null;
        }

        public int getFrequency() {
            return frequency;
        }

        public Mode getMode() {
            return mode;
        }

        public boolean onAir() {
            return mode == Mode.LIVE || broadcasting || chainAir;
        }

        public void setChainAir(boolean air) {
            chainAir = air;
        }

        public long key() {
            return worldPosition.asLong();
        }

        // ---------------------------------------------------------------- lifecycle

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.STATION);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.sendStreamEnd(serverLevel, key());
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.STATION);
            }
            cancelLoading();
            super.setRemoved();
        }

        // ---------------------------------------------------------------- settings

        public void applySettings(ServerLevel serverLevel, int newFrequency, Mode newMode) {
            boolean modeChanged = newMode != mode;
            boolean frequencyChanged = newFrequency != frequency;
            if (modeChanged) {
                RadioNetworkManager.sendStreamEnd(serverLevel, key());
                resetPlayback();
                broadcasting = false;
                mode = newMode;
            }
            frequency = newFrequency;
            if (modeChanged || frequencyChanged) {
                setChanged();
                RadioNetworkManager.markDirty(serverLevel);
            }
        }

        /** Starts looping the given track. Returns false when the file does not exist or the station is not in playlist mode. */
        public boolean startTrack(ServerLevel serverLevel, String name) {
            if (mode != Mode.PLAYLIST) {
                return false;
            }
            Optional<Path> file = TrackLibrary.resolve(name);
            if (file.isEmpty()) {
                return false;
            }
            RadioNetworkManager.sendStreamEnd(serverLevel, key());
            resetPlayback();
            track = name;
            broadcasting = true;
            loading = Mp3Source.loadAsync(file.get());
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
            return true;
        }

        public void stopBroadcast(ServerLevel serverLevel) {
            if (!broadcasting) {
                return;
            }
            RadioNetworkManager.sendStreamEnd(serverLevel, key());
            resetPlayback();
            broadcasting = false;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        private void resetPlayback() {
            cancelLoading();
            source = null;
            cursor = 0;
            budget = 0.0;
        }

        private void cancelLoading() {
            if (loading != null) {
                loading.cancel(false);
                loading = null;
            }
        }

        // ---------------------------------------------------------------- streaming (server tick)

        public void streamTick(ServerLevel serverLevel, long gameTime) {
            if (VoidChain.isDriven(serverLevel, worldPosition)) {
                return; // a v4 chain station is streamed by its broadcast computer
            }
            if (gameTime % 20 == 0) {
                liveBytesThisSecond = 0;
            }
            if (mode == Mode.LIVE) {
                liveKeepAlive(serverLevel, gameTime);
                return;
            }
            if (!broadcasting) {
                return;
            }

            if (source == null) {
                if (loading == null) {
                    Optional<Path> file = TrackLibrary.resolve(track);
                    if (file.isEmpty()) {
                        McRadio.LOGGER.warn("Track '{}' of station at {} is no longer available", track, worldPosition);
                        failPlayback(serverLevel);
                        return;
                    }
                    loading = Mp3Source.loadAsync(file.get());
                    return;
                }
                if (!loading.isDone()) {
                    return;
                }
                try {
                    source = loading.join();
                } catch (RuntimeException e) {
                    McRadio.LOGGER.warn("Could not load track '{}' for station at {}", track, worldPosition, e);
                    failPlayback(serverLevel);
                    return;
                }
                loading = null;
                cursor = source.start();
                budget = source.bytesPerSecond() * PREBUFFER_SECONDS;
            }

            budget += source.bytesPerSecond() / 20.0;
            if (Math.floorMod(gameTime + worldPosition.hashCode(), STREAM_INTERVAL_TICKS) != 0) {
                return;
            }
            int wanted = (int) Math.min(budget, MAX_CHUNK_BYTES);
            if (wanted < MIN_CHUNK_BYTES) {
                return;
            }
            budget -= wanted;

            byte[] chunk = new byte[wanted];
            byte[] data = source.data();
            int filled = 0;
            while (filled < wanted) {
                int take = Math.min(wanted - filled, source.end() - cursor);
                System.arraycopy(data, cursor, chunk, filled, take);
                filled += take;
                cursor += take;
                if (cursor >= source.end()) {
                    cursor = source.start();
                }
            }
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.MP3, chunk));
        }

        private void failPlayback(ServerLevel serverLevel) {
            resetPlayback();
            broadcasting = false;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        /** In live mode a short burst of silence once per second keeps client streams (and sound instances) alive. */
        private void liveKeepAlive(ServerLevel serverLevel, long gameTime) {
            if (Math.floorMod(gameTime + worldPosition.hashCode(), 20) != 0) {
                return;
            }
            byte[] silence = new byte[LIVE_KEEPALIVE_BYTES];
            Arrays.fill(silence, UlawCodec.silence());
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.ULAW_16K, silence));
        }

        public void broadcast(ServerLevel serverLevel, AudioChunkPacket packet) {
            for (ServerPlayer player : RadioNetworkManager.listeners(serverLevel, key())) {
                RadioNetwork.sendToPlayer(player, packet);
            }
        }

        /** Relays microphone audio received from the player operating the GUI. */
        public void relayLive(ServerLevel serverLevel, byte[] data) {
            if (mode != Mode.LIVE || liveBytesThisSecond + data.length > LIVE_BYTES_PER_SECOND) {
                return;
            }
            liveBytesThisSecond += data.length;
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.ULAW_16K, data));
        }

        /** Sends a typed message to everyone who can currently hear this station. Returns false if nothing was sent. */
        public boolean broadcastText(ServerLevel serverLevel, ServerPlayer sender, String raw) {
            if (mode != Mode.LIVE) {
                return false;
            }
            StringBuilder cleaned = new StringBuilder();
            for (char c : raw.strip().toCharArray()) {
                if (c >= 32 && c != 127) {
                    cleaned.append(c);
                }
            }
            String text = cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned.toString();
            if (text.isEmpty()) {
                return false;
            }
            for (ServerPlayer player : RadioNetworkManager.listeners(serverLevel, key())) {
                double jam = RadioNetworkManager.jamLevel(serverLevel, player.blockPosition());
                RadioNetwork.sendToPlayer(player, new RadioTextPacket(frequency, sender.getGameProfile().getName(),
                        ChatJam.scramble(text, jam)));
            }
            return true;
        }

        public void refreshActiveState(ServerLevel serverLevel) {
            RadioDeviceBlock.setActive(serverLevel, worldPosition, onAir());
        }

        // ---------------------------------------------------------------- persistence

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Frequency", frequency);
            tag.putString("Mode", mode.name());
            tag.putString("Track", track);
            tag.putBoolean("Broadcasting", broadcasting);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            frequency = tag.contains("Frequency") ? RadioPhysics.clampFrequency(tag.getInt("Frequency"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            try {
                mode = Mode.valueOf(tag.getString("Mode"));
            } catch (IllegalArgumentException e) {
                mode = Mode.PLAYLIST;
            }
            track = tag.getString("Track");
            broadcasting = tag.getBoolean("Broadcasting") && mode == Mode.PLAYLIST && !track.isEmpty();
        }

        // ---------------------------------------------------------------- menu

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            int radius = level != null ? RadioPhysics.stationRadius(level, worldPosition) : RadioPhysics.STATION_BASE_RADIUS;
            return new RadioDeviceMenu.DeviceData(worldPosition, frequency, mode.ordinal(), radius, broadcasting, track);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.station");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.STATION_MENU.get(), containerId, menuData());
        }
    }


    /** Signal Repeater: catches a signal on the input frequency and re-emits it on the output frequency. */
    public static final class RepeaterBlockEntity extends BlockEntity implements RadioDevice {
        private int inputFrequency = RadioPhysics.DEFAULT_FREQUENCY;
        private int outputFrequency = RadioPhysics.DEFAULT_FREQUENCY;
        private boolean active;

        public RepeaterBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.REPEATER_BE.get(), pos, state);
        }

        public int getInputFrequency() {
            return inputFrequency;
        }

        public int getOutputFrequency() {
            return outputFrequency;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.REPEATER);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.REPEATER);
            }
            super.setRemoved();
        }

        public void configure(ServerLevel serverLevel, int input, int output) {
            if (input == inputFrequency && output == outputFrequency) {
                return;
            }
            inputFrequency = input;
            outputFrequency = output;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        /** Called by the radio graph after each recompute. */
        public void setActive(ServerLevel serverLevel, boolean value) {
            active = value;
            RadioDeviceBlock.setActive(serverLevel, worldPosition, value);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Input", inputFrequency);
            tag.putInt("Output", outputFrequency);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            inputFrequency = tag.contains("Input") ? RadioPhysics.clampFrequency(tag.getInt("Input"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            outputFrequency = tag.contains("Output") ? RadioPhysics.clampFrequency(tag.getInt("Output"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
        }

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            int radius = level != null ? RadioPhysics.repeaterRadius(level, worldPosition) : RadioPhysics.REPEATER_BASE_RADIUS;
            return new RadioDeviceMenu.DeviceData(worldPosition, inputFrequency, outputFrequency, radius, active, "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.repeater");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.REPEATER_MENU.get(), containerId, menuData());
        }
    }


    /**
     * Radio Receiver &amp; Player. The server decides whether the receiver has a signal (frequency match inside
     * a station's or repeater's radius) and syncs {@code receiving} + the originating station to clients, which
     * then play the locally decoded stream through Minecraft's sound engine.
     */
    public static final class ReceiverBlockEntity extends BlockEntity implements RadioDevice {
        private int frequency = RadioPhysics.DEFAULT_FREQUENCY;
        private int volume = 100;
        private boolean receiving;
        private long sourceStation;
        private int quality = 100;
        private String cipher = "";
        private String password = "";
        private boolean encrypted;

        /** Client-only handle to the running sound instance; typed as Object to keep client classes out of this class. */
        public Object clientSound;

        public ReceiverBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.RECEIVER_BE.get(), pos, state);
        }

        public int getFrequency() {
            return frequency;
        }

        public int getVolume() {
            return volume;
        }

        public boolean isReceiving() {
            return receiving;
        }

        public long getSourceStation() {
            return sourceStation;
        }

        /** Signal quality in percent: below 100 the client mixes in static, at 0 only white noise is audible. */
        public int getQuality() {
            return quality;
        }

        /** Morse cipher of the number station this receiver is tuned to (empty otherwise). */
        public String getCipher() {
            return cipher;
        }

        public String getPassword() {
            return password;
        }

        /** True while this receiver is on a secure channel (310/320/330) without the right master password. */
        public boolean isEncrypted() {
            return encrypted;
        }

        public void setPassword(ServerLevel serverLevel, String newPassword) {
            String cleaned = newPassword == null ? "" : newPassword.strip();
            password = cleaned.length() > 24 ? cleaned.substring(0, 24) : cleaned;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        public ReceiverVariant variant() {
            return getBlockState().getBlock() instanceof ReceiverBlock block ? block.variant() : ReceiverVariant.RADIO;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.RECEIVER);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.RECEIVER);
            }
            super.setRemoved();
        }

        public void configure(ServerLevel serverLevel, int newFrequency, int newVolume) {
            if (newFrequency == frequency && newVolume == volume) {
                return;
            }
            boolean frequencyChanged = newFrequency != frequency;
            frequency = newFrequency;
            volume = newVolume;
            setChanged();
            syncToClients();
            if (frequencyChanged) {
                RadioNetworkManager.markDirty(serverLevel);
            }
        }

        /** Called by the radio graph after each recompute. */
        public void applySignal(ServerLevel serverLevel, boolean hasSignal, long station, int newQuality, String newCipher,
                                boolean newEncrypted) {
            long newSource = hasSignal ? station : 0L;
            int clampedQuality = RadioPhysics.clampVolume(newQuality);
            String cleanCipher = hasSignal ? newCipher : "";
            if (hasSignal == receiving && newSource == sourceStation && clampedQuality == quality
                    && cleanCipher.equals(cipher) && newEncrypted == encrypted) {
                return;
            }
            encrypted = newEncrypted;
            receiving = hasSignal;
            sourceStation = newSource;
            quality = clampedQuality;
            cipher = cleanCipher;
            setChanged();
            RadioDeviceBlock.setActive(serverLevel, worldPosition, hasSignal);
            syncToClients();
        }

        private void syncToClients() {
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Frequency", frequency);
            tag.putInt("Volume", volume);
            tag.putBoolean("Receiving", receiving);
            tag.putLong("Source", sourceStation);
            tag.putInt("Quality", quality);
            tag.putString("Cipher", cipher);
            tag.putString("Pass", password);
            tag.putBoolean("Encrypted", encrypted);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            frequency = tag.contains("Frequency") ? RadioPhysics.clampFrequency(tag.getInt("Frequency"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            volume = tag.contains("Volume") ? RadioPhysics.clampVolume(tag.getInt("Volume")) : 100;
            receiving = tag.getBoolean("Receiving");
            sourceStation = tag.getLong("Source");
            quality = tag.contains("Quality") ? RadioPhysics.clampVolume(tag.getInt("Quality")) : 100;
            cipher = tag.getString("Cipher");
            password = tag.getString("Pass");
            encrypted = tag.getBoolean("Encrypted");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = saveWithoutMetadata(registries);
            tag.remove("Pass"); // the password never leaves the server
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            return new RadioDeviceMenu.DeviceData(worldPosition, frequency, volume, 0, receiving, "");
        }

        @Override
        public Component getDisplayName() {
            return getBlockState().getBlock().getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.RECEIVER_MENU.get(), containerId, menuData());
        }
    }


    /**
     * Computer: a remote terminal for a Signal Distributor. Once linked it shows exactly the distributor's
     * interface (frequency, playlist / live mode, broadcast), and all actions are applied to the distributor.
     */
    public static final class ComputerBlockEntity extends BlockEntity implements RadioDevice {
        private BlockPos linked;

        public ComputerBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.COMPUTER_BE.get(), pos, state);
        }

        private boolean inRange(BlockPos target) {
            double max = (double) RadioPhysics.COMPUTER_LINK_RANGE * RadioPhysics.COMPUTER_LINK_RANGE;
            return target.distSqr(worldPosition) <= max;
        }

        /** Connects to the distributor at {@code target}. Returns false if there is none in range. */
        public boolean link(ServerLevel level, BlockPos target) {
            if (!inRange(target) || !level.isLoaded(target) || !(level.getBlockEntity(target) instanceof StationBlockEntity)) {
                return false;
            }
            linked = target.immutable();
            setChanged();
            RadioDeviceBlock.setActive(level, worldPosition, true);
            return true;
        }

        public void unlink(ServerLevel level) {
            linked = null;
            setChanged();
            RadioDeviceBlock.setActive(level, worldPosition, false);
        }

        /** The linked distributor, or null if not linked, unloaded, removed or out of range. */
        public StationBlockEntity resolve(ServerLevel level) {
            if (linked == null || !inRange(linked) || !level.isLoaded(linked)) {
                return null;
            }
            return level.getBlockEntity(linked) instanceof StationBlockEntity station ? station : null;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putBoolean("HasLink", linked != null);
            if (linked != null) {
                tag.putLong("Link", linked.asLong());
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            linked = tag.getBoolean("HasLink") ? BlockPos.of(tag.getLong("Link")) : null;
        }

        /**
         * Linked: the distributor's snapshot (so the client shows the same interface). Not linked: radius -1,
         * which tells the client to show the connection page.
         */
        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            if (level instanceof ServerLevel serverLevel) {
                StationBlockEntity station = resolve(serverLevel);
                if (station != null) {
                    RadioDeviceMenu.DeviceData data = station.menuData();
                    return new RadioDeviceMenu.DeviceData(worldPosition, data.a(), data.b(), data.c(), data.flag(), data.text());
                }
            }
            return new RadioDeviceMenu.DeviceData(worldPosition, 0, 0, -1, false, "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.computer");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.COMPUTER_MENU.get(), containerId, menuData());
        }
    }


    /**
     * Signal Jammer. Active when switched on in its GUI or when powered by redstone. Inside its field the
     * jam level rises from 0 at the edge to 1 in the centre: walkie voice turns to hoarse noise, radio music to
     * static crackle, receivers lose the signal and text chat letters turn into scrambled symbols.
     */
    public static final class JammerBlockEntity extends BlockEntity implements RadioDevice {
        public static final int MIN_RADIUS = 8;
        public static final int MAX_RADIUS = 96;

        private int radius = 64;
        private boolean enabled;
        private boolean powered;

        public JammerBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.JAMMER_BE.get(), pos, state);
        }

        public int getRadius() {
            return radius;
        }

        public boolean isActive() {
            return enabled || powered;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.JAMMER);
                powered = serverLevel.hasNeighborSignal(worldPosition);
                refresh(serverLevel);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.JAMMER);
            }
            super.setRemoved();
        }

        public void configure(ServerLevel serverLevel, int newRadius, boolean newEnabled) {
            radius = Mth.clamp(newRadius, MIN_RADIUS, MAX_RADIUS);
            enabled = newEnabled;
            setChanged();
            refresh(serverLevel);
        }

        public void setPowered(ServerLevel serverLevel, boolean value) {
            if (powered != value) {
                powered = value;
                refresh(serverLevel);
            }
        }

        private void refresh(ServerLevel serverLevel) {
            RadioDeviceBlock.setActive(serverLevel, worldPosition, isActive());
            RadioNetworkManager.markDirty(serverLevel);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Radius", radius);
            tag.putBoolean("Enabled", enabled);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            radius = tag.contains("Radius") ? Mth.clamp(tag.getInt("Radius"), MIN_RADIUS, MAX_RADIUS) : 64;
            enabled = tag.getBoolean("Enabled");
        }

        /** a = radius, b = switched on (1/0), flag = currently active (switch or redstone). */
        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            return new RadioDeviceMenu.DeviceData(worldPosition, radius, enabled ? 1 : 0, 0, isActive(), "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.jammer");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.JAMMER_MENU.get(), containerId, menuData());
        }
    }


    /** Shared behaviour of the station, repeater and receiver: facing, "active" light/texture state, GUI opening. */
    public static abstract class RadioDeviceBlock extends BaseEntityBlock {
        public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

        protected RadioDeviceBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(ACTIVE, false);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RadioDevice device && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(device, buf -> device.menuData().write(buf));
            }
            return InteractionResult.CONSUME;
        }

        /** Updates the ACTIVE blockstate property only when it actually changes. */
        public static void setActive(Level level, BlockPos pos, boolean active) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(ACTIVE) && state.getValue(ACTIVE) != active) {
                level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
            }
        }
    }


    /** Signal Broadcast Station (Станция раздачи сигнала). */
    public static class StationBlock extends RadioDeviceBlock {
        public static final MapCodec<StationBlock> CODEC = simpleCodec(StationBlock::new);
        private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);

        public StationBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends StationBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StationBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, ModRegistry.STATION_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /** Signal Repeater (Ретранслятор сигнала). */
    public static class RepeaterBlock extends RadioDeviceBlock {
        public static final MapCodec<RepeaterBlock> CODEC = simpleCodec(RepeaterBlock::new);
        private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 15.0, 14.0);

        public RepeaterBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends RepeaterBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RepeaterBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, ModRegistry.REPEATER_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /** Radio Receiver, Industrial Horn and Wall Speaker: the same device in three {@link ReceiverVariant}s. */
    public static class ReceiverBlock extends RadioDeviceBlock {
        public static final MapCodec<ReceiverBlock> CODEC =
                simpleCodec(properties -> new ReceiverBlock(properties, ReceiverVariant.RADIO));
        private static final VoxelShape RADIO_SHAPE = Block.box(2.0, 0.0, 3.0, 14.0, 9.0, 13.0);
        private static final VoxelShape HORN_SHAPE = Block.box(3.0, 0.0, 1.0, 13.0, 15.0, 13.0);
        private static final VoxelShape SPEAKER_SHAPE = Block.box(3.0, 0.0, 4.0, 13.0, 12.0, 12.0);

        private final ReceiverVariant variant;

        public ReceiverBlock(Properties properties, ReceiverVariant variant) {
            super(properties);
            this.variant = variant;
        }

        public ReceiverVariant variant() {
            return variant;
        }

        @Override
        protected MapCodec<? extends ReceiverBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (variant) {
                case RADIO -> RADIO_SHAPE;
                case HORN -> HORN_SHAPE;
                case SPEAKER -> SPEAKER_SHAPE;
            };
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReceiverBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                // Client-only class is referenced exclusively inside this lambda, which only runs on the client.
                return createTickerHelper(type, ModRegistry.RECEIVER_BE.get(),
                        (tickLevel, pos, tickState, entity) -> ClientReceiverLogic.tick(entity));
            }
            return createTickerHelper(type, ModRegistry.RECEIVER_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /**
     * Signal Transmitter (Транслятор сигнала): an antenna tower. Placed face-adjacent to a Broadcast Station
     * (or a Repeater), or chained to other transmitters that touch one, it raises that device's broadcast
     * radius; see {@link RadioPhysics#transmitterCount}. It has no GUI and no block entity.
     */
    public static class TransmitterBlock extends Block {
        public static final MapCodec<TransmitterBlock> CODEC = simpleCodec(TransmitterBlock::new);
        private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

        public TransmitterBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends TransmitterBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }
    }


    /** Computer (Компьютер): remote terminal for a Signal Distributor. */
    public static class ComputerBlock extends RadioDeviceBlock {
        public static final MapCodec<ComputerBlock> CODEC = simpleCodec(ComputerBlock::new);
        private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

        public ComputerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends ComputerBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ComputerBlockEntity(pos, state);
        }
    }


    /** Signal Jammer (Глушилка): GUI for radius and power, also switched by redstone. */
    public static class JammerBlock extends RadioDeviceBlock {
        public static final MapCodec<JammerBlock> CODEC = simpleCodec(JammerBlock::new);
        private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 15.0, 14.0);

        public JammerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends JammerBlock> codec() {
            return CODEC;
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                       BlockPos neighborPos, boolean movedByPiston) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof JammerBlockEntity jammer) {
                jammer.setPowered((ServerLevel) level, level.hasNeighborSignal(pos));
            }
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new JammerBlockEntity(pos, state);
        }
    }


    /**
     * Antenna Cable: carries the signal of a repeater to every other repeater touching the same cable network,
     * with no loss over distance and without the depth attenuation that applies to radio waves. This is how a
     * signal is brought from the surface to any depth.
     */
    public static class CableBlock extends Block {
        public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
        private static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);

        static {
            for (Direction direction : Direction.values()) {
                CONNECTIONS.put(direction, BooleanProperty.create(direction.getName()));
            }
        }

        private static final VoxelShape CORE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
        private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

        static {
            ARMS.put(Direction.NORTH, Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0));
            ARMS.put(Direction.SOUTH, Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0));
            ARMS.put(Direction.WEST, Block.box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0));
            ARMS.put(Direction.EAST, Block.box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0));
            ARMS.put(Direction.DOWN, Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0));
            ARMS.put(Direction.UP, Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0));
        }

        public CableBlock(Properties properties) {
            super(properties);
            BlockState state = stateDefinition.any();
            for (BooleanProperty property : CONNECTIONS.values()) {
                state = state.setValue(property, false);
            }
            registerDefaultState(state);
        }

        @Override
        protected MapCodec<? extends CableBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            CONNECTIONS.values().forEach(builder::add);
        }

        private static boolean connectsTo(BlockState neighbour) {
            return neighbour.getBlock() instanceof CableBlock || neighbour.getBlock() instanceof RepeaterBlock;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = defaultBlockState();
            for (Direction direction : Direction.values()) {
                state = state.setValue(CONNECTIONS.get(direction), connectsTo(level.getBlockState(pos.relative(direction))));
            }
            return state;
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                         LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
            return state.setValue(CONNECTIONS.get(direction), connectsTo(neighbourState));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) {
                if (state.getValue(CONNECTIONS.get(direction))) {
                    shape = Shapes.or(shape, ARMS.get(direction));
                }
            }
            return shape;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }
    }


    /** Deferred registers for blocks, items, block entities, menus and the creative tab. */
    public static final class ModRegistry {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, McRadio.MOD_ID);
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, McRadio.MOD_ID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, McRadio.MOD_ID);
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, McRadio.MOD_ID);
        public static final DeferredRegister<CreativeModeTab> TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, McRadio.MOD_ID);

        // ------------------------------------------------------------------ blocks

        public static final RegistryObject<Block> STATION_BLOCK = BLOCKS.register("signal_station",
                () -> new StationBlock(deviceProperties()));
        public static final RegistryObject<Block> TRANSMITTER_BLOCK = BLOCKS.register("signal_transmitter",
                () -> new TransmitterBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));
        public static final RegistryObject<Block> REPEATER_BLOCK = BLOCKS.register("signal_repeater",
                () -> new RepeaterBlock(deviceProperties()));
        public static final RegistryObject<Block> RECEIVER_BLOCK = BLOCKS.register("signal_receiver",
                () -> new ReceiverBlock(deviceProperties(), ReceiverVariant.RADIO));

        public static final RegistryObject<Block> COMPUTER_BLOCK = BLOCKS.register("signal_computer",
                () -> new ComputerBlock(deviceProperties()));
        public static final RegistryObject<Block> JAMMER_BLOCK = BLOCKS.register("signal_jammer",
                () -> new JammerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                        .lightLevel(state -> state.getValue(RadioDeviceBlock.ACTIVE) ? 4 : 0)));

        public static final RegistryObject<Block> HORN_BLOCK = BLOCKS.register("signal_horn",
                () -> new ReceiverBlock(deviceProperties(), ReceiverVariant.HORN));
        public static final RegistryObject<Block> SPEAKER_BLOCK = BLOCKS.register("signal_speaker",
                () -> new ReceiverBlock(deviceProperties(), ReceiverVariant.SPEAKER));
        public static final RegistryObject<Block> CABLE_BLOCK = BLOCKS.register("antenna_cable",
                () -> new CableBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(1.0F, 3.0F)
                        .sound(SoundType.METAL)
                        .noOcclusion()));

        // ------------------------------------------------------------------ items

        public static final RegistryObject<Item> STATION_ITEM = ITEMS.register("signal_station",
                () -> new BlockItem(STATION_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> TRANSMITTER_ITEM = ITEMS.register("signal_transmitter",
                () -> new BlockItem(TRANSMITTER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> REPEATER_ITEM = ITEMS.register("signal_repeater",
                () -> new BlockItem(REPEATER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> RECEIVER_ITEM = ITEMS.register("signal_receiver",
                () -> new BlockItem(RECEIVER_BLOCK.get(), new Item.Properties()));

        public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register("signal_computer",
                () -> new BlockItem(COMPUTER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> JAMMER_ITEM = ITEMS.register("signal_jammer",
                () -> new BlockItem(JAMMER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> HORN_ITEM = ITEMS.register("signal_horn",
                () -> new BlockItem(HORN_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> SPEAKER_ITEM = ITEMS.register("signal_speaker",
                () -> new BlockItem(SPEAKER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> CABLE_ITEM = ITEMS.register("antenna_cable",
                () -> new BlockItem(CABLE_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> FIELD_RADIO_ITEM = ITEMS.register("field_radio",
                () -> new FieldRadioItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> POCKET_ITEM = ITEMS.register("pocket_player",
                () -> new PocketPlayerItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> DECODER_ITEM = ITEMS.register("radio_decoder",
                () -> new DecoderItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> CIRCUIT_BOARD_ITEM = ITEMS.register("circuit_board",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> FERRITE_COIL_ITEM = ITEMS.register("ferrite_coil",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ANTENNA_ROD_ITEM = ITEMS.register("antenna_rod",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SATELLITE_CORE_ITEM = ITEMS.register("satellite_core",
                () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
        public static final RegistryObject<Item> WALKIE_ITEM = ITEMS.register("walkie_talkie",
                () -> new WalkieTalkieItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> SCANNER_ITEM = ITEMS.register("radio_scanner",
                () -> new ScannerItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> TABLET_ITEM = ITEMS.register("radio_tablet",
                () -> new TabletItem(new Item.Properties().stacksTo(1)));

        // ------------------------------------------------------------------ block entities

        public static final RegistryObject<BlockEntityType<StationBlockEntity>> STATION_BE = BLOCK_ENTITIES.register(
                "signal_station",
                () -> BlockEntityType.Builder.of(StationBlockEntity::new, STATION_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<RepeaterBlockEntity>> REPEATER_BE = BLOCK_ENTITIES.register(
                "signal_repeater",
                () -> BlockEntityType.Builder.of(RepeaterBlockEntity::new, REPEATER_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<ReceiverBlockEntity>> RECEIVER_BE = BLOCK_ENTITIES.register(
                "signal_receiver",
                () -> BlockEntityType.Builder.of(ReceiverBlockEntity::new, RECEIVER_BLOCK.get(), HORN_BLOCK.get(),
                        SPEAKER_BLOCK.get()).build(null));

        public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITIES.register(
                "signal_computer",
                () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<JammerBlockEntity>> JAMMER_BE = BLOCK_ENTITIES.register(
                "signal_jammer",
                () -> BlockEntityType.Builder.of(JammerBlockEntity::new, JAMMER_BLOCK.get()).build(null));

        // ------------------------------------------------------------------ menus

        public static final RegistryObject<MenuType<RadioDeviceMenu>> STATION_MENU = MENUS.register("signal_station",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.STATION_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));
        public static final RegistryObject<MenuType<RadioDeviceMenu>> REPEATER_MENU = MENUS.register("signal_repeater",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.REPEATER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));
        public static final RegistryObject<MenuType<RadioDeviceMenu>> RECEIVER_MENU = MENUS.register("signal_receiver",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.RECEIVER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));

        public static final RegistryObject<MenuType<RadioDeviceMenu>> COMPUTER_MENU = MENUS.register("signal_computer",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.COMPUTER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));

        public static final RegistryObject<MenuType<RadioDeviceMenu>> JAMMER_MENU = MENUS.register("signal_jammer",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.JAMMER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));

        // ------------------------------------------------------------------ creative tab

        public static final RegistryObject<CreativeModeTab> RADIO_TAB = TABS.register("radio", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcradio"))
                .icon(() -> new ItemStack(STATION_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(STATION_ITEM.get());
                    output.accept(COMPUTER_ITEM.get());
                    output.accept(TRANSMITTER_ITEM.get());
                    output.accept(REPEATER_ITEM.get());
                    output.accept(RECEIVER_ITEM.get());
                    output.accept(HORN_ITEM.get());
                    output.accept(SPEAKER_ITEM.get());
                    output.accept(CABLE_ITEM.get());
                    output.accept(JAMMER_ITEM.get());
                    output.accept(WALKIE_ITEM.get());
                    output.accept(FIELD_RADIO_ITEM.get());
                    output.accept(POCKET_ITEM.get());
                    output.accept(SCANNER_ITEM.get());
                    output.accept(DECODER_ITEM.get());
                    output.accept(TABLET_ITEM.get());
                    output.accept(CIRCUIT_BOARD_ITEM.get());
                    output.accept(FERRITE_COIL_ITEM.get());
                    output.accept(ANTENNA_ROD_ITEM.get());
                    output.accept(SATELLITE_CORE_ITEM.get());
                    VoidChain.addToTab(output);
                })
                .build());

        private ModRegistry() {
        }

        private static BlockBehaviour.Properties deviceProperties() {
            return BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(RadioDeviceBlock.ACTIVE) ? 6 : 0);
        }

        public static void register(IEventBus modBus) {
            BLOCKS.register(modBus);
            ITEMS.register(modBus);
            BLOCK_ENTITIES.register(modBus);
            MENUS.register(modBus);
            TABS.register(modBus);
        }
    }


    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ServerEvents {
        private ServerEvents() {
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            RadioNetworkManager.clear();
        }

        /** Players inside a Signal Jammer's field cannot write clearly: their letters turn into §k symbols. */
        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer player = event.getPlayer();
            double jam = RadioNetworkManager.jamLevel(player.serverLevel(), player.blockPosition());
            if (jam > 0.02) {
                event.setMessage(Component.literal(ChatJam.scramble(event.getRawText(), jam)));
            }
        }
    }


    /** Mod-bus client initialisation. Only ever loaded on the physical client. */
    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientSetup {
        private ClientSetup() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModRegistry.STATION_MENU.get(), StationScreen::new);
                MenuScreens.register(ModRegistry.REPEATER_MENU.get(), RepeaterScreen::new);
                MenuScreens.register(ModRegistry.RECEIVER_MENU.get(), ReceiverScreen::new);
                // A computer shows the interface of the distributor it is linked to.
                MenuScreens.register(ModRegistry.COMPUTER_MENU.get(), StationScreen::new);
                MenuScreens.register(ModRegistry.JAMMER_MENU.get(), JammerScreen::new);
                VoidChain.ClientInit.register();
                // The pocket player's needle: item model overrides select a texture from this 0..1 signal value.
                ItemProperties.register(ModRegistry.POCKET_ITEM.get(),
                        ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "signal"),
                        (stack, level, entity, seed) -> ClientRadioState.pocketQuality() / 100.0F);
            });
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(WalkieClientLogic.PUSH_TO_TALK);
        }
    }


    /** Forge-bus client events. */
    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            MicCapture.stop();
            ClientAudioManager.clear();
            ClientRadioState.reset();
        }
    }


    /**
     * The only entry point from network packet handlers into client code. Keeping these as static methods of a
     * dedicated client-only class means a dedicated server never needs to resolve any client class.
     */
    public static final class ClientPacketHandlers {
        private ClientPacketHandlers() {
        }

        public static void trackList(List<String> tracks) {
            if (Minecraft.getInstance().screen instanceof StationScreen screen) {
                screen.setTracks(tracks);
            }
        }

        public static void audioChunk(long station, int codec, byte[] data) {
            ClientAudioManager.accept(station, codec, data);
        }

        public static void streamEnd(long station) {
            ClientAudioManager.end(station);
        }

        public static void voice(UUID speaker, String name, int frequency, int mode, int quality, byte[] data) {
            ClientAudioManager.voice(speaker, name, frequency, mode, quality, data);
        }

        public static void pocketState(long source, int quality, String cipher) {
            ClientRadioState.update(source, quality, cipher);
        }

        public static void mapData(List<MapDataPacket.Node> nodes) {
            if (Minecraft.getInstance().screen instanceof TabletScreen screen) {
                screen.setNodes(nodes);
            }
        }

        public static void stationList(List<StationListPacket.Entry> entries) {
            if (Minecraft.getInstance().screen instanceof StationScreen screen) {
                screen.setStations(entries);
            }
        }

        public static void radioText(int frequency, String sender, String text) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.mcradio.radio_text", frequency, sender, text), false);
            }
        }
    }


    /**
     * Registry of active client-side radio streams: station streams keyed by the packed position of the
     * originating station, and walkie-talkie voice streams keyed by the speaker.
     */
    public static final class ClientAudioManager {
        private static final long STALE_NANOS = 8_000_000_000L;
        private static final int VOICE_PREBUFFER_SAMPLES = PcmBuffer.SAMPLE_RATE * 12 / 100;

        private static final Map<Long, ClientRadioStream> STREAMS = new HashMap<>();
        private static final Map<UUID, ClientRadioStream> VOICES = new HashMap<>();
        private static final Map<Long, ClientRadioStream> NUMBER_STREAMS = new HashMap<>();

        private ClientAudioManager() {
        }

        /** Returns the live stream for a station, creating it if necessary. Main thread only. */
        public static ClientRadioStream get(long station) {
            ClientRadioStream stream = STREAMS.get(station);
            if (stream == null || stream.isClosed()) {
                stream = new ClientRadioStream(station);
                STREAMS.put(station, stream);
            }
            return stream;
        }

        /** The Morse loop of a number station, rebuilt when the cipher changes (e.g. another world). */
        public static ClientRadioStream numberStation(long key, String cipher) {
            ClientRadioStream stream = NUMBER_STREAMS.get(key);
            if (stream == null || !stream.cipher().equals(cipher)) {
                stream = ClientRadioStream.synthetic(key, cipher);
                NUMBER_STREAMS.put(key, stream);
            }
            return stream;
        }

        /** The stream a receiver or pocket player should play for the given source key. */
        public static ClientRadioStream sourceStream(long source, String cipher) {
            return NumberStations.isKey(source) ? numberStation(source, cipher) : get(source);
        }

        public static void accept(long station, int codec, byte[] data) {
            get(station).accept(codec, data);
            prune();
        }

        public static void end(long station) {
            ClientRadioStream stream = STREAMS.remove(station);
            if (stream != null) {
                stream.close();
            }
        }

        /** Appends walkie-talkie audio; starts a new sound (and announces the speaker) when a transmission begins. */
        public static void voice(UUID speaker, String name, int frequency, int mode, int quality, byte[] data) {
            ClientRadioStream stream = VOICES.get(speaker);
            if (stream == null || stream.isClosed()) {
                stream = new ClientRadioStream(0L, VOICE_PREBUFFER_SAMPLES);
                VOICES.put(speaker, stream);
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.getSoundManager().play(new WalkieSoundInstance(speaker, stream));
                LocalPlayer player = minecraft.player;
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mcradio.voice." + mode, name, frequency), true);
                }
            }
            stream.acceptVoice(data, quality);
        }

        public static void endVoice(UUID speaker) {
            ClientRadioStream stream = VOICES.remove(speaker);
            if (stream != null) {
                stream.close();
            }
        }

        public static void clear() {
            STREAMS.values().forEach(ClientRadioStream::close);
            STREAMS.clear();
            VOICES.values().forEach(ClientRadioStream::close);
            VOICES.clear();
            NUMBER_STREAMS.clear();
        }

        private static void prune() {
            long now = System.nanoTime();
            STREAMS.values().removeIf(stream -> {
                if (stream.isClosed()) {
                    return true;
                }
                if (now - stream.lastDataNanos() > STALE_NANOS) {
                    stream.close();
                    return true;
                }
                return false;
            });
        }
    }


    /** Streaming linear-interpolation resampler for mono 16-bit PCM. One instance per source stream. */
    static final class LinearResampler {
        private final int outputRate;
        private double position;
        private short last;

        LinearResampler(int outputRate) {
            this.outputRate = outputRate;
        }

        short[] process(short[] input, int length, int sourceRate) {
            if (length <= 0) {
                return new short[0];
            }
            if (sourceRate == outputRate) {
                short[] copy = new short[length];
                System.arraycopy(input, 0, copy, 0, length);
                last = input[length - 1];
                position = 0.0;
                return copy;
            }
            double step = (double) sourceRate / outputRate;
            short[] output = new short[(int) (length / step) + 2];
            int produced = 0;
            double p = position;
            while (p < length && produced < output.length) {
                int index = (int) p;
                double fraction = p - index;
                double a = index == 0 ? last : input[index - 1];
                double b = input[index];
                output[produced++] = (short) Math.round(a + (b - a) * fraction);
                p += step;
            }
            position = p - length;
            last = input[length - 1];
            if (produced == output.length) {
                return output;
            }
            short[] trimmed = new short[produced];
            System.arraycopy(output, 0, trimmed, 0, produced);
            return trimmed;
        }
    }


    /**
     * Thread-safe ring buffer of mono 44.1 kHz PCM with a jitter-buffer state machine. One instance per sound
     * instance (so several receivers can play the same station without stealing samples from each other).
     * While "buffering", the reader gets short silent chunks until enough audio has accumulated.
     */
    public static final class PcmBuffer {
        public static final int SAMPLE_RATE = 44100;
        public static final int DEFAULT_PREBUFFER_SAMPLES = SAMPLE_RATE / 2;
        private static final int SILENCE_CHUNK = SAMPLE_RATE / 20;
        private static final int AUDIO_CHUNK = SAMPLE_RATE / 10;
        private static final long IDLE_FLUSH_NANOS = 250_000_000L;

        private final short[] ring;
        private final int prebufferSamples;
        private int readPosition;
        private int size;
        private boolean buffering = true;
        private long lastWriteNanos = System.nanoTime();

        PcmBuffer(int capacity, int prebufferSamples) {
            this.ring = new short[capacity];
            this.prebufferSamples = prebufferSamples;
        }

        synchronized void write(short[] samples, int count) {
            for (int i = 0; i < count; i++) {
                int index = (readPosition + size) % ring.length;
                ring[index] = samples[i];
                if (size == ring.length) {
                    readPosition = (readPosition + 1) % ring.length;
                } else {
                    size++;
                }
            }
            lastWriteNanos = System.nanoTime();
        }

        /** Fills {@code destination} with at most {@code max} samples and returns how many (always at least one). */
        synchronized int readChunk(short[] destination, int max) {
            int limit = Math.max(1, Math.min(max, destination.length));
            if (buffering) {
                boolean idle = size > 0 && System.nanoTime() - lastWriteNanos > IDLE_FLUSH_NANOS;
                if (size >= prebufferSamples || idle) {
                    buffering = false;
                } else {
                    int silence = Math.min(limit, SILENCE_CHUNK);
                    Arrays.fill(destination, 0, silence, (short) 0);
                    return silence;
                }
            }
            int count = Math.min(Math.min(limit, AUDIO_CHUNK), size);
            if (count <= 0) {
                buffering = true;
                int silence = Math.min(limit, SILENCE_CHUNK);
                Arrays.fill(destination, 0, silence, (short) 0);
                return silence;
            }
            for (int i = 0; i < count; i++) {
                destination[i] = ring[readPosition];
                readPosition = (readPosition + 1) % ring.length;
            }
            size -= count;
            if (size == 0) {
                buffering = true;
            }
            return count;
        }

        synchronized void clear() {
            size = 0;
            readPosition = 0;
            buffering = true;
        }
    }


    /** Producer/consumer byte pipe: the network thread offers MP3 chunks, the decoder thread reads (blocking). */
    static final class BlockingByteStream extends InputStream {
        private static final int MAX_QUEUED_BYTES = 4 * 1024 * 1024;

        private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private final AtomicInteger queuedBytes = new AtomicInteger();
        private byte[] current;
        private int offset;
        private volatile boolean closed;

        void offer(byte[] data) {
            if (closed || data.length == 0 || queuedBytes.get() > MAX_QUEUED_BYTES) {
                return;
            }
            queuedBytes.addAndGet(data.length);
            queue.offer(data);
        }

        private boolean ensureData() {
            while (current == null || offset >= current.length) {
                if (closed) {
                    return false;
                }
                try {
                    byte[] next = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (next != null) {
                        queuedBytes.addAndGet(-next.length);
                        current = next;
                        offset = 0;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }

        @Override
        public int read() {
            if (!ensureData()) {
                return -1;
            }
            return current[offset++] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int off, int len) {
            if (len == 0) {
                return 0;
            }
            if (!ensureData()) {
                return -1;
            }
            int count = Math.min(len, current.length - offset);
            System.arraycopy(current, offset, buffer, off, count);
            offset += count;
            return count;
        }

        @Override
        public int available() {
            return current == null ? 0 : Math.max(0, current.length - offset);
        }

        @Override
        public void close() {
            closed = true;
            queue.clear();
        }
    }


    /**
     * Client-side state of one station's stream: raw MP3 chunks are fed to a background JLayer decoder thread,
     * live voice chunks are expanded inline, and everything ends up as mono 44.1 kHz PCM that is fanned out to
     * every attached {@link PcmBuffer} (one per playing sound instance).
     */
    public static final class ClientRadioStream {
        private final long station;
        private final int prebufferSamples;
        private final short[] syntheticLoop;
        private final String cipher;
        private final CopyOnWriteArrayList<PcmBuffer> readers = new CopyOnWriteArrayList<>();
        private final LinearResampler voiceResampler = new LinearResampler(PcmBuffer.SAMPLE_RATE);

        private BlockingByteStream mp3Input;
        private Thread decoderThread;
        // Second channel for announcements, so the music can be ducked underneath them.
        private final CopyOnWriteArrayList<PcmBuffer> announcementReaders = new CopyOnWriteArrayList<>();
        private BlockingByteStream announcementInput;
        private Thread announcementThread;
        private volatile float musicGain = 1.0F;
        private volatile float announcementGain = 1.0F;
        private volatile boolean closed;
        private volatile long lastDataNanos = System.nanoTime();

        ClientRadioStream(long station) {
            this(station, PcmBuffer.DEFAULT_PREBUFFER_SAMPLES);
        }

        ClientRadioStream(long station, int prebufferSamples) {
            this(station, prebufferSamples, null, "");
        }

        private ClientRadioStream(long station, int prebufferSamples, short[] syntheticLoop, String cipher) {
            this.station = station;
            this.prebufferSamples = prebufferSamples;
            this.syntheticLoop = syntheticLoop;
            this.cipher = cipher;
        }

        /** A number station: an endless Morse loop generated locally from the cipher, no network audio involved. */
        static ClientRadioStream synthetic(long station, String cipher) {
            return new ClientRadioStream(station, 0, MorseSynth.render(cipher), cipher);
        }

        /** Looping Morse samples if this is a number station stream, else null. */
        short[] syntheticLoop() {
            return syntheticLoop;
        }

        String cipher() {
            return cipher;
        }

        public long station() {
            return station;
        }

        public boolean isClosed() {
            return closed;
        }

        long lastDataNanos() {
            return lastDataNanos;
        }

        PcmBuffer attach() {
            PcmBuffer buffer = new PcmBuffer(PcmBuffer.SAMPLE_RATE * 4, prebufferSamples);
            readers.add(buffer);
            return buffer;
        }

        void detach(PcmBuffer buffer) {
            readers.remove(buffer);
        }

        PcmBuffer attachAnnouncement() {
            PcmBuffer buffer = new PcmBuffer(PcmBuffer.SAMPLE_RATE * 4, prebufferSamples);
            announcementReaders.add(buffer);
            return buffer;
        }

        void detachAnnouncement(PcmBuffer buffer) {
            announcementReaders.remove(buffer);
        }

        /** Gains of the music and announcement channels (0..1), set by the broadcast computer for ducking. */
        public void setGains(float music, float announcement) {
            this.musicGain = Math.max(0.0F, Math.min(1.5F, music));
            this.announcementGain = Math.max(0.0F, Math.min(1.5F, announcement));
        }

        float musicGain() {
            return musicGain;
        }

        float announcementGain() {
            return announcementGain;
        }

        /** Called on the main thread when an {@code AudioChunkPacket} arrives. */
        void accept(int codec, byte[] data) {
            if (closed) {
                return;
            }
            lastDataNanos = System.nanoTime();
            switch (codec) {
                case AudioCodec.MP3 -> acceptMp3(data);
                case AudioCodec.ULAW_16K -> acceptVoice(data);
                case AudioCodec.MP3_ANNOUNCEMENT -> acceptAnnouncement(data);
                default -> McRadio.LOGGER.debug("Ignoring audio chunk with unknown codec {}", codec);
            }
        }

        private void acceptMp3(byte[] data) {
            if (decoderThread == null) {
                mp3Input = new BlockingByteStream();
                final BlockingByteStream input = mp3Input;
                decoderThread = new Thread(() -> decodeLoop(input, false), "mcradio-mp3-decoder-" + station);
                decoderThread.setDaemon(true);
                decoderThread.start();
            }
            mp3Input.offer(data);
        }

        private void acceptAnnouncement(byte[] data) {
            if (announcementThread == null) {
                announcementInput = new BlockingByteStream();
                final BlockingByteStream input = announcementInput;
                announcementThread = new Thread(() -> decodeLoop(input, true), "mcradio-announcement-decoder-" + station);
                announcementThread.setDaemon(true);
                announcementThread.start();
            }
            announcementInput.offer(data);
        }

        private void acceptVoice(byte[] data) {
            acceptVoice(data, 100);
        }

        /**
         * Expands a walkie voice chunk. Quality below 100 mixes in white noise; below 60 the voice is also
         * crushed to a hoarse, distorted rasp; at 0 only noise remains.
         */
        void acceptVoice(byte[] data, int quality) {
            if (closed) {
                return;
            }
            lastDataNanos = System.nanoTime();
            double clarity = Math.max(0, Math.min(100, quality)) / 100.0;
            java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
            short[] pcm = new short[data.length];
            for (int i = 0; i < data.length; i++) {
                double sample = UlawCodec.decode(data[i]) * clarity;
                if (clarity < 0.6) {
                    sample = Math.round(sample * 2.5 / 1024.0) * 1024.0;
                }
                sample += (random.nextDouble() * 2.0 - 1.0) * 7000.0 * (1.0 - clarity);
                pcm[i] = (short) Math.max(-32768, Math.min(32767, Math.round(sample)));
            }
            short[] resampled = voiceResampler.process(pcm, pcm.length, AudioCodec.VOICE_SAMPLE_RATE);
            push(resampled);
        }

        private void push(short[] mono) {
            if (mono.length == 0) {
                return;
            }
            for (PcmBuffer buffer : readers) {
                buffer.write(mono, mono.length);
            }
        }

        private void pushAnnouncement(short[] mono) {
            if (mono.length == 0) {
                return;
            }
            for (PcmBuffer buffer : announcementReaders) {
                buffer.write(mono, mono.length);
            }
        }

        private void decodeLoop(BlockingByteStream source, boolean announcement) {
            LinearResampler resampler = new LinearResampler(PcmBuffer.SAMPLE_RATE);
            Bitstream bitstream = null;
            try {
                bitstream = new Bitstream(source);
                Decoder decoder = new Decoder();
                int consecutiveErrors = 0;
                while (!closed) {
                    try {
                        Header header = bitstream.readFrame();
                        if (header == null) {
                            break;
                        }
                        SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                        int channels = Math.max(1, decoder.getOutputChannels());
                        short[] interleaved = samples.getBuffer();
                        int frames = samples.getBufferLength() / channels;
                        short[] mono = new short[frames];
                        for (int i = 0; i < frames; i++) {
                            int sum = 0;
                            for (int c = 0; c < channels; c++) {
                                sum += interleaved[i * channels + c];
                            }
                            mono[i] = (short) (sum / channels);
                        }
                        short[] pcm = resampler.process(mono, frames, decoder.getOutputFrequency());
                        if (announcement) {
                            pushAnnouncement(pcm);
                        } else {
                            push(pcm);
                        }
                        bitstream.closeFrame();
                        consecutiveErrors = 0;
                    } catch (JavaLayerException | RuntimeException e) {
                        if (closed || ++consecutiveErrors > 400) {
                            break;
                        }
                        bitstream.closeFrame();
                    }
                }
            } catch (RuntimeException e) {
                if (!closed) {
                    McRadio.LOGGER.debug("MP3 decoder for station {} stopped", station, e);
                }
            } finally {
                if (bitstream != null) {
                    try {
                        bitstream.close();
                    } catch (JavaLayerException ignored) {
                        // nothing left to release
                    }
                }
            }
        }

        void close() {
            closed = true;
            if (mp3Input != null) {
                mp3Input.close();
            }
            if (decoderThread != null) {
                decoderThread.interrupt();
            }
            if (announcementInput != null) {
                announcementInput.close();
            }
            if (announcementThread != null) {
                announcementThread.interrupt();
            }
            readers.clear();
            announcementReaders.clear();
        }
    }


    /**
     * Adapter that lets Minecraft's streaming sound channel pull PCM from a {@link ClientRadioStream}.
     * It never ends on its own: while no audio is available it hands out short silent chunks, so the OpenAL
     * source keeps running and playback resumes seamlessly when data arrives. Signal quality is applied here:
     * below 100 % the audio is attenuated and mixed with static and crackle, at 0 % only white noise remains
     * (radio silence). Horn loudspeakers additionally clip and bit-crush the sound.
     */
    static final class RadioAudioStream implements AudioStream {
        private static final AudioFormat FORMAT = new AudioFormat(PcmBuffer.SAMPLE_RATE, 16, 1, true,
                ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
        private static final double NOISE_AMPLITUDE = 7500.0;
        private static final int NUMBER_STATION_CHUNK = PcmBuffer.SAMPLE_RATE / 10;

        private final ClientRadioStream stream;
        private final PcmBuffer buffer;
        private final PcmBuffer announcementBuffer;
        private final short[] loop;
        private final IntSupplier quality;
        private final boolean distort;
        private int loopIndex;
        private volatile boolean closed;

        RadioAudioStream(ClientRadioStream stream, IntSupplier quality, boolean distort) {
            this.stream = stream;
            this.quality = quality;
            this.distort = distort;
            this.loop = stream.syntheticLoop();
            this.buffer = loop == null ? stream.attach() : null;
            this.announcementBuffer = loop == null ? stream.attachAnnouncement() : null;
        }

        @Override
        public AudioFormat getFormat() {
            return FORMAT;
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            if (closed || stream.isClosed()) {
                return ByteBuffer.allocateDirect(0);
            }
            int maxSamples = Math.max(1, size / 2);
            short[] samples = new short[Math.min(maxSamples, PcmBuffer.SAMPLE_RATE)];
            int count;
            if (loop != null) {
                count = Math.min(samples.length, NUMBER_STATION_CHUNK);
                for (int i = 0; i < count; i++) {
                    samples[i] = loop[loopIndex];
                    loopIndex = (loopIndex + 1) % loop.length;
                }
            } else {
                count = buffer.readChunk(samples, maxSamples);
                // mix the announcement channel over the (possibly ducked) music channel
                short[] announcement = new short[count];
                announcementBuffer.readChunk(announcement, count);
                float musicGain = stream.musicGain();
                float announcementGain = stream.announcementGain();
                for (int i = 0; i < count; i++) {
                    int mixed = Math.round(samples[i] * musicGain + announcement[i] * announcementGain);
                    samples[i] = (short) Math.max(-32768, Math.min(32767, mixed));
                }
            }
            applyEffects(samples, count);

            ByteBuffer out = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder());
            out.asShortBuffer().put(samples, 0, count);
            return out;
        }

        private void applyEffects(short[] samples, int count) {
            double clarity = Math.max(0, Math.min(100, quality.getAsInt())) / 100.0;
            double interference = 1.0 - clarity;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                double sample = samples[i];
                if (interference > 0.0) {
                    sample *= clarity;
                    sample += (random.nextDouble() * 2.0 - 1.0) * NOISE_AMPLITUDE * interference;
                    if (interference > 0.05 && random.nextInt(2500) == 0) {
                        sample += (random.nextBoolean() ? 1 : -1) * 14000.0 * interference;
                    }
                }
                if (distort) {
                    sample = Math.max(-24000.0, Math.min(24000.0, sample * 3.0));
                    sample = Math.round(sample / 1500.0) * 1500.0;
                }
                samples[i] = (short) Math.max(-32768, Math.min(32767, Math.round(sample)));
            }
        }

        @Override
        public void close() {
            closed = true;
            if (buffer != null) {
                stream.detach(buffer);
                buffer.clear();
            }
            if (announcementBuffer != null) {
                stream.detachAnnouncement(announcementBuffer);
                announcementBuffer.clear();
            }
        }
    }


    /**
     * A positional, tickable sound whose audio data does not come from a .ogg file but from a live
     * {@link ClientRadioStream}. The event (radio_stream, radio_stream_horn or radio_stream_speaker, which differ in
     * audible range) is declared in sounds.json (it points at
     * a vanilla placeholder file that is never actually read because {@link #getStream} supplies the data).
     */
    public static final class RadioSoundInstance extends AbstractTickableSoundInstance {
        private static final double STOP_DISTANCE_SQR = 64.0 * 64.0;

        private final ReceiverBlockEntity receiver;
        private final ClientRadioStream stream;

        public RadioSoundInstance(ReceiverBlockEntity receiver, ClientRadioStream stream) {
            super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID,
                    receiver.variant().soundName())), SoundSource.RECORDS, RandomSource.create());
            this.receiver = receiver;
            this.stream = stream;
            this.looping = false;
            this.delay = 0;
            this.volume = receiver.getVolume() / 100.0F;
            this.x = receiver.getBlockPos().getX() + 0.5;
            this.y = receiver.getBlockPos().getY() + 0.5;
            this.z = receiver.getBlockPos().getZ() + 0.5;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.completedFuture(
                    new RadioAudioStream(stream, receiver::getQuality, receiver.variant().distorted()));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (receiver.isRemoved() || !receiver.isReceiving() || stream.isClosed()
                    || receiver.getSourceStation() != stream.station()) {
                stop();
                return;
            }
            Player player = Minecraft.getInstance().player;
            if (player == null || player.distanceToSqr(x, y, z) > STOP_DISTANCE_SQR) {
                stop();
                return;
            }
            this.volume = receiver.getVolume() / 100.0F;
        }
    }


    /** Per-tick client logic of a receiver: starts a sound instance when it has a signal and the player is near. */
    public static final class ClientReceiverLogic {
        private static final double START_DISTANCE_SQR = 48.0 * 48.0;

        private ClientReceiverLogic() {
        }

        public static void tick(ReceiverBlockEntity receiver) {
            if (!receiver.isReceiving()) {
                receiver.clientSound = null;
                return;
            }
            if (receiver.clientSound instanceof RadioSoundInstance existing && !existing.isStopped()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || player.distanceToSqr(Vec3.atCenterOf(receiver.getBlockPos())) > START_DISTANCE_SQR) {
                return;
            }
            RadioSoundInstance instance = new RadioSoundInstance(receiver,
                    ClientAudioManager.sourceStream(receiver.getSourceStation(), receiver.getCipher()));
            receiver.clientSound = instance;
            minecraft.getSoundManager().play(instance);
        }
    }


    /**
     * Push-to-talk microphone capture: 16 kHz mono, mu-law encoded, handed out in 100 ms chunks to a sink.
     * The sink returns false to stop the capture.
     */
    public static final class MicCapture {
        private static final AudioFormat FORMAT = new AudioFormat(AudioCodec.VOICE_SAMPLE_RATE, 16, 1, true, false);
        private static final int PACKET_SAMPLES = AudioCodec.VOICE_SAMPLE_RATE / 10;

        private static volatile boolean running;

        private MicCapture() {
        }

        public static boolean isActive() {
            return running;
        }

        /** Captures for a distributor GUI: chunks are sent to the server as {@link LiveAudioPacket}s. */
        public static boolean start(BlockPos station) {
            BlockPos target = station.immutable();
            return begin(data -> {
                LiveAudioPacket packet = new LiveAudioPacket(target, data);
                Minecraft.getInstance().execute(() -> RadioNetwork.sendToServer(packet));
                return true;
            });
        }

        /** Captures for a custom sink (used by the walkie-talkie). Returns false if no microphone is available. */
        public static boolean start(Predicate<byte[]> sink) {
            return begin(sink);
        }

        private static synchronized boolean begin(Predicate<byte[]> sink) {
            if (running) {
                return true;
            }
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
                if (!AudioSystem.isLineSupported(info)) {
                    return false;
                }
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(FORMAT, PACKET_SAMPLES * 4);
                line.start();
                running = true;
                Thread thread = new Thread(() -> captureLoop(line, sink), "mcradio-microphone");
                thread.setDaemon(true);
                thread.start();
                return true;
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException | LinkageError e) {
                McRadio.LOGGER.warn("Microphone unavailable: {}", e.toString());
                running = false;
                return false;
            }
        }

        public static synchronized void stop() {
            running = false;
        }

        private static void captureLoop(TargetDataLine line, Predicate<byte[]> sink) {
            byte[] raw = new byte[PACKET_SAMPLES * 2];
            try {
                while (running) {
                    int read = line.read(raw, 0, raw.length);
                    if (read <= 0) {
                        continue;
                    }
                    int samples = read / 2;
                    byte[] encoded = new byte[samples];
                    for (int i = 0; i < samples; i++) {
                        short sample = (short) ((raw[i * 2] & 0xFF) | (raw[i * 2 + 1] << 8));
                        encoded[i] = UlawCodec.encode(sample);
                    }
                    if (!sink.test(encoded)) {
                        running = false;
                    }
                }
            } finally {
                line.stop();
                line.close();
                running = false;
            }
        }
    }


    /** Entry points from common item classes into client code (only ever called when the level is client-side). */
    public static final class ClientHooks {
        private ClientHooks() {
        }

        /** Frequency dialog of a walkie-talkie / field radio (transmitter) or of a pocket player. */
        public static void openTuner(InteractionHand hand, int frequency, boolean transmitter) {
            Minecraft.getInstance().setScreen(new WalkieScreen(hand, frequency, transmitter));
        }

        public static void openDecoder(InteractionHand hand) {
            Minecraft.getInstance().setScreen(new DecoderScreen(hand));
        }

        public static void openTablet() {
            Minecraft.getInstance().setScreen(new TabletScreen());
        }

        public static void walkieTick(ItemStack stack, Entity entity) {
            WalkieClientLogic.tick(stack, entity);
        }
    }


    /** Client-side push-to-talk handling for a held walkie-talkie. */
    public static final class WalkieClientLogic {
        public static final KeyMapping PUSH_TO_TALK = new KeyMapping("key.mcradio.push_to_talk",
                InputConstants.KEY_V, "key.categories.mcradio");

        private static final long HELD_GRACE_MILLIS = 500L;

        private static volatile long lastHeldMillis;
        private static boolean ownsMicrophone;

        private WalkieClientLogic() {
        }

        /** Called every client tick for every walkie-talkie stack in the local player's inventory. */
        public static void tick(ItemStack stack, Entity entity) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || entity != player) {
                return;
            }
            if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) {
                return;
            }
            boolean wantsToTalk = minecraft.screen == null && PUSH_TO_TALK.isDown();
            if (wantsToTalk) {
                lastHeldMillis = System.currentTimeMillis();
                if (!MicCapture.isActive()) {
                    ownsMicrophone = MicCapture.start(WalkieClientLogic::sendVoice);
                    if (!ownsMicrophone) {
                        player.displayClientMessage(Component.translatable("gui.mcradio.mic_unavailable"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.mcradio.transmitting",
                                WalkieTalkieItem.getFrequency(stack)), true);
                    }
                }
            } else if (ownsMicrophone) {
                ownsMicrophone = false;
                MicCapture.stop();
            }
        }

        private static boolean sendVoice(byte[] data) {
            if (System.currentTimeMillis() - lastHeldMillis > HELD_GRACE_MILLIS) {
                return false;
            }
            WalkieVoicePacket packet = new WalkieVoicePacket(data);
            Minecraft.getInstance().execute(() -> RadioNetwork.sendToServer(packet));
            return true;
        }
    }


    /** Non-positional sound of one walkie-talkie speaker, fed by that speaker's {@link ClientRadioStream}. */
    public static final class WalkieSoundInstance extends AbstractTickableSoundInstance {
        private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "radio_stream"));
        private static final long SILENCE_TIMEOUT_NANOS = 3_000_000_000L;

        private final UUID speaker;
        private final ClientRadioStream stream;

        public WalkieSoundInstance(UUID speaker, ClientRadioStream stream) {
            super(EVENT, SoundSource.VOICE, RandomSource.create());
            this.speaker = speaker;
            this.stream = stream;
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.completedFuture(new RadioAudioStream(stream, () -> 100, false));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (stream.isClosed() || System.nanoTime() - stream.lastDataNanos() > SILENCE_TIMEOUT_NANOS) {
                ClientAudioManager.endVoice(speaker);
                stop();
            }
        }
    }


    /** Non-positional sound of the held Pocket Player. */
    public static final class PocketSoundInstance extends AbstractTickableSoundInstance {
        private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "radio_stream"));

        private final ClientRadioStream stream;

        public PocketSoundInstance(ClientRadioStream stream) {
            super(EVENT, SoundSource.RECORDS, RandomSource.create());
            this.stream = stream;
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.completedFuture(
                    new RadioAudioStream(stream, ClientRadioState::pocketQuality, false));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        /** Stops the sound from outside the tick loop. */
        public void halt() {
            stop();
        }

        @Override
        public void tick() {
            if (stream.isClosed() || ClientRadioState.source() != stream.station()) {
                stop();
            }
        }
    }


    /** Client-side state of the held Pocket Player: what it picks up and how clearly (drives sound and needle). */
    public static final class ClientRadioState {
        private static long source;
        private static int quality;
        private static PocketSoundInstance instance;

        private ClientRadioState() {
        }

        /** 0-100 signal quality for the needle of the pocket player; 0 if nothing is received. */
        public static int pocketQuality() {
            return source == 0L ? 0 : quality;
        }

        static long source() {
            return source;
        }

        /** Applies the server's once-per-second report for the held pocket player. */
        public static void update(long newSource, int newQuality, String cipher) {
            quality = newQuality;
            boolean changed = newSource != source;
            source = newSource;
            if (newSource == 0L) {
                stopInstance();
                return;
            }
            if (changed || instance == null || instance.isStopped()) {
                stopInstance();
                instance = new PocketSoundInstance(ClientAudioManager.sourceStream(newSource, cipher));
                Minecraft.getInstance().getSoundManager().play(instance);
            }
        }

        public static void reset() {
            source = 0L;
            quality = 0;
            stopInstance();
        }

        private static void stopInstance() {
            if (instance != null) {
                instance.halt();
                instance = null;
            }
        }
    }


    /** Renders the Morse code of a cipher (digits and spaces) as one looping mono 44.1 kHz sample buffer. */
    static final class MorseSynth {
        private static final String[] DIGITS = {"-----", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----."};
        private static final int UNIT_SAMPLES = PcmBuffer.SAMPLE_RATE * 7 / 100;
        private static final double TONE_HZ = 620.0;
        private static final double AMPLITUDE = 9000.0;
        private static final int ENDING_PAUSE_UNITS = 60;

        private MorseSynth() {
        }

        private record Segment(boolean tone, int units) {
        }

        static short[] render(String cipher) {
            List<Segment> segments = new ArrayList<>();
            for (char c : cipher.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    String pattern = DIGITS[c - '0'];
                    for (char symbol : pattern.toCharArray()) {
                        segments.add(new Segment(true, symbol == '.' ? 1 : 3));
                        segments.add(new Segment(false, 1));
                    }
                    segments.add(new Segment(false, 2));
                } else if (c == ' ') {
                    segments.add(new Segment(false, 4));
                }
            }
            segments.add(new Segment(false, ENDING_PAUSE_UNITS));

            int total = 0;
            for (Segment segment : segments) {
                total += segment.units() * UNIT_SAMPLES;
            }
            short[] samples = new short[total];
            int position = 0;
            int ramp = PcmBuffer.SAMPLE_RATE / 200;
            for (Segment segment : segments) {
                int length = segment.units() * UNIT_SAMPLES;
                if (segment.tone()) {
                    for (int i = 0; i < length; i++) {
                        double envelope = Math.min(1.0, Math.min(i, length - 1 - i) / (double) ramp);
                        samples[position + i] = (short) Math.round(
                                Math.sin(2.0 * Math.PI * TONE_HZ * i / PcmBuffer.SAMPLE_RATE) * AMPLITUDE * envelope);
                    }
                }
                position += length;
            }
            return samples;
        }
    }


    /** Shared drawing helpers that give all radio GUIs the same dark "equipment rack" look (no texture files needed). */
    static final class RadioUi {
        static final int TEXT = 0xFFD8D8D8;
        static final int MUTED = 0xFF7E7E7E;
        /** Phosphor green of old oscilloscopes. */
        static final int ACCENT = 0xFF39FF88;
        static final int OK = 0xFF39FF88;
        static final int BAD = 0xFFFF4A3D;
        /** Amber indicator. */
        static final int WARN = 0xFFFFB000;

        private RadioUi() {
        }

        static void panel(GuiGraphics g, int x, int y, int w, int h, Component title, Font font) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A2A2A);
            g.fillGradient(x, y, x + w, y + h, 0xFF181818, 0xFF0B0B0B);
            // brushed-metal scratches
            for (int i = 0; i < 14; i++) {
                int sy = y + 26 + ((i * 53) % Math.max(1, h - 30));
                int sx = x + 4 + ((i * 97) % Math.max(1, w - 40));
                g.fill(sx, sy, sx + 10 + (i * 7) % 26, sy + 1, 0x14FFFFFF);
            }
            g.fill(x, y, x + w, y + 22, 0xFF111111);
            g.fill(x, y + 22, x + w, y + 23, 0xFF2A2A2A);
            g.fill(x, y + 23, x + w, y + 24, 0x6039FF88);
            g.drawString(font, title, x + 12, y + 7, ACCENT, false);
            for (int i = 0; i < 3; i++) {
                g.fill(x + w - 18 - i * 9, y + 8, x + w - 12 - i * 9, y + 14, i == 0 ? ACCENT : 0xFF2E2E2E);
            }
            screw(g, x + 3, y + h - 7);
            screw(g, x + w - 7, y + h - 7);
        }

        private static void screw(GuiGraphics g, int x, int y) {
            g.fill(x, y, x + 4, y + 4, 0xFF4A4A4A);
            g.fill(x + 1, y + 1, x + 3, y + 3, 0xFF1A1A1A);
        }

        static void inset(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, 0xFF000000);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF070A08);
            // oscilloscope scan lines
            for (int line = y + 2; line < y + h - 2; line += 3) {
                g.fill(x + 1, line, x + w - 1, line + 1, 0x1839FF88);
            }
        }

        static Button button(Component label, Button.OnPress onPress, int x, int y, int w, int h) {
            return new DarkButton(x, y, w, h, label, onPress);
        }

        static void led(GuiGraphics g, int x, int y, boolean on, int onColor) {
            g.fill(x, y, x + 8, y + 8, 0xFF000000);
            g.fill(x + 1, y + 1, x + 7, y + 7, on ? onColor : 0xFF262626);
            if (on) {
                g.fill(x + 2, y + 2, x + 4, y + 4, 0x80FFFFFF);
            }
        }

        /** Animated level meter, purely decorative. */
        static void equalizer(GuiGraphics g, int x, int y, int bars, boolean active) {
            long t = Util.getMillis();
            for (int i = 0; i < bars; i++) {
                double level = active ? 3 + 11 * Math.abs(Math.sin(t / 220.0 + i * 0.9) * Math.cos(t / 370.0 + i * 0.5)) : 2;
                int top = y + 14 - (int) level;
                g.fill(x + i * 5, top, x + i * 5 + 3, y + 14, active ? ACCENT : 0xFF262626);
            }
        }
    }


    /** Flat anthracite button with a phosphor-green frame on hover, matching the industrial GUI style. */
    static final class DarkButton extends Button {
        DarkButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hot = active && isHoveredOrFocused();
            int frame = !active ? 0xFF2A2A2A : hot ? RadioUi.ACCENT : 0xFF3A3A3A;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, frame);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, hot ? 0xFF1F1F1F : 0xFF151515);
            int color = !active ? 0xFF555555 : hot ? RadioUi.ACCENT : 0xFFD0D0D0;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, color);
        }
    }


    /** Jammer radius slider; the chosen radius is reported once, when the mouse button is released. */
    static final class RadiusSlider extends AbstractSliderButton {
        private final IntConsumer onCommit;

        RadiusSlider(int x, int y, int width, int height, int radius, IntConsumer onCommit) {
            super(x, y, width, height, Component.empty(), toValue(radius));
            this.onCommit = onCommit;
            updateMessage();
        }

        private static double toValue(int radius) {
            return (radius - JammerBlockEntity.MIN_RADIUS) / (double) (JammerBlockEntity.MAX_RADIUS - JammerBlockEntity.MIN_RADIUS);
        }

        int radius() {
            return JammerBlockEntity.MIN_RADIUS
                    + (int) Math.round(value * (JammerBlockEntity.MAX_RADIUS - JammerBlockEntity.MIN_RADIUS));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.mcradio.jammer.radius", radius()));
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            onCommit.accept(radius());
        }
    }


    /** GUI of the Signal Jammer: field radius and manual power switch (redstone also powers it). */
    public static final class JammerScreen extends RadioScreen {
        private int radius;
        private boolean enabled;

        public JammerScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 252, 170);
            this.radius = menu.data().a();
            this.enabled = menu.data().b() == 1;
        }

        @Override
        protected void buildWidgets() {
            addRenderableWidget(new RadiusSlider(left + 12, top + 36, panelWidth - 24, 20, radius, value -> {
                radius = value;
                send();
            }));
            addRenderableWidget(RadioUi.button(powerLabel(), button -> {
                enabled = !enabled;
                send();
                rebuildWidgets();
            }, left + 12, top + 62, panelWidth - 24, 20));
        }

        private Component powerLabel() {
            return Component.translatable(enabled ? "gui.mcradio.jammer.on" : "gui.mcradio.jammer.off");
        }

        private void send() {
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.JAMMER_SET, radius, enabled ? 1 : 0, ""));
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean active = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof JammerBlockEntity jammer
                    && jammer.isActive();
            RadioUi.led(graphics, left + 12, top + 96, active, RadioUi.BAD);
            graphics.drawString(font, Component.translatable(active ? "gui.mcradio.jammer.status.active" : "gui.mcradio.jammer.status.idle"),
                    left + 26, top + 97, active ? RadioUi.BAD : RadioUi.MUTED, false);
            RadioUi.equalizer(graphics, left + panelWidth - 12 - 60, top + 90, 12, active);
            graphics.drawString(font, Component.translatable("gui.mcradio.jammer.effect1"), left + 12, top + 114, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.jammer.effect2"), left + 12, top + 126, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.jammer.effect3"), left + 12, top + 138, RadioUi.MUTED, false);
        }
    }


    /** Radio Decoder: enter the number station's frequency and the digits heard, or capture from a nearby receiver. */
    public static final class DecoderScreen extends Screen {
        private static final int WIDTH = 270;
        private static final int HEIGHT = 190;

        private final InteractionHand hand;
        private EditBox frequencyBox;
        private EditBox cipherBox;
        private int left;
        private int top;

        public DecoderScreen(InteractionHand hand) {
            super(Component.translatable("item.mcradio.radio_decoder"));
            this.hand = hand;
        }

        @Override
        protected void init() {
            left = (width - WIDTH) / 2;
            top = (height - HEIGHT) / 2;
            frequencyBox = new EditBox(font, left + 100, top + 33, 56, 16, Component.translatable("gui.mcradio.frequency"));
            frequencyBox.setMaxLength(4);
            frequencyBox.setFilter(value -> value.matches("\\d{0,4}"));
            addRenderableWidget(frequencyBox);

            cipherBox = new EditBox(font, left + 100, top + 55, 150, 16, Component.translatable("gui.mcradio.decoder.cipher"));
            cipherBox.setMaxLength(11);
            cipherBox.setFilter(value -> value.matches("[0-9 ]{0,11}"));
            cipherBox.setHint(Component.literal("12345 67890"));
            addRenderableWidget(cipherBox);

            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.decoder.decode"), button -> send(false),
                    left + 12, top + 80, 120, 20));
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.decoder.capture"), button -> send(true),
                    left + WIDTH - 12 - 120, top + 80, 120, 20));
        }

        private void send(boolean capture) {
            int frequency = 0;
            try {
                frequency = RadioPhysics.clampFrequency(Integer.parseInt(frequencyBox.getValue().trim()));
            } catch (NumberFormatException e) {
                // frequency stays 0; the server answers "not a number station" unless capturing
            }
            RadioNetwork.sendToServer(new DecoderPacket(hand, frequency, cipherBox.getValue(), capture));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xC0000000, 0xE0000000);
            RadioUi.panel(graphics, left, top, WIDTH, HEIGHT, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 37, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.decoder.cipher"), left + 12, top + 59, RadioUi.TEXT, false);

            RadioUi.inset(graphics, left + 12, top + 108, WIDTH - 24, 70);
            graphics.drawString(font, Component.translatable("gui.mcradio.decoder.log"), left + 18, top + 112, RadioUi.WARN, false);
            ItemStack stack = minecraft != null && minecraft.player != null ? minecraft.player.getItemInHand(hand) : ItemStack.EMPTY;
            List<String> sites = DecoderItem.sites(stack);
            int shown = Math.min(sites.size(), 4);
            for (int i = 0; i < shown; i++) {
                String[] parts = sites.get(sites.size() - 1 - i).split("\\|");
                if (parts.length == 4) {
                    graphics.drawString(font, parts[0] + " kHz  >  X " + parts[1] + "  Z " + parts[2] + "  (" + parts[3].toLowerCase() + ")",
                            left + 18, top + 124 + i * 12, RadioUi.ACCENT, false);
                }
            }
            if (sites.isEmpty()) {
                graphics.drawString(font, Component.translatable("gui.mcradio.decoder.empty"), left + 18, top + 124, RadioUi.MUTED, false);
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                send(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }


    /** Base class of the three device screens: centred custom-drawn panel plus menu bookkeeping. */
    static abstract class RadioScreen extends Screen implements MenuAccess<RadioDeviceMenu> {
        protected final RadioDeviceMenu menu;
        protected final int panelWidth;
        protected final int panelHeight;
        protected int left;
        protected int top;

        protected RadioScreen(RadioDeviceMenu menu, Component title, int panelWidth, int panelHeight) {
            super(title);
            this.menu = menu;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
        }

        @Override
        public RadioDeviceMenu getMenu() {
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

        protected void onEnter() {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        /** The background is drawn in {@link #render}; this prevents the default blur from being applied twice. */
        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, panelWidth, panelHeight, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            drawForeground(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onEnter();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            } else {
                super.onClose();
            }
        }

        protected EditBox frequencyBox(int x, int y, int w, int initial) {
            EditBox box = new EditBox(font, x, y, w, 16, Component.translatable("gui.mcradio.frequency"));
            box.setMaxLength(4);
            box.setFilter(value -> value.matches("\\d{0,4}"));
            box.setValue(Integer.toString(initial));
            addRenderableWidget(box);
            return box;
        }

        protected static int parseFrequency(EditBox box, int fallback) {
            try {
                return RadioPhysics.clampFrequency(Integer.parseInt(box.getValue().trim()));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        protected static Minecraft mc() {
            return Minecraft.getInstance();
        }
    }


    /** Scrollable single-selection list of MP3 file names (mouse wheel + click). */
    static final class TrackListWidget extends AbstractWidget {
        private static final int ROW_HEIGHT = 14;

        private List<String> entries = List.of();
        private int selected = -1;
        private double scroll;

        TrackListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("gui.mcradio.tracks"));
        }

        void setEntries(List<String> newEntries, String preselect) {
            entries = List.copyOf(newEntries);
            selected = preselect == null ? -1 : entries.indexOf(preselect);
            scroll = Mth.clamp(scroll, 0, maxScroll());
        }

        int getSelectedIndex() {
            return selected >= 0 && selected < entries.size() ? selected : -1;
        }

        String getSelected() {
            return selected >= 0 && selected < entries.size() ? entries.get(selected) : null;
        }

        private int maxScroll() {
            return Math.max(0, entries.size() * ROW_HEIGHT - (height - 4));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            RadioUi.inset(graphics, getX(), getY(), width, height);
            if (entries.isEmpty()) {
                graphics.drawCenteredString(font, Component.translatable("gui.mcradio.no_tracks"),
                        getX() + width / 2, getY() + height / 2 - 4, RadioUi.MUTED);
                return;
            }
            graphics.enableScissor(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2);
            for (int i = 0; i < entries.size(); i++) {
                int rowY = getY() + 2 + i * ROW_HEIGHT - (int) scroll;
                if (rowY + ROW_HEIGHT < getY() || rowY > getY() + height) {
                    continue;
                }
                boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                        && mouseY >= getY() && mouseY < getY() + height;
                if (i == selected) {
                    graphics.fill(getX() + 2, rowY, getX() + width - 6, rowY + ROW_HEIGHT, 0xFF16421F);
                } else if (hovered) {
                    graphics.fill(getX() + 2, rowY, getX() + width - 6, rowY + ROW_HEIGHT, 0xFF1E1E1E);
                }
                String name = font.plainSubstrByWidth(entries.get(i), width - 18);
                graphics.drawString(font, name, getX() + 6, rowY + 3, i == selected ? RadioUi.TEXT : 0xFFB0B0B0, false);
            }
            graphics.disableScissor();

            int max = maxScroll();
            if (max > 0) {
                int trackHeight = height - 4;
                int thumb = Math.max(10, trackHeight * trackHeight / (entries.size() * ROW_HEIGHT));
                int thumbY = getY() + 2 + (int) ((trackHeight - thumb) * (scroll / max));
                graphics.fill(getX() + width - 5, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF050505);
                graphics.fill(getX() + width - 5, thumbY, getX() + width - 2, thumbY + thumb, RadioUi.ACCENT);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) {
                return false;
            }
            int index = (int) ((mouseY - getY() - 2 + scroll) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                selected = index;
                playDownSound(Minecraft.getInstance().getSoundManager());
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT, 0, maxScroll());
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }


    /** Push-to-talk button: active only while the mouse button is held down. */
    static final class HoldButton extends AbstractWidget {
        private final Component idleLabel;
        private final Component heldLabel;
        private final Runnable onPress;
        private final Runnable onRelease;
        private boolean held;

        HoldButton(int x, int y, int width, int height, Component idleLabel, Component heldLabel,
                   Runnable onPress, Runnable onRelease) {
            super(x, y, width, height, idleLabel);
            this.idleLabel = idleLabel;
            this.heldLabel = heldLabel;
            this.onPress = onPress;
            this.onRelease = onRelease;
        }

        boolean isHeld() {
            return held;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!held) {
                held = true;
                setMessage(heldLabel);
                onPress.run();
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            forceRelease();
        }

        /** Releases the button even if the mouse was let go outside of it. */
        void forceRelease() {
            if (held) {
                held = false;
                setMessage(idleLabel);
                onRelease.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = held ? RadioUi.BAD : RadioUi.ACCENT;
            int fill = held ? 0xFF8A1710 : (isHoveredOrFocused() ? 0xFF262626 : 0xFF181818);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, border);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, fill);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }


    /** 0-100 % slider; the new value is reported once, when the mouse button is released. */
    static final class VolumeSlider extends AbstractSliderButton {
        private final IntConsumer onCommit;

        VolumeSlider(int x, int y, int width, int height, int percent, IntConsumer onCommit) {
            super(x, y, width, height, Component.empty(), percent / 100.0);
            this.onCommit = onCommit;
            updateMessage();
        }

        int percent() {
            return (int) Math.round(value * 100.0);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.mcradio.volume", percent()));
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            onCommit.accept(percent());
        }
    }


    /**
     * GUI of the Signal Distributor and, identically, of a Computer linked to one: frequency, Playlist/Live
     * mode switch, track list, push-to-talk, text. An unlinked computer first shows a connection page.
     */
    public static final class StationScreen extends RadioScreen {
        private final boolean computer;
        private final boolean linked;

        private Mode mode;
        private int frequency;
        private boolean broadcasting;
        private String currentTrack;
        private final int radius;

        private List<String> tracks = List.of();
        private boolean tracksRequested;
        private boolean micFailed;

        private List<StationListPacket.Entry> stations = List.of();
        private boolean stationsRequested;

        private EditBox frequencyBox;
        private EditBox textBox;
        private TrackListWidget trackList;
        private TrackListWidget stationList;
        private HoldButton talkButton;

        public StationScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 268, 232);
            RadioDeviceMenu.DeviceData data = menu.data();
            this.computer = menu.getType() == ModRegistry.COMPUTER_MENU.get();
            this.linked = !computer || data.c() >= 0;
            this.frequency = data.a();
            this.mode = Mode.of(data.b());
            this.radius = Math.max(0, data.c());
            this.broadcasting = data.flag();
            this.currentTrack = data.text();
        }

        /** Called when the server's track list arrives. */
        public void setTracks(List<String> newTracks) {
            tracks = List.copyOf(newTracks);
            if (trackList != null) {
                trackList.setEntries(tracks, currentTrack);
            }
        }

        /** Called when the server's list of connectable distributors arrives (computer, not linked). */
        public void setStations(List<StationListPacket.Entry> newStations) {
            stations = List.copyOf(newStations);
            if (stationList != null) {
                stationList.setEntries(stationLabels(), null);
            }
        }

        private List<String> stationLabels() {
            return stations.stream()
                    .map(entry -> String.format(Locale.ROOT, "%d, %d, %d    %d kHz%s", entry.pos().getX(), entry.pos().getY(),
                            entry.pos().getZ(), entry.frequency(), entry.onAir() ? "    [ON AIR]" : ""))
                    .toList();
        }

        @Override
        protected void buildWidgets() {
            if (!linked) {
                buildLinkWidgets();
                return;
            }
            int innerWidth = panelWidth - 24;

            frequencyBox = frequencyBox(left + 90, top + 31, 56, frequency);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.apply"), button -> applyFrequency(), left + 152, top + 30, 56, 18));

            Component modeName = Component.translatable(mode == Mode.PLAYLIST ? "gui.mcradio.mode.playlist" : "gui.mcradio.mode.live");
            int modeWidth = computer ? innerWidth - 84 : innerWidth;
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.mode", modeName), button -> toggleMode(), left + 12, top + 54, modeWidth, 20));
            if (computer) {
                addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.disconnect"), button -> disconnect(), left + 12 + innerWidth - 80, top + 54, 80, 20));
            }

            if (mode == Mode.PLAYLIST) {
                buildPlaylistWidgets(innerWidth);
            } else {
                buildLiveWidgets(innerWidth);
            }
        }

        private void buildLinkWidgets() {
            int innerWidth = panelWidth - 24;
            stationList = new TrackListWidget(left + 12, top + 52, innerWidth, 112);
            stationList.setEntries(stationLabels(), null);
            addRenderableWidget(stationList);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.connect"), button -> connectSelected(), left + 12, top + 170, 110, 20));
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.refresh"), button -> requestStations(), left + panelWidth - 12 - 110, top + 170, 110, 20));
            if (!stationsRequested) {
                requestStations();
            }
        }

        private void buildPlaylistWidgets(int innerWidth) {
            trackList = new TrackListWidget(left + 12, top + 80, innerWidth, 84);
            trackList.setEntries(tracks, currentTrack);
            addRenderableWidget(trackList);

            int buttonWidth = (innerWidth - 8) / 3;
            int buttonY = top + 170;
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.broadcast"), button -> broadcastSelected(), left + 12, buttonY, buttonWidth, 20));
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.stop"), button -> stopBroadcast(), left + 16 + buttonWidth, buttonY, buttonWidth, 20));
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.refresh"), button -> requestTracks(), left + 20 + buttonWidth * 2, buttonY, buttonWidth, 20));

            if (!tracksRequested) {
                requestTracks();
            }
        }

        private void buildLiveWidgets(int innerWidth) {
            trackList = null;
            talkButton = new HoldButton(left + 12, top + 84, innerWidth, 34,
                    Component.translatable("gui.mcradio.hold_to_talk"), Component.translatable("gui.mcradio.talking"),
                    () -> micFailed = !MicCapture.start(menu.pos()), MicCapture::stop);
            addRenderableWidget(talkButton);

            textBox = new EditBox(font, left + 12, top + 130, innerWidth - 66, 18, Component.translatable("gui.mcradio.text_hint"));
            textBox.setMaxLength(120);
            textBox.setHint(Component.translatable("gui.mcradio.text_hint"));
            addRenderableWidget(textBox);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.send"), button -> sendText(), left + panelWidth - 12 - 60, top + 129, 60, 20));
        }

        private void requestTracks() {
            tracksRequested = true;
            RadioNetwork.sendToServer(new TrackListRequestPacket());
        }

        private void requestStations() {
            stationsRequested = true;
            RadioNetwork.sendToServer(new StationListRequestPacket());
        }

        private void connectSelected() {
            int index = stationList == null ? -1 : stationList.getSelectedIndex();
            if (index < 0 || index >= stations.size()) {
                return;
            }
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.COMPUTER_LINK, 0, 0,
                    Long.toString(stations.get(index).pos().asLong())));
        }

        private void disconnect() {
            MicCapture.stop();
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.COMPUTER_UNLINK, 0, 0, ""));
        }

        private void applyFrequency() {
            frequency = parseFrequency(frequencyBox, frequency);
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_APPLY, frequency, mode.ordinal(), ""));
        }

        private void toggleMode() {
            MicCapture.stop();
            mode = mode == Mode.PLAYLIST ? Mode.LIVE : Mode.PLAYLIST;
            broadcasting = false;
            frequency = parseFrequency(frequencyBox, frequency);
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_APPLY, frequency, mode.ordinal(), ""));
            rebuildWidgets();
        }

        private void broadcastSelected() {
            String selected = trackList == null ? null : trackList.getSelected();
            if (selected == null) {
                return;
            }
            currentTrack = selected;
            broadcasting = true;
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_BROADCAST, 0, 0, selected));
        }

        private void stopBroadcast() {
            broadcasting = false;
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_STOP, 0, 0, ""));
        }

        private void sendText() {
            String message = textBox.getValue().strip();
            if (message.isEmpty()) {
                return;
            }
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_TEXT, 0, 0, message));
            textBox.setValue("");
        }

        @Override
        protected void onEnter() {
            if (frequencyBox != null && frequencyBox.isFocused()) {
                applyFrequency();
            } else if (textBox != null && textBox.isFocused()) {
                sendText();
            }
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!linked) {
                graphics.drawString(font, Component.translatable("gui.mcradio.computer.not_connected"), left + 12, top + 34, RadioUi.WARN, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.computer.hint"), left + 12, top + 198, RadioUi.MUTED, false);
                return;
            }
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 35, RadioUi.TEXT, false);
            Component range = Component.translatable("gui.mcradio.range", radius);
            graphics.drawString(font, range, left + panelWidth - 12 - font.width(range), top + 35, RadioUi.MUTED, false);

            boolean onAir = mode == Mode.LIVE || broadcasting;
            RadioUi.led(graphics, left + 12, top + 204, onAir, RadioUi.OK);
            Component status;
            if (mode == Mode.LIVE) {
                status = micFailed ? Component.translatable("gui.mcradio.mic_unavailable")
                        : Component.translatable("gui.mcradio.status.live", frequency);
            } else if (broadcasting) {
                status = Component.literal(font.plainSubstrByWidth(
                        Component.translatable("gui.mcradio.status.onair", currentTrack).getString(), 150));
            } else {
                status = Component.translatable("gui.mcradio.status.idle");
            }
            graphics.drawString(font, status, left + 26, top + 205, micFailed && mode == Mode.LIVE ? RadioUi.BAD : RadioUi.TEXT, false);
            RadioUi.equalizer(graphics, left + panelWidth - 12 - 60, top + 198, 12,
                    onAir && (mode == Mode.PLAYLIST || (talkButton != null && talkButton.isHeld())));

            if (mode == Mode.LIVE) {
                graphics.drawString(font, Component.translatable("gui.mcradio.live_hint"), left + 12, top + 156, RadioUi.MUTED, false);
            } else {
                graphics.drawString(font, Component.translatable("gui.mcradio.folder_hint",
                        TrackLibrary.folder().getParent().getFileName() + "/" + TrackLibrary.folder().getFileName()),
                        left + 12, top + 218, RadioUi.MUTED, false);
            }
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (talkButton != null) {
                talkButton.forceRelease();
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void removed() {
            MicCapture.stop();
            super.removed();
        }
    }


    /** GUI of the Radio Receiver: tuned frequency, volume, and live signal indicator. */
    public static final class ReceiverScreen extends RadioScreen {
        private int frequency;
        private int volume;
        private EditBox frequencyBox;
        private EditBox passwordBox;

        public ReceiverScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 240, 178);
            this.frequency = menu.data().a();
            this.volume = menu.data().b();
        }

        @Override
        protected void buildWidgets() {
            frequencyBox = frequencyBox(left + 90, top + 35, 56, frequency);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.tune"), button -> send(), left + 152, top + 34, 76, 18));
            addRenderableWidget(new VolumeSlider(left + 12, top + 62, panelWidth - 24, 20, volume, percent -> {
                volume = percent;
                send();
            }));
            passwordBox = new EditBox(font, left + 66, top + 144, 130, 16, Component.translatable("gui.mcradio.v4.password"));
            passwordBox.setMaxLength(24);
            passwordBox.setFormatter((text, offset) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(text.length()),
                    net.minecraft.network.chat.Style.EMPTY));
            passwordBox.setHint(Component.translatable("gui.mcradio.v4.password_hint"));
            addRenderableWidget(passwordBox);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.v4.password_set"), button ->
                    RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.RECEIVER_PASSWORD, 0, 0,
                            passwordBox.getValue())), left + 200, top + 143, 28, 18));
        }

        private void send() {
            frequency = parseFrequency(frequencyBox, frequency);
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.RECEIVER_SET, frequency, volume, ""));
        }

        @Override
        protected void onEnter() {
            send();
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 39, RadioUi.TEXT, false);

            boolean locked = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof ReceiverBlockEntity receiver
                    && receiver.isReceiving();
            RadioUi.led(graphics, left + 12, top + 96, locked, RadioUi.OK);
            graphics.drawString(font, Component.translatable(locked ? "gui.mcradio.signal.locked" : "gui.mcradio.signal.none"),
                    left + 26, top + 97, locked ? RadioUi.OK : RadioUi.WARN, false);
            RadioUi.equalizer(graphics, left + panelWidth - 12 - 60, top + 90, 12, locked);
            boolean encrypted = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof ReceiverBlockEntity rx
                    && rx.isEncrypted();
            if (encrypted) {
                graphics.drawString(font, Component.translatable("gui.mcradio.v4.encrypted"), left + 12, top + 118, RadioUi.BAD, false);
            } else {
                graphics.drawString(font, Component.translatable("gui.mcradio.receiver_hint"), left + 12, top + 118, RadioUi.MUTED, false);
            }
            graphics.drawString(font, Component.translatable("gui.mcradio.v4.password"), left + 12, top + 148, RadioUi.TEXT, false);
        }
    }


    /** GUI of the Signal Repeater: input frequency to catch, output frequency to re-emit on. */
    public static final class RepeaterScreen extends RadioScreen {
        private int inputFrequency;
        private int outputFrequency;
        private final int radius;
        private EditBox inputBox;
        private EditBox outputBox;

        public RepeaterScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 240, 154);
            this.inputFrequency = menu.data().a();
            this.outputFrequency = menu.data().b();
            this.radius = menu.data().c();
        }

        @Override
        protected void buildWidgets() {
            inputBox = frequencyBox(left + 140, top + 33, 60, inputFrequency);
            outputBox = frequencyBox(left + 140, top + 57, 60, outputFrequency);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.apply"), button -> send(), left + 12, top + 84, panelWidth - 24, 20));
        }

        private void send() {
            inputFrequency = parseFrequency(inputBox, inputFrequency);
            outputFrequency = parseFrequency(outputBox, outputFrequency);
            inputBox.setValue(Integer.toString(inputFrequency));
            outputBox.setValue(Integer.toString(outputFrequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.REPEATER_SET,
                    inputFrequency, outputFrequency, ""));
        }

        @Override
        protected void onEnter() {
            send();
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(font, Component.translatable("gui.mcradio.input_frequency"), left + 12, top + 37, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.output_frequency"), left + 12, top + 61, RadioUi.TEXT, false);

            boolean active = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof RepeaterBlockEntity repeater
                    && repeater.isActive();
            RadioUi.led(graphics, left + 12, top + 114, active, RadioUi.OK);
            graphics.drawString(font, Component.translatable(active ? "gui.mcradio.repeater.relaying" : "gui.mcradio.repeater.waiting"),
                    left + 26, top + 115, active ? RadioUi.OK : RadioUi.WARN, false);
            Component range = Component.translatable("gui.mcradio.range", radius);
            graphics.drawString(font, range, left + panelWidth - 12 - font.width(range), top + 115, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.repeater_hint"), left + 12, top + 134, RadioUi.MUTED, false);
        }
    }


    /** Frequency dialog of the walkie-talkie item. */
    public static final class WalkieScreen extends Screen {
        private static final int WIDTH = 252;
        private static final int HEIGHT = 128;

        private final InteractionHand hand;
        private final boolean transmitter;
        private int frequency;
        private EditBox frequencyBox;
        private int left;
        private int top;

        /** @param transmitter true for walkie-talkies and field radios, false for the receive-only pocket player */
        public WalkieScreen(InteractionHand hand, int frequency, boolean transmitter) {
            super(Component.translatable(transmitter ? "item.mcradio.walkie_talkie" : "item.mcradio.pocket_player"));
            this.transmitter = transmitter;
            this.hand = hand;
            this.frequency = frequency;
        }

        @Override
        protected void init() {
            left = (width - WIDTH) / 2;
            top = (height - HEIGHT) / 2;
            frequencyBox = new EditBox(font, left + 90, top + 35, 56, 16, Component.translatable("gui.mcradio.frequency"));
            frequencyBox.setMaxLength(4);
            frequencyBox.setFilter(value -> value.matches("\\d{0,4}"));
            frequencyBox.setValue(Integer.toString(frequency));
            addRenderableWidget(frequencyBox);
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.tune"), button -> apply(), left + 152, top + 34, 88, 18));
        }

        private void apply() {
            try {
                frequency = RadioPhysics.clampFrequency(Integer.parseInt(frequencyBox.getValue().trim()));
            } catch (NumberFormatException e) {
                // keep the previous frequency
            }
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new WalkieConfigPacket(hand, frequency));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, WIDTH, HEIGHT, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 39, RadioUi.TEXT, false);
            if (transmitter) {
                graphics.drawString(font, Component.translatable("gui.mcradio.walkie.hint",
                        WalkieClientLogic.PUSH_TO_TALK.getTranslatedKeyMessage()), left + 12, top + 66, RadioUi.OK, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.walkie.range", RadioPhysics.WALKIE_RANGE),
                        left + 12, top + 82, RadioUi.MUTED, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.walkie.repeater"),
                        left + 12, top + 96, RadioUi.MUTED, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.walkie.warning"),
                        left + 12, top + 110, RadioUi.WARN, false);
            } else {
                graphics.drawString(font, Component.translatable("gui.mcradio.pocket.hint1"), left + 12, top + 66, RadioUi.OK, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.pocket.hint2"), left + 12, top + 82, RadioUi.MUTED, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.pocket.hint3"), left + 12, top + 96, RadioUi.MUTED, false);
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                apply();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }


    /**
     * Radio Tablet: a top-down map of the loaded terrain with every repeater, distributor, receiver and jammer
     * the server knows about, and their coverage areas (colour = frequency). Drag to pan, wheel to zoom.
     * Terrain is only drawn where chunks are loaded on the client; devices are listed by the server for the
     * whole dimension as long as their chunks are loaded there.
     */
    public static final class TabletScreen extends Screen {
        private static final int CELLS = 56;
        private static final int CELL_PX = 4;
        private static final int MAP_PX = CELLS * CELL_PX;
        private static final int[] BLOCKS_PER_CELL = {2, 4, 8, 16, 32};
        private static final int PANEL_WIDTH = 346;
        private static final int PANEL_HEIGHT = 272;
        private static final int UNKNOWN_TERRAIN = 0xFF0B0F15;
        private static final int[] KIND_COLORS = {0xFF39FF88, 0xFFFFB000, 0xFFD8D8D8, 0xFFFF4A3D};

        private final int[] terrain = new int[CELLS * CELLS];
        private List<MapDataPacket.Node> nodes = List.of();
        private int zoom = 2;
        private double centerX;
        private double centerZ;
        private int left;
        private int top;
        private int mapX;
        private int mapY;
        private int ticks;
        private int filter;
        private EditBox filterBox;

        public TabletScreen() {
            super(Component.translatable("item.mcradio.radio_tablet"));
            centerOnPlayer();
        }

        public void setNodes(List<MapDataPacket.Node> newNodes) {
            nodes = List.copyOf(newNodes);
        }

        private void centerOnPlayer() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                centerX = player.getX();
                centerZ = player.getZ();
            }
        }

        @Override
        protected void init() {
            left = (width - PANEL_WIDTH) / 2;
            top = (height - PANEL_HEIGHT) / 2;
            mapX = left + 12;
            mapY = top + 32;
            int columnX = left + 246;

            filterBox = new EditBox(font, columnX, top + 50, 88, 16, Component.translatable("gui.mcradio.map.filter"));
            filterBox.setMaxLength(4);
            filterBox.setFilter(value -> value.matches("\\d{0,4}"));
            filterBox.setValue(filter > 0 ? Integer.toString(filter) : "");
            filterBox.setResponder(value -> {
                try {
                    filter = value.isEmpty() ? 0 : Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    filter = 0;
                }
            });
            addRenderableWidget(filterBox);

            addRenderableWidget(RadioUi.button(Component.literal("+"), button -> changeZoom(-1), columnX, top + 74, 42, 18));
            addRenderableWidget(RadioUi.button(Component.literal("-"), button -> changeZoom(1), columnX + 46, top + 74, 42, 18));
            addRenderableWidget(RadioUi.button(Component.translatable("gui.mcradio.map.center"), button -> {
                centerOnPlayer();
                resample();
            }, columnX, top + 96, 88, 18));

            resample();
            requestNodes();
        }

        private void changeZoom(int delta) {
            zoom = Mth.clamp(zoom + delta, 0, BLOCKS_PER_CELL.length - 1);
            resample();
        }

        private void requestNodes() {
            RadioNetwork.sendToServer(new MapRequestPacket());
        }

        @Override
        public void tick() {
            ticks++;
            if (ticks % 20 == 0) {
                resample();
            }
            if (ticks % 100 == 0) {
                requestNodes();
            }
        }

        // ------------------------------------------------------------------ terrain

        private void resample() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            int scale = BLOCKS_PER_CELL[zoom];
            int half = CELLS / 2;
            int originX = (int) Math.floor(centerX);
            int originZ = (int) Math.floor(centerZ);
            for (int cz = 0; cz < CELLS; cz++) {
                for (int cx = 0; cx < CELLS; cx++) {
                    terrain[cz * CELLS + cx] = sample(level, originX + (cx - half) * scale, originZ + (cz - half) * scale);
                }
            }
        }

        private static int sample(ClientLevel level, int x, int z) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) {
                return UNKNOWN_TERRAIN;
            }
            int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            BlockPos pos = new BlockPos(x, height - 1, z);
            BlockState state = chunk.getBlockState(pos);
            MapColor color = state.getMapColor(level, pos);
            if (color == MapColor.NONE) {
                return UNKNOWN_TERRAIN;
            }
            double shade = 0.72 + Mth.clamp((height - level.getSeaLevel()) / 140.0, -0.25, 0.4);
            int red = Mth.clamp((int) (((color.col >> 16) & 0xFF) * shade), 0, 255);
            int green = Mth.clamp((int) (((color.col >> 8) & 0xFF) * shade), 0, 255);
            int blue = Mth.clamp((int) ((color.col & 0xFF) * shade), 0, 255);
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        // ------------------------------------------------------------------ coordinates

        private double toScreenX(double worldX) {
            return mapX + MAP_PX / 2.0 + (worldX - centerX) * CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private double toScreenY(double worldZ) {
            return mapY + MAP_PX / 2.0 + (worldZ - centerZ) * CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private double pixelsPerBlock() {
            return (double) CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private boolean insideMap(double x, double y) {
            return x >= mapX && x < mapX + MAP_PX && y >= mapY && y < mapY + MAP_PX;
        }

        // ------------------------------------------------------------------ rendering

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        private boolean visible(MapDataPacket.Node node) {
            if (filter <= 0) {
                return true;
            }
            return switch (node.kind()) {
                case MapDataPacket.KIND_REPEATER -> node.a() == filter || node.b() == filter;
                case MapDataPacket.KIND_JAMMER -> true;
                default -> node.a() == filter;
            };
        }

        private static int frequencyColor(int frequency) {
            return Mth.hsvToRgb((frequency * 37 % 360) / 360.0F, 0.65F, 1.0F);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, title, font);
            RadioUi.inset(graphics, mapX - 2, mapY - 2, MAP_PX + 4, MAP_PX + 4);

            graphics.enableScissor(mapX, mapY, mapX + MAP_PX, mapY + MAP_PX);
            for (int cz = 0; cz < CELLS; cz++) {
                for (int cx = 0; cx < CELLS; cx++) {
                    int x = mapX + cx * CELL_PX;
                    int y = mapY + cz * CELL_PX;
                    graphics.fill(x, y, x + CELL_PX, y + CELL_PX, terrain[cz * CELLS + cx]);
                }
            }

            List<MapDataPacket.Node> shown = new ArrayList<>();
            for (MapDataPacket.Node node : nodes) {
                if (visible(node)) {
                    shown.add(node);
                }
            }
            for (MapDataPacket.Node node : shown) {
                boolean coverage = node.kind() == MapDataPacket.KIND_DISTRIBUTOR || node.kind() == MapDataPacket.KIND_REPEATER
                        || node.kind() == MapDataPacket.KIND_JAMMER;
                if (node.active() && coverage) {
                    double cx = toScreenX(node.pos().getX() + 0.5);
                    double cy = toScreenY(node.pos().getZ() + 0.5);
                    if (node.kind() == MapDataPacket.KIND_JAMMER) {
                        drawJammerField(graphics, cx, cy, node.radius() * pixelsPerBlock());
                    } else {
                        drawCoverage(graphics, cx, cy, node.radius() * pixelsPerBlock(),
                                frequencyColor(node.kind() == MapDataPacket.KIND_REPEATER ? node.b() : node.a()));
                    }
                } else if (node.kind() == MapDataPacket.KIND_JAMMER) {
                    drawRing(graphics, toScreenX(node.pos().getX() + 0.5), toScreenY(node.pos().getZ() + 0.5),
                            node.radius() * pixelsPerBlock(), 0x66FF4A3D);
                }
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                double px = toScreenX(player.getX());
                double py = toScreenY(player.getZ());
                drawRing(graphics, px, py, RadioPhysics.WALKIE_RANGE * pixelsPerBlock(), 0xCCFFFFFF);
                MapMarker.draw(graphics, px, py, 0xFF39FF88, 5);
            }

            MapDataPacket.Node hovered = null;
            for (MapDataPacket.Node node : shown) {
                double sx = toScreenX(node.pos().getX() + 0.5);
                double sy = toScreenY(node.pos().getZ() + 0.5);
                int color = KIND_COLORS[Mth.clamp(node.kind(), 0, KIND_COLORS.length - 1)];
                if (!node.active()) {
                    color = (color & 0x00FFFFFF) | 0x80000000;
                }
                MapMarker.draw(graphics, sx, sy, color, node.kind() == MapDataPacket.KIND_REPEATER ? 6 : 5);
                if (insideMap(mouseX, mouseY) && Math.abs(mouseX - sx) <= 4 && Math.abs(mouseY - sy) <= 4) {
                    hovered = node;
                }
            }
            graphics.drawString(font, "N", mapX + MAP_PX / 2 - 2, mapY + 3, RadioUi.TEXT, true);
            graphics.disableScissor();

            super.render(graphics, mouseX, mouseY, partialTick);

            int columnX = left + 246;
            graphics.drawString(font, Component.translatable("gui.mcradio.map.filter"), columnX, top + 38, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.scale", BLOCKS_PER_CELL[zoom] * CELLS),
                    columnX, top + 120, RadioUi.MUTED, false);
            String[] legend = {"gui.mcradio.map.legend.distributor", "gui.mcradio.map.legend.repeater",
                    "gui.mcradio.map.legend.receiver", "gui.mcradio.map.legend.jammer"};
            for (int i = 0; i < legend.length; i++) {
                MapMarker.draw(graphics, columnX + 4, top + 146 + i * 14 + 4, KIND_COLORS[i], 5);
                graphics.drawString(font, Component.translatable(legend[i]), columnX + 14, top + 146 + i * 14, RadioUi.TEXT, false);
            }
            MapMarker.draw(graphics, columnX + 4, top + 146 + 4 * 14 + 4, 0xFF39FF88, 5);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.legend.you"), columnX + 14, top + 146 + 4 * 14, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.legend.walkie", RadioPhysics.WALKIE_RANGE),
                    columnX, top + 146 + 5 * 14, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.hint"), columnX, top + 146 + 6 * 14 + 4, RadioUi.MUTED, false);

            if (hovered != null) {
                graphics.renderComponentTooltip(font, describe(hovered), mouseX, mouseY);
            }
        }

        private static List<Component> describe(MapDataPacket.Node node) {
            List<Component> lines = new ArrayList<>();
            switch (node.kind()) {
                case MapDataPacket.KIND_DISTRIBUTOR -> lines.add(Component.translatable("gui.mcradio.map.distributor", node.a(), node.radius()));
                case MapDataPacket.KIND_REPEATER -> lines.add(Component.translatable("gui.mcradio.map.repeater", node.a(), node.b(), node.radius()));
                case MapDataPacket.KIND_RECEIVER -> lines.add(Component.translatable("gui.mcradio.map.receiver", node.a()));
                default -> lines.add(Component.translatable("gui.mcradio.map.jammer", node.radius()));
            }
            lines.add(Component.literal(node.pos().getX() + ", " + node.pos().getY() + ", " + node.pos().getZ()));
            lines.add(Component.translatable(node.active() ? "gui.mcradio.map.active" : "gui.mcradio.map.inactive"));
            return lines;
        }

        /** Translucent coverage disc plus an outline. */
        private void drawCoverage(GuiGraphics graphics, double cx, double cy, double radius, int rgb) {
            int r = (int) Math.round(radius);
            if (r < 1) {
                return;
            }
            int fromY = Math.max(-r, mapY - (int) cy);
            int toY = Math.min(r, mapY + MAP_PX - (int) cy);
            for (int dy = fromY; dy <= toY; dy++) {
                int halfWidth = (int) Math.sqrt((double) r * r - (double) dy * dy);
                graphics.fill((int) cx - halfWidth, (int) cy + dy, (int) cx + halfWidth, (int) cy + dy + 1, 0x30000000 | rgb);
            }
            drawRing(graphics, cx, cy, radius, 0xFF000000 | rgb);
        }

        /** Active jammer: a dark red field that pulses, brighter towards the centre. */
        private void drawJammerField(GuiGraphics graphics, double cx, double cy, double radius) {
            double pulse = 0.5 + 0.5 * Math.sin(Util.getMillis() / 260.0);
            int fromRing = Math.max(1, (int) Math.round(radius));
            for (int layer = 0; layer < 3; layer++) {
                double scale = 1.0 - layer * 0.28;
                int r = (int) Math.round(fromRing * scale);
                int alpha = (int) ((0x10 + layer * 0x10) * (0.6 + 0.8 * pulse));
                int top = Math.max(-r, mapY - (int) cy);
                int bottom = Math.min(r, mapY + MAP_PX - (int) cy);
                for (int dy = top; dy <= bottom; dy++) {
                    int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
                    graphics.fill((int) cx - half, (int) cy + dy, (int) cx + half, (int) cy + dy + 1,
                            (Mth.clamp(alpha, 0, 255) << 24) | 0xE02010);
                }
            }
            int ringAlpha = (int) (0x90 + 0x6F * pulse);
            drawRing(graphics, cx, cy, radius * (0.9 + 0.1 * pulse), (ringAlpha << 24) | 0xFF3020);
        }

        private void drawRing(GuiGraphics graphics, double cx, double cy, double radius, int argb) {
            int steps = (int) Mth.clamp(radius * 0.8, 24, 160);
            for (int i = 0; i < steps; i++) {
                double angle = i * Math.PI * 2.0 / steps;
                int x = (int) Math.round(cx + Math.cos(angle) * radius);
                int y = (int) Math.round(cy + Math.sin(angle) * radius);
                graphics.fill(x, y, x + 2, y + 2, argb);
            }
        }

        // ------------------------------------------------------------------ input

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 0 && insideMap(mouseX, mouseY)) {
                centerX -= dragX / pixelsPerBlock();
                centerZ -= dragY / pixelsPerBlock();
                resample();
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (insideMap(mouseX, mouseY) && scrollY != 0) {
                changeZoom(scrollY > 0 ? -1 : 1);
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        /** Small filled square used for every device icon. */
        private static final class MapMarker {
            private MapMarker() {
            }

            static void draw(GuiGraphics graphics, double x, double y, int argb, int size) {
                int half = size / 2;
                int ix = (int) Math.round(x);
                int iy = (int) Math.round(y);
                graphics.fill(ix - half - 1, iy - half - 1, ix + half + 2, iy + half + 2, 0xFF05080C);
                graphics.fill(ix - half, iy - half, ix + half + 1, iy + half + 1, argb);
            }
        }
    }

}
