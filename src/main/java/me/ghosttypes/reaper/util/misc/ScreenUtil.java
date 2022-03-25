package me.ghosttypes.reaper.util.misc;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ScreenUtil {

    public static ScreenType getCurrentScreen() {
        if (mc.currentScreen instanceof TitleScreen) return ScreenType.TitleScreen;
        if (mc.currentScreen instanceof SelectWorldScreen) return ScreenType.SelectWorldScreen;
        if (mc.currentScreen instanceof MultiplayerScreen) return ScreenType.SelectServerScreen;
        if (mc.currentScreen instanceof WidgetScreen) return ScreenType.ClickGUIScreen;
        if (mc.currentScreen instanceof OptionsScreen || mc.currentScreen instanceof SkinOptionsScreen ||
            mc.currentScreen instanceof SoundOptionsScreen || mc.currentScreen instanceof VideoOptionsScreen ||
            mc.currentScreen instanceof ControlsOptionsScreen || mc.currentScreen instanceof LanguageOptionsScreen ||
            mc.currentScreen instanceof ChatOptionsScreen || mc.currentScreen instanceof PackScreen || mc.currentScreen instanceof AccessibilityOptionsScreen)
            return ScreenType.SettingScreen;
        return ScreenType.MainMenu;
    }


    public enum ScreenType {
        TitleScreen,
        SelectWorldScreen,
        SelectServerScreen,
        ClickGUIScreen,
        SettingScreen,
        MainMenu
    }
}
