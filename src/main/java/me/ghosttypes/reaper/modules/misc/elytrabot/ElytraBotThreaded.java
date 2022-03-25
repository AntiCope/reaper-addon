package me.ghosttypes.reaper.modules.misc.elytrabot;

import baritone.api.BaritoneAPI;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.modules.misc.elytrabot.utils.*;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ElytraBotThreaded extends ReaperModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final SettingGroup sgElytraFly = settings.createGroup("ElytraFly");
    private final SettingGroup sgCoordinates = settings.createGroup("Coordinates");
    private final SettingGroup sgAutoEat = settings.createGroup("AutoEat");
    private final SettingGroup sgRender = settings.createGroup("Rendering");


    public final Setting<Mode> botMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("bot-mode")
        .description("What mode for the module to use.")
        .defaultValue(Mode.Highway)
        .build()
    );

    private final Setting<TakeoffMode> takeoffMode = sgGeneral.add(new EnumSetting.Builder<TakeoffMode>()
        .name("takeoff-mode")
        .description("What mode to use for taking off.")
        .defaultValue(TakeoffMode.SlowGlide)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Whether or not to use baritone to walk a bit if stuck or a path cannot be found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> walkDistance = sgGeneral.add(new IntSetting.Builder()
        .name("walk-distance")
        .description("How far to walk with baritone.")
        .defaultValue(20)
        .sliderMax(30)
        .visible(useBaritone::get)
        .build()
    );

    public final Setting<Boolean> avoidLava = sgGeneral.add(new BoolSetting.Builder()
        .name("avoid-lava")
        .description("Whether or not the pathfinding will avoid lava.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y")
        .description("The maximum y coordinate the pathfinding can go to. Set to -1 to disable.")
        .defaultValue(-1)
        .sliderMax(128)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches equipped low durability elytra with a new one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> restoreChest = sgGeneral.add(new BoolSetting.Builder()
        .name("restore-chestplate")
        .description("Switch back to a chestplate after disabling.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> switchDurability = sgGeneral.add(new IntSetting.Builder()
        .name("switch-durability")
        .description("The durability threshold your elytra will be replaced at.")
        .defaultValue(2)
        .min(1)
        .max(Items.ELYTRA.getMaxDamage() - 1)
        .sliderMax(20)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Boolean> toggleOnPop = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-pop")
        .description("Whether to toggle the module if you pop a totem or not.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Sends debug messages.")
        .defaultValue(false)
        .build()
    );

    private final Setting<FlyMode> flyMode = sgElytraFly.add(new EnumSetting.Builder<FlyMode>()
        .name("fly-mode")
        .description("What mode to use for flying.")
        .defaultValue(FlyMode.Control)
        .onChanged(this::onModeChange)
        .build()
    );

    public final Setting<Double> flySpeed = sgElytraFly.add(new DoubleSetting.Builder()
        .name("fly-speed")
        .description("The speed for control flight.")
        .defaultValue(1.81)
        .sliderMax(5)
        .visible(() -> flyMode.get() == FlyMode.Control)
        .build()
    );

    public final Setting<Double> maneuverSpeed = sgElytraFly.add(new DoubleSetting.Builder()
        .name("maneuver-speed")
        .description("The speed used for maneuvering.")
        .defaultValue(1)
        .sliderMax(3)
        .visible(() -> flyMode.get() == FlyMode.Control)
        .build()
    );

    private final Setting<Double> fireworkDelay = sgElytraFly.add(new DoubleSetting.Builder()
        .name("firework-delay")
        .description("The delay between using fireworks in seconds.")
        .defaultValue(2.0)
        .visible(() -> flyMode.get() == FlyMode.Firework)
        .build()
    );

    private final Setting<Boolean> useCoordinates = sgCoordinates.add(new BoolSetting.Builder()
        .name("use-coordinates")
        .description("If true, uses the given coordinates. If not, starts flying in the direction you are facing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> gotoX = sgCoordinates.add(new IntSetting.Builder()
        .name("goto-x")
        .description("The x coordinate the bot will try to go to.")
        .defaultValue(0)
        .sliderMin(-100000)
        .sliderMax(100000)
        .min(-30000000)
        .max(30000000)
        .visible(useCoordinates::get)
        .build()
    );

    private final Setting<Integer> gotoZ = sgCoordinates.add(new IntSetting.Builder()
        .name("goto-z")
        .description("The z coordinate the bot will try to go to.")
        .defaultValue(0)
        .sliderMin(-100000)
        .sliderMax(100000)
        .min(-30000000)
        .max(30000000)
        .visible(useCoordinates::get)
        .build()
    );

    private final Setting<Boolean> autoEat = sgAutoEat.add(new BoolSetting.Builder()
        .name("auto-eat")
        .description("Automatically eats gaps or other food when health or hunger is low.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minHeatlh = sgAutoEat.add(new IntSetting.Builder()
        .name("min-health")
        .description("The health value at which the bot will eat food.")
        .defaultValue(10)
        .sliderMin(1)
        .sliderMax(19)
        .visible(autoEat::get)
        .build()
    );

    private final Setting<Integer> minHunger = sgAutoEat.add(new IntSetting.Builder()
        .name("min-hunger")
        .description("The health hunger at which the bot will eat food.")
        .defaultValue(10)
        .sliderMin(1)
        .sliderMax(19)
        .visible(autoEat::get)
        .build()
    );

    public final Setting<Boolean> allowGaps = sgAutoEat.add(new BoolSetting.Builder()
        .name("allow-gaps")
        .description("Whether or not the bot is allowed to eat gapples.")
        .defaultValue(true)
        .visible(autoEat::get)
        .build()
    );

    public final Setting<Boolean> renderPath = sgRender.add(new BoolSetting.Builder()
        .name("render-path")
        .description("Whether or not the path should be rendered.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> pathColour = sgRender.add(new ColorSetting.Builder()
        .name("path-color")
        .description("The path's color.")
        .defaultValue(new SettingColor(255, 0, 0, 150))
        .visible(renderPath::get)
        .build()
    );

    private ArrayList<BlockPos> path;
    private Thread thread;
    private BlockPos goal, previous, lastSecondPos;
    private BlockPos last = null;
    private DirectionUtil direction;
    private int x, z;
    private int startingTotems;
    private boolean watchTotems;
    private double jumpY = -1;
    private int packetsSent, lagbackCounter, useBaritoneCounter;
    private boolean lagback, toggledNoFall, isRunning;
    private double blocksPerSecond;
    private int blocksPerSecondCounter;
    private TimerUtil blocksPerSecondTimer = new TimerUtil();
    private TimerUtil packetTimer = new TimerUtil();
    private TimerUtil fireworkTimer = new TimerUtil();
    private TimerUtil takeoffTimer = new TimerUtil();

    public enum Mode {Highway, Overworld, Tunnel}
    public enum TakeoffMode {SlowGlide, PacketFLy, Jump}
    public enum FlyMode {Control, Firework}

    public String Status = "Disabled";
    public String Goal = null;
    public String Time = null;
    public String Fireworks = null;


    public ElytraBotThreaded() {
        super(ML.M, "elytra-bot", "Elytra AutoPilot");
    }

    @Override
    public void onActivate() {
        int up = 1;
        Status = "Enabled";

        // this should work to properly end the thread, rather than doing thread.suspend()
        isRunning = true;

        // equip an elytra before starting the thead (doesn't seem to work when first starting in the thread)
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            if (elytra.found()) InvUtils.move().from(elytra.slot()).toArmor(2);
        }

        // no fall can mess up takeoff and the entire module, so disable it if its active, and re-enable it after
        NoFall noFall = Modules.get().get(NoFall.class);
        if (noFall.isActive()) {
            noFall.toggle();
            toggledNoFall = true;
            warning("NoFall is on, disabling while ElytraBot is active.");
        }

        // alternative to using packet event for checking if we poppped
        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found()) {
            watchTotems = false;
        } else {
            startingTotems = totem.count();
            watchTotems = true;
        }

        if (!useCoordinates.get()) {
            if (Math.abs(Math.abs(mc.player.getX()) - Math.abs(mc.player.getZ())) <= 5 && Math.abs(mc.player.getX()) > 10 && Math.abs(mc.player.getZ()) > 10 && (botMode.get() == Mode.Highway)) {
                direction = DirectionUtil.getDiagonalDirection();
            } else direction = DirectionUtil.getDirection();

            goal = generateGoalFromDirection(direction, up);
            Goal = direction.name;
        }
        else {
            x = gotoX.get();
            z = gotoZ.get();
            goal = new BlockPos(x, mc.player.getY() + up, z);
            Goal = ("X: " + x + ", Z: " + z);
        }

        thread = new Thread() {
            public void run() {
                // to stop the thread loop just set isRunning to false
                while (thread != null && thread.equals(this) && isRunning) {
                    try {
                        loop();
                    } catch (NullPointerException e) {

                    }

                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        blocksPerSecondTimer.reset();
        thread.start();
    }

    @Override
    public void onDeactivate() {
        direction = null;
        path = null;
        useBaritoneCounter = 0;
        lagback = false;
        lagbackCounter = 0;
        blocksPerSecond = 0;
        blocksPerSecondCounter = 0;
        lastSecondPos = null;
        jumpY = -1;
        last = null;
        PacketFly.toggle(false);
        ElytraFly.toggle(false);
        goal = null;
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
        MiscUtil.suspend(thread);
        thread = null;
        Status = "Disabled";
        Goal = null;
        Time = null;
        Fireworks = null;
        if (toggledNoFall) {
            NoFall noFall = Modules.get().get(NoFall.class);
            if (!noFall.isActive() && toggledNoFall) { // it shouldn't be active but better to just assume a monke user might have
                // somehow enabled it during flight
                info("Re-enabling NoFall");
                noFall.toggle();
            }
        }
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isRunning) { // when the thread is stopped
            if (restoreChest.get()) enableGroundListener();
            toggle();
        }

        // "new" totem check
        if (watchTotems && toggleOnPop.get()) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING); // check current totem count
            if (!totem.found() || totem.count() < startingTotems) {
                warning("Disabling because you've popped a totem.");
                isRunning = false; // toggle if the user has none left, or it's below what they started with
            }
        }
    }


    public void loop() {
        ElytraBotThreaded elytraBot = Modules.get().get(ElytraBotThreaded.class);
        if (!Utils.canUpdate()) return;

        // stop if we reached the goal
        if (PlayerUtils.distanceTo(goal) < 15) {
            mc.world.playSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), new SoundEvent(new Identifier("minecraft:entity.player.levelup")), SoundCategory.PLAYERS, 100, 18, true);
            info("Goal reached!");
            isRunning = false;
        }

        // elytra check
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            error("You need an elytra.");
            isRunning = false;
        }

        // no fall check again
        NoFall noFall = Modules.get().get(NoFall.class);
        if (noFall.isActive()) {
            error("You cannot use NoFall while ElytraBot is active!");
            if (!toggledNoFall) toggledNoFall = true;
            noFall.toggle();
        }

        //if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
        //    for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
        //        ItemStack itemStack = mc.player.getInventory().getStack(i);
        //        if (itemStack.getItem().equals(Items.ELYTRA)) InvUtils.move().from(i).toArmor(2);
        //        if (debug.get()) info("Equipping elytra");
        //        else if (i == mc.player.getInventory().main.size() - 1) {
        //            error("You need an elytra.");
        //            MiscUtil.toggle(thread);
        //        }
        //   }
        //}

        // toggle if no fireworks and using firework mode
        if (flyMode.get() == FlyMode.Firework && InvUtils.find(Items.FIREWORK_ROCKET).count() == 0) {
            error("You need fireworks in your inventory if you are using firework mode.");
            isRunning = false;
        }

        // waiting if in an unloaded chunk
        if (!MiscUtil.isInRenderDistance(getPlayerPos())) {
            Status = "Waiting for chunk";
            mc.player.setVelocity(0, 0, 0);
            return;
        }

        // switch low dura elytra with fresh one if setting on
        if (autoSwitch.get()) {
            ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= switchDurability.get()) {
                    if (debug.get()) info("Trying to switch elytra");
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > switchDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                    if (debug.get()) info("Swapped elytra");
                }
            }
        }

        // takeoff
        double preventPhase = (jumpY + 0.6);
        if (mc.player.isFallFlying() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
            if (PacketFly.toggled) {
                if (debug.get()) info("//takeoff 1");
                sleep(1500);

                if (mc.player.isFallFlying() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
                    sleep(100);
                    if (debug.get()) info("//takeoff 2");
                }
            }
        }

        if (!mc.player.isFallFlying()) {
            ElytraFly.toggle(false);

            BlockPos blockPosAbove = getPlayerPos().add(0, 2, 0);

            if (mc.player.isOnGround() && MiscUtil.isSolid(blockPosAbove) && useBaritone.get() && botMode.get()== Mode.Highway) {
                Status = "Using baritone";
                useBaritone();
            }

            if (MiscUtil.isSolid(blockPosAbove) && botMode.get() == Mode.Tunnel) {
                if (MiscUtil.getBlock(blockPosAbove) != Blocks.BEDROCK) {
                    Status = "Mining obstruction";
                    PlayerUtils.centerPlayer();
                    Rotations.rotate(Rotations.getYaw(blockPosAbove), Rotations.getPitch(blockPosAbove), () -> MiscUtil.mine(blockPosAbove));
                } else {
                    if (useBaritone.get()) {
                        Status = "Using baritone";
                        useBaritone();
                    } else {
                        info("The above block is bedrock and useBaritone is false.");
                        isRunning = false;
                    }
                }
            }

            if (jumpY != 1 && Math.abs(mc.player.getY() - jumpY) >= 2) {
                if (useBaritone.get() && direction != null && botMode.get() == Mode.Highway) {
                    info("Using baritone to get back to the highway.");
                    Status = "Using baritone";
                    useBaritone();
                }
            }

            if (packetsSent < 20) {
                if (debug.get()) info("Trying to takeoff.");
                Status = "Taking off";
            }

            fireworkTimer.ms = 0;

            if (mc.player.isOnGround()) {
                jumpY = mc.player.getY();
                generatePath();
                mc.player.jump();
                if (debug.get()) info("Path generated, taking off.");
                if (debug.get()) info("Path: " + path);
            }

            else if (mc.player.getY() < mc.player.prevY) {
                if (takeoffMode.get() == TakeoffMode.PacketFLy) {
                    if (mc.player.getY() > preventPhase && !PacketFly.toggled) PacketFly.toggle(true);
                    if (debug.get()) info("Toggling on packet fly.");
                } else if (takeoffMode.get() == TakeoffMode.SlowGlide) {
                    mc.player.setVelocity(0, -0.04, 0);
                    if (debug.get()) info("Slow gliding.");
                }


                // Don't send any more packets for about 15 seconds if the takeoff isn't successful.
                // Bcs 2b2t has this annoying thing where it will not let u open elytra if u don't stop sending the packets for a while
                if (packetsSent <= 15) {
                    if (takeoffTimer.hasPassed(650)) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        if (debug.get()) info("Sending elytra open packet.");
                        takeoffTimer.reset();
                        packetTimer.reset();
                        packetsSent++;
                    }
                } else if (packetTimer.hasPassed(15000)) {
                    packetsSent = 0;
                    if (debug.get()) info("15 seconds over.");
                }
                else {
                    info("Waiting 15 seconds before sending elytra opening packets again");
                    Status = "Waiting to takeoff";
                }
            }
            return;
        }

        else if (!PacketFly.toggled) {
            packetsSent = 0;

            double speed = MiscUtil.getSpeed();
            if (speed < 0.1) {
                useBaritoneCounter++;

                if (useBaritoneCounter >= 15) {
                    useBaritoneCounter = 0;

                    if (useBaritone.get()) {
                        info("Using baritone to walk a bit because we are stuck.");
                        Status = "Using baritone";
                        useBaritone();
                    }
                    else {
                        info("We are stuck. Enabling the 'useBaritone' setting would help.");
                        isRunning = false;
                    }
                }
            } else useBaritoneCounter = 0;

            if (flyMode.get() == FlyMode.Firework) {
                if (speed > 3) lagback = true;


                if (lagback) {
                    if (speed < 1) {
                        lagbackCounter++;
                        if (debug.get()) info("Potential lagback detected.");
                        if (lagbackCounter > 3) {
                            lagback = false;
                            lagbackCounter = 0;
                            if (debug.get()) info("Lagback reset.");
                        }
                    } else lagbackCounter = 0;
                }

                if (fireworkTimer.hasPassed((int) (fireworkDelay.get() * 1000)) && !lagback) {
                    clickOnFirework();
                    //if (debug.get()) info("Using firework.");
                }
            }
        }

        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float hunger = mc.player.getHungerManager().getFoodLevel();
        int prevSlot = mc.player.getInventory().selectedSlot;
        if (autoEat.get() && !mc.player.isUsingItem() && !Modules.get().get(AutoEat.class).isActive()) {
            if (flyMode.get() != FlyMode.Firework || (flyMode.get() == FlyMode.Firework && !fireworkTimer.hasPassed(100))) {
                if (health <= minHeatlh.get() || hunger <= minHunger.get()) {
                    if (debug.get()) info("Need to eat.");
                    for (int i = 0; i < 9; i++) {
                        Item item = mc.player.getInventory().getStack(i).getItem();
                        if (debug.get()) info("Finding food item.");
                        if (item.isFood()) {
                            if (MiscUtil.shouldEatItem(item)) {
                                MiscUtil.eat(i);
                                if (debug.get()) info("Trying to eat item.");
                            }
                        }
                    }
                }
            }
        } else if (mc.player.isUsingItem() && health >= minHeatlh.get() && hunger >= minHunger.get()) {
            stopEating(prevSlot);
            if (debug.get()) info("Stopped eating.");
        }

        if (path == null || path.size() <= 20 || isNextPathTooFar()) {
            generatePath();
            //if (debug.get()) info("Generating more path.");
        }

        int distance = 12;
        if (botMode.get() == Mode.Highway || flyMode.get() == FlyMode.Control) distance = 2;

        boolean remove = false;
        ArrayList<BlockPos> removePositions = new ArrayList<BlockPos>();

        for (BlockPos pos : path) {
            if (!remove && MiscUtil.distance(pos, getPlayerPos()) <= distance) remove = true;
            if (remove) removePositions.add(pos);
        }

        for (BlockPos pos :removePositions) {
            path.remove(pos);
            previous = pos;
        }

        if (path.size() > 0) {
            if (direction != null) {
                if (debug.get()) info("Going to " + direction.name);
            } else {
                if (debug.get()) info("Going to X: " + x + " Z: " + z);

                if (blocksPerSecondTimer.hasPassed(1000)) {
                    blocksPerSecondTimer.reset();
                    if (lastSecondPos != null) {
                        blocksPerSecondCounter++;
                        blocksPerSecond += PlayerUtils.distanceTo(lastSecondPos);
                    }

                    lastSecondPos = getPlayerPos();
                }

                int seconds = (int)(PlayerUtils.distanceTo(goal) / (blocksPerSecond / blocksPerSecondCounter));
                int h = seconds / 3600;
                int m = (seconds % 3600) / 60;
                int s = seconds % 60;

                if (debug.get()) info("Estimated arrival in " + h + "h, " + m + "m, " + s + "s");
                Time = (h + "h, " + m + "m, " + s + "s");

                if (flyMode.get() == FlyMode.Firework) {
                    if (debug.get()) info("Estimated fireworks needed: " + (int) (seconds / fireworkDelay.get()));
                    Fireworks = String.valueOf(Math.round(seconds / fireworkDelay.get()));
                }
            }

            if (flyMode.get() == FlyMode.Firework) {
                Vec3d vec = new Vec3d(path.get(path.size() - 1).add(0.5, 0.5, 0.5).getX(), path.get(path.size() - 1).add(0.5, 0.5, 0.5).getY(), path.get(path.size() - 1).add(0.5, 0.5, 0.5).getZ());
                mc.player.setYaw((float) Rotations.getYaw(vec));
                mc.player.setPitch((float) Rotations.getPitch(vec));
                if (debug.get()) info("Rotating to use firework.");
                Status = "Flying";
            } else if (flyMode.get() == FlyMode.Control) {
                ElytraFly.toggle(true);

                BlockPos next = null;
                if (path.size() > 1) {
                    next = path.get(path.size() - 2);
                }
                ElytraFly.setMotion(path.get(path.size() - 1), next, previous);
                if (debug.get()) info("Elytra flying to next position.");
                Status = "Flying";
            }
        }
    }

    public void generatePath() {
        BlockPos[] positions = {new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1),
            new BlockPos(0, -1, 0), new BlockPos(0, 1, 0)};

        ArrayList<BlockPos> checkPositions = new ArrayList<BlockPos>();

        if (botMode.get() == Mode.Highway) {
            BlockPos[] list = {new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
                new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1)};
            checkPositions = new ArrayList<BlockPos>(Arrays.asList(list));
        }

        else if (botMode.get() == Mode.Overworld) {
            int radius = 3;
            for (int x = (-radius); x < radius; x++) {
                for (int z = (-radius); z < radius; z++) {
                    for (int y = (radius); y > -radius; y--) {
                        checkPositions.add(new BlockPos(x, y, z));
                    }
                }
            }
        }


        else if (botMode.get() == Mode.Tunnel) {
            positions = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};
            //checkPositions = new ArrayList<BlockPos>(Arrays.asList(new BlockPos[]{new BlockPos(0, -1, 0)})); dirtyish way of doing it, below is cleaner and should
            // work the same
            checkPositions = new ArrayList<>(List.of(new BlockPos(0, -1, 0)));
        }

        if (path == null || path.size() == 0 || isNextPathTooFar() || mc.player.isOnGround()) {
            BlockPos start;
            if (botMode.get() == Mode.Overworld) {
                start = getPlayerPos().add(0, 4, 0);
            } else if (Math.abs(jumpY - mc.player.getY()) <= 2) {
                start = new BlockPos(mc.player.getX(), jumpY + 1, mc.player.getZ());
            } else {
                start = getPlayerPos().add(0, 1, 0);
            }

            if (isNextPathTooFar()) {
                start = getPlayerPos();
            }

            path = AStar.generatePath(start, goal, positions, checkPositions, 500);
        } else {
            ArrayList<BlockPos> temp = AStar.generatePath(path.get(0), goal, positions, checkPositions, 500);
            try {
                temp.addAll(path);
            } catch (NullPointerException ignored) {

            }

            path = temp;
        }
    }


    private class StaticGroundListener {
        @EventHandler
        private void chestSwapGroundListener(PlayerMoveEvent event) {
            if (mc.player != null && mc.player.isOnGround()) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    Modules.get().get(ChestSwap.class).swap();
                    disableGroundListener();
                }
            }
        }
    }

    private final StaticGroundListener staticGroundListener = new StaticGroundListener();

    protected void enableGroundListener() {
        MeteorClient.EVENT_BUS.subscribe(staticGroundListener);
    }

    protected void disableGroundListener() {
        MeteorClient.EVENT_BUS.unsubscribe(staticGroundListener);
    }


    //@EventHandler
    //private void onRecievePacket(PacketEvent.Receive event) {
    //    if (event.packet instanceof EntityStatusS2CPacket p) {
    //        if (p.getEntity(mc.world) == mc.player && p.getStatus() == 35 && toggleOnPop.get()) {
    //            info("Toggling because you popped a totem.");
    //            isRunning = false;
    //        }
    //    }
    //}

    @EventHandler
    private void render3DEvent(Render3DEvent event) {
        if (path != null && renderPath.get()) {
            try {
                for (BlockPos pos : path) {
                    if (last != null) {
                        if (debug.get()) info("Rendering path.");
                        event.renderer.line(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, last.getX() + 0.5, last.getY() + 0.5, last.getZ() + 0.5, pathColour.get());
                    }

                    last = pos;
                }
            }
            catch (Exception exception) {
                last = null;
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        isRunning = false;
    }

    public void useBaritone() {
        ElytraFly.toggle(false);

        int y = (int)(jumpY - mc.player.getY());
        int x = 0;
        int z = 0;

        int blocks = walkDistance.get();
        switch (direction) {
            case ZM: z = -blocks;
            case XM: x = -blocks;
            case XP: x = blocks;
            case ZP: z = blocks;
            case XP_ZP: x = blocks; z = blocks;
            case XM_ZM: x = -blocks; z = -blocks;
            case XP_ZM: x = blocks; z = -blocks;
            case XM_ZP: x = -blocks; z = blocks;
        }

        walkTo(getPlayerPos().add(x, y, z), true);
        sleep(5000);
        sleepUntil(() -> !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), 120000);
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
    }

    private void clickOnFirework() {
        if (MeteorClient.mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET) {
            FindItemResult result = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (result.slot() != -1) {
                InvUtils.swap(result.slot(), false);
            }
        }

        //Click
        MiscUtil.useItem();
        fireworkTimer.reset();
    }

    public BlockPos generateGoalFromDirection(DirectionUtil direction, int up) {
        // since we call mc.player.getX/Y/Z multiple times we should just have them as variables
        // and use those
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (direction == DirectionUtil.ZM) {
            return new BlockPos(0, y + up, z - 30000000);
        } else if (direction == DirectionUtil.ZP) {
            return new BlockPos(0, y + up, z + 30000000);
        } else if (direction == DirectionUtil.XM) {
            return new BlockPos(x - 30000000, y + up, 0);
        } else if (direction == DirectionUtil.XP) {
            return new BlockPos(x + 30000000, y + up, 0);
        } else if (direction == DirectionUtil.XP_ZP) {
            return new BlockPos(x + 30000000, y + up, z + 30000000);
        } else if (direction == DirectionUtil.XM_ZM) {
            return new BlockPos(x - 30000000, y + up, z - 30000000);
        } else if (direction == DirectionUtil.XP_ZM) {
            return new BlockPos(x + 30000000, y + up, z - 30000000);
        } else {
            return new BlockPos(x - 30000000, y + up, z + 30000000);
        }
    }

    private BlockPos getPlayerPos() {
        return new BlockPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private void walkTo(BlockPos goal, boolean sleepUntilDone) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("goto " + goal.getX() + " " + goal.getY() + " " + goal.getZ());

        if (sleepUntilDone) {
            sleepUntil(() -> BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), 100);
            sleepUntil(() -> !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), -1);
        }
    }

    private boolean isNextPathTooFar() {
        try {
            return MiscUtil.distance(getPlayerPos(), path.get(path.size() - 1)) > 15;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopEating(int slot) {
        InvUtils.swap(slot, false);
        mc.options.useKey.setPressed(false);
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {}
    }

    public static void sleepUntil(BooleanSupplier condition, int timeout) {
        long startTime = System.currentTimeMillis();
        while(true) {
            if (condition.getAsBoolean()) {
                break;
            } else if (timeout != -1 && System.currentTimeMillis() - startTime >= timeout) {
                break;
            }

            sleep(10);
        }
    }

    private void onModeChange(FlyMode flyMode) {
        Fireworks = null;
        Time = null;
    }

    // todo:
    // make it toggle off properly - probably need to remove threading
    // make elytra fly work independently of the direction you're facing
    // test it to make sure it works (get others for help - ricky, ghost(?))
    // add scale to hud element
}
