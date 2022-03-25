package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.BlockHelper.BlockListType;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelfTrapPlus extends ReaperModule {

    public enum PlaceMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum Mode {
        Normal,
        Smart
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").description("Which mode to use.").defaultValue(Mode.Normal).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    private final Setting<Boolean> smartBeds = sgGeneral.add(new BoolSetting.Builder().name("consider-beds").description("Fully trap yourself if a player has beds nearby.").defaultValue(true).build());
    public final Setting<Double> smartRangeBeds = sgGeneral.add(new DoubleSetting.Builder().name("bed-check-range").description("How far to check for players holding beds.").defaultValue(5).min(0).sliderMax(5).build());
    private final Setting<Double> smartDura = sgGeneral.add(new DoubleSetting.Builder().name("smart-dura").description("How low an armor piece needs to be to fully trap.").defaultValue(2).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>().name("place-mode").description("Which positions to place at.").defaultValue(PlaceMode.Top).build());
    private final Setting<Boolean> antiCev = sgGeneral.add(new BoolSetting.Builder().name("anti-cev-breaker").description("Protect yourself from cev breaker.").defaultValue(true).build());
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("How many block placements per tick.").defaultValue(4).sliderMin(1).sliderMax(10).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Centers you on the block you are standing on before placing.").defaultValue(true).build());
    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder().name("turn-off").description("Turns off after placing.").defaultValue(true).build());
    private final Setting<Boolean> toggleOnMove = sgGeneral.add(new BoolSetting.Builder().name("toggle-on-move").description("Turns off if you move (chorus, pearl phase etc).").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Won't place unless you're in a hole").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(true).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    private final List<BlockPos> placePositions = new ArrayList<>();
    private boolean isDone;
    private BlockPos startPos;
    private int bpt;


    public SelfTrapPlus(){
        super(ML.R, "self-trap-plus", "Places obsidian around your head.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
        if (center.get()) PlayerUtils.centerPlayer();
        startPos = mc.player.getBlockPos();
        bpt = 0;
        isDone = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        bpt = 0;
        FindItemResult placeItem = getPlaceItem();
        if (!placeItem.found()) { error("No item to place with"); toggle(); return; }
        if (isDone && turnOff.get()) { info("Finished self trap."); toggle(); return;}
        if (toggleOnMove.get() && startPos != mc.player.getBlockPos()) { toggle(); return; }
        if (onlyInHole.get() && !Interactions.isInHole()) { toggle(); return; }
        if (antiCev.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    BlockPos crystalPos = entity.getBlockPos();
                    if (crystalPos.equals(mc.player.getBlockPos().up(3)) || crystalPos.equals(mc.player.getBlockPos().up(4))) {
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }
                    break;
                }
            }
        }
        for (BlockPos pos : getPlacePositions()) {
            if (!BlockHelper.canPlace(pos)) continue;
            BlockHelper.place(pos, placeItem, rotate.get(), packetPlace.get());
            bpt++;
            if (bpt >= blockPerTick.get()) break;
        }
        isDone = isDone();
    }

    private boolean isDone() {
        for (BlockPos p : getPlacePositions()) if (BlockHelper.canPlace(p)) return false;
        return true;
    }

    private ArrayList<BlockPos> getPlacePositions() {
        ArrayList<BlockPos> positions = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        switch (mode.get()) {
            case Normal -> {
                switch (placeMode.get()) {
                    case Top -> positions.add(center.up(2));
                    case AntiFacePlace -> positions.addAll(BlockHelper.getBlockList(center, BlockListType.DoubleSurround));
                    case Full -> {
                        positions.addAll(BlockHelper.getBlockList(center, BlockListType.SelfTrap));
                        positions.add(center.up(2));
                    }
                }
            }
            case Smart -> {
                boolean full = bedfagNearby() || lowArmor();
                if (full) {
                    positions.addAll(BlockHelper.getBlockList(center, BlockListType.SelfTrap));
                    positions.add(center.up(2));
                } else {
                    positions.add(center.up(2));
                }
            }
        }
        if (antiCev.get()) positions.add(center.up(3));
        return positions;
    }

    private boolean bedfagNearby() {
        if (!smartBeds.get()) return false;
        for (Entity entity : mc.world.getEntities()) if (entity instanceof PlayerEntity player && player != mc.player) if (player.getMainHandStack().getItem() instanceof BedItem || player.getOffHandStack().getItem() instanceof BedItem) if (mc.player.distanceTo(player) < smartRangeBeds.get()) return true;
        return false;
    }

    private boolean lowArmor() {
        for (ItemStack armorPiece : mc.player.getArmorItems()) if (Interactions.checkThreshold(armorPiece, smartDura.get())) return true;
        return false;
    }

    private FindItemResult getPlaceItem() { return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))); }
    private boolean blockFilter(Block block) { return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.NETHERITE_BLOCK || block == Blocks.ENDER_CHEST || block == Blocks.RESPAWN_ANCHOR; }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || isDone) return;
        for (BlockPos bb : getPlacePositions()) if (BlockHelper.isAir(bb)) event.renderer.box(bb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
