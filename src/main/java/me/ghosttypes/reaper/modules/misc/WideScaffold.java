package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.render.Renderers.SimpleBlockRender;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class WideScaffold extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(1).min(1).sliderMax(10).build());
    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").defaultValue(3).min(1).sliderMax(5).build());
    private final Setting<Integer> rRange = sgGeneral.add(new IntSetting.Builder().name("radius").defaultValue(5).min(1).sliderMax(5).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("blocks").build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").defaultValue(1).min(1).sliderMax(10).visible(render::get).build());
    private final Setting<Integer> fadeFactor = sgRender.add(new IntSetting.Builder().name("fade-factor").defaultValue(10).min(1).sliderMax(50).build());
    private final Setting<Integer> increaseFactor = sgRender.add(new IntSetting.Builder().name("increase-factor").defaultValue(8).min(1).sliderMax(20).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(15, 255, 211)).build());

    private int blocksPlaced, rendersAdded, timer;
    private final ArrayList<SimpleBlockRender> renders = new ArrayList<>();

    public WideScaffold() {
        super(ML.M, "wide-scaffold", "Scaffold but wider.");
    }

    @Override
    public void onActivate() {
        renders.clear();
        blocksPlaced = 0;
        rendersAdded = 0;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (!renders.isEmpty()) {
            renders.forEach(SimpleBlockRender::tick);
            renders.removeIf(SimpleBlockRender::shouldRemove);
        }

        if (timer <= 0) {
            timer = delay.get();
        } else {
            timer--;
            return;
        }

        // reset
        blocksPlaced = 0;
        rendersAdded = 0;

        // Get nearby blocks
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos belowPos = playerPos.down();
        List<BlockPos> nearby = BlockHelper.getSphere(playerPos, rRange.get(), 1);
        // Remove any blocks not on our y level
        nearby.removeIf(blockPos -> blockPos.getY() != belowPos.getY());
        // Remove any blocks we can't place
        nearby.removeIf(blockPos -> !BlockUtils.canPlace(blockPos));
        // Sort all the blocks by shortest -> the longest distance
        nearby.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        // Place the scaffold blocks
        for (BlockPos pos : nearby) {
            if (blocksPlaced >= blocksPerTick.get()) break;
            placeScaffoldBlock(pos);
            // create a render for the block after its placed
            int renderT = renderTime.get();
            renderT += IntStream.range(0, rendersAdded).map(i -> (increaseFactor.get() * i)).sum();
            renders.add(new SimpleBlockRender(pos, renderT, sideColor.get(), lineColor.get(), fadeFactor.get()));
        }
    }


    private void placeScaffoldBlock(BlockPos pos) {
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) return;
        blocksPlaced++;
        rendersAdded++;
        BlockHelper.place(pos, block, rotation.get(), packetPlace.get());
    }

    // Rendering
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && !renders.isEmpty()) renders.forEach(renderBlock -> event.renderer.box(renderBlock.getPos(), renderBlock.getSideColor(), renderBlock.getLineColor(), shapeMode.get(), 0));
    }
}
