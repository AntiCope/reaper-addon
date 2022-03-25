package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.network.PacketManager;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.BlockHelper.BlockListType;
import me.ghosttypes.reaper.util.world.CombatHelper;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ReaperSurround extends ReaperModule {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtension = settings.createGroup("Extensions");
    private final SettingGroup sgRequirement = settings.createGroup("Requirements");
    private final SettingGroup sgAutoToggle = settings.createGroup("AutoToggle");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotate on block interactions.").defaultValue(false).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    //private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder().name("strict").description("For strict servers.").defaultValue(false).build());
    private final Setting<Boolean> forceInstant = sgGeneral.add(new BoolSetting.Builder().name("force-instant").description("Attempt to instantly replace a broken surround block, can cause desync.").defaultValue(false).build());
    private final Setting<Boolean> antiCity = sgGeneral.add(new BoolSetting.Builder().name("anti-city").description("Try to protect you from getting citied.").defaultValue(false).build());
    private final Setting<Boolean> antiCityWait = sgGeneral.add(new BoolSetting.Builder().name("anti-city-wait").description("Wait for the base surround to be finished before anti city activates.").defaultValue(false).visible(antiCity::get).build());
    private final Setting<Integer> antiCityFactor = sgGeneral.add(new IntSetting.Builder().name("anti-city-factor").description("").defaultValue(2).min(0).sliderMax(20).visible(antiCity::get).build());
    private final Setting<Integer> antiCityDelay = sgGeneral.add(new IntSetting.Builder().name("anti-city-delay").description("").defaultValue(3).min(0).sliderMax(20).visible(antiCity::get).build());
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Block placements per tick.").defaultValue(4).min(1).sliderMax(10).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    // additions
    private final Setting<Boolean> antiFall = sgExtension.add(new BoolSetting.Builder().name("anti-fall").description("Protect the block underneath you from being mined.").defaultValue(false).build());
    private final Setting<Boolean> useDouble = sgExtension.add(new BoolSetting.Builder().name("double").description("Place at your feet and head.").defaultValue(false).build());
    private final Setting<Boolean> legacy = sgGeneral.add(new BoolSetting.Builder().name("legacy").description("For 1.12 servers.").defaultValue(false).build());
    //private final Setting<Boolean> extra = sgExtension.add(new BoolSetting.Builder().name("extra").description("Place extra surround blocks.").defaultValue(false).build());
    //private final Setting<Boolean> russian = sgExtension.add(new BoolSetting.Builder().name("russian").description("Russian surround.").defaultValue(false).build());

    // requirements
    private final Setting<Boolean> groundOnly = sgRequirement.add(new BoolSetting.Builder().name("require-ground").description("Only activate when you're on the ground.").defaultValue(true).build());
    private final Setting<Boolean> sneakOnly = sgRequirement.add(new BoolSetting.Builder().name("require-sneak").description("Only activate while you're sneaking.").defaultValue(false).build());

    // auto toggle
    private final Setting<Boolean> disableAfter = sgAutoToggle.add(new BoolSetting.Builder().name("toggle-after").description("Disable after the surround is complete.").defaultValue(false).build());
    private final Setting<Boolean> centerPlayer = sgAutoToggle.add(new BoolSetting.Builder().name("center").description("Center you before starting the surround.").defaultValue(true).build());
    private final Setting<Boolean> disableJump = sgAutoToggle.add(new BoolSetting.Builder().name("toggle-on-jump").description("Disable if you jump.").defaultValue(true).build());
    private final Setting<Boolean> disableYchange = sgAutoToggle.add(new BoolSetting.Builder().name("toggle-on-y-change").description("Disable if your Y coord changes.").defaultValue(true).build());

    // render (placeholders until i do fancier render stuff)
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders where the surround will be placed.").defaultValue(true).build());
    private final Setting<Boolean> alwaysRender = sgRender.add(new BoolSetting.Builder().name("always").description("Render the surround blocks after they are placed.").defaultValue(false).visible(render :: get).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());


    private int crystalDelay, bpt;

    public ReaperSurround() {
        super(ML.R, "reaper-surround", "surround");
    }

    @Override
    public void onActivate() {
        if (centerPlayer.get()) PlayerUtils.centerPlayer();
        crystalDelay = antiCityDelay.get();
        bpt = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) { // building the surround
        bpt = 0;

        boolean done = false;
        if (!useDouble.get() && Interactions.isInHole()) done = true;
        else if (useDouble.get() && CombatHelper.isDoubleSurrounded(mc.player)) done = true;
        if ((disableJump.get() && (mc.options.jumpKey.isPressed() || mc.player.input.jumping)) || (disableYchange.get() && mc.player.prevY < mc.player.getY())) { toggle(); return; }
        if (groundOnly.get() && !mc.player.isOnGround()) return;
        if (sneakOnly.get() && !mc.options.sneakKey.isPressed()) return;

        if (antiCity.get() && !antiCityWait.get()) doAntiCity();
        if (done) {
            if (disableAfter.get()) toggle();
            return;
        }

        ArrayList<BlockPos> toPlace = getPlaceLocations();
        if (toPlace.isEmpty()) return;
        for (BlockPos bp : toPlace) {
            if (BlockHelper.canPlace(bp)) {
                BlockHelper.place(bp, getPlaceItem(), rotation.get(), packetPlace.get());
                bpt++;
            }
            if (bpt >= blockPerTick.get()) break;
        }
        if (bpt < blockPerTick.get()) doAntiCity();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) { // misc shit that can happen at the end of the tick
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof BlockBreakingProgressS2CPacket breakPacket) {
            BlockPos down = mc.player.getBlockPos().down();
            if (antiFall.get() && breakPacket.getPos().equals(down) && breakPacket.getProgress() >= 1) {
                FindItemResult placeItem = getPlaceItem();
                if (placeItem.found()) {
                    PacketManager.sendBurrow();
                    Interactions.setSlot(placeItem.slot(), false);
                    BlockHelper.place(down, placeItem, rotation.get(), true);
                    PacketManager.clipUp(1);
                    Interactions.swapBack();
                }
            }
            if (forceInstant.get() && getPlaceLocations().contains(breakPacket.getPos())) BlockHelper.place(breakPacket.getPos(), getPlaceItem(), rotation.get(), true); // instant placing
        }
    }

    private void doAntiCity() {
        if (crystalDelay > 0) {
            crystalDelay--;
            return;
        }
        int f = 0;
        for (BlockPos pos : getPlaceLocations()) {
            boolean m = BlockHelper.isReplacable(pos);
            for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) if (value.getPos().equals(pos)) { m = true; break; }
            Box pBox = new Box(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            boolean m2 = m;
            Predicate<Entity> ePr = entity -> entity instanceof EndCrystalEntity && m2;
            for (Entity crystal : mc.world.getOtherEntities(null, pBox, ePr)) {
                if (mc.player.distanceTo(crystal) <= 2.6) {
                    PacketManager.sendAttackPacket(crystal, mc.player.isSneaking());
                    f++;
                }
                if (f >= antiCityFactor.get()) break;
            }
        }
    }

    private ArrayList<BlockPos> getPlaceLocations() {
        BlockPos center = mc.player.getBlockPos();
        ArrayList<BlockPos> toPlace = new ArrayList<>();
        ArrayList<BlockPos> mainBlocks = BlockHelper.getBlockList(center, BlockListType.Surround); // main surround blocks
        if (legacy.get()) toPlace = BlockHelper.addLegacyPositions(mainBlocks); // add legacy positions first
        toPlace.addAll(mainBlocks);
        if (useDouble.get()) toPlace.addAll(BlockHelper.getBlockList(center, BlockListType.DoubleSurround));
        return toPlace;
    }

    private FindItemResult getPlaceItem() { return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))); }
    private boolean blockFilter(Block block) { return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.NETHERITE_BLOCK || block == Blocks.ENDER_CHEST || block == Blocks.RESPAWN_ANCHOR; }

    @EventHandler
    private void onRender(Render3DEvent event) { // todo - prettier rendering
        if (render.get()) {
            getPlaceLocations().forEach(bb -> {
                if (BlockHelper.getBlock(bb) == Blocks.AIR) event.renderer.box(bb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (alwaysRender.get()) event.renderer.box(bb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            });
        }
    }
}
