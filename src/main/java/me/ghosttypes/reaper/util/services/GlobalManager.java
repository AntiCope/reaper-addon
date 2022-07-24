package me.ghosttypes.reaper.util.services;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.misc.MathUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class GlobalManager {
    public static ArrayList<DeathEntry> deathEntries = new ArrayList<>();
    public static final Object2IntMap<UUID> deaths = new Object2IntOpenHashMap<>(); // todo - implement as a placeholder for autoez


    public static long lastRespawnTime = System.currentTimeMillis();

    public static boolean canAutoEz() {
        if (mc.player.isDead()) return false;
        return MathUtil.msPassed(lastRespawnTime) >= 5;
    }

    public static int getDeaths(PlayerEntity player) {
        return deaths.getOrDefault(player.getUuid(), 0);
    }

    public static DeathEntry getDeathEntry(String name) {
        for (DeathEntry entry : deathEntries) if (entry.getName().equalsIgnoreCase(name)) return entry;
        return null;
    }

    public static DeathEntry getDeathEntry(PlayerEntity player) {
        for (DeathEntry entry : deathEntries) if (entry.getPlayer().equals(player)) return entry;
        return null;
    }

    public static void addDeathEntry(PlayerEntity player, int pops, Vec3d pos) {
        ArrayList<DeathEntry> toRemove = new ArrayList<>();
        String pName = player.getEntityName();
        UUID u = player.getUuid();
        for (DeathEntry entry : deathEntries) if (entry.getName().equalsIgnoreCase(pName)) toRemove.add(entry); // remove all entries with the same name
        deathEntries.removeIf(toRemove::contains);
        deathEntries.add(new DeathEntry(player, pops, pos)); // add new entry
        if (deaths.containsKey(u)) deaths.put(u, deaths.getInt(u) + 1); // increase death count for the player
        else deaths.put(u, 1); // create new death count for the player
    }


    @EventHandler
    public void onTick(TickEvent.Post event) {
        ArrayList<DeathEntry> toRemove = new ArrayList<>();
        for (DeathEntry entry : deathEntries) if (entry.getTime() + 2500 < System.currentTimeMillis()) toRemove.add(entry); // remove all entries that are older than 2.5 seconds
        deathEntries.removeIf(toRemove::contains);
        Reaper.log("test");
    }

    public static class DeathEntry {
        private final PlayerEntity player;
        private final String name;
        private final Vec3d pos;
        private final int pops;
        private final long time;

        public DeathEntry(PlayerEntity player, int pops, Vec3d pos) {
            this.player = player;
            this.name = player.getEntityName();
            this.pops = pops;
            this.time = MathUtil.now();
            this.pos = pos;
        }

        public PlayerEntity getPlayer() { return player; }
        public String getName() { return name; }
        public Vec3d getPos() { return pos; }
        public int getPops() { return pops; }
        public long getTime() { return time; }
    }
}
