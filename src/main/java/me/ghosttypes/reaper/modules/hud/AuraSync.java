package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.services.AuraSyncService;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;

public class AuraSync extends HudElement {

    public AuraSync(HUD hud) {
        super(hud, "aura-sync", "Sync all rgb elements in reaper together", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> syncHUDtext = sgGeneral.add(new BoolSetting.Builder().name("hud-text").description("enable the hud module and this setting to enable aura sync").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).build());

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(Math.max(0, renderer.textWidth("AuraSync")), renderer.textHeight());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            // copius way to do this, but it works better to have this module as a 'hud element'
            if (this.active) renderer.text("AuraSync - ON", x, y, hud.secondaryColor.get());
            else renderer.text("AuraSync - OFF", x, y, hud.secondaryColor.get());
            return;
        }
        AuraSyncService.setSpeed(chromaSpeed.get());
        AuraSyncService.RGB_COLOR = AuraSyncService.getNext(renderer.delta);
    }
}
