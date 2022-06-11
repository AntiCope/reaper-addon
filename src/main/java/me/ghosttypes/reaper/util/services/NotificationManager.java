package me.ghosttypes.reaper.util.services;

import com.mojang.text2speech.Narrator;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.modules.chat.NotificationSettings;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NotificationManager {

    public static ArrayList<Notification> notifications = new ArrayList<>();
    public static ArrayList<Notification> threadSafeNotifs = new ArrayList<>();

    public static void init() {
        TL.schedueled.scheduleAtFixedRate(NotificationManager::update, 0, 1, TimeUnit.SECONDS);

    }

    public static void update() {
        notifications.forEach(Notification::update);
        notifications.removeIf(Notification::shouldRemove);
    }

    // Adding and getting notifications
    public static void addDirectly(Notification notif) {
        notifications.add(notif);
    }

    public static void addNotification(String message) {
        for (Notification n : threadSafeNotifs) if (n.text.contains(message) || n.text.equalsIgnoreCase(message)) return; // no duplicates
        notifications.add(new Notification(message, Modules.get().get(NotificationSettings.class).displayTime.get())); }

    public static ArrayList<Notification> getNotifications() {
        threadSafeNotifs.clear();
        threadSafeNotifs.addAll(notifications);
        return threadSafeNotifs;
    }

    public static class Notification {
        public final String text; // notification body
        public int renderTime; // how many seconds the notification lasts for

        public Notification(String notificationText, int notificationTime) {
            text = notificationText;
            renderTime = notificationTime;
        }

        public void update() {
            renderTime--;
        }
        public boolean shouldRemove() {
            return renderTime <= 0;
        }
    }



}
