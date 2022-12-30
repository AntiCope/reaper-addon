package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.misc.RPC;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.os.OSUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SL { // Service loader


    public static void load() {
        long start = MathUtil.now();
        OSUtil.init(); // setup current os for stuff like spotify
        ResourceLoaderService.init(); // download assets
        MeteorClient.EVENT_BUS.subscribe(GlobalManager.class);
        //GlobalManager.init();
        MessageUtil.init();
        NotificationManager.init();
        //SpotifyService.init();
        //WellbeingService.init(); useless
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
