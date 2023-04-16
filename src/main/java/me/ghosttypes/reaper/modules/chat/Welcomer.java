package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Welcomer extends ReaperModule {


    private final Executor messageSender = Executors.newSingleThreadExecutor();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between sending messages.").defaultValue(1).min(1).sliderMax(10).max(10).build());
    private final Setting<List<String>> joinMessages = sgGeneral.add(new StringListSetting.Builder().name("join-messages").description("Messages to send when a player joins.").defaultValue(Collections.emptyList()).build());
    private final Setting<List<String>> leaveMessages = sgGeneral.add(new StringListSetting.Builder().name("leave-messages").description("Messages to send when a player leaves.").defaultValue(Collections.emptyList()).build());
    public final Setting<Boolean> clientside = sgGeneral.add(new BoolSetting.Builder().name("clientside").description("Whether or not the messages are sent clientside.").defaultValue(false).build());


    public Welcomer() {
        super(ML.M, "welcomer", "Sends a message when somebody joins or leaves the server.");
    }


    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        Packet<?> eventPacket = event.packet;
        assert mc.player != null;
        if (mc.player.age < 30) return;
        if ((eventPacket instanceof GameMessageS2CPacket packet)) {
            String msg = packet.content().getString();
            if (msg.contains("left")) {
                boolean valid = false;
                String name = msg.substring(0, msg.indexOf(" "));
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player.getEntityName().equals(name) || player.getDisplayName().getString().contains(name)) {
                        valid = true;
                        break;
                    }
                }
                if (valid) sendLeaveMsg(name);
            }
        }

        if (eventPacket instanceof PlayerListS2CPacket packet) {
            for (PlayerListS2CPacket.Action action : packet.getActions()) {
                if (action == PlayerListS2CPacket.Action.ADD_PLAYER) {
                    PlayerListS2CPacket.Entry entry = packet.getEntries().get(0);
                    if (entry != null) {
                        String name = entry.profile().getName();
                        if (name != null) sendJoinMsg(name);
                    }
                }
            }
        }
    }

    private void sendJoinMsg(String name) {
        if (joinMessages.get().isEmpty()) return;
        String msg = joinMessages.get().get(new Random().nextInt(joinMessages.get().size()));
        msg = msg.replace("{player}", name);
        String finalMsg = msg;
        if (!clientside.get()) messageSender.execute(() -> queueMsg(finalMsg));
        else info(finalMsg);
    }

    private void sendLeaveMsg(String name) {
        if (leaveMessages.get().isEmpty()) return;
        String msg = leaveMessages.get().get(new Random().nextInt(leaveMessages.get().size()));
        msg = msg.replace("{player}", name);
        String finalMsg = msg;
        if (!clientside.get()) messageSender.execute(() -> queueMsg(finalMsg));
        else info(finalMsg);
    }

    private void queueMsg(String msg) {
        try { Thread.sleep(delay.get() * 1000); } catch (Exception ignored) {}
        MessageUtil.sendClientMessage(msg);
    }
}
