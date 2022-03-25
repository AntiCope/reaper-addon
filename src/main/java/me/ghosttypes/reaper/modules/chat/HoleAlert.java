package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import oshi.util.tuples.Pair;

import java.util.Objects;

public class HoleAlert extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.getDefaultGroup();

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    private final Setting<Boolean> showBreakerName = sgGeneral.add(new BoolSetting.Builder().name("display-breaker").defaultValue(false).build());
    private final Setting<Boolean> reinforce = sgGeneral.add(new BoolSetting.Builder().name("reinforce").defaultValue(false).build());
    private final Setting<Integer> reinforceTries = sgGeneral.add(new IntSetting.Builder().name("tries").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Boolean> reinforceBurrow = sgGeneral.add(new BoolSetting.Builder().name("semi-burrow").defaultValue(false).build());
    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder().name("rotation").defaultValue(false).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(1).min(0).sliderMax(10).build());


    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    private boolean runReinforce;
    private Pair<BlockPos, Direction> placeAt;
    private int timer, reinforceT;
    private String lastMsg;

    public HoleAlert() {
        super(ML.M, "hole-alert", "alerts you when your hole is being broken");
    }

    @Override
    public void onActivate() {
        runReinforce = false;
        reinforceT = reinforceTries.get();
        placeAt = null;
        lastMsg = null;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!runReinforce || placeAt == null || !reinforce.get()) return;

        if (timer <= 0) {

            boolean failed = false;

            timer = delay.get();

            FindItemResult obby = Interactions.findObby();
            if (!obby.found()) {
                error("No obby in hotbar, can't protect surround.");
                failed = true;
            } else {
                BlockPos surroundPos = placeAt.getA();
                Direction dir = placeAt.getB();

                BlockPos down = surroundPos.offset(dir);
                BlockPos up = down.up();

                if (debug.get()) info("Placing reinforce blocks");
                BlockHelper.place(surroundPos, obby, rotation.get(), packetPlace.get());
                BlockHelper.place(down, obby, rotation.get(), packetPlace.get());
                BlockHelper.place(up, obby, rotation.get(), packetPlace.get());
            }


            if (reinforceBurrow.get()) {
                FindItemResult anvil = Interactions.findAnvil();
                if (!anvil.found()) {
                    error("No anvils in hotbar, can't semi-burrow.");
                    failed = true;
                } else {
                    info("Auto semi-burrowing.");
                    BlockHelper.place(mc.player.getBlockPos().up(2), anvil, rotation.get(), packetPlace.get());
                }
            }
            if (failed || reinforceTries.get() < 2) {
                error("Reinforce failed.");
                runReinforce = false;
                placeAt = null;
            } else {
                reinforceT--;
                if (reinforceT <= 0) {
                    runReinforce = false;
                    placeAt = null;
                    reinforceT = reinforceTries.get();
                }
            }
        } else {
            timer--;
        }
    }


    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof BlockBreakingProgressS2CPacket packet) {
            if (debug.get()) info("Received block break progress packet, checking");
            if (packet.getProgress() > 1 || !BlockHelper.isOurSurroundBlock(packet.getPos())) {
                if (debug.get()) info("Progress > 0 or it's not our surround block.");
                return;
            }
            if (!BlockHelper.isTrapBlock(packet.getPos())) {
                if (debug.get()) info("Ignoring packet (entity id: " + packet.getEntityId() + ") - invalid block");
                return;
            }

            String msg = "Your " + BlockHelper.getBlockDirectionFromPlayer(packet.getPos()) + " surround is being broken";
            if (showBreakerName.get()) msg += (" by " + Objects.requireNonNull(mc.world.getEntityById(packet.getEntityId())).getEntityName());
            if (!msg.equals(lastMsg)) {
                lastMsg = msg;
            } else {
                return;
            }
            warning(msg);
            if (!reinforce.get()) return;
            for (Direction dir : Direction.values()) {
                BlockPos playerPos = mc.player.getBlockPos();
                BlockPos offset = playerPos.offset(dir);
                if (offset.equals(packet.getPos())) {
                    placeAt = new Pair<>(offset, dir);
                    runReinforce = true;
                    info("Reinforcing surround.");
                    return;
                }
            }
        }
    }

    // Rendering
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && placeAt != null) {

            BlockPos center = placeAt.getA();
            BlockPos down = center.offset(placeAt.getB());
            BlockPos up = down.up();

            event.renderer.box(center, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            event.renderer.box(down, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            event.renderer.box(up, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

}
