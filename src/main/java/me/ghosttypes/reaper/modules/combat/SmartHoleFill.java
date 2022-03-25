package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmartHoleFill extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Ranges");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Pause");


    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    private final Setting<CheckMode> fillMode = sgGeneral.add(new EnumSetting.Builder<CheckMode>().name("fill-mode").description("When to fill holes.").defaultValue(CheckMode.Either).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("fill-delay").description("Delay between filling holes.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Integer> holesPerTick = sgGeneral.add(new IntSetting.Builder().name("holes-per-tick").description("How many holes to fill per tick.").defaultValue(3).min(0).sliderMax(10).build());
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder().name("target-range").description("How far to target players from.").defaultValue(4).min(1).sliderMax(10).build());
    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Integer> rotatePrio = sgGeneral.add(new IntSetting.Builder().name("rotate-priority").defaultValue(50).min(1).sliderMax(100).build());

    // Ranges
    private final Setting<Integer> selfHoleRangeH = sgRange.add(new IntSetting.Builder().name("self-range-horizontal").description("Horizontal place range.").defaultValue(4).min(0).sliderMax(10).build());
    private final Setting<Integer> selfHoleRangeV = sgRange.add(new IntSetting.Builder().name("self-range-vertical").description("Vertical place range.").defaultValue(2).min(0).sliderMax(10).build());
    private final Setting<Integer> targetHoleRange = sgRange.add(new IntSetting.Builder().name("target-range").description("How close the target needs to be to the hole.").defaultValue(2).min(0).sliderMax(10).build());

    // Pause
    public final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnGap = sgPause.add(new BoolSetting.Builder().name("pause-on-gap").description("Pauses while holding a gap.").defaultValue(false).build());

    // Render
    public final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(114, 11, 135,75)).build());
    public final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(114, 11, 135)).build());

    public SmartHoleFill() {
        super(ML.R, "smart-holefill", "Hole fill but smart");
    }

    private int delayTimer = 0;
    private PlayerEntity target;
    private final ArrayList<BlockPos> renderBlocks = new ArrayList<>();

    @EventHandler
    private void onTick(TickEvent.Post event) {
        FindItemResult item = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!item.found()) {
            error("No obby in hotbar!");
            toggle();
            return;
        }
        // Targeting
        target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (target == null) {
            renderBlocks.clear();
            return;
        }
        // Pauses
        if (debug.get()) info("Checking pauses");
        if (!shouldFill()) {
            if (debug.get()) info("ShouldFill is false");
            return;
        }
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) {
            if (debug.get()) info("Pausing on mine/eat/drink");
            return;
        }
        if (Interactions.isHolding(Items.ENCHANTED_GOLDEN_APPLE) && pauseOnGap.get()) {
            if (debug.get()) info("Pausing on heldGap");
            return;
        }
        delayTimer--;
        if (delayTimer <= 0) {
            delayTimer = delay.get();
            if (debug.get()) info("Getting holes");
            List<BlockPos> holes = BlockHelper.getHoles(mc.player.getBlockPos(), selfHoleRangeH.get(), selfHoleRangeV.get()); // get all nearby holes
            if (debug.get()) info("Starting list size: " + holes.size());
            holes.removeIf(hole -> BlockHelper.distanceBetween(target.getBlockPos(), hole) <= targetHoleRange.get()); // check target distance to hole
            if (debug.get()) info("List size after range check: " + holes.size());
            renderBlocks.addAll(holes);
            int filled = 0;
            for (BlockPos hole : holes) { // iterate through them
                BlockUtils.place(hole, item, rotate.get(), rotatePrio.get(), true);
                renderBlocks.removeIf(renderBlock -> BlockHelper.getBlock(renderBlock) != Blocks.AIR);
                filled++;
                if (filled >= holesPerTick.get()) break;
            }
        }
    }

    private boolean shouldFill() {
        if (debug.get()) info("Checking should fill");
        boolean shouldFill = false;
        switch (fillMode.get()) {
            case Either -> { if (Interactions.isInHole() || Interactions.isBurrowed()) shouldFill = true; }
            case Burrowed -> shouldFill = Interactions.isBurrowed();
            case InHole -> shouldFill = Interactions.isInHole();
            case None -> shouldFill = true;
        }
        return shouldFill;
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            renderBlocks.removeIf(renderBlock -> BlockHelper.getBlock(renderBlock) != Blocks.AIR);
            renderBlocks.forEach(blockPos -> event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0));
        }
    }


    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum CheckMode {
        InHole,
        Burrowed,
        Either,
        None
    }

}
