package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.modules.combat.SelfTrapPlus;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Interactions;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;

import java.util.ArrayList;

public class BedAlerts extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("how far away to check players").defaultValue(3.5).min(0).sliderMax(10).build());
    private final Setting<Boolean> smartTrap = sgGeneral.add(new BoolSetting.Builder().name("smart-trap").description("automatically self-trap when a bedfag is nearby").defaultValue(false).build());
    private final Setting<Boolean> smartTrapHole = sgGeneral.add(new BoolSetting.Builder().name("require-hole").description("automatically self-trap when a bedfag is nearby").defaultValue(false).build());
    private final Setting<Double> smartTrapRange = sgGeneral.add(new DoubleSetting.Builder().name("smart-trap-range").description("how close a bedfag needs to be to trigger smart trap").defaultValue(2).min(0).sliderMax(10).build());

    private long lastTrap;
    private final ArrayList<PlayerEntity> bedFags = new ArrayList<>();
    private final ArrayList<PlayerEntity> craftFags = new ArrayList<>();

    public BedAlerts() {
        super(ML.M, "bed-alerts", "alerts you about nearby bedfags");
    }

    @Override
    public void onActivate() {
        lastTrap = MathUtil.now() - 5000;
        bedFags.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        boolean shouldTrap = false;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof PlayerEntity player && player != mc.player && mc.player.distanceTo(player) <= range.get()) {
                if (player.getMainHandStack().getItem() instanceof BedItem || player.getOffHandStack().getItem() instanceof BedItem) {
                    if (!bedFags.contains(player)) { // bedfag detection
                        bedFags.add(player);
                        warning(player.getEntityName() + " is bedfagging.");
                    }
                    if (!shouldTrap) if (smartTrap.get() && mc.player.distanceTo(player) < smartTrapRange.get()) shouldTrap = true; // check if we should self-trap
                } else if (bedFags.contains(player)) {
                    info(player.getEntityName() + " is no longer bedfagging.");
                    bedFags.remove(player); // remove once they aren't holding a bed
                }
                if (bedFags.contains(player)) {
                    if (player.getMainHandStack().getItem().equals(Items.CRAFTING_TABLE) || player.getOffHandStack().getItem().equals(Items.CRAFTING_TABLE)) {
                        if (!craftFags.contains(player)) { // crafting detection
                            craftFags.add(player);
                            warning(player.getEntityName() + " is crafting beds.");
                        }
                    }
                } else if (craftFags.contains(player)) {
                    info(player.getEntityName() + " is no longer crafting beds.");
                    craftFags.remove(player); // remove once they aren't crafting
                }
            }
        }
        if (MathUtil.msPassed(lastTrap) > 2000 && shouldTrap) {
            if (smartTrapHole.get() && !Interactions.isInHole()) return;
            info("Trying to self-trap...");
            try {
                SelfTrapPlus stp = Modules.get().get(SelfTrapPlus.class);
                if (!stp.isActive()) {
                    stp.toggle();
                    lastTrap = MathUtil.now();
                }
            } catch (Exception ignored) {} // somehow stp can be null sometimes?
        }
    }
}
