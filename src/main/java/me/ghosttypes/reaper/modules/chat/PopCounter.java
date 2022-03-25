package me.ghosttypes.reaper.modules.chat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.player.Stats;
import me.ghosttypes.reaper.util.world.PlayerHelper;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.*;

public class PopCounter extends ReaperModule {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMessages = settings.createGroup("Messages");
    //private final SettingGroup sgKillFX = settings.createGroup("KillFX");

    // General
    private final Setting<Boolean> popAlerts = sgGeneral.add(new BoolSetting.Builder().name("pop-alerts").description("Enable global pop notifications").defaultValue(false).build());
    private final Setting<Boolean> deathAlerts = sgGeneral.add(new BoolSetting.Builder().name("death-alerts").description("Enable global death alerts.").defaultValue(false).build());
    private final Setting<Boolean> removeAlerts = sgGeneral.add(new BoolSetting.Builder().name("remove-alerts").description("Enable global entity removal alerts.").defaultValue(false).build());
    private final Setting<Boolean> own = sgGeneral.add(new BoolSetting.Builder().name("own").description("Notifies you of your own totem pops.").defaultValue(false).build());
    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder().name("friends").description("Notifies you of your friends totem pops.").defaultValue(true).build());
    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder().name("others").description("Notifies you of other players totem pops.").defaultValue(true).build());
    private final Setting<Boolean> announceOthers = sgGeneral.add(new BoolSetting.Builder().name("announce").description("Announce when other players pop.").defaultValue(false).visible(others::get).build());
    public final Setting<Boolean> pmOthers = sgGeneral.add(new BoolSetting.Builder().name("pm").description("Message players when they pop a totem.").defaultValue(false).visible(announceOthers::get).build());
    private final Setting<Integer> announceDelay = sgGeneral.add(new IntSetting.Builder().name("announce-delay").description("How many seconds between announcements.").defaultValue(5).min(1).sliderMax(100).visible(announceOthers::get).build());
    private final Setting<Double> announceRange = sgGeneral.add(new DoubleSetting.Builder().name("announce-range").description("How close players need to be to announce pops or AutoEz.").defaultValue(3).min(0).sliderMax(10).visible(announceOthers::get).build());
    private final Setting<Boolean> dontAnnounceFriends = sgGeneral.add(new BoolSetting.Builder().name("dont-announce-friends").description("Don't announce when your friends pop.").defaultValue(true).build());
    public final Setting<Boolean> doPlaceholders = sgGeneral.add(new BoolSetting.Builder().name("placeholders").description("Enable global placeholders for pop messages.").defaultValue(false).build());
    private final Setting<List<String>> popMessages = sgMessages.add(new StringListSetting.Builder().name("pop-messages").description("Messages to use when announcing pops.").defaultValue(Collections.emptyList()).build());

    // KillFX
    //private final Setting<Boolean> killFX = sgGeneral.add(new BoolSetting.Builder().name("kill-fx").description("render a death effect on people you kill").defaultValue(false).build());

    public final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();
    public final Object2IntMap<UUID> deathPops = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIds = new Object2IntOpenHashMap<>();



    public PopCounter() {
        super(ML.M, "pop-counter", "Count player's totem pops.");
    }
    private int announceWait;

    @Override
    public void onActivate() {
        deathPops.clear();
        totemPops.clear();
        chatIds.clear();
        announceWait = announceDelay.get() * 20;
        if (popMessages.get().isEmpty()) {
            warning("Your pop message list was empty, using the default message.");
            popMessages.get().add("Ez pop {player}");
        }
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        announceWait--;
        synchronized (totemPops) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPops.containsKey(player.getUuid())) continue;
                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    UUID u = player.getUuid();
                    int pops = totemPops.removeInt(u);
                    sendPopAlert(player, pops, true);
                    chatIds.removeInt(player.getUuid());
                    if (deathPops.containsKey(u)) deathPops.removeInt(u); // update death pops
                    deathPops.put(u, pops);
                }
            }
        }
    }


    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        Stats.reset();
        deathPops.clear();
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof PlayerEntity player) {
            String n = player.getEntityName();
            UUID u = player.getUuid();
            if (removeAlerts.get()) { // entity removal alerts
                if (totemPops.containsKey(u)) {
                    int pops = totemPops.getOrDefault(u, 0);
                    info(n + " despawned after popping " + pops + getPopGrammar(pops) + ".");
                } else {
                    info(n + " despawned.");
                }
            }
        }
    }


    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) { // pop tracking
            Entity e = packet.getEntity(mc.world);
            PlayerEntity pl = null;
            boolean isFriend = false;
            if (e == null) { // null check
                return;
            } else if (e instanceof PlayerEntity p) { // make sure it's a player
                pl = p;
                isFriend = Friends.get().isFriend(p); // self, friend, others check
                if (p.equals(mc.player) && !own.get()) return;
                if (isFriend && !friends.get()) return;
                if (!others.get() && !isFriend && !p.equals(mc.player)) return;
            }
            if (pl == null) return; // double-check 'cached' player
            synchronized (totemPops) { // update totem pop database
                int pops = totemPops.getOrDefault(e.getUuid(), 0);
                totemPops.put(e.getUuid(), ++pops);
                sendPopAlert(pl , pops, false);
            }
            if (announceOthers.get() && mc.player.distanceTo(e) <= announceRange.get() && announceWait <= 0) { // handle announcing
                if (isFriend && !dontAnnounceFriends.get()) return;
                String popMessage = getPopMessage(pl);
                String name = pl.getEntityName();
                if (doPlaceholders.get()) popMessage = Formatter.applyPlaceholders(popMessage);
                //if (suffix.get()) popMessage = popMessage + Formatter.getSuffix();
                MessageUtil.sendClientMessage(popMessage);
                if (pmOthers.get()) MessageUtil.sendDM(name, popMessage);
                announceWait = announceDelay.get() * 20;
            }
        }
    }




    private String getPopAlert(PlayerEntity p, int pops, boolean died) {
        String popAlert;
        if (died) popAlert = p.getEntityName() + " died after popping " + pops + getPopGrammar(pops);
        else popAlert = p.getEntityName() + " popped " + pops + getPopGrammar(pops);
        return popAlert;
    }

    private void sendPopAlert(PlayerEntity p, int pops, boolean died) {
        String popAlert = getPopAlert(p, pops, died);
        if (!popAlerts.get() && popAlert.contains("popped")) return;
        if (!deathAlerts.get() && popAlert.contains("died")) return;
        info(getPopAlert(p, pops, died));
    }

    private String getPopGrammar(int pops) {
        if (pops <= 1) return " totem";
        else return " totems";
    }

    private String getPopMessage(PlayerEntity p) {
        if (popMessages.get().isEmpty()) return "Ez pop {player}";
        String playerName = p.getEntityName();
        String popMessage = popMessages.get().get(new Random().nextInt(popMessages.get().size()));
        if (popMessage.contains("{pops}") && totemPops.containsKey(p.getUuid())) {
            int pops = totemPops.getOrDefault(p.getUuid(), 0);
            popMessage = popMessage.replace("{pops}", pops + " " + getPopGrammar(pops));
        } else {
            boolean f = false;
            for (String s : popMessages.get()) {
                if (!s.contains("{pops}")) {
                    f = true;
                    popMessage = s;
                    break;
                }
            }
            if (!f) popMessage = "Ez pop {player}";
        }
        if (popMessage.contains("{player}")) popMessage = popMessage.replace("{player}", playerName);
        return popMessage;
    }


    private void doFX(double x, double y, double z) {
        LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        if (lightningEntity.getType() == null) return;
        lightningEntity.updatePosition(x, y ,z);
        lightningEntity.refreshPositionAfterTeleport(x, y, z);
        mc.world.addEntity(lightningEntity.getId(), lightningEntity);
    }

    /*@Override
    public String getInfoString() {
        return Integer.toString(targets.size());
    }*/
}
