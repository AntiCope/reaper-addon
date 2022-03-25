package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.ModuleHelper;
import me.ghosttypes.reaper.util.network.PacketManager;
import me.ghosttypes.reaper.util.player.Interactions;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class QuickMend extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPause = settings.createGroup("Pause");

    public final Setting<Double> enableAt = sgGeneral.add(new DoubleSetting.Builder().name("threshold").description("What durability to enable at.").defaultValue(20).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder().name("min-health").description("Min health for repairing.").defaultValue(10).min(0).sliderMax(36).max(36).build());
    private final Setting<Boolean> passive = sgGeneral.add(new BoolSetting.Builder().name("passive").description("Keep AutoXP on and repair automatically.").defaultValue(false).build());
    private final Setting<Boolean> moduleControl = sgGeneral.add(new BoolSetting.Builder().name("module-control").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("require-hole").defaultValue(false).build());
    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder().name("silent").defaultValue(false).build());
    private final Setting<Boolean> refill = sgGeneral.add(new BoolSetting.Builder().name("refill").defaultValue(false).build());
    private final Setting<Integer> refillSlot = sgGeneral.add(new IntSetting.Builder().name("refill-slot").defaultValue(1).min(1).sliderMin(1).max(9).sliderMax(9).visible(refill::get).build());
    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder().name("look-down").defaultValue(true).build());

    private final Setting<Boolean> pauseOnGap = sgPause.add(new BoolSetting.Builder().name("pause-on-gap").description("Pauses while holding egaps.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());

    public QuickMend() {
        super(ML.R, "quick-mend", "Automatically repair your armor.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyInHole.get() && !Interactions.isInHole()) {
            error("You're not in a hole.");
            toggle();
            return;
        }
        if (PlayerUtils.getTotalHealth() < minHealth.get()) {
            error("Your health is too low.");
            toggle();
            return;
        }
        if (!needsRepair()) {
            error("Your armor is above the threshold.");
            toggle();
            return;
        }
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        FindItemResult hXP = Interactions.findXP();
        if (!hXP.found()) {
            if (refill.get()) {
                FindItemResult iXP = Interactions.findXPinAll();
                if (!iXP.found()) {
                    error("No XP in inventory.");
                    toggle();
                } else {
                    Interactions.transfer(iXP.slot(), refillSlot.get() - 1, true);
                }
            } else {
                error("No XP in hotbar.");
                toggle();
            }
            return;
        }
        if (moduleControl.get()) ModuleHelper.disableCombat(this);
        mend(hXP);
    }

    private void mend(FindItemResult xp) {
        if (lookDown.get()) Rotations.rotate(mc.player.getYaw(), 90, 50, () -> throwXP(xp));
        else throwXP(xp);
    }

    private void throwXP(FindItemResult xp) {
        if (!xp.found() || !xp.isHotbar()) return; // just in case lol
        if (Interactions.isHolding(Items.ENCHANTED_GOLDEN_APPLE) && pauseOnGap.get()) return;
        Interactions.setSlot(xp.slot(), false);
        PacketManager.interactItem(xp.getHand());
        if (silent.get()) Interactions.swapBack();
    }

    private boolean needsRepair() {
        for (int i = 0; i < 4; i++) if (Interactions.checkThreshold(Interactions.getArmor(i), enableAt.get())) return true;
        return false;
    }
}
