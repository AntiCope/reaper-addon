package me.ghosttypes.reaper.modules.render;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.render.Renderers;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.CombatHelper;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class ReaperHoleESP extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    private final Setting<Boolean> debugVerbose = sgGeneral.add(new BoolSetting.Builder().name("verbose-debug").defaultValue(false).build());
    private final Setting<Integer> rRange = sgGeneral.add(new IntSetting.Builder().name("radius").defaultValue(10).min(1).sliderMax(15).build());
    private final Setting<Integer> yRange = sgGeneral.add(new IntSetting.Builder().name("y-range").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Integer> renderTime = sgGeneral.add(new IntSetting.Builder().name("render-time").defaultValue(1).min(1).sliderMax(10).build());
    private final Setting<Integer> fadeFactor = sgGeneral.add(new IntSetting.Builder().name("fade-factor").defaultValue(10).min(1).sliderMax(50).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> bedrockColor = sgGeneral.add(new ColorSetting.Builder().name("bedrock-color").defaultValue(new SettingColor(100, 255, 0, 0)).build());
    private final Setting<SettingColor> obsidianColor = sgGeneral.add(new ColorSetting.Builder().name("obsidian-top").defaultValue(new SettingColor(255, 0, 0, 200)).build());
    private final Setting<SettingColor> mixedColor = sgGeneral.add(new ColorSetting.Builder().name("mixed-top").defaultValue(new SettingColor(255, 127, 0, 200)).build());

    private int rendersAdded, rendersRemoved;
    private final ArrayList<Renderers.SimpleBlockFadeIn> renders = new ArrayList<>();
    private final ArrayList<Renderers.SimpleBlockRender> removingRenders = new ArrayList<>();

    public ReaperHoleESP() { super(ML.R, "reaper-hole-esp", "Renders nearby holes"); }

    @Override
    public void onActivate() {
        rendersAdded = 0;
        rendersRemoved = 0;
        renders.clear();
        removingRenders.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // reset renders added this tick
        rendersAdded = 0;
        rendersRemoved = 0;

        // deal with current renders
        if (!renders.isEmpty()) {
            if (debugVerbose.get()) info("DEBUG: Ticking " + renders.size() + " renders");
            // tick each render
            renders.forEach(Renderers.SimpleBlockFadeIn::tick);
            renders.forEach(simpleBlockFadeIn -> check(simpleBlockFadeIn.getPos()));
            // remove once its render ticks expire, and its no longer 'valid'
            renders.removeIf(h -> !isValid(h.getPos()));
        }

        if (!removingRenders.isEmpty()) {
            removingRenders.forEach(Renderers.SimpleBlockRender::tick);
            removingRenders.removeIf(Renderers.SimpleBlockRender::shouldRemove);
        }

        // Find new holes to render
        ArrayList<CombatHelper.Hole> holess = CombatHelper.getHoles(rRange.get(), yRange.get());
        if (holess == null) {
            if (debugVerbose.get()) info("DEBUG: getHoles returned null");
            return;
        }

        if (!holess.isEmpty()) {
            if (debug.get()) info("DEBUG: Checking " + holess.size() + " holes");
            for (CombatHelper.Hole hole : holess) {
                if (isGoodNewHole(hole.getPos())) {
                    rendersAdded++; // increase renders added this tick (for cool rendering stuff)
                    int renderT = renderTime.get();
                    renderT += IntStream.range(0, rendersAdded).map(i -> (i * 10)).sum(); // increase the rendering time by how many new renders have been added
                    Color bedrock = Formatter.sToMC(bedrockColor.get()); // colors
                    Color obby = Formatter.sToMC(obsidianColor.get());
                    Color mixed = Formatter.sToMC(mixedColor.get());
                    switch (hole.getType()) { // add the render
                        case Bedrock -> renders.add(new Renderers.SimpleBlockFadeIn(hole.getPos(), renderT, bedrock, bedrock, fadeFactor.get() + rendersAdded));
                        case Obsidian -> renders.add(new Renderers.SimpleBlockFadeIn(hole.getPos(), renderT, obby, obby, fadeFactor.get() + rendersAdded));
                        case Mixed -> renders.add(new Renderers.SimpleBlockFadeIn(hole.getPos(), renderT, mixed, mixed, fadeFactor.get() + rendersAdded));
                    }
                } else {
                    if (debugVerbose.get()) info("Invalid or old hole found, skipping");
                }
            }
        } else {
            if (debugVerbose.get()) info("DEBUG: getHoles returned empty list");
        }
    }


    private boolean isGoodNewHole(BlockPos pos) {
        for (Renderers.SimpleBlockFadeIn fadeIn : renders) if (fadeIn.getPos().equals(pos)) return false;
        return true;
    }

    private boolean isValid(BlockPos pos) {
        return CombatHelper.isValidHole(pos) && !(BlockHelper.distanceTo(pos) > rRange.get());
    }

    private void check(BlockPos pos) {
        if (!isValid(pos)) {
            boolean shouldRender = true;
            for (Renderers.SimpleBlockRender r : removingRenders) {
                if (r.getPos().equals(pos)) {
                    shouldRender = false;
                    break;
                }
            }
            rendersRemoved++;
            int renderT = renderTime.get();
            renderT += IntStream.range(0, rendersRemoved).map(i -> (i * 10)).sum();
            if (shouldRender) { // fade the hole back out once its invalid
                Color bedrock = Formatter.sToMC(bedrockColor.get()); // colors
                Color obby = Formatter.sToMC(obsidianColor.get());
                Color mixed = Formatter.sToMC(mixedColor.get());

                CombatHelper.HoleType type = CombatHelper.getHoleType(pos);
                if (type == CombatHelper.HoleType.Invalid) return;

                switch (type) { // add the render
                    case Bedrock -> removingRenders.add(new Renderers.SimpleBlockRender(pos, renderT, bedrock, bedrock, fadeFactor.get()));
                    case Obsidian -> removingRenders.add(new Renderers.SimpleBlockRender(pos, renderT, obby, obby, fadeFactor.get()));
                    case Mixed -> removingRenders.add(new Renderers.SimpleBlockRender(pos, renderT, Formatter.sToMC(mixedColor.get()), mixed, fadeFactor.get()));
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renders.isEmpty()) renders.forEach(render -> {
            if (isValid(render.getPos())) event.renderer.box(render.getPos(), render.getSideColor(), render.getLineColor(), shapeMode.get(), 0);
        });
        if (!removingRenders.isEmpty()) removingRenders.forEach(render -> {
            if (isValid(render.getPos())) event.renderer.box(render.getPos(), render.getSideColor(), render.getLineColor(), shapeMode.get(), 0);
        });
    }
}
