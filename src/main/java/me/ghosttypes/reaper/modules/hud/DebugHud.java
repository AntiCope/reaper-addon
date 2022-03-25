package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

import java.util.ArrayList;
import java.util.Comparator;

public class DebugHud extends HudElement {

    public DebugHud(HUD hud) {
        super(hud, "debug-hud", "reaper debug hud.", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> solidFoot = sgGeneral.add(new BoolSetting.Builder().name("solid-feet").defaultValue(false).build());
    private final Setting<Boolean> solidAboveFoot = sgGeneral.add(new BoolSetting.Builder().name("solid-above-feet").defaultValue(false).build());
    private final Setting<Boolean> inSwimmingPose = sgGeneral.add(new BoolSetting.Builder().name("in-swimming-pose").defaultValue(false).build());
    private final Setting<Boolean> health = sgGeneral.add(new BoolSetting.Builder().name("health").defaultValue(false).build());

    @Override
    public void update(HudRenderer renderer) {
        double width = 0;
        double height = 0;
        int i = 0;

        if (!Utils.canUpdate()) return;

        ArrayList<String> stats = getDebugInfo();
        if (stats.isEmpty()) {
            width = Math.max(width, renderer.textWidth("debug-hud"));
            height += renderer.textHeight();
        } else {
            for (String s : getDebugInfo()) {
                width = Math.max(width, renderer.textWidth(s));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }

        box.setSize(width, height);
    }


    @Override
    public void render(HudRenderer renderer) {
        ArrayList<String> stats = getDebugInfo();
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            renderer.text("Stats", x, y, hud.secondaryColor.get());
            return;
        }
        int i = 0;
        if (stats.isEmpty()) {
            String t = "Stats";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        } else {
            for (String s : stats) {
                renderer.text(s, x + box.alignX(renderer.textWidth(s)), y, hud.secondaryColor.get());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    private ArrayList<String> getDebugInfo() {
        ArrayList<String> stats = new ArrayList<>();
        if (solidFoot.get()) {
            if (BlockHelper.isAir(mc.player.getBlockPos())) {
                stats.add("Solid Feet: False");
            } else {
                stats.add("Solid Feet: True");
            }
        }
        if (solidAboveFoot.get()) {
            if (BlockHelper.isAir(mc.player.getBlockPos().up())) {
                stats.add("Solid Above Feet: False");
            } else {
                stats.add("Solid Above Feet: True");
            }
        }
        if (inSwimmingPose.get()) {
            if (mc.player.isInSwimmingPose()) {
                stats.add("In Swimming Pose: True");
            } else {
                stats.add("In Swimming Pose: False");
            }
        }
        if (health.get()) stats.add("Health: " + PlayerUtils.getTotalHealth());


        stats.sort(Comparator.comparing(String::length));
        return stats;
    }

}
