package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.modules.hud.AuraSync;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;

public class AuraSyncService {

    public static HudElement auraSync = null;
    public static final RainbowColor RAINBOW = new RainbowColor();
    public static Color RGB_COLOR = RAINBOW.getNext();

    public static void init() {
        HUD hud = Systems.get(HUD.class);
        for (HudElement element : hud.elements) {
            if (element.active && element.name.equalsIgnoreCase("aura-sync")) {
                auraSync = element;
                break;
            }
        }
    }


    public static boolean isEnabled() {
        if (auraSync == null) { // just in case
            init();
            return false;
        }
        return auraSync.active;
    }

    public static Color getNext() {return RAINBOW.getNext();}
    public static Color getNext(double delta) {return RAINBOW.getNext(delta);}
    public static void setSpeed(double speed) {RAINBOW.setSpeed(speed / 100);}
}
