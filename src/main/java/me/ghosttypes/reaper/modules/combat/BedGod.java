package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.events.DeathEvent;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.network.PacketManager;
import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.player.Interactions.MineInstance;
import me.ghosttypes.reaper.util.render.Renderers.SimpleBedRender;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.CombatHelper;
import me.ghosttypes.reaper.util.world.DamageCalculator;
import me.ghosttypes.reaper.util.world.DamageCalculator.BedPlacement;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.List;

public class BedGod extends ReaperModule {

    public enum RenderMode {
        Single,
        Outline
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDamages = settings.createGroup("Damages");
    private final SettingGroup sgZeroTick = settings.createGroup("ZeroTick");
    private final SettingGroup sgAutoCraft = settings.createGroup("AutoCraft");
    private final SettingGroup sgAntiHoleFag = settings.createGroup("AntiHoleFag");
    private final SettingGroup sgSync = settings.createGroup("Sync");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // general
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("cope").defaultValue(false).build());
    //public final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder().name("strict").description("cope").defaultValue(true).build());
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder().name("packet-place").defaultValue(false).build());
    private final Setting<Boolean> packetBreak = sgGeneral.add(new BoolSetting.Builder().name("packet-break").defaultValue(false).build());
    public final Setting<Boolean> strictGround = sgGeneral.add(new BoolSetting.Builder().name("ground-only").description("Only place on the ground.").defaultValue(false).build());
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(10).description("The delay between bed placements.").sliderRange(0, 20).build());
    public final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder().name("smart-delay").description("Only place when the target will take full damage.").defaultValue(false).build());
    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").description("How far to look for targets.").defaultValue(16).sliderRange(1, 30).build());
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(4.6).description("The range for bed placements.").sliderRange(1, 30).build());
    public final Setting<Integer> xRadius = sgGeneral.add(new IntSetting.Builder().name("x-radius").defaultValue(5).sliderRange(1, 9).build());
    public final Setting<Integer> yRadius = sgGeneral.add(new IntSetting.Builder().name("y-radius").defaultValue(4).sliderRange(1, 5).build());
    public final Setting<DamageCalculator.BlockSortMode> blockSortMode = sgGeneral.add(new EnumSetting.Builder<DamageCalculator.BlockSortMode>().name("sort-filter").description("How to sort nearby blocks.").defaultValue(DamageCalculator.BlockSortMode.CloseToSelf).build());
    public final Setting<Boolean> autoRefill = sgGeneral.add(new BoolSetting.Builder().name("auto-refill").description("Automatically move beds to your hotbar.").defaultValue(true).build());
    public final Setting<Integer> refillSlot = sgGeneral.add(new IntSetting.Builder().name("refill-slot").description("What hotbar slot to move beds to.").defaultValue(7).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoRefill::get).build());
    public final Setting<Boolean> bedPerSec = sgGeneral.add(new BoolSetting.Builder().name("bed-per-sec").description("Try to calculate how many beds you place per second.").defaultValue(false).build());
    public final Setting<Boolean> showLastCalc = sgGeneral.add(new BoolSetting.Builder().name("show-last-calc").description("Displays when the last break/placement calculation was.").defaultValue(true).build());

    // damages
    public final Setting<Boolean> ignoreBurrow = sgGeneral.add(new BoolSetting.Builder().name("ignore-burrow").description("Ignore burrowed targets.").defaultValue(false).build());
    public final Setting<Boolean> ignoreSelfHole = sgGeneral.add(new BoolSetting.Builder().name("ignore-self-hole").description("Prevents placing on yourself if the target is in your hole.").defaultValue(true).build());
    public final Setting<Double> minTargetDamage = sgDamages.add(new DoubleSetting.Builder().name("min-target-damage").description("The min damage to deal to the target.").defaultValue(6.5).range(0, 36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgDamages.add(new DoubleSetting.Builder().name("max-self-damage").description("The max damage to deal to yourself.").defaultValue(3.85).range(0, 36).sliderMax(36).build());
    public final Setting<Boolean> antiSuicide = sgDamages.add(new BoolSetting.Builder().name("anti-suicide").description("Prevent breaking beds that kill you.").defaultValue(false).build());

    // zero tick
    public final Setting<Boolean> smartZeroTick = sgZeroTick.add(new BoolSetting.Builder().name("smart-zero-tick").description("Use zero tick delay automatically when needed.").defaultValue(false).build());
    public final Setting<Boolean> zeroTickCheckHole = sgZeroTick.add(new BoolSetting.Builder().name("require-target-hole").description("Only zero ticks if the target is in a hole").defaultValue(false).build());
    public final Setting<Double> zeroTickActivateAt = sgZeroTick.add(new DoubleSetting.Builder().name("activate-at-health").description("What health the target needs to be at to zero tick them.").defaultValue(10.25).range(0, 36).sliderMax(36).build());
    public final Setting<Integer> zeroTickMinBed = sgZeroTick.add(new IntSetting.Builder().name("min-beds").description("How many beds you need to have to use smart zero tick.").defaultValue(5).sliderRange(1, 10).visible(smartZeroTick::get).build());

    // auto craft
    public final Setting<Boolean> autoCraft = sgAutoCraft.add(new BoolSetting.Builder().name("auto-craft").description("Automatically craft beds when you're out.").defaultValue(false).build());
    public final Setting<Boolean> autoCraftInfo = sgAutoCraft.add(new BoolSetting.Builder().name("auto-craft-info").description("Alerts you when refilling.").defaultValue(false).build());
    public final Setting<Boolean> autoCraftPause = sgAutoCraft.add(new BoolSetting.Builder().name("pause").description("Pauses placing/breaking while crafting.").defaultValue(false).build());
    public final Setting<Boolean> autoClose = sgAutoCraft.add(new BoolSetting.Builder().name("auto-close").description("Automatically close the crafting table when done.").defaultValue(false).build());
    public final Setting<Integer> autoCraftStopAt = sgAutoCraft.add(new IntSetting.Builder().name("stop-at").defaultValue(4).description("Number of beds in your inventory to stop crafting.").sliderRange(0, 20).build());
    public final Setting<Integer> autoCraftEmptySlots = sgAutoCraft.add(new IntSetting.Builder().name("min-empty-slot").defaultValue(6).description("Required empty slots to start crafting.").sliderRange(0, 20).build());
    public final Setting<Boolean> autoCraftRequireHole = sgAutoCraft.add(new BoolSetting.Builder().name("check-hole").description("Only refill while you're in a hole.").defaultValue(true).build());
    public final Setting<Boolean> autoCraftRequireTotem = sgAutoCraft.add(new BoolSetting.Builder().name("require-totem").description("Only refill while you're holding a totem.").defaultValue(true).build());
    public final Setting<Integer> autoCraftTableDelay = sgAutoCraft.add(new IntSetting.Builder().name("table-delay").defaultValue(10).description("The delay between placing crafting tables.").sliderRange(0, 20).build());

    // anti hole fag
    public final Setting<Boolean> antiSelfTrap = sgAntiHoleFag.add(new BoolSetting.Builder().name("anti-self-trap").description("Automatically mine the target's self trap.").defaultValue(false).build());
    public final Setting<Boolean> antiBurrow = sgAntiHoleFag.add(new BoolSetting.Builder().name("anti-burrow").description("Automatically mine the target's burrow.").defaultValue(false).build());
    public final Setting<Boolean> antiString = sgAntiHoleFag.add(new BoolSetting.Builder().name("anti-string").description("Automatically mine the target's string/web.").defaultValue(false).build());
    public final Setting<Boolean> antiRequireHole = sgAntiHoleFag.add(new BoolSetting.Builder().name("require-hole").description("Only automatically mines if you're in a hole.").defaultValue(false).build());

    // sync
    public final Setting<Boolean> autoCraftSync = sgSync.add(new BoolSetting.Builder().name("sync-auto-craft").description("Syncs your inventory when closing crafting tables.").defaultValue(false).build());
    public final Setting<Boolean> autoRefillSync = sgSync.add(new BoolSetting.Builder().name("sync-auto-refill").description("Syncs your inventory when moving beds to the hotbar.").defaultValue(true).build());
    public final Setting<Boolean> slotSync = sgSync.add(new BoolSetting.Builder().name("sync-refill-slot").description("Periodically click your refill slot to prevent desync.").defaultValue(true).build());
    public final Setting<Integer> refillSyncTicks = sgSync.add(new IntSetting.Builder().name("refill-sync-delay").defaultValue(30).description("Ticks between inventory syncs.").sliderRange(0, 35).visible(autoRefillSync::get).build());
    public final Setting<Integer> slotSyncTicks = sgSync.add(new IntSetting.Builder().name("slot-sync-delay").defaultValue(30).description("Ticks between clicking the refill slot.").sliderRange(0, 35).visible(slotSync::get).build());

    // rendering
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    public final Setting<Integer> bedRenderTime = sgRender.add(new IntSetting.Builder().name("render-time").defaultValue(3).min(1).sliderMax(10).visible(render::get).build());
    public final Setting<Integer> bedFadeTime = sgRender.add(new IntSetting.Builder().name("fade-factor").defaultValue(8).min(1).sliderMax(100).visible(render::get).build());
    public final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("How the render beds.").defaultValue(RenderMode.Outline).build());
    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("Side color.").defaultValue(new SettingColor(255, 0, 170, 35)).build());
    public final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("Line color.").defaultValue(new SettingColor(255, 0, 170)).build());
    public final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").defaultValue(1.4).min(0).max(5.0).sliderMax(5.0).build());
    public final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").defaultValue(new SettingColor(15, 255, 211)).build());


    private PlayerEntity target;
    private BedPlacement placePos;
    private SimpleBedRender bedRender;
    private MineInstance trapMine, burrowMine;
    private int timer, tableTimer, syncTimer, refillTimer, clickTimer, bedsPlaced;
    private String lastCalc, status;
    private boolean alertedCraft;
    private long bedTimer;

    public BedGod() {
        super(ML.R, "bed-god", "the best bed aura");
    }

    @Override
    public void onActivate() {
        target = null;
        timer = 0;
        syncTimer = 0;
        refillTimer = 0;
        clickTimer = 45;
        tableTimer = autoCraftTableDelay.get();
        trapMine = null;
        burrowMine = null;
        bedsPlaced = 0;
        bedTimer = MathUtil.now();
    }


    @EventHandler
    private void onPostTick(TickEvent.Post event) { // stuff that doesn't need to be handled 'quickly' gets done post-tick
        if (bedRender != null) { // tick render
            bedRender.tick();
            if (bedRender.shouldRemove()) bedRender = null;
        }
        if (autoCraftSync.get() && Interactions.isCrafting()) { // inventory syncing
            syncTimer--;
            if (syncTimer <= 0) {
                syncTimer = refillSyncTicks.get();
                mc.player.getInventory().updateItems();
            }
        }
        if (slotSync.get()) { // refill slot syncing
            clickTimer--;
            if (clickTimer <= 0) {
                clickTimer = slotSyncTicks.get();
                InvUtils.click().slot(refillSlot.get() - 1);
            }
        }
        if (bedPerSec.get() && MathUtil.msPassed(bedTimer) >= 1000) {
            bedsPlaced = 0;
            bedTimer = MathUtil.now();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        timer--;
        if (autoCraft.get()) doAutoCraft(); // auto craft + move bed to hotbar if needed
        doRefill();

        if (autoCraftPause.get() && Interactions.isCrafting()) { // auto craft pause check
            if (!alertedCraft && autoCraftInfo.get()) {
                info("Refilling beds.");
                alertedCraft = true;
            }
            return;
        }
        alertedCraft = false;

        if (target != null) {
            if (target.deathTime > 0 || target.getHealth() <= 0) MeteorClient.EVENT_BUS.post(DeathEvent.KillEvent.get(target, target.getPos()));
            target = null;
        }
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance); // targeting
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
            placePos = null;
            return;
        }

        if (ignoreBurrow.get() && CombatHelper.isBurrowed(target) || // ignore burrow / ignore self hole checks
            ignoreSelfHole.get() && target.getBlockPos().equals(mc.player.getBlockPos())
        ) {
            placePos = null;
            bedRender = null;
            return;
        }

        if (antiSelfTrap.get()) { // anti camper checks
            doAntiTrap();
            if (trapMine != null) return;
        }
        if (antiBurrow.get()) {
            doAntiBurrow();
            if (burrowMine != null) return;
        }
        if (antiString.get()) doAntiString();

        if (timer <= 0) {
            timer = delay.get();
            if (smartDelay.get() && target.isInSwimmingPose()) {
                if (debug.get()) info("target in swimming pose, skipping");
                return; // smart delay
            }
            if (canZeroTick()) timer = getZeroTick();
            doBomb(); // place / break
        }
    }

    private boolean canZeroTick() {
        if (!smartZeroTick.get() || EntityUtils.getTotalHealth(target) > zeroTickActivateAt.get()) return false;
        return !zeroTickCheckHole.get() || CombatHelper.isInHole(target);
    }

    private int getZeroTick() {
        int t;
        if (!CombatHelper.isInHole(target)) {
            if (Interactions.findBedInAll().count() >= zeroTickMinBed.get()) t = Formatter.randInt(0, 3);
            else t = 5;
        } else {
            if (Interactions.findBedInAll().count() < zeroTickMinBed.get()) {
                t = Formatter.randInt(2, 4);
            } else {
                t = Formatter.randInt(0, 3);
                if (CombatHelper.isSelfTrapped(target)) t = 0; // 0 tick delay for self trap
            }
        }
        if (debug.get()) info("Current zero-tick delay: " + t);
        return t;
    }

    private void doBomb() {
        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler && autoCraftPause.get()) return;
        long start = MathUtil.now();
        if (!placeCheck(placePos)) { // check if we have a current place pos
            if (debug.get()) info("invalid place pos, recalculating.");
            setPlacePos(); // recalculate if not
        } else { // recalculate if the current pos isn't valid
            if (debug.get()) info("re-checking last place pos.");
            if (!placePos.isStillValid(target, minTargetDamage.get(), maxSelfDamage.get())) {
                info("invalid, recalculating.");
                setPlacePos();
            } else if (debug.get()) {
                info("place pos still valid, reusing");
            }
        }
        if (!placeCheck(placePos)) { // look for a bed to break if we still don't have a place pos
            if (debug.get()) info("final place pos is invalid, doing break");
            status = "[Break]";
            doBreak(DamageCalculator.getBreakPos(target, range.get(), maxSelfDamage.get(), antiSuicide.get()));
            lastCalc = MathUtil.millisElapsed(start);
            if (debug.get()) info("Calculated break pos in " + lastCalc);
            return;
        }
        lastCalc = MathUtil.millisElapsed(start);
        if (debug.get()) info("Calculated placement in " + lastCalc + " , placing + exploding");
        doBed(placePos.getPos(), placePos.getVec(), placePos.getRotationOffset(), placePos.getHitResult());
        if (render.get()) bedRender = new SimpleBedRender(placePos.getPos(), bedRenderTime.get(), placePos.getRotationOffset(), sideColor.get(), lineColor.get(), damageColor.get(), bedFadeTime.get(), placePos.getDamage());
    }

    private void setPlacePos() {
        placePos = DamageCalculator.getPlacePos(target, xRadius.get(), yRadius.get(), range.get(), minTargetDamage.get(), maxSelfDamage.get(), antiSuicide.get(), strictGround.get(), blockSortMode.get());
    }

    private void doAntiTrap() {
        if (antiRequireHole.get() && !Interactions.isInHole()) return;
        if (target == null) {
            trapMine = null;
            return;
        }
        if (!CombatHelper.isTopTrapped(target)) {
            trapMine = null;
            return;
        }
        if (BlockHelper.distanceTo(target.getBlockPos().up(2)) > 4.8) return;
        if (trapMine == null) {
            trapMine = new MineInstance(target.getBlockPos().up(2));
            trapMine.init();
        } else {
            if (!trapMine.isValid()) {
                trapMine = null;
                return;
            }
            trapMine.tick();
            if (trapMine.isReady()) {
                trapMine.finish();
                trapMine = null;
            }
        }
    }

    private void doAntiBurrow() {
        if (antiRequireHole.get() && !Interactions.isInHole()) return;
        if (target == null) {
            burrowMine = null;
            return;
        }
        if (!CombatHelper.isBurrowed(target)) {
            burrowMine = null;
            return;
        }
        if (BlockHelper.distanceTo(target.getBlockPos()) > 4.8) return;
        if (burrowMine == null) {
            burrowMine = new MineInstance(target.getBlockPos());
            burrowMine.init();
        } else {
            if (!burrowMine.isValid()) {
                burrowMine = null;
                return;
            }
            burrowMine.tick();
            if (burrowMine.isReady()) {
                burrowMine.finish();
                burrowMine = null;
            }
        }
    }

    private void doAntiString() {
        if (antiRequireHole.get() && !Interactions.isInHole()) return;
        if (target == null) return;
        if (BlockHelper.distanceTo(target.getBlockPos()) > 4.8) return;
        for (BlockPos p : BlockHelper.getBlockList(target.getBlockPos(), BlockHelper.BlockListType.Web)) {
            if (BlockHelper.isWeb(p)) { // check if it's string or web
                if (BlockHelper.isCobweb(p)) { // check if we need a sword
                    FindItemResult sword = Interactions.findSword(); // mine the first web found
                    if (sword.found()) Interactions.mine(p, sword);
                    else break;
                }
                Interactions.mine(p); // mine the first string found
                break;
            }
        }
    }


    private void doBreak(BlockPos pos) {
        if (pos == null) return;
        if (debug.get()) info("doBreak()");
        PacketManager.sendInteract(Hand.OFF_HAND, new BlockHitResult(BlockHelper.bestHitPos(pos), Direction.UP, pos, false), packetBreak.get());
    }

    private void doBed(BlockPos pos, Vec3d hitPos, CardinalDirection dir, BlockHitResult hr) {
        if (pos == null || hitPos == null || dir == null || hr == null) return;

        FindItemResult bed = Interactions.findBed();
        if (!bed.found()) return; // refilling will be done at the start of the next tick
        Interactions.setSlot(bed.slot(), false);

        bedsPlaced++;

        double y = switch (dir) {
            case North -> 180;
            case East -> -90;
            case West -> 90;
            case South -> 0;
        };

        Rotations.rotate(y, Rotations.getPitch(hitPos), () -> {
            if (debug.get()) info("placing and exploding bed");
            boolean s = mc.player.isSneaking();
            if (s) mc.player.setSneaking(false);
            PacketManager.sendInteract(Hand.MAIN_HAND, hr, packetPlace.get()); // use separate hands for 0 tick explosion
            PacketManager.sendInteract(Hand.OFF_HAND, hr, packetBreak.get());
            mc.player.setSneaking(s);
            Interactions.swapBack();
        });

    }

    private boolean placeCheck(BedPlacement placement) {
        if (placement == null) return false;
        if (placement.getPos() == null) return false;
        return BlockHelper.distanceTo(placePos.getPos()) <= range.get();
    }


    private void doRefill() {
        FindItemResult hbed = Interactions.findBed();
        if (hbed.found()) return;
        FindItemResult ibed = Interactions.findBedInAll();
        if (!ibed.found()) return;
        Interactions.transfer(ibed.slot(), refillSlot.get() - 1, true);
    }

    // auto crafting stuff
    private void doAutoCraft() {
        if (!autoCraft.get()) return;
        FindItemResult bed = Interactions.findBed();
        if (bed.count() >= autoCraftStopAt.get()) { // first check if we even need to craft
            if (debug.get()) warning("beds already in inventory, skipping auto craft.");
            if (autoClose.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
            return;
        }
        FindItemResult craftingTable = Interactions.findCraftTable();
        FindItemResult wool = Interactions.findWool();
        FindItemResult planks = Interactions.findPlanks();
        if (!craftingTable.found() || !wool.found() || !planks.found()) { // check for the needed items
            if (debug.get()) warning("cant auto craft, no table wool or planks.");
            if (autoClose.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
            return;
        }
        if (wool.count() < 3 || planks.count() < 3) { // check that we have enough wool and planks for at least one bed
            if (debug.get()) warning("cant auto craft, wool or plank count too low");
            if (autoClose.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
            return;
        }
        if (Interactions.getEmptySlots() < autoCraftEmptySlots.get() || // make sure we have enough empty slots, and check requireHole and requireTotem
            autoCraftRequireHole.get() && !CombatHelper.isInHole(mc.player) ||
            autoCraftRequireTotem.get() && Interactions.getOffHandItem() != Items.TOTEM_OF_UNDYING
        ) {
            if (autoClose.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
            if (debug.get()) warning("can't auto craft , minSlots, hole, or totem check fail");
            return;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler h) { // craft a bed if we're already in the crafting screen
            if (debug.get()) info("we're in a crafting table, crafting a bed.");
            doCraft(h);
        } else { // if not prep to place and open a crafting table
            BlockHelper.BlockPosExtended table = findCraftingTable(); // see if there is a crafting table nearby first
            if (table.nullCheck()) {
                PacketManager.sendInteract(Hand.MAIN_HAND, table.getHitResult(), packetPlace.get());
            } else { // place one if there isn't
                if (tableTimer > 0) {
                    tableTimer--;
                    return;
                }
                tableTimer = autoCraftTableDelay.get();
                placeCraftingTable(craftingTable);
            }
        }
    }

    private void doCraft(ScreenHandler h) {
        if (h == null) return;
        List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
        for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
            for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                if (recipe.getOutput().getItem() instanceof BedItem) { // find bed recipe (matches to whatever wool/planks you have)
                    assert mc.interactionManager != null;
                    mc.interactionManager.clickRecipe(h.syncId, recipe, false); // click the recipe
                    Interactions.windowClick(h, 0, SlotActionType.QUICK_MOVE, 1); // pick up the result and move to our inventory
                }
            }
        }
    }

    private void placeCraftingTable(FindItemResult craftingTable) {
        for (BlockPos p : BlockHelper.getSphere(mc.player.getBlockPos(), 3, 2)) {
            BlockHelper.BlockPosExtended pos = new BlockHelper.BlockPosExtended(p);
            if (!pos.canPlace()) continue;
            if (debug.get()) info("placeCraftingTable()");
            Interactions.setSlot(craftingTable.slot(), false);
            BlockHelper.place(p, craftingTable, true, true);
            PacketManager.sendInteract(Hand.MAIN_HAND, pos.getHitResult(), true);
            break;
        }
    }

    private BlockHelper.BlockPosExtended findCraftingTable() {
        BlockHelper.BlockPosExtended table = new BlockHelper.BlockPosExtended();
        for (BlockPos p : BlockHelper.getSphere(mc.player.getBlockPos(), 3, 2)) {
            if (BlockHelper.getBlock(p) != Blocks.CRAFTING_TABLE) continue;
            table.set(p);
        }
        return table;
    }


    // render
    @EventHandler
    private void onRender3d(Render3DEvent event) { // bed rendering
        if (render.get() && bedRender != null && target != null) {
            if (bedRender.getPos() == null || bedRender.getDir() == null) return;
            BlockPos placePos = bedRender.getPos();
            CardinalDirection direction = bedRender.getDir();
            switch (renderMode.get()) {
                case Single -> event.renderer.box(bedRender.getPos(), bedRender.getSideColor(), bedRender.getLineColor(), shapeMode.get(), 0);
                case Outline -> {
                    int x = placePos.getX();
                    int y = placePos.getY();
                    int z = placePos.getZ();
                    Color s = bedRender.getSideColor();
                    Color l = bedRender.getLineColor();
                    switch (direction) {
                        case North -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, s, l, shapeMode.get(), 0);
                        case South -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, s, l, shapeMode.get(), 0);
                        case East -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, s, l, shapeMode.get(), 0);
                        case West -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, s, l, shapeMode.get(), 0);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) { // damage rendering
        if (render.get() && bedRender != null && placeCheck(placePos) && target != null) {
            if (bedRender.getPos() == null) return;
            Vector3d textVec = BlockHelper.vec3(bedRender.getPos());
            String damageText = String.valueOf(Math.round(placePos.getDamage() * 100.0) / 100.0);
            if (textVec != null) {
                if (NametagUtils.to2D(textVec, damageScale.get())) {
                    NametagUtils.begin(textVec);
                    TextRenderer.get().begin(1.0, false, true);
                    final double w = TextRenderer.get().getWidth(damageText) / 2.0;
                    TextRenderer.get().render(damageText, -w, 0.0, bedRender.getDamageColor());
                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) {
            String iString = target.getGameProfile().getName();
            if (lastCalc != null && showLastCalc.get()) iString += " [" + lastCalc + "]";
            if (EntityUtils.getTotalHealth(target) < 10.5) iString += " [0TICK]";
            iString += status;

            if (bedPerSec.get()) return iString + " BPS: [" + bedsPlaced + "]";
            else return iString;
        }
        return "Idle";
    }
}
