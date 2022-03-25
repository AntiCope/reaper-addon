package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class LongJump extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");
    private final SettingGroup sgAC = settings.createGroup("Anti Cheat");


    // general
    private final Setting<Integer> durationTicks = sgGeneral.add(new IntSetting.Builder().name("jump-ticks").description("How many ticks to jump for.").defaultValue(60).min(1).sliderRange(1, 100).build());
    private final Setting<Double> factor = sgGeneral.add(new DoubleSetting.Builder().name("factor").defaultValue(3.5).min(1).sliderRange(1, 10).build());
    private final Setting<Boolean> onJump = sgGeneral.add(new BoolSetting.Builder().name("wait-for-manual-jump").defaultValue(false).build());
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder().name("distance").description("How far to jump.").defaultValue(12).min(1).sliderRange(1, 20).build());
    private final Setting<Boolean> onRubberband = sgGeneral.add(new BoolSetting.Builder().name("cancel-on-rubberband").description("Stops the jump if you rubberband.").defaultValue(true).build());

    // delay
    private final Setting<Boolean> delay = sgDelay.add(new BoolSetting.Builder().name("delay").description("Cache movement packets before long-jumping.").defaultValue(true).build());
    private final Setting<Integer> delayTicks = sgDelay.add(new IntSetting.Builder().name("delay-ticks").description("How many ticks to cache packets for.").defaultValue(30).min(1).sliderRange(1, 50).visible(delay::get).build());
    private final Setting<Boolean> lockMovement = sgDelay.add(new BoolSetting.Builder().name("stop-motion").description("Prevents you from moving while caching packets.").defaultValue(false).visible(delay::get).build());
    private final Setting<Boolean> chatInfo = sgDelay.add(new BoolSetting.Builder().name("chat-info").defaultValue(false).visible(delay::get).build());


    // ac
    private final Setting<Boolean> inWater = sgAC.add(new BoolSetting.Builder().name("allow-in-water").defaultValue(false).build());
    private final Setting<Boolean> inLava = sgAC.add(new BoolSetting.Builder().name("allow-in-lava").defaultValue(false).build());
    private final Setting<Boolean> whenSneaking = sgAC.add(new BoolSetting.Builder().name("allow-when-sneaking").defaultValue(false).build());
    private final Setting<Boolean> strict = sgAC.add(new BoolSetting.Builder().name("strict").description("Pauses when hunger reaches 3 or less drumsticks").defaultValue(true).build());
    private final Setting<Boolean> step = sgAC.add(new BoolSetting.Builder().name("allow-step").defaultValue(false).build());
    private final Setting<Double> stepHeight = sgAC.add(new DoubleSetting.Builder().name("step-height").defaultValue(1).min(0.5).sliderRange(0.5, 10).visible(step::get).build());

    public LongJump() {
        super(ML.R, "long-jump", "Manipulate movement packets to travel quickly.");
    }

    private int chargeTicked;
    private int durationTicked;

    private boolean messaged;

    private boolean charged;
    private boolean moved;

    private Vec3d startPos;

    Timer timerClass = Modules.get().get(Timer.class);

    @Override
    public void onActivate() {
        reset();
        startPos = Vec3d.ofCenter(mc.player.getBlockPos());
        messaged = false;
    }

    @Override
    public void onDeactivate() {
        reset();
        startPos = null;
    }

    private void reset() {
        timerClass.setOverride(Timer.OFF);
        chargeTicked = 0;
        durationTicked = 0;

        charged = false;
        moved = false;

        if (step.get()) mc.player.stepHeight = 0.5f;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (onRubberband.get()) toggle();
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!charged && lockMovement.get()) {
            if (Input.isKeyPressed(GLFW.GLFW_KEY_W) || Input.isKeyPressed(GLFW.GLFW_KEY_A) || Input.isKeyPressed(GLFW.GLFW_KEY_S) || Input.isKeyPressed(GLFW.GLFW_KEY_D) || Input.isKeyPressed(GLFW.GLFW_KEY_SPACE))
                event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Check pause settings
        if (mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (!inWater.get() && mc.player.isTouchingWater()) return;
        if (!inLava.get() && mc.player.isInLava()) return;
        if (strict.get() && (mc.player.getHungerManager().getFoodLevel() <= 6)) return;

        // Make sure timer is off first
        timerClass.setOverride(Timer.OFF);

        if (delay.get()) chargeTicked++;
        if (chargeTicked >= delayTicks.get() || !delay.get()) charged = true;

        if (charged) {
            if (delay.get() && chatInfo.get() && !messaged) {
                warning("Ready!");
                messaged = true;
            }

            if (PlayerUtils.isMoving() && (!onJump.get() || (mc.options.jumpKey.isPressed() && onJump.get()))) moved = true;

            if (moved) {
                durationTicked++;
                if (step.get()) mc.player.stepHeight = stepHeight.get().floatValue();
            }
        }
        if (startPos != null && (Math.sqrt(mc.player.squaredDistanceTo(startPos)) >= distance.get() || (durationTicked * factor.get()) > durationTicks.get())) toggle();
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (charged && moved) {
            timerClass.setOverride(factor.get());

            double moveForward = mc.player.input.movementForward;
            double moveStrafe = mc.player.input.movementSideways;
            double rotationYaw = mc.player.getYaw();
            if (moveForward == 0.0 && moveStrafe == 0.0) {
                ((IVec3d) event.movement).setXZ(0, 0);
            } else {
                if (moveForward != 0.0) {
                    if (moveStrafe > 0.0) rotationYaw += (moveForward > 0.0 ? -45 : 45);
                    else if (moveStrafe < 0.0) rotationYaw += (moveForward > 0.0 ? 45 : -45);
                    moveStrafe = 0.0;
                }
                moveStrafe = moveStrafe == 0.0 ? moveStrafe : (moveStrafe > 0.0 ? 1.0 : -1.0);
                ((IVec3d) event.movement).setXZ(moveForward * getMaxSpeed() * Math.cos(Math.toRadians(rotationYaw + 90.0) + moveStrafe * getMaxSpeed() * Math.sin(Math.toRadians(rotationYaw + 90.0))), moveForward * getMaxSpeed() * Math.sin(Math.toRadians(rotationYaw + 90.0)) - moveStrafe * getMaxSpeed() * Math.cos(Math.toRadians(rotationYaw + 90.0)));
            }
        }

    }

    private double getMaxSpeed() {
        double defaultSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }

    @Override
    public String getInfoString() {
        if (delay.get()) {
            if (chargeTicked < delayTicks.get()) {
                return chargeTicked + "";
            } else {
                return "Ready";
            }
        } else {
            return durationTicked + "";
        }
    }
}
