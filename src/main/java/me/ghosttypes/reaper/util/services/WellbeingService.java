package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.misc.Formatter;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WellbeingService {

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public static void init() {
        executor.scheduleAtFixedRate(WellbeingService::alert, 0, 30, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static void alert() {
        String msg = "Take a break and get some water!";
        int selector = Formatter.random(1, 9);
        if (selector == 1) msg = "Don't play for too long!";
        if (selector == 2) msg = "Take a break and get some water!";
        if (selector == 3) msg = "Get up and stretch!";
        if (selector == 4) msg = "Don't spend all your time on block game!";
        if (selector == 5) msg = "Hope you're having a good time!";
        if (selector == 6) msg = "Tell people about us! " + Reaper.INVITE_LINK;
        if (selector == 7) msg = "Remember to hydrate!";
        if (selector == 8) msg = "looking cute ;)";
        if (selector == 9) msg = "Leave us a review!";
        ChatUtils.info(msg);
    }

}
