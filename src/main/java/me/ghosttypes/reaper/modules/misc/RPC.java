package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.ScreenUtil;
import me.ghosttypes.reaper.util.services.SpotifyService;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RPC extends ReaperModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Logo {Default, Legacy, Letter, Red, Orange, Yellow, Green, Blue, Purple, Pink, White}

    public final Setting<Logo> logo = sgGeneral.add(new EnumSetting.Builder<Logo>().name("logo").description("Which RPC logo to use.").defaultValue(Logo.Default).onChanged(logo1 -> setImage(getImgKey())).build());
    private final Setting<Boolean> rgbLogo = sgGeneral.add(new BoolSetting.Builder().name("rgb-logo").description("Flex").defaultValue(false).build());
    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder().name("line-1").description("Messages for the first RPC line.").defaultValue(Collections.emptyList()).build());
    private final Setting<List<String>> messages2 = sgGeneral.add(new StringListSetting.Builder().name("line-2").description("Messages for the second RPC line.").defaultValue(Collections.emptyList()).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("How many seconds before switching to a new RPC message.").defaultValue(5).min(0).sliderMax(30).build());
    private final Setting<Integer> rgbDelay = sgGeneral.add(new IntSetting.Builder().name("rgb-delay").description("Delay between changing logo color.").defaultValue(6).min(0).sliderMax(30).visible(rgbLogo::get).build());
    private final Setting<Boolean> showSpotify = sgGeneral.add(new BoolSetting.Builder().name("show-spotify").description("Periodically show what you're listening to on Spotify").defaultValue(false).build());


    private int messageI = 0;
    private int messageI2 = 0;
    private int updateTimer = 0;
    private int rgbTimer = 0;
    private int rgb_i = 0;
    private static final RichPresence rpc = new RichPresence();
    private final ArrayList<String> rgb_names = new ArrayList<>(List.of("rpc_red", "rpc_orange", "rpc_yellow", "rpc_green", "rpc_blue", "rpc_purple", "rpc_pink"));


    public RPC() {
        super(ML.M, "discord-presence-reaper", "Discord RPC for Reaper");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        checkMeteorRPC();
        DiscordIPC.start(919264957172973570L, null);
        rpc.setStart(System.currentTimeMillis() / 1000L);
        rgbTimer = MathUtil.intToTicks(rgbDelay.get());
        if (rgbLogo.get()) {
            rpc.setLargeImage("rpc_red", "Reaper " + Reaper.VERSION);
            rgb_i++;
        } else {
            rpc.setLargeImage(getImgKey(), "Reaper " + Reaper.VERSION);
        }
    }

    public void checkMeteorRPC() {
        DiscordPresence dp = Modules.get().get(DiscordPresence.class);
        if (dp == null) return;
        if (dp.isActive()) dp.toggle();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    public void resetTimestamp() {
        rpc.setStart(System.currentTimeMillis() / 1000L);
    }

    private void setImage(String imgKey) {
        rpc.setLargeImage(imgKey, "Reaper " + Reaper.VERSION);
        DiscordIPC.setActivity(rpc);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        updateTimer--;

        if (rgbLogo.get()) {
            rgbTimer--;
            if (rgbTimer <= 0) {
                rgbTimer = MathUtil.intToTicks(rgbDelay.get());
                if (rgb_i >= rgb_names.size()) rgb_i = 0;
                setImage(rgb_names.get(rgb_i));
                rgb_i++;
            }
        }

        if (updateTimer <= 0) updateTimer = MathUtil.intToTicks(delay.get());
        else return;

        List<String> mainText = messages.get();
        List<String> subText = messages2.get();
        if (mainText.isEmpty()) mainText.add("{username}");
        if (subText.isEmpty()) subText.add("Minecraft ");

        String mainT, subT;
        if (Utils.canUpdate()) {
            if (showSpotify.get() && SpotifyService.hasMedia() && Formatter.random(1, 3) == 3) {
                mainT = Formatter.getCurrentTrack();
                subT = Formatter.getCurrentArtist();
            } else {
                if (messageI >= mainText.size()) messageI = 0;
                if (messageI2 >= subText.size()) messageI2 = 0;
                int i = messageI++;
                int i2 = messageI2++;
                mainT = Formatter.applyPlaceholders(mainText.get(i));
                subT = Formatter.applyPlaceholders(subText.get(i2));
            }
        } else {
            mainT = "Reaper " + Reaper.VERSION;
            subT = getScreenName();
        }

        rpc.setDetails(mainT);
        rpc.setState(subT);
        DiscordIPC.setActivity(rpc);
    }

    private String getImgKey() {
        String imgKey = "reaper_letter";
        switch (logo.get()) {
            case Default -> imgKey = "reaper_purple";
            case Legacy -> imgKey = "reaper_logo";
            case Letter -> imgKey = "reaper_letter";
            case Red -> imgKey = "rpc_red";
            case Orange -> imgKey = "rpc_orange";
            case Yellow -> imgKey = "rpc_yellow";
            case Green -> imgKey = "rpc_green";
            case Blue -> imgKey = "rpc_blue";
            case Purple -> imgKey = "rpc_purple";
            case Pink -> imgKey = "rpc_pink";
            case White -> imgKey = "rpc_white";
        }
        return imgKey;
    }

    private String getScreenName() {
        ScreenUtil.ScreenType st = ScreenUtil.getCurrentScreen();
        if (st == ScreenUtil.ScreenType.TitleScreen) return "Browsing the Title Screen";
        if (st == ScreenUtil.ScreenType.SelectWorldScreen) return "Selecting a Singleplayer World";
        if (st == ScreenUtil.ScreenType.SelectServerScreen) return "Selecting a Server";
        if (st == ScreenUtil.ScreenType.ClickGUIScreen) return "Browsing Reaper GUI";
        if (st == ScreenUtil.ScreenType.SettingScreen) return "Changing Settings";
        if (st == ScreenUtil.ScreenType.MainMenu) return "Browsing the Main Menu";
        return "Idle";
    }
}
