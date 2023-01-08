package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.events.DeathEvent;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.network.PacketManager;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.render.Renderers;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.CombatHelper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;

public class AnchorGod extends ReaperModule {

    private class AnchorPlacement {
        private BlockPos pos;
        private Vec3d vec;
        private BlockHitResult hitResult;

        public AnchorPlacement() {}
        public AnchorPlacement(BlockPos anchorPos) {
            this.set(anchorPos);
        }

        public void set(BlockPos anchorPos) {
            Vec3d v = BlockHelper.vec3d(anchorPos);
            this.pos = anchorPos;
            this.vec = v;
            this.hitResult = new BlockHitResult(BlockHelper.bestHitPos(anchorPos), Direction.UP, anchorPos, true);
        }

        public double getSelfDMG() {
            return DamageUtils.bedDamage(mc.player, this.vec);
        }
        public double getTargetDMG() {
            return DamageUtils.bedDamage(target, this.vec);
        }
        public BlockPos getPos() {return this.pos;}
        public BlockHitResult getHitResult() {return this.hitResult;}
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTurbo = settings.createGroup("Turbo");
    private final SettingGroup sgAutoRefill = settings.createGroup("AutoRefill");
    private final SettingGroup sgAntiCheat = settings.createGroup("AntiCheat");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // general
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    public final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotate for block interactions.").defaultValue(false).build());
    public final Setting<Boolean> fastCalc = sgGeneral.add(new BoolSetting.Builder().name("fast-calc").description("Makes placement and breaking calculation faster at the cost of accuracy.").defaultValue(false).build());
    public final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("Ticks between placing anchors.").defaultValue(9).sliderRange(0, 20).build());
    public final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder().name("break-delay").description("Ticks between exploding anchors.").defaultValue(9).sliderRange(0, 20).build());
    public final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").description("Prevents exploding anchors that will kill you.").defaultValue(true).build());
    public final Setting<Boolean> antiDesync = sgGeneral.add(new BoolSetting.Builder().name("anti-desync").description("Prevents desynced placements and 'glowstone desync'.").defaultValue(false).build());
    public final Setting<Boolean> antiSelfTrap = sgGeneral.add(new BoolSetting.Builder().name("anti-self-trap").description("Automatically mine the target's self trap.").defaultValue(false).build());

    // targeting
    public final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).sliderRange(1, 30).build());
    public final Setting<Integer> xRadius = sgTargeting.add(new IntSetting.Builder().name("x-radius").defaultValue(5).sliderRange(1, 9).build());
    public final Setting<Integer> yRadius = sgTargeting.add(new IntSetting.Builder().name("y-radius").defaultValue(4).sliderRange(1, 5).build());
    public final Setting<Double> placeRange = sgTargeting.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.5).sliderRange(1, 10).build());
    public final Setting<Boolean> flatMode = sgTargeting.add(new BoolSetting.Builder().name("flat-mode").description("Only place on the ground.").defaultValue(false).build());
    public final Setting<Double> minTargetDamage = sgTargeting.add(new DoubleSetting.Builder().name("min-target-damage").description("The minimum damage to inflict on your target.").defaultValue(5.2).range(0, 36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgTargeting.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(4).range(0, 36).sliderMax(36).build());
    public final Setting<Boolean> popOverride = sgTargeting.add(new BoolSetting.Builder().name("pop-override").description("Ignore self damage when you can pop the target without popping yourself.").defaultValue(true).build());
    public final Setting<Double> popOverrideHP = sgTargeting.add(new DoubleSetting.Builder().name("post-hp").description("How much hp you need after ignoring self damage.").defaultValue(4.5).min(0).sliderMax(36).build());
    public final Setting<Boolean> prediction = sgTargeting.add(new BoolSetting.Builder().name("predict").description("Predict the players position next tick for calculations.").defaultValue(false).build());
    public final Setting<Integer> predictionTicks = sgTargeting.add(new IntSetting.Builder().name("predict-ticks").description("How many ticks ahead to predict.").defaultValue(3).sliderRange(1, 5).visible(prediction::get).build());

    // turbo
    public final Setting<Boolean> turbo = sgTurbo.add(new BoolSetting.Builder().name("turbo").description("Increase place/break speed when the target can chain-pop.").defaultValue(false).build());
    public final Setting<Boolean> turboHoleCheck = sgTurbo.add(new BoolSetting.Builder().name("require-target-hole").description("Requires the target to be in a hole.").defaultValue(true).build());
    public final Setting<Boolean> turboHoleCheckSelf = sgTurbo.add(new BoolSetting.Builder().name("require-self-hole").description("Requires you to be in a hole.").defaultValue(false).build());
    public final Setting<Double> turboHP = sgTurbo.add(new DoubleSetting.Builder().name("turbo-activation").description("What health turbo activates at.").defaultValue(10.5).min(0).sliderMax(36).build());
    public final Setting<Integer> turboSpeed = sgTurbo.add(new IntSetting.Builder().name("turbo-speed").description("How fast anchors are placed and exploded when turbo is active.").defaultValue(4).sliderRange(0, 5).build());

    // anti cheat
    public final Setting<Boolean> raytraceSelf = sgAntiCheat.add(new BoolSetting.Builder().name("raytrace-self").description("Raytrace anchors to yourself.").defaultValue(false).build());
    public final Setting<Boolean> raytraceTarget = sgAntiCheat.add(new BoolSetting.Builder().name("raytrace-target").description("Raytrace anchors to the target.").defaultValue(false).build());
    public final Setting<Boolean> strictRange = sgAntiCheat.add(new BoolSetting.Builder().name("strict-ranges").description("Limits place/break range for strict anti cheats.").defaultValue(false).build());
    public final Setting<Boolean> strictMotion = sgAntiCheat.add(new BoolSetting.Builder().name("strict-motion").description("Limits some actions while moving for strict anti cheats.").defaultValue(false).build());
    public final Setting<Boolean> strictInv = sgAntiCheat.add(new BoolSetting.Builder().name("strict-interact").description("Inhibits inventory interactions for strict anti cheats.").defaultValue(false).build());

    // auto refill
    public final Setting<Boolean> autoRefill = sgAutoRefill.add(new BoolSetting.Builder().name("auto-refill").description("Automatically move needed material to the hotbar.").defaultValue(true).build());
    public final Setting<Integer> anchorSlot = sgAutoRefill.add(new IntSetting.Builder().name("anchor-slot").description("Where to move anchors to.").defaultValue(7).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoRefill::get).build());
    public final Setting<Integer> glowstoneSlot = sgAutoRefill.add(new IntSetting.Builder().name("glowstone-slot").description("Where to move glowstone to.").defaultValue(7).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoRefill::get).build());

    // render
    public final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render where the anchor will be placed.").defaultValue(true).build());
    public final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder().name("render-break").description("Render where anchors are broken.").defaultValue(true).build());
    public final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("How long anchor placements are rendered.").defaultValue(3).min(1).sliderMax(10).visible(render::get).build());
    public final Setting<Integer> renderTimeBreak = sgRender.add(new IntSetting.Builder().name("render-time-break").description("How long anchor explosions are rendered.").defaultValue(2).min(1).sliderMax(10).visible(renderBreak::get).build());
    public final Setting<Integer> fadeFactor = sgRender.add(new IntSetting.Builder().name("fade-factor").description("How much the place render fades per tick.").defaultValue(8).min(1).sliderMax(100).visible(render::get).build());
    public final Setting<Integer> fadeFactorBreak = sgRender.add(new IntSetting.Builder().name("fade-factor-break").description("How much the explosion render fades per tick.").defaultValue(8).min(1).sliderMax(100).visible(renderBreak::get).build());
    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How to render the anchor.").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for placements.").defaultValue(new SettingColor(255, 0, 170, 35)).build());
    public final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for placements.").defaultValue(new SettingColor(255, 0, 170)).build());
    public final Setting<SettingColor> sideColorBreak = sgRender.add(new ColorSetting.Builder().name("side-color-break").description("The side color for explosions.").defaultValue(new SettingColor(255, 0, 170, 35)).build());
    public final Setting<SettingColor> lineColorBreak = sgRender.add(new ColorSetting.Builder().name("line-color-break").description("The line color for explosions.").defaultValue(new SettingColor(255, 0, 170)).build());
    public final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Render the damage the anchor will do.").defaultValue(true).build());
    public final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The damage text color.").defaultValue(new SettingColor(15, 255, 211)).build());
    public final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").defaultValue(1.4).min(0).max(5.0).sliderMax(5.0).build());


    private PlayerEntity target;
    private AnchorPlacement placePos, breakPos;
    private Renderers.SimpleAnchorRender anchorRender, breakRender;
    private int placeTimer, breakTimer;
    private long lastTrapMine, lastPlace, lastBreak;

    public AnchorGod() {
        super(ML.R, "anchor-god", "overworld fags coping");
    }

    @Override
    public void onActivate() {
        target = null;
        placePos = null;
        breakPos = null;
        anchorRender = null;
        breakRender = null;
        placeTimer = 0;
        breakTimer = breakDelay.get();
        lastPlace = MathUtil.now();
        lastBreak = MathUtil.now() + breakDelay.get();
        lastTrapMine = MathUtil.now() - 5000;
    }

    @EventHandler
    private void onRender(Render3DEvent event) { // block rendering
        if (render.get()) {
            if (anchorRender != null) if (anchorRender.getPos() != null) event.renderer.box(anchorRender.getPos(), anchorRender.getSideColor(), anchorRender.getLineColor(), shapeMode.get(), 0);
            if (breakRender != null) if (breakRender.getPos() != null) event.renderer.box(breakRender.getPos(), breakRender.getSideColor(), breakRender.getLineColor(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) { // damage rendering
        if (render.get() && renderDamage.get() && anchorRender != null) {
            if (anchorRender.getPos() == null) return;
            Vector3d textVec = BlockHelper.vec3(anchorRender.getPos());
            String damageText = anchorRender.getDamageTxt();
            if (textVec != null && damageText != null) {
                if (NametagUtils.to2D(textVec, damageScale.get())) {
                    NametagUtils.begin(textVec);
                    TextRenderer.get().begin(1.0, false, true);
                    final double w = TextRenderer.get().getWidth(damageText) / 2.0;
                    TextRenderer.get().render(damageText, -w, 0.0, anchorRender.getDamageColor());
                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (anchorRender != null) { // update the render
            anchorRender.tick();
            if (anchorRender.shouldRemove()) anchorRender = null;
        }

        if (breakRender != null) {
            breakRender.tick();
            if (breakRender.shouldRemove()) breakRender = null;
        }

        if (target != null) {
            if (target.deathTime > 0 || target.getHealth() <= 0) MeteorClient.EVENT_BUS.post(DeathEvent.KillEvent.get(target, target.getPos()));
            target = null;
        }
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance); // targeting
        if (TargetUtils.isBadTarget(target, targetRange.get())) return;

        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);

        if (!glowstone.found() && autoRefill.get()) { // refilling glowstone
            if (strictMotion.get() && CombatHelper.isMoving(mc.player)) return;
            FindItemResult g2 = InvUtils.find(Items.GLOWSTONE);
            if (g2.found()) {
                InvUtils.move().from(g2.slot()).toHotbar(glowstoneSlot.get() - 1);
                if (strictInv.get()) return; // only move one item per tick if strict inventory is on
            } else {
                error("No glowstone in hotbar or inventory.");
                toggle();
                return;
            }
        } else if (!glowstone.found() && !autoRefill.get()) {
            error("No glowstone in hotbar.");
            toggle();
            return;
        }

        if (!anchor.found() && autoRefill.get()) { // refilling anchors
            if (strictMotion.get() && CombatHelper.isMoving(mc.player)) return;
            FindItemResult a2 = InvUtils.find(Items.RESPAWN_ANCHOR);
            if (a2.found()) {
                InvUtils.move().from(a2.slot()).toHotbar(anchorSlot.get() - 1);
            } else {
                error("No anchors in hotbar or inventory.");
                toggle();
                return;
            }
        } else if (!anchor.found() && !autoRefill.get()) {
            error("No anchors in hotbar.");
            toggle();
            return;
        }

        /*if (antiSelfTrap.get() && CombatHelper.isTopTrapped(target) && MathUtil.msPassed(lastTrapMine) > 1000 && !strictMotion.get() && BlockHelper.getBlock(target.getBlockPos().up(2)) != Blocks.RESPAWN_ANCHOR) { // anti self trap
            TrapMiner t = Modules.get().get(TrapMiner.class);
            if (!t.isActive()) t.toggle();
            return;
        }*/

        if (placeTimer <= 0) {
            placePos = getPlacePos(); // find place pos and set the break pos
            placeAnchor(placePos); // place the anchor
            placeTimer = getPlaceDelay();
        } else {
            placeTimer--;
        }

        if (breakTimer <= 0) {
            if (!placeCheck(breakPos)) { // break the anchor
                breakAnchor(findBreak());
            } else {
                breakAnchor(breakPos);
            }
            breakTimer = getBreakDelay();
        } else {
            breakTimer--;
        }

    }

    private boolean placeCheck(AnchorPlacement placement) {
        if (placement == null) return false;
        return placement.getPos() != null;
    }

    private int getPlaceDelay() {
        if (target == null) return placeDelay.get();
        if (turbo.get()) { // see if we should use the turbo delay
            if (turboHoleCheck.get() && !CombatHelper.isInHole(target)) return placeDelay.get();
            if (turboHoleCheckSelf.get() && !CombatHelper.isInHole(mc.player)) return placeDelay.get();
            if (EntityUtils.getTotalHealth(target) > turboHP.get()) return placeDelay.get();
            return turboSpeed.get();
        }
        return placeDelay.get();
    }


    private int getBreakDelay() {
        if (target == null) return breakDelay.get();
        if (turbo.get()) { // see if we should use the turbo delay
            if (turboHoleCheck.get() && !CombatHelper.isInHole(target)) return breakDelay.get();
            if (turboHoleCheckSelf.get() && !CombatHelper.isInHole(mc.player)) return breakDelay.get();
            if (EntityUtils.getTotalHealth(target) > turboHP.get()) return breakDelay.get();
            return turboSpeed.get();
        }
        return breakDelay.get();
    }

    private void placeAnchor(AnchorPlacement placement) {
        if (placement == null || target == null) {
            if (debug.get()) error("placeAnchor null placement");
            return;
        }
        breakPos = placement;
        BlockPos pos = placement.getPos();
        if (debug.get()) info("placing anchor");
        boolean swapBack = false;
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        if (!anchor.found()) {
            if (debug.get()) warning("placeAnchor no anchors?");
            return;
        }
        if (!Interactions.isHolding(Items.RESPAWN_ANCHOR)) { // only need to swap and swap back if we aren't holding an anchor
            swapBack = true;
            Interactions.setSlot(anchor.slot(), false);
        }
        BlockHelper.place(pos, anchor, rotate.get(), packetPlace.get());
        if (swapBack) Interactions.swapBack();
    }

    private void breakAnchor(AnchorPlacement placement) {
        if (placement == null || target == null) {
            if (debug.get()) error("breakAnchor null placement");
            return;
        }
        BlockPos pos = placement.getPos();
        BlockHitResult hitResult = placement.getHitResult();
        if (pos == null || hitResult == null) {
            if (debug.get()) error("breakAnchor null placement pos or hitresult");
            return;
        }
        if (BlockHelper.getBlock(pos) != Blocks.RESPAWN_ANCHOR) { // prevent "glowstone desync"
            if (debug.get()) warning("desync detected, resetting breakPos");
            if (BlockHelper.getBlock(pos) == Blocks.GLOWSTONE && antiDesync.get()) {
                if (debug.get()) error("glowstone desync detected, resetting breakPos and mining");
                PacketManager.startPacketMine(pos, true, true);
                PacketManager.finishPacketMine(pos, true, true);
            }
            breakPos = null;
            return;
        }
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!anchor.found() || !glowstone.found()) {
            if (debug.get()) warning("breakAnchor no glowstone or anchors?");
            return;
        }

        if (debug.get()) info("exploding anchor");
        if (renderBreak.get()) breakRender = new Renderers.SimpleAnchorRender(placement.getPos(), renderTimeBreak.get(), sideColorBreak.get(), lineColorBreak.get(), damageColor.get(), fadeFactorBreak.get(), 0.0);

        Hand glowHand = Hand.MAIN_HAND;
        Hand ancrHand = Hand.MAIN_HAND;
        if (glowstone.isOffhand()) glowHand = Hand.OFF_HAND;
        if (anchor.isOffhand()) ancrHand = Hand.OFF_HAND;

        PacketManager.sendInteract(glowHand, glowstone, hitResult, rotate.get(), packetPlace.get());
        PacketManager.sendInteract(ancrHand, anchor, hitResult, false, packetPlace.get());

        if (antiDesync.get()) {
            if (debug.get()) info("sending anti desync (post-explode)");
            PacketManager.startPacketMine(pos, true, true);
            PacketManager.finishPacketMine(pos, true, true);
        }

        if (!strictInv.get() || !strictMotion.get()) Interactions.setSlot(anchor.slot(), false); // swap to anchors rather than the "old slot" because we need to be holding anchors to place again anyways
    }

    private AnchorPlacement findBreak() {
        if (target == null) return null;
        if (TargetUtils.isBadTarget(target, targetRange.get())) return null;
        ArrayList<BlockPos> toScan = new ArrayList<>(getSphereArray(target, xRadius.get(), yRadius.get()));
        AnchorPlacement placement = new AnchorPlacement(); // init the placements
        AnchorPlacement finalPlacement = new AnchorPlacement();
        for (BlockPos p : toScan) {
            if (BlockHelper.getBlock(p) != Blocks.RESPAWN_ANCHOR) continue;
            if (!placeCheck(p)) continue; // check if we can place, distance, and strict settings
            if (flatMode.get() && !BlockHelper.isSolid(p.down())) continue;
            placement.set(p);
            double selfDMG = placement.getSelfDMG();
            if (selfDMG > maxSelfDamage.get()) continue;
            if (antiSuicide.get() && PlayerUtils.getTotalHealth() - selfDMG <= 0) continue;
            finalPlacement.set(p);
            break;
        }
        return finalPlacement;
    }

    private AnchorPlacement getPlacePos() {
        if (target == null) return null;
        if (TargetUtils.isBadTarget(target, targetRange.get())) return null;
        ArrayList<BlockPos> toScan = new ArrayList<>(getSphereArray(target, xRadius.get(), yRadius.get()));
        AnchorPlacement placement = new AnchorPlacement(); // init the placements
        AnchorPlacement finalPlacement = new AnchorPlacement();
        double bestDamage = 0;
        long start = MathUtil.now();
        int calcs = 0;
        if (debug.get()) info("place calculation started");
        for (BlockPos p : toScan) {
            if (!placeCheck(p)) continue; // check if we can place, distance, and strict settings
            if (p.equals(target.getBlockPos()) || p.equals(target.getBlockPos().up())) continue;
            if (flatMode.get() && !BlockHelper.isSolid(p.down())) continue;
            placement.set(p);
            calcs++;
            double selfDMG = placement.getSelfDMG(); // calculate damages
            double targetDMG = placement.getTargetDMG();
            if (antiSuicide.get() && PlayerUtils.getTotalHealth() - selfDMG <= 0) continue;
            if (popOverride.get()) { // check self damage & anti suicide
                if (EntityUtils.getTotalHealth(target) - targetDMG > 0 && PlayerUtils.getTotalHealth() - selfDMG < popOverrideHP.get()) continue;
            } else {
                if (selfDMG > maxSelfDamage.get()) continue;
            } // check target damage
            if (targetDMG < minTargetDamage.get()) continue;
            if (targetDMG > bestDamage) {
                bestDamage = targetDMG;
                finalPlacement.set(p);
                if (fastCalc.get()) break;
            }
        }
        if (debug.get()) info("calculated place position in " + MathUtil.msPassed(start) + "ms (checked " + calcs + " positions)");
        if (render.get()) anchorRender = new Renderers.SimpleAnchorRender(finalPlacement.getPos(), renderTime.get(), sideColor.get(), lineColor.get(), damageColor.get(), fadeFactor.get(), bestDamage);
        return finalPlacement;
    }

    private boolean placeCheck(BlockPos anchor) {
        if (!BlockHelper.canPlace(anchor)) return false;
        if (BlockHelper.distanceTo(anchor) > placeRange.get()) return false;
        if (MathUtil.intersects(anchor)) return false;
        return strictCheck(anchor);
    }

    private boolean strictCheck(BlockPos anchor) {
        if (raytraceSelf.get() && !raytraceAnchor(mc.player, anchor)) return false;
        if (raytraceTarget.get() && !raytraceAnchor(target, anchor)) return false;
        return !strictRange.get() || !(BlockHelper.distanceTo(anchor) > mc.interactionManager.getReachDistance());
    }

    private boolean raytraceAnchor(Entity e, BlockPos anchor) {
        return BlockHelper.canSee(e, anchor);
    }

    private ArrayList<BlockPos> getSphereArray(PlayerEntity target, int xRadius, int yRadius) {
        ArrayList<BlockPos> ar = new ArrayList<>();
        BlockPos tPos = target.getBlockPos();
        BlockPos tPos2 = target.getBlockPos().up();
        if (prediction.get() && CombatHelper.isMoving(target) && !CombatHelper.isInHole(target)) tPos = MathUtil.generatePredict(tPos, target, predictionTicks.get());
        BlockPos.Mutable p = new BlockPos.Mutable();

        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    p.set(tPos).move(x, y, z);
                    BlockPos p2 = p.toImmutable();
                    if (p2.equals(tPos) || p2.equals(tPos2)) continue;
                    if (MathHelper.sqrt((float) ((tPos.getX() - p2.getX()) * (tPos.getX() - p2.getX()) + (tPos.getZ() - p2.getZ()) * (tPos.getZ() - p2.getZ()))) <= xRadius && MathHelper.sqrt((float) ((tPos.getY() - p2.getY()) * (tPos.getY() - p2.getY()))) <= yRadius)
                    { if (!ar.contains(p2)) ar.add(p2);}
                }
            }
        }
        ar.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return ar;
    }
}
