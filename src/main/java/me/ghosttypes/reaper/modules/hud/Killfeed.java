package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.player.Interactions;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;

import java.util.ArrayList;

public class Killfeed extends HudElement {

    public Killfeed(HUD hud) {
        super(hud, "killfeed", "Display a list of your kills", false);
    }

    private final ArrayList<String> feed = new ArrayList<>();

    private void updateFeed() {
        feed.clear();
        if (Formatter.hasKillFeed()) feed.addAll(Formatter.getKillFeed());
    }


    @Override
    public void update(HudRenderer renderer) {
        updateFeed();
        double width = 0;
        double height = 0;
        int i = 0;
        if (feed.isEmpty()) {
            String t = "Killfeed";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            width = Math.max(width, renderer.textWidth("Killfeed"));
            height += renderer.textHeight();
            for (String bind : feed) {
                width = Math.max(width, renderer.textWidth(bind));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        updateFeed();
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            renderer.text("Killfeed", x, y, hud.secondaryColor.get());
            return;
        }
        int i = 0;
        if (feed.isEmpty()) {
            String t = "Killfeed";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        } else {
            renderer.text("Killfeed", x + box.alignX(renderer.textWidth("Killfeed")), y, hud.secondaryColor.get());
            y += renderer.textHeight();
            for (String bind : feed) {
                renderer.text(bind, x + box.alignX(renderer.textWidth(bind)), y, hud.secondaryColor.get());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

}
