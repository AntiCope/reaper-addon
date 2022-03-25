package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.services.AuraSyncService;
import me.ghosttypes.reaper.util.services.NotificationManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;

public class Notifications extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("rgb notifications :)").defaultValue(false).build());
    public final Setting<Boolean> chromaText = sgGeneral.add(new BoolSetting.Builder().name("chroma-text").description("makes the text rgb too").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chroma::get).build());
    public final Setting<Boolean> drawBack = sgGeneral.add(new BoolSetting.Builder().name("render-background").description("render a background behind notifications").defaultValue(false).build());
    public final Setting<Boolean> drawSide = sgGeneral.add(new BoolSetting.Builder().name("render-side").description("render outlines on the sides of notifications").defaultValue(false).build());
    public final Setting<SettingColor> backColor = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(50, 50, 50)).build());
    public final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0)).build());

    private static final RainbowColor RAINBOW = new RainbowColor();

    public Notifications(HUD hud) {
        super(hud, "notifications", "Display notifications", false);
    }

    public static ArrayList<String> getNotifications() {
        ArrayList<String> notifs = new ArrayList<>();
        NotificationManager.getNotifications().forEach(notification -> notifs.add(notification.text));
        return notifs;
    }

    @Override
    public void update(HudRenderer renderer) {
        double width = 0;
        double height = 0;
        int i = 0;

        if (NotificationManager.getNotifications().isEmpty()) {
            String t = "Notifications";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
            box.setSize(width, height);
            return;
        } else {
            ArrayList<String> notifs = new ArrayList<>();
            NotificationManager.getNotifications().forEach(notification -> notifs.add(notification.text));
            for (String n : notifs) {
                width = Math.max(width, renderer.textWidth(n));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        int i = 0;
        if (isInEditor()) {
            renderer.text("Notifications", x, y, hud.secondaryColor.get());
            return;
        }

        if (NotificationManager.getNotifications().isEmpty()) return;
        ArrayList<String> notifs = new ArrayList<>();
        NotificationManager.getNotifications().forEach(notification -> notifs.add(notification.text));


        RAINBOW.setSpeed(chromaSpeed.get() / 100);
        Color next = RAINBOW.getNext(renderer.delta); // store so the sides and back are synced
        if (AuraSyncService.isEnabled()) next = AuraSyncService.RGB_COLOR;
        Color sideC = sideColor.get();
        Color textColor = hud.secondaryColor.get();
        if (chroma.get()) sideC = next;
        if (chromaText.get()) textColor = next;

        for (String n : notifs) {
            Renderer2D.COLOR.begin();
            if (drawSide.get()) Renderer2D.COLOR.quad(x + box.alignX(renderer.textWidth(n)) - 6, y - 4, TextRenderer.get().getWidth(n) + 10, renderer.textHeight(), sideC);
            if (drawBack.get()) Renderer2D.COLOR.quad(x + box.alignX(renderer.textWidth(n)) - 2, y - 4, TextRenderer.get().getWidth(n) + 2, renderer.textHeight(), backColor.get());
            Renderer2D.COLOR.render(null);
            renderer.text(n, x + box.alignX(renderer.textWidth(n)), y, textColor);
            y += renderer.textHeight();
            if (i > 0) y += 2;
            i++;
        }
    }


    public static void spotify(String artist, String track) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(Items.NOTE_BLOCK, artist, track, 2000));
    }

    public static void lowArmor(Item armorPiece, String text) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(armorPiece, "Armor Alert", text, 2000));
    }

    public static void popAlert(String p) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(Items.TOTEM_OF_UNDYING, "PopCounter", p, 1000));
    }
}
