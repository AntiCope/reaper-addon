package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.modules.hud.AuraSync;
import me.ghosttypes.reaper.modules.misc.RPC;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.network.DiscordWebhook;
import me.ghosttypes.reaper.util.os.OSUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SL { // Service loader


    public static void load() {
        long start = MathUtil.now();
        OSUtil.init(); // setup current os for stuff like spotify
        ResourceLoaderService.init(); // download assets
        AuraSyncService.init();
        MeteorClient.EVENT_BUS.subscribe(GlobalManager.class);
        //GlobalManager.init();
        MessageUtil.init();
        NotificationManager.init();
        SpotifyService.init();
        StreamService.init();
        WellbeingService.init();
        Runtime.getRuntime().addShutdownHook(new Thread(TL::shutdown));
        Reaper.log("Started services (" + MathUtil.msPassed(start) + "ms)");

    }

    public static void setupRPC() {
        TL.cached.execute(() -> {
            try { Thread.sleep(5000); } catch (Exception ignored) {}
            RPC rpc = Modules.get().get(RPC.class);
            if (rpc == null) return;
            if (!rpc.runInMainMenu) rpc.runInMainMenu = true;
            rpc.checkMeteorRPC();
            if (!rpc.isActive()) rpc.toggle();
        });
    }



}
