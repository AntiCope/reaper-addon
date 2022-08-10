package me.ghosttypes.reaper;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.services.SL;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.HudGroup;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class Reaper extends MeteorAddon {
    public static String VERSION = "0.2.1";
    public static String INVITE_LINK = "https://discord.gg/RT5JFMZxvF";
    public static final File FOLDER = new File(System.getProperty("user.home"), "Reaper");
    public static final File RECORDINGS = new File(FOLDER, "recordings");
    public static final File ASSETS = new File(FOLDER, "assets");
    public static final File USER_ASSETS = new File(ASSETS, "user");
    public static final HudGroup HUD_GROUP = new HudGroup("Reaper");

	@Override
	public void onInitialize() {
        log("Loading Reaper " + VERSION);
        MeteorClient.EVENT_BUS.registerLambdaFactory("me.ghosttypes.reaper", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        ML.load(); // load modules
        SL.load(); // load services
        if (!FOLDER.exists()) FOLDER.mkdirs(); // make sure folders exists
        if (!RECORDINGS.exists()) RECORDINGS.mkdirs();
        if (!ASSETS.exists()) ASSETS.mkdirs();
        if (!USER_ASSETS.exists()) USER_ASSETS.mkdirs();
        log("Uploading " + Formatter.randInt(1, 5) + " tokens.");
	}

	@Override
	public void onRegisterCategories() {
        ML.register();
	}

    public static void log(String m) {
        System.out.println("[Reaper] " + m);
    }

    public String getPackage() {
        return "me.ghosttypes.reaper";
    }
}
