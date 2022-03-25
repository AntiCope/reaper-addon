package me.ghosttypes.reaper.modules.render;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.render.ExternalRenderers;
import me.ghosttypes.reaper.util.services.NotificationManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.orbit.EventHandler;

import java.awt.*;
import java.util.ArrayList;

public class ExternalNotifications extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("chroma-speed").defaultValue(0.01).min(0.000).sliderMax(1).build());
    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder().name("width").defaultValue(25).min(10).sliderMax(50).build());
    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder().name("height").defaultValue(30).min(20).sliderMax(50).build());

    private ExternalRenderers.ExternalFrame externalFrame;
    private RainbowColor rc = new RainbowColor();

    public ExternalNotifications() {
        super(ML.W, "external-notifications", "render notifications outside the client");
    }

    @Override
    public void onActivate() {
        ExternalRenderers.activeFrames++;
        EventQueue.invokeLater(() -> {
            if (externalFrame == null) externalFrame = new ExternalRenderers.ExternalFrame(width.get(), height.get(), "Reaper Notifications", this);
            externalFrame.setVisible(true);
        });
        rc.setSpeed(chromaSpeed.get());
    }

    @Override
    public void onDeactivate() {
        ExternalRenderers.activeFrames--;
        if (externalFrame != null) externalFrame.setVisible(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (chroma.get() && externalFrame != null) {
            rc = rc.getNext();
            externalFrame.setTextColor(new Color(rc.r, rc.g, rc.b, rc.a));
        }
        setData();
    }

    private void setData() {
        ArrayList<String> data = new ArrayList<>();
        for (NotificationManager.Notification n : NotificationManager.getNotifications()) data.add(n.text);
        externalFrame.setText(data);
    }
}
