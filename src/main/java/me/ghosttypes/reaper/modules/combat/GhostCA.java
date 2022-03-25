package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class GhostCA extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> legacy = sgGeneral.add(new BoolSetting.Builder().name("1.12").defaultValue(false).build());
    public final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(16).sliderRange(1, 30).build());
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(4.65).sliderRange(1, 30).build());
    public final Setting<Integer> xRadius = sgGeneral.add(new IntSetting.Builder().name("x-radius").defaultValue(5).sliderRange(1, 9).build());
    public final Setting<Integer> yRadius = sgGeneral.add(new IntSetting.Builder().name("y-radius").defaultValue(4).sliderRange(1, 5).build());
    public final Setting<Double> minTargetDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-target-damage").description("The min damage to deal to the target.").defaultValue(6.5).range(0, 36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").description("The max damage to deal to yourself.").defaultValue(3.85).range(0, 36).sliderMax(36).build());
    public final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").description("Prevent breaking beds that kill you.").defaultValue(false).build());

    private PlayerEntity target;
    private long lastCalc = MathUtil.now();

    public GhostCA() {
        super(ML.R, "ghost-ca", "test ca");
    }




}
