package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.misc.Formatter;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class Greeting extends DoubleTextHudElement {
    public Greeting(HUD hud) {
        super(hud, "greeting", "", "");
    }

    @Override
    protected String getRight() {
        if (mc.player == null) return Formatter.getGreeting();
        return Formatter.getGreeting() + mc.player.getEntityName();
    }
}
