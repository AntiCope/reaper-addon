package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.player.Stats;
import me.ghosttypes.reaper.util.services.GlobalManager;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AutoRespawn extends ReaperModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRekit = settings.createGroup("Rekit");
    private final SettingGroup sgExcuse = settings.createGroup("AutoExcuse");
    private final SettingGroup sgHS = settings.createGroup("HighScore");

    private final Setting<Boolean> rekit = sgRekit.add(new BoolSetting.Builder().name("rekit").description("Rekit after dying on pvp servers.").defaultValue(false).build());
    private final Setting<String> kitName = sgRekit.add(new StringSetting.Builder().name("kit-name").description("The name of your kit.").defaultValue("default").build());

    private final Setting<Boolean> excuse = sgExcuse.add(new BoolSetting.Builder().name("excuse").description("Send an excuse to global chat after death.").defaultValue(false).build());
    private final Setting<Boolean> randomize = sgExcuse.add(new BoolSetting.Builder().name("randomize").description("Randomizes the excuse message.").defaultValue(false).build());
    private final Setting<List<String>> messages = sgExcuse.add(new StringListSetting.Builder().name("excuse-messages").description("Messages to use for AutoExcuse").defaultValue(Collections.emptyList()).build());

    private final Setting<Boolean> alertHS = sgHS.add(new BoolSetting.Builder().name("alert").description("Alerts you client side when you reach a new highscore.").defaultValue(false).build());
    private final Setting<Boolean> announceHS = sgHS.add(new BoolSetting.Builder().name("announce").description("Announce when you reach a new highscore.").defaultValue(false).build());



    private boolean shouldRekit = false;
    private boolean shouldExcuse = false;
    private boolean shouldHS = false;
    private int excuseWait = 50;
    private int rekitWait = 50;
    private int messageI = 0;

    public AutoRespawn() {
        super(ML.M, "auto-respawn", "Automatically respawns after death.");
    }

    @Override
    public void onActivate() {

    }

    @EventHandler
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;
        // reset time since last respawn
        GlobalManager.lastRespawnTime = System.currentTimeMillis();
        mc.player.requestRespawn();
        if (rekit.get()) shouldRekit = true;
        if (excuse.get()) shouldExcuse = true;
        Stats.deaths++;
        //clear these when we die
        if (Stats.killStreak > Stats.highscore) {
            shouldHS = true;
            Stats.highscore = Stats.killStreak;
        }
        Stats.killStreak = 0;
        Stats.killfeed.clear();
        event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        //if (Wrapper.isLagging()) return;
        if (shouldRekit && rekitWait <= 1) {
            if (shouldHS) {
                if (alertHS.get()) notify("You reached a new highscore of " + Stats.highscore + "!");
                if (announceHS.get()) MessageUtil.sendMessage("I reached a new highscore of " + Stats.highscore + " thanks to Reaper!");
                shouldHS = false;
            }
            notify("Rekitting with kit " + kitName.get());
            MessageUtil.sendMessage("/kit " + kitName.get());
            shouldRekit = false;
            shouldHS = false;
            rekitWait = 50;
            return;
        } else { rekitWait--; }
        if (shouldExcuse && excuseWait <= 1) {
            String excuseMessage = getExcuseMessage();
            MessageUtil.sendMessage(excuseMessage);
            shouldExcuse = false;
            excuseWait = 50;
        } else { excuseWait--; }
    }


    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        //NotificationManager.addNotification(title + msg);
    }

    private String getExcuseMessage() {
        String excuseMessage;
        if (messages.get().isEmpty()) {
            notify("Your excuse message list is empty!");
            return "Lag";
        } else {
            if (randomize.get()) {
                excuseMessage = messages.get().get(new Random().nextInt(messages.get().size()));
            } else {
                if (messageI >= messages.get().size()) messageI = 0;
                int i = messageI++;
                excuseMessage = messages.get().get(i);
            }
        }
        return excuseMessage;
    }
}

