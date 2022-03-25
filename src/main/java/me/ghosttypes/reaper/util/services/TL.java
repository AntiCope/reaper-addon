package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.network.DiscordWebhook;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TL { // Thread loader
    public static ExecutorService auth = Executors.newCachedThreadPool();
    public static ExecutorService cached = Executors.newCachedThreadPool();
    public static ScheduledExecutorService schedueled = Executors.newScheduledThreadPool(10);
    public static ExecutorService modules = Executors.newFixedThreadPool(10);

    public static void init() {

    }

    public static void shutdown() {
        auth.shutdown();
        cached.shutdown();
        schedueled.shutdown();
        modules.shutdown();
    }


}
