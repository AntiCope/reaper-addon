package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import java.util.ArrayList;

public class AutoLogin extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder().name("password").description("The password to log in with.").defaultValue("password").build());

    private final ArrayList<String> loginMessages = new ArrayList<>() {{
        add("/login ");
        add("/login <password>");
    }};

    public AutoLogin() {
        super(ML.M, "auto-login", "Automatically log into servers that use /login.");
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (mc.world == null || mc.player == null) return;
        String msg = event.getMessage().getString();
        if (msg.startsWith(">")) return; //ignore chat messages
        for (String loginMsg: loginMessages) {
            if (msg.contains(loginMsg)) {
                MessageUtil.sendClientMessage("/login " + password.get());
                break;
            }
        }
    }

}
