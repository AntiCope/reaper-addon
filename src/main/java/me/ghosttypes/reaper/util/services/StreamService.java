package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.misc.StreamerMode;
import me.ghosttypes.reaper.modules.render.ExternalHUD;
import me.ghosttypes.reaper.modules.render.ExternalNotifications;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class StreamService {

    public static boolean isStreaming = false;
    public static boolean toggled = false;
    public static ArrayList<HudElement> toggledElements = new ArrayList<>();
    public static ArrayList<String> elementsToToggle = new ArrayList<>();


    public static void init() {
        // HUD modules that need to be toggled
        elementsToToggle.add("Active Modules");
        elementsToToggle.add("Armor");
        elementsToToggle.add("Biome");
        elementsToToggle.add("Fps");
        elementsToToggle.add("Looking At");
        elementsToToggle.add("Ping");
        elementsToToggle.add("Coords");
        elementsToToggle.add("Speed");
        elementsToToggle.add("Player Info");
        elementsToToggle.add("Tps");
        // Check for OBS every 500ms
        TL.schedueled.scheduleAtFixedRate(StreamService::check, 1000, 500, TimeUnit.MILLISECONDS);
    }

    public static void check() {
        if (mc.world == null || mc.player == null) return;
        try {
            StreamerMode streamMode = Modules.get().get(StreamerMode.class);
            isStreaming = isObsOpen();
            if (streamMode.autoToggle.get() && isStreaming && !toggled) {
                toggled = true;
                enableStreamMode();
            }
            if (streamMode.autoToggle.get() && !isStreaming && toggled) {
                toggled = false;
                disableStreamMode();
            }
        } catch (Exception e) {
            Reaper.log("StreamService check() exception: " + e);
        }
    }


    public static void enableStreamMode() {
        ChatUtils.info("Stream Mode enabled!");
        // Disable sensitive HUD modules
        toggledElements.clear();
        HUD hud = Systems.get(HUD.class);
        for (HudElement element : hud.elements) {
            for (String str : elementsToToggle) { // comparing by name is probably meh, but I didn't see a better way
                if (element.name.equalsIgnoreCase(str) || element.title.equalsIgnoreCase(str)) {
                    if (element.active) { // don't toggle anything that isn't active
                        element.toggle();
                        toggledElements.add(element);
                        break;
                    }
                }
            }
        }
        startExternals();
    }

    public static void disableStreamMode() {
        for (HudElement element : toggledElements) if (!element.active) element.toggle(); // Re-enable disabled HUD modules
        toggledElements.clear();
        stopExternals();
    }

    public static void startExternals() {
        ExternalHUD externalHUD = Modules.get().get(ExternalHUD.class);
        ExternalNotifications externalNotifications = Modules.get().get(ExternalNotifications.class);
        if (!externalHUD.isActive()) externalHUD.toggle();
        if (!externalNotifications.isActive()) externalNotifications.toggle();
    }

    public static void stopExternals() {
        ExternalHUD externalHUD = Modules.get().get(ExternalHUD.class);
        ExternalNotifications externalNotifications = Modules.get().get(ExternalNotifications.class);
        if (externalHUD.isActive()) externalHUD.toggle();
        if (externalNotifications.isActive()) externalNotifications.toggle();
    }


    public static boolean isObsOpen() {
        AtomicBoolean isRunning = new AtomicBoolean(false);
        Stream<ProcessHandle> liveProcesses = ProcessHandle.allProcesses();
        liveProcesses.filter(ProcessHandle::isAlive).forEach(ph -> {
            String pName = ph.info().command().toString();
            if (pName.equalsIgnoreCase("obs-studio") || pName.contains("obs-studio")) isRunning.set(true);
        });
        return isRunning.get();
    }

}
