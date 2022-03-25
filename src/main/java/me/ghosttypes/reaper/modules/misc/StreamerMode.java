package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.services.StreamService;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class StreamerMode extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder().name("automatic").defaultValue(false).build());

    public StreamerMode() {
        super(ML.M, "streamer-mode", "Move sensitive information to another screen for streaming");
    }

    @Override
    public void onActivate() {
        StreamService.enableStreamMode();
    }

    @Override
    public void onDeactivate() {
        StreamService.disableStreamMode();
    }

}
