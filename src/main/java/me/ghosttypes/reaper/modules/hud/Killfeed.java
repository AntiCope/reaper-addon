package me.ghosttypes.reaper.modules.hud;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.misc.Formatter;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;

import java.util.ArrayList;

public class Killfeed extends HudElement {

    public static final HudElementInfo<Killfeed> INFO = new HudElementInfo<>(Reaper.HUD_GROUP, "killfeed", "Display a list of your kills", Killfeed::new);

    private final ArrayList<String> feed = new ArrayList<>();

    private void updateFeed() {
        feed.clear();
        if (Formatter.hasKillFeed()) feed.addAll(Formatter.getKillFeed());
    }

    public Killfeed() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
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
        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        Hud hud = Hud.get();
        var color = hud.textColors.get().get(0);
        updateFeed();
        double x = getX();
        double y = getY();
        if (isInEditor()) {
            renderer.text("Killfeed", x, y, color, false);
            return;
        }
        int i = 0;
        if (feed.isEmpty()) {
            String t = "Killfeed";
            renderer.text(t, x + alignX(renderer.textWidth(t), Alignment.Auto), y, color, false);
        } else {
            renderer.text("Killfeed", x + alignX(renderer.textWidth("Killfeed"), Alignment.Auto), y, color, false);
            y += renderer.textHeight();
            for (String bind : feed) {
                renderer.text(bind, x + alignX(renderer.textWidth(bind), Alignment.Auto), y, color, false);
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

}
