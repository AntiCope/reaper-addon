package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.services.SpotifyService;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class SpotifyHud extends HudElement {
    public static final HudElementInfo<SpotifyHud> INFO = new HudElementInfo<>(Reaper.HUD_GROUP, "spotify-hud", "Display the current song playing in spotify.", SpotifyHud::new);

    public SpotifyHud() {
        super(INFO);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> format = sgGeneral.add(new StringSetting.Builder().name("format").description("The format for the hud").defaultValue("Playing {track} - {artist}").build());
    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("rgb").defaultValue(false).build());
    public final Setting<Boolean> chromaText = sgGeneral.add(new BoolSetting.Builder().name("chroma-text").description("makes the text rgb too").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chroma::get).build());
    public final Setting<Boolean> drawBack = sgGeneral.add(new BoolSetting.Builder().name("render-background").description("render a background behind notifications").defaultValue(false).build());
    public final Setting<Boolean> drawSide = sgGeneral.add(new BoolSetting.Builder().name("render-side").description("render outlines on the sides of notifications").defaultValue(false).build());
    public final Setting<SettingColor> backColor = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(50, 50, 50)).build());
    public final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0)).build());

    private static final RainbowColor RAINBOW = new RainbowColor();

    @Override
    public void tick(HudRenderer renderer) {
        double width = 0;
        double height = 0;

        String t;

        if (!SpotifyService.isSpotifyRunning) {
            t = "Spotify is not running";
        } else if (SpotifyService.currentTrack == null || SpotifyService.currentArtist == null) {
            t = "Idle";
        } else {
            t = "Playing " + SpotifyService.currentTrack + " - " + SpotifyService.currentArtist;
        }

        width = Math.max(width, renderer.textWidth(t));
        height += renderer.textHeight();
        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        Hud hud = Hud.get();
        var color = hud.textColors.get().get(0);

        double x = getX();
        double y = getY();

        if (isInEditor()) {
            renderer.text("Spotify HUD", x, y, color, false);
            return;
        }

        RAINBOW.setSpeed(chromaSpeed.get() / 100);
        Color next = RAINBOW.getNext(renderer.delta); // store so the sides and back are synced
        Color sideC = sideColor.get();
        Color textColor = color;
        if (chroma.get()) sideC = next;
        if (chromaText.get()) textColor = next;

        String t;
        if (SpotifyService.hasMedia()) t = format.get().replace("{track}", SpotifyService.currentTrack).replace("{artist}", SpotifyService.currentArtist);
        else t = "Idle.";

        Renderer2D.COLOR.begin();
        if (drawSide.get()) Renderer2D.COLOR.quad(x + alignX(renderer.textWidth(t), Alignment.Auto) - 6, y - 4, TextRenderer.get().getWidth(t) + 10, renderer.textHeight(), sideC);
        if (drawBack.get()) Renderer2D.COLOR.quad(x + alignX(renderer.textWidth(t), Alignment.Auto) - 2, y - 4, TextRenderer.get().getWidth(t) + 2, renderer.textHeight(), backColor.get());
        Renderer2D.COLOR.render(null);
        renderer.text(t, x + alignX(renderer.textWidth(t), Alignment.Auto), y - 2, textColor, false);
    }
}
