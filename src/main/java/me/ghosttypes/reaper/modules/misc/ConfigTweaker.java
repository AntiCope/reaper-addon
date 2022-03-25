package me.ghosttypes.reaper.modules.misc;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ConfigTweaker {
    public static ConfigTweaker INSTANCE;

    public ConfigTweaker() {
        INSTANCE = this;
    }

    public final SettingGroup sgReaper = Config.get().settings.createGroup("Reaper");

}
