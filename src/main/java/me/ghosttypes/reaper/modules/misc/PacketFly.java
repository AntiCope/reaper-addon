package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.SystemTimer;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PacketFly extends ReaperModule {
    public enum PacketFlyType {
        FACTOR,
        SETBACK,
        FAST,
        SLOW,
        ELYTRA,
        DESYNC,
        GHOST
    }

    public enum PacketFlyMode {
        PRESERVE,
        UP,
        DOWN,
        LIMITJITTER,
        BYPASS,
        OBSCURE
    }

    public enum PacketFlyBypass {
        NONE,
        DEFAULT,
        NCP
    }

    public enum PacketFlyPhase {
        NONE,
        VANILLA,
        NCP
    }

    public enum PacketFlyAntiKick {
        NONE,
        NORMAL,
        LIMITED,
        STRICT
    }

    public enum PacketFlylimit {
        NONE,
        STRONG,
        STRICT
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speeds");
    private final SettingGroup sgAntiCheat = settings.createGroup("AntiCheat");
    private final SettingGroup sgPhase = settings.createGroup("Phase");
    private final SettingGroup sgKeybind = settings.createGroup("Keybinds");

    // general
    private final Setting<PacketFlyType> type = sgGeneral.add(new EnumSetting.Builder<PacketFlyType>().name("fly-type").description("How the player is moved.").defaultValue(PacketFlyType.FACTOR).onChanged(this::updateFlying).build());
    private final Setting<PacketFlyMode> packetMode = sgGeneral.add(new EnumSetting.Builder<PacketFlyMode>().name("packet-mode").description("Which mode to use for sending packets.").defaultValue(PacketFlyMode.DOWN).build());
    private final Setting<PacketFlyBypass> bypass = sgGeneral.add(new EnumSetting.Builder<PacketFlyBypass>().name("bypass-mode").description("Which bypass mode to use.").defaultValue(PacketFlyBypass.NONE).build());
    private final Setting<Boolean> onlyOnMove = sgGeneral.add(new BoolSetting.Builder().name("only-on-move").description("Stops sending packets if you stand still").defaultValue(true).build());
    private final Setting<Boolean> stopOnGround = sgGeneral.add(new BoolSetting.Builder().name("stop-on-ground").description("Stops sending packets if you're on the ground").defaultValue(true).build());
    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder().name("strict").description("Can improve vertical movement").defaultValue(false).build());
    private final Setting<Boolean> bounds = sgGeneral.add(new BoolSetting.Builder().name("bounds").description("Set bounds for the packets sent").defaultValue(true).build());
    private final Setting<Boolean> multiAxis = sgGeneral.add(new BoolSetting.Builder().name("multi-axis").description("Allow sending packets in any direction").defaultValue(true).build());
    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder().name("toggle").description("Automatically disable").defaultValue(true).build());

    // Fly
    private final Setting<Double> factor = sgSpeed.add(new DoubleSetting.Builder().name("factor").description("How many packets are sent per iteration").defaultValue(5).min(0).visible(() -> type.get() == PacketFlyType.FACTOR || type.get() == PacketFlyType.DESYNC).build());
    private final Setting<Keybind> factorize = sgSpeed.add(new KeybindSetting.Builder().name("factorize").description("Quickly moves you on keybind").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Boolean> boost = sgSpeed.add(new BoolSetting.Builder().name("boost").description("Boosts player motion").defaultValue(false).build());
    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder().name("speed").description("How fast each packet moves you").defaultValue(1).min(0).build());
    private final Setting<Double> motion = sgSpeed.add(new DoubleSetting.Builder().name("factorize-motion").description("How far you move when factorize is enabled.").defaultValue(100).min(0).sliderMin(50).sliderMax(200).visible(() -> type.get() == PacketFlyType.FACTOR || type.get() == PacketFlyType.DESYNC).build());
    private final Setting<Double> boostTimer = sgSpeed.add(new DoubleSetting.Builder().name("boost-timer").description("The timer for boost.").defaultValue(1.1).min(0).visible(boost::get).build());

    // Anti Kick
    private final Setting<PacketFlyAntiKick> antiKick = sgAntiCheat.add(new EnumSetting.Builder<PacketFlyAntiKick>().name("anti-kick").description("Which anti kick mode to use").defaultValue(PacketFlyAntiKick.NORMAL).build());
    private final Setting<PacketFlylimit> limit = sgAntiCheat.add(new EnumSetting.Builder<PacketFlylimit>().name("limit").description("Which packet limiter to use").defaultValue(PacketFlylimit.STRICT).build());
    private final Setting<Boolean> constrict = sgAntiCheat.add(new BoolSetting.Builder().name("constrict").description("Sends the packets before the tick").defaultValue(false).build());
    private final Setting<Boolean> jitter = sgAntiCheat.add(new BoolSetting.Builder().name("jitter").description("Randomize movements (can help bypass)").defaultValue(false).build());

    // Keybind
    private final Setting<Boolean> message = sgKeybind.add(new BoolSetting.Builder().name("keybind-message").description("Whether or not to send you a message when toggled a mode.").defaultValue(true).build());
    private final Setting<Keybind> toggleLimit = sgKeybind.add(new KeybindSetting.Builder().name("toggle-limit").description("Key to toggle PacketFlyLimit on or off.").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> toggleAntiKick = sgKeybind.add(new KeybindSetting.Builder().name("toggle-anti-kick").description("Key to toggle anti kick on or off.").defaultValue(Keybind.fromKey(-1)).build());

    // Phase
    private final Setting<PacketFlyPhase> phase = sgPhase.add(new EnumSetting.Builder<PacketFlyPhase>().name("phase").description("Allow phasing through blocks").defaultValue(PacketFlyPhase.NONE).build());
    private final Setting<Boolean> noPhaseSlow = sgPhase.add(new BoolSetting.Builder().name("boost").description("Increase phase speed").defaultValue(true).build());
    private final Setting<Boolean> noCollision = sgPhase.add(new BoolSetting.Builder().name("no-collision").description("Disable block collision while phasing").defaultValue(false).build());

    private int teleportId;

    private PlayerMoveC2SPacket.PositionAndOnGround startingOutOfBoundsPos;

    private final ArrayList<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private final Map<Integer, TimeVec3d> posLooks = new ConcurrentHashMap<>();

    private int antiKickTicks = 0;
    private int vDelay = 0;
    private int hDelay = 0;

    private boolean limitStrict = false;
    private int limitTicks = 0;
    private int jitterTicks = 0;
    private int ticksExisted = 0;

    private boolean oddJitter = false;

    private boolean forceAntiKick = true;
    private boolean forceLimit = true;

    double speedX = 0;
    double speedY = 0;
    double speedZ = 0;

    boolean lastDown = false;

    private int factorCounter = 0;
    private final SystemTimer intervalTimer = new SystemTimer();
    private static final Random random = new Random();

    public PacketFly() {
        super(ML.M, "packet-fly", "easily");
    }

    @Override
    public void onActivate() {
        packets.clear();
        posLooks.clear();
        teleportId = 0;
        vDelay = 0;
        hDelay = 0;
        antiKickTicks = 0;
        limitTicks = 0;
        jitterTicks = 0;
        ticksExisted = 0;
        speedX = 0;
        speedY = 0;
        speedZ = 0;
        lastDown = false;
        oddJitter = false;
        forceAntiKick = true;
        forceLimit = true;
        startingOutOfBoundsPos = null;
        startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
        packets.add(startingOutOfBoundsPos);
        mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
        }

        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().allowFlying = false;

        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }


    @Override
    public String getInfoString() {
        String info = "";
        info += "[" + type.get().name().substring(0, 1).toUpperCase() + type.get().name().substring(1).toLowerCase() + "] ";
        return info;
    }


    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        if (autoToggle.get()) toggle();
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        if (autoToggle.get()) toggle();
    }


    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if (phase.get() != PacketFlyPhase.NONE && noCollision.get()) event.shape = VoxelShapes.empty();
    }


    @EventHandler
    private void onKey(KeyEvent event) {
        if (!(mc.currentScreen == null)) return;

        if (toggleLimit.get().isPressed()) {
            forceLimit = !forceLimit;
            if (message.get()) ChatUtils.sendMsg(Text.of(forceLimit ? "Activated Packet Limit" : "Disabled Packet Limit"));
        }
        if (toggleAntiKick.get().isPressed()) {
            forceAntiKick = !forceAntiKick;
            if (message.get()) ChatUtils.sendMsg(Text.of(forceAntiKick ? "Activated Anti Kick" : "Disabled Anti Kick"));
        }
    }

    // For Boost

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (boost.get()) {
            Modules.get().get(Timer.class).setOverride(boostTimer.get().floatValue());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    // Main Loop

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (type.get() == PacketFlyType.ELYTRA) {
            Vec3d vec3d = new Vec3d(0,0,0);

            if (mc.player.fallDistance <= 0.2) return;

            if (mc.options.forwardKey.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY(-(float) Math.toRadians(mc.player.getYaw()));
            } else if (mc.options.backKey.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY((float) Math.toRadians(mc.player.getYaw()));
            }

            if (mc.options.jumpKey.isPressed()) {
                vec3d.add(0, speed.get(), 0);
            } else if (mc.options.sneakKey.isPressed()) {
                vec3d.add(0, -speed.get(), 0);
            }

            mc.player.setVelocity(vec3d);
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));

            return;
        }

        if (ticksExisted % 20 == 0) {
            posLooks.forEach((tp, timeVec3d) -> {
                if (System.currentTimeMillis() - timeVec3d.getTime() > TimeUnit.SECONDS.toMillis(30L)) {
                    posLooks.remove(tp);
                }
            });
        }

        ticksExisted++;

        mc.player.setVelocity(0.0D, 0.0D, 0.0D);

        if (teleportId <= 0 && type.get() != PacketFlyType.SETBACK) {
            startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
            packets.add(startingOutOfBoundsPos);
            mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
            return;
        }

        boolean phasing = checkCollisionBox();

        speedX = 0;
        speedY = 0;
        speedZ = 0;

        if (mc.options.jumpKey.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            if (ticksExisted % (type.get() == PacketFlyType.SETBACK || type.get() == PacketFlyType.SLOW || limit.get() == PacketFlylimit.STRICT && forceLimit ? 10 : 20) == 0) {
                speedY = (antiKick.get() != PacketFlyAntiKick.NONE && forceAntiKick && onGround()) ? -0.032 : 0.062;
            } else {
                speedY = 0.062;
            }
            antiKickTicks = 0;
            vDelay = 5;
        } else if (mc.options.sneakKey.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            speedY = -0.062;
            antiKickTicks = 0;
            vDelay = 5;
        }

        if ((multiAxis.get() && phasing) || !(mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed())) {
            if (isPlayerMoving()) {
                double[] dir = directionSpeed((((phasing && phase.get() == PacketFlyPhase.NCP ) || bypass.get() == PacketFlyBypass.NCP) ? (noPhaseSlow.get() ? (multiAxis.get() ? 0.0465 : 0.062) : 0.031) : 0.26) * speed.get());
                if ((dir[0] != 0 || dir[1] != 0) && (vDelay < 1 || (multiAxis.get() && phasing))) {
                    speedX = dir[0];
                    speedZ = dir[1];
                    hDelay = 5;
                }
            }

            if (antiKick.get() != PacketFlyAntiKick.NONE && forceAntiKick && onGround() && ((limit.get() == PacketFlylimit.NONE && forceLimit) || limitTicks != 0)) {
                if (antiKickTicks < (packetMode.get() == PacketFlyMode.BYPASS && !bounds.get() ? 1 : 3)) {
                    antiKickTicks++;
                } else {
                    antiKickTicks = 0;
                    if ((antiKick.get() != PacketFlyAntiKick.LIMITED && forceAntiKick && onGround()) || !phasing) {
                        speedY = (antiKick.get() == PacketFlyAntiKick.STRICT && forceAntiKick && onGround()) ? -0.08 : -0.04;
                    }
                }
            }
        }

        if (((phasing && phase.get() == PacketFlyPhase.NCP) || bypass.get() == PacketFlyBypass.NCP)
            && (double) mc.player.forwardSpeed != 0.0 || (double) mc.player.sidewaysSpeed != 0.0 && speedY != 0)
        {
            speedY /= 2.5;
        }

        if (limit.get() != PacketFlylimit.NONE && forceLimit) {
            if (limitTicks == 0) {
                speedX = 0;
                speedY = 0;
                speedZ = 0;
            } else if (limitTicks == 2 && jitter.get()) {
                if (oddJitter) {
                    speedX = 0;
                    speedY = 0;
                    speedZ = 0;
                }
                oddJitter = !oddJitter;
            }
        } else if (jitter.get() && jitterTicks == 7) {
            speedX = 0;
            speedY = 0;
            speedZ = 0;
        }

        switch (type.get()) {
            case FAST -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case SLOW -> {
                if (!isMoving()) break;
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case SETBACK -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), false, false);
            }
            case GHOST -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, true);
            }
            case FACTOR, DESYNC -> {
                float rawFactor = factor.get().floatValue();
                if (factorize.get().isPressed() && intervalTimer.hasPassed(3500)) {
                    intervalTimer.reset();
                    rawFactor = motion.get().floatValue();
                }
                int factorInt = (int) Math.floor(rawFactor);
                factorCounter++;
                if (factorCounter > (int) (20D / ((rawFactor - (double) factorInt) * 20D))) {
                    factorInt += 1;
                    factorCounter = 0;
                }
                for (int i = 1; i <= factorInt; ++i) {
                    mc.player.setVelocity(speedX * i, speedY * i, speedZ * i);
                    sendPackets(isMoving() ? speedX * i : 0, speedY * i, isMoving() ? speedZ * i : 0, packetMode.get(), true, false);
                }
                speedX = mc.player.getVelocity().x;
                speedY = mc.player.getVelocity().y;
                speedZ = mc.player.getVelocity().z;
            }
        }

        vDelay--;
        hDelay--;

        if (constrict.get() && ((limit.get() == PacketFlylimit.NONE && forceLimit) || limitTicks > 1)) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
        }

        limitTicks++;
        jitterTicks++;

        if (limitTicks > ((limit.get() == PacketFlylimit.STRICT && forceLimit) ? (limitStrict ? 1 : 2) : 3)) {
            limitTicks = 0;
            limitStrict = !limitStrict;
        }

        if (jitterTicks > 7) {
            jitterTicks = 0;
        }
    }

    // Accept Server Packets & Cancel Rotation

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (type.get() == PacketFlyType.ELYTRA) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (!(mc.currentScreen instanceof DownloadingTerrainScreen)) {
                PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
                if (mc.player.isAlive()) {
                    if (this.teleportId <= 0) {
                        this.teleportId = ((PlayerPositionLookS2CPacket) event.packet).getTeleportId();
                    } else {
                        if (mc.world.isPosLoaded(mc.player.getBlockX(), mc.player.getBlockZ()) &&
                            type.get() != PacketFlyType.SETBACK) {
                            if (type.get() == PacketFlyType.DESYNC) {
                                posLooks.remove(packet.getTeleportId());
                                event.cancel();
                                if (type.get() == PacketFlyType.SLOW) {
                                    mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                                }
                                return;
                            } else if (posLooks.containsKey(packet.getTeleportId())) {
                                TimeVec3d vec = posLooks.get(packet.getTeleportId());
                                if (vec.x == packet.getX() && vec.y == packet.getY() && vec.z == packet.getZ()) {
                                    posLooks.remove(packet.getTeleportId());
                                    event.cancel();
                                    if (type.get() == PacketFlyType.SLOW) {
                                        mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }

                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
                packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
                teleportId = packet.getTeleportId();
            } else {
                teleportId = 0;
            }
        }
    }

    // Movement

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (type.get() == PacketFlyType.ELYTRA) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(speed.get().floatValue() / 20);
            return;
        }

        if (type.get() != PacketFlyType.SETBACK && teleportId <= 0) return;
        if (type.get() != PacketFlyType.SLOW) ((IVec3d) event.movement).set(speedX, speedY, speedZ);
    }

    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (type.get() == PacketFlyType.ELYTRA && event.packet instanceof PlayerMoveC2SPacket) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return;
        }

        if (event.packet instanceof PlayerMoveC2SPacket && !(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)) event.cancel();

        if (event.packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
            if (this.packets.contains(packet)) {
                this.packets.remove(packet);
                return;
            }
            event.cancel();
        }
    }

    // Utils

    public void updateFlying(PacketFlyType type) {
        if (mc.world != null && mc.player != null && type != PacketFlyType.ELYTRA) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }
    }

    private void sendPackets(double x, double y, double z, PacketFlyMode mode, boolean confirmTeleport, boolean sendExtraConfirmTeleport) {
        Vec3d nextPos = new Vec3d(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
        Vec3d bounds = getBoundsVec(x, y, z, mode);

        PlayerMoveC2SPacket nextPosPacket = new PlayerMoveC2SPacket.PositionAndOnGround(nextPos.x, nextPos.y, nextPos.z, mc.player.isOnGround());
        packets.add(nextPosPacket);
        mc.getNetworkHandler().sendPacket(nextPosPacket);

        if ((limit.get() != PacketFlylimit.NONE && forceLimit) && limitTicks == 0) return;

        PlayerMoveC2SPacket boundsPacket = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, mc.player.isOnGround());
        packets.add(boundsPacket);
        mc.getNetworkHandler().sendPacket(boundsPacket);

        if (confirmTeleport) {
            teleportId++;

            if (sendExtraConfirmTeleport) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId - 1));
            }

            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));

            posLooks.put(teleportId, new TimeVec3d(nextPos.x, nextPos.y, nextPos.z, System.currentTimeMillis()));

            if (sendExtraConfirmTeleport) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId + 1));
            }
        }
    }

    private Vec3d getBoundsVec(double x, double y, double z, PacketFlyMode mode) {
        switch (mode) {
            case UP:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 255 : 256) : mc.player.getY() + 420, mc.player.getZ() + z);
            case PRESERVE:
                return new Vec3d(bounds.get() ? mc.player.getX() + randomHorizontal() : randomHorizontal(), strict.get() ? (Math.max(mc.player.getY(), 2D)) : mc.player.getY(), bounds.get() ? mc.player.getZ() + randomHorizontal() : randomHorizontal());
            case LIMITJITTER:
                return new Vec3d(mc.player.getX() + (strict.get() ? x : randomLimitedHorizontal()), mc.player.getY() + randomLimitedVertical(), mc.player.getZ() + (strict.get() ? z : randomLimitedHorizontal()));
            case BYPASS:
                if (bounds.get()) {
                    double rawY = y * 510;
                    return new Vec3d(mc.player.getX() + x, mc.player.getY() + ((rawY > ((PlayerUtils.getDimension() == Dimension.End) ? 127 : 255)) ? -rawY : (rawY < 1) ? -rawY : rawY), mc.player.getZ() + z);
                } else {
                    return new Vec3d(mc.player.getX() + (x == 0D ? (random.nextBoolean() ? -10 : 10) : x * 38), mc.player.getY() + y, mc.player.getX() + (z == 0D ? (random.nextBoolean() ? -10 : 10) : z * 38));
                }
            case OBSCURE:
                return new Vec3d(mc.player.getX() + randomHorizontal(), Math.max(1.5D, Math.min(mc.player.getY() + y, 253.5D)), mc.player.getZ() + randomHorizontal());
            default:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 1 : 0) : mc.player.getY() - 1337, mc.player.getZ() + z);
        }
    }

    public double randomHorizontal() {
        int randomValue = random.nextInt(bounds.get() ? 80 : (packetMode.get() == PacketFlyMode.OBSCURE ? (ticksExisted % 2 == 0 ? 480 : 100) : 29000000)) + (bounds.get() ? 5 : 500);
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    public static double randomLimitedVertical() {
        int randomValue = random.nextInt(22);
        randomValue += 70;
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    public static double randomLimitedHorizontal() {
        int randomValue = random.nextInt(10);
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);  // * mc.getRenderPartialTicks();

        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }

        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;
        return new double[]{posX, posZ};
    }

    private boolean checkCollisionBox() {
        //!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox()).
        //return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox()).findAny().isEmpty();
        return true; //TODO Fix, hotfix so this compiles on 1.18
    }

    private boolean onGround() {
        if (stopOnGround.get()) return !BlockHelper.isSolid(mc.player.getBlockPos().down());

        return true;
    }

    private boolean isPlayerMoving() {
        if (mc.options.jumpKey.isPressed()) return true;
        if (mc.options.forwardKey.isPressed()) return true;
        if (mc.options.backKey.isPressed()) return true;
        if (mc.options.leftKey.isPressed()) return true;
        return mc.options.rightKey.isPressed();
    }

    private boolean isMoving() {
        if (onlyOnMove.get()) {
            if (mc.options.jumpKey.isPressed()) return true;
            if (mc.options.forwardKey.isPressed()) return true;
            if (mc.options.backKey.isPressed()) return true;
            if (mc.options.leftKey.isPressed()) return true;
            if (mc.options.rightKey.isPressed()) return true;
        }

        return true;
    }

    static class TimeVec3d extends Vec3d {
        private final long time;

        public TimeVec3d(double xIn, double yIn, double zIn, long time) {
            super(xIn, yIn, zIn);
            this.time = time;
        }

        public TimeVec3d(Vec3i vec, long time) {
            super(new Vec3f(vec.getX(), vec.getY(), vec.getZ()));
            this.time = time;
        }

        public long getTime() {
            return time;
        }
    }
}
