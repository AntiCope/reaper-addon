package me.ghosttypes.reaper.modules.hud;


import me.ghosttypes.reaper.util.services.AuraSyncService;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static me.ghosttypes.reaper.util.services.ResourceLoaderService.*;

public class Watermark extends HudElement {

    public enum LogoDesign {Default, Beams, Colorsplash, Galaxy, PurpleGalaxy, RedGalaxy}

    private static final RainbowColor RAINBOW = new RainbowColor();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<LogoDesign> logo = sgGeneral.add(new EnumSetting.Builder<LogoDesign>().name("logo").description("Which logo to use.").defaultValue(LogoDesign.Default).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").defaultValue(2).min(1).sliderMin(1).sliderMax(5).build());
    private final Setting<Double> boxW = sgGeneral.add(new DoubleSetting.Builder().name("box-width").defaultValue(100).min(1).sliderMin(1).sliderMax(600).build());
    private final Setting<Double> boxH = sgGeneral.add(new DoubleSetting.Builder().name("box-height").description("The scale.").defaultValue(100).min(1).sliderMin(1).sliderMax(600).build());
    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("Chroma logo animation.").defaultValue(false).visible(() -> logo.get() == LogoDesign.Default).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chroma::get).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(255, 255, 255)).visible(() -> !chroma.get()).build());

    public Watermark(HUD hud) {
        super(hud, "reaper-logo", "Displays the Reaper logo.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(boxW.get() * scale.get(), boxH.get() * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;
        double x = box.getX();
        double y = box.getY();
        int w = (int) box.width;
        int h = (int) box.height;

        switch (logo.get()) {
            case Default -> GL.bindTexture(LOGO);
            case Beams -> GL.bindTexture(LOGO_BEAMS);
            case Colorsplash -> GL.bindTexture(LOGO_COLORSPLASH);
            case Galaxy -> GL.bindTexture(LOGO_GALAXY);
            case PurpleGalaxy -> GL.bindTexture(LOGO_PURPLE);
            case RedGalaxy -> GL.bindTexture(LOGO_RED);
        }
        Renderer2D.TEXTURE.begin();
        if (chroma.get() && logo.get() == LogoDesign.Default) {
            RAINBOW.setSpeed(chromaSpeed.get() / 100);
            if (AuraSyncService.isEnabled()) Renderer2D.TEXTURE.texQuad(x, y, w, h, AuraSyncService.RGB_COLOR);
            else Renderer2D.TEXTURE.texQuad(x, y, w, h, RAINBOW.getNext(renderer.delta));
        } else {
            Renderer2D.TEXTURE.texQuad(x, y, w, h, color.get());
        }
        Renderer2D.TEXTURE.render(null);
    }

}
