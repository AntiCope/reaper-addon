package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.player.Interactions;
import me.ghosttypes.reaper.util.services.AuraSyncService;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;

import java.util.ArrayList;
import java.util.Comparator;

public class Stats extends HudElement {
    public Stats(HUD hud) {
        super(hud, "reaper-stats", "Displays various client info.", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCombat = settings.createGroup("Combat");

    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("rgb").defaultValue(false).build());
    public final Setting<Boolean> chromaText = sgGeneral.add(new BoolSetting.Builder().name("chroma-text").description("makes the text rgb too").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chroma::get).build());
    public final Setting<Boolean> drawBack = sgGeneral.add(new BoolSetting.Builder().name("render-background").description("render a background behind notifications").defaultValue(false).build());
    public final Setting<Boolean> drawSide = sgGeneral.add(new BoolSetting.Builder().name("render-side").description("render outlines on the sides of notifications").defaultValue(false).build());
    public final Setting<SettingColor> backColor = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(50, 50, 50)).build());
    public final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0)).build());
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("How to sort the stats list.").defaultValue(SortMode.Shortest).build());
    private final Setting<Boolean> fps = sgGeneral.add(new BoolSetting.Builder().name("fps").defaultValue(false).build());
    private final Setting<Boolean> tps = sgGeneral.add(new BoolSetting.Builder().name("tps").defaultValue(false).build());
    private final Setting<Boolean> ping = sgGeneral.add(new BoolSetting.Builder().name("ping").defaultValue(false).build());
    private final Setting<Boolean> playtime = sgGeneral.add(new BoolSetting.Builder().name("playtime").defaultValue(false).build());

    private final Setting<Boolean> deaths = sgCombat.add(new BoolSetting.Builder().name("deaths").description("Display your total deaths.").defaultValue(false).build());
    private final Setting<Boolean> highscore = sgCombat.add(new BoolSetting.Builder().name("highscore").description("Display your highest killstreak.").defaultValue(false).build());
    private final Setting<Boolean> kd = sgCombat.add(new BoolSetting.Builder().name("kd-ratio").description("Display your kills to death ratio.").defaultValue(false).build());
    private final Setting<Boolean> kills = sgCombat.add(new BoolSetting.Builder().name("kills").description("Display your total kills.").defaultValue(false).build());
    private final Setting<Boolean> killstreak = sgCombat.add(new BoolSetting.Builder().name("killstread").description("Display your current killstreak.").defaultValue(false).build());

    private static final RainbowColor RAINBOW = new RainbowColor();

    @Override
    public void update(HudRenderer renderer) {
        double width = 0;
        double height = 0;
        int i = 0;

        if (!Utils.canUpdate()) return;

        ArrayList<String> stats = getStats();
        if (stats.isEmpty()) {
            width = Math.max(width, renderer.textWidth("Stats"));
            height += renderer.textHeight();
        } else {
            for (String s : getStats()) {
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
        ArrayList<String> stats = getStats();
        double x = box.getX();
        double y = box.getY();
        double yBack = y; // offsets for the background and side shit
        double sideHoffset = 0;
        double backHoffset = 0;
        if (isInEditor()) {
            renderer.text("Stats", x, y, hud.secondaryColor.get());
            return;
        }
        int i = 0;
        if (stats.isEmpty()) {
            String t = "Stats";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        } else {
            // todo fix - still need work i give up for now
            RAINBOW.setSpeed(chromaSpeed.get() / 100);
            Color next = RAINBOW.getNext(renderer.delta); // store so the sides and back are synced
            if (AuraSyncService.isEnabled()) next = AuraSyncService.RGB_COLOR;
            Color sideC = sideColor.get();
            Color textColor = hud.secondaryColor.get();
            if (chroma.get()) sideC = next;
            if (chromaText.get()) textColor = next;
            for (String s : stats) {
                if (i == 1) backHoffset++; // static extra offset after the first element (copes otherwise beyond me)
                if (i >= stats.size()) { // extra offsets for the last element
                    sideHoffset += 2.5;
                    backHoffset += 2.5;
                    yBack += 0.25;
                }
                Renderer2D.COLOR.begin();
                if (drawSide.get()) Renderer2D.COLOR.quad(x + box.alignX(renderer.textWidth(s)) - 6, yBack - 4, TextRenderer.get().getWidth(s) + 10, renderer.textHeight() + sideHoffset, sideC);
                if (drawBack.get()) Renderer2D.COLOR.quad(x + box.alignX(renderer.textWidth(s)) - 2, yBack - 4, TextRenderer.get().getWidth(s) + 2, renderer.textHeight() + backHoffset, backColor.get());
                Renderer2D.COLOR.render(null);
                renderer.text(s, x + box.alignX(renderer.textWidth(s)), y, textColor);
                y += renderer.textHeight(); // standard offsets for the text height
                yBack += renderer.textHeight();
                if (i > 0) { // extra offsets after rendering the first element
                    y += 1.5;
                    yBack += 0.55;
                }
                i++;
            }
        }
    }

    private ArrayList<String> getStats() {
        ArrayList<String> stats = new ArrayList<>();
        // general
        if (fps.get()) stats.add("FPS: " + MinecraftClientAccessor.getFps());
        if (tps.get()) stats.add("TPS: " + String.format("%.1f", TickRate.INSTANCE.getTickRate()));
        if (ping.get()) stats.add("Ping: " + Interactions.getCurrentPing());
        if (playtime.get()) stats.add("Playtime: " + me.ghosttypes.reaper.util.player.Stats.getPlayTime());

        // combat
        if (deaths.get()) stats.add("Deaths: " + Interactions.getDeaths());
        if (highscore.get()) stats.add("Highscore: " + Interactions.getHighscore());
        if (kd.get()) stats.add("KD: " + Interactions.getKD());
        if (kills.get()) stats.add("Kills: " + Interactions.getKills());
        if (killstreak.get()) stats.add("Killstreak: " + Interactions.getKillstreak());

        switch (sortMode.get()) { // sorting
            case Shortest -> stats.sort(Comparator.comparing(String::length));
            case Longest -> stats.sort(Comparator.comparing(String::length).reversed());
        }

        return stats;
    }

    public enum SortMode {
        Longest,
        Shortest
    }
}
